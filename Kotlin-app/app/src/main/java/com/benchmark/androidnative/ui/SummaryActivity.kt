package com.benchmark.androidnative.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivitySummaryBinding
import com.benchmark.androidnative.model.BenchmarkResult
import com.benchmark.androidnative.util.BenchmarkUtils
import com.benchmark.androidnative.util.CsvExportHelper
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var viewModel: BenchmarkViewModel

    private val scenarioTitles = mapOf(
        "http" to R.string.scenario_http_summary,
        "rendering" to R.string.scenario_rendering_summary,
        "sqlite" to R.string.scenario_sqlite_summary,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        viewModel.allResults.observe(this) { results ->
            renderResults(results)
        }
    }

    private fun renderResults(results: List<BenchmarkResult>) {
        binding.contentContainer.removeAllViews()

        if (results.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.GONE
            return
        }

        binding.tvEmpty.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        scenarioTitles.forEach { (scenario, titleRes) ->
            val scenarioResults = results.filter { it.scenario == scenario }
            if (scenarioResults.isEmpty()) return@forEach

            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = 24 }
            }

            val cardContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }

            cardContent.addView(
                TextView(this).apply {
                    text = getString(titleRes)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                },
            )

            cardContent.addView(buildResultsTable(scenarioResults))
            card.addView(cardContent)
            binding.contentContainer.addView(card)
        }

        val csv = viewModel.getCsvExport()
        binding.contentContainer.addView(buildExportCard(csv))

        binding.contentContainer.addView(
            MaterialButton(this).apply {
                text = getString(R.string.clear_all_data)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 16 }
                setOnClickListener { viewModel.clearAllResults() }
            },
        )
    }

    private fun buildResultsTable(results: List<BenchmarkResult>): TableLayout {
        return TableLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 24 }

            addView(
                TableRow(this@SummaryActivity).apply {
                    addView(headerCell(getString(R.string.table_run)))
                    addView(headerCell(getString(R.string.table_time)))
                    addView(headerCell("CPU %"))
                    addView(headerCell("Mem MB"))
                },
            )

            results.forEach { result ->
                addView(
                    TableRow(this@SummaryActivity).apply {
                        addView(dataCell("${result.run}"))
                        addView(
                            dataCell(
                                String.format(Locale.US, "%.2f", result.executionTimeMs),
                            ),
                        )
                        addView(
                            dataCell(
                                String.format(Locale.US, "%.1f", result.cpuPercent),
                            ),
                        )
                        addView(
                            dataCell(
                                String.format(Locale.US, "%.1f", result.memoryMb),
                            ),
                        )
                    },
                )
            }
        }
    }

    private fun headerCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(16, 8, 16, 8)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun dataCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(16, 8, 16, 8)
        }
    }

    private fun buildExportCard(csv: String): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 16 }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        content.addView(
            MaterialButton(this).apply {
                text = getString(R.string.save_csv)
                setOnClickListener { saveCsv(csv) }
            },
        )
        content.addView(spacer())
        content.addView(
            MaterialButton(this).apply {
                text = getString(R.string.share_csv)
                setOnClickListener { shareCsv(csv) }
            },
        )
        content.addView(spacer())
        content.addView(
            MaterialButton(this).apply {
                text = getString(R.string.preview_csv)
                setOnClickListener { previewCsv(csv) }
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.csv_export_note)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setPadding(0, 24, 0, 0)
            },
        )

        card.addView(content)
        return card
    }

    private fun spacer(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                16,
            )
        }
    }

    private fun saveCsv(csv: String) {
        try {
            val path = CsvExportHelper.saveCsvToDevice(this, csv)
            Snackbar.make(
                binding.root,
                getString(R.string.csv_saved, path),
                Snackbar.LENGTH_LONG,
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                getString(R.string.csv_save_fail, e.message),
                Snackbar.LENGTH_LONG,
            ).show()
        }
    }

    private fun shareCsv(csv: String) {
        try {
            val path = CsvExportHelper.saveCsvToDevice(this, csv)
            CsvExportHelper.shareCsv(this, path)
            Snackbar.make(
                binding.root,
                getString(R.string.csv_share_ready, path),
                Snackbar.LENGTH_LONG,
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                getString(R.string.csv_share_fail, e.message),
                Snackbar.LENGTH_LONG,
            ).show()
        }
    }

    private fun previewCsv(csv: String) {
        val scroll = android.widget.ScrollView(this)
        val textView = TextView(this).apply {
            text = csv
            setTextIsSelectable(true)
            setPadding(32, 32, 32, 32)
        }
        scroll.addView(textView)

        AlertDialog.Builder(this)
            .setTitle(R.string.csv_preview_title)
            .setView(scroll)
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.copy) { _, _ ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("csv", csv))
                Snackbar.make(binding.root, R.string.csv_copied, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }
}
