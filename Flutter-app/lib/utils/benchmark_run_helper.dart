import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/benchmark_result.dart';
import '../providers/benchmark_summary_provider.dart';
import 'benchmark_utils.dart';

enum BenchmarkCompleteAction { runAgain, reset, dismiss }

class BenchmarkRunStats {
  const BenchmarkRunStats({
    required this.averageMs,
    required this.minMs,
    required this.maxMs,
  });

  final double averageMs;
  final double minMs;
  final double maxMs;

  factory BenchmarkRunStats.fromResults(List<BenchmarkResult> results) {
    if (results.isEmpty) {
      return const BenchmarkRunStats(averageMs: 0, minMs: 0, maxMs: 0);
    }
    final times = results.map((r) => r.executionTimeMs).toList();
    final total = times.fold<double>(0, (sum, ms) => sum + ms);
    return BenchmarkRunStats(
      averageMs: total / times.length,
      minMs: times.reduce((a, b) => a < b ? a : b),
      maxMs: times.reduce((a, b) => a > b ? a : b),
    );
  }
}

Future<bool> confirmResetBenchmarkRuns(
  BuildContext context, {
  required String scenarioTitle,
  required int currentRunCount,
}) async {
  if (currentRunCount == 0) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Belum ada run yang perlu direset.')),
    );
    return false;
  }

  final confirmed = await showDialog<bool>(
    context: context,
    builder: (dialogContext) {
      return AlertDialog(
        title: const Text('Reset Run?'),
        content: Text(
          'Run $scenarioTitle akan dikembalikan ke 0 dan '
          '$currentRunCount hasil pengujian skenario ini dihapus dari ringkasan.\n\n'
          'Lanjutkan?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Batal'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Reset'),
          ),
        ],
      );
    },
  );

  return confirmed ?? false;
}

Future<void> resetScenarioRuns(
  BuildContext context, {
  required String scenarioKey,
  required String scenarioTitle,
  required int currentRunCount,
  required VoidCallback resetProvider,
}) async {
  final confirmed = await confirmResetBenchmarkRuns(
    context,
    scenarioTitle: scenarioTitle,
    currentRunCount: currentRunCount,
  );
  if (!confirmed || !context.mounted) return;

  resetProvider();
  context.read<BenchmarkSummaryProvider>().clearScenario(scenarioKey);

  if (!context.mounted) return;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text('Run $scenarioTitle direset ke 0.'),
      behavior: SnackBarBehavior.floating,
    ),
  );
}

void showBenchmarkProgressSnackBar(
  BuildContext context, {
  required String scenarioTitle,
  required int completedRuns,
  required int targetRuns,
}) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(
        '$scenarioTitle selesai — $completedRuns/$targetRuns run tercatat.',
      ),
      behavior: SnackBarBehavior.floating,
      duration: const Duration(seconds: 4),
    ),
  );
}

