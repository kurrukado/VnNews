package com.example.vnnews.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vnnews.NewsApplication
import com.example.vnnews.data.local.UserPreferencesRepository
import com.example.vnnews.data.remote.RssItemDto
import com.example.vnnews.data.repository.NewsCategory
import com.example.vnnews.data.repository.NewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val articles: List<RssItemDto>,
        val categories: List<NewsCategory>,
        val selectedCategory: NewsCategory,
        val bookmarkedUrls: Set<String> = emptySet()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val newsRepository: NewsRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val categories = newsRepository.getCategories()
    private var currentCategory = categories.first()
    private var fetchJob: Job? = null

    init {
        observeBookmarks()
        loadInitialNews()
    }

    private fun loadInitialNews() {
        viewModelScope.launch {
            val savedCategoryId = try {
                userPreferencesRepository?.selectedCategory?.first()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            val initialCategory = categories.find { it.id == savedCategoryId } ?: categories.first()
            currentCategory = initialCategory
            loadNews(initialCategory)
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            newsRepository.getBookmarkedArticles().collect { bookmarkedList ->
                val urls = bookmarkedList.map { it.link }.toSet()
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(bookmarkedUrls = urls)
                }
            }
        }
    }

    fun selectCategory(category: NewsCategory) {
        if (currentCategory.id == category.id && _uiState.value is HomeUiState.Success) return
        currentCategory = category
        viewModelScope.launch {
            try {
                userPreferencesRepository?.saveSelectedCategory(category.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Log or ignore to prevent UI disruption
            }
        }
        loadNews(category)
    }

    fun retry() {
        loadNews(currentCategory)
    }

    fun toggleBookmark(item: RssItemDto) {
        viewModelScope.launch {
            newsRepository.toggleBookmark(item)
        }
    }

    private fun loadNews(category: NewsCategory) {
        _uiState.value = HomeUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                val articles = newsRepository.getNews(category.rssUrl)
                val bookmarkedUrls = try {
                    newsRepository.getBookmarkedArticles().first().map { it.link }.toSet()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptySet()
                }
                _uiState.value = HomeUiState.Success(
                    articles = articles,
                    categories = categories,
                    selectedCategory = category,
                    bookmarkedUrls = bookmarkedUrls
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = HomeUiState.Error("Không có kết nối mạng hoặc máy chủ không phản hồi. Vui lòng thử lại!")
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Đã xảy ra lỗi: ${e.localizedMessage ?: "Không xác định"}")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as NewsApplication)
                HomeViewModel(
                    newsRepository = application.container.newsRepository,
                    userPreferencesRepository = application.container.userPreferencesRepository
                )
            }
        }
    }
}
