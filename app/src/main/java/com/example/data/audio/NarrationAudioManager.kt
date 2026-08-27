package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.random.Random

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val progress: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val visualizerAmplitudes: List<Float> = List(16) { 0.1f },
    val activeSentenceIndex: Int = 0
)

class NarrationAudioManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentScript: String = ""
    private var scriptSentences: List<String> = emptyList()

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("NarrationAudio", "Failed to init TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _playerState.value = _playerState.value.copy(isPlaying = true)
                    startVisualizerProgress()
                }

                override fun onDone(utteranceId: String?) {
                    _playerState.value = _playerState.value.copy(isPlaying = false, progress = 1f)
                    stopVisualizerProgress()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _playerState.value = _playerState.value.copy(isPlaying = false)
                    stopVisualizerProgress()
                }
            })
        }
    }

    fun playClip(
        script: String,
        audioBase64: String?,
        audioMimeType: String? = "audio/wav",
        speed: Float = 1.0f
    ) {
        stop()
        currentScript = script
        scriptSentences = script.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }

        if (!audioBase64.isNullOrBlank()) {
            try {
                val tempFile = writeBase64AudioToTempFile(audioBase64, audioMimeType)
                playWithMediaPlayer(tempFile, speed)
                return
            } catch (e: Exception) {
                Log.e("NarrationAudio", "Failed to play base64 audio, falling back to TTS engine", e)
            }
        }

        // Fallback to Android TTS
        playWithTts(script, speed)
    }

    private fun playWithMediaPlayer(file: File, speed: Float) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply { this.speed = speed }
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration,
                        progress = 1.0f
                    )
                    stopVisualizerProgress()
                }
                start()
            }

            val duration = mediaPlayer?.duration ?: 1
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                durationMs = duration,
                currentPositionMs = 0,
                progress = 0f,
                playbackSpeed = speed
            )
            startVisualizerProgress()
        } catch (e: Exception) {
            Log.e("NarrationAudio", "MediaPlayer error", e)
            playWithTts(currentScript, speed)
        }
    }

    private fun playWithTts(text: String, speed: Float) {
        if (text.isBlank()) return
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.setSpeechRate(speed)
            val estimatedDurationMs = (text.split(" ").size / (2.5f * speed) * 1000).toInt().coerceAtLeast(5000)
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                durationMs = estimatedDurationMs,
                currentPositionMs = 0,
                progress = 0f,
                playbackSpeed = speed
            )
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ar_tour_${System.currentTimeMillis()}")
            startVisualizerProgress()
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
                stopVisualizerProgress()
            }
        } ?: run {
            textToSpeech?.stop()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopVisualizerProgress()
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startVisualizerProgress()
        } ?: run {
            playWithTts(currentScript, _playerState.value.playbackSpeed)
        }
    }

    fun seekTo(progress: Float) {
        val targetProgress = progress.coerceIn(0f, 1f)
        mediaPlayer?.let {
            val targetMs = (it.duration * targetProgress).toInt()
            it.seekTo(targetMs)
            _playerState.value = _playerState.value.copy(
                currentPositionMs = targetMs,
                progress = targetProgress
            )
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.playbackParams = PlaybackParams().apply { this.speed = speed }
                }
            }
        }
        textToSpeech?.setSpeechRate(speed)
    }

    fun stop() {
        stopVisualizerProgress()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        textToSpeech?.stop()
        _playerState.value = _playerState.value.copy(isPlaying = false, progress = 0f, currentPositionMs = 0)
    }

    private fun startVisualizerProgress() {
        stopVisualizerProgress()
        progressJob = scope.launch {
            while (isActive && _playerState.value.isPlaying) {
                val currentMs = mediaPlayer?.currentPosition ?: (_playerState.value.currentPositionMs + 100)
                val durationMs = (_playerState.value.durationMs).coerceAtLeast(1)
                val progress = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                val sentenceCount = scriptSentences.size.coerceAtLeast(1)
                val activeIndex = (progress * sentenceCount).toInt().coerceIn(0, sentenceCount - 1)

                // Generate dynamic realistic audio wave visualizer amplitudes
                val baseAmps = List(16) {
                    val rnd = Random.nextFloat()
                    val wave = kotlin.math.sin((System.currentTimeMillis() / 150.0 + it).toFloat()) * 0.4f + 0.5f
                    (wave * rnd * 0.8f + 0.15f).coerceIn(0.1f, 1.0f)
                }

                _playerState.value = _playerState.value.copy(
                    currentPositionMs = currentMs,
                    progress = progress,
                    visualizerAmplitudes = baseAmps,
                    activeSentenceIndex = activeIndex
                )

                delay(80)
            }
        }
    }

    private fun stopVisualizerProgress() {
        progressJob?.cancel()
        progressJob = null
        _playerState.value = _playerState.value.copy(
            visualizerAmplitudes = List(16) { 0.1f }
        )
    }

    private fun writeBase64AudioToTempFile(base64Data: String, mimeType: String?): File {
        val extension = if (mimeType?.contains("mp3") == true) ".mp3" else ".wav"
        val tempFile = File(context.cacheDir, "gemini_ar_narration_$extension")
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        FileOutputStream(tempFile).use { fos ->
            fos.write(bytes)
            fos.flush()
        }
        return tempFile
    }

    fun release() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
