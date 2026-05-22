import 'package:flutter/material.dart';

import 'package:provider/provider.dart';



import 'providers/benchmark_settings_provider.dart';
import 'providers/benchmark_summary_provider.dart';

import 'providers/database_provider.dart';

import 'providers/http_provider.dart';

import 'providers/list_provider.dart';

import 'screens/home_screen.dart';



void main() {

  runApp(const MyApp());

}



class MyApp extends StatelessWidget {

  const MyApp({super.key});



  @override

  Widget build(BuildContext context) {

    return MultiProvider(

      providers: [

        ChangeNotifierProvider(create: (_) => BenchmarkSummaryProvider()),
        ChangeNotifierProvider(create: (_) => BenchmarkSettingsProvider()),

        ChangeNotifierProvider(

          create: (context) => HttpProvider(

            onResultRecorded: context.read<BenchmarkSummaryProvider>().addResult,

          ),

        ),

        ChangeNotifierProvider(

          create: (context) => ListProvider(

            onResultRecorded: context.read<BenchmarkSummaryProvider>().addResult,

          ),

        ),

        ChangeNotifierProvider(

          create: (context) => DatabaseProvider(

            onResultRecorded: context.read<BenchmarkSummaryProvider>().addResult,

          ),

        ),

      ],

      child: MaterialApp(

        debugShowCheckedModeBanner: false,

        title: 'Flutter Benchmark',

        theme: ThemeData(

          colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),

          useMaterial3: true,

        ),

        home: const HomeScreen(),

      ),

    );

  }

}

