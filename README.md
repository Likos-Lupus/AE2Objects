# AE2 Objects

[![Stars](https://img.shields.io/github/stars/Likos-Lupus/AE2Objects?style=flat-square&label=Stars&labelColor=444444&color=eac54f)](https://github.com/Likos-Lupus/AE2Objects/)
[![Release](https://img.shields.io/github/v/release/Likos-Lupus/AE2Objects?style=flat-square&labelColor=444444&label=Release&include_prereleases)](https://github.com/Likos-Lupus/AE2Objects/releases)
[![GitHub CI](https://img.shields.io/github/actions/workflow/status/Likos-Lupus/AE2Objects/build.yml?style=flat-square&labelColor=444444&branch=master&label=GitHub%20CI)](https://github.com/Likos-Lupus/AE2Objects/actions/workflows/build.yml)
[![Modrinth](https://img.shields.io/badge/Modrinth-AE2%20Objcts-22ff84?style=flat-square&labelColor=444444)](https://modrinth.com/mod/ae2-objects/)
[![CurseForge](https://img.shields.io/badge/CurseForge-AE2%20Objcts-f16436?style=flat-square&labelColor=444444)](https://www.curseforge.com/minecraft/mc-mods/ae2-objects)

A **NeoForge addon mod** for **Applied Energistics 2** that introduces high-density **Deep Storage
Cells** with **no type limits**.

AE2Objects provides flexible, high-capacity storage solutions that remove the traditional AE2
63-type limit while maintaining balanced byte-based mechanics.

## Features

- **Deep Item Storage Cells**: High-density storage cells with **no item type limits**—every item
  consumes exactly 1 byte.
- **Multiple Capacity Tiers**: Available in **1K**, **4K**, **16K**, **64K**, and **256K** tiers.
- **Full AE2 Integration**: Works seamlessly with ME Drives, Cell Workbenches, Partitioning, Fuzzy
  Cards, and Inverter Cards.
- **Safe Cell Disassembly**: Sneak + right-click with an empty cell in hand (or on a block) to
  reclaim its storage housing and component.
- **In-Game Management & Recovery**: Built-in commands for checking cell UUIDs and recovering
  storage data.
- **Dedicated Creative Tab Integration**: Seamlessly integrated into Applied Energistics 2 creative
  tabs.

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

```bash
# On Linux / macOS
./gradlew clean build
```

On Windows (PowerShell / Command Prompt):

```powershell
.\gradlew.bat clean build
```

The compiled mod jar will be located in:

```text
build/libs/
```

### Development Runs

- **Client**: `./gradlew runClient`
- **Dedicated Server**: `./gradlew runServer`
- **Data Generation**: `./gradlew runServerData`

## World Compatibility Notice

AE2Objects is a major rewrite and rebrand of **AE2Things** for modern Minecraft versions. While core
storage concepts remain familiar, upgrading directly from legacy AE2Things save files is not
guaranteed to be seamless. Always make a backup of your worlds before migrating.

## Credits

AE2Objects is a continuation and derivative work based on
**[AE2Things](https://github.com/ProjectET/AE2Things)**, originally created by **ProjectET** and
maintained by **Technici4n**.

## License

This project is licensed under the [GNU Lesser General Public License v3.0 (LGPL-3.0)](LICENSE).

The original AE2Things copyright notices are preserved in [`LICENSE`](LICENSE), along with the
continued project copyright notices.
