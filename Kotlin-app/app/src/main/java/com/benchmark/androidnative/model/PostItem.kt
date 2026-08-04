package com.benchmark.androidnative.model

import androidx.annotation.Keep

@Keep
data class PostItem(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
)
