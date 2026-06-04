package com.benchmark.androidnative.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT * FROM posts ORDER BY id ASC")
    suspend fun getAllPosts(): List<PostEntity>

    @Query("UPDATE posts SET title = 'updated_title' WHERE id <= 500")
    suspend fun updateHalf(): Int

    @Query("DELETE FROM posts WHERE id > 500")
    suspend fun deleteHalf(): Int

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
