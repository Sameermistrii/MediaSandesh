package com.mediasandesh.news.data

data class City(
    val id: Int,
    val hi: String,
    val en: String,
    val district: Int? = null,
    val lat: Double,
    val lon: Double
) {
    fun label(english: Boolean) = if (english) en else hi
}

object Cities {
    const val UDHAM_SINGH_NAGAR = 135
    const val UTTARAKHAND = 150

    val district = City(UDHAM_SINGH_NAGAR, "उधम सिंह नगर", "Udham Singh Nagar", lat = 28.98, lon = 79.40)
    val state = City(UTTARAKHAND, "उत्तराखण्ड", "Uttarakhand", lat = 30.32, lon = 78.03)

    val all = listOf(
        City(152, "रुद्रपुर", "Rudrapur", UDHAM_SINGH_NAGAR, 28.98, 79.40),
        City(159, "काशीपुर", "Kashipur", UDHAM_SINGH_NAGAR, 29.21, 78.96),
        City(165, "किच्छा", "Kichha", UDHAM_SINGH_NAGAR, 28.91, 79.52),
        City(164, "खटीमा", "Khatima", UDHAM_SINGH_NAGAR, 28.92, 79.97),
        City(163, "सितारगंज", "Sitarganj", UDHAM_SINGH_NAGAR, 28.93, 79.70),
        City(161, "गदरपुर", "Gadarpur", UDHAM_SINGH_NAGAR, 29.04, 79.25),
        City(162, "जसपुर", "Jaspur", UDHAM_SINGH_NAGAR, 29.28, 78.83),
        City(157, "दिनेशपुर", "Dineshpur", UDHAM_SINGH_NAGAR, 29.03, 79.33),
        City(384, "बाजपुर", "Bajpur", UDHAM_SINGH_NAGAR, 29.15, 79.12),
        City(166, "नानकमत्ता", "Nanakmatta", UDHAM_SINGH_NAGAR, 28.97, 79.83),
        City(220, "शक्ति फार्म", "Shaktifarm", UDHAM_SINGH_NAGAR, 28.87, 79.63),
        City(158, "हल्द्वानी", "Haldwani", lat = 29.22, lon = 79.51),
        City(160, "नैनीताल", "Nainital", lat = 29.38, lon = 79.46),
        City(170, "लाल कुआं", "Lalkuan", lat = 29.07, lon = 79.52),
        City(167, "देहरादून", "Dehradun", lat = 30.32, lon = 78.03),
        City(173, "हरिद्वार", "Haridwar", lat = 29.95, lon = 78.16),
        City(342, "रुड़की", "Roorkee", lat = 29.85, lon = 77.89),
        City(346, "टनकपुर", "Tanakpur", lat = 29.07, lon = 80.11),
        City(263, "चंपावत", "Champawat", lat = 29.34, lon = 80.09),
        City(168, "मसूरी", "Mussoorie", lat = 30.46, lon = 78.07)
    )

    fun find(id: Int) = all.find { it.id == id }

    /** The district a town belongs to, shown under its name. */
    fun districtOf(city: City, english: Boolean): String = when {
        city.district == UDHAM_SINGH_NAGAR -> district.label(english)
        city.id in setOf(158, 160, 170) -> if (english) "Nainital" else "नैनीताल"
        city.id in setOf(167, 168) -> if (english) "Dehradun" else "देहरादून"
        city.id in setOf(173, 342) -> if (english) "Haridwar" else "हरिद्वार"
        city.id in setOf(346, 263) -> if (english) "Champawat" else "चंपावत"
        else -> state.label(english)
    }
}
