import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/database_provider.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class DatabaseScreen extends StatelessWidget {
  const DatabaseScreen({super.key});

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

  Future<void> _resetDatabase(BuildContext context) async {
    final provider = context.read<DatabaseProvider>();

    try {
      await provider.resetDatabase();

      if (!context.mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Database berhasil direset')),
      );
    } catch (e) {
      if (!context.mounted) return;

      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Gagal reset database: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Skenario Basis Data SQLite')),
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
                          Text('INSERT: ${formatMs(provider.insertTimeMs)}'),
                          const SizedBox(height: 4),
                          Text(
                            'SELECT: ${formatMs(provider.selectTimeMs)} '
                            '(${provider.selectedCount} records)',
                          ),
                          const SizedBox(height: 4),
                          Text('UPDATE: ${formatMs(provider.updateTimeMs)}'),
                          const SizedBox(height: 4),
                          Text('DELETE: ${formatMs(provider.deleteTimeMs)}'),
                          const Divider(),
                          Text(
                            'TOTAL: ${formatMs(provider.totalTimeMs)}',
                            style: Theme.of(context).textTheme.titleMedium
                                ?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: () => copyBenchmarkResult(
                              context,
                              text: provider.lastResultCopyText,
                            ),
                            child: const Text('Salin hasil ke clipboard'),
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
                      : provider.runFullBenchmark,
                  child: const Text('Jalankan Benchmark'),
                ),
                if (provider.isLoading) ...[
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(),
                ],
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: provider.isLoading
                      ? null
                      : () => _resetDatabase(context),
                  child: const Text('Reset Database'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
