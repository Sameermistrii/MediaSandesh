package com.mediasandesh.news.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

object Library {
    const val MAX_HISTORY = 40

    private val gson = Gson()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private lateinit var dir: File

    var saved by mutableStateOf<List<WpPost>>(emptyList())
        private set
    var history by mutableStateOf<List<WpPost>>(emptyList())
        private set
    var readAt by mutableStateOf<Map<Int, Long>>(emptyMap())
        private set

    fun init(context: Context) {
        dir = File(context.filesDir, "library").apply { mkdirs() }
        saved = read("saved.json")
        history = read("history.json")
        readAt = runCatching {
            val file = File(dir, "read_times.json")
            if (!file.exists()) null
            else gson.fromJson<Map<Int, Long>>(file.readText(), object : TypeToken<Map<Int, Long>>() {}.type)
        }.getOrNull() ?: emptyMap()
    }

    fun isSaved(id: Int) = saved.any { it.id == id }

    fun find(id: Int): WpPost? = saved.find { it.id == id } ?: history.find { it.id == id }

    fun toggleSaved(post: WpPost) {
        saved = if (isSaved(post.id)) saved.filterNot { it.id == post.id } else listOf(post) + saved
        write("saved.json", saved)
    }

    fun markRead(post: WpPost) {
        history = (listOf(post) + history.filterNot { it.id == post.id }).take(MAX_HISTORY)
        val kept = history.map { it.id }.toSet()
        readAt = (readAt + (post.id to System.currentTimeMillis())).filterKeys { it in kept }
        write("history.json", history)
        writeTimes()
    }

    fun clearHistory() {
        history = emptyList()
        readAt = emptyMap()
        write("history.json", history)
        writeTimes()
    }

    private fun writeTimes() {
        val times = readAt
        io.launch { runCatching { File(dir, "read_times.json").writeText(gson.toJson(times)) } }
    }

    private fun read(name: String): List<WpPost> = runCatching {
        val file = File(dir, name)
        if (!file.exists()) return emptyList()
        gson.fromJson<List<WpPost>>(file.readText(), object : TypeToken<List<WpPost>>() {}.type) ?: emptyList()
    }.getOrDefault(emptyList())

    private fun write(name: String, posts: List<WpPost>) {
        io.launch { runCatching { File(dir, name).writeText(gson.toJson(posts)) } }
    }
}
