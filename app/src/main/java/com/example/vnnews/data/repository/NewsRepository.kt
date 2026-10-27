package com.example.vnnews.data.repository

import com.example.vnnews.data.local.ArticleDao
import com.example.vnnews.data.local.ArticleEntity
import com.example.vnnews.data.remote.NewsApiService
import com.example.vnnews.data.remote.RssItemDto
import kotlinx.coroutines.flow.Flow

data class NewsCategory(
    val id: String,
    val title: String,
    val rssUrl: String
)

interface NewsRepository {
    fun getCategories(): List<NewsCategory>
    suspend fun getNews(categoryRssUrl: String): List<RssItemDto>
    fun getBookmarkedArticles(): Flow<List<ArticleEntity>>
    fun isBookmarkedFlow(link: String): Flow<Boolean>
    suspend fun toggleBookmark(item: RssItemDto)
    suspend fun removeBookmark(link: String)
}

class NetworkNewsRepository(
    private val newsApiService: NewsApiService,
    private val articleDao: ArticleDao
) : NewsRepository {

    override fun getCategories(): List<NewsCategory> = listOf(
        NewsCategory("tin-moi-nhat", "Mới nhất", "https://vnexpress.net/rss/tin-noi-bat.rss"),
        NewsCategory("so-hoa", "Công nghệ", "https://vnexpress.net/rss/khoa-hoc-cong-nghe.rss"),
        NewsCategory("kinh-doanh", "Kinh doanh", "https://vnexpress.net/rss/kinh-doanh.rss"),
        NewsCategory("the-thao", "Thể thao", "https://vnexpress.net/rss/the-thao.rss"),
        NewsCategory("giai-tri", "Giải trí", "https://vnexpress.net/rss/giai-tri.rss")
    )

    override suspend fun getNews(categoryRssUrl: String): List<RssItemDto> {
        val response = newsApiService.getNewsFeed(categoryRssUrl)
        return response.items
    }

    override fun getBookmarkedArticles(): Flow<List<ArticleEntity>> {
        return articleDao.getBookmarkedArticles()
    }

    override fun isBookmarkedFlow(link: String): Flow<Boolean> {
        return articleDao.isBookmarkedFlow(link)
    }

    override suspend fun toggleBookmark(item: RssItemDto) {
        val existing = articleDao.getArticleByLink(item.link)
        if (existing != null && existing.isBookmarked) {
            articleDao.updateBookmarkStatus(item.link, false)
        } else {
            val entity = ArticleEntity(
                link = item.link,
                title = item.title,
                pubDate = item.pubDate,
                description = item.getCleanDescription(),
                imageUrl = item.getImageUrl(),
                isBookmarked = true
            )
            articleDao.insertOrUpdate(entity)
        }
    }

    override suspend fun removeBookmark(link: String) {
        articleDao.updateBookmarkStatus(link, false)
    }
}
