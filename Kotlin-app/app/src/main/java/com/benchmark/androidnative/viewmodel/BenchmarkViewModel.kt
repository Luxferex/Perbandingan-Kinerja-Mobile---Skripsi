package com.benchmark.androidnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.benchmark.androidnative.model.BenchmarkResult
import com.benchmark.androidnative.model.PostItem
import com.benchmark.androidnative.repository.BenchmarkRepository
import com.benchmark.androidnative.util.BenchmarkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

data class HttpUiState(
    val posts: List<PostItem> = emptyList(),
    val isLoading: Boolean = false,
    val executionTimeMs: Double = 0.0,
    val runCount: Int = 0,
    val error: String? = null,
    val isWarmedUp: Boolean = false,
)

data class ListUiState(
    val items: List<PostItem> = emptyList(),
    val isGenerated: Boolean = false,
    val isLoading: Boolean = false,
    val executionTimeMs: Double = 0.0,
    val runCount: Int = 0,
)

data class DatabaseUiState(
    val isLoading: Boolean = false,
    val currentOperation: String = "idle",
    val insertTimeMs: Double = 0.0,
    val selectTimeMs: Double = 0.0,
    val updateTimeMs: Double = 0.0,
    val deleteTimeMs: Double = 0.0,
    val totalTimeMs: Double = 0.0,
    val selectedCount: Int = 0,
    val error: String? = null,
    val runCount: Int = 0,
    val isDatabaseInitialized: Boolean = false,
)

