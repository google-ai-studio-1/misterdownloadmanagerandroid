package com.misterdownloadmanager.port.core.labserver

data class LabRoute(val path: String, val statusCode: Int, val body: String) {
    fun matches(requestPath: String): Boolean {
        if (path == requestPath) return true
        if (path.endsWith("/*")) {
            return requestPath.startsWith(path.dropLast(1))
        }
        val patternParts = path.split('/')
        val requestParts = requestPath.split('/')
        if (patternParts.size != requestParts.size) return false
        return patternParts.zip(requestParts).all { (pattern, actual) ->
            pattern.startsWith("{") && pattern.endsWith("}") || pattern == actual || pattern == "*"
        }
    }
}

object LabServer {
    const val DEFAULT_BIND: String = "127.0.0.1"
    const val DEFAULT_PORT: Int = 8090

    fun routes(): List<LabRoute> {
        return listOf(
            LabRoute("/", 200, ""),
            LabRoute("/gen/{size}", 200, ""),
            LabRoute("/gen/{size}/{name}", 200, ""),
            LabRoute("/form/{size}", 200, ""),
            LabRoute("/files/{name}", 200, ""),
            LabRoute("/_log", 200, ""),
            LabRoute("/redirect", 302, ""),
            LabRoute("/hop", 302, ""),
            LabRoute("/page", 200, ""),
            LabRoute("/feed", 200, ""),
            LabRoute("/feed/post/{n}", 200, ""),
            LabRoute("/leadon", 200, ""),
            LabRoute("/declared", 200, ""),
            LabRoute("/formpage", 200, ""),
            LabRoute("/cookie", 200, ""),
            LabRoute("/assets/{file}", 200, ""),
            LabRoute("/hls/*", 200, ""),
            LabRoute("/hlsg/*", 200, ""),
            LabRoute("/dash/*", 200, ""),
            LabRoute("/hls2/*", 200, ""),
            LabRoute("/dash2/*", 200, ""),
            LabRoute("/stream.php", 200, ""),
            LabRoute("/remote_control.php", 200, ""),
            LabRoute("/gh/api/*", 200, ""),
            LabRoute("/gh/assets/*", 200, ""),
            LabRoute("/gh/raw/*", 200, ""),
            LabRoute("/rss/shows.xml", 200, ""),
            LabRoute("/rss/dir.xml", 200, ""),
            LabRoute("/rss/files/{name}", 200, ""),
            LabRoute("/rd/rest/1.0/user", 200, ""),
            LabRoute("/rd/rest/1.0/hosts/domains", 200, ""),
            LabRoute("/rd/rest/1.0/unrestrict/link", 200, ""),
            LabRoute("/rd/oauth/v2/device/code", 200, ""),
            LabRoute("/rd/oauth/v2/device/credentials", 200, ""),
            LabRoute("/rd/oauth/v2/token", 200, ""),
            LabRoute("/ad/v4/user", 200, ""),
            LabRoute("/ad/v4/hosts/domains", 200, ""),
            LabRoute("/ad/v4/link/unlock", 200, ""),
            LabRoute("/ad/v4/link/delayed", 200, ""),
            LabRoute("/ad/v4.1/pin/get", 200, ""),
            LabRoute("/ad/v4.1/pin/check", 200, ""),
            LabRoute("/pm/api/account/info", 200, ""),
            LabRoute("/pm/api/services/list", 200, ""),
            LabRoute("/pm/api/transfer/directdl", 200, ""),
            LabRoute("/pm/api/transfer/create", 200, ""),
            LabRoute("/pm/api/transfer/list", 200, ""),
            LabRoute("/pm/api/transfer/delete", 200, ""),
            LabRoute("/pm/api/folder/list", 200, ""),
            LabRoute("/md/api.php", 200, ""),
            LabRoute("/tb/v1/api/user/me", 200, ""),
            LabRoute("/tb/v1/api/user/auth/device/start", 200, ""),
            LabRoute("/tb/v1/api/user/auth/device/token", 200, ""),
            LabRoute("/tb/v1/api/webdl/hosters", 200, ""),
            LabRoute("/tb/v1/api/webdl/createwebdownload", 200, ""),
            LabRoute("/tb/v1/api/webdl/controlwebdownload", 200, ""),
            LabRoute("/tb/v1/api/torrents/createtorrent", 200, ""),
            LabRoute("/tb/v1/api/torrents/controltorrent", 200, ""),
            LabRoute("/tb/v1/api/webdl/mylist", 200, ""),
            LabRoute("/tb/v1/api/webdl/requestdl", 200, ""),
            LabRoute("/tb/v1/api/torrents/mylist", 200, ""),
            LabRoute("/tb/v1/api/torrents/requestdl", 200, "")
        )
    }

    fun routeFor(path: String): LabRoute? {
        return routes().firstOrNull { it.matches(path) }
    }
}
