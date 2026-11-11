#!/usr/bin/env python3
"""Compares two sets of checkout screenshots file by file.

    python3 tools/visual/compare.py compose-baseline compose-baseline-2
    python3 tools/visual/compare.py tools/visual/golden/compose swiftui

A set is a directory, or the name of one under tools/visual/out/. Per file it prints the share of differing pixels and
the largest channel difference; a mismatch gets `A | B | diff` written to tools/visual/out/diff/. The exit code is 1
when a file differs in more than --threshold of its pixels, or exists in one set only.
"""

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

HERE = Path(__file__).resolve().parent
OUT = HERE / "out"

# Rectangles (left, top, right, bottom) in pixels that are not compared, per file name; "*" applies to every file.
MASKS = {
    # The ring around the lock turns for as long as the screen is up; the lock inside it goes with it.
    # Below the text the Compose sheet shows a ghost of the Apple Pay button it keeps alive at 1 % opacity: a defect of
    # that renderer, not a part of the design.
    "40_processing.png": [(480, 1790, 726, 2040), (56, 2379, 1150, 2549)],
}

# A larger channel tolerance for the files whose whole content pulses: the skeleton's rows fade in and out, and a shot
# catches them a few levels lighter or darker (6 of 255 between two runs).
TOLERANCES = {
    "01_skeleton_cards3_wallet.png": 16,
    "02_skeleton_cards0.png": 16,
}


def set_dir(name):
    path = Path(name)
    if path.is_dir():
        return path
    if (OUT / name).is_dir():
        return OUT / name
    sys.exit("No such set: %s" % name)


def masked(image, name):
    for box in MASKS.get("*", []) + MASKS.get(name, []):
        image.paste((0, 0, 0), box)
    return image


def outside(image, other, slack):
    """How far each channel of `image` lies outside the range `other` takes within `slack` pixels of the same place."""
    size = 2 * slack + 1
    low, high = other.filter(ImageFilter.MinFilter(size)), other.filter(ImageFilter.MaxFilter(size))
    return ImageChops.add(ImageChops.subtract(low, image), ImageChops.subtract(image, high))


def compare(a_path, b_path, name, tolerance, slack):
    a = masked(Image.open(a_path).convert("RGB"), name)
    b = masked(Image.open(b_path).convert("RGB"), name)
    if a.size != b.size:
        return 1.0, 255, None, "size %sx%s vs %sx%s" % (a.size + b.size)
    diff = ImageChops.lighter(outside(a, b, slack), outside(b, a, slack)) if slack else ImageChops.difference(a, b)
    max_delta = max(high for _, high in diff.getextrema())
    # A pixel differs when any of its channels is off by more than the tolerance.
    over = diff.point(lambda v: 255 if v > tolerance else 0).convert("L", (1, 1, 1, 0))
    differing = sum(over.point(lambda v: 1 if v else 0).getdata())
    share = differing / (a.size[0] * a.size[1])
    sheet = None
    if differing:
        mark = Image.new("RGB", a.size, (255, 0, 80))
        highlighted = Image.composite(mark, a.point(lambda v: v // 3 + 170), over)
        sheet = Image.new("RGB", (a.size[0] * 3 + 40, a.size[1]), (40, 40, 40))
        for index, image in enumerate((a, b, highlighted)):
            sheet.paste(image, (index * (a.size[0] + 20), 0))
    return share, max_delta, sheet, None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("set_a")
    parser.add_argument("set_b")
    parser.add_argument("--threshold", type=float, default=0.0005, help="share of differing pixels a file may have")
    parser.add_argument("--tolerance", type=int, default=2, help="channel difference that does not count")
    parser.add_argument(
        "--slack", type=int, default=0,
        help="pixels a shape may sit away from its place: two text engines put the same glyph a fraction of a pixel apart",
    )
    parser.add_argument("--diff-dir", default=str(OUT / "diff"))
    args = parser.parse_args()

    a_dir, b_dir = set_dir(args.set_a), set_dir(args.set_b)
    a_names = {p.name for p in a_dir.glob("*.png")}
    b_names = {p.name for p in b_dir.glob("*.png")}
    diff_dir = Path(args.diff_dir)
    diff_dir.mkdir(parents=True, exist_ok=True)
    for stale in diff_dir.glob("*.png"):
        stale.unlink()

    failed = 0
    identical = 0
    for name in sorted(a_names | b_names):
        if name not in a_names or name not in b_names:
            print("%-52s only in %s" % (name, a_dir if name in a_names else b_dir))
            failed += 1
            continue
        share, max_delta, sheet, note = compare(a_dir / name, b_dir / name, name, max(args.tolerance, TOLERANCES.get(name, 0)), args.slack)
        bad = share > args.threshold
        if sheet is not None and (bad or share > 0):
            sheet.save(diff_dir / name)
        if share == 0 and max_delta == 0:
            identical += 1
        failed += bad
        print("%-52s %8.4f%%  max delta %3d  %s%s" % (
            name, share * 100, max_delta, "DIFF" if bad else "ok", "  (%s)" % note if note else ""))
    total = len(a_names | b_names)
    print("\n%d files: %d byte-for-pixel identical, %d within the threshold, %d differ" % (
        total, identical, total - identical - failed, failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
