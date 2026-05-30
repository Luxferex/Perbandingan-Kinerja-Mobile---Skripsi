package com.benchmark.androidnative.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ItemResultBinding
import com.benchmark.androidnative.util.BenchmarkResult

class ResultAdapter : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    private val results = mutableListOf<BenchmarkResult>()

    fun submitList(newResults: List<BenchmarkResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    class ResultViewHolder(
        private val binding: ItemResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: BenchmarkResult) {
            binding.tvRun.text = binding.root.context.getString(
                R.string.run_label,
                result.run
            )
            binding.tvMetrics.text = binding.root.context.getString(
                R.string.run_metrics_format,
                result.executionMs,
                result.cpuPercent,
                result.memoryMB
            )
        }
    }
}
