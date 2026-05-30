package com.benchmark.androidnative.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.benchmark.androidnative.database.PostEntity
import com.benchmark.androidnative.repository.BenchmarkRepository
import com.benchmark.androidnative.util.BenchmarkLogger
import com.benchmark.androidnative.util.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BenchmarkViewModel(
    application: Application,
    private val repository: BenchmarkRepository,
    private val logger: BenchmarkLogger = BenchmarkLogger()
) : AndroidViewModel(application) {

    val httpResults = MutableLiveData<List<BenchmarkResult>>(emptyList())
    val renderResults = MutableLiveData<List<BenchmarkResult>>(emptyList())
    val sqliteResults = MutableLiveData<List<BenchmarkResult>>(emptyList())
    val isLoading = MutableLiveData(false)
    val statusMessage = MutableLiveData("Siap menjalankan benchmark")
    val exportPathEvent = MutableLiveData<String>()

    fun runHttpBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            try {
                repeat(3) { index ->
                    statusMessage.postValue("HTTP warm-up ${index + 1}/3")
                    repository.fetchPostsFromApi()
                }

                val results = mutableListOf<BenchmarkResult>()
                repeat(50) { index ->
                    val runNumber = index + 1
                    statusMessage.postValue("HTTP benchmark run $runNumber/50")

                    logger.startTimer()
                    repository.fetchPostsFromApi()
                    val executionMs = logger.stopTimer()
                    val cpuPercent = logger.getCurrentCpuPercent()
                    val memoryMB = logger.getCurrentMemoryMB(getApplication())

                    val result = BenchmarkResult(
                        scenario = "HTTP",
                        run = runNumber,
                        executionMs = executionMs,
                        cpuPercent = cpuPercent,
                        memoryMB = memoryMB
                    )
                    results.add(result)
                    logger.addResult(result)
                    httpResults.postValue(results.toList())
                }

                statusMessage.postValue("HTTP benchmark selesai (50 run)")
            } catch (e: Exception) {
                statusMessage.postValue("HTTP benchmark gagal: ${e.message}")
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun runSqliteBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            try {
                val testPosts = createTestPosts(1000)
                val results = mutableListOf<BenchmarkResult>()

                repeat(30) { index ->
                    val runNumber = index + 1
                    statusMessage.postValue("SQLite benchmark run $runNumber/30")

                    repository.clearDatabase()

                    logger.startTimer()
                    repository.insertPostsToDb(testPosts)
                    val allPosts = repository.getAllPostsFromDb()
                    val postsToUpdate = allPosts.map { post ->
                        post.copy(title = "${post.title} (updated)")
                    }
                    repository.updatePosts(postsToUpdate)
                    repository.deletePosts(allPosts)
                    val executionMs = logger.stopTimer()

                    val cpuPercent = logger.getCurrentCpuPercent()
                    val memoryMB = logger.getCurrentMemoryMB(getApplication())

                    val result = BenchmarkResult(
                        scenario = "SQLite",
                        run = runNumber,
                        executionMs = executionMs,
                        cpuPercent = cpuPercent,
                        memoryMB = memoryMB
                    )
                    results.add(result)
                    logger.addResult(result)
                    sqliteResults.postValue(results.toList())
                }

                statusMessage.postValue("SQLite benchmark selesai (30 run)")
            } catch (e: Exception) {
                statusMessage.postValue("SQLite benchmark gagal: ${e.message}")
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun runRenderBenchmark() {
        statusMessage.postValue(
            "Benchmark rendering dijalankan dari RenderActivity saat auto-scroll"
        )
    }

    fun recordRenderSample(context: Context, second: Int): BenchmarkResult {
        val result = BenchmarkResult(
            scenario = "Render",
            run = second,
            executionMs = 1000.0,
            cpuPercent = logger.getCurrentCpuPercent(),
            memoryMB = logger.getCurrentMemoryMB(context)
        )
        logger.addResult(result)
        return result
    }

    fun setRenderResults(results: List<BenchmarkResult>) {
        renderResults.postValue(results)
        statusMessage.postValue("Render benchmark selesai (${results.size} sampel)")
    }

    fun exportAllResults(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = mergeAndExportToCsv(context)
            exportPathEvent.postValue(path)
            statusMessage.postValue("Hasil diekspor ke: $path")
        }
    }

    private suspend fun mergeAndExportToCsv(context: Context): String = withContext(Dispatchers.IO) {
        val allResults = buildList {
            addAll(httpResults.value.orEmpty())
            addAll(renderResults.value.orEmpty())
            addAll(sqliteResults.value.orEmpty())
        }
        logger.clearResults()
        allResults.forEach { logger.addResult(it) }
        logger.exportToCsv(context)
    }

    private fun createTestPosts(count: Int): List<PostEntity> {
        return (1..count).map { index ->
            PostEntity(
                id = index,
                userId = (index % 10) + 1,
                title = "Benchmark post $index",
                body = "Body content for benchmark post $index"
            )
        }
    }
}
