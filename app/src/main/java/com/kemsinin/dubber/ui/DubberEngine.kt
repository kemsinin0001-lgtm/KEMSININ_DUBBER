package com.kemsinin.dubber.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.chaquo.python.Python
import com.kemsinin.dubber.media.AudioExtraction
import com.kemsinin.dubber.media.AudioExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * All the real dubbing work: talks to the Chaquopy Python engine for network,
 * translation and speech synthesis, and uses Android APIs for local media.
 */
object DubberEngine {

    private const val ENGINE = "dubber_engine"
    private val videoExtensions = listOf(".mp4", ".mkv", ".mov", ".webm", ".avi", ".m4v", ".3gp")

    // -----------------------------------------------------------------------
    // Chaquopy bridge
    // -----------------------------------------------------------------------
    private fun callJson(function: String, vararg args: Any): JSONObject {
        val raw = Python.getInstance().getModule(ENGINE).callAttr(function, *args).toString()
        return JSONObject(raw)
    }

    private fun errorOf(json: JSONObject): String =
        json.optString("error", "Unknown engine error")

    suspend fun describeEngine(): String = withContext(Dispatchers.IO) {
        try {
            val json = callJson("engine_info")
            "Python ${json.optString("python")} · requests ${json.optString("requests")} · " +
                "edge-tts ${json.optString("edge_tts")} · Gemini ${json.optString("gemini")}"
        } catch (error: Throwable) {
            "Engine unavailable: ${error.localizedMessage ?: "unknown"}"
        }
    }

