package com.example.vnnews.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val link: String,
    val title: String,
    val pubDate: String?,
    val description: String?,
    val imageUrl: String?,
    val isBookmarked: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)
