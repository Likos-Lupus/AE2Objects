# AE2 Objects

[![Stars](https://img.shields.io/github/stars/Likos-Lupus/AE2Objects?style=flat-square&label=Stars&labelColor=444444&color=eac54f)](https://github.com/Likos-Lupus/AE2Objects/)
[![Release](https://img.shields.io/github/v/release/Likos-Lupus/AE2Objects?style=flat-square&labelColor=444444&label=Release&include_prereleases)](https://github.com/Likos-Lupus/AE2Objects/releases)
[![GitHub CI](https://img.shields.io/github/actions/workflow/status/Likos-Lupus/AE2Objects/build.yml?style=flat-square&labelColor=444444&branch=master&label=GitHub%20CI)](https://github.com/Likos-Lupus/AE2Objects/actions/workflows/build.yml)
[![Modrinth](https://img.shields.io/badge/Modrinth-AE2%20Objects-22ff84?style=flat-square&labelColor=444444)](https://modrinth.com/mod/ae2-objects/)
[![CurseForge](https://img.shields.io/badge/CurseForge-AE2%20Objects-f16436?style=flat-square&labelColor=444444)](https://www.curseforge.com/minecraft/mc-mods/ae2-objects)

A **NeoForge addon mod** for **Applied Energistics 2** that introduces high-density **Deep Storage
Cells** with **no type limits**.

AE2Objects provides flexible, high-capacity storage solutions that remove the traditional AE2 type
limit while maintaining balanced byte-based mechanics.

## Features

- **No type limits** — a deep cell is bounded only by its byte capacity, not by a type count.
- **Item and fluid** deep cells, available as **drive** cells or **portable** (pocket ME Chest)
  cells.
- **Ten capacity tiers** — `1k`, `4k`, `16k`, `64k`, `256k`, `1m`, `4m`, `16m`, `64m`, `256m`. The
  `1m`–`256m` tiers mirror **MEGA Cells** sizes.
- **Full AE2 integration** — works with ME Drives, ME Chests, the Cell Workbench, partitioning,
  Fuzzy Cards, Inverter Cards and the Overflow Destruction Card. Portable cells also accept Energy
  Cards and can be dyed.
- **Safe disassembly** — sneak + right-click with an empty cell in hand (or on a block) to reclaim
  its housing, component and upgrades. Portable cells transfer their remaining charge into the
  returned energy cell.
- **In-game management & recovery** — commands for checking cell UUIDs and recovering storage data.

## Optional Integrations

- **[MEGA Cells](https://github.com/62832/MEGACells)** — the `1m`–`256m` deep tiers are crafted from
  MEGA's storage components. MEGA-tier cells are always registered; their recipes only appear when
  MEGA Cells is installed.
- **[Applied Mekanistics](https://github.com/ramidzkh/Applied-Mekanistics)** — chemical deep cells
  are designed and reserved, but stay inactive until Applied Mekanistics ships a build for this
  Minecraft version.

## Requirements

- **Minecraft**: `26.1.2`
- **NeoForge**: `26.1.2.98` or compatible
- **Applied Energistics 2**: `26.1.10-beta` or compatible
- **Java**: `25`

## Installation

1. Make sure you have installed **Minecraft**, **NeoForge**, and **Applied Energistics 2**.
2. Download the latest release from
   the [Releases](https://github.com/Likos-Lupus/AE2Objects/releases) page.
3. Place the downloaded `.jar` file into your `.minecraft/mods` directory.
4. Launch the game and start crafting Deep Storage Cells!

## Commands

AE2Objects includes administrative commands for inspecting and recovering deep storage cells:

| Command                      | Permission   | Description                                                                    |
|:-----------------------------|:-------------|:-------------------------------------------------------------------------------|
| `/ae2objects getuuid`        | All Players  | Gets the UUID of the deep storage cell in hand and copies it to the clipboard. |
| `/ae2objects recover <UUID>` | Level 2 (OP) | Reconstructs and gives a storage cell corresponding to the specified UUID.     |

## Building from Source

The project uses [Stonecutter](https://github.com/kikugie/stonecutter) for multi-version builds.
Only Minecraft `26.1.2` is registered today.

Build and collect every registered version:

```bash
# On Linux / macOS
./gradlew chiseledBuildAndCollect
```

On Windows (PowerShell / Command Prompt):

```powershell
.\gradlew.bat chiseledBuildAndCollect
```

The collected jars are written to:

```text
build/libs/
```

- Default (dirty) build: `ae2objects-<modVersion>-<commit>+<mcVersion>.jar`
- Release build (`./gradlew :26.1.2:releaseJar`): `ae2objects-<modVersion>+<mcVersion>.jar`

### Development Runs

The versioned project is `:26.1.2`.

- **Client**: `./gradlew :26.1.2:runClient`
- **Dedicated Server**: `./gradlew :26.1.2:runServer`
- **Data Generation**: `./gradlew :26.1.2:runServerData`
- **Game Tests**: `./gradlew gameTestActive`

## Documentation

Design and reference documentation lives in [`docs/`](docs/README.md): architecture, content
catalog, numeric values, integrations and the refactor notes.

## World Compatibility Notice

AE2Objects is a major rewrite and rebrand of **AE2Things** for modern Minecraft versions. While core
storage concepts remain familiar, upgrading directly from legacy AE2Things save files is not
guaranteed to be seamless. Always make a backup of your worlds before migrating.

## Credits

AE2Objects is a continuation and derivative work based on
**[AE2Things](https://github.com/ProjectET/AE2Things)**, originally created by **ProjectET** and
maintained by **Technici4n**.

### Textures

Several placeholder textures (the deep fluid item icons and the in-drive `drive_cells` atlas, and
the portable-cell housings) are recoloured derivatives of **Applied Energistics 2** assets (©
AlgorithmX2 and the Applied Energistics team, LGPL-3.0), generated by
[`tools/generate_fluid_textures.py`](tools/generate_fluid_textures.py) and
[`tools/generate_portable_textures.py`](tools/generate_portable_textures.py). The per-tier "size
indicator" and portable "side" pixels are used unmodified.

## License

This project is licensed under
the [GNU Lesser General Public License v3.0 (LGPL-3.0)](https://github.com/Likos-Lupus/AE2Objects/blob/master/LICENSE).

The original AE2Things copyright notices are preserved in [
`LICENSE`](https://github.com/Likos-Lupus/AE2Objects/blob/master/LICENSE), along with the continued
project copyright notices.
