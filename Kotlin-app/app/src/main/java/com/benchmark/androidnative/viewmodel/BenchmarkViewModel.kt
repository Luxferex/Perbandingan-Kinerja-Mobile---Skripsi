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
import kotlinx.coroutines.CompletableDeferred
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
    val progressRun: Int = 0,
    val error: String? = null,
    val isWarmedUp: Boolean = false,
)

data class ListUiState(
    val items: List<PostItem> = emptyList(),
    val isGenerated: Boolean = false,
    val isLoading: Boolean = false,
    val executionTimeMs: Double = 0.0,
    val runCount: Int = 0,
    val progressRun: Int = 0,
    val pendingMeasurement: Boolean = false,
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
    val progressRun: Int = 0,
    val isDatabaseInitialized: Boolean = false,
)

class BenchmarkViewModel(
    application: Application,
    private val repository: BenchmarkRepository,
) : AndroidViewModel(application) {

    private val _httpTargetRuns = MutableLiveData(50)
    private val _renderingTargetRuns = MutableLiveData(30)
    private val _sqliteTargetRuns = MutableLiveData(30)

    private var nextRunNumber = 1
    private var listMeasurementDeferred: CompletableDeferred<Unit>? = null

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
        nextRunNumber = 1
        _allResults.value = emptyList()
    }

    fun clearScenario(scenario: String) {
        _allResults.value = _allResults.value.orEmpty().filter { it.scenario != scenario }
        renumberResultsGlobally()
    }

    private fun renumberResultsGlobally() {
        val renumbered = _allResults.value.orEmpty().mapIndexed { index, result ->
            result.copy(run = index + 1)
        }
        _allResults.value = renumbered
        nextRunNumber = renumbered.size + 1
    }

    fun resultsForScenario(scenario: String): List<BenchmarkResult> =
        _allResults.value.orEmpty().filter { it.scenario == scenario }

    fun resetHttpRuns() {
        clearScenario("http")
        val warmedUp = _httpState.value?.isWarmedUp ?: false
        _httpState.value = HttpUiState(isWarmedUp = warmedUp)
    }

    fun resetListRuns() {
        clearScenario("rendering")
        _listState.value = ListUiState()
    }

    fun resetDatabaseRuns() {
        clearScenario("sqlite")
        val initialized = _databaseState.value?.isDatabaseInitialized ?: false
        _databaseState.value = DatabaseUiState(isDatabaseInitialized = initialized)
    }

    fun getCsvExport(): String = BenchmarkUtils.buildCsvExport(_allResults.value.orEmpty())

    private fun recordResult(
        scenario: String,
        executionTimeMs: Double,
        cpuPercent: Double,
        memoryMb: Double,
        timestamp: Date = Date(),
    ) {
        val result = BenchmarkResult(
            run = nextRunNumber++,
            framework = "kotlin",
            scenario = scenario,
            executionTimeMs = executionTimeMs,
            cpuPercent = cpuPercent,
            memoryMb = memoryMb,
            timestamp = timestamp,
        )
        _allResults.postValue(_allResults.value.orEmpty() + result)
    }

    fun warmUpHttp() {
        val current = _httpState.value ?: HttpUiState()
        if (current.isWarmedUp) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nonce = "${System.nanoTime()}_warmup"
                repository.fetchPostsFromApi(nonce = nonce)
                // Warm-up sukses → izinkan tombol Run.
                _httpState.postValue(
                    current.copy(isWarmedUp = true, error = null),
                )
            } catch (e: Exception) {
                // Warm-up gagal: tetap buka tombol Run supaya user bisa coba lagi,
                // tapi tampilkan error (sering SSL proxy WiFi kampus).
                _httpState.postValue(
                    current.copy(
                        isWarmedUp = true,
                        error = "Warm-up gagal: ${e.message ?: e}",
                    ),
                )
            }
        }
    }

    suspend fun runHttpMultiple(count: Int) {
        for (i in 0 until count) {
            val current = _httpState.value ?: HttpUiState()
            _httpState.value = current.copy(progressRun = i + 1)
            fetchAndMeasureHttp()
            if (i < count - 1) {
                delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
            }
        }
        val finished = _httpState.value ?: HttpUiState()
        _httpState.value = finished.copy(progressRun = 0)
    }

    /** Skenario HTTP: fetch API + ukur wall-clock, CPU%, RSS. */
    private suspend fun fetchAndMeasureHttp() {
        val current = _httpState.value ?: HttpUiState()
        _httpState.value = current.copy(isLoading = true, error = null)

        try {
            // Cache-busting nonce (unik per run) agar request benar-benar fresh.
            val nonce = "${System.nanoTime()}_${current.runCount + 1}"
            val (posts, executionMs, cpuPercent) = withContext(Dispatchers.IO) {
                // Snapshot CPU + wall-clock sebelum request.
                val cpuBefore = BenchmarkUtils.getProcessCpuTimeMs()
                val startTime = System.nanoTime()
                val result = repository.fetchPostsFromApi(nonce = nonce)
                val endTime = System.nanoTime()
                val cpuAfter = BenchmarkUtils.getProcessCpuTimeMs()
                // Execution time = wall-clock; CPU% = Δ CPU / wall-clock.
                val wallTimeMs = BenchmarkUtils.wallTimeMs(startTime, endTime)
                val cpuPercent = BenchmarkUtils.calculateCpuPercent(
                    cpuBefore,
                    cpuAfter,
                    wallTimeMs,
                )
                Triple(result, wallTimeMs, cpuPercent)
            }
            // Memori RSS setelah operasi selesai.
            val memoryMb = BenchmarkUtils.getMemoryRssMb()
            val newRunCount = current.runCount + 1
            recordResult(
                scenario = "http",
                executionTimeMs = executionMs,
                cpuPercent = cpuPercent,
                memoryMb = memoryMb,
            )
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
        val last = _allResults.value.orEmpty().lastOrNull { it.scenario == "http" } ?: return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "HTTP",
            runNumber = last.run,
            executionTimeMs = last.executionTimeMs,
            cpuPercent = last.cpuPercent,
            memoryMb = last.memoryMb,
            timestamp = last.timestamp,
        )
    }

    suspend fun runListMultiple(count: Int) {
        val current = _listState.value ?: ListUiState()
        _listState.value = current.copy(isLoading = true)
        try {
            for (i in 0 until count) {
                _listState.value = (_listState.value ?: ListUiState()).copy(progressRun = i + 1)
                prepareAndAwaitListRender()
                if (i < count - 1) {
                    delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
                }
            }
        } finally {
            _listState.value = _listState.value?.copy(isLoading = false, progressRun = 0)
        }
    }

    private suspend fun prepareAndAwaitListRender() {
        val current = _listState.value ?: ListUiState()
        val deferred = CompletableDeferred<Unit>()
        listMeasurementDeferred = deferred

        val entities = BenchmarkUtils.generateDummyPosts(BenchmarkUtils.BENCHMARK_ITEM_COUNT)
        val items = entities.map { entity ->
            PostItem(
                id = entity.id,
                userId = entity.userId,
                title = entity.title,
                body = entity.body,
            )
        }

        _listState.value = current.copy(
            items = items,
            isGenerated = true,
            pendingMeasurement = true,
        )
        deferred.await()
    }

    fun completeListRenderMeasurement(
        executionTimeMs: Double,
        cpuPercent: Double,
        memoryMb: Double,
    ) {
        val current = _listState.value ?: return
        val newRunCount = current.runCount + 1
        recordResult(
            scenario = "rendering",
            executionTimeMs = executionTimeMs,
            cpuPercent = cpuPercent,
            memoryMb = memoryMb,
        )
        _listState.value = current.copy(
            executionTimeMs = executionTimeMs,
            runCount = newRunCount,
            pendingMeasurement = false,
        )
        listMeasurementDeferred?.complete(Unit)
    }

    fun listLastResultCopyText(): String? {
        val state = _listState.value ?: return null
        if (state.runCount == 0) return null
        val last = _allResults.value.orEmpty().lastOrNull { it.scenario == "rendering" } ?: return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "Rendering",
            runNumber = last.run,
            executionTimeMs = last.executionTimeMs,
            cpuPercent = last.cpuPercent,
            memoryMb = last.memoryMb,
            timestamp = last.timestamp,
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

    suspend fun runSqliteMultiple(count: Int) {
        for (i in 0 until count) {
            val current = _databaseState.value ?: DatabaseUiState()
            postDatabaseState(current.copy(progressRun = i + 1))
            runFullSqliteBenchmark()
            if (i < count - 1) {
                delay(BenchmarkUtils.INTER_RUN_DELAY_MS)
            }
        }
        val finished = _databaseState.value ?: DatabaseUiState()
        postDatabaseState(finished.copy(progressRun = 0))
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
                repository.clearDatabase()

                val dummyPosts = BenchmarkUtils.generateDummyPosts(
                    BenchmarkUtils.BENCHMARK_ITEM_COUNT,
                )

                // Snapshot CPU + wall-clock sebelum rangkaian CRUD SQLite.
                val cpuBefore = BenchmarkUtils.getProcessCpuTimeMs()
                val startTime = System.nanoTime()

                // Per-operasi (untuk tampilan UI); total tetap dari startTime→endTime.
                val insertStart = System.nanoTime() / 1000
                repository.insertPostsToDb(dummyPosts)
                val insertEnd = System.nanoTime() / 1000
                val insertMs = BenchmarkUtils.elapsedMs(insertStart, insertEnd)

                val selectStart = System.nanoTime() / 1000
                val selected = repository.getAllPostsFromDb()
                val selectEnd = System.nanoTime() / 1000
                val selectMs = BenchmarkUtils.elapsedMs(selectStart, selectEnd)

                val updateStart = System.nanoTime() / 1000
                repository.updateHalf()
                val updateEnd = System.nanoTime() / 1000
                val updateMs = BenchmarkUtils.elapsedMs(updateStart, updateEnd)

                val deleteStart = System.nanoTime() / 1000
                repository.deleteHalf()
                val deleteEnd = System.nanoTime() / 1000
                val deleteMs = BenchmarkUtils.elapsedMs(deleteStart, deleteEnd)

                // 3 metrik: total wall-clock, CPU%, RSS.
                val endTime = System.nanoTime()
                val cpuAfter = BenchmarkUtils.getProcessCpuTimeMs()
                val (totalMs, cpuPercent, memoryMb) = BenchmarkUtils.collectMetrics(
                    startTime,
                    endTime,
                    cpuBefore,
                    cpuAfter,
                )

                val newRunCount = state.runCount + 1
                recordResult(
                    scenario = "sqlite",
                    executionTimeMs = totalMs,
                    cpuPercent = cpuPercent,
                    memoryMb = memoryMb,
                )
                state = state.copy(
                    isLoading = false,
                    currentOperation = "idle",
                    isDatabaseInitialized = true,
                    insertTimeMs = insertMs,
                    selectTimeMs = selectMs,
                    updateTimeMs = updateMs,
                    deleteTimeMs = deleteMs,
                    totalTimeMs = totalMs,
                    selectedCount = selected.size,
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
        val last = _allResults.value.orEmpty().lastOrNull { it.scenario == "sqlite" } ?: return null
        return BenchmarkUtils.formatResultLogLine(
            scenarioLabel = "SQLite",
            runNumber = last.run,
            executionTimeMs = last.executionTimeMs,
            cpuPercent = last.cpuPercent,
            memoryMb = last.memoryMb,
            timestamp = last.timestamp,
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
