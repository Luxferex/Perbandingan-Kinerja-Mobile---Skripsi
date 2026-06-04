package com.benchmark.androidnative.ui

import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.benchmark.androidnative.R
import com.benchmark.androidnative.databinding.ActivityPreTestBinding

class PreTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreTestBinding
    private val checklist = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val items = listOf(
            "wifi" to R.string.check_wifi,
            "brightness" to R.string.check_brightness,
            "background" to R.string.check_background,
            "developer" to R.string.check_developer,
            "battery" to R.string.check_battery,
            "warmup" to R.string.check_warmup,
        )

        items.forEach { (key, labelRes) ->
            checklist[key] = false
            val checkBox = CheckBox(this).apply {
                text = getString(labelRes)
                setPadding(8, 16, 8, 16)
                setOnCheckedChangeListener { _, isChecked ->
                    checklist[key] = isChecked
                    updateStartButton()
                }
            }
            binding.checklistContainer.addView(
                checkBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        binding.btnStart.setOnClickListener {
            android.widget.Toast.makeText(
                this,
                R.string.pre_test_done,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    private fun updateStartButton() {
        binding.btnStart.isEnabled = checklist.values.all { it }
    }
}
