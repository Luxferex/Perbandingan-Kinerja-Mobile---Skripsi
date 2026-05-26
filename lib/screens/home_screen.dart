import 'package:flutter/material.dart';

import '../widgets/repetition_setting_row.dart';
import 'database_screen.dart';
import 'http_screen.dart';
import 'list_screen.dart';
import 'pre_test_screen.dart';
import 'summary_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Flutter Benchmark'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Jumlah Repetisi',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    const RepetitionSettingRow(
                      scenario: 'http',
                      label: 'Repetisi HTTP',
                    ),
                    const RepetitionSettingRow(
                      scenario: 'rendering',
                      label: 'Repetisi Rendering',
                    ),
                    const RepetitionSettingRow(
                      scenario: 'sqlite',
                      label: 'Repetisi SQLite',
                    ),
                    Text(
                      'Ketuk angka untuk mengetik langsung. Minimum 5, maksimum 100.',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => const PreTestScreen(),
                  ),
                );
              },
              child: const Text('Persiapan Pengujian'),
            ),
            const SizedBox(height: 12),
            _ScenarioCard(
              icon: Icons.wifi,
              title: 'HTTP Request',
              description:
                  'Fetch 100 posts dari JSONPlaceholder API dan ukur waktu eksekusi.',
              onStart: () {
                Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => const HttpScreen(),
                  ),
                );
              },
            ),
            const SizedBox(height: 12),
            _ScenarioCard(
              icon: Icons.list,
              title: 'Rendering Daftar',
              description:
                  'Generate 1000 item dummy dan render dalam ListView.',
              onStart: () {
                Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => const ListScreen(),
                  ),
                );
              },
            ),
            const SizedBox(height: 12),
            _ScenarioCard(
              icon: Icons.storage,
              title: 'Basis Data SQLite',
              description:
                  'Insert, SELECT, UPDATE, dan DELETE 1000 record lokal.',
              onStart: () {
                Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => const DatabaseScreen(),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => const SummaryScreen(),
                  ),
                );
              },
              child: const Text('Lihat Ringkasan Hasil'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScenarioCard extends StatelessWidget {
  const _ScenarioCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.onStart,
  });

  final IconData icon;
  final String title;
  final String description;
  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 32),
            const SizedBox(height: 8),
            Text(
              title,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 4),
            Text(
              description,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: onStart,
              child: const Text('Mulai Pengujian'),
            ),
          ],
        ),
      ),
    );
  }
}
