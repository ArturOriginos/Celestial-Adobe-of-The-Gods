# Celestial chapter 2 (beta 2.7x)

**English** | [Русский](README.ru.md)

A NeoForge 1.21.1 mod about the sky realm of the gods: advanced enchanting, unbreakable Celestia weapons and tools, the Guardian of the Gods, the Celestia dimension of floating islands, the Cursed Niflheim biome in the Nether with its boss, and the ultimate tool, the Paxel of Gods and the Cursed. Current version: **1.3**.

All items and blocks are in the creative tab **"Celestial chapter 2"**. Server config: `world/serverconfig/celestial-server.toml`.

## Enchanting

- **Advanced Enchanting Table** ("Grace of the Gods of Celestia"): recipe `B G B / G E G / B G B` (B = bookshelf, G = gold block, E = enchanting table). Enchantment levels are doubled (Efficiency 10, Silk Touch 2 mines spawners).
- **Advanced Book**: 4 gold nuggets (top/bottom/left/right) around a book. Put it into the advanced table to get only the custom enchantments. An enchanted book changes to a shimmering sprite.
- Custom enchantments: Miner's Syndrome (1), Slayer (2), Bleeding (3), Experience Boost (2), Magnet (1), Nature's Wrath, Soul Devourer (I-V, 2% per level to drop a spawn egg from a mob, swords only). Sneaking disables the 3x3 mining and the magnet.
- **Solar Anvil**: put an item and a "Boon of Hephaestus and Athena" book with custom enchantments into it. Custom enchantments can be transferred from a book to an item **only** in the Solar Anvil. Make it from the table plus an anvil, or in a normal anvil with a book that has the "Blessing of Helios" enchantment.

## Celestia items and the Guardian of the Gods

- **Guardian of the Gods**: summon egg only (creative tab "Spawn Eggs"). 280 HP, spikes from underground, drops the Soul of the Guardian of the Gods. It has its own boss bar.
- The Ender Dragon always drops the soul. The Wither drops it with a 25% chance, and every 5th kill guarantees it.
- Crafting (soul in the center, one row): sword = 2 netherite swords, axe = 2 netherite axes, pickaxe-shovel = netherite pickaxe + netherite shovel, shield = 2 normal shields. Celestia Shears = 2 shears + soul.
- All Celestia items are unbreakable. Their names shimmer white-gold. Tools are twice as fast as netherite. The pickaxe-shovel breaks stone, dirt and ores instantly.
- **Celestia Block**: 8 runes + soul, can only be broken with a Celestia or Cursed pickaxe.
- The Celestia Sword, Axe, Pickaxe and Shears have 3D (voxel) models. The **Protective Rune of Celestia** (shield) is a flat texture drawn like a real shield, with its own winged-star design.

## World of Celestia

- Blocks: earth, stone, sand, ore, ice, dense and fluffy cloud, glowstone, grass, flower, mushroom, crystal. Celestia Ore drops exactly 1 Celestia Rune.
- **Celestia Bricks** (4 Celestia Stone in a 2x2), **Celestia Brick Stairs**, **Celestia Brick Slab** and **Celestia Glass** (smelt Celestia Ice). White-lavender stone with gold corners and a glowing star. Stairs and slabs are also in the stonecutter. The blocks emit a soft light.
- **Celestia Key**: `GG / G. / R.` (G = gold ingot, R = Celestia Rune).
- **Celestial Tree**: wood, planks, slab, stairs, leaves, sapling (bone meal only, 10%). Grows into a big tree with a 3x3 trunk. Leaves give 100 XP at 20%.
- **Giant Celestial Tree**: a real structure with a room, chests and a Guardian (`/locate structure celestial:giant_celestial_tree`).

## Cursed Niflheim and the Cursed

