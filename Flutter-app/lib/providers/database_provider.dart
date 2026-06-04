import 'dart:io';

import 'package:flutter/foundation.dart';

import '../models/benchmark_result.dart';
import '../services/cpu_service.dart';
import '../services/database_service.dart';
import '../utils/benchmark_utils.dart';

class DatabaseProvider extends ChangeNotifier {
  static const int _benchmarkItemCount = 1000;

  final DatabaseService _databaseService;
  final void Function(BenchmarkResult)? onResultRecorded;

  bool _isLoading = false;
  String _currentOperation = 'idle';
  double _insertTimeMs = 0;
  double _selectTimeMs = 0;
  double _updateTimeMs = 0;
  double _deleteTimeMs = 0;
  double _totalTimeMs = 0;
  int _selectedCount = 0;
  String? _error;
  int _runCount = 0;
  bool _isDatabaseInitialized = false;
  final List<BenchmarkResult> _results = [];

  DatabaseProvider({
    DatabaseService? databaseService,
    this.onResultRecorded,
  }) : _databaseService = databaseService ?? DatabaseService();

  bool get isLoading => _isLoading;
  String get currentOperation => _currentOperation;
  double get insertTimeMs => _insertTimeMs;
  double get selectTimeMs => _selectTimeMs;
  double get updateTimeMs => _updateTimeMs;
  double get deleteTimeMs => _deleteTimeMs;
  double get totalTimeMs => _totalTimeMs;
  int get selectedCount => _selectedCount;
  String? get error => _error;
  int get runCount => _runCount;
  List<BenchmarkResult> get results => List.unmodifiable(_results);
  bool get isDatabaseInitialized => _isDatabaseInitialized;

  String? get lastResultCopyText {
    if (_results.isEmpty) return null;
    final last = _results.last;
    return formatResultLogLine(
      scenarioLabel: 'SQLite',
      runNumber: _runCount,
      executionTimeMs: last.executionTimeMs,
      timestamp: last.timestamp,
    );
  }

  void _recordResult(BenchmarkResult result) {
    _results.add(result);
    onResultRecorded?.call(result);
  }

  Future<bool> ensureDatabaseReady() async {
    try {
      await _databaseService.initDatabase();
      _isDatabaseInitialized = true;
      _error = null;
      notifyListeners();
      return true;
    } catch (e) {
      _isDatabaseInitialized = false;
      _error = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<void> runMultiple(int count) async {
    for (var i = 0; i < count; i++) {
      await runFullBenchmark();
      if (i < count - 1) {
        await Future.delayed(const Duration(milliseconds: 500));
      }
    }
  }

  Future<double> _runTimedOperation(Future<void> Function() operation) async {
    final startTime = DateTime.now().microsecondsSinceEpoch;
    await operation();
    final endTime = DateTime.now().microsecondsSinceEpoch;
    return elapsedMs(startTime, endTime);
  }

  Future<void> runFullBenchmark() async {
    _isLoading = true;
    _error = null;
    _insertTimeMs = 0;
    _selectTimeMs = 0;
    _updateTimeMs = 0;
    _deleteTimeMs = 0;
    _totalTimeMs = 0;
    _selectedCount = 0;
    _currentOperation = 'idle';
    notifyListeners();

    try {
      final cpuBefore = await CpuService.getCpuTimeNanos();

      await _databaseService.initDatabase();
      _isDatabaseInitialized = true;

      _currentOperation = 'clearing';
      notifyListeners();
      await _databaseService.clearAll();

      final dummyPosts = generateDummyPosts(_benchmarkItemCount);

      _currentOperation = 'inserting';
      notifyListeners();
      _insertTimeMs = await _runTimedOperation(
        () => _databaseService.insertBatch(dummyPosts),
      );
      notifyListeners();

      _currentOperation = 'selecting';
      notifyListeners();
      final selectStart = DateTime.now().microsecondsSinceEpoch;
      final selected = await _databaseService.selectAll();
      final selectEnd = DateTime.now().microsecondsSinceEpoch;
      _selectTimeMs = elapsedMs(selectStart, selectEnd);
      _selectedCount = selected.length;
      notifyListeners();

      _currentOperation = 'updating';
      notifyListeners();
      _updateTimeMs = await _runTimedOperation(
        () async => _databaseService.updateHalf(),
      );
      notifyListeners();

      _currentOperation = 'deleting';
      notifyListeners();
      _deleteTimeMs = await _runTimedOperation(
        () async => _databaseService.deleteHalf(),
      );

      _totalTimeMs =
          _insertTimeMs + _selectTimeMs + _updateTimeMs + _deleteTimeMs;
      final cpuAfter = await CpuService.getCpuTimeNanos();
      final cpuPercent = CpuService.calculateCpuPercent(
        cpuAfter - cpuBefore,
        _totalTimeMs,
      );
      final memoryMb = ProcessInfo.currentRss / (1024 * 1024);
      _runCount++;
      _currentOperation = 'idle';

      _recordResult(
        BenchmarkResult(
          scenario: 'sqlite',
          executionTimeMs: _totalTimeMs,
          cpuPercent: cpuPercent,
          memoryMb: memoryMb,
          timestamp: DateTime.now(),
        ),
      );
    } catch (e) {
      _error = e.toString();
      _currentOperation = 'idle';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> resetDatabase() async {
    try {
      await _databaseService.initDatabase();
      _isDatabaseInitialized = true;
      await _databaseService.clearAll();
      reset();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  void reset() {
    _isLoading = false;
    _currentOperation = 'idle';
    _insertTimeMs = 0;
    _selectTimeMs = 0;
    _updateTimeMs = 0;
    _deleteTimeMs = 0;
    _totalTimeMs = 0;
    _selectedCount = 0;
    _error = null;
    _runCount = 0;
    _isDatabaseInitialized = false;
    _results.clear();
    notifyListeners();
  }
}
