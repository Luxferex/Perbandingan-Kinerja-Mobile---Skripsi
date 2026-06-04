package com.benchmark.androidnative.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    private fun buildFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "benchmark_hasil_$timestamp.csv"
    }

    fun saveCsvToDevice(context: Context, csvContent: String): String {
        val fileName = buildFileName()
        var directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(null)
            ?: context.filesDir

        if (directory?.path?.contains("cache") == true) {
            val external = context.getExternalFilesDir(null)
            if (external != null) {
                val downloadsDir = File(external, "Download")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                directory = downloadsDir
            }
        }

        val file = File(directory, fileName)
        file.writeText(csvContent)
        return file.absolutePath
    }

    fun shareCsv(context: Context, filePath: String) {
        val file = File(filePath)
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hasil Benchmark Kotlin")
            putExtra(
                Intent.EXTRA_TEXT,
                "Export hasil pengujian kinerja untuk analisis Python",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan CSV"))
    }
}
