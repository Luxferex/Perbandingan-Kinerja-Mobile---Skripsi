package com.benchmark.androidnative.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.benchmark.androidnative.BenchmarkApplication
import com.benchmark.androidnative.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: com.benchmark.androidnative.viewmodel.BenchmarkViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = (application as BenchmarkApplication).benchmarkViewModel

        binding.toolbar.title = getString(com.benchmark.androidnative.R.string.app_title)

        binding.btnHttpBenchmark.setOnClickListener {
            startActivity(Intent(this, HttpActivity::class.java))
        }
        binding.btnRenderBenchmark.setOnClickListener {
            startActivity(Intent(this, RenderActivity::class.java))
        }
        binding.btnSqliteBenchmark.setOnClickListener {
            startActivity(Intent(this, SqliteActivity::class.java))
        }
        binding.btnExportCsv.setOnClickListener {
            viewModel.exportAllResults(this)
        }

        viewModel.statusMessage.observe(this) { message ->
            binding.tvStatus.text = message
        }

        viewModel.exportPathEvent.observe(this) { path ->
            if (!path.isNullOrBlank()) {
                Toast.makeText(this, path, Toast.LENGTH_LONG).show()
            }
        }
    }
}
