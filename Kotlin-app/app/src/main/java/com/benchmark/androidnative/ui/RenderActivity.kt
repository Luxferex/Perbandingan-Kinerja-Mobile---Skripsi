package com.benchmark.androidnative.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityRenderBinding
import com.benchmark.androidnative.model.PostItem
import com.benchmark.androidnative.ui.adapter.PostAdapter
import com.benchmark.androidnative.util.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class RenderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRenderBinding
    private lateinit var viewModel: com.benchmark.androidnative.viewmodel.BenchmarkViewModel
    private val postAdapter = PostAdapter()

    companion object {
        private const val ITEM_COUNT = 1000
        private const val SCROLL_DURATION_MS = 30_000L
        private const val SAMPLE_INTERVAL_MS = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRenderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.render_activity_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.rvPosts.layoutManager = LinearLayoutManager(this)
        binding.rvPosts.setHasFixedSize(true)
        binding.rvPosts.adapter = postAdapter
        postAdapter.submitList(createRenderPosts(ITEM_COUNT))

        binding.btnStartRender.setOnClickListener { startRenderBenchmark() }
    }

    private fun startRenderBenchmark() {
        binding.btnStartRender.isEnabled = false
        binding.tvRenderAverage.visibility = View.GONE
        binding.tvRenderProgress.text = getString(R.string.render_progress_format, 0)

        lifecycleScope.launch {
            val results = mutableListOf<BenchmarkResult>()
            val scrollJob = launch(Dispatchers.Main) {
                autoScrollRecyclerView(SCROLL_DURATION_MS)
            }

            var elapsedMs = 0L
            while (elapsedMs < SCROLL_DURATION_MS && isActive) {
                delay(SAMPLE_INTERVAL_MS)
                elapsedMs += SAMPLE_INTERVAL_MS
                val second = (elapsedMs / SAMPLE_INTERVAL_MS).toInt()
                val sample = withContext(Dispatchers.IO) {
                    viewModel.recordRenderSample(this@RenderActivity, second)
                }
                results.add(sample)
                binding.tvRenderProgress.text =
                    getString(R.string.render_progress_format, second.coerceAtMost(30))
            }

            scrollJob.join()
            viewModel.setRenderResults(results)

            val avgCpu = results.map { it.cpuPercent }.average()
            val avgMemory = results.map { it.memoryMB }.average()
            binding.tvRenderAverage.visibility = View.VISIBLE
            binding.tvRenderAverage.text = getString(
                R.string.render_average_format,
                avgCpu,
                avgMemory
            )
            binding.btnStartRender.isEnabled = true
        }
    }

    private suspend fun autoScrollRecyclerView(durationMs: Long) {
        val recyclerView = binding.rvPosts
        val startTime = System.currentTimeMillis()
        var lastY = 0

        while (System.currentTimeMillis() - startTime < durationMs) {
            val scrollRange = recyclerView.computeVerticalScrollRange() -
                recyclerView.computeVerticalScrollExtent()
            if (scrollRange <= 0) {
                delay(16)
                continue
            }

            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toFloat() / durationMs.toFloat()
            val targetY = (scrollRange * progress).toInt()
            val delta = max(0, targetY - lastY)
            if (delta > 0) {
                recyclerView.scrollBy(0, delta)
                lastY = targetY
            }
            delay(16)
        }
    }

    private fun createRenderPosts(count: Int): List<PostItem> {
        return (1..count).map { index ->
            PostItem(
                id = index,
                userId = (index % 10) + 1,
                title = "Render benchmark post $index",
                body = "Body content for render benchmark item $index"
            )
        }
    }
}
