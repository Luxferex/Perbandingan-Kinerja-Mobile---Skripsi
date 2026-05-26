import 'package:flutter/foundation.dart';

class BenchmarkSettingsProvider extends ChangeNotifier {
  static const int minRuns = 5;
  static const int maxRuns = 100;

  String _activeScenario = 'http';
  int _httpTargetRuns = 50;
  int _sqliteTargetRuns = 30;
  int _renderingTargetRuns = 30;

  String get activeScenario => _activeScenario;
  int get httpTargetRuns => _httpTargetRuns;
  int get sqliteTargetRuns => _sqliteTargetRuns;
  int get renderingTargetRuns => _renderingTargetRuns;

  int get targetRuns => targetRunsFor(_activeScenario);

  int targetRunsFor(String scenario) {
    switch (scenario) {
      case 'http':
        return _httpTargetRuns;
      case 'sqlite':
        return _sqliteTargetRuns;
      case 'rendering':
        return _renderingTargetRuns;
      default:
        return _httpTargetRuns;
    }
  }

  void setActiveScenario(String scenario) {
    _activeScenario = scenario;
    notifyListeners();
  }

  void incrementTargetRuns([String? scenario]) {
    final key = scenario ?? _activeScenario;
    final current = targetRunsFor(key);
    if (current >= maxRuns) return;
    _setTargetRuns(key, current + 1);
  }

  void decrementTargetRuns([String? scenario]) {
    final key = scenario ?? _activeScenario;
    final current = targetRunsFor(key);
    if (current <= minRuns) return;
    _setTargetRuns(key, current - 1);
  }

  void setTargetRuns(String scenario, int value) {
    _setTargetRuns(scenario, value.clamp(minRuns, maxRuns));
  }

  void _setTargetRuns(String scenario, int value) {
    switch (scenario) {
      case 'http':
        _httpTargetRuns = value;
      case 'sqlite':
        _sqliteTargetRuns = value;
      case 'rendering':
        _renderingTargetRuns = value;
    }
    notifyListeners();
  }
}
