import 'package:flutter/foundation.dart';

import '../models/post_model.dart';
import '../services/database_service.dart';

class DatabaseProvider extends ChangeNotifier {
  static const int _benchmarkItemCount = 1000;

  final DatabaseService _databaseService;

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

  DatabaseProvider({DatabaseService? databaseService})
      : _databaseService = databaseService ?? DatabaseService();

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

  List<PostModel> _generateDummyPosts(int count) {
    return List.generate(count, (index) {
      final id = index + 1;
      return PostModel(
        id: id,
        userId: 1,
        title: 'Item $id',
        body: 'Body text for item $id',
      );
    });
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
      await _databaseService.initDatabase();

      _currentOperation = 'clearing';
      notifyListeners();
      await _databaseService.clearAll();

      final dummyPosts = _generateDummyPosts(_benchmarkItemCount);

      _currentOperation = 'inserting';
      notifyListeners();
      final insertStopwatch = Stopwatch()..start();
      await _databaseService.insertBatch(dummyPosts);
      insertStopwatch.stop();
      _insertTimeMs = insertStopwatch.elapsedMicroseconds / 1000;
      notifyListeners();

      _currentOperation = 'selecting';
      notifyListeners();
      final selectStopwatch = Stopwatch()..start();
      final selected = await _databaseService.selectAll();
      selectStopwatch.stop();
      _selectTimeMs = selectStopwatch.elapsedMicroseconds / 1000;
      _selectedCount = selected.length;
      notifyListeners();

      _currentOperation = 'updating';
      notifyListeners();
      final updateStopwatch = Stopwatch()..start();
      await _databaseService.updateHalf();
      updateStopwatch.stop();
      _updateTimeMs = updateStopwatch.elapsedMicroseconds / 1000;
      notifyListeners();

      _currentOperation = 'deleting';
      notifyListeners();
      final deleteStopwatch = Stopwatch()..start();
      await _databaseService.deleteHalf();
      deleteStopwatch.stop();
      _deleteTimeMs = deleteStopwatch.elapsedMicroseconds / 1000;

      _totalTimeMs =
          _insertTimeMs + _selectTimeMs + _updateTimeMs + _deleteTimeMs;
      _runCount++;
      _currentOperation = 'idle';
    } catch (e) {
      _error = e.toString();
      _currentOperation = 'idle';
    } finally {
      _isLoading = false;
      notifyListeners();
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
    notifyListeners();
  }
}
