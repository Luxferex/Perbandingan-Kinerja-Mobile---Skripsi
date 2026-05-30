package com.benchmark.androidnative.ui

import com.benchmark.androidnative.R

class HttpActivity : BaseBenchmarkActivity() {

    override val screenTitle: String
        get() = getString(R.string.http_activity_title)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        observeResults(viewModel.httpResults)
    }

    override fun startBenchmark() {
        binding.tvAverage.visibility = android.view.View.GONE
        viewModel.runHttpBenchmark()
    }
}
