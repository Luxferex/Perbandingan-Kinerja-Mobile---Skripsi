package com.benchmark.androidnative.ui

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.benchmark.androidnative.databinding.ViewRepetitionRowBinding
import com.benchmark.androidnative.viewmodel.BenchmarkViewModel

class RepetitionRowBinder(
    private val binding: ViewRepetitionRowBinding,
    private val scenario: String,
    private val viewModel: BenchmarkViewModel,
    private val runsLiveData: LiveData<Int>,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var updatingFromModel = false

    fun bind(labelRes: Int) {
        binding.tvLabel.setText(labelRes)

        runsLiveData.observe(lifecycleOwner) { count ->
            if (!binding.etCount.isFocused) {
                updatingFromModel = true
                binding.etCount.setText(count.toString())
                updatingFromModel = false
            }
        }

        binding.btnDecrease.setOnClickListener {
            viewModel.decrementTargetRuns(scenario)
        }
        binding.btnIncrease.setOnClickListener {
            viewModel.incrementTargetRuns(scenario)
        }

        binding.etCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updatingFromModel) return
                val parsed = s?.toString()?.trim()?.toIntOrNull() ?: return
                viewModel.setTargetRuns(scenario, parsed)
            }
        })

        binding.etCount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updatingFromModel = true
                binding.etCount.setText(viewModel.targetRunsFor(scenario).toString())
                updatingFromModel = false
            }
        }
    }
}
