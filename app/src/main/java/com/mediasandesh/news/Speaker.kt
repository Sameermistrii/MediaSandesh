package com.mediasandesh.news

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

object Speaker {
    private const val MAX_CHUNK = 600
    var playing by mutableStateOf<String?>(null)
        private set

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var lastUtterance: String? = null

    fun speak(context: Context, id: String, parts: List<String>) {
        val app = context.applicationContext
        val job = { start(app, id, parts) }
        if (ready) job() else {
            pending = job
            if (tts == null) tts = TextToSpeech(app) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) pending?.invoke() else toast(app, R.string.listen_unavailable)
                pending = null
            }
        }
    }

    fun toggle(context: Context, id: String, parts: () -> List<String>) {
        if (playing == id) stop() else speak(context, id, parts())
    }

    fun stop() {
        tts?.stop()
        playing = null
        lastUtterance = null
    }

    private fun start(context: Context, id: String, parts: List<String>) {
        val engine = tts ?: return
        val sample = parts.joinToString(" ").filter { it.isLetter() }
        val devanagari = sample.count { it in '\u0900'..'\u097F' }
        val hindi = LocalePrefs.get(context) == "hi" || (sample.isNotEmpty() && devanagari * 3 > sample.length)
        val locale = if (hindi) Locale("hi", "IN") else Locale.US
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            toast(context, if (hindi) R.string.listen_no_hindi else R.string.listen_unavailable)
            if (engine.setLanguage(Locale.ENGLISH) < 0) return
        }
        engine.setSpeechRate(0.95f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastUtterance) playing = null
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == lastUtterance) playing = null
            }
        })
        engine.stop()
        val chunks = parts.flatMap { chunk(it, TextToSpeech.getMaxSpeechInputLength().coerceAtMost(MAX_CHUNK)) }
            .filter { it.isNotBlank() }
        if (chunks.isEmpty()) return
        playing = id
        chunks.forEachIndexed { i, text ->
            val utterance = "$id-$i"
            if (i == chunks.lastIndex) lastUtterance = utterance
            engine.speak(text, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, utterance)
        }
    }

    private fun chunk(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (sentence in text.split(Regex("(?<=[।.!?])\\s+"))) {
            if (current.length + sentence.length + 1 > max && current.isNotEmpty()) {
                out += current.toString()
                current.clear()
            }
            if (sentence.length > max) sentence.chunked(max).forEach { out += it } else current.append(sentence).append(' ')
        }
        if (current.isNotBlank()) out += current.toString()
        return out
    }

    private fun toast(context: Context, res: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, context.getString(res), Toast.LENGTH_LONG).show()
        }
    }
}
