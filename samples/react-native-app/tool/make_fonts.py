#!/usr/bin/env python3
"""Makes the fonts of this app from the Flutter sample's variable Google Fonts and Flutter's Material Icons.

React Native picks a custom font by its name (the file name on Android, the PostScript name on iOS) and cannot
set the weight axis of a variable font, so every weight the demo uses becomes a static font of its own:
`Inter-Bold.ttf`, PostScript name `Inter-Bold`. The icons are the Material Icons Flutter draws, cut down to the
few glyphs the demo shows.

    python3 -m pip install fonttools
    python3 tool/make_fonts.py [path/to/MaterialIcons-Regular.otf]

The icon font defaults to the one in the Flutter SDK (`flutter/bin/cache/artifacts/material_fonts`).
"""

import shutil
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

APP = Path(__file__).resolve().parent.parent
SOURCE = APP.parent / "flutter-app" / "assets" / "fonts"
OUT = APP / "assets" / "fonts"

WEIGHTS = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold", 800: "ExtraBold"}

# Family -> (variable file, weights the demo uses, fixed values of the other axes).
FAMILIES = {
    "Inter": ("inter.ttf", (400, 500, 600, 700, 800), {"opsz": 14}),
    "Montserrat": ("montserrat.ttf", (400, 500, 600, 700), {}),
    "Cormorant": ("cormorant.ttf", (600, 700), {}),
    "Nunito": ("nunito.ttf", (400, 500, 700, 800), {}),
    "IBMPlexSans": ("ibm_plex_sans.ttf", (400, 500, 600, 700), {"wdth": 100}),
}
ITALICS = {"PlayfairDisplay": ("playfair_display_italic.ttf", (600,))}

# Material Icons by name, as in Flutter's `Icons`; src/ui/Icon.tsx has the same list.
ICONS = {
    "arrow_back": 0xE092,
    "chevron_left": 0xE15E,
    "settings_outlined": 0xF36E,
    "shopping_basket_outlined": 0xF37E,
    "place_outlined": 0xF2AC,
    "add": 0xE047,
    "remove": 0xE516,
    "check": 0xE156,
    "close": 0xE16A,
    "schedule": 0xE556,
    "keyboard_arrow_down": 0xE353,
    "trending_up": 0xE67F,
    "account_balance_wallet_outlined": 0xEE33,
    "arrow_upward": 0xE0A0,
    "notifications_none": 0xE451,
    "error_outline": 0xE238,
    "check_circle_outline": 0xE15A,
    "credit_card": 0xE19F,
}


def rename(font: TTFont, family: str, style: str) -> None:
    """One family per file, named as the app asks for it: `Inter-Bold`."""
    postscript = f"{family}-{style}"
    names = font["name"]
    for record in list(names.names):
        if record.nameID in (1, 2, 3, 4, 6, 16, 17, 25):
            names.removeNames(nameID=record.nameID)
    for name_id, value in {1: postscript, 2: "Regular", 3: postscript, 4: postscript, 6: postscript}.items():
        names.setName(value, name_id, 3, 1, 0x409)
        names.setName(value, name_id, 1, 0, 0)


def static(source: str, family: str, weight: int, axes: dict, italic: bool) -> None:
    font = TTFont(SOURCE / source)
    instancer.instantiateVariableFont(font, {"wght": weight, **axes}, inplace=True)
    style = WEIGHTS[weight] + ("Italic" if italic else "")
    rename(font, family, style)
    font["OS/2"].usWeightClass = weight
    font.save(OUT / f"{family}-{style}.ttf")


def icons(source: Path) -> None:
    options = subset.Options()
    options.layout_features = []
    options.name_IDs = ["*"]
    font = TTFont(source)
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=ICONS.values())
    subsetter.subset(font)
    font.save(OUT / "MaterialIcons-Regular.otf")
    license_file = source.parent / "MaterialIcons_LICENSE.txt"
    if license_file.exists():
        shutil.copy(license_file, OUT / "MaterialIcons_LICENSE.txt")


def main() -> None:
    flutter = shutil.which("flutter")
    default_icons = (
        Path(flutter).resolve().parent / "cache" / "artifacts" / "material_fonts" / "MaterialIcons-Regular.otf"
        if flutter
        else None
    )
    icon_font = Path(sys.argv[1]) if len(sys.argv) > 1 else default_icons
    if icon_font is None or not icon_font.exists():
        sys.exit("MaterialIcons-Regular.otf not found: pass its path")
    OUT.mkdir(parents=True, exist_ok=True)
    for family, (source, weights, axes) in FAMILIES.items():
        for weight in weights:
            static(source, family, weight, axes, italic=False)
    for family, (source, weights) in ITALICS.items():
        for weight in weights:
            static(source, family, weight, {}, italic=True)
    icons(icon_font)


if __name__ == "__main__":
    main()
