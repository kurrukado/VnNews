package com.example.vnnews.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface NewsApiService {
    suspend fun getNewsFeed(rssUrl: String): RssFeedResponse
}

interface VnExpressRawApi {
    @GET
    suspend fun getRawXml(@Url url: String): ResponseBody
}

class DefaultNewsApiService(
    private val rawApi: VnExpressRawApi
) : NewsApiService {
    override suspend fun getNewsFeed(rssUrl: String): RssFeedResponse {
        val responseBody = rawApi.getRawXml(rssUrl)
        val items = VnExpressRssParser.parse(responseBody.byteStream())
        return RssFeedResponse(
            status = "ok",
            items = items
        )
    }
}
