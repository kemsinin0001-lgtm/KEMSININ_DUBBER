package com.kemsinin.dubber.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which part of the dubbing pipeline the app is currently running. */
enum class Stage {
    IDLE,
    RESOLVING,
    DOWNLOADING,
    EXTRACTING,
    TRANSCRIBING,
    TRANSLATING,
    SPEAKING,
    DONE,
    ERROR,
}

/** One subtitle cue. */
data class Cue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/** A video either downloaded from Douyin or imported from the device. */
data class MediaItem(
    val name: String,
    val path: String,
    val durationMs: Long = 0L,
    val remote: Boolean = false,
    val cover: String = "",
)

data class LanguageOption(val value: String, val label: String)

data class VoiceOption(val value: String, val label: String)

val LANGUAGE_OPTIONS = listOf(
    LanguageOption("Khmer", "ខ្មែរ"),
    LanguageOption("English", "អង់គ្លេស"),
    LanguageOption("Thai", "ថៃ"),
    LanguageOption("Chinese", "ចិន"),
    LanguageOption("Vietnamese", "វៀតណាម"),
)

val VOICE_OPTIONS = listOf(
    VoiceOption("km-KH-PisethNeural", "ប្រុស · ភាសាខ្មែរ"),
    VoiceOption("km-KH-SreymomNeural", "ស្រី · ភាសាខ្មែរ"),
    VoiceOption("th-TH-PremwadeeNeural", "ស្រី · ភាសាថៃ"),
    VoiceOption("zh-CN-XiaoxiaoNeural", "ស្រី · ភាសាចិន"),
    VoiceOption("en-US-AriaNeural", "ស្រី · អង់គ្លេស"),
)

// ---------------------------------------------------------------------------
// SRT helpers (kept in Kotlin so the editor works without waking the engine)
// ---------------------------------------------------------------------------
private val SRT_BLOCK = Regex(
    "(\\d+)\\s*\\n\\s*(\\d{2}:\\d{2}:\\d{2}[,.]\\d{1,3})\\s*-->\\s*" +
        "(\\d{2}:\\d{2}:\\d{2}[,.]\\d{1,3})\\s*\\n([\\s\\S]*?)(?=\\n\\s*\\n|$)"
)

fun srtTimeToMs(value: String): Long {
    val parts = value.replace(',', '.').trim().split(":")
    if (parts.size < 3) return 0L
    val hours = parts[0].toLongOrNull() ?: 0L
    val minutes = parts[1].toLongOrNull() ?: 0L
    val seconds = parts[2].toDoubleOrNull() ?: 0.0
    return (hours * 3600L + minutes * 60L) * 1000L + (seconds * 1000.0).toLong()
}

fun msToSrt(value: Long): String {
    val total = if (value < 0L) 0L else value
    return "%02d:%02d:%02d,%03d".format(
        total / 3600000L,
        (total / 60000L) % 60L,
        (total / 1000L) % 60L,
        total % 1000L,
    )
}

