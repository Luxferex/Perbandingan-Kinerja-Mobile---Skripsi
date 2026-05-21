import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/http_provider.dart';
import '../utils/benchmark_utils.dart';
import '../utils/clipboard_helper.dart';

class HttpScreen extends StatelessWidget {
  const HttpScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Skenario HTTP Request'),
      ),
      body: Consumer<HttpProvider>(
        builder: (context, provider, _) {
          final executionMs =
              provider.runCount > 0 ? provider.executionTimeMs : 0.0;

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
                  onPressed: provider.isLoading
                      ? null
                      : provider.fetchAndMeasure,
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
