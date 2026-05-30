package com.benchmark.androidnative.ui

import com.benchmark.androidnative.R

class SqliteActivity : BaseBenchmarkActivity() {

    override val screenTitle: String
        get() = getString(R.string.sqlite_activity_title)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        observeResults(viewModel.sqliteResults)
    }

    override fun startBenchmark() {
        binding.tvAverage.visibility = android.view.View.GONE
        viewModel.runSqliteBenchmark()
    }
}
