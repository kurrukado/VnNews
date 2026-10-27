package com.example.vnnews.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY savedAt DESC")
    fun getBookmarkedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    suspend fun getArticleByLink(link: String): ArticleEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE link = :link AND isBookmarked = 1)")
    fun isBookmarkedFlow(link: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(article: ArticleEntity)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE link = :link")
    suspend fun updateBookmarkStatus(link: String, isBookmarked: Boolean): Int

    @Delete
    suspend fun delete(article: ArticleEntity)
}
