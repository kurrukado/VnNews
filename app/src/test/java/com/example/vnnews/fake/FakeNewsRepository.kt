package com.example.vnnews.fake

import com.example.vnnews.data.local.ArticleEntity
import com.example.vnnews.data.remote.RssItemDto
import com.example.vnnews.data.repository.NewsCategory
import com.example.vnnews.data.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

class FakeNewsRepository : NewsRepository {

    val fakeCategories = listOf(
        NewsCategory("tin-moi-nhat", "Mới nhất", "https://vnexpress.net/rss/tin-moi-nhat.rss"),
        NewsCategory("so-hoa", "Công nghệ", "https://vnexpress.net/rss/so-hoa.rss"),
        NewsCategory("kinh-doanh", "Kinh doanh", "https://vnexpress.net/rss/kinh-doanh.rss"),
        NewsCategory("the-thao", "Thể thao", "https://vnexpress.net/rss/the-thao.rss"),
        NewsCategory("giai-tri", "Giải trí", "https://vnexpress.net/rss/giai-tri.rss")
    )

    private val _bookmarkedArticles = MutableStateFlow<List<ArticleEntity>>(emptyList())
    var fakeArticles: List<RssItemDto> = emptyList()
    var shouldThrowIoException: Boolean = false
    var shouldThrowGenericException: Boolean = false

    override fun getCategories(): List<NewsCategory> = fakeCategories

    override suspend fun getNews(categoryRssUrl: String): List<RssItemDto> {
        if (shouldThrowIoException) {
            throw IOException("Không có kết nối mạng hoặc máy chủ không phản hồi. Vui lòng thử lại!")
        }
        if (shouldThrowGenericException) {
            throw RuntimeException("Generic server failure")
        }
        return fakeArticles
    }

    override fun getBookmarkedArticles(): Flow<List<ArticleEntity>> = _bookmarkedArticles

    override fun isBookmarkedFlow(link: String): Flow<Boolean> {
        return _bookmarkedArticles.map { list -> list.any { it.link == link && it.isBookmarked } }
    }

    override suspend fun toggleBookmark(item: RssItemDto) {
        val current = _bookmarkedArticles.value.toMutableList()
        val index = current.indexOfFirst { it.link == item.link }
        if (index != -1) {
            current.removeAt(index)
        } else {
            current.add(
                ArticleEntity(
                    link = item.link,
                    title = item.title,
                    pubDate = item.pubDate,
                    description = item.getCleanDescription(),
                    imageUrl = item.getImageUrl(),
                    isBookmarked = true
                )
            )
        }
        _bookmarkedArticles.value = current
    }

    override suspend fun removeBookmark(link: String) {
        val current = _bookmarkedArticles.value.toMutableList()
        current.removeAll { it.link == link }
        _bookmarkedArticles.value = current
    }

    fun setBookmarkedArticles(articles: List<ArticleEntity>) {
        _bookmarkedArticles.value = articles
    }
}
