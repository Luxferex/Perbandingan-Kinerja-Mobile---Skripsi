package com.benchmark.androidnative.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityScenarioBinding
import com.benchmark.androidnative.util.BenchmarkRunHelper
import com.benchmark.androidnative.util.BenchmarkUtils
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.benchmark.androidnative.viewmodel.DatabaseUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SqliteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScenarioBinding
    private lateinit var viewModel: BenchmarkViewModel

    private lateinit var tvTargetRuns: TextView
    private lateinit var tvInsert: TextView
    private lateinit var tvSelect: TextView
    private lateinit var tvUpdate: TextView
    private lateinit var tvDelete: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCopy: MaterialButton
    private lateinit var metricsCard: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScenarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.sqlite_screen_title)
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

        binding.tvDescription.text = getString(R.string.sqlite_card_desc)
        binding.btnResetDatabase.visibility = View.VISIBLE
        metricsCard = binding.cardMetrics

        viewModel.ensureDatabaseReady()

        setupMetricsViews()

        viewModel.sqliteTargetRuns.observe(this) { target ->
            binding.btnRun.text = getString(R.string.run_sqlite, target)
            updateProgressFromState(viewModel.databaseState.value, target)
        }

        viewModel.databaseState.observe(this) { state ->
            updateUi(state)
        }

        binding.btnRun.setOnClickListener {
            runWithConfirmation()
        }

        binding.btnResetRuns.setOnClickListener {
            resetRuns()
        }

        binding.btnResetDatabase.setOnClickListener {
            resetDatabase()
        }
    }

    private fun setupMetricsViews() {
        val container = binding.metricsContainer
        container.removeAllViews()

        tvTargetRuns = addMetricLine(container)
        tvInsert = addMetricLine(container)
        tvSelect = addMetricLine(container)
        tvUpdate = addMetricLine(container)
        tvDelete = addMetricLine(container)

        tvTotal = TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 8 }
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        container.addView(tvTotal)

        btnCopy = MaterialButton(this).apply {
            text = getString(R.string.copy_result)
            setOnClickListener { copyResult() }
        }
        container.addView(btnCopy)
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

    private fun updateUi(state: DatabaseUiState) {
        val target = viewModel.targetRunsFor("sqlite")
        val showResults = state.runCount > 0 && !state.isLoading
        metricsCard.visibility = if (showResults) View.VISIBLE else View.GONE

        if (showResults) {
            tvTargetRuns.text = getString(R.string.target_runs, target)
            tvInsert.text = getString(
                R.string.insert_time,
                BenchmarkUtils.formatMs(state.insertTimeMs),
            )
            tvSelect.text = getString(
                R.string.select_time,
                BenchmarkUtils.formatMs(state.selectTimeMs),
                state.selectedCount,
            )
            tvUpdate.text = getString(
                R.string.update_time,
                BenchmarkUtils.formatMs(state.updateTimeMs),
            )
            tvDelete.text = getString(
                R.string.delete_time,
                BenchmarkUtils.formatMs(state.deleteTimeMs),
            )
            tvTotal.text = getString(
                R.string.total_time,
                BenchmarkUtils.formatMs(state.totalTimeMs),
            )
            btnCopy.visibility = View.VISIBLE
        } else {
            btnCopy.visibility = View.GONE
        }

        binding.tvOperationStatus.visibility = View.VISIBLE
        binding.tvOperationStatus.text = if (state.isDatabaseInitialized) {
            viewModel.operationLabel(state.currentOperation)
        } else {
            getString(R.string.init_database)
        }

        binding.tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE
        binding.tvError.text = state.error

        binding.btnRun.isEnabled = !state.isLoading
        binding.btnResetDatabase.isEnabled = !state.isLoading
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnResetRuns.visibility =
            if (state.runCount > 0 && !state.isLoading) View.VISIBLE else View.GONE

        updateProgressFromState(state, target)
    }

    private fun updateProgressFromState(state: DatabaseUiState?, target: Int) {
        val current = state ?: DatabaseUiState()
        BenchmarkRunHelper.updateProgressCard(
            binding = binding,
            isLoading = current.isLoading,
            progressRun = current.progressRun,
            runCount = current.runCount,
            targetRuns = target,
        )
    }

    private fun runWithConfirmation() {
        val state = viewModel.databaseState.value
        if (state?.isDatabaseInitialized != true) {
            viewModel.ensureDatabaseReady()
            Snackbar.make(
                binding.root,
                state?.error ?: getString(R.string.init_database),
                Snackbar.LENGTH_SHORT,
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_title)
            .setMessage(R.string.confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_action) { _, _ ->
                runBenchmark()
            }
            .show()
    }

    private fun runBenchmark() {
        lifecycleScope.launch { executeBenchmark() }
    }

    private suspend fun executeBenchmark() {
        val target = viewModel.targetRunsFor("sqlite")
        viewModel.runSqliteMultiple(target)

        val state = viewModel.databaseState.value ?: DatabaseUiState()
        BenchmarkRunHelper.handleBenchmarkFinished(
            activity = this@SqliteActivity,
            binding = binding,
            viewModel = viewModel,
            scenarioKey = "sqlite",
            scenarioTitle = getString(R.string.scenario_sqlite_summary),
            completedRuns = state.runCount,
            targetRuns = target,
            lastExecutionMs = state.totalTimeMs,
            results = viewModel.resultsForScenario("sqlite"),
            errorMessage = state.error,
            resetAction = { viewModel.resetDatabaseRuns() },
            runAgain = { runWithConfirmationAndBenchmark() },
        )
    }

    private suspend fun runWithConfirmationAndBenchmark() {
        val state = viewModel.databaseState.value
        if (state?.isDatabaseInitialized != true) {
            viewModel.ensureDatabaseReady()
            return
        }

        val confirmed = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { continuation ->
            AlertDialog.Builder(this@SqliteActivity)
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.confirm_message)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(false) {}
                }
                .setPositiveButton(R.string.continue_action) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(true) {}
                }
                .setOnCancelListener {
                    continuation.resume(false) {}
                }
                .show()
        }

        if (confirmed) {
            executeBenchmark()
        }
    }

    private fun resetRuns() {
        lifecycleScope.launch {
            val state = viewModel.databaseState.value ?: DatabaseUiState()
            BenchmarkRunHelper.resetScenarioRuns(
                activity = this@SqliteActivity,
                binding = binding,
                scenarioKey = "sqlite",
                scenarioTitle = getString(R.string.scenario_sqlite_summary),
                currentRunCount = state.runCount,
                viewModel = viewModel,
                resetAction = { viewModel.resetDatabaseRuns() },
            )
        }
    }

    private fun resetDatabase() {
        try {
            viewModel.resetDatabase()
            Snackbar.make(binding.root, R.string.database_reset_ok, Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                getString(R.string.database_reset_fail, e.message),
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    private fun copyResult() {
        val text = viewModel.databaseLastResultCopyText() ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("benchmark", text))
        Snackbar.make(binding.root, R.string.copied_clipboard, Snackbar.LENGTH_SHORT).show()
    }
}
