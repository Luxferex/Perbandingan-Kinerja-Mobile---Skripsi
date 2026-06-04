package com.benchmark.androidnative.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityScenarioBinding
import com.benchmark.androidnative.ui.adapter.PostListAdapter
import com.benchmark.androidnative.util.BenchmarkUtils
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.benchmark.androidnative.viewmodel.ListUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScenarioBinding
    private lateinit var viewModel: BenchmarkViewModel
    private val postAdapter = PostListAdapter()

    private lateinit var tvTargetRuns: TextView
    private lateinit var tvGenerateTime: TextView
    private lateinit var tvRunCount: TextView
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.list_screen_title)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvDescription.text = getString(R.string.list_card_desc)
        binding.rvList.visibility = View.VISIBLE
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = postAdapter

        setupMetricsViews()

        viewModel.renderingTargetRuns.observe(this) { target ->
            binding.btnRun.text = getString(R.string.run_list, target)
            tvTargetRuns.text = getString(R.string.target_runs, target)
        }

        viewModel.listState.observe(this) { state ->
            updateUi(state)
        }

        binding.btnRun.setOnClickListener {
            val count = viewModel.targetRunsFor("rendering")
            viewModel.runListMultiple(count)
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
        val generateMs = if (state.isGenerated) state.executionTimeMs else 0.0
        tvGenerateTime.text = getString(
            R.string.generate_time,
            BenchmarkUtils.formatMs(generateMs),
        )
        tvRunCount.text = getString(R.string.run_count, state.runCount)

        binding.btnRun.isEnabled = !state.isLoading
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        btnCopy.visibility = if (state.runCount > 0) View.VISIBLE else View.GONE

        postAdapter.submitList(state.items)
    }

    private fun copyResult() {
        val text = viewModel.listLastResultCopyText() ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("benchmark", text))
        Snackbar.make(binding.root, R.string.copied_clipboard, Snackbar.LENGTH_SHORT).show()
    }
}
