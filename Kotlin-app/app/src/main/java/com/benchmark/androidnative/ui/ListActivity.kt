package com.benchmark.androidnative.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityScenarioBinding
import com.benchmark.androidnative.model.PostItem
import com.benchmark.androidnative.ui.adapter.PostListAdapter
import com.benchmark.androidnative.util.BenchmarkRunHelper
import com.benchmark.androidnative.util.BenchmarkUtils
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.benchmark.androidnative.viewmodel.ListUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScenarioBinding
    private lateinit var viewModel: BenchmarkViewModel
    private val postAdapter = PostListAdapter()

    private lateinit var tvTargetRuns: TextView
    private lateinit var tvGenerateTime: TextView
    private lateinit var tvRunCount: TextView
    private lateinit var btnCopy: MaterialButton
    private var isMeasuringRender = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.list_screen_title)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.inflateMenu(R.menu.menu_scenario)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_reset_runs) {
                resetRuns()
                true
            } else {
                false
            }
        }

        binding.tvDescription.text = getString(R.string.list_card_desc)
        binding.rvList.visibility = View.VISIBLE
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = postAdapter

        setupMetricsViews()

        viewModel.renderingTargetRuns.observe(this) { target ->
            binding.btnRun.text = getString(R.string.run_list, target)
            tvTargetRuns.text = getString(R.string.target_runs, target)
            updateProgressFromState(viewModel.listState.value, target)
        }

        viewModel.listState.observe(this) { state ->
            updateUi(state)
        }

        binding.btnRun.setOnClickListener {
            lifecycleScope.launch { runBenchmark() }
        }

        binding.btnResetRuns.setOnClickListener {
            resetRuns()
        }
    }

    private fun setupMetricsViews() {
        val container = binding.metricsContainer
        container.removeAllViews()

        tvTargetRuns = addMetricLine(container)
        tvGenerateTime = addMetricLine(container)
        tvRunCount = addMetricLine(container)

        btnCopy = MaterialButton(this).apply {
            text = getString(R.string.copy_result)
            setOnClickListener { copyResult() }
        }
        container.addView(btnCopy)
        btnCopy.visibility = View.GONE
    }

    private fun addMetricLine(container: android.view.ViewGroup): TextView {
        val textView = TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 8 }
        }
        container.addView(textView)
        return textView
    }

    private fun updateUi(state: ListUiState) {
        val target = viewModel.targetRunsFor("rendering")
        val generateMs = if (state.isGenerated) state.executionTimeMs else 0.0
        tvGenerateTime.text = getString(
            R.string.generate_time,
            BenchmarkUtils.formatMs(generateMs),
        )
        tvRunCount.text = getString(R.string.run_count, state.runCount)

        binding.btnRun.isEnabled = !state.isLoading
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnResetRuns.visibility =
            if (state.runCount > 0 && !state.isLoading) View.VISIBLE else View.GONE

        btnCopy.visibility = if (state.runCount > 0) View.VISIBLE else View.GONE

        updateProgressFromState(state, target)

        if (state.pendingMeasurement && !isMeasuringRender) {
            measureListRender(state.items)
        } else if (!state.pendingMeasurement) {
            postAdapter.submitList(state.items)
        }
    }

    private fun updateProgressFromState(state: ListUiState?, target: Int) {
        val current = state ?: ListUiState()
        BenchmarkRunHelper.updateProgressCard(
            binding = binding,
            isLoading = current.isLoading,
            progressRun = current.progressRun,
            runCount = current.runCount,
            targetRuns = target,
        )
    }

    /**
     * Skenario Rendering: ukur dari submitList sampai RecyclerView layout selesai.
     * Metrik: wall-clock (nanoTime), CPU% (/proc/self/stat), RSS memori.
     */
    private fun measureListRender(items: List<PostItem>) {
        isMeasuringRender = true
        val cpuBefore = BenchmarkUtils.getProcessCpuTimeMs()
        val startTime = System.nanoTime()

        binding.rvList.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.rvList.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Layout selesai → catat wall-clock, CPU%, RSS.
                    val endTime = System.nanoTime()
                    val cpuAfter = BenchmarkUtils.getProcessCpuTimeMs()
                    val (wallTimeMs, cpuPercent, memoryMb) = BenchmarkUtils.collectMetrics(
                        startTime,
                        endTime,
                        cpuBefore,
                        cpuAfter,
                    )

                    isMeasuringRender = false
                    viewModel.completeListRenderMeasurement(
                        executionTimeMs = wallTimeMs,
                        cpuPercent = cpuPercent,
                        memoryMb = memoryMb,
                    )
                }
            },
        )
        postAdapter.submitList(items)
    }

    private suspend fun runBenchmark() {
        val target = viewModel.targetRunsFor("rendering")
        viewModel.runListMultiple(target)

        val state = viewModel.listState.value ?: ListUiState()
        BenchmarkRunHelper.handleBenchmarkFinished(
            activity = this@ListActivity,
            binding = binding,
            viewModel = viewModel,
            scenarioKey = "rendering",
            scenarioTitle = getString(R.string.scenario_rendering_summary),
            completedRuns = state.runCount,
            targetRuns = target,
            lastExecutionMs = state.executionTimeMs,
            results = viewModel.resultsForScenario("rendering"),
            errorMessage = null,
            resetAction = { viewModel.resetListRuns() },
            runAgain = { runBenchmark() },
        )
    }

    private fun resetRuns() {
        lifecycleScope.launch {
            val state = viewModel.listState.value ?: ListUiState()
            BenchmarkRunHelper.resetScenarioRuns(
                activity = this@ListActivity,
                binding = binding,
                scenarioKey = "rendering",
                scenarioTitle = getString(R.string.scenario_rendering_summary),
                currentRunCount = state.runCount,
                viewModel = viewModel,
                resetAction = { viewModel.resetListRuns() },
            )
        }
    }

    private fun copyResult() {
        val text = viewModel.listLastResultCopyText() ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("benchmark", text))
        Snackbar.make(binding.root, R.string.copied_clipboard, Snackbar.LENGTH_SHORT).show()
    }
}
