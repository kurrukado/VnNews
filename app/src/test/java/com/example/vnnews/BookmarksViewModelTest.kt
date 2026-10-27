package com.example.vnnews

import com.example.vnnews.data.local.ArticleEntity
import com.example.vnnews.fake.FakeNewsRepository
import com.example.vnnews.ui.bookmarks.BookmarksViewModel
import com.example.vnnews.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [BookmarksViewModel] (Unit 6: Room Offline Bookmarking, StateFlow stateIn).
 * Uses [FakeNewsRepository] and [MainDispatcherRule].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeNewsRepository

    private val article1 = ArticleEntity(
        link = "https://vnexpress.net/bai-viet-1.html",
        title = "Bài viết số 1",
        pubDate = "Sun, 27 Oct 2026 08:00:00 +0700",
        description = "Mô tả bài viết 1",
        imageUrl = "https://example.com/img1.jpg",
        isBookmarked = true,
        savedAt = 1000L
    )

    private val article2 = ArticleEntity(
        link = "https://vnexpress.net/bai-viet-2.html",
        title = "Bài viết số 2",
        pubDate = "Sun, 27 Oct 2026 09:00:00 +0700",
        description = "Mô tả bài viết 2",
        imageUrl = "https://example.com/img2.jpg",
        isBookmarked = true,
        savedAt = 2000L
    )

    @Before
    fun setUp() {
        fakeRepository = FakeNewsRepository()
    }

    @Test
    fun bookmarks_initiallyEmitsSavedArticlesFromRepository() = runTest {
        fakeRepository.setBookmarkedArticles(listOf(article1, article2))
        val viewModel = BookmarksViewModel(fakeRepository)

        // BookmarksViewModel uses SharingStarted.WhileSubscribed(5000).
        // Must subscribe/collect to activate upstream Flow emission.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.bookmarks.collect()
        }

        val emittedList = viewModel.bookmarks.value
        assertEquals(2, emittedList.size)
        assertEquals(article1.link, emittedList[0].link)
        assertEquals(article2.link, emittedList[1].link)
    }

    @Test
    fun bookmarks_whenNoArticlesBookmarked_emitsEmptyList() = runTest {
        val viewModel = BookmarksViewModel(fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.bookmarks.collect()
        }

        val emittedList = viewModel.bookmarks.value
        assertTrue("Expected bookmarks to be empty initially", emittedList.isEmpty())
    }

    @Test
    fun removeBookmark_removesTargetArticleAndEmitsUpdatedList() = runTest {
        fakeRepository.setBookmarkedArticles(listOf(article1, article2))
        val viewModel = BookmarksViewModel(fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.bookmarks.collect()
        }

        assertEquals(2, viewModel.bookmarks.value.size)

        // Remove article 1
        viewModel.removeBookmark(article1.link)

        val updatedList = viewModel.bookmarks.value
        assertEquals(1, updatedList.size)
        assertEquals(article2.link, updatedList[0].link)
    }

    @Test
    fun bookmarks_updatesDynamicallyWhenRepositoryAddsNewBookmark() = runTest {
        val viewModel = BookmarksViewModel(fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.bookmarks.collect()
        }

        assertTrue(viewModel.bookmarks.value.isEmpty())

        // Add a bookmark dynamically in repository
        fakeRepository.setBookmarkedArticles(listOf(article1))

        assertEquals(1, viewModel.bookmarks.value.size)
        assertEquals(article1.link, viewModel.bookmarks.value[0].link)
    }
}
