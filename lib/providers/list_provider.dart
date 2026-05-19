import 'package:flutter/foundation.dart';

import '../models/post_model.dart';

class ListProvider extends ChangeNotifier {
  List<PostModel> _items = [];
  bool _isGenerated = false;
  double _executionTimeMs = 0;
  final int _itemCount = 1000;

  List<PostModel> get items => List.unmodifiable(_items);
  bool get isGenerated => _isGenerated;
  double get executionTimeMs => _executionTimeMs;
  int get itemCount => _itemCount;

  Future<void> generateAndMeasure() async {
    final stopwatch = Stopwatch()..start();

    _items = List.generate(_itemCount, (index) {
      final id = index + 1;
      return PostModel(
        id: id,
        userId: 1,
        title: 'Item $id',
        body: 'Body text for item $id',
      );
    });

    stopwatch.stop();
    _executionTimeMs = stopwatch.elapsedMicroseconds / 1000;
    _isGenerated = true;
    notifyListeners();
  }

  void reset() {
    _items = [];
    _isGenerated = false;
    _executionTimeMs = 0;
    notifyListeners();
  }
}
