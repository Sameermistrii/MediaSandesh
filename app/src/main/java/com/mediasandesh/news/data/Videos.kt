package com.mediasandesh.news.data

import okhttp3.FormBody
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

data class VideoPage(val videos: List<VideoItem>, val nextToken: String?)

object Videos {
    private const val CHANNEL = "jGTEJjc-859tcF-7WPXeYg"
    private const val UPLOADS = "UU$CHANNEL"
    private const val SHORTS = "UUSH$CHANNEL"
    private const val GALLERY = "https://www.mediasandesh.com/wp-admin/admin-ajax.php"
    private const val FEED = "https://www.youtube.com/feeds/videos.xml"

    private val known = ConcurrentHashMap<String, VideoItem>()
    private val shortIds = ConcurrentHashMap.newKeySet<String>()

    fun find(id: String): VideoItem? = known[id]

    /** Latest uploads with views, dates and descriptions from the channel feed. */
    suspend fun recent(): List<VideoItem> = feed("channel_id=UC$CHANNEL")

    suspend fun shorts(): List<VideoItem> {
        val items = runCatching { feed("playlist_id=$SHORTS") }.getOrDefault(emptyList())
            .ifEmpty { galleryPage(SHORTS, "").videos }
            .map { it.copy(isShort = true) }
        items.forEach { shortIds += it.id; known[it.id] = it }
        return items
    }

    /** One page of every upload, 15 at a time, via the website's YouTube gallery. */
    suspend fun page(token: String): VideoPage {
        val page = galleryPage(UPLOADS, token)
        return page.copy(videos = page.videos.filter { it.id !in shortIds }.map { known[it.id] ?: it })
    }

    private fun feed(query: String): List<VideoItem> {
        val request = Request.Builder().url("$FEED?$query").build()
        val xml = Repository.http.newCall(request).execute().use { r ->
            if (!r.isSuccessful) error("Video feed ${r.code}")
            r.body!!.string()
        }
        return Regex("<entry>([\\s\\S]*?)</entry>").findAll(xml).map { m ->
            val e = m.groupValues[1]
            fun tag(name: String) = Regex("<$name[^>]*>([\\s\\S]*?)</$name>").find(e)?.groupValues?.get(1).orEmpty()
            val id = tag("yt:videoId")
            VideoItem(
                id = id,
                title = tag("title").decodeXml(),
                published = tag("published"),
                thumbnail = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                views = Regex("<media:statistics views=\"(\\d+)\"").find(e)?.groupValues?.get(1)?.toLongOrNull(),
                description = tag("media:description").decodeXml()
            )
        }.filter { it.id.isNotBlank() }.toList().onEach { known[it.id] = it }
    }

    private fun galleryPage(playlist: String, token: String): VideoPage {
        val form = FormBody.Builder()
            .add("action", "my_embedplus_gallery_page")
            .add("options[playlistId]", playlist)
            .add("options[pageToken]", token)
            .add("options[pageSize]", "15")
            .add("options[columns]", "3")
            .add("options[showTitle]", "1")
            .add("options[showPaging]", "1")
            .add("options[autonext]", "0")
            .add("options[thumbplay]", "1")
            .build()
        val saved = Repository.http.cache?.directory?.parentFile?.resolve("video_pages/${playlist}_${token.ifBlank { "first" }}.html")
        val html = try {
            Repository.http.newCall(Request.Builder().url(GALLERY).post(form).build()).execute().use { r ->
                if (!r.isSuccessful) error("Video gallery ${r.code}")
                r.body!!.string()
            }.also { body -> saved?.runCatching { parentFile?.mkdirs(); writeText(body) } }
        } catch (e: Exception) {
            saved?.takeIf { it.exists() }?.readText() ?: throw e
        }
        val videos = Regex("""data-videoid="([A-Za-z0-9_-]{11})"[\s\S]*?epyt-gallery-title">([^<]+)""")
            .findAll(html).map { m ->
                val id = m.groupValues[1]
                VideoItem(id, m.groupValues[2].decodeHtml(), "", "https://i.ytimg.com/vi/$id/hqdefault.jpg")
            }.distinctBy { it.id }.toList()
        videos.forEach { known.putIfAbsent(it.id, it) }
        val next = Regex("""epyt-next[^"]*"[^>]*data-pagetoken="([^"]*)"""").find(html)?.groupValues?.get(1)
        return VideoPage(videos, next?.ifBlank { null })
    }

    private fun String.decodeXml() = replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").trim()
}
