# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Bookshelves for various minestuck wood types
- Ladders for various minestuck wood types
- Combination recipes and grist costs for items introduced in minecraft 1.19

### Changed

- Update to minecraft 1.19.2
- Minestuck now uses a biome modifier to add ores to the overworld (minestuck:overworld_ores)

### Removed

- Remove json grist cost for treated planks (There is already a grist cost for the planks item tag which will determine the cost instead)

### Fixed

- More gui text has translation keys now, making them translatable by a resource pack

## [1.18.2-1.9.2.0] - 2023-03-13

### Added

- New weapon "Shadowrazor"

### Changed

- Update metal boat entity textures
- Update textures for items, blocks and guis

### Fixed

- Reapply stone tablet crash fix that was mistakenly reverted during the mc1.16->mc1.18 move
- Fix some player decoy behavior when entering editmode while flying
- Machine guis with a GO-button can now be started with the keypad enter key