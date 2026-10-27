package com.example.vnnews

import com.example.vnnews.data.local.ArticleEntity
import com.example.vnnews.data.local.Converters
import com.example.vnnews.data.remote.RssItemDto
import com.example.vnnews.data.repository.NewsCategory
import com.example.vnnews.data.repository.NewsRepository
import com.example.vnnews.ui.home.HomeUiState
import com.example.vnnews.ui.home.HomeViewModel
import com.example.vnnews.ui.navigation.NewsDestination
import com.example.vnnews.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AdversarialChallengeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val converters = Converters()

    // =========================================================================
    // Challenge 1: Room TypeConverters Extreme Values
    // =========================================================================

    @Test
    fun converters_nullValue_returnsNull() {
        assertNull(converters.fromTimestamp(null))
        assertNull(converters.dateToTimestamp(null))
    }

    @Test
    fun converters_epochZero_returnsExactZeroDate() {
        val date = converters.fromTimestamp(0L)
        assertNotNull(date)
        assertEquals(0L, date!!.time)

        val timestamp = converters.dateToTimestamp(Date(0L))
        assertEquals(0L, timestamp)
    }

    @Test
    fun converters_negativeTimestamps_preservesPreEpochValues() {
        val negativeValues = listOf(-1L, -86400000L, -62135596800000L, -1000000000000L)
        for (value in negativeValues) {
            val date = converters.fromTimestamp(value)
            assertNotNull(date)
            assertEquals(value, date!!.time)

            val timestamp = converters.dateToTimestamp(date)
            assertEquals(value, timestamp)
        }
    }

    @Test
    fun converters_longMaxValue_preservesExactValue() {
        val maxVal = Long.MAX_VALUE
        val date = converters.fromTimestamp(maxVal)
        assertNotNull(date)
        assertEquals(maxVal, date!!.time)

        val timestamp = converters.dateToTimestamp(date)
        assertEquals(maxVal, timestamp)
    }

    @Test
    fun converters_longMinValue_preservesExactValue() {
        val minVal = Long.MIN_VALUE
        val date = converters.fromTimestamp(minVal)
        assertNotNull(date)
        assertEquals(minVal, date!!.time)

        val timestamp = converters.dateToTimestamp(date)
        assertEquals(minVal, timestamp)
    }

    @Test
    fun converters_roundTrip_acrossAllExtremes() {
        val testValues = listOf(
            null,
            0L,
            -1L,
            1L,
            -86400000L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            1730000000000L
        )

        for (v in testValues) {
            val date = converters.fromTimestamp(v)
            val backToTimestamp = converters.dateToTimestamp(date)
            assertEquals("Roundtrip mismatch for value $v", v, backToTimestamp)
        }
    }

    // =========================================================================
    // Challenge 2: NewsDestinations.Detail URL & Title Passing
    // =========================================================================

    @Test
    fun newsDestinations_urlWithMultipleQueryParams_preservesSyntaxAndDoesNotCrash() {
        val complexUrl = "https://vnexpress.net/tin-tuc/bai-viet.html?category=so-hoa&page=2&filter=recent&ref=home"
        val title = "Tiêu đề bài viết thử nghiệm"

        val route = NewsDestination.Detail.createRoute(complexUrl, title)
        assertTrue(route.startsWith("detail/"))
        assertFalse(route.contains("?category")) // Query parameters in URL should be encoded in route path

        // Test decodeUrl
        val decoded = NewsDestination.Detail.decodeUrl(complexUrl)
        assertEquals(complexUrl, decoded)
    }

    @Test
    fun newsDestinations_urlWithSpecialDelimiters_questionMarkAmpersandHashPlus() {
        val delimiterUrl = "https://vnexpress.net/search?q=c++&sort=desc#comments-section+top"
        val title = "C++ & #Hash tag"

        val route = NewsDestination.Detail.createRoute(delimiterUrl, title)
        assertTrue(route.startsWith("detail/"))

        val decodedUrl = NewsDestination.Detail.decodeUrl(delimiterUrl)
        val decodedTitle = NewsDestination.Detail.decodeTitle(title)

        assertEquals(delimiterUrl, decodedUrl)
        assertEquals(title, decodedTitle)
    }

    @Test
    fun newsDestinations_titleWithPercentQuotesEmojisAndVietnameseDiacritics() {
        val url = "https://vnexpress.net/kinh-doanh/chung-khoan.html"
        val extremeTitle = """Lãi suất "tăng 6%" & GDP VN: Có dấu sắc huyền hỏi ngã nặng, Đ/đ 🚀🔥"""

        val route = NewsDestination.Detail.createRoute(url, extremeTitle)
        // Ensure no raw space in route
        assertFalse("Route must not contain unencoded spaces", route.contains(" "))
        assertTrue(route.startsWith("detail/"))

        // Decoders must safely handle this title without throwing IllegalArgumentException
        val decodedTitle = NewsDestination.Detail.decodeTitle(extremeTitle)
        assertEquals(extremeTitle, decodedTitle)
    }

    @Test
    fun newsDestinations_emptyAndBlankTitles_fallbackToDefaultTitle() {
        val url = "https://vnexpress.net/thoi-su"
        val blankRoute = NewsDestination.Detail.createRoute(url, "   ")
        val emptyRoute = NewsDestination.Detail.createRoute(url, "")

        // Both should contain "VnExpress"
        assertTrue(blankRoute.contains("VnExpress"))
        assertTrue(emptyRoute.contains("VnExpress"))
    }

    @Test
    fun newsDestinations_nullHandling_returnsEmptyStringWithoutThrowing() {
        assertEquals("", NewsDestination.Detail.decodeUrl(null))
        assertEquals("", NewsDestination.Detail.decodeTitle(null))
    }

    // =========================================================================
    // Challenge 3: HomeViewModel StateFlow under fast switching and network failures
    // =========================================================================

    private class ControllableNewsRepository : NewsRepository {
        val fakeCategories = listOf(
            NewsCategory("cat-1", "Cat 1", "https://vnexpress.net/rss/cat1.rss"),
            NewsCategory("cat-2", "Cat 2", "https://vnexpress.net/rss/cat2.rss"),
            NewsCategory("cat-3", "Cat 3", "https://vnexpress.net/rss/cat3.rss")
        )

        var delayMillis: Long = 0L
        var exceptionToThrow: Exception? = null
        val fetchCallCounts = mutableMapOf<String, Int>()

        override fun getCategories(): List<NewsCategory> = fakeCategories

        override suspend fun getNews(categoryRssUrl: String): List<RssItemDto> {
            fetchCallCounts[categoryRssUrl] = (fetchCallCounts[categoryRssUrl] ?: 0) + 1
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            exceptionToThrow?.let { throw it }
            return listOf(
                RssItemDto(
                    title = "Article for $categoryRssUrl",
                    pubDate = "Sun, 27 Oct 2026 12:00:00 +0700",
                    link = "https://vnexpress.net/article-$categoryRssUrl",
                    thumbnail = null,
                    description = "Description"
                )
            )
        }

        override fun getBookmarkedArticles(): Flow<List<ArticleEntity>> = flowOf(emptyList())

        override fun isBookmarkedFlow(link: String): Flow<Boolean> = flowOf(false)

        override suspend fun toggleBookmark(item: RssItemDto) {}

        override suspend fun removeBookmark(link: String) {}
    }

    @Test
    fun homeViewModel_fastCategorySwitching_cancelsPriorJobAndDoesNotEmitCancellationError() = runTest {
        val repo = ControllableNewsRepository()
        repo.delayMillis = 200L // Simulate slow network

        val viewModel = HomeViewModel(repo)
        runCurrent()
        // Initial load started for cat-1, currently in Loading
        assertTrue(viewModel.uiState.value is HomeUiState.Loading)

        // Rapidly switch to cat-2 before cat-1 finishes
        viewModel.selectCategory(repo.fakeCategories[1])
        runCurrent()

        // ADVERSARIAL CHALLENGE:
        // When cat-1 is cancelled, CancellationException should NOT be caught as a generic exception
        // and leaked to the UI state as an error message!
        val stateAfterSwitch = viewModel.uiState.value
        assertFalse(
            "BUG CONFIRMED: CancellationException was caught and emitted as UI Error: $stateAfterSwitch",
            stateAfterSwitch is HomeUiState.Error && stateAfterSwitch.message.contains("cancelled")
        )

        // Rapidly switch to cat-3 before cat-2 finishes
        viewModel.selectCategory(repo.fakeCategories[2])
        runCurrent()
        assertTrue(viewModel.uiState.value is HomeUiState.Loading)

        // Advance time to allow cat-3 to finish
        advanceTimeBy(300L)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        // CRITICAL CHECK: The final state MUST NOT be HomeUiState.Error caused by CancellationException!
        assertTrue(
            "Expected Success for cat-3, but got $finalState",
            finalState is HomeUiState.Success
        )
        val success = finalState as HomeUiState.Success
        assertEquals("cat-3", success.selectedCategory.id)
        assertEquals("https://vnexpress.net/article-https://vnexpress.net/rss/cat3.rss", success.articles[0].link)
    }

    @Test
    fun homeViewModel_networkFailureThenSuccessfulRetry_transitionsProperly() = runTest {
        val repo = ControllableNewsRepository()
        repo.exceptionToThrow = IOException("Simulated network timeout")

        val viewModel = HomeViewModel(repo)
        advanceUntilIdle()

        // Should be in Error state
        val errorState = viewModel.uiState.value
        assertTrue(errorState is HomeUiState.Error)
        assertTrue((errorState as HomeUiState.Error).message.contains("Không có kết nối mạng"))

        // Now network recovers
        repo.exceptionToThrow = null
        viewModel.retry()
        advanceUntilIdle()

        // Should transition to Success
        val recoveredState = viewModel.uiState.value
        assertTrue("Expected Success after retry, but got $recoveredState", recoveredState is HomeUiState.Success)
        assertEquals("cat-1", (recoveredState as HomeUiState.Success).selectedCategory.id)
    }

    @Test
    fun homeViewModel_switchingWhileFailing_recoversGracefully() = runTest {
        val repo = ControllableNewsRepository()
        // cat-1 fails with network error
        repo.exceptionToThrow = IOException("Network error on cat-1")

        val viewModel = HomeViewModel(repo)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is HomeUiState.Error)

        // cat-2 succeeds
        repo.exceptionToThrow = null
        viewModel.selectCategory(repo.fakeCategories[1])
        advanceUntilIdle()

        val successState = viewModel.uiState.value
        assertTrue(successState is HomeUiState.Success)
        assertEquals("cat-2", (successState as HomeUiState.Success).selectedCategory.id)
    }

    @Test
    fun homeViewModel_rapidSwitchingLoop_stressTest() = runTest {
        val repo = ControllableNewsRepository()
        repo.delayMillis = 50L

        val viewModel = HomeViewModel(repo)

        // Stress: rapid sequential switching through all categories multiple times
        for (i in 0 until 20) {
            val targetCategory = repo.fakeCategories[i % repo.fakeCategories.size]
            viewModel.selectCategory(targetCategory)
            advanceTimeBy(10L) // Switch before 50ms delay expires
        }

        // Now settle on cat-1
        viewModel.selectCategory(repo.fakeCategories[0])
        advanceTimeBy(100L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Final state after stress must be Success, but was $state", state is HomeUiState.Success)
        assertEquals("cat-1", (state as HomeUiState.Success).selectedCategory.id)
    }
}
