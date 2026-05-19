import 'package:dio/dio.dart';

import '../models/post_model.dart';

class HttpService {
  final Dio _dio;

  HttpService({Dio? dio}) : _dio = dio ?? Dio();

  Future<List<PostModel>> fetchPosts() async {
    try {
      final response = await _dio.get<List<dynamic>>(
        'https://jsonplaceholder.typicode.com/posts',
        queryParameters: {'_limit': 100},
      );

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
      throw Exception('Failed to fetch posts: ${e.message}');
    }
  }
}
