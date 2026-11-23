package com.misterdownloadmanager.port.core.engine

object HtmlParser {
    fun extractLinks(html: String, baseUrl: String): List<String> {
        val pattern = Regex("href\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        return pattern.findAll(html).map { it.groupValues[1] }.map { resolve(baseUrl, it) }.distinct().toList()
    }

    fun extractTitle(html: String): String? {
        return Regex("<title>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)?.trim()
    }

    fun resolve(baseUrl: String, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("magnet:")) return href
        if (href.startsWith("//")) return "https:$href"
        val base = baseUrl.substringBefore('?')
        if (href.startsWith("/")) {
            val origin = base.substringBefore('/', base)
            val schemeEnd = base.indexOf("://")
            val authority = if (schemeEnd >= 0) base.substring(0, base.indexOf('/', schemeEnd + 3).takeIf { it >= 0 } ?: base.length) else origin
            return authority + href
        }
        return base.substringBeforeLast('/') + "/" + href
    }
}
