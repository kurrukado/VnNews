package com.example.vnnews

import com.example.vnnews.ui.navigation.NewsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsDestinationsTest {

    @Test
    fun createRoute_encodesSpecialCharactersInUrlAndSpacesInTitle() {
        val url = "https://vnexpress.net/thoi-su/bai-viet.html?tag=c++"
        val title = "Tăng trưởng 6% & phát triển"
        val route = NewsDestination.Detail.createRoute(url, title)

        // Route should not contain raw spaces
        assertFalse(route.contains(" "))
        assertTrue(route.startsWith("detail/"))
    }

    @Test
    fun decodeUrl_and_decodeTitle_safelyPreserveCharactersWithoutCrashing() {
        val testUrl = "https://vnexpress.net/kinh-doanh/bai-viet-123.html?tag=c%2B%2B"
        val testTitle = "Lãi suất tăng 5%"

        // Navigation Compose internally passes decoded string to destination
        val decodedUrl = NewsDestination.Detail.decodeUrl(testUrl)
        val decodedTitle = NewsDestination.Detail.decodeTitle(testTitle)

        assertEquals(testUrl, decodedUrl)
        assertEquals(testTitle, decodedTitle)
    }

    @Test
    fun decode_handlesNullGracefully() {
        assertEquals("", NewsDestination.Detail.decodeUrl(null))
        assertEquals("", NewsDestination.Detail.decodeTitle(null))
    }
}
