import 'dart:math';

/// [length] random lowercase hex digits, as in a UUID.
String randomHex(int length) {
  final random = Random.secure();
  return List.generate(length, (_) => random.nextInt(16).toRadixString(16)).join();
}
