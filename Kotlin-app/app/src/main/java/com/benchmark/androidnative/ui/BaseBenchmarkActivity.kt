package com.benchmark.androidnative.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityBenchmarkBinding
import com.benchmark.androidnative.ui.adapter.ResultAdapter
import com.benchmark.androidnative.util.BenchmarkResult
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel

abstract class BaseBenchmarkActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityBenchmarkBinding
    protected lateinit var viewModel: BenchmarkViewModel
    protected val resultAdapter = ResultAdapter()

    protected abstract val screenTitle: String
    protected abstract fun startBenchmark()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBenchmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = screenTitle
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = resultAdapter

        binding.btnStart.setOnClickListener { startBenchmark() }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnStart.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.statusMessage.observe(this) { message ->
            binding.tvProgress.text = message
        }
    }

    protected fun observeResults(
        resultsLiveData: androidx.lifecycle.LiveData<List<BenchmarkResult>>
    ) {
        resultsLiveData.observe(this) { results ->
            resultAdapter.submitList(results)
            if (results.isNotEmpty()) {
                showAverage(results)
            }
        }
    }

    protected fun showAverage(results: List<BenchmarkResult>) {
        val avgTime = results.map { it.executionMs }.average()
        val avgCpu = results.map { it.cpuPercent }.average()
        val avgMemory = results.map { it.memoryMB }.average()

        binding.tvAverage.visibility = View.VISIBLE
        binding.tvAverage.text = getString(
            R.string.average_format,
            avgTime,
            avgCpu,
            avgMemory
        )
    }
}
