package com.mediasandesh.news.data

import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class DayForecast(val date: String, val code: Int, val max: Int, val min: Int, val rainChance: Int)

data class WeatherNow(
    val temp: Int,
    val feelsLike: Int,
    val humidity: Int,
    val wind: Int,
    val code: Int,
    val isDay: Boolean,
    val days: List<DayForecast>
)

object Weather {
    private const val TTL = 15 * 60 * 1000L
    private val cache = ConcurrentHashMap<Int, Pair<Long, WeatherNow>>()

    fun cached(city: City) = cache[city.id]?.second

    fun fetchedAt(city: City) = cache[city.id]?.first

    fun get(city: City): WeatherNow {
        cache[city.id]?.let { (at, w) -> if (System.currentTimeMillis() - at < TTL) return w }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=Asia%2FKolkata&forecast_days=3"
        val body = Repository.http.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Weather ${res.code}")
            res.body!!.string()
        }
        val json = JSONObject(body)
        val now = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val days = (0 until dates.length()).map { i ->
            DayForecast(
                date = dates.getString(i),
                code = daily.getJSONArray("weather_code").optInt(i),
                max = Math.round(daily.getJSONArray("temperature_2m_max").optDouble(i)).toInt(),
                min = Math.round(daily.getJSONArray("temperature_2m_min").optDouble(i)).toInt(),
                rainChance = daily.getJSONArray("precipitation_probability_max").optInt(i)
            )
        }
        val weather = WeatherNow(
            temp = Math.round(now.getDouble("temperature_2m")).toInt(),
            feelsLike = Math.round(now.getDouble("apparent_temperature")).toInt(),
            humidity = now.getInt("relative_humidity_2m"),
            wind = Math.round(now.getDouble("wind_speed_10m")).toInt(),
            code = now.getInt("weather_code"),
            isDay = now.optInt("is_day", 1) == 1,
            days = days
        )
        cache[city.id] = System.currentTimeMillis() to weather
        return weather
    }

    fun icon(code: Int, isDay: Boolean = true) = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌤️"
    }

    fun condition(code: Int, english: Boolean) = when (code) {
        0 -> if (english) "Clear sky" else "साफ़ आसमान"
        1 -> if (english) "Mostly clear" else "ज़्यादातर साफ़"
        2 -> if (english) "Partly cloudy" else "आंशिक बादल"
        3 -> if (english) "Cloudy" else "बादल छाए"
        45, 48 -> if (english) "Fog" else "कोहरा"
        in 51..57 -> if (english) "Drizzle" else "बूंदाबांदी"
        in 61..65 -> if (english) "Rain" else "बारिश"
        66, 67 -> if (english) "Freezing rain" else "ठंडी बारिश"
        in 71..77, 85, 86 -> if (english) "Snow" else "बर्फ़बारी"
        in 80..82 -> if (english) "Rain showers" else "बौछारें"
        in 95..99 -> if (english) "Thunderstorm" else "आंधी-तूफ़ान"
        else -> if (english) "Weather" else "मौसम"
    }
}
