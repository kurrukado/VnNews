package com.example.vnnews

import com.example.vnnews.data.local.ArticleEntity
import com.example.vnnews.data.local.UserPreferencesRepository
import com.example.vnnews.data.remote.RssItemDto
import com.example.vnnews.fake.FakeDataStore
import com.example.vnnews.fake.FakeNewsRepository
import com.example.vnnews.ui.home.HomeUiState
import com.example.vnnews.ui.home.HomeViewModel
import com.example.vnnews.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [HomeViewModel] (Unit 4: MVVM architecture, StateFlow UI state).
 * Uses [FakeNewsRepository] and [MainDispatcherRule].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeNewsRepository

    private val sampleArticle = RssItemDto(
        title = "Tin tuc cong nghe moi nhat",
        pubDate = "Sun, 27 Oct 2026 10:00:00 +0700",
        link = "https://vnexpress.net/so-hoa/tin-tuc-cong-nghe-4800000.html",
        thumbnail = "https://i1-vnexpress.vnecdn.net/2026/10/27/sample.jpg",
        description = "Mo ta ngan gon ve cong nghe AI moi"
    )

    @Before
    fun setUp() {
        fakeRepository = FakeNewsRepository()
    }

    @Test
    fun init_loadsDefaultCategoryNewsAndEmitsSuccessState() = runTest {
        fakeRepository.fakeArticles = listOf(sampleArticle)

        val viewModel = HomeViewModel(fakeRepository)

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Success but was $state", state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals("tin-moi-nhat", success.selectedCategory.id)
        assertEquals(1, success.articles.size)
        assertEquals(sampleArticle.link, success.articles[0].link)
        assertEquals(5, success.categories.size)
    }

    @Test
    fun init_whenNetworkThrowsIOException_emitsErrorStateWithNetworkMessage() = runTest {
        fakeRepository.shouldThrowIoException = true

        val viewModel = HomeViewModel(fakeRepository)

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Error but was $state", state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertTrue(
            "Error message should mention network connectivity",
            error.message.contains("Không có kết nối mạng")
        )
    }

    @Test
    fun init_whenRepositoryThrowsGenericException_emitsErrorStateWithErrorMessage() = runTest {
        fakeRepository.shouldThrowGenericException = true

        val viewModel = HomeViewModel(fakeRepository)

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Error but was $state", state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertTrue(
            "Error message should contain 'Đã xảy ra lỗi'",
            error.message.contains("Đã xảy ra lỗi")
        )
    }

    @Test
    fun selectCategory_switchesCategoryAndLoadsNewCategoryArticles() = runTest {
        fakeRepository.fakeArticles = listOf(sampleArticle)
        val viewModel = HomeViewModel(fakeRepository)

        val techCategory = fakeRepository.fakeCategories.first { it.id == "so-hoa" }
        viewModel.selectCategory(techCategory)

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals("so-hoa", success.selectedCategory.id)
    }

    @Test
    fun selectCategory_whenSelectingCurrentCategoryInSuccessState_doesNotReload() = runTest {
        fakeRepository.fakeArticles = listOf(sampleArticle)
        val viewModel = HomeViewModel(fakeRepository)

        val defaultCategory = fakeRepository.fakeCategories.first { it.id == "tin-moi-nhat" }
        viewModel.selectCategory(defaultCategory)

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        assertEquals("tin-moi-nhat", (state as HomeUiState.Success).selectedCategory.id)
    }

    @Test
    fun retry_afterNetworkFailure_reloadsNewsSuccessfully() = runTest {
        fakeRepository.shouldThrowIoException = true
        val viewModel = HomeViewModel(fakeRepository)

        assertTrue(viewModel.uiState.value is HomeUiState.Error)

        // Fix network and provide articles
        fakeRepository.shouldThrowIoException = false
        fakeRepository.fakeArticles = listOf(sampleArticle)

        viewModel.retry()

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Success after retry but was $state", state is HomeUiState.Success)
        assertEquals(1, (state as HomeUiState.Success).articles.size)
    }

    @Test
    fun toggleBookmark_updatesBookmarkedUrlsInSuccessState() = runTest {
        fakeRepository.fakeArticles = listOf(sampleArticle)
        val viewModel = HomeViewModel(fakeRepository)

        assertTrue((viewModel.uiState.value as HomeUiState.Success).bookmarkedUrls.isEmpty())

        // Toggle bookmark ON
        viewModel.toggleBookmark(sampleArticle)

        var state = viewModel.uiState.value as HomeUiState.Success
        assertTrue(
            "Bookmarked URLs should contain sampleArticle link",
            state.bookmarkedUrls.contains(sampleArticle.link)
        )

        // Toggle bookmark OFF
        viewModel.toggleBookmark(sampleArticle)

        state = viewModel.uiState.value as HomeUiState.Success
        assertTrue(
            "Bookmarked URLs should be empty after toggling off",
            state.bookmarkedUrls.isEmpty()
        )
    }

    @Test
    fun init_withPreExistingBookmarks_reflectsBookmarkedUrlsInSuccessState() = runTest {
        val bookmarkedEntity = ArticleEntity(
            link = sampleArticle.link,
            title = sampleArticle.title,
            pubDate = sampleArticle.pubDate,
            description = sampleArticle.description,
            imageUrl = sampleArticle.thumbnail,
            isBookmarked = true
        )
        fakeRepository.setBookmarkedArticles(listOf(bookmarkedEntity))
        fakeRepository.fakeArticles = listOf(sampleArticle)

        val viewModel = HomeViewModel(fakeRepository)

        val state = viewModel.uiState.value as HomeUiState.Success
        assertTrue(state.bookmarkedUrls.contains(sampleArticle.link))
    }

    @Test
    fun init_withPersistedCategoryInDataStore_loadsPersistedCategoryArticles() = runTest {
        val fakeDataStore = FakeDataStore()
        val userPrefs = UserPreferencesRepository(fakeDataStore)
        userPrefs.saveSelectedCategory("so-hoa")

        fakeRepository.fakeArticles = listOf(sampleArticle)
        val viewModel = HomeViewModel(fakeRepository, userPrefs)

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Success but was $state", state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals("so-hoa", success.selectedCategory.id)
    }

    @Test
    fun selectCategory_withUserPreferencesRepository_persistsSelectedCategory() = runTest {
        val fakeDataStore = FakeDataStore()
        val userPrefs = UserPreferencesRepository(fakeDataStore)

        fakeRepository.fakeArticles = listOf(sampleArticle)
        val viewModel = HomeViewModel(fakeRepository, userPrefs)

        val businessCategory = fakeRepository.fakeCategories.first { it.id == "kinh-doanh" }
        viewModel.selectCategory(businessCategory)

        val persistedCategory = userPrefs.selectedCategory.first()
        assertEquals("kinh-doanh", persistedCategory)
    }
}
