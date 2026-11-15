package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Clean provider abstraction separating speech recognition engines.
 * Enables Android SpeechRecognizer and System Speech Intent to work
 * 100% offline without requiring a paid Google Cloud Speech API key.
 */
interface SpeechRecognitionProvider {
    val isAvailable: Boolean
    val audioLevel: StateFlow<Float>

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
    fun destroy()
}

/**
 * Native Android SpeechRecognizer implementation targeting Bengali (bn-BD).
 */
class AndroidSpeechRecognizerProvider(
    private val context: Context
) : SpeechRecognitionProvider, RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var resultCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    private val _audioLevel = MutableStateFlow(0f)
    override val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        this.resultCallback = onResult
        this.errorCallback = onError

        if (!isAvailable) {
            onError("ডিভাইসে সরাসরি স্পিচ রিকগনিশন সার্ভিস সক্রিয় নেই। সিস্টেম ভয়েস ডায়ালগ ব্যবহার করুন।")
            return
        }

        try {
            stopListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@AndroidSpeechRecognizerProvider)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "bn", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("ভয়েস চালু করতে সমস্যা: ${e.localizedMessage}")
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        _audioLevel.value = 0f
    }

    override fun destroy() {
        stopListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {
        _audioLevel.value = 0.15f
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
        _audioLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _audioLevel.value = 0f
    }

    override fun onError(error: Int) {
        _audioLevel.value = 0f
        val errorMsg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "কথা স্পষ্ট শোনা যায়নি, অনুগ্রহ করে পুনরায় বলুন অথবা গুগল ভয়েস ডায়ালগ চাপুন"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ শোনা যায়নি, পুনরায় চেষ্টা করুন"
            SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগে সমস্যা হয়েছে"
            SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোন অডিও সংগ্রহে ত্রুটি"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত রয়েছে"
            SpeechRecognizer.ERROR_SERVER -> "সার্ভার থেকে সাড়া পাওয়া যায়নি, নিচে গুগল ভয়েস ডায়ালগ চাপুন"
            else -> "ভয়েস রিকগনিশনে সমস্যা হয়েছে (কোড: $error)"
        }
        errorCallback?.invoke(errorMsg)
    }

    override fun onResults(results: Bundle?) {
        _audioLevel.value = 0f
        val spokenMatches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!spokenMatches.isNullOrEmpty()) {
            resultCallback?.invoke(spokenMatches[0])
        } else {
            errorCallback?.invoke("কোনো কথা চিহ্নিত করা যায়নি")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
