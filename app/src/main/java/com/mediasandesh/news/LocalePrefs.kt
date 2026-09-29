package com.mediasandesh.news

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocalePrefs {
    private const val FILE = "mediasandesh"
    private const val KEY = "lang"

    fun get(context: Context): String {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "hi") ?: "hi"
    }

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, lang)
            .apply()
        com.mediasandesh.news.widget.HeadlinesWidget.refresh(context)
    }
}

fun Context.withAppLocale(): Context {
    val locale = Locale.forLanguageTag(LocalePrefs.get(this))
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}
