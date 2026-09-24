package com.kemsinin.dubber.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Outcome of decoding a video's audio track. */
sealed class AudioExtraction {
    data class Success(
        val path: String,
        val durationMs: Long,
        val sizeBytes: Long,
        val sampleRate: Int,
    ) : AudioExtraction()

    data class Failure(val message: String) : AudioExtraction()
}

/**
 * Decodes the audio track of a video into a 16 kHz mono WAV file so it can be
 * fed to speech recognition. Runs entirely on the device using MediaExtractor
 * and MediaCodec — no native libraries and no extra dependencies.
 */
object AudioExtractor {

    private const val TARGET_RATE = 16_000
    private const val MAX_FRAMES = TARGET_RATE * 60L * 15L // 15 minutes of audio
    private const val TIMEOUT_US = 10_000L
    private const val WALL_CLOCK_LIMIT_MS = 5L * 60L * 1000L

    suspend fun extract(context: Context, source: Uri, outFile: File): AudioExtraction =
        withContext(Dispatchers.IO) {
            try {
                decode(context, source, outFile)
            } catch (error: Throwable) {
                AudioExtraction.Failure(error.localizedMessage ?: "បំបែកសំឡេងមិនបានសម្រេច")
            }
        }

    private fun decode(context: Context, source: Uri, outFile: File): AudioExtraction {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(context, source, null)

            var trackIndex = -1
            var trackFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    trackFormat = format
                    break
                }
            }

            val format = trackFormat
            if (trackIndex < 0 || format == null) {
                return AudioExtraction.Failure("វីដេអូនេះមិនមានផ្លូវសំឡេងទេ")
            }

            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return AudioExtraction.Failure("មិនស្គាល់ប្រភេទសំឡេង")
            extractor.selectTrack(trackIndex)

            var sourceRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44_100
            }
            var sourceChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }
            var floatPcm = isFloatPcm(format)
            var resampler = LinearResampler(ratioFor(sourceRate))

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val sink = ShortSink()
            val bufferInfo = MediaCodec.BufferInfo()
            val deadline = System.currentTimeMillis() + WALL_CLOCK_LIMIT_MS
            var inputDone = false
            var outputDone = false

            while (!outputDone && System.currentTimeMillis() < deadline) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val changed = decoder.outputFormat
                    if (changed.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sourceRate = changed.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (changed.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        sourceChannels = changed.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    floatPcm = isFloatPcm(changed)
                    resampler = LinearResampler(ratioFor(sourceRate))
                } else if (outputIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null) {
                            val frames = readFrames(outputBuffer, bufferInfo, sourceChannels, floatPcm)
                            if (frames.size > 1) {
                                resampler.resample(frames, frames.size, sink)
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                    if (sink.count >= MAX_FRAMES) {
                        outputDone = true
                    }
                }
            }

            if (sink.count == 0L) {
                return AudioExtraction.Failure("រកមិនឃើញសំឡេងក្នុងវីដេអូនេះទេ")
            }

            val size = writeWav(outFile, sink)
            return AudioExtraction.Success(
                path = outFile.absolutePath,
                durationMs = sink.count * 1000L / TARGET_RATE,
                sizeBytes = size,
                sampleRate = TARGET_RATE,
            )
        } finally {
            try {
                decoder?.stop()
            } catch (_: Throwable) {
                // Decoder was never started.
            }
            try {
                decoder?.release()
            } catch (_: Throwable) {
                // Already released.
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
                // Already released.
            }
        }
    }

    private fun ratioFor(sourceRate: Int): Double {
        if (sourceRate <= 0) return 1.0
        return sourceRate.toDouble() / TARGET_RATE.toDouble()
    }

    private fun isFloatPcm(format: MediaFormat): Boolean {
        if (!format.containsKey(MediaFormat.KEY_PCM_ENCODING)) return false
        return format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
    }

    /** Reads one decoded buffer and folds it down to mono 16-bit frames. */
    private fun readFrames(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        floatPcm: Boolean,
    ): ShortArray {
        val safeChannels = if (channels < 1) 1 else channels
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)

        if (floatPcm) {
            val floats = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
            val frameCount = floats.remaining() / safeChannels
            val mono = ShortArray(frameCount)
            for (frame in 0 until frameCount) {
                var sum = 0f
                for (channel in 0 until safeChannels) {
                    if (floats.remaining() <= 0) break
                    sum += floats.get()
                }
                val value = (sum / safeChannels).coerceIn(-1f, 1f)
                mono[frame] = (value * 32767f).toInt().toShort()
            }
            return mono
        }

        val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val frameCount = shorts.remaining() / safeChannels
        val mono = ShortArray(frameCount)
        for (frame in 0 until frameCount) {
            var sum = 0
            for (channel in 0 until safeChannels) {
                if (shorts.remaining() <= 0) break
                sum += shorts.get().toInt()
            }
            mono[frame] = (sum / safeChannels).toShort()
        }
        return mono
    }

    private fun writeWav(outFile: File, sink: ShortSink): Long {
        outFile.parentFile?.mkdirs()
        val data = sink.toByteArray()
        val channels = 1
        val bitsPerSample = 16
        val blockAlign = channels * bitsPerSample / 8
        val byteRate = TARGET_RATE * blockAlign

        FileOutputStream(outFile).use { out ->
            out.write(ascii("RIFF"))
            out.write(intLe(36 + data.size))
            out.write(ascii("WAVE"))
            out.write(ascii("fmt "))
            out.write(intLe(16))
            out.write(shortLe(1)) // PCM
            out.write(shortLe(channels))
            out.write(intLe(TARGET_RATE))
            out.write(intLe(byteRate))
            out.write(shortLe(blockAlign))
            out.write(shortLe(bitsPerSample))
            out.write(ascii("data"))
            out.write(intLe(data.size))
            out.write(data)
        }
        return outFile.length()
    }

    private fun ascii(value: String): ByteArray = value.toByteArray(Charsets.US_ASCII)

    private fun intLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun shortLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
    )

    /** Growable little-endian 16-bit PCM sink. */
    private class ShortSink {
        private val stream = ByteArrayOutputStream(1 shl 20)

        var count = 0L
            private set

        fun add(value: Short) {
            val number = value.toInt()
            stream.write(number and 0xFF)
            stream.write((number shr 8) and 0xFF)
            count += 1L
        }

        fun toByteArray(): ByteArray = stream.toByteArray()
    }

    /** Linear-interpolation resampler that carries its fractional position between buffers. */
    private class LinearResampler(private val ratio: Double) {
        private var position = 0.0
        private val step = if (ratio > 0.0) ratio else 1.0

        fun resample(frames: ShortArray, count: Int, sink: ShortSink) {
            if (count < 2) return
            var cursor = position
            while (cursor + 1.0 < count) {
                val index = cursor.toInt()
                val fraction = (cursor - index).toFloat()
                val first = frames[index].toFloat()
                val second = frames[index + 1].toFloat()
                sink.add((first + (second - first) * fraction).toInt().toShort())
                cursor += step
            }
            val carried = cursor - count
            position = if (carried > 0.0) carried else 0.0
        }
    }
}
