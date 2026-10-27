package com.example.vnnews.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class NewsDestination(val route: String) {
    data object Home : NewsDestination("home")
    data object Bookmarks : NewsDestination("bookmarks")
    data object Detail : NewsDestination("detail/{encodedUrl}/{encodedTitle}") {
        fun createRoute(url: String, title: String): String {
            val safeUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val safeTitle = URLEncoder.encode(
                title.ifBlank { "VnExpress" },
                StandardCharsets.UTF_8.toString()
            ).replace("+", "%20")
            return "detail/$safeUrl/$safeTitle"
        }

        fun decodeUrl(arg: String?): String {
            return arg.orEmpty()
        }

        fun decodeTitle(arg: String?): String {
            return arg.orEmpty()
        }
    }
}
