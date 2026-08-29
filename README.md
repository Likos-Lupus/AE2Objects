# AE2Objects

**AE2Objects** is a Minecraft mod for NeoForge that adds helpful storage extensions to Applied
Energistics 2.

## Features

- **Deep Item Storage Cell**: High-density storage cells with **no item type limits**—every item
  consumes exactly 1 byte.
- Available in multiple capacity tiers: **1K**, **4K**, **16K**, **64K**, and **256K**.
- Full support for AE2 cell workbench partitioning, fuzzy cards, and inverter cards.
- Clean disassembly: Crouch + right-click an empty cell in hand or on a block to recover the storage
  housing and storage component.
- In-game recovery & inspection commands (`/ae2objects getuuid`, `/ae2objects recover <uuid>`).

## Requirements

- **Minecraft**: `26.1.2`
- **NeoForge**: `26.1.2.94` or compatible
- **Applied Energistics 2**: `26.1.10-beta` or compatible
- **Java**: `25`

## Building from Source

```bash
# On Linux/macOS
./gradlew build

# On Windows (PowerShell / pwsh)
.\gradlew.bat build
```

### Development Runs

- Client: `./gradlew runClient`
- Dedicated Server: `./gradlew runServer`
- Data Generation: `./gradlew runServerData`

## World Compatibility Notice

AE2Objects is a major rewrite and rebrand of AE2Things for Minecraft 26.1.2. Upgrading directly from
older AE2Things worlds is not guaranteed to be seamless. Always back up your worlds before
upgrading.

## Attribution & License

AE2Objects is a fork and derivative work based on **AE2Things**, originally created by **ProjectET**
and maintained by **Technici4n**.

- Current Maintainer: **Likos-Lupus**
- Licensed under the [MIT License](LICENSE).
