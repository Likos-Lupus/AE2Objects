#!/usr/bin/env python3
"""Generate the AE2Objects deep-fluid placeholder textures.

The fluid placeholders are derived from Applied Energistics 2 assets by
rotating the *body* hue by +180 degrees, while deliberately preserving the
34 "size indicator" pixels that encode the storage tier. Those indicator
pixels are identical across every cell type and housing (item, fluid, ...),
so they must never be recoloured.

Outputs:
  src/main/resources/assets/ae2objects/textures/item/deep_fluid_cell_housing.png
  src/main/resources/assets/ae2objects/textures/item/deep_fluid_storage_cell_<tier>.png
  src/main/resources/assets/ae2objects/textures/block/drive/drive_cells.png

The drive atlas has two sprite regions on a 16x16 canvas:
  U 0..6  -> item cells   (left untouched, already present in the repo)
  U 6..12 -> fluid cells  (filled here, one 2px row per tier, V 0..10)

Usage:
  python3 tools/generate_fluid_textures.py --ae2-jar /path/to/appliedenergistics2.jar

Requires Pillow. Development tool only; it is not part of the Gradle build.
"""

from __future__ import annotations

import argparse
import colorsys
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

TIERS = ("1k", "4k", "16k", "64k", "256k")
HUE_SHIFT = 150.0
SAT_SCALE = 0.80
VALUE_SCALE = 0.55

AE2_ITEM_DIR = "assets/ae2/textures/item"
AE2_DRIVE_ATLAS = "assets/ae2/textures/block/drive/drive_cells.png"

# Item cells occupy U 0..6, fluid cells U 6..12, one 2px row per tier (V 0..10).
FLUID_REGION = (6, 12, 0, 10)

DEFAULT_JAR = (
    "~/.gradle/caches/modules-2/files-2.1/org.appliedenergistics/"
    "appliedenergistics2/26.1.10-beta/a4803b04e885c698eff0236cb8faeb8abc33f216/"
    "appliedenergistics2-26.1.10-beta.jar"
)


def recolor_pixel(pixel, degrees, sat_scale, value_scale):
    r, g, b, a = pixel
    if a == 0:
        return pixel
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    h = (h + degrees / 360.0) % 1.0
    s = min(1.0, s * sat_scale)
    v = min(1.0, v * value_scale)
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def recolor_image(image, degrees, sat_scale, value_scale):
    image = image.convert("RGBA")
    out = Image.new("RGBA", image.size)
    src, dst = image.load(), out.load()
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            dst[x, y] = recolor_pixel(src[x, y], degrees, sat_scale, value_scale)
    return out


def differing_pixels(images):
    """Pixels whose RGBA differs between any of the given same-sized images."""
    width, height = images[0].size
    layers = [im.convert("RGBA").load() for im in images]
    mask = set()
    for y in range(height):
        for x in range(width):
            first = layers[0][x, y]
            if any(layer[x, y] != first for layer in layers[1:]):
                mask.add((x, y))
    return mask


def read_ae2_texture(archive, name):
    with archive.open(f"{AE2_ITEM_DIR}/{name}.png") as entry:
        return Image.open(io.BytesIO(entry.read())).convert("RGBA")


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
    atlas_out = root / "src/main/resources/assets/ae2objects/textures/block/drive/drive_cells.png"
    atlas_out.parent.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(jar_path) as archive:
        fluid_cells = [read_ae2_texture(archive, f"fluid_storage_cell_{t}") for t in TIERS]
        indicator_mask = differing_pixels(fluid_cells)
        item_cells = [read_ae2_texture(archive, f"item_storage_cell_{t}") for t in TIERS]
        # Colours of the size indicator, identical across item and fluid cells.
        indicator_colors = set()
        for image in item_cells:
            px = image.load()
            for (x, y) in indicator_mask:
                if px[x, y][3] > 0:
                    indicator_colors.add(px[x, y][:3])

        for tier, cell in zip(TIERS, fluid_cells):
            shifted = recolor_image(cell, HUE_SHIFT, SAT_SCALE, VALUE_SCALE)
            src, dst = cell.load(), shifted.load()
            for (x, y) in indicator_mask:
                dst[x, y] = src[x, y]
            shifted.save(item_out / f"deep_fluid_storage_cell_{tier}.png")

        housing = read_ae2_texture(archive, "fluid_cell_housing")
        recolor_image(housing, HUE_SHIFT, SAT_SCALE, VALUE_SCALE).save(
            item_out / "deep_fluid_cell_housing.png"
        )

        with archive.open(AE2_DRIVE_ATLAS) as entry:
            ae2_atlas = Image.open(io.BytesIO(entry.read())).convert("RGBA")

    atlas = Image.open(atlas_out).convert("RGBA")
    src, dst = ae2_atlas.load(), atlas.load()
    x0, x1, y0, y1 = FLUID_REGION
    for y in range(y0, y1):
        for x in range(x0, x1):
            pixel = src[x, y]
            if pixel[3] == 0:
                dst[x, y] = pixel
            elif pixel[:3] in indicator_colors:
                dst[x, y] = pixel
            else:
                dst[x, y] = recolor_pixel(pixel, HUE_SHIFT, SAT_SCALE, VALUE_SCALE)
    atlas.save(atlas_out)

    print(f"Wrote fluid item textures ({len(TIERS)} cells + housing)")
    print(f"Wrote drive atlas {atlas_out.relative_to(root)}")


if __name__ == "__main__":
    main()
