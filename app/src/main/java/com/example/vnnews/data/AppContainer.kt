package com.example.vnnews.data

import android.content.Context
import com.example.vnnews.data.local.AppDatabase
import com.example.vnnews.data.local.UserPreferencesRepository
import com.example.vnnews.data.local.dataStore
import com.example.vnnews.data.remote.DefaultNewsApiService
import com.example.vnnews.data.remote.NewsApiService
import com.example.vnnews.data.remote.VnExpressRawApi
import com.example.vnnews.data.repository.NetworkNewsRepository
import com.example.vnnews.data.repository.NewsRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

interface AppContainer {
    val newsRepository: NewsRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val baseUrl = "https://vnexpress.net/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()

    private val rawApi: VnExpressRawApi by lazy {
        retrofit.create(VnExpressRawApi::class.java)
    }

    private val retrofitService: NewsApiService by lazy {
        DefaultNewsApiService(rawApi)
    }

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val newsRepository: NewsRepository by lazy {
        NetworkNewsRepository(retrofitService, database.articleDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }
}
