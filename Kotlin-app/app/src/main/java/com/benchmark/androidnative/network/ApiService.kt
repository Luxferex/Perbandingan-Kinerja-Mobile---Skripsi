package com.benchmark.androidnative.network

import com.benchmark.androidnative.model.PostItem
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("posts")
    suspend fun getPosts(@Query("_limit") limit: Int = 100): List<PostItem>
}