/** mm:ss, or h:mm:ss for long videos. */
fun formatClock(value: Long): String {
    val total = if (value < 0L) 0L else value
    val hours = total / 3600000L
    val minutes = (total / 60000L) % 60L
    val seconds = (total / 1000L) % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun parseSrt(content: String): List<Cue> {
    val normalized = content.replace("\r\n", "\n").trim()
    if (normalized.isEmpty()) return emptyList()
    return SRT_BLOCK.findAll(normalized).mapIndexed { position, match ->
        val values = match.groupValues
        Cue(
            index = position + 1,
            startMs = srtTimeToMs(values[2]),
            endMs = srtTimeToMs(values[3]),
            text = values[4].replace("\n", " ").trim(),
        )
    }.filter { it.text.isNotEmpty() }.toList()
}

fun buildSrt(cues: List<Cue>): String {
    val builder = StringBuilder()
    cues.forEachIndexed { position, cue ->
        builder.append(position + 1).append('\n')
        builder.append(msToSrt(cue.startMs)).append(" --> ").append(msToSrt(cue.endMs)).append('\n')
        builder.append(cue.text.trim()).append("\n\n")
    }
    return builder.toString().trimEnd() + "\n"
}

/** Percent value for edge-tts, e.g. -20 -> "-20%". */
fun percentArg(percent: Float): String {
    val rounded = percent.toInt()
    return (if (rounded >= 0) "+" else "") + rounded + "%"
}

/** Pitch value for edge-tts, e.g. 5 -> "+5Hz". */
fun hertzArg(hertz: Float): String {
    val rounded = hertz.toInt()
    return (if (rounded >= 0) "+" else "") + rounded + "Hz"
}

// ---------------------------------------------------------------------------
// Screen state
// ---------------------------------------------------------------------------
class DubberState {
    var tab by mutableStateOf(0)

    // Source
    var linkInput by mutableStateOf("")
    var videoTitle by mutableStateOf("")
    var videoAuthor by mutableStateOf("")
    var cover by mutableStateOf("")
    var durationMs by mutableStateOf(0L)
    var streamUrl by mutableStateOf("")
    var localVideoPath by mutableStateOf("")
    var localVideoName by mutableStateOf("")
    var localVideoLabel by mutableStateOf("")

    // On-device audio decoded from the video, used for speech-to-text
    var audioPath by mutableStateOf("")
    var audioLabel by mutableStateOf("")

    // AI settings
    var apiKey by mutableStateOf("")
    var targetLang by mutableStateOf(LANGUAGE_OPTIONS.first().value)
    var voice by mutableStateOf(VOICE_OPTIONS.first().value)
    var ratePercent by mutableStateOf(0f)
    var pitchHz by mutableStateOf(0f)
    var volumePercent by mutableStateOf(0f)

    // Group of imported/downloaded videos
    val group = mutableStateListOf<MediaItem>()

    // Subtitles
    val cues = mutableStateListOf<Cue>()
    val originalCues = mutableStateListOf<Cue>()
    var showOriginal by mutableStateOf(true)
    var scriptText by mutableStateOf("")

    // Pipeline
    var stage by mutableStateOf(Stage.IDLE)
    var busy by mutableStateOf(false)
    var detail by mutableStateOf("")
    var progress by mutableStateOf(0f)
    var failed by mutableStateOf(false)
    val log = mutableStateListOf<String>()

    // Export artifacts
    var artifactPath by mutableStateOf("")
    var artifactName by mutableStateOf("")
    var artifactMime by mutableStateOf("text/plain")

    val currentItem: MediaItem?
        get() = group.firstOrNull()

    val totalDurationMs: Long
        get() = group.sumOf { it.durationMs }

    fun note(message: String) {
        detail = message
        log.add(0, message)
        if (log.size > 60) {
            log.removeAt(log.size - 1)
        }
    }

    fun begin(next: Stage, message: String) {
        stage = next
        busy = true
        failed = false
        note(message)
    }

    fun succeed(message: String) {
        stage = Stage.DONE
        busy = false
        failed = false
        progress = 1f
        note(message)
    }

    fun fail(message: String) {
        stage = Stage.ERROR
        busy = false
        failed = true
        note(message)
    }

    fun reset() {
        stage = Stage.IDLE
        busy = false
        failed = false
        progress = 0f
        detail = ""
    }

    fun addToGroup(item: MediaItem) {
        if (group.none { it.path == item.path }) {
            group.add(item)
        }
    }

    /** Makes [item] the active video of the studio. */
    fun select(item: MediaItem) {
        group.remove(item)
        group.add(0, item)
        localVideoPath = item.path
        localVideoName = item.name
        localVideoLabel = formatClock(item.durationMs)
        durationMs = item.durationMs
        videoTitle = item.name
        cover = item.cover
    }

    /** Splits pasted script text into evenly timed cues across the current video. */
    fun cuesFromScript(script: String) {
        val lines = script.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        val total = if (durationMs > 0L) durationMs else lines.size * 3000L
        val span = total / lines.size
        cues.clear()
        lines.forEachIndexed { position, line ->
            cues.add(
                Cue(
                    index = position + 1,
                    startMs = span * position,
                    endMs = span * (position + 1),
                    text = line,
                )
            )
        }
        originalCues.clear()
        originalCues.addAll(cues)
    }

    /**
     * Appends dictated lines as cues, filling the timeline from the end of the
     * last cue to the end of the video (3 s per line when there is no room).
     */
    fun appendCues(text: String) {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        val start = cues.lastOrNull()?.endMs ?: 0L
        val end = if (durationMs > start) durationMs else start + lines.size * 3000L
        val span = ((end - start) / lines.size).coerceAtLeast(1500L)

        lines.forEachIndexed { offset, line ->
            val from = start + span * offset
            cues.add(Cue(index = cues.size + 1, startMs = from, endMs = from + span, text = line))
        }
        originalCues.clear()
        originalCues.addAll(cues)
    }
}
