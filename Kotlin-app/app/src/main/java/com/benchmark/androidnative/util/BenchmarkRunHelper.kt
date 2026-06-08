package com.benchmark.androidnative.util

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityScenarioBinding
import com.benchmark.androidnative.model.BenchmarkResult
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

enum class BenchmarkCompleteAction {
    RUN_AGAIN,
    RESET,
    DISMISS,
}

data class BenchmarkRunStats(
    val averageMs: Double,
    val minMs: Double,
    val maxMs: Double,
) {
    companion object {
        fun fromResults(results: List<BenchmarkResult>): BenchmarkRunStats {
            if (results.isEmpty()) {
                return BenchmarkRunStats(0.0, 0.0, 0.0)
            }
            val times = results.map { it.executionTimeMs }
            val total = times.sum()
            return BenchmarkRunStats(
                averageMs = total / times.size,
                minMs = times.min(),
                maxMs = times.max(),
            )
        }
    }
}

object BenchmarkRunHelper {

    fun updateProgressCard(
        binding: ActivityScenarioBinding,
        isLoading: Boolean,
        progressRun: Int,
        runCount: Int,
        targetRuns: Int,
    ) {
        val activeRun = if (isLoading) progressRun else runCount
        val showProgress = isLoading || runCount > 0

        binding.cardProgress.visibility = if (showProgress) View.VISIBLE else View.GONE
        if (!showProgress) return

        binding.tvProgressStatus.text = when {
            isLoading -> binding.root.context.getString(
                R.string.progress_running,
                progressRun,
                targetRuns,
            )
            runCount > 0 -> binding.root.context.getString(
                R.string.progress_finished,
                runCount,
                targetRuns,
            )
            else -> binding.root.context.getString(R.string.progress_none)
        }

        val progress = if (targetRuns > 0) {
            min(1f, activeRun.toFloat() / targetRuns.toFloat())
        } else {
            0f
        }
        binding.progressRuns.isIndeterminate = false
        binding.progressRuns.setProgressCompat((progress * 100).toInt(), true)
        binding.progressRuns.visibility = View.VISIBLE
    }