- The **Cursed Niflheim** biome (`celestial:cursed_niflheim`) lives in the Nether: cursed stone, basalt, floating islands, cursed trees of three sizes, wither skeletons and ghasts. Find it with `/locate biome celestial:cursed_niflheim`. The file `data/minecraft/dimension/nether.json` is overridden, so other Nether-changing mods may conflict.
- Cursed blocks: Cursed Nether Bricks, Stairs, Slab, Glass, rune stone, tree wood and more.
- **The Cursed** (boss): 500 HP, armor 16, damage 36. Rises from the ground, summons crimson spikes, adds a fourth ring below 50% health. Has a custom 3D model and boss bar. Drops the **Cursed Soul**. Summon: spawn egg or `/summon celestial:cursed_boss`.
- Cursed weapons: Sword, Axe, Pickaxe, **Shield** (shown as a real shield).
- **Cursed Church** (`celestial:cursed_church`): a structure in Cursed Niflheim with the Cursed boss already waiting in the main hall. Find it with `/locate structure celestial:cursed_church`.

## Celestia dimension

- Dimension `celestial:celestia`: **50000 x 50000 blocks** (border from -25000 to +25000) of floating islands above a sea of clouds you can walk on.
- Small (radius 7-15), medium (20-34) and large (44-64) islands. Celestia earth on top, stone and Celestia Ore inside, glowstone on the underside. Grass, flowers, mushrooms, crystals, trees, sheep, chickens and rabbits. No hostile mobs.
- **Portal**: build a vertical frame of Celestia Portal blocks (inside 2x3 up to 21x21, corners not needed) and **right-click the frame with the Celestia Key**. One craft gives 10 portal blocks. Works from any dimension.
- The center (0, 0) has a flat spawn island with a return portal. The portal back returns you to where you came from.
- Advancement "Above the Clouds" for the first visit. Teleport: `/execute in celestial:celestia run tp @s 0 140 0`.

## United Soul and the Paxel

- **United Soul of Gods and the Cursed**: shapeless craft of the Soul of the Guardian of the Gods + the Cursed Soul.
- **Paxel of Gods and the Cursed** (`celestial:paxel`): a multi-tool. Instantly mines everything a pickaxe, axe, shovel or hoe can mine, deals **50 damage**, unbreakable, strips logs on right click. Works with the enchantments of Celestia axes and pickaxes. The name shimmers orange, red and dark.
- Recipe (cross): United Soul in the center, Celestia Pickaxe on top, Celestia Axe on the left, Cursed Sword on the right, Cursed Axe at the bottom (all consumed).
- Advancement **"The End? Or the Beginning of the End?..."** for obtaining it (hidden until then).

## Cursed Book and the ??? dimension

- **Cursed Book**: craft 4 Books + Soul of the Cursed One (shapeless). Right-click to travel to the empty dimension with the biome "???" (celestial:unknown); right-click the book again inside to return where you came from.
- The dimension holds one structure (the Eye): you appear on the floor of the great hall, the All-Knowing Angel stands on the pedestal in the centre (a new one appears on entry if none is alive).
- Nothing can be broken or placed there (creative mode is exempt), explosions do not destroy blocks.

## Versions

Versioning: small edits bump the third number (1.7.1, 1.7.2), big updates bump the second (1.7, 1.8).

