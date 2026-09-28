package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import kotlin.math.sin

class SpeechRecognizerHelper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartial: (String) -> Unit,
    private val onRmsChangedCallback: (Float) -> Unit,
    private val onErrorCallback: (String) -> Unit
) {
    companion object {
        private const val TAG = "SpeechRecognizerHelper"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningInternal = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorCallback("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "SpeechRecognizer ready")
                            isListeningInternal = true
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "User started speaking")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize dB (-2 to 10 typical) to 0.0 .. 1.0 range
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            onRmsChangedCallback(normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(TAG, "User stopped speaking")
                            isListeningInternal = false
                            _isListening.value = false
                        }

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                                else -> "Recognition error: $error"
                            }
                            Log.w(TAG, "Speech error: $msg ($error)")
                            isListeningInternal = false
                            _isListening.value = false
                            // Only report meaningful errors to user, ignore simple silence timeouts
                            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                onErrorCallback(msg)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListeningInternal = false
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestText = matches?.firstOrNull()?.trim()
                            if (!bestText.isNullOrBlank()) {
                                Log.d(TAG, "Speech recognized: $bestText")
                                onSpeechResult(bestText)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim()
                            if (!partial.isNullOrBlank()) {
                                onSpeechPartial(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    // Multi-lingual support: prefer Hindi + English
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                onErrorCallback("Failed to start voice listener: ${e.message}")
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up speech recognizer", e)
            } finally {
                speechRecognizer = null
                isListeningInternal = false
                _isListening.value = false
            }
        }
    }
}

class ArushiAudioPlayer(private val context: Context) {
    companion object {
        private const val TAG = "ArushiAudioPlayer"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private val playbackQueue: Queue<String> = LinkedList()
    private var isPlayingInternal = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var onPlaybackFinishedCallback: (() -> Unit)? = null

    fun playAudioBase64(base64Audio: String, onFinished: (() -> Unit)? = null) {
        onPlaybackFinishedCallback = onFinished
        playbackQueue.clear()
        playbackQueue.add(base64Audio)
        playNextInQueue()
    }

    private fun playNextInQueue() {
        val next = playbackQueue.poll()
        if (next == null) {
            isPlayingInternal = false
            _isPlaying.value = false
            releaseAudioFocus()
            onPlaybackFinishedCallback?.invoke()
            return
        }

        try {
            val audioBytes = Base64.decode(next, Base64.DEFAULT)
            if (audioBytes.isEmpty()) {
                playNextInQueue()
                return
            }

            requestAudioFocus()
            isPlayingInternal = true
            _isPlaying.value = true

            // Determine if bytes contain a container header (RIFF WAV or ID3 MP3)
            val isWavOrMp3 = isContainerFormat(audioBytes)

            if (isWavOrMp3) {
                playViaMediaPlayer(audioBytes)
            } else {
                playRawPcm(audioBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding or playing audio", e)
            playNextInQueue()
        }
    }

    private fun isContainerFormat(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        // RIFF header for WAV
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return true
        }
        // ID3 header for MP3
        if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        // MP3 frame sync (0xFF 0xFB/0xF3/0xF2)
        if ((bytes[0].toInt() and 0xFF) == 0xFF && ((bytes[1].toInt() and 0xE0) == 0xE0)) {
            return true
        }
        return false
    }

    private fun playViaMediaPlayer(audioBytes: ByteArray) {
        stopCurrentPlayers()
        try {
            val tempFile = File.createTempFile("arushi_audio", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    tempFile.delete()
                    playNextInQueue()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what extra=$extra")
                    tempFile.delete()
                    playNextInQueue()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed, falling back to PCM", e)
            playRawPcm(audioBytes)
        }
    }

    private fun playRawPcm(audioBytes: ByteArray) {
        stopCurrentPlayers()
        Thread {
            try {
                // Gemini TTS raw PCM is 24000Hz, 16-bit mono
                val sampleRate = 24000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = maxOf(minBufferSize, audioBytes.size)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()
                track.write(audioBytes, 0, audioBytes.size)

                // Wait for playback to complete
                val durationMs = (audioBytes.size * 1000L) / (sampleRate * 2)
                Thread.sleep(durationMs + 100)

                track.stop()
                track.release()
                audioTrack = null

                playNextInQueue()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing raw PCM", e)
                playNextInQueue()
            }
        }.start()
    }

    /**
     * Requirement #15: Speaker Diagnostic Test
     * Plays a pure 440Hz test tone through AudioTrack and device speaker.
     */
    fun play440HzDiagnosticTone(durationMs: Int = 1200, onComplete: (() -> Unit)? = null) {
        stopPlayback()
        requestAudioFocus()
        _isPlaying.value = true

        Thread {
            try {
                val sampleRate = 44100
                val numSamples = (durationMs * sampleRate) / 1000
                val pcmBuffer = ShortArray(numSamples)
                val freq = 440.0 // Standard concert A

                // Generate sine wave with soft fade-in & fade-out envelope to avoid clicks
                val fadeSamples = sampleRate / 20 // 50ms fade
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                    var envelope = 1.0
                    if (i < fadeSamples) {
                        envelope = i.toDouble() / fadeSamples
                    } else if (i > numSamples - fadeSamples) {
                        envelope = (numSamples - i).toDouble() / fadeSamples
                    }
                    val sample = (sin(angle) * 32767 * 0.8 * envelope).toInt()
                    pcmBuffer[i] = sample.toShort()
                }

                // Convert ShortArray to ByteArray
                val byteBuffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmBuffer) {
                    byteBuffer.putShort(s)
                }
                val rawBytes = byteBuffer.array()

                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, rawBytes.size))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                track.write(rawBytes, 0, rawBytes.size)
                Thread.sleep(durationMs.toLong() + 150)
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing 440Hz diagnostic tone", e)
            } finally {
                _isPlaying.value = false
                releaseAudioFocus()
                onComplete?.invoke()
            }
        }.start()
    }

    fun stopPlayback() {
        playbackQueue.clear()
        stopCurrentPlayers()
        isPlayingInternal = false
        _isPlaying.value = false
        releaseAudioFocus()
    }

    private fun stopCurrentPlayers() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            audioTrack = null
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopPlayback()
                    }
                }
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
