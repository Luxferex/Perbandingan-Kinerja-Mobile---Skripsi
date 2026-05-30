package com.benchmark.androidnative

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.benchmark.androidnative.viewmodel.BenchmarkViewModelFactory

class BenchmarkApplication : Application(), ViewModelStoreOwner {

    private val appViewModelStore = ViewModelStore()

    val benchmarkViewModel: BenchmarkViewModel by lazy {
        ViewModelProvider(
            this,
            BenchmarkViewModelFactory(this)
        )[BenchmarkViewModel::class.java]
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
