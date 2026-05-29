import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

import '../models/post_model.dart';

class DatabaseService {
  static DatabaseService? _instance;
  Database? _database;
  String? _databasePath;

  DatabaseService._();

  factory DatabaseService() {
    _instance ??= DatabaseService._();
    return _instance!;
  }

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await initDatabase();
    return _database!;
  }

  Future<Database> initDatabase() async {
    _databasePath = join(
      await getDatabasesPath(),
      'benchmark_posts.db',
    );

    final db = await openDatabase(
      _databasePath!,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE posts (
            id INTEGER PRIMARY KEY,
            userId INTEGER NOT NULL,
            title TEXT NOT NULL,
            body TEXT NOT NULL
          )
        ''');
      },
    );

    _database = db;
    return db;
  }

  Future<void> insertBatch(List<PostModel> posts) async {
    final db = await database;
    final batch = db.batch();

    for (final post in posts) {
      batch.insert(
        'posts',
        post.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }

    await batch.commit(noResult: true);
  }

  Future<List<PostModel>> selectAll() async {
    final db = await database;
    final rows = await db.query('posts', orderBy: 'id ASC');

    return rows.map(PostModel.fromMap).toList();
  }

  Future<int> updateHalf() async {
    final db = await database;
    return db.rawUpdate(
      "UPDATE posts SET title = 'updated_title' WHERE id <= 500",
    );
  }

  Future<int> deleteHalf() async {
    final db = await database;
    return db.rawDelete('DELETE FROM posts WHERE id > 500');
  }

  Future<int> clearAll() async {
    final db = await database;
    return db.delete('posts');
  }

  Future<String> getDatabasePath() async {
    if (_databasePath != null) return _databasePath!;
    _databasePath = join(
      await getDatabasesPath(),
      'benchmark_posts.db',
    );
    return _databasePath!;
  }
}
