import 'package:flutter/foundation.dart';

import '../models/post_model.dart';
import '../services/http_service.dart';

class HttpProvider extends ChangeNotifier {
  final HttpService _httpService;

  List<PostModel> _posts = [];
  bool _isLoading = false;
  double _executionTimeMs = 0;
  int _runCount = 0;
  String? _error;

  HttpProvider({HttpService? httpService})
      : _httpService = httpService ?? HttpService();

  List<PostModel> get posts => List.unmodifiable(_posts);
  bool get isLoading => _isLoading;
  double get executionTimeMs => _executionTimeMs;
  int get runCount => _runCount;
  String? get error => _error;

  Future<void> fetchAndMeasure() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    final stopwatch = Stopwatch()..start();

    try {
      _posts = await _httpService.fetchPosts();
      stopwatch.stop();
      _executionTimeMs = stopwatch.elapsedMicroseconds / 1000;
      _runCount++;
    } catch (e) {
      stopwatch.stop();
      _error = e.toString();
      _posts = [];
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void reset() {
    _posts = [];
    _isLoading = false;
    _executionTimeMs = 0;
    _runCount = 0;
    _error = null;
    notifyListeners();
  }
}
