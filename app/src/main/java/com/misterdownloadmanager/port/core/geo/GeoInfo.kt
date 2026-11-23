package com.misterdownloadmanager.port.core.geo

import java.util.Locale

data class Country(val code: String, val name: String) {
    fun flagAssetName(): String = "flags/${code.lowercase()}.svg"
}

object GeoInfo {
    const val ATTRIBUTION: String = GeoDatabaseFormat.ATTRIBUTION
    const val ATTRIBUTION_URL: String = GeoDatabaseFormat.ATTRIBUTION_URL

    private var database: GeoDatabase? = null

    fun loadDatabase(bytes: ByteArray): Boolean {
        database = GeoDatabase.load(bytes)
        return database != null
    }

    fun countryForIp(ip: String): String? {
        return database?.country(ip)
    }

    fun flagCell(countryIndex: Int): Pair<Int, Int>? {
        return GeoDatabaseFormat.flagCell(countryIndex)
    }

    fun allCountries(): List<Country> {
        return Locale.getISOCountries().map { code ->
            val locale = Locale("", code)
            Country(code = code, name = locale.displayCountry)
        }.sortedBy { it.name }
    }

    fun countryForCode(code: String): Country? {
        val normalized = code.uppercase()
        val locale = Locale("", normalized)
        if (locale.displayCountry.isBlank()) return null
        return Country(code = normalized, name = locale.displayCountry)
    }

    fun languageName(languageTag: String): String {
        return Locale.forLanguageTag(languageTag).displayLanguage
    }
}