    // -----------------------------------------------------------------------
    // Local media helpers
    // -----------------------------------------------------------------------
    private fun displayName(context: Context, uri: Uri): String {
        var name = uri.lastPathSegment ?: "video.mp4"
        try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (column >= 0) {
                            name = cursor.getString(column) ?: name
                        }
                    }
                }
        } catch (_: Throwable) {
            // Fall back to the path segment.
        }
        return name
    }

    private fun durationOf(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
                // Nothing to release.
            }
        }
    }

    private fun isVideoName(name: String): Boolean =
        videoExtensions.any { name.lowercase().endsWith(it) }

    /** Accepts both downloaded file paths and imported content:// URIs. */
    private fun sourceUri(path: String): Uri =
        if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            Uri.fromFile(File(path))
        }

    fun thumbnailOf(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
                // Nothing to release.
            }
        }
    }

    // -----------------------------------------------------------------------
    // Douyin source
    // -----------------------------------------------------------------------
    suspend fun resolveLink(state: DubberState) {
        val link = state.linkInput.trim()
        if (link.isEmpty()) {
            state.fail("សូមបញ្ចូល Link វីដេអូ Douyin ជាមុនសិន")
            return
        }
        state.begin(Stage.RESOLVING, "កំពុងអាន Link វីដេអូ…")
        withContext(Dispatchers.IO) {
            try {
                val json = callJson("fetch_video_info", link)
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }
                val title = json.optString("title").ifBlank { "Douyin video" }
                state.videoTitle = title
                state.videoAuthor = json.optString("author")
                state.cover = json.optString("cover")
                state.durationMs = json.optLong("duration_ms", 0L)
                state.streamUrl = json.optString("video_url")
                state.succeed("រកឃើញ៖ $title")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "អាន Link មិនបានសម្រេច")
            }
        }
    }

    suspend fun downloadFromLink(state: DubberState, context: Context) {
        val link = state.linkInput.trim()
        if (link.isEmpty()) {
            state.fail("សូមបញ្ចូល Link វីដេអូ Douyin ជាមុនសិន")
            return
        }
        state.begin(Stage.DOWNLOADING, "កំពុងទាញយកវីដេអូគ្មាន Watermark…")
        withContext(Dispatchers.IO) {
            try {
                val folder = File(context.filesDir, "downloads").absolutePath
                val json = callJson("download_video", link, folder)
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }
                val path = json.optString("file_path")
                val title = json.optString("title").ifBlank { "Douyin video" }
                state.videoTitle = title
                state.videoAuthor = json.optString("author")
                state.cover = json.optString("cover")
                state.streamUrl = ""
                state.durationMs = json.optLong("duration_ms", 0L)
                state.localVideoPath = path
                state.localVideoName = File(path).name
                state.localVideoLabel = json.optString("size_label")
                state.artifactPath = path
                state.artifactName = File(path).name
                state.artifactMime = "video/mp4"
                state.addToGroup(
                    MediaItem(
                        name = title,
                        path = path,
                        durationMs = state.durationMs,
                        remote = true,
                        cover = state.cover,
                    )
                )
                state.succeed("បានទាញយក៖ $title")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "ទាញយកមិនបានសម្រេច")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Device import
    // -----------------------------------------------------------------------
    suspend fun importVideo(state: DubberState, context: Context, uri: Uri) {
        state.begin(Stage.IDLE, "កំពុងនាំចូលវីដេអូ…")
        withContext(Dispatchers.IO) {
            try {
                val name = displayName(context, uri)
                val duration = durationOf(context, uri)
                state.localVideoPath = uri.toString()
                state.localVideoName = name
                state.localVideoLabel = formatClock(duration)
                state.durationMs = duration
                state.addToGroup(
                    MediaItem(name = name, path = uri.toString(), durationMs = duration)
                )
                state.succeed("បាននាំចូល៖ $name")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "នាំចូលមិនបានសម្រេច")
            }
        }
    }

    suspend fun importFolder(state: DubberState, context: Context, treeUri: Uri) {
        state.begin(Stage.IDLE, "កំពុងអាន Folder…")
        withContext(Dispatchers.IO) {
            try {
                val treeId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri =
                    DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                )
                var added = 0
                context.contentResolver
                    .query(childrenUri, projection, null, null, null)
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val documentId = cursor.getString(0) ?: continue
                            val name = cursor.getString(1) ?: continue
                            val mime = cursor.getString(2) ?: ""
                            if (!mime.startsWith("video/") && !isVideoName(name)) continue

                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                            state.addToGroup(
                                MediaItem(
                                    name = name,
                                    path = fileUri.toString(),
                                    durationMs = durationOf(context, fileUri),
                                )
                            )
                            added += 1
                        }
                    }
                if (added == 0) {
                    state.fail("រកមិនឃើញវីដេអូក្នុង Folder នេះទេ")
                } else {
                    state.succeed("បាននាំចូលវីដេអូ $added ពី Folder")
                }
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "អាន Folder មិនបានសម្រេច")
            }
        }
    }

    suspend fun importSrt(state: DubberState, context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val parsed = parseSrt(content)
                if (parsed.isEmpty()) {
                    state.fail("ឯកសារ SRT នេះទទេ ឬមិនត្រឹមត្រូវ")
                    return@withContext
                }
                state.cues.clear()
                state.cues.addAll(parsed)
                state.originalCues.clear()
                state.originalCues.addAll(parsed)
                state.note("បាននាំចូលអក្សររត់ ${parsed.size} បន្ទាត់")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "នាំចូល SRT មិនបានសម្រេច")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Translation
    // -----------------------------------------------------------------------
    suspend fun testApiKey(state: DubberState) {
        if (state.apiKey.isBlank()) {
            state.fail("សូមបញ្ចូល Gemini API Key")
            return
        }
        state.begin(Stage.TRANSLATING, "កំពុងផ្ទៀងផ្ទាត់ API Key…")
        withContext(Dispatchers.IO) {
            try {
                val json = callJson("test_gemini", state.apiKey.trim())
                if (json.optBoolean("ok")) {
                    state.succeed("API Key ដំណើរការល្អ (${json.optString("reply")})")
                } else {
                    state.fail(errorOf(json))
                }
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "ផ្ទៀងផ្ទាត់មិនបានសម្រេច")
            }
        }
    }

    suspend fun translateSubtitles(state: DubberState, context: Context) {
        val cues = state.cues.toList()
        if (cues.isEmpty()) {
            state.fail("មិនទាន់មានអក្សររត់សម្រាប់បកប្រែទេ")
            return
        }
        if (state.apiKey.isBlank()) {
            state.fail("សូមបញ្ចូល Gemini API Key ក្នុងផ្ទាំង «កែសម្រួល»")
            return
        }
        state.begin(Stage.TRANSLATING, "កំពុងបកប្រែអក្សររត់ទៅ ${state.targetLang}…")
        withContext(Dispatchers.IO) {
            try {
                if (state.originalCues.isEmpty()) {
                    state.originalCues.addAll(cues)
                }
                val json = callJson(
                    "translate_srt",
                    buildSrt(cues),
                    state.apiKey.trim(),
                    state.targetLang,
                )
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }
                val translated = parseSrt(json.optString("srt"))
                if (translated.isEmpty()) {
                    state.fail("ការបកប្រែត្រឡប់មកវិញទទេ")
                    return@withContext
                }
                state.cues.clear()
                state.cues.addAll(translated)

                val label = state.videoTitle.ifBlank { "subtitle" }
                val name = "${label.take(40)}-${state.targetLang.lowercase()}.srt"
                val folder = File(context.filesDir, "export").apply { mkdirs() }
                val target = File(folder, name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                target.writeText(buildSrt(translated), Charsets.UTF_8)
                state.artifactPath = target.absolutePath
                state.artifactName = target.name
                state.artifactMime = "text/plain"
                state.succeed("បានបកប្រែ ${translated.size} បន្ទាត់ទៅ ${state.targetLang}")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "បកប្រែមិនបានសម្រេច")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Speech to text (subtitles from the video audio)
    // -----------------------------------------------------------------------
    /**
     * Decodes the video's audio track on the device and turns it into timed
     * subtitle cues, so the rest of the pipeline can run without a script.
     */
    suspend fun autoSubtitles(state: DubberState, context: Context) {
        val source = state.localVideoPath
        if (source.isBlank()) {
            state.fail("សូមបញ្ចូល Link ឬនាំចូលវីដេអូជាមុនសិន")
            return
        }
        if (state.apiKey.isBlank()) {
            state.fail("សូមបញ្ចូល Gemini API Key ក្នុងផ្ទាំង «កែសម្រួល» ដើម្បីសរសេរអក្សររត់")
            return
        }

        state.begin(Stage.EXTRACTING, "កំពុងបំបែកសំឡេងពីវីដេអូ…")
        val folder = File(context.cacheDir, "audio").apply { mkdirs() }
        val target = File(folder, "source.wav")
        val outcome = AudioExtractor.extract(context, sourceUri(source), target)
        if (outcome is AudioExtraction.Failure) {
            state.fail(outcome.message)
            return
        }
        val audio = outcome as AudioExtraction.Success
        state.audioPath = audio.path
        state.audioLabel = "%.1f MB".format(audio.sizeBytes / 1048576.0)
        if (state.durationMs <= 0L) {
            state.durationMs = audio.durationMs
        }
        state.note(
            "សំឡេង ${formatClock(audio.durationMs)} · ${state.audioLabel} (បំបែកនៅលើទូរស័ព្ទ)"
        )

        state.begin(Stage.TRANSCRIBING, "កំពុងស្តាប់ និងសរសេរអក្សររត់…")
        withContext(Dispatchers.IO) {
            try {
                val json = callJson("transcribe_audio", audio.path, state.apiKey.trim(), "auto")
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }

                val parsed = parseSrt(json.optString("srt"))
                if (parsed.isNotEmpty()) {
                    state.cues.clear()
                    state.cues.addAll(parsed)
                    state.originalCues.clear()
                    state.originalCues.addAll(parsed)
                    state.succeed("បានសរសេរអក្សររត់ ${parsed.size} បន្ទាត់ពីសំឡេងវីដេអូ")
                    return@withContext
                }

                val plain = json.optString("plain").trim()
                if (plain.isEmpty()) {
                    state.fail("រកមិនឃើញសំឡេងនិយាយក្នុងវីដេអូនេះទេ")
                    return@withContext
                }
                state.cuesFromScript(plain)
                state.succeed(
                    "បានបង្កើតអក្សររត់ ${state.cues.size} បន្ទាត់ (ដោយគ្មាន timestamp ពី AI)"
                )
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "សរសេរអក្សររត់មិនបានសម្រេច")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Voice synthesis
    // -----------------------------------------------------------------------
    suspend fun previewVoice(state: DubberState, context: Context, sample: String) {
        state.begin(Stage.SPEAKING, "កំពុងបង្កើតសំឡេងសាកល្បង…")
        withContext(Dispatchers.IO) {
            try {
                val folder = File(context.cacheDir, "voice").apply { mkdirs() }
                val target = File(folder, "preview.mp3").absolutePath
                val json = callJson(
                    "synthesize",
                    sample,
                    state.voice,
                    target,
                    percentArg(state.ratePercent),
                    hertzArg(state.pitchHz),
                    percentArg(state.volumePercent),
                )
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }
                state.artifactPath = target
                state.artifactName = "voice-preview.mp3"
                state.artifactMime = "audio/mpeg"
                state.succeed("សំឡេងសាកល្បងរួចរាល់ · ${state.voice}")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "បង្កើតសំឡេងមិនបានសម្រេច")
            }
        }
    }

    suspend fun generateVoiceover(state: DubberState, context: Context) {
        val cues = state.cues.toList()
        if (cues.isEmpty()) {
            state.fail("មិនទាន់មានអត្ថបទសម្រាប់បង្កើតសំឡេងទេ")
            return
        }
        state.begin(Stage.SPEAKING, "កំពុងបង្កើតសំឡេងខ្មែរ ${cues.size} បន្ទាត់…")
        withContext(Dispatchers.IO) {
            try {
                val payload = org.json.JSONArray()
                cues.forEach { cue ->
                    payload.put(
                        JSONObject()
                            .put("start_ms", cue.startMs)
                            .put("end_ms", cue.endMs)
                            .put("text", cue.text)
                    )
                }
                val folder = File(context.filesDir, "voiceover").apply { mkdirs() }
                val json = callJson(
                    "synthesize_segments",
                    payload.toString(),
                    state.voice,
                    folder.absolutePath,
                    percentArg(state.ratePercent),
                    hertzArg(state.pitchHz),
                    percentArg(state.volumePercent),
                )
                if (!json.optBoolean("ok")) {
                    state.fail(errorOf(json))
                    return@withContext
                }
                val succeeded = json.optInt("succeeded")
                val failed = json.optInt("failed")
                val first = json.optJSONArray("segments")
                    ?.let { array ->
                        (0 until array.length())
                            .map { array.optJSONObject(it) }
                            .firstOrNull { it?.optString("file_path").orEmpty().isNotEmpty() }
                    }
                    ?.optString("file_path")
                    .orEmpty()
                if (first.isNotEmpty()) {
                    state.artifactPath = first
                    state.artifactName = "${state.videoTitle.ifBlank { "voiceover" }.take(40)}-${state.targetLang.lowercase()}.mp3"
                    state.artifactMime = "audio/mpeg"
                }
                state.succeed("បានបង្កើតសំឡេង $succeeded បន្ទាត់ · បរាជ័យ $failed")
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "បង្កើតសំឡេងមិនបានសម្រេច")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Full pipeline
    // -----------------------------------------------------------------------
    /** Download (when a link is present) then translate the cues and synthesize the voice. */
    suspend fun runPipeline(state: DubberState, context: Context) {
        if (state.linkInput.isNotBlank() && state.localVideoPath.isBlank()) {
            downloadFromLink(state, context)
            if (state.failed) return
        }
        if (state.cues.isEmpty()) {
            if (state.localVideoPath.isBlank()) {
                state.fail("សូមបញ្ចូល Link ឬនាំចូលវីដេអូជាមុនសិន")
                return
            }
            // No script and no subtitles yet: generate them from the audio.
            autoSubtitles(state, context)
            if (state.failed || state.cues.isEmpty()) return
        }
        translateSubtitles(state, context)
        if (state.failed) return
        generateVoiceover(state, context)
    }

    // -----------------------------------------------------------------------
    // Export
    // -----------------------------------------------------------------------
    /** Copies the current artifact (video / mp3 / srt) into the location the user picked. */
    suspend fun exportArtifact(state: DubberState, context: Context, target: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val source = File(state.artifactPath)
                if (!source.exists()) {
                    state.fail("មិនមានឯកសារសម្រាប់រក្សាទុកទេ")
                    return@withContext false
                }
                context.contentResolver.openOutputStream(target)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                }
                state.note("បានរក្សាទុក៖ ${state.artifactName}")
                true
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "រក្សាទុកមិនបានសម្រេច")
                false
            }
        }

    suspend fun exportSrt(state: DubberState, context: Context, target: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val content = buildSrt(state.cues)
                context.contentResolver.openOutputStream(target)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
                state.note("បានរក្សាទុកអក្សររត់ SRT")
                true
            } catch (error: Throwable) {
                state.fail(error.localizedMessage ?: "រក្សាទុកមិនបានសម្រេច")
                false
            }
        }
}
