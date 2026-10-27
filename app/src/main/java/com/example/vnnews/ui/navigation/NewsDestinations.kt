package com.example.vnnews.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class NewsDestination(val route: String) {
    data object Home : NewsDestination("home")
    data object Bookmarks : NewsDestination("bookmarks")
    data object Detail : NewsDestination("detail/{encodedUrl}/{encodedTitle}") {
        fun createRoute(url: String, title: String): String {
            val safeUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val safeTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "detail/$safeUrl/$safeTitle"
        }

        fun decodeUrl(arg: String?): String {
            return if (arg != null) URLDecoder.decode(arg, StandardCharsets.UTF_8.toString()) else ""
        }

        fun decodeTitle(arg: String?): String {
            return if (arg != null) URLDecoder.decode(arg, StandardCharsets.UTF_8.toString()) else ""
        }
    }
}
