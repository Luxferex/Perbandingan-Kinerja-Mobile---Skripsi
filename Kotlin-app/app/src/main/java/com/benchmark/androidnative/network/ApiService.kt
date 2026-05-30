package com.benchmark.androidnative.network

import com.benchmark.androidnative.model.PostItem
import retrofit2.http.GET

interface ApiService {

    @GET("posts")
    suspend fun getPosts(): List<PostItem>
}
