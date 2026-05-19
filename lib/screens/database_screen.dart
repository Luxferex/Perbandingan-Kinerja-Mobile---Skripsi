import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/benchmark_result.dart';
import '../providers/benchmark_summary_provider.dart';
import '../providers/database_provider.dart';

class DatabaseScreen extends StatelessWidget {
  const DatabaseScreen({super.key});

  String _formatMs(double ms) => ms.toStringAsFixed(2);

  String _operationLabel(String operation) {
    switch (operation) {
      case 'clearing':
        return 'Sedang membersihkan data...';
      case 'inserting':
        return 'Sedang insert...';
      case 'selecting':
        return 'Sedang select...';
      case 'updating':
        return 'Sedang update...';
      case 'deleting':
        return 'Sedang delete...';
      default:
        return 'Siap menjalankan benchmark';
    }
  }

  void _showErrorSnackBar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  Future<void> _runBenchmark(BuildContext context) async {
    final databaseProvider = context.read<DatabaseProvider>();
    final previousRunCount = databaseProvider.runCount;

    await databaseProvider.runFullBenchmark();

    if (!context.mounted) return;

    if (databaseProvider.error != null) {
      _showErrorSnackBar(context, databaseProvider.error!);
      return;
    }

    if (databaseProvider.runCount > previousRunCount) {
      context.read<BenchmarkSummaryProvider>().addResult(
            BenchmarkResult(
              scenario: 'sqlite',
              executionTimeMs: databaseProvider.totalTimeMs,
            ),
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario Basis Data SQLite'),
      ),
      body: Consumer<DatabaseProvider>(
        builder: (context, provider, _) {
          final showResults = provider.runCount > 0 && !provider.isLoading;

          return Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Insert 1000 record, SELECT all, UPDATE 500, DELETE 500',
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                if (showResults)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'INSERT: ${_formatMs(provider.insertTimeMs)} ms',
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'SELECT: ${_formatMs(provider.selectTimeMs)} ms '
                            '(${provider.selectedCount} records)',
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'UPDATE: ${_formatMs(provider.updateTimeMs)} ms',
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'DELETE: ${_formatMs(provider.deleteTimeMs)} ms',
                          ),
                          const Divider(),
                          Text(
                            'TOTAL: ${_formatMs(provider.totalTimeMs)} ms',
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                  ),
                if (showResults) const SizedBox(height: 12),
                Text(_operationLabel(provider.currentOperation)),
                if (provider.error != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    provider.error!,
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: provider.isLoading
                      ? null
                      : () => _runBenchmark(context),
                  child: const Text('Jalankan Benchmark'),
                ),
                if (provider.isLoading) ...[
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(),
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}
