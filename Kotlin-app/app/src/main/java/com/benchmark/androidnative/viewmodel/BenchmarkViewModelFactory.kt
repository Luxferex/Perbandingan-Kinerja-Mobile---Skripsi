package com.benchmark.androidnative.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.benchmark.androidnative.database.AppDatabase
import com.benchmark.androidnative.repository.BenchmarkRepository

class BenchmarkViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BenchmarkViewModel::class.java)) {
            val database = AppDatabase.getInstance(application)
            val repository = BenchmarkRepository(database)
            return BenchmarkViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
