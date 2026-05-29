import 'package:flutter/material.dart';

class PreTestScreen extends StatefulWidget {
  const PreTestScreen({super.key});

  @override
  State<PreTestScreen> createState() => _PreTestScreenState();
}

class _PreTestScreenState extends State<PreTestScreen> {
  final Map<String, bool> _checklist = {
    'wifi': false,
    'brightness': false,
    'background': false,
    'developer': false,
    'battery': false,
    'warmup': false,
  };

  bool get _allChecked => _checklist.values.every((checked) => checked);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Persiapan Pengujian'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Centang semua item sebelum memulai pengujian kinerja:',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 12),
            Expanded(
              child: Card(
                child: ListView(
                  children: [
                    CheckboxListTile(
                      value: _checklist['wifi'],
                      onChanged: (v) => setState(() => _checklist['wifi'] = v!),
                      title: const Text('WiFi terhubung dan stabil'),
                    ),
                    CheckboxListTile(
                      value: _checklist['brightness'],
                      onChanged: (v) =>
                          setState(() => _checklist['brightness'] = v!),
                      title: const Text('Kecerahan layar sudah diatur ke 50%'),
                    ),
                    CheckboxListTile(
                      value: _checklist['background'],
                      onChanged: (v) =>
                          setState(() => _checklist['background'] = v!),
                      title: const Text(
                        'Tidak ada aplikasi lain yang berjalan di background',
                      ),
                    ),
                    CheckboxListTile(
                      value: _checklist['developer'],
                      onChanged: (v) =>
                          setState(() => _checklist['developer'] = v!),
                      title: const Text('Mode pengembang aktif'),
                    ),
                    CheckboxListTile(
                      value: _checklist['battery'],
                      onChanged: (v) =>
                          setState(() => _checklist['battery'] = v!),
                      title: const Text('Baterai > 50%'),
                    ),
                    CheckboxListTile(
                      value: _checklist['warmup'],
                      onChanged: (v) =>
                          setState(() => _checklist['warmup'] = v!),
                      title: const Text('Aplikasi sudah dijalankan sekali (warm-up)'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _allChecked
                  ? () {
                      Navigator.pop(context);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Persiapan selesai. Siap memulai pengujian.'),
                        ),
                      );
                    }
                  : null,
              child: const Text('Mulai Pengujian'),
            ),
          ],
        ),
      ),
    );
  }
}
