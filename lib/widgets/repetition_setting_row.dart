import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_settings_provider.dart';

class RepetitionSettingRow extends StatelessWidget {
  const RepetitionSettingRow({
    super.key,
    required this.scenario,
    required this.label,
  });

  final String scenario;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Consumer<BenchmarkSettingsProvider>(
      builder: (context, settings, _) {
        final count = settings.targetRunsFor(scenario);

        return Row(
          children: [
            Expanded(
              child: Text('$label: $count'),
            ),
            IconButton(
              onPressed: () => settings.decrementTargetRuns(scenario),
              icon: const Icon(Icons.remove),
            ),
            Text(
              '$count',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            IconButton(
              onPressed: () => settings.incrementTargetRuns(scenario),
              icon: const Icon(Icons.add),
            ),
          ],
        );
      },
    );
  }
}
