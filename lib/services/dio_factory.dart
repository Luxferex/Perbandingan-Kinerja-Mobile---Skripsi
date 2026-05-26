import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';

/// Host API benchmark — harus sama dengan URL di [HttpService].
const benchmarkApiHost = 'jsonplaceholder.typicode.com';

/// Membuat instance Dio untuk skenario HTTP benchmark.
///
/// Jaringan kampus/kantor sering memakai proxy SSL (sertifikat self-signed)
/// sehingga [HandshakeException] muncul. Untuk host API penelitian ini,
/// sertifikat proxy diterima agar pengujian tetap bisa berjalan.
Dio createBenchmarkDio() {
  final dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(seconds: 30),
      headers: {'Accept': 'application/json'},
    ),
  );

  if (!kIsWeb) {
    dio.httpClientAdapter = IOHttpClientAdapter(
      createHttpClient: () {
        final client = HttpClient();
        client.badCertificateCallback = (cert, host, port) {
          final isBenchmarkHost =
              host == benchmarkApiHost || host.endsWith('.typicode.com');

          if (isBenchmarkHost) {
            if (kDebugMode) {
              debugPrint(
                '[HttpService] SSL: menerima sertifikat untuk $host:$port '
                '(sering karena proxy WiFi kampus). '
                'Subject: ${cert.subject}',
              );
            }
            return true;
          }

          if (kDebugMode) {
            debugPrint(
              '[HttpService] SSL: menolak sertifikat untuk $host:$port',
            );
          }
          return false;
        };
        return client;
      },
    );
  }

  return dio;
}
