package com.mediasandesh.news.data

import com.google.gson.annotations.SerializedName

data class Rendered(val rendered: String = "")

data class WpPost(
    val id: Int,
    val date: String = "",
    val link: String = "",
    val title: Rendered = Rendered(),
    val excerpt: Rendered = Rendered(),
    val content: Rendered = Rendered(),
    val categories: List<Int> = emptyList(),
    @SerializedName("_embedded") val embedded: Embedded? = null
) {
    val headline: String get() = title.rendered.decodeHtml()
    val summary: String get() = excerpt.rendered.decodeHtml()
    val authorName: String get() = embedded?.authors?.firstOrNull()?.name ?: "मीडिया संदेश"
    val imageUrl: String? get() = embedded?.media?.firstOrNull()?.sourceUrl
}

data class Embedded(
    val author: List<WpAuthor>? = null,
    @SerializedName("wp:featuredmedia") val media: List<WpMedia>? = null
) {
    val authors: List<WpAuthor> get() = author ?: emptyList()
}

data class WpAuthor(val name: String = "")

data class WpMedia(@SerializedName("source_url") val sourceUrl: String? = null)

data class WpCategory(
    val id: Int,
    val name: String = "",
    val count: Int = 0,
    val parent: Int = 0
)

data class VideoItem(
    val id: String,
    val title: String,
    val published: String,
    val thumbnail: String,
    val views: Long? = null,
    val description: String = "",
    val isShort: Boolean = false
)

fun String.decodeHtml(): String {
    return this
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#8217;", "’")
        .replace("&#8216;", "‘")
        .replace("&#8220;", "“")
        .replace("&#8221;", "”")
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&#8230;", "…")
        .replace(Regex("&#(\\d{1,7});")) { m ->
            m.groupValues[1].toInt()
                .takeIf { Character.isValidCodePoint(it) && it !in 0xD800..0xDFFF }
                ?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
}
