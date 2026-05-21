import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/list_provider.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class ListScreen extends StatelessWidget {
  const ListScreen({super.key});

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
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
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
                  onPressed: provider.generateAndMeasure,
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
