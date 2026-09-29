package com.mediasandesh.news.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object NewsTranslator {
    private val cache = ConcurrentHashMap<String, String>()
    private val modelLock = Mutex()
    private val translateGate = Semaphore(2)
    private var modelReady = false
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.HINDI)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    private val categoryEn = mapOf(
        "देश" to "Nation",
        "विदेश" to "World",
        "विज्ञानं" to "Science",
        "राजनीति" to "Politics",
        "क्राइम" to "Crime",
        "खेल" to "Sports",
        "उत्तरप्रदेश" to "Uttar Pradesh",
        "उत्तराखण्ड" to "Uttarakhand",
        "उधम सिंह नगर" to "Udham Singh Nagar",
        "रुद्रपुर" to "Rudrapur",
        "किसान" to "Farmers",
        "पर्यटन" to "Tourism",
        "शिक्षा" to "Education",
        "मनोरंजन" to "Entertainment",
        "स्वास्थ्य" to "Health",
        "संस्कृति" to "Culture",
        "फिल्म" to "Film",
        "पुलिस" to "Police",
        "अन्य" to "Others",
        "अपराध" to "Crime",
        "कलम से !" to "From the pen",
        "Featured" to "Featured",
        "अंतरराष्ट्रीय" to "International",
        "किच्छा" to "Kichha",
        "खटीमा" to "Khatima",
        "काशीपुर" to "Kashipur",
        "जसपुर" to "Jaspur",
        "दिनेशपुर" to "Dineshpur",
        "देहरादून" to "Dehradun",
        "गदरपुर" to "Gadarpur",
        "नैनीताल" to "Nainital",
        "सितारगंज" to "Sitarganj",
        "टनकपुर" to "Tanakpur",
        "नानकमत्ता" to "Nanakmatta",
        "शक्ति फार्म" to "Shakti Farm",
        "हरिद्वार" to "Haridwar",
        "दिल्ली" to "Delhi",
        "हल्द्वानी" to "Haldwani",
        "मौसम" to "Weather",
        "बाजपुर" to "Bajpur",
        "लाल कुआं" to "Lalkuan",
        "रुड़की" to "Roorkee",
        "चंपावत" to "Champawat",
        "मसूरी" to "Mussoorie",
        "राम मंदिर" to "Ram Mandir",
        "बरेली" to "Bareilly",
        "लखनऊ" to "Lucknow",
        "जम्मू कश्मीर" to "Jammu & Kashmir",
        "केदारनाथ" to "Kedarnath"
    )

    suspend fun postsTitles(items: List<WpPost>): List<WpPost> {
        val titles = texts(items.map { it.headline })
        return items.mapIndexed { i, post ->
            post.copy(
                title = Rendered(titles[i]),
                excerpt = Rendered("")
            )
        }
    }

    suspend fun postFull(post: WpPost): WpPost {
        val (title, summary) = texts(listOf(post.headline, post.summary))
        return post.copy(
            title = Rendered(title),
            excerpt = Rendered(summary),
            content = Rendered(html(post.content.rendered))
        )
    }

    suspend fun categories(items: List<WpCategory>): List<WpCategory> {
        val unknown = items.map { it.name }.filter { it !in categoryEn }
        val translated = unknown.zip(texts(unknown)).toMap()
        return items.map { cat -> cat.copy(name = categoryEn[cat.name] ?: translated[cat.name] ?: cat.name) }
    }

    fun categoryKnown(name: String): String = categoryEn[name] ?: name

    suspend fun text(value: String): String = texts(listOf(value)).first()

    /** Translates many strings at once: Google Translate online, the on-device model when that fails. */
    suspend fun texts(values: List<String>): List<String> {
        loadSaved()
        val pending = values.map { it.trim() }
            .filter { it.length >= 2 && it.any { c -> c.code > 127 } && cache[it] == null }
            .distinct()
        if (pending.isNotEmpty()) {
            val online = runCatching { withContext(Dispatchers.IO) { GoogleTranslate.batch(pending) } }.getOrNull()
            if (online != null) {
                pending.zip(online).forEach { (src, out) -> cache[src] = out }
                save(pending.zip(online))
            } else {
                pending.forEach { src -> onDevice(src)?.let { cache[src] = it } }
            }
        }
        return values.map { cache[it.trim()] ?: it }
    }

    private suspend fun onDevice(source: String): String? = try {
        translateGate.withPermit {
            ensureModel()
            buildString {
                for (part in chunks(source)) {
                    if (isNotEmpty()) append(' ')
                    append(translator.translate(part).await())
                }
            }
        }
    } catch (_: Exception) {
        null
    }

    suspend fun html(value: String): String {
        val clean = value.replace(Regex("(?is)<(script|style).*?>.*?</\\1>"), "")
        val matcher = Regex(">([^<]+)<")
        val matches = matcher.findAll(clean).toList()
        if (matches.isEmpty()) return text(clean.decodeHtml())
        val plains = matches.map { it.groupValues[1].decodeHtml().trim() }
        val needs = plains.filter { it.length > 1 && it.any { c -> c.code > 127 } }
        val translated = needs.zip(texts(needs)).toMap()
        val out = StringBuilder()
        var index = 0
        matches.forEachIndexed { i, match ->
            out.append(clean, index, match.range.first + 1)
            out.append(translated[plains[i]]?.let { escape(it) } ?: match.groupValues[1])
            index = match.range.last
        }
        out.append(clean.substring(index))
        return out.toString()
    }

    private val savedFile by lazy { Repository.http.cache?.directory?.parentFile?.resolve("translations.jsonl") }
    private val saveLock = Any()
    @Volatile private var savedLoaded = false

    private fun loadSaved() {
        if (savedLoaded) return
        synchronized(saveLock) {
            if (savedLoaded) return
            savedFile?.takeIf { it.exists() }?.let { file ->
                if (file.length() > 4L * 1024 * 1024) file.delete()
                else runCatching {
                    file.forEachLine { line ->
                        runCatching { JSONObject(line) }.getOrNull()?.let { cache.putIfAbsent(it.getString("s"), it.getString("t")) }
                    }
                }
            }
            savedLoaded = true
        }
    }

    private fun save(pairs: List<Pair<String, String>>) {
        val file = savedFile ?: return
        synchronized(saveLock) {
            runCatching {
                file.appendText(pairs.joinToString("") { (s, t) -> JSONObject().put("s", s).put("t", t).toString() + "\n" })
            }
        }
    }

    private suspend fun ensureModel() {
        if (modelReady) return
        modelLock.withLock {
            if (modelReady) return
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            modelReady = true
        }
    }

    private fun chunks(value: String): List<String> {
        if (value.length <= 1800) return listOf(value)
        val parts = mutableListOf<String>()
        var rest = value
        while (rest.length > 1800) {
            val cut = rest.lastIndexOf(' ', 1800).takeIf { it > 400 } ?: 1800
            parts.add(rest.substring(0, cut))
            rest = rest.substring(cut).trimStart()
        }
        if (rest.isNotBlank()) parts.add(rest)
        return parts
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
