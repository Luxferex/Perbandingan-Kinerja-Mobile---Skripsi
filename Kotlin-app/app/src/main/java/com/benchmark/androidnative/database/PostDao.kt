package com.benchmark.androidnative.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT * FROM posts")
    suspend fun getAllPosts(): List<PostEntity>

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int
}
