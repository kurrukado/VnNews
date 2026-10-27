package com.example.vnnews.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v1/api.json")
    suspend fun getNewsFeed(
        @Query("rss_url") rssUrl: String
    ): RssFeedResponse
}
