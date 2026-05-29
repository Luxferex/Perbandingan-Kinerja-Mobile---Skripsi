import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../providers/benchmark_settings_provider.dart';

class RepetitionSettingRow extends StatefulWidget {
  const RepetitionSettingRow({
    super.key,
    required this.scenario,
    required this.label,
  });

  final String scenario;
  final String label;

  @override
  State<RepetitionSettingRow> createState() => _RepetitionSettingRowState();
}

class _RepetitionSettingRowState extends State<RepetitionSettingRow> {
  late final TextEditingController _controller;
  final FocusNode _focusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _commitValue(BenchmarkSettingsProvider settings) {
    final parsed = int.tryParse(_controller.text.trim());
    if (parsed == null) {
      _controller.text = '${settings.targetRunsFor(widget.scenario)}';
      return;
    }
    settings.setTargetRuns(widget.scenario, parsed);
    _controller.text = '${settings.targetRunsFor(widget.scenario)}';
    _focusNode.unfocus();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<BenchmarkSettingsProvider>(
      builder: (context, settings, _) {
        final count = settings.targetRunsFor(widget.scenario);

        if (!_focusNode.hasFocus && _controller.text != '$count') {
          _controller.text = '$count';
          _controller.selection = TextSelection.collapsed(
            offset: _controller.text.length,
          );
        }

        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Expanded(
                child: Text(widget.label),
              ),
              IconButton(
                onPressed: () => settings.decrementTargetRuns(widget.scenario),
                icon: const Icon(Icons.remove),
                tooltip: 'Kurangi',
              ),
              SizedBox(
                width: 72,
                child: TextField(
                  controller: _controller,
                  focusNode: _focusNode,
                  keyboardType: TextInputType.number,
                  textInputAction: TextInputAction.done,
                  textAlign: TextAlign.center,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: Theme.of(context).textTheme.titleMedium,
                  decoration: InputDecoration(
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 10,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    hintText: '$count',
                  ),
                  onTap: () {
                    _controller.selection = TextSelection(
                      baseOffset: 0,
                      extentOffset: _controller.text.length,
                    );
                  },
                  onSubmitted: (_) => _commitValue(settings),
                  onEditingComplete: () => _commitValue(settings),
                ),
              ),
              IconButton(
                onPressed: () => settings.incrementTargetRuns(widget.scenario),
                icon: const Icon(Icons.add),
                tooltip: 'Tambah',
              ),
            ],
          ),
        );
      },
    );
  }
}
