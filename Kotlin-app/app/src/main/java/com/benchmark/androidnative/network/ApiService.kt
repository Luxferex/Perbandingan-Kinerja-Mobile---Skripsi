package com.benchmark.androidnative.network

import com.benchmark.androidnative.model.PostItem
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiService {

    @Headers(
        "Cache-Control: no-cache, no-store, max-age=0",
        "Pragma: no-cache",
    )
    @GET("posts")
    suspend fun getPosts(
        @Query("_limit") limit: Int = 100,
        @Query("_nonce") nonce: String,
    ): List<PostItem>
}
