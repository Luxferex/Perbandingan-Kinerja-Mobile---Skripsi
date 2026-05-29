import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_settings_provider.dart';
import '../providers/database_provider.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class DatabaseScreen extends StatefulWidget {
  const DatabaseScreen({super.key});

  @override
  State<DatabaseScreen> createState() => _DatabaseScreenState();
}

class _DatabaseScreenState extends State<DatabaseScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<DatabaseProvider>().ensureDatabaseReady();
    });
  }

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

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Gagal reset database: $e')),
      );
    }
  }

  Future<bool> _confirmBenchmark(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Konfirmasi Pengujian'),
          content: const Text(
            'Pengujian akan menjalankan 4 operasi berurutan. '
            'Pastikan tidak ada interupsi. Lanjutkan?',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Batal'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Lanjutkan'),
            ),
          ],
        );
      },
    );

    return confirmed ?? false;
  }

  Future<void> _runBenchmark(BuildContext context) async {
    final provider = context.read<DatabaseProvider>();

    if (!provider.isDatabaseInitialized) {
      final ready = await provider.ensureDatabaseReady();
      if (!ready) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              provider.error ?? 'Database belum siap. Inisialisasi gagal.',
            ),
          ),
        );
        return;
      }
    }

    if (!context.mounted) return;

    final confirmed = await _confirmBenchmark(context);
    if (!confirmed || !context.mounted) return;

    final settings = context.read<BenchmarkSettingsProvider>();
    await provider.runMultiple(settings.sqliteTargetRuns);

    if (!context.mounted) return;

    if (provider.error != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.error!)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario Basis Data SQLite'),
      ),
      body: Consumer2<DatabaseProvider, BenchmarkSettingsProvider>(
        builder: (context, provider, settings, _) {
          final showResults = provider.runCount > 0 && !provider.isLoading;
          final targetRuns = settings.sqliteTargetRuns;

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
                          Text('Target repetisi: $targetRuns'),
                          const SizedBox(height: 4),
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
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
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
                Text(
                  provider.isDatabaseInitialized
                      ? _operationLabel(provider.currentOperation)
                      : 'Menginisialisasi database...',
                ),
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
                  child: Text('Jalankan Benchmark ($targetRuns x)'),
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
