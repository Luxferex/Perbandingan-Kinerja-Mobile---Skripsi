import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_summary_provider.dart';

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
          title: const Text('Export CSV'),
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
          ],
        );
      },
    );
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
                    child: FilledButton(
                      onPressed: () {
                        _showExportDialog(
                          context,
                          provider.getCsvExport(),
                        );
                      },
                      child: const Text('Export CSV'),
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: () {
                    provider.clearAll();
                  },
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
