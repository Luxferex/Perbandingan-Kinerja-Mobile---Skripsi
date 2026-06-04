import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../models/benchmark_result.dart';
import 'benchmark_utils.dart';

/// Bangun konten CSV benchmark dengan kolom standar penelitian.
String buildBenchmarkCsvExport(List<BenchmarkResult> results) {
  final buffer = StringBuffer(
    'run,framework,scenario,execution_time_ms,cpu_percent,memory_mb,timestamp\n',
  );

  for (var i = 0; i < results.length; i++) {
    final result = results[i];
    buffer.writeln(
      '${i + 1},'
      'flutter,'
      '${result.scenario},'
      '${result.executionTimeMs.toStringAsFixed(2)},'
      '${result.cpuPercent.toStringAsFixed(1)},'
      '${result.memoryMb.toStringAsFixed(1)},'
      '${formatCsvTimestamp(result.timestamp)}',
    );
  }

  return buffer.toString();
}

String _buildFileName() {
  final timestamp = DateTime.now();
  return 'benchmark_hasil_${timestamp.year}'
      '${timestamp.month.toString().padLeft(2, '0')}'
      '${timestamp.day.toString().padLeft(2, '0')}_'
      '${timestamp.hour.toString().padLeft(2, '0')}'
      '${timestamp.minute.toString().padLeft(2, '0')}'
      '${timestamp.second.toString().padLeft(2, '0')}.csv';
}

/// Simpan CSV langsung ke folder Downloads (atau dokumen app jika Downloads tidak tersedia).
Future<String> saveCsvToDevice(String csvContent) async {
  final fileName = _buildFileName();
  Directory? directory = await getDownloadsDirectory();

  directory ??= await getApplicationDocumentsDirectory();

  if (Platform.isAndroid) {
    final external = await getExternalStorageDirectory();
    if (directory.path.contains('cache') && external != null) {
      final downloadsSubdir = Directory(p.join(external.path, 'Download'));
      if (!downloadsSubdir.existsSync()) {
        downloadsSubdir.createSync(recursive: true);
      }
      directory = downloadsSubdir;
    }
  }

  final filePath = p.join(directory.path, fileName);
  await File(filePath).writeAsString(csvContent);
  return filePath;
}

/// Simpan ke perangkat lalu buka menu bagikan (opsional ke Drive, WhatsApp, dll).
Future<String> exportCsvToFile(String csvContent) async {
  final filePath = await saveCsvToDevice(csvContent);
  final fileName = p.basename(filePath);

  await Share.shareXFiles(
    [XFile(filePath, mimeType: 'text/csv', name: fileName)],
    subject: 'Hasil Benchmark Flutter',
    text: 'Export hasil pengujian kinerja untuk analisis Python',
  );

  return filePath;
}

void showCsvSavedSnackBar(BuildContext context, String filePath) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(
        'CSV disimpan:\n$filePath\n'
        'Buka File Manager → Downloads untuk analisis di Python.',
      ),
      duration: const Duration(seconds: 6),
      action: SnackBarAction(
        label: 'OK',
        onPressed: () {},
      ),
    ),
  );
}

void showCsvExportSnackBar(BuildContext context, String filePath) {
  showCsvSavedSnackBar(context, filePath);
}
