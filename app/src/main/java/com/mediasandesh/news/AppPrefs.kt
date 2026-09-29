package com.mediasandesh.news

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppPrefs {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val FILE = "mediasandesh"
    private const val MAX_RECENT = 8

    var theme by mutableIntStateOf(THEME_SYSTEM)
        private set
    var textSize by mutableIntStateOf(1)
        private set
    var recentSearches by mutableStateOf<List<String>>(emptyList())
        private set
    var cityId by mutableIntStateOf(0)
        private set

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        theme = p.getInt("theme", THEME_SYSTEM)
        textSize = p.getInt("text_size", 1)
        recentSearches = p.getString("recent_searches", "")!!
            .split('\n').filter { it.isNotBlank() }
        cityId = p.getInt("city_id", 0)
    }

    fun setCity(context: Context, id: Int) {
        cityId = id
        prefs(context).edit().putInt("city_id", id).apply()
    }

    fun setTheme(context: Context, value: Int) {
        theme = value
        prefs(context).edit().putInt("theme", value).apply()
    }

    fun setTextSize(context: Context, value: Int) {
        textSize = value.coerceIn(0, 3)
        prefs(context).edit().putInt("text_size", textSize).apply()
    }

    fun addSearch(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        recentSearches = (listOf(q) + recentSearches.filterNot { it.equals(q, ignoreCase = true) }).take(MAX_RECENT)
        prefs(context).edit().putString("recent_searches", recentSearches.joinToString("\n")).apply()
    }

    fun removeSearch(context: Context, query: String) {
        recentSearches = recentSearches.filterNot { it == query }
        prefs(context).edit().putString("recent_searches", recentSearches.joinToString("\n")).apply()
    }

    fun clearSearches(context: Context) {
        recentSearches = emptyList()
        prefs(context).edit().remove("recent_searches").apply()
    }
}
