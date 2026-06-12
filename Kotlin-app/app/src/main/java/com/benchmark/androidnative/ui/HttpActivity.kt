package com.benchmark.androidnative.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityScenarioBinding
import com.benchmark.androidnative.ui.adapter.PostListAdapter
import com.benchmark.androidnative.util.BenchmarkRunHelper
import com.benchmark.androidnative.util.BenchmarkUtils
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.benchmark.androidnative.viewmodel.HttpUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HttpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScenarioBinding
    private lateinit var viewModel: BenchmarkViewModel
    private val postAdapter = PostListAdapter()

    private lateinit var tvTargetRuns: TextView
    private lateinit var tvExecutionTime: TextView
    private lateinit var tvRunCount: TextView
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.http_screen_title)
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

        binding.tvDescription.text = getString(R.string.http_card_desc)
        binding.rvList.visibility = View.VISIBLE
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = postAdapter

        setupMetricsViews()

        binding.btnRun.isEnabled = false
        viewModel.warmUpHttp()

        viewModel.httpTargetRuns.observe(this) { target ->
            binding.btnRun.text = getString(R.string.run_http, target)
            tvTargetRuns.text = getString(R.string.target_runs, target)
            updateProgressFromState(viewModel.httpState.value, target)
        }

        viewModel.httpState.observe(this) { state ->
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
        tvExecutionTime = addMetricLine(container)
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

    private fun updateUi(state: HttpUiState) {
        val target = viewModel.targetRunsFor("http")
        val executionMs = if (state.runCount > 0) state.executionTimeMs else 0.0
        tvExecutionTime.text = getString(
            R.string.execution_time,
            BenchmarkUtils.formatMs(executionMs),
        )
        tvRunCount.text = getString(R.string.run_count, state.runCount)

        binding.tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE
        binding.tvError.text = state.error

        binding.btnRun.isEnabled = state.isWarmedUp && !state.isLoading
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnResetRuns.visibility =
            if (state.runCount > 0 && !state.isLoading) View.VISIBLE else View.GONE

        btnCopy.visibility = if (state.runCount > 0) View.VISIBLE else View.GONE

        updateProgressFromState(state, target)
        postAdapter.submitList(state.posts)
    }

    private fun updateProgressFromState(state: HttpUiState?, target: Int) {
        val current = state ?: HttpUiState()
        BenchmarkRunHelper.updateProgressCard(
            binding = binding,
            isLoading = current.isLoading,
            progressRun = current.progressRun,
            runCount = current.runCount,
            targetRuns = target,
        )
    }

    private suspend fun runBenchmark() {
        val target = viewModel.targetRunsFor("http")
        viewModel.runHttpMultiple(target)

        val state = viewModel.httpState.value ?: HttpUiState()
        BenchmarkRunHelper.handleBenchmarkFinished(
            activity = this@HttpActivity,
            binding = binding,
            viewModel = viewModel,
            scenarioKey = "http",
            scenarioTitle = getString(R.string.scenario_http_summary),
            completedRuns = state.runCount,
            targetRuns = target,
            lastExecutionMs = state.executionTimeMs,
            results = viewModel.resultsForScenario("http"),
            errorMessage = state.error,
            resetAction = { viewModel.resetHttpRuns() },
            runAgain = { runBenchmark() },
        )
    }

    private fun resetRuns() {
        lifecycleScope.launch {
            val state = viewModel.httpState.value ?: HttpUiState()
            BenchmarkRunHelper.resetScenarioRuns(
                activity = this@HttpActivity,
                binding = binding,
                scenarioKey = "http",
                scenarioTitle = getString(R.string.scenario_http_summary),
                currentRunCount = state.runCount,
                viewModel = viewModel,
                resetAction = { viewModel.resetHttpRuns() },
            )
        }
    }

    private fun copyResult() {
        val text = viewModel.httpLastResultCopyText() ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("benchmark", text))
        Snackbar.make(binding.root, R.string.copied_clipboard, Snackbar.LENGTH_SHORT).show()
    }
}
