package com.kemsinin.dubber.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Plays a synthesized voice file so the user can hear the result in the app. */
class AudioPreview(private val context: Context) {

    private var player: MediaPlayer? = null

    var isPlaying by mutableStateOf(false)
        private set

    var currentPath by mutableStateOf("")
        private set

    /** Starts playback, or stops it when the same file is already playing. */
    fun toggle(path: String): String? {
        if (path.isBlank()) {
            return "មិនមានឯកសារសំឡេងសម្រាប់ចាក់ទេ"
        }
        if (isPlaying && path == currentPath) {
            stop()
            return null
        }
        stop()
        return try {
            val created = MediaPlayer()
            created.setDataSource(context, Uri.fromFile(File(path)))
            created.setOnCompletionListener { isPlaying = false }
            created.setOnErrorListener { _, _, _ ->
                isPlaying = false
                true
            }
            created.prepare()
            created.start()
            player = created
            currentPath = path
            isPlaying = true
            null
        } catch (error: Throwable) {
            isPlaying = false
            error.localizedMessage ?: "ចាក់សំឡេងមិនបានសម្រេច"
        }
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Throwable) {
            // Already stopped.
        }
        try {
            player?.release()
        } catch (_: Throwable) {
            // Already released.
        }
        player = null
        isPlaying = false
    }

    fun release() {
        stop()
        currentPath = ""
    }
}

@Composable
fun rememberAudioPreview(): AudioPreview {
    val context = LocalContext.current
    val preview = remember { AudioPreview(context) }
    DisposableEffect(Unit) {
        onDispose { preview.release() }
    }
    return preview
}
