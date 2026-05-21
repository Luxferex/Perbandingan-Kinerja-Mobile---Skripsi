import '../models/post_model.dart';

/// Format angka ms dengan 2 desimal.
String formatMs(double ms) => '${ms.toStringAsFixed(2)} ms';

/// Generate [count] PostModel dummy untuk benchmark.
List<PostModel> generateDummyPosts(int count) {
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

/// Format timestamp untuk log penelitian.
String formatTimestamp(DateTime dt) =>
    '${dt.hour}:${dt.minute}:${dt.second}.${dt.millisecond}';

/// Hitung selisih waktu dalam milidetik (desimal).
double elapsedMs(int startMicroseconds, int endMicroseconds) {
  return (endMicroseconds - startMicroseconds) / 1000.0;
}

/// Format baris hasil untuk clipboard.
String formatResultLogLine({
  required String scenarioLabel,
  required int runNumber,
  required double executionTimeMs,
  required DateTime timestamp,
}) {
  return '$scenarioLabel | Run $runNumber | '
      '${executionTimeMs.toStringAsFixed(2)} ms | '
      '[${formatTimestamp(timestamp)}]';
}
