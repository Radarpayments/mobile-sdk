const _greekStressed = 'ΆΈΉΊΌΎΏ';
const _greekPlain = 'ΑΕΗΙΟΥΩ';

/// [text] in capitals for a heading. Greek capitals carry no stress mark ("ΠΥΛΗ", not "ΠΎΛΗ"), but
/// [String.toUpperCase] keeps it.
String toCaps(String text) => text.toUpperCase().split('').map((c) {
  final i = _greekStressed.indexOf(c);
  return i < 0 ? c : _greekPlain[i];
}).join();