Future<BenchmarkCompleteAction?> showBenchmarkCompleteDialog(
  BuildContext context, {
  required String scenarioTitle,
  required int completedRuns,
  required int targetRuns,
  required double lastExecutionMs,
  required BenchmarkRunStats stats,
  String? errorMessage,
}) {
  return showDialog<BenchmarkCompleteAction>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) {
      final hasError = errorMessage != null && errorMessage.isNotEmpty;

      return AlertDialog(
        icon: Icon(
          hasError ? Icons.error_outline : Icons.check_circle_outline,
          color: hasError
              ? Theme.of(dialogContext).colorScheme.error
              : Theme.of(dialogContext).colorScheme.primary,
          size: 40,
        ),
        title: Text(hasError ? 'Pengujian Gagal' : 'Pengujian Selesai'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (hasError) ...[
                Text(errorMessage),
                const SizedBox(height: 12),
              ] else ...[
                Text(
                  '$scenarioTitle — $completedRuns/$targetRuns run berhasil.',
                  style: Theme.of(dialogContext).textTheme.bodyLarge,
                ),
                const SizedBox(height: 12),
                _StatRow(
                  label: 'Run terakhir',
                  value: formatMs(lastExecutionMs),
                ),
                if (completedRuns > 1) ...[
                  _StatRow(
                    label: 'Rata-rata',
                    value: formatMs(stats.averageMs),
                  ),
                  _StatRow(label: 'Tercepat', value: formatMs(stats.minMs)),
                  _StatRow(label: 'Terlambat', value: formatMs(stats.maxMs)),
                ],
                const SizedBox(height: 8),
                Text(
                  'Ingin menguji lagi atau mereset run ke 0?',
                  style: Theme.of(dialogContext).textTheme.bodySmall,
                ),
              ],
            ],
          ),
        ),
        actions: [
          if (!hasError) ...[
            TextButton(
              onPressed: () =>
                  Navigator.pop(dialogContext, BenchmarkCompleteAction.reset),
              child: const Text('Reset Run'),
            ),
            FilledButton(
              onPressed: () =>
                  Navigator.pop(dialogContext, BenchmarkCompleteAction.runAgain),
              child: const Text('Uji Lagi'),
            ),
          ],
          TextButton(
            onPressed: () =>
                Navigator.pop(dialogContext, BenchmarkCompleteAction.dismiss),
            child: Text(hasError ? 'Tutup' : 'Selesai'),
          ),
        ],
      );
    },
  );
}

Future<void> handleBenchmarkFinished(
  BuildContext context, {
  required String scenarioKey,
  required String scenarioTitle,
  required int completedRuns,
  required int targetRuns,
  required double lastExecutionMs,
  required List<BenchmarkResult> results,
  required VoidCallback resetProvider,
  required Future<void> Function() runAgain,
  String? errorMessage,
}) async {
  if (!context.mounted) return;

  if (errorMessage == null || errorMessage.isEmpty) {
    showBenchmarkProgressSnackBar(
      context,
      scenarioTitle: scenarioTitle,
      completedRuns: completedRuns,
      targetRuns: targetRuns,
    );
  }

  final action = await showBenchmarkCompleteDialog(
    context,
    scenarioTitle: scenarioTitle,
    completedRuns: completedRuns,
    targetRuns: targetRuns,
    lastExecutionMs: lastExecutionMs,
    stats: BenchmarkRunStats.fromResults(results),
    errorMessage: errorMessage,
  );

  if (!context.mounted || action == null) return;

  switch (action) {
    case BenchmarkCompleteAction.runAgain:
      await runAgain();
    case BenchmarkCompleteAction.reset:
      await resetScenarioRuns(
        context,
        scenarioKey: scenarioKey,
        scenarioTitle: scenarioTitle,
        currentRunCount: completedRuns,
        resetProvider: resetProvider,
      );
    case BenchmarkCompleteAction.dismiss:
      break;
  }
}

class BenchmarkRunProgressCard extends StatelessWidget {
  const BenchmarkRunProgressCard({
    super.key,
    required this.isLoading,
    required this.progressRun,
    required this.runCount,
    required this.targetRuns,
  });

  final bool isLoading;
  final int progressRun;
  final int runCount;
  final int targetRuns;

  @override
  Widget build(BuildContext context) {
    final activeRun = isLoading ? progressRun : runCount;
    final progress = targetRuns > 0 ? activeRun / targetRuns : 0.0;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    isLoading
                        ? 'Sedang berjalan: run $progressRun / $targetRuns'
                        : runCount > 0
                            ? 'Selesai: $runCount / $targetRuns run'
                            : 'Belum ada run',
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                ),
                if (isLoading)
                  const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
              ],
            ),
            if (isLoading || runCount > 0) ...[
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: progress.clamp(0.0, 1.0),
                minHeight: 6,
                borderRadius: BorderRadius.circular(3),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatRow extends StatelessWidget {
  const _StatRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}
