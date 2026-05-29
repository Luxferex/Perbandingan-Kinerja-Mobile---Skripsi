import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_summary_provider.dart';
import '../utils/csv_export_helper.dart';

class SummaryScreen extends StatelessWidget {
  const SummaryScreen({super.key});

  static const _scenarios = <String, String>{
    'http': 'HTTP Request',
    'rendering': 'Rendering Daftar',
    'sqlite': 'Basis Data SQLite',
  };

  String _formatMs(double ms) => ms.toStringAsFixed(2);

  void _showExportDialog(BuildContext context, String csv) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Pratinjau CSV'),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: SelectableText(csv),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Tutup'),
            ),
            TextButton(
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: csv));
                if (dialogContext.mounted) {
                  ScaffoldMessenger.of(dialogContext).showSnackBar(
                    const SnackBar(content: Text('CSV disalin ke clipboard')),
                  );
                }
              },
              child: const Text('Salin'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _saveCsvToDevice(BuildContext context, String csv) async {
    try {
      final filePath = await saveCsvToDevice(csv);
      if (!context.mounted) return;
      showCsvSavedSnackBar(context, filePath);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Gagal menyimpan CSV: $e')),
      );
    }
  }

  Future<void> _shareCsv(BuildContext context, String csv) async {
    try {
      final filePath = await exportCsvToFile(csv);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('File siap dibagikan:\n$filePath'),
          duration: const Duration(seconds: 4),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Gagal membagikan CSV: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ringkasan Hasil'),
      ),
      body: Consumer<BenchmarkSummaryProvider>(
        builder: (context, provider, _) {
          if (provider.allResults.isEmpty) {
            return const Center(
              child: Text('Belum ada data pengujian'),
            );
          }

          final csv = provider.getCsvExport();

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (final entry in _scenarios.entries) ...[
                  Builder(
                    builder: (context) {
                      final scenarioResults = provider.allResults
                          .where((result) => result.scenario == entry.key)
                          .toList();

                      if (scenarioResults.isEmpty) {
                        return const SizedBox.shrink();
                      }

                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Card(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  entry.value,
                                  style: Theme.of(context)
                                      .textTheme
                                      .titleMedium,
                                ),
                                const SizedBox(height: 8),
                                DataTable(
                                  columns: const [
                                    DataColumn(label: Text('Run')),
                                    DataColumn(label: Text('Waktu (ms)')),
                                  ],
                                  rows: [
                                    for (var i = 0;
                                        i < scenarioResults.length;
                                        i++)
                                      DataRow(
                                        cells: [
                                          DataCell(Text('${i + 1}')),
                                          DataCell(
                                            Text(
                                              _formatMs(
                                                scenarioResults[i]
                                                    .executionTimeMs,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ],
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        FilledButton.icon(
                          onPressed: () => _saveCsvToDevice(context, csv),
                          icon: const Icon(Icons.save_alt),
                          label: const Text('Simpan CSV ke Perangkat'),
                        ),
                        const SizedBox(height: 8),
                        OutlinedButton.icon(
                          onPressed: () => _shareCsv(context, csv),
                          icon: const Icon(Icons.share),
                          label: const Text('Bagikan CSV'),
                        ),
                        const SizedBox(height: 8),
                        OutlinedButton.icon(
                          onPressed: () => _showExportDialog(context, csv),
                          icon: const Icon(Icons.visibility),
                          label: const Text('Pratinjau CSV'),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'File CSV disimpan di folder Downloads (atau dokumen aplikasi). '
                          'Salin ke PC via USB atau Google Drive untuk uji normalitas di Python.',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: provider.clearAll,
                  child: const Text('Hapus Semua Data'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
