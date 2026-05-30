package com.benchmark.androidnative.database
import androidx.room.*

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT * FROM posts")
    suspend fun getAllPosts(): List<PostEntity>

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    fun deletePostById(id: Int)        // ← hapus suspend

    @Query("DELETE FROM posts")
    fun deleteAll()                    // ← hapus suspend

    @Query("SELECT COUNT(*) FROM posts")
    fun getPostCount(): Int            // ← hapus suspend
}