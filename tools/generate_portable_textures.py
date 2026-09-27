#!/usr/bin/env python3
"""Generate the AE2Objects deep portable-cell placeholder housing textures.

The portable item model has four layers:

  layer0  housing        <- recoloured here (deep body colour)
  layer1  LED            <- AE2, unchanged (tinted by storage-cell state)
  layer2  screen         <- AE2, unchanged (tinted by dye)
  layer3  side_<tier>    <- AE2, unchanged (shared tier indicator)

Only the housing layer is derived. Its hue is set to the deep body colour of the
matching storage type and the value is darkened so it reads as "deep" next to the
netherite-based crafting.

Outputs:
  src/main/resources/assets/ae2objects/textures/item/deep_portable_item_housing.png
  src/main/resources/assets/ae2objects/textures/item/deep_portable_fluid_housing.png

Usage:
  python3 tools/generate_portable_textures.py --ae2-jar /path/to/appliedenergistics2.jar

Requires Pillow. Development tool only; it is not part of the Gradle build.
"""

from __future__ import annotations

import argparse
import colorsys
import io
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

AE2_ITEM_DIR = "assets/ae2/textures/item"

DEFAULT_JAR = (
    "~/.gradle/caches/modules-2/files-2.1/org.appliedenergistics/"
    "appliedenergistics2/26.1.10-beta/a4803b04e885c698eff0236cb8faeb8abc33f216/"
    "appliedenergistics2-26.1.10-beta.jar"
)


@dataclass(frozen=True)
class Target:
    """Absolute hue (degrees) and multiplicative saturation/value shaping."""

    hue: float
    sat_scale: float
    sat_floor: float
    value_scale: float


# Item -> dark violet, Fluid -> dark teal, both matched to the deep drive bodies.
TARGETS = {
    "item": ("portable_cell_item_housing", Target(hue=270.0, sat_scale=1.0, sat_floor=0.20, value_scale=0.55)),
    "fluid": ("portable_cell_fluid_housing", Target(hue=164.0, sat_scale=0.75, sat_floor=0.10, value_scale=0.55)),
}


def recolor_pixel(pixel, target: Target):
    r, g, b, a = pixel
    if a == 0:
        return pixel
    _, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    s = min(1.0, s * target.sat_scale + target.sat_floor)
    v = min(1.0, v * target.value_scale)
    r, g, b = colorsys.hsv_to_rgb(target.hue / 360.0, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def recolor(image, target: Target):
    image = image.convert("RGBA")
    out = Image.new("RGBA", image.size)
    src, dst = image.load(), out.load()
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            dst[x, y] = recolor_pixel(src[x, y], target)
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ae2-jar", default=DEFAULT_JAR, help="Path to the AE2 jar")
    parser.add_argument("--root", default=".", help="Repository root")
    args = parser.parse_args()

    jar_path = Path(args.ae2_jar).expanduser()
    root = Path(args.root).resolve()
    if not jar_path.is_file():
        sys.exit(f"AE2 jar not found: {jar_path}")

    item_out = root / "src/main/resources/assets/ae2objects/textures/item"

    with zipfile.ZipFile(jar_path) as archive:
        for type_id, (source, target) in TARGETS.items():
            with archive.open(f"{AE2_ITEM_DIR}/{source}.png") as entry:
                housing = Image.open(io.BytesIO(entry.read())).convert("RGBA")
            recolor(housing, target).save(item_out / f"deep_portable_{type_id}_housing.png")
            print(f"Wrote deep_portable_{type_id}_housing.png")


if __name__ == "__main__":
    main()
