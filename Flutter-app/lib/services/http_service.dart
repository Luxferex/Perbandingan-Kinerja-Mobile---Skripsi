import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../models/post_model.dart';
import 'dio_factory.dart';

class HttpService {
  final Dio _dio;

  HttpService({Dio? dio}) : _dio = dio ?? createBenchmarkDio();

  static String _dioErrorMessage(DioException e) {
    final inner = e.error;
    if (inner is HandshakeException) {
      return 'Sertifikat SSL ditolak (proxy/VPN kampus?). '
          'Coba hotspot HP atau nonaktifkan proxy. Detail: $inner';
    }
    if (inner is SocketException) {
      return 'Tidak ada koneksi jaringan: $inner';
    }
    if (e.message != null && e.message!.trim().isNotEmpty) {
      return e.message!;
    }
    if (inner != null) {
      return inner.toString();
    }
    final status = e.response?.statusCode;
    if (status != null) {
      return 'HTTP $status';
    }
    return e.type.name;
  }

  /// [nonce] digunakan untuk cache-busting per run agar request tidak
  /// berpotensi dibaca dari cache perantara / library.
  Future<List<PostModel>> fetchPosts({required String nonce}) async {
    const url = 'https://jsonplaceholder.typicode.com/posts';

    if (kDebugMode) {
      debugPrint('[HttpService] GET $url?_limit=100');
    }

    try {
      final response = await _dio.get<List<dynamic>>(
        url,
        queryParameters: {
          '_limit': 100,
          '_nonce': nonce,
        },
        options: Options(
          headers: const {
            'Cache-Control': 'no-cache, no-store, max-age=0',
            'Pragma': 'no-cache',
          },
        ),
      );

      if (kDebugMode) {
        debugPrint(
          '[HttpService] Response ${response.statusCode}, '
          '${response.data?.length ?? 0} items',
        );
      }

      if (response.statusCode != 200 || response.data == null) {
        throw Exception(
          'Failed to fetch posts: HTTP ${response.statusCode}',
        );
      }

      return response.data!
          .map(
            (item) => PostModel.fromJson(item as Map<String, dynamic>),
          )
          .toList();
    } on DioException catch (e) {
      if (kDebugMode) {
        debugPrint(
          '[HttpService] DioException type=${e.type}, '
          'message=${e.message}, error=${e.error}',
        );
      }
      throw Exception('Failed to fetch posts: ${_dioErrorMessage(e)}');
    } on TypeError catch (e) {
      if (kDebugMode) {
        debugPrint('[HttpService] Parse error: $e');
      }
      throw Exception('Failed to parse posts: $e');
    }
  }
}
