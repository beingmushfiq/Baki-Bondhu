package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Service providing high-accuracy Bangla speech recognition on real Android devices
 * using Google Cloud Speech-to-Text API v1 and Google Cloud AI.
 * Captures 16kHz 16-bit Mono PCM audio directly via AudioRecord.
 */
class GoogleCloudSpeechService(
    private val context: Context
) {
    companion object {
        private const val TAG = "CloudSpeechService"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isRecording = false

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private var recordedPcmStream = ByteArrayOutputStream()

    fun isRecordingActive(): Boolean = isRecording

    /**
     * Resolves the best available Google Cloud API key for speech recognition.
     */
    fun resolveApiKey(): String {
        val cloudKey = try {
            BuildConfig.GOOGLE_CLOUD_SPEECH_API_KEY
        } catch (_: Throwable) {
            ""
        }
        if (cloudKey.isNotBlank() && !cloudKey.startsWith("MY_")) {
            return cloudKey
        }

        val geminiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }
        if (geminiKey.isNotBlank() && !geminiKey.startsWith("MY_")) {
            return geminiKey
        }

        return ""
    }

    /**
     * Starts capturing hardware microphone audio at 16kHz 16-bit Mono PCM.
     */
    fun startRecording(
        onError: (String) -> Unit = {}
    ): Boolean {
        if (isRecording) {
            return true
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError("মাইক্রোফোন ব্যবহারের অনুমতি নেই (Audio Permission Missing)")
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("অডিও হার্ডওয়্যার প্রস্তুত করতে ব্যর্থ হয়েছে")
            return false
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                onError("মাইক্রোফোন ইনিশিয়ালাইজ করা যায়নি। ডিভাইসে অন্য কোনো অ্যাপ মাইক ব্যবহার করছে কিনা দেখুন।")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true
            recordedPcmStream.reset()
            _recordingDurationSeconds.value = 0

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)
                var totalSamples = 0L
                val startTime = System.currentTimeMillis()

                try {
                    while (isActive && isRecording) {
                        val shortsRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (shortsRead > 0) {
                            // Compute RMS for live waveform
                            var sumOfSquares = 0.0
                            for (i in 0 until shortsRead) {
                                val sample = buffer[i].toDouble()
                                sumOfSquares += sample * sample

                                // Convert short to little-endian bytes
                                byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                            }

                            val rms = sqrt(sumOfSquares / shortsRead)
                            val normalized = (rms / 3500.0).coerceIn(0.05, 1.0).toFloat()
                            _audioLevel.value = normalized

                            recordedPcmStream.write(byteBuffer, 0, shortsRead * 2)
                            totalSamples += shortsRead

                            val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                            _recordingDurationSeconds.value = elapsedSeconds

                            // Automatically stop if user speaks for 15 seconds
                            if (elapsedSeconds >= 15) {
                                break
                            }
                        } else if (shortsRead == AudioRecord.ERROR_INVALID_OPERATION || shortsRead == AudioRecord.ERROR_BAD_VALUE) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Audio recording loop error", e)
                } finally {
                    _audioLevel.value = 0f
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            onError("মাইক্রোফোন চালু করতে সমস্যা: ${e.localizedMessage}")
            return false
        }
    }

    /**
     * Stops audio recording and returns raw 16kHz PCM bytes.
     */
    fun stopRecording(): ByteArray {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordingJob?.cancel()
        _audioLevel.value = 0f

        return recordedPcmStream.toByteArray()
    }

    /**
     * Transcribes recorded audio using Google Cloud Speech-to-Text API v1,
     * with fallback to Gemini multimodal speech transcription if needed.
     */
    suspend fun transcribeAudio(
        pcmData: ByteArray,
        onProgress: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (pcmData.isEmpty() || pcmData.size < 4000) {
            return@withContext Result.failure(
                IllegalArgumentException("কোনো অডিও রেকর্ড হয়নি, পুনরায় স্পষ্ট করে বলুন")
            )
        }

        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "গুগল ক্লাউড স্পিচ এপিআই কী সেট করা নেই। নিচে 'গুগল ভয়েস ডায়ালগ' ব্যবহার করুন অথবা AI Studio Secrets প্যানেলে GOOGLE_CLOUD_SPEECH_API_KEY সেট করুন।"
                )
            )
        }

        onProgress("গুগল ক্লাউড স্পিচ দিয়ে নির্ভুলভাবে বিশ্লেষণ করা হচ্ছে...")

        // Attempt 1: Google Cloud Speech-to-Text v1 Recognize API
        val sttResult = runCatching {
            callGoogleCloudSpeechApi(pcmData, apiKey)
        }

        if (sttResult.isSuccess && !sttResult.getOrNull().isNullOrBlank()) {
            return@withContext Result.success(sttResult.getOrThrow())
        }

        // Attempt 2: Gemini 2.5 Flash / 1.5 Flash Multimodal Audio Transcription
        onProgress("গুগল এআই দিয়ে বাংলা উচ্চারণ বিশ্লেষণ করা হচ্ছে...")
        val geminiResult = runCatching {
            callGeminiAudioTranscription(pcmData, apiKey)
        }

        if (geminiResult.isSuccess && !geminiResult.getOrNull().isNullOrBlank()) {
            return@withContext Result.success(geminiResult.getOrThrow())
        }

        val errorReason = sttResult.exceptionOrNull()?.message
            ?: geminiResult.exceptionOrNull()?.message
            ?: "কথা বোঝা যায়নি"

        return@withContext Result.failure(
            Exception("ভয়েস চিহ্নিত করতে সমস্যা: $errorReason। গুগল ভয়েস ডায়ালগ ব্যবহার করে দেখতে পারেন।")
        )
    }

    /**
     * Direct call to Google Cloud Speech-to-Text v1 REST endpoint.
     */
    private fun callGoogleCloudSpeechApi(pcmData: ByteArray, apiKey: String): String {
        val base64Audio = Base64.encodeToString(pcmData, Base64.NO_WRAP)

        val requestJson = JSONObject().apply {
            put("config", JSONObject().apply {
                put("encoding", "LINEAR16")
                put("sampleRateHertz", SAMPLE_RATE)
                put("languageCode", "bn-BD")
                put("alternativeLanguageCodes", JSONArray().apply {
                    put("bn-IN")
                    put("bn")
                })
                put("enableAutomaticPunctuation", true)
                put("model", "default")
                put("speechContexts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("phrases", JSONArray().apply {
                            put("বাকি")
                            put("বাকী")
                            put("জমা")
                            put("পরিশোধ")
                            put("শোধ")
                            put("টাকা")
                            put("নগদ")
                            put("ধার")
                            put("একশো")
                            put("দুইশো")
                            put("তিনশো")
                            put("চারশো")
                            put("পাঁচশো")
                            put("হাজার")
                            put("দেড়শো")
                            put("আড়াইশো")
                        })
                    })
                })
            })
            put("audio", JSONObject().apply {
                put("content", base64Audio)
            })
        }

        val url = "https://speech.googleapis.com/v1/speech:recognize?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
            } catch (_: Exception) {
                "HTTP ${response.code}"
            }
            throw RuntimeException("Google Cloud Speech Error: $errorMsg")
        }

        val jsonResponse = JSONObject(responseBody)
        val resultsArray = jsonResponse.optJSONArray("results")
        if (resultsArray != null && resultsArray.length() > 0) {
            val stringBuilder = StringBuilder()
            for (i in 0 until resultsArray.length()) {
                val resultObj = resultsArray.getJSONObject(i)
                val alternatives = resultObj.optJSONArray("alternatives")
                if (alternatives != null && alternatives.length() > 0) {
                    val transcript = alternatives.getJSONObject(0).optString("transcript")
                    if (transcript.isNotBlank()) {
                        if (stringBuilder.isNotEmpty()) stringBuilder.append(" ")
                        stringBuilder.append(transcript.trim())
                    }
                }
            }
            if (stringBuilder.isNotBlank()) {
                return stringBuilder.toString()
            }
        }

        throw RuntimeException("কোনো বাংলা শব্দ চিহ্নিত করা যায়নি")
    }

    /**
     * Multimodal Gemini Audio Transcription fallback with RIFF/WAVE audio encapsulation.
     */
    private fun callGeminiAudioTranscription(pcmData: ByteArray, apiKey: String): String {
        val wavData = wrapPcmWithWavHeader(pcmData, SAMPLE_RATE, 1, 16)
        val base64Wav = Base64.encodeToString(wavData, Base64.NO_WRAP)

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/wav")
                                put("data", base64Wav)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Listen to this Bangla speech for a neighborhood shop credit/baki ledger entry. Transcribe the exact Bangla words spoken (such as 'রহিম ৪৫০ বাকী' or 'করিম ভাই ২০০ জমা'). Return ONLY the Bengali text without English, notes, explanations, or quotes.")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
            })
        }

        // Try gemini-2.5-flash first, then gemini-1.5-flash
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        var lastError: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text").trim()
                            if (text.isNotBlank()) {
                                return text.replace("\"", "").replace("`", "").trim()
                            }
                        }
                    }
                } else {
                    lastError = RuntimeException("Model $model returned HTTP ${response.code}")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw (lastError ?: RuntimeException("AI অডিও বিশ্লেষণ ব্যর্থ হয়েছে"))
    }

    /**
     * Converts raw PCM bytes into a standard 44-byte RIFF/WAVE container.
     */
    private fun wrapPcmWithWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header + pcmData
    }

    fun release() {
        stopRecording()
    }
}
