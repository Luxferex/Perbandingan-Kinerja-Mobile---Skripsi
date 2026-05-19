import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/benchmark_result.dart';
import '../providers/benchmark_summary_provider.dart';
import '../providers/http_provider.dart';

class HttpScreen extends StatelessWidget {
  const HttpScreen({super.key});

  String _formatMs(double ms) => ms.toStringAsFixed(2);

  void _showErrorSnackBar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  Future<void> _runBenchmark(BuildContext context) async {
    final httpProvider = context.read<HttpProvider>();
    final previousRunCount = httpProvider.runCount;

    await httpProvider.fetchAndMeasure();

    if (!context.mounted) return;

    if (httpProvider.error != null) {
      _showErrorSnackBar(context, httpProvider.error!);
      return;
    }

    if (httpProvider.runCount > previousRunCount) {
      context.read<BenchmarkSummaryProvider>().addResult(
            BenchmarkResult(
              scenario: 'http',
              executionTimeMs: httpProvider.executionTimeMs,
            ),
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario HTTP Request'),
      ),
      body: Consumer<HttpProvider>(
        builder: (context, provider, _) {
          final executionMs = provider.runCount > 0
              ? provider.executionTimeMs
              : 0.0;

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
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Waktu Eksekusi: ${_formatMs(executionMs)} ms',
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
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: provider.isLoading
                      ? null
                      : () => _runBenchmark(context),
                  child: const Text('Jalankan'),
                ),
                if (provider.isLoading) ...[
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(),
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
