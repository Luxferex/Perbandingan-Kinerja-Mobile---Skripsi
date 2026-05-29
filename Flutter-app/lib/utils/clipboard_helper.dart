import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

Future<void> copyBenchmarkResult(
  BuildContext context, {
  required String? text,
}) async {
  if (text == null) return;

  await Clipboard.setData(ClipboardData(text: text));

  if (!context.mounted) return;

  ScaffoldMessenger.of(context).showSnackBar(
    const SnackBar(content: Text('Hasil disalin!')),
  );
}
