package com.benchmark.androidnative.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityMainBinding
import com.benchmark.androidnative.databinding.IncludeScenarioCardBinding
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BenchmarkViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(R.string.home_title)

        RepetitionRowBinder(
            binding.rowHttp,
            "http",
            viewModel,
            viewModel.httpTargetRuns,
            this,
        ).bind(R.string.repetition_http)

        RepetitionRowBinder(
            binding.rowRendering,
            "rendering",
            viewModel,
            viewModel.renderingTargetRuns,
            this,
        ).bind(R.string.repetition_rendering)

        RepetitionRowBinder(
            binding.rowSqlite,
            "sqlite",
            viewModel,
            viewModel.sqliteTargetRuns,
            this,
        ).bind(R.string.repetition_sqlite)

        binding.btnPreTest.setOnClickListener {
            startActivity(Intent(this, PreTestActivity::class.java))
        }

        setupScenarioCard(
            binding.cardHttp,
            R.drawable.ic_wifi,
            R.string.http_title,
            R.string.http_description,
            HttpActivity::class.java,
        )
        setupScenarioCard(
            binding.cardRendering,
            R.drawable.ic_list,
            R.string.rendering_title,
            R.string.rendering_description,
            ListActivity::class.java,
        )
        setupScenarioCard(
            binding.cardSqlite,
            R.drawable.ic_storage,
            R.string.sqlite_title,
            R.string.sqlite_description,
            SqliteActivity::class.java,
        )

        binding.btnSummary.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
    }

    private fun setupScenarioCard(
        card: IncludeScenarioCardBinding,
        iconRes: Int,
        titleRes: Int,
        descriptionRes: Int,
        activityClass: Class<*>,
    ) {
        card.ivIcon.setImageResource(iconRes)
        card.tvTitle.setText(titleRes)
        card.tvDescription.setText(descriptionRes)
        card.btnStart.setOnClickListener {
            startActivity(Intent(this, activityClass))
        }
    }
}