    suspend fun confirmResetBenchmarkRunsAsync(
        activity: AppCompatActivity,
        scenarioTitle: String,
        currentRunCount: Int,
    ): Boolean {
        if (currentRunCount == 0) {
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                R.string.reset_no_runs,
                Snackbar.LENGTH_SHORT,
            ).show()
            return false
        }

        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            AlertDialog.Builder(activity)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(
                    activity.getString(
                        R.string.reset_confirm_message,
                        scenarioTitle,
                        currentRunCount,
                    ),
                )
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(false) {}
                }
                .setPositiveButton(R.string.reset_action) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(true) {}
                }
                .setOnCancelListener {
                    continuation.resume(false) {}
                }
                .show()
        }
    }

    suspend fun resetScenarioRuns(
        activity: AppCompatActivity,
        binding: ActivityScenarioBinding,
        scenarioKey: String,
        scenarioTitle: String,
        currentRunCount: Int,
        viewModel: BenchmarkViewModel,
        resetAction: () -> Unit,
    ) {
        val confirmed = confirmResetBenchmarkRunsAsync(
            activity,
            scenarioTitle,
            currentRunCount,
        )
        if (!confirmed || activity.isFinishing) return

        resetAction()

        Snackbar.make(
            binding.root,
            activity.getString(R.string.reset_done, scenarioTitle),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    fun showBenchmarkProgressSnackBar(
        binding: ActivityScenarioBinding,
        scenarioTitle: String,
        completedRuns: Int,
        targetRuns: Int,
    ) {
        Snackbar.make(
            binding.root,
            binding.root.context.getString(
                R.string.benchmark_progress_snackbar,
                scenarioTitle,
                completedRuns,
                targetRuns,
            ),
            Snackbar.LENGTH_LONG,
        ).show()
    }

    fun showBenchmarkCompleteDialog(
        activity: AppCompatActivity,
        scenarioTitle: String,
        completedRuns: Int,
        targetRuns: Int,
        lastExecutionMs: Double,
        stats: BenchmarkRunStats,
        errorMessage: String?,
        onAction: (BenchmarkCompleteAction) -> Unit,
    ) {
        val hasError = !errorMessage.isNullOrEmpty()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * activity.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
        }

        if (hasError) {
            content.addView(TextView(activity).apply { text = errorMessage })
        } else {
            content.addView(
                TextView(activity).apply {
                    text = activity.getString(
                        R.string.benchmark_complete_summary,
                        scenarioTitle,
                        completedRuns,
                        targetRuns,
                    )
                },
            )
            content.addView(statRow(activity, R.string.stat_last_run, BenchmarkUtils.formatMs(lastExecutionMs)))
            if (completedRuns > 1) {
                content.addView(statRow(activity, R.string.stat_average, BenchmarkUtils.formatMs(stats.averageMs)))
                content.addView(statRow(activity, R.string.stat_fastest, BenchmarkUtils.formatMs(stats.minMs)))
                content.addView(statRow(activity, R.string.stat_slowest, BenchmarkUtils.formatMs(stats.maxMs)))
            }
            content.addView(
                TextView(activity).apply {
                    val top = (8 * activity.resources.displayMetrics.density).toInt()
                    setPadding(0, top, 0, 0)
                    text = activity.getString(R.string.benchmark_complete_prompt)
                },
            )
        }

        val builder = AlertDialog.Builder(activity)
            .setTitle(if (hasError) R.string.benchmark_failed_title else R.string.benchmark_complete_title)
            .setView(content)
            .setCancelable(false)

        if (!hasError) {
            builder
                .setNegativeButton(R.string.reset_run) { _, _ ->
                    onAction(BenchmarkCompleteAction.RESET)
                }
                .setPositiveButton(R.string.run_again) { _, _ ->
                    onAction(BenchmarkCompleteAction.RUN_AGAIN)
                }
        }

        builder.setNeutralButton(
            if (hasError) R.string.close else R.string.done,
        ) { _, _ ->
            onAction(BenchmarkCompleteAction.DISMISS)
        }

        builder.show()
    }

    suspend fun handleBenchmarkFinished(
        activity: AppCompatActivity,
        binding: ActivityScenarioBinding,
        viewModel: BenchmarkViewModel,
        scenarioKey: String,
        scenarioTitle: String,
        completedRuns: Int,
        targetRuns: Int,
        lastExecutionMs: Double,
        results: List<BenchmarkResult>,
        errorMessage: String?,
        resetAction: () -> Unit,
        runAgain: suspend () -> Unit,
    ) {
        if (activity.isFinishing) return

        if (errorMessage.isNullOrEmpty()) {
            showBenchmarkProgressSnackBar(
                binding,
                scenarioTitle,
                completedRuns,
                targetRuns,
            )
        }

        val action = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            showBenchmarkCompleteDialog(
                activity = activity,
                scenarioTitle = scenarioTitle,
                completedRuns = completedRuns,
                targetRuns = targetRuns,
                lastExecutionMs = lastExecutionMs,
                stats = BenchmarkRunStats.fromResults(results),
                errorMessage = errorMessage,
            ) { selected ->
                continuation.resume(selected) {}
            }
        }

        if (activity.isFinishing) return

        when (action) {
            BenchmarkCompleteAction.RUN_AGAIN -> runAgain()
            BenchmarkCompleteAction.RESET -> resetScenarioRuns(
                activity = activity,
                binding = binding,
                scenarioKey = scenarioKey,
                scenarioTitle = scenarioTitle,
                currentRunCount = completedRuns,
                viewModel = viewModel,
                resetAction = resetAction,
            )
            BenchmarkCompleteAction.DISMISS -> Unit
        }
    }

    private fun statRow(activity: Context, labelRes: Int, value: String): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            val bottom = (4 * activity.resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, bottom)
            addView(
                TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = activity.getString(labelRes)
                },
            )
            addView(
                TextView(activity).apply {
                    text = value
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                },
            )
        }
    }
}
