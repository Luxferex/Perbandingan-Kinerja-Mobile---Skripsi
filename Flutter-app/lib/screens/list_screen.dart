import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_settings_provider.dart';
import '../providers/list_provider.dart';
import '../utils/benchmark_run_helper.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class ListScreen extends StatelessWidget {
  const ListScreen({super.key});

  Future<void> _runBenchmark(BuildContext context) async {
    final settings = context.read<BenchmarkSettingsProvider>();
    final provider = context.read<ListProvider>();
    final targetRuns = settings.renderingTargetRuns;

    await provider.runMultiple(targetRuns);

    if (!context.mounted) return;

    await handleBenchmarkFinished(
      context,
      scenarioKey: 'rendering',
      scenarioTitle: 'Rendering Daftar',
      completedRuns: provider.runCount,
      targetRuns: targetRuns,
      lastExecutionMs: provider.executionTimeMs,
      results: provider.results,
      resetProvider: provider.reset,
      runAgain: () => _runBenchmark(context),
    );
  }

  Future<void> _resetRuns(BuildContext context) async {
    final provider = context.read<ListProvider>();
    await resetScenarioRuns(
      context,
      scenarioKey: 'rendering',
      scenarioTitle: 'Rendering Daftar',
      currentRunCount: provider.runCount,
      resetProvider: provider.reset,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario Rendering Daftar'),
        actions: [
          IconButton(
            tooltip: 'Reset run ke 0',
            onPressed: () => _resetRuns(context),
            icon: const Icon(Icons.restart_alt),
          ),
        ],
      ),
      body: Consumer2<ListProvider, BenchmarkSettingsProvider>(
        builder: (context, provider, settings, _) {
          final generateMs =
              provider.isGenerated ? provider.executionTimeMs : 0.0;
          final targetRuns = settings.renderingTargetRuns;

          return Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Render 1000 item dummy',
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                BenchmarkRunProgressCard(
                  isLoading: provider.isLoading,
                  progressRun: provider.progressRun,
                  runCount: provider.runCount,
                  targetRuns: targetRuns,
                ),
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Target repetisi: $targetRuns'),
                        const SizedBox(height: 4),
                        Text(
                          'Waktu Generate: ${formatMs(generateMs)}',
                        ),
                        const SizedBox(height: 4),
                        Text('Jumlah run: ${provider.runCount}'),
                        if (provider.runCount > 0) ...[
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: () => copyBenchmarkResult(
                              context,
                              text: provider.lastResultCopyText,
                            ),
                            child: const Text('Salin hasil ke clipboard'),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: provider.isLoading
                      ? null
                      : () => _runBenchmark(context),
                  child: Text('Generate & Render ($targetRuns x)'),
                ),
                if (provider.runCount > 0 && !provider.isLoading) ...[
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: () => _resetRuns(context),
                    icon: const Icon(Icons.restart_alt),
                    label: const Text('Reset Run ke 0'),
                  ),
                ],
                const SizedBox(height: 12),
                Expanded(
                  child: ListView.builder(
                    itemCount: provider.items.length,
                    itemBuilder: (context, index) {
                      final item = provider.items[index];
                      return ListTile(
                        leading: CircleAvatar(
                          child: Text('${index + 1}'),
                        ),
                        title: Text(item.title),
                        subtitle: Text(item.body),
                      );
                    },
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
