package com.benchmark.androidnative.repository

import com.benchmark.androidnative.database.AppDatabase
import com.benchmark.androidnative.database.PostEntity
import com.benchmark.androidnative.model.PostItem
import com.benchmark.androidnative.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BenchmarkRepository(
    private val database: AppDatabase,
    private val retrofitClient: RetrofitClient = RetrofitClient,
) {

    private val postDao = database.postDao()
    private val apiService = retrofitClient.apiService

    suspend fun fetchPostsFromApi(): List<PostItem> =
        apiService.getPosts(limit = 100)

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        postDao.deleteAll()
    }

    suspend fun insertPostsToDb(posts: List<PostEntity>) = withContext(Dispatchers.IO) {
        postDao.insertAll(posts)
    }

    suspend fun getAllPostsFromDb(): List<PostEntity> = withContext(Dispatchers.IO) {
        postDao.getAllPosts()
    }

    suspend fun updateHalf(): Int = withContext(Dispatchers.IO) {
        postDao.updateHalf()
    }

    suspend fun deleteHalf(): Int = withContext(Dispatchers.IO) {
        postDao.deleteHalf()
    }
}