- **beta 3.0xy** - English health bar for Hades (God of Death / Hades), chosen by the game language.
- **beta 3.0x** - Final boss: **Hades, God of Death** (20000 health, 80-90 damage). Spawn cutscene with the camera on him: three lines of dialogue, then the Soul of Celestia answers (only for players who have it). Three phases with immortality at 60% (6 s) and 30% (10 s, heals 3000); attacks: flame spears, homing skulls, dark pillars, eruption, crystal spikes, comets, chains, and in the last phase a ritual circle explosion. Own health bar. For now he is spawned with the Hades Spawn Egg or `/summon celestial:hades`.
- **beta 2.9xy** - The Forbidden Fruit is craftable: United Soul + 2 Apples (shapeless).
- **beta 2.9x** - New boss: **Cerberus** (10000 health, about 80 damage; bite, dash, fire breath and shockwave; breaks blocks; at 5000 health he roars, is immortal for 10 seconds and restores all his health once). Summoned by the **Forbidden Fruit** (right-click, then answer Yes or No in chat; "No" keeps the fruit). When he falls, Hades curses you in chat and Cursed Niflheim spreads twice as fast. Drops the Heart of Cerberus. Own health bar with a three-headed frame. `tools/bbmodel_to_bbdata.py` converts Blockbench models.
- **beta 2.8xy** - Damage of Fenrir, the All-Knowing Angel and the God of Celestia cut by 1.5x. Full God armor gives Resistance III, full Celestia armor Resistance II. Killing the All-Knowing Angel opens red eyes in the sky and Hades writes meaningless symbols in chat; killing Fenrir brings eternal night. Operators can toggle both with `/celestialworld eternal_night|sky_eyes <true|false>`.
- **beta 2.8x** - Cursed Book, the ??? dimension with the Eye structure, new Guardian of the Gods health bar, Fenrir bar in English, Wither custom-model crash fixed.
- **1.3** - New advancements: Cursed items (soul, sword, axe, pickaxe-shovel, shield, full arsenal), killing the Cursed One, killing the God of Celestia, and the full Celestia armor set.
- **1.2** - God of Celestia: Star Rain, Pillars of Judgement and the Beam now have real visuals (light pillars, falling stars, beams); every hit of the boss is capped at 20 health. Celestia armor can be enchanted at the Grace of the Gods of Celestia table. Full set (chestplate + boots) gives +20 max health (one more row of hearts) and a light-yellow armor bar.
- **1.1.2** - Fixed the build error in the Celestia armor halo code (wrong `PlayerSkin` import).
- **1.1.1** - The Celestia Shield texture is redrawn on the Cursed Shield layout (same shape, no stray edge strip) in Celestia colors: white and lavender field, golden border and cross, blue gem.
- **1.1** - **Celestia Chestplate** and **Celestia Boots**: 30 armor each, a glowing halo floats above the wearer. Made in the Solar Anvil: Netherite Chestplate / Boots + Guardian Soul (30 levels).
- **1.0** - New final boss **God of Celestia** (1000 HP, two phases, 30 s of immortality at half health, custom health bar, Paxel in hand that never drops). Drops the **Ring of Divine Flight** (creative flight while in the inventory) and the **Official Cheat Permit** (a shimmering gray paper that does nothing). New Celestia structures **Celestial Stash** (common) and **Celestial Adobe** (rare, the God of Celestia spawns inside).
- **1.8.1** - The Celestia shield is a real flat shield again (like the Cursed Shield) with a new shape and a winged-star design on the front.
- **1.8** - Celestia Bricks, Celestia Brick Stairs, Celestia Brick Slab and Celestia Glass (with recipes, stonecutter and loot).
- **1.7.2** - New 3D models for Celestia Sword, Axe, Pickaxe and Shears. (The voxel shield of this version was replaced in 1.8.1.) Advancement for the Paxel. README in English and Russian.
- **1.7.1** - Paxel is twice as big in the hand (first and third person).
- **1.7** - Paxel of Gods and the Cursed.
- **1.6.1** - Cursed Church with the Cursed boss, United Soul, Celestia Shield displayed as a shield.
- **1.6** - Celestia dimension and portal.
- **1.5** - Cursed Nether Bricks, Stairs, Slab and Glass.
- **1.4** - The Cursed boss, Cursed Soul, boss bars (the Guardian of the Gods got his own too).
- **1.3** - Cursed Niflheim biome in the Nether.
- **1.2** - Giant Celestial Tree structure, Soul Devourer, Celestia Ore tuning, many fixes.
- **1.1** - Blockbench shield, new enchanting table and Solar Anvil UI, Celestia Block, Celestia Shears, Nature's Wrath, tree sizes.
