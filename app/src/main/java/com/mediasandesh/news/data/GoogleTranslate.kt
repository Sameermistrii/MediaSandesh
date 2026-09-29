package com.mediasandesh.news.data

import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray

/** Hindi to English through Google Translate's web endpoint (the one translate.google.com uses). */
internal object GoogleTranslate {
    private const val URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=hi&tl=en&dt=t"
    private const val MAX_CHARS = 3500
    private const val SEPARATOR = "\n\n"

    /** Returns one translation per input, in order, sending as few requests as possible. */
    fun batch(texts: List<String>): List<String> {
        val out = ArrayList<String>(texts.size)
        var group = mutableListOf<String>()
        var size = 0
        fun flush() {
            if (group.isNotEmpty()) out += translateGroup(group)
            group = mutableListOf()
            size = 0
        }
        for (text in texts) {
            if (text.length > MAX_CHARS) {
                flush()
                out += splitLong(text).joinToString(" ") { translate(it) }
                continue
            }
            if (size + text.length + SEPARATOR.length > MAX_CHARS) flush()
            group += text
            size += text.length + SEPARATOR.length
        }
        flush()
        return out
    }

    private fun translateGroup(group: List<String>): List<String> {
        if (group.size == 1) return listOf(translate(group[0]))
        val clean = group.map { it.replace(Regex("\\s*\\n+\\s*"), " ") }
        val parts = translate(clean.joinToString(SEPARATOR)).split(Regex("\\n\\s*\\n")).map { it.trim() }
        return if (parts.size == group.size) parts else clean.map { translate(it) }
    }

    private fun translate(text: String): String {
        val request = Request.Builder().url(URL).post(FormBody.Builder().add("q", text).build()).build()
        val body = Repository.http.newCall(request).execute().use { res ->
            if (!res.isSuccessful) error("Translate ${res.code}")
            res.body!!.string()
        }
        val sentences = JSONArray(body).getJSONArray(0)
        return buildString {
            for (i in 0 until sentences.length()) {
                sentences.optJSONArray(i)?.optString(0)?.takeIf { it != "null" }?.let { append(it) }
            }
        }.trim()
    }

    private fun splitLong(text: String): List<String> {
        val parts = mutableListOf<String>()
        var rest = text
        while (rest.length > MAX_CHARS) {
            val cut = rest.lastIndexOf('।', MAX_CHARS).takeIf { it > 500 }?.plus(1)
                ?: rest.lastIndexOf(' ', MAX_CHARS).takeIf { it > 500 }
                ?: MAX_CHARS
            parts += rest.substring(0, cut).trim()
            rest = rest.substring(cut)
        }
        if (rest.isNotBlank()) parts += rest.trim()
        return parts
    }
}
