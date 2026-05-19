import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/benchmark_result.dart';
import '../providers/benchmark_summary_provider.dart';
import '../providers/list_provider.dart';

class ListScreen extends StatelessWidget {
  const ListScreen({super.key});

  String _formatMs(double ms) => ms.toStringAsFixed(2);

  Future<void> _runBenchmark(BuildContext context) async {
    final listProvider = context.read<ListProvider>();
    await listProvider.generateAndMeasure();

    if (!context.mounted) return;

    if (listProvider.isGenerated) {
      context.read<BenchmarkSummaryProvider>().addResult(
            BenchmarkResult(
              scenario: 'rendering',
              executionTimeMs: listProvider.executionTimeMs,
            ),
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario Rendering Daftar'),
      ),
      body: Consumer<ListProvider>(
        builder: (context, provider, _) {
          final generateMs =
              provider.isGenerated ? provider.executionTimeMs : 0.0;

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
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Waktu Generate: ${_formatMs(generateMs)} ms',
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: () => _runBenchmark(context),
                  child: const Text('Generate & Render'),
                ),
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
