import 'package:flutter/material.dart';

import 'app.dart';
import 'app_scope.dart';
import 'prefs/app_prefs.dart';
import 'store/store_model.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await AppPrefs.load();
  runApp(AppScope(prefs: prefs, store: StoreModel(), child: const SampleApp()));
}
