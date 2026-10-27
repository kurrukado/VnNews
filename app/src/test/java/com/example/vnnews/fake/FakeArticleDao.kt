package com.example.vnnews.fake

import com.example.vnnews.data.local.ArticleDao
import com.example.vnnews.data.local.ArticleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeArticleDao : ArticleDao {
    private val storage = mutableMapOf<String, ArticleEntity>()
    private val articlesFlow = MutableStateFlow<List<ArticleEntity>>(emptyList())

    private fun notifyFlow() {
        articlesFlow.value = storage.values.filter { it.isBookmarked }.sortedByDescending { it.savedAt }
    }

    override fun getBookmarkedArticles(): Flow<List<ArticleEntity>> = articlesFlow

    override suspend fun getArticleByLink(link: String): ArticleEntity? = storage[link]

    override fun isBookmarkedFlow(link: String): Flow<Boolean> {
        return articlesFlow.map { list -> list.any { it.link == link && it.isBookmarked } }
    }

    override suspend fun insertOrUpdate(article: ArticleEntity) {
        storage[article.link] = article
        notifyFlow()
    }

    override suspend fun updateBookmarkStatus(link: String, isBookmarked: Boolean): Int {
        val existing = storage[link]
        return if (existing != null) {
            storage[link] = existing.copy(isBookmarked = isBookmarked)
            notifyFlow()
            1
        } else {
            0
        }
    }

    override suspend fun delete(article: ArticleEntity) {
        storage.remove(article.link)
        notifyFlow()
    }
}
