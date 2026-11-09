#!/usr/bin/env python3
"""Converts the Android sample's string resources into the ARB files of this app.

The Android sample is the source of the demo's texts in all nine languages; run this after
changing them there, then `flutter gen-l10n` (or any `flutter run` / `flutter build`):

    python3 tool/android_strings_to_arb.py

Keys become lowerCamelCase (`store_pay` -> `storePay`), `%1$s` / `%1$d` placeholders become
named ICU placeholders. Strings marked translatable="false" (brand and product names) are
copied into every language as they are.
"""

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

APP = Path(__file__).resolve().parent.parent
RES = APP.parent / "android-app" / "src" / "main" / "res"
OUT = APP / "lib" / "l10n"
FILES = ("strings_store.xml", "strings_catalog.xml", "strings_core.xml", "strings_invest.xml")
LANGUAGES = ("en", "ru", "ro", "el", "bg", "de", "fr", "es", "ar")

# Names of the placeholders, by key; a key not listed here gets arg1, arg2, ...
PLACEHOLDERS = {
    "store_cart_count": ["count"],
    "store_pay": ["amount"],
    "store_add_to_cart": ["price"],
    "store_error_failed": ["message"],
    "scenario_row": ["buyer"],
    "result_paid_amount": ["amount"],
    "coffee_add_item": ["item"],
    "tickets_to_payment": ["amount"],
    "tickets_event_line": ["category"],
    "tickets_hold": ["time"],
    "invest_account": ["account"],
    "invest_free_cash_just_now": ["amount"],
    "invest_units": ["units", "price"],
    "invest_op_buy": ["asset"],
    "topup_limits": ["min", "max"],
    "topup_continue": ["amount"],
    "topup_failed_detail": ["message"],
    "done_credited": ["account"],
    "done_invest": ["amount"],
}

# Texts only this app needs: Apple Pay, which the Android sample does not have. Brand names, not
# translated.
FLUTTER_ONLY = {
    "scenario_wallet_apple": "Apple Pay",
    "dev_apple_pay": "Apple Pay",
}

FORMAT = re.compile(r"%(\d+)\$([sd])")


def camel(key: str) -> str:
    head, *rest = key.split("_")
    return head + "".join(part.capitalize() for part in rest)


def unescape(text: str) -> str:
    """Android's backslash escapes; XML entities are already decoded by the parser."""
    return re.sub(r"\\(.)", lambda m: "\n" if m.group(1) == "n" else m.group(1), text)


def read(language: str) -> dict[str, tuple[str, bool]]:
    """Strings of one language: key -> (text, translatable)."""
    folder = RES / ("values" if language == "en" else f"values-{language}")
    strings = {}
    for name in FILES:
        for element in ET.parse(folder / name).getroot().iter("string"):
            text = unescape("".join(element.itertext()))
            strings[element.get("name")] = (text, element.get("translatable") != "false")
    return strings


def to_icu(key: str, text: str) -> tuple[str, dict]:
    names = PLACEHOLDERS.get(key, [])
    placeholders = {}

    def replace(match: re.Match) -> str:
        index = int(match.group(1))
        name = names[index - 1] if index <= len(names) else f"arg{index}"
        placeholders[name] = {"type": "int" if match.group(2) == "d" else "String"}
        return "{" + name + "}"

    return FORMAT.sub(replace, text).replace("%%", "%"), placeholders


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    template = read("en")
    fixed = {key: text for key, (text, translatable) in template.items() if not translatable} | FLUTTER_ONLY
    for language in LANGUAGES:
        translated = {key: text for key, (text, _) in read(language).items() if key in template and key not in fixed}
        arb = {"@@locale": language}
        # Template order, so every file reads the same; brand names and other fixed texts are copied as they are.
        for key in [*template, *FLUTTER_ONLY]:
            text = fixed.get(key) or translated.get(key)
            if text is None:
                continue
            message, placeholders = to_icu(key, text)
            arb[camel(key)] = message
            if language == "en" and placeholders:
                arb["@" + camel(key)] = {"placeholders": placeholders}
        path = OUT / f"app_{language}.arb"
        path.write_text(json.dumps(arb, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"{path.relative_to(APP)}: {sum(1 for k in arb if not k.startswith('@'))} strings")
    return 0


if __name__ == "__main__":
    sys.exit(main())
