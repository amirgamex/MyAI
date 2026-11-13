package com.myai.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * مدیریت تبدیل گفتار به متن و متن به گفتار
 */
class SpeechHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private val persianLocale = Locale("fa", "IR")

    fun initializeTts(onReady: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(persianLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // اگر فارسی پشتیبانی نشد، انگلیسی را امتحان کن
                    tts?.setLanguage(Locale.US)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
                onReady(true)
            } else {
                onReady(false)
            }
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("تشخیص گفتار روی این دستگاه پشتیبانی نمی‌شود")
            return
        }

        onResultCallback = onResult
        onErrorCallback = onError

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        _isListening.value = true
        _partialResult.value = ""
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun cancelListening() {
        speechRecognizer?.cancel()
        _isListening.value = false
        _partialResult.value = ""
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "myai_utterance")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "خطای صوتی"
                SpeechRecognizer.ERROR_CLIENT -> "خطای کلاینت"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشده"
                SpeechRecognizer.ERROR_NETWORK -> "خطای شبکه"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "زمان شبکه تمام شد"
                SpeechRecognizer.ERROR_NO_MATCH -> "گفتاری تشخیص داده نشد"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سیستم مشغول است"
                SpeechRecognizer.ERROR_SERVER -> "خطای سرور"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "زمان صحبت تمام شد"
                else -> "خطای ناشناخته ($error)"
            }
            onErrorCallback?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                onResultCallback?.invoke(text)
            } else {
                onErrorCallback?.invoke("متن خالی دریافت شد")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            _partialResult.value = matches?.firstOrNull().orEmpty()
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
