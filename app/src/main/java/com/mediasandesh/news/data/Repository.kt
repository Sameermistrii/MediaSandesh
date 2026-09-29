package com.mediasandesh.news.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object Repository {
    private const val SITE = "https://www.mediasandesh.com/wp-json/"
    const val FEATURED_CATEGORY = 79
    private const val UNCATEGORIZED = 1
    private val MERGED = mapOf(146 to 77)

    var offline by mutableStateOf(false)
        private set

    private lateinit var appContext: Context
    private lateinit var api: WpApi
    internal lateinit var http: OkHttpClient
        private set
    private val known = ConcurrentHashMap<Int, WpPost>()

    private val offlineCache = CacheControl.Builder()
        .onlyIfCached()
        .maxStale(30, TimeUnit.DAYS)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
        http = OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "news_http"), 30L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=120")
                    .build()
            }
            .addInterceptor { chain ->
                val request = chain.request()
                if (!isOnline()) {
                    offline = true
                    return@addInterceptor chain.proceed(request.newBuilder().cacheControl(offlineCache).build())
                }
                try {
                    chain.proceed(request).also { offline = false }
                } catch (e: IOException) {
                    val cached = chain.proceed(request.newBuilder().cacheControl(offlineCache).build())
                    if (!cached.isSuccessful) {
                        cached.close()
                        throw e
                    }
                    offline = true
                    cached
                }
            }
            .build()
        api = Retrofit.Builder()
            .baseUrl(SITE)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WpApi::class.java)
    }

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun List<WpPost>.remember(): List<WpPost> = onEach { known[it.id] = it }

    suspend fun latest(page: Int = 1) = api.posts(perPage = 10, page = page).remember()

    suspend fun byCategory(id: Int, page: Int = 1) =
        api.posts(perPage = 10, page = page, category = id).remember()

    suspend fun featured() = api.posts(perPage = 3, category = FEATURED_CATEGORY).remember()

    suspend fun search(query: String, page: Int = 1) =
        api.posts(perPage = 15, page = page, search = query).remember()

    suspend fun related(post: WpPost): List<WpPost> {
        val category = post.categories.firstOrNull { it != FEATURED_CATEGORY } ?: return emptyList()
        return api.posts(perPage = 6, category = category, exclude = post.id).remember().take(5)
    }

    suspend fun post(id: Int): WpPost {
        known[id]?.let { return it }
        return try {
            api.post(include = id).first().also { known[id] = it }
        } catch (e: Exception) {
            Library.find(id) ?: throw e
        }
    }

    @Volatile private var categoryCache: List<WpCategory>? = null
    @Volatile private var trendingCache: Pair<Long, List<WpCategory>>? = null

    suspend fun categories(): List<WpCategory> = categoryCache ?: api.categories().also { categoryCache = it }

    /** Topics with the most stories in the last 7 days, or in the latest 50 stories when the week is quiet. */
    suspend fun trending(): List<WpCategory> {
        trendingCache?.let { (at, list) -> if (System.currentTimeMillis() - at < 10 * 60_000) return list }
        val recent = api.recent()
        val weekAgo = LocalDateTime.now().minusDays(7)
        val thisWeek = recent.filter { p ->
            runCatching { LocalDateTime.parse(p.date).isAfter(weekAgo) }.getOrDefault(false)
        }
        val pool = if (thisWeek.size >= 8) thisWeek else recent
        val counts = pool.map { p -> p.categories.map { MERGED[it] ?: it }.toSet() }
            .flatten()
            .filter { it != FEATURED_CATEGORY && it != UNCATEGORIZED }
            .groupingBy { it }.eachCount()
        val byId = categories().associateBy { it.id }
        val result = counts.entries.sortedByDescending { it.value }
            .mapNotNull { (id, n) -> byId[id]?.copy(count = n) }
            .take(8)
        trendingCache = System.currentTimeMillis() to result
        return result
    }

}
