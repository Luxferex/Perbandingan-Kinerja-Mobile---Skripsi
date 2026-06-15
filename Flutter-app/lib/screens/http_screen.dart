import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_settings_provider.dart';
import '../providers/http_provider.dart';
import '../utils/benchmark_run_helper.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class HttpScreen extends StatefulWidget {
  const HttpScreen({super.key});

  @override
  State<HttpScreen> createState() => _HttpScreenState();
}

class _HttpScreenState extends State<HttpScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<HttpProvider>().warmUp();
    });
  }

  Future<void> _runBenchmark(BuildContext context) async {
    final settings = context.read<BenchmarkSettingsProvider>();
    final provider = context.read<HttpProvider>();
    final targetRuns = settings.httpTargetRuns;

    await provider.runMultiple(targetRuns);

    if (!context.mounted) return;

    await handleBenchmarkFinished(
      context,
      scenarioKey: 'http',
      scenarioTitle: 'HTTP Request',
      completedRuns: provider.runCount,
      targetRuns: targetRuns,
      lastExecutionMs: provider.executionTimeMs,
      results: provider.results,
      errorMessage: provider.error,
      resetProvider: provider.reset,
      runAgain: () => _runBenchmark(context),
    );
  }

  Future<void> _resetRuns(BuildContext context) async {
    final provider = context.read<HttpProvider>();
    await resetScenarioRuns(
      context,
      scenarioKey: 'http',
      scenarioTitle: 'HTTP Request',
      currentRunCount: provider.runCount,
      resetProvider: provider.reset,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario HTTP Request'),
        actions: [
          IconButton(
            tooltip: 'Reset run ke 0',
            onPressed: () => _resetRuns(context),
            icon: const Icon(Icons.restart_alt),
          ),
        ],
      ),
      body: Consumer2<HttpProvider, BenchmarkSettingsProvider>(
        builder: (context, provider, settings, _) {
          final executionMs =
              provider.runCount > 0 ? provider.executionTimeMs : 0.0;
          final targetRuns = settings.httpTargetRuns;

          return Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Fetch 100 posts dari JSONPlaceholder API',
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
                          'Waktu Eksekusi: ${formatMs(executionMs)}',
                        ),
                        const SizedBox(height: 4),
                        Text('Jumlah run: ${provider.runCount}'),
                        if (provider.error != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            provider.error!,
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                            ),
                          ),
                        ],
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
                  onPressed: provider.isWarmedUp && !provider.isLoading
                      ? () => _runBenchmark(context)
                      : null,
                  child: Text('Jalankan ($targetRuns x)'),
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
                    itemCount: provider.posts.length,
                    itemBuilder: (context, index) {
                      final post = provider.posts[index];
                      return ListTile(
                        leading: CircleAvatar(
                          child: Text('${index + 1}'),
                        ),
                        title: Text(post.title),
                        subtitle: Text(
                          post.body,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
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
