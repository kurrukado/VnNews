package com.example.vnnews.fake

import com.example.vnnews.data.remote.NewsApiService
import com.example.vnnews.data.remote.RssFeedResponse
import com.example.vnnews.data.remote.RssItemDto
import java.io.IOException

class FakeNewsApiService : NewsApiService {
    var responseItems: List<RssItemDto> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun getNewsFeed(rssUrl: String): RssFeedResponse {
        if (shouldThrow) {
            throw IOException("Network error fetching RSS feed")
        }
        return RssFeedResponse(
            status = "ok",
            items = responseItems
        )
    }
}