class BenchmarkViewModel(
    application: Application,
    private val repository: BenchmarkRepository,
) : AndroidViewModel(application) {

    private val _httpTargetRuns = MutableLiveData(50)
    private val _renderingTargetRuns = MutableLiveData(30)
    private val _sqliteTargetRuns = MutableLiveData(30)

    private val _allResults = MutableLiveData<List<BenchmarkResult>>(emptyList())
    private val _httpState = MutableLiveData(HttpUiState())
    private val _listState = MutableLiveData(ListUiState())
    private val _databaseState = MutableLiveData(DatabaseUiState())

    val httpTargetRuns: LiveData<Int> = _httpTargetRuns
    val renderingTargetRuns: LiveData<Int> = _renderingTargetRuns
    val sqliteTargetRuns: LiveData<Int> = _sqliteTargetRuns
    val allResults: LiveData<List<BenchmarkResult>> = _allResults
    val httpState: LiveData<HttpUiState> = _httpState
    val listState: LiveData<ListUiState> = _listState
    val databaseState: LiveData<DatabaseUiState> = _databaseState

    fun targetRunsFor(scenario: String): Int = when (scenario) {
        "http" -> _httpTargetRuns.value ?: 50
        "rendering" -> _renderingTargetRuns.value ?: 30
        "sqlite" -> _sqliteTargetRuns.value ?: 30
        else -> _httpTargetRuns.value ?: 50
    }

    fun incrementTargetRuns(scenario: String) {
        setTargetRuns(scenario, targetRunsFor(scenario) + 1)
    }

    fun decrementTargetRuns(scenario: String) {
        setTargetRuns(scenario, targetRunsFor(scenario) - 1)
    }

    fun setTargetRuns(scenario: String, value: Int) {
        val clamped = BenchmarkUtils.clampRuns(value)
        when (scenario) {
            "http" -> _httpTargetRuns.value = clamped
            "rendering" -> _renderingTargetRuns.value = clamped
            "sqlite" -> _sqliteTargetRuns.value = clamped
        }
    }

    fun clearAllResults() {
        _allResults.value = emptyList()
    }

    fun getCsvExport(): String = BenchmarkUtils.buildCsvExport(_allResults.value.orEmpty())

    private fun recordResult(result: BenchmarkResult) {
        _allResults.postValue(_allResults.value.orEmpty() + result)
    }

    fun warmUpHttp() {
        val current = _httpState.value ?: HttpUiState()
        if (current.isWarmedUp) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.fetchPostsFromApi()
                _httpState.postValue(current.copy(isWarmedUp = true))
            } catch (_: Exception) {
                // Warm-up failure is non-fatal, same as Flutter.
            }
        }
    }

    fun runHttpMultiple(count: Int) {
        viewModelScope.launch {
            for (i in 0 until count) {
                fetchAndMeasureHttp()
                if (i < count - 1) {
                    delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
                }
            }
        }
    }

    private suspend fun fetchAndMeasureHttp() {
        val current = _httpState.value ?: HttpUiState()
        _httpState.value = current.copy(isLoading = true, error = null)

        val start = System.nanoTime() / 1000
        try {
            val posts = withContext(Dispatchers.IO) {
                repository.fetchPostsFromApi()
            }
            val end = System.nanoTime() / 1000
            val executionMs = BenchmarkUtils.elapsedMs(start, end)
            val newRunCount = current.runCount + 1
            val result = BenchmarkResult(
                scenario = "http",
                executionTimeMs = executionMs,
                timestamp = Date(),
            )
            recordResult(result)
            _httpState.value = current.copy(
                posts = posts,
                isLoading = false,
                executionTimeMs = executionMs,
                runCount = newRunCount,
                error = null,
            )
        } catch (e: Exception) {
            _httpState.value = current.copy(
                isLoading = false,
                error = e.message ?: e.toString(),
                posts = emptyList(),
            )
        }
    }

    fun httpLastResultCopyText(): String? {
        val state = _httpState.value ?: return null
        if (state.runCount == 0) return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "HTTP",
            runNumber = state.runCount,
            executionTimeMs = state.executionTimeMs,
            timestamp = Date(),
        )
    }

    fun runListMultiple(count: Int) {
        viewModelScope.launch {
            val current = _listState.value ?: ListUiState()
            _listState.value = current.copy(isLoading = true)
            try {
                for (i in 0 until count) {
                    generateAndMeasureList()
                    if (i < count - 1) {
                        delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
                    }
                }
            } finally {
                _listState.value = _listState.value?.copy(isLoading = false)
            }
        }
    }

    private fun generateAndMeasureList() {
        val current = _listState.value ?: ListUiState()
        val start = System.nanoTime() / 1000
        val entities = BenchmarkUtils.generateDummyPosts(BenchmarkUtils.BENCHMARK_ITEM_COUNT)
        val items = entities.map { entity ->
            PostItem(
                id = entity.id,
                userId = entity.userId,
                title = entity.title,
                body = entity.body,
            )
        }
        val end = System.nanoTime() / 1000
        val executionMs = BenchmarkUtils.elapsedMs(start, end)
        val newRunCount = current.runCount + 1
        recordResult(
            BenchmarkResult(
                scenario = "rendering",
                executionTimeMs = executionMs,
                timestamp = Date(),
            ),
        )
        _listState.value = current.copy(
            items = items,
            isGenerated = true,
            executionTimeMs = executionMs,
            runCount = newRunCount,
        )
    }

    fun listLastResultCopyText(): String? {
        val state = _listState.value ?: return null
        if (state.runCount == 0) return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "Rendering",
            runNumber = state.runCount,
            executionTimeMs = state.executionTimeMs,
            timestamp = Date(),
        )
    }

    fun ensureDatabaseReady() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllPostsFromDb()
                _databaseState.postValue(
                    (_databaseState.value ?: DatabaseUiState()).copy(
                        isDatabaseInitialized = true,
                        error = null,
                    ),
                )
            } catch (e: Exception) {
                _databaseState.postValue(
                    (_databaseState.value ?: DatabaseUiState()).copy(
                        isDatabaseInitialized = false,
                        error = e.message,
                    ),
                )
            }
        }
    }

    fun runSqliteMultiple(count: Int) {
        viewModelScope.launch {
            for (i in 0 until count) {
                runFullSqliteBenchmark()
                if (i < count - 1) {
                    delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
                }
            }
        }
    }

    private suspend fun runFullSqliteBenchmark() {
        var state = _databaseState.value ?: DatabaseUiState()
        state = state.copy(
            isLoading = true,
            error = null,
            insertTimeMs = 0.0,
            selectTimeMs = 0.0,
            updateTimeMs = 0.0,
            deleteTimeMs = 0.0,
            totalTimeMs = 0.0,
            selectedCount = 0,
            currentOperation = "idle",
        )
        postDatabaseState(state)

        try {
            withContext(Dispatchers.IO) {
                state = state.copy(currentOperation = "clearing", isDatabaseInitialized = true)
                postDatabaseState(state)
                repository.clearDatabase()

                val dummyPosts = BenchmarkUtils.generateDummyPosts(
                    BenchmarkUtils.BENCHMARK_ITEM_COUNT,
                )

                state = state.copy(currentOperation = "inserting")
                postDatabaseState(state)
                val insertStart = System.nanoTime() / 1000
                repository.insertPostsToDb(dummyPosts)
                val insertEnd = System.nanoTime() / 1000
                val insertMs = BenchmarkUtils.elapsedMs(insertStart, insertEnd)

                state = state.copy(currentOperation = "selecting", insertTimeMs = insertMs)
                postDatabaseState(state)
                val selectStart = System.nanoTime() / 1000
                val selected = repository.getAllPostsFromDb()
                val selectEnd = System.nanoTime() / 1000
                val selectMs = BenchmarkUtils.elapsedMs(selectStart, selectEnd)

                state = state.copy(
                    currentOperation = "updating",
                    selectTimeMs = selectMs,
                    selectedCount = selected.size,
                )
                postDatabaseState(state)
                val updateStart = System.nanoTime() / 1000
                repository.updateHalf()
                val updateEnd = System.nanoTime() / 1000
                val updateMs = BenchmarkUtils.elapsedMs(updateStart, updateEnd)

                state = state.copy(currentOperation = "deleting", updateTimeMs = updateMs)
                postDatabaseState(state)
                val deleteStart = System.nanoTime() / 1000
                repository.deleteHalf()
                val deleteEnd = System.nanoTime() / 1000
                val deleteMs = BenchmarkUtils.elapsedMs(deleteStart, deleteEnd)

                val totalMs = insertMs + selectMs + updateMs + deleteMs
                val newRunCount = state.runCount + 1
                recordResult(
                    BenchmarkResult(
                        scenario = "sqlite",
                        executionTimeMs = totalMs,
                        timestamp = Date(),
                    ),
                )
                state = state.copy(
                    isLoading = false,
                    currentOperation = "idle",
                    deleteTimeMs = deleteMs,
                    totalTimeMs = totalMs,
                    runCount = newRunCount,
                )
                postDatabaseState(state)
            }
        } catch (e: Exception) {
            postDatabaseState(
                state.copy(
                    isLoading = false,
                    currentOperation = "idle",
                    error = e.message ?: e.toString(),
                ),
            )
        }
    }

    private fun postDatabaseState(state: DatabaseUiState) {
        _databaseState.postValue(state)
    }

    fun resetDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearDatabase()
            _databaseState.postValue(DatabaseUiState(isDatabaseInitialized = true))
        }
    }

    fun databaseLastResultCopyText(): String? {
        val state = _databaseState.value ?: return null
        if (state.runCount == 0) return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "SQLite",
            runNumber = state.runCount,
            executionTimeMs = state.totalTimeMs,
            timestamp = Date(),
        )
    }

    fun operationLabel(operation: String): String = when (operation) {
        "clearing" -> "Sedang membersihkan data..."
        "inserting" -> "Sedang insert..."
        "selecting" -> "Sedang select..."
        "updating" -> "Sedang update..."
        "deleting" -> "Sedang delete..."
        else -> "Siap menjalankan benchmark"
    }
}
