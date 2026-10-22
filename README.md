## Cobblemon: Player Shops

A Fabric mod for Minecraft 1.21.1 that adds player-run shops and admin shops that sell items for **CobbleDollars**, with a CobbleMerchant-style shop screen.

Shops only sell. Nobody can sell items *to* a shop.

If you enjoy the mod and would like to support me, [**buy me a coffee**](https://buymeacoffee.com/gaaus).

<!-- screenshots -->

## Dependencies

* Minecraft 1.21.1
* Fabric Loader 0.16.5+
* [Fabric API](https://modrinth.com/mod/fabric-api)
* [Cobblemon](https://modrinth.com/mod/cobblemon) 1.7.0+ (the range CobbleDollars requires). Developed and tested on 1.8.2
* [CobbleDollars](https://modrinth.com/mod/cobbledollars) 2.0.0+
* [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin): required by Cobblemon and CobbleDollars

The mod is required on both the client and the server.

### Optional

* [LuckPerms](https://modrinth.com/mod/luckperms): for the permission nodes. fabric-permissions-api is bundled, so nothing else is needed
* A claim mod to protect shops from being broken, for example [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims) or [Flan](https://modrinth.com/mod/flan)

## Installation

1. Install Fabric Loader for Minecraft 1.21.1.
2. Install Fabric API, Fabric Language Kotlin, Cobblemon, and CobbleDollars.
3. Place the mod `.jar` into your `mods` folder (on the server and on every client).
4. Launch Minecraft.

## Downloads

* **Modrinth:** <https://modrinth.com/mod/cobblemon-player-shops>
* **GitHub:** <https://github.com/Gaaus-Digital/Cobblemon-Player-Shops>

## Versioning

Mod files are named:

```text
cobblemon_player_shops-fabric-<mod version>+<Cobblemon version>+<Minecraft version>[-beta]
```

For example, `cobblemon_player_shops-fabric-0.1.0+1.8.2+1.21.1-beta.jar` is mod version **0.1.0**, built for **Cobblemon 1.8.2** on **Minecraft 1.21.1**.

## Features

- **Player Shop:** craftable. The owner stocks a 36-slot inventory and sets up to 24 offers (configurable)
- **Admin Shop:** infinite stock, and money spent is removed from the economy. Admin permission only, available from the creative tab or `/give`, no recipe
- **Shop screen:** a CobbleMerchant-style screen with offer search, quantity selector, price totals and stock display
- **Store balance:** sales go into the shop's balance, and the owner withdraws it with one button
- **Trusted players:** per-player *Allow inventory access* and *Allow withdraw* toggles, up to 8 players (configurable)
- **Inline offer editing:** an edit mode with the item, units per sale and price
- **Optional hopper input:** insert only; hoppers can never take items out
- **Breaking a shop:** the stock drops, and the store balance is always paid to the owner, even if they are offline. The player who breaks it never gets it. Protecting shops from breaking is left to claim mods
- **Server-authoritative design:** no item slots in the buy screen, anti bait-and-switch price checks, overflow-checked math, and rate limiting
- **Permission nodes:** LuckPerms support through the bundled fabric-permissions-api, with an op-level fallback
- **Transaction log** and a JSON config
- Dedicated creative mode tab: **Cobblemon: Player Shops**

## Blocks

| Block       | ID                                | How to get                                 |
| ----------- | --------------------------------- | ------------------------------------------ |
| Player Shop | `cobblemon_player_shops:player_shop` | Crafting                                |
| Admin Shop  | `cobblemon_player_shops:admin_shop`  | `/give` or the creative tab (admin only) |

Give yourself an Admin Shop:

```text
/give @s cobblemon_player_shops:admin_shop
```

## Crafting

The Player Shop is a shaped recipe. Planks can be any planks.

```text
Smooth Stone Slab | Smooth Stone Slab | Smooth Stone Slab
Planks            | Chest             | Planks
Planks            | Planks            | Planks
```

---

# Using a Shop

## Setting up

1. Place a Player Shop. You become its owner.
2. Right-click it to open the shop screen.
3. Click the pencil button in the top-right corner to enter edit mode.
4. Click an item in your own inventory (shown in the screen) to pick what you want to sell.
5. Set the units per sale (up to the item's stack size) and the price.
6. Click **Save**. The form resets so you can add the next offer.

To change an offer, stay in edit mode and click it in the list. To remove one, use its delete button. Click the pencil again to leave edit mode.

Use the **Inventory** button to open the shop's 36-slot stock and fill it.

## Buying

1. Right-click a shop and select an offer from the list. The search box filters the list.
2. Set the quantity with the arrows next to the quantity field, by typing it, or with the mouse wheel over the field. Hold **Shift** to change it by 10. **Ctrl** + an arrow jumps to the maximum (up) or minimum (down).
3. Press **Buy**.

If **Buy** is disabled, hover over it. The tooltip says why: no offer selected, out of stock, not enough stock, not enough money, or not enough inventory space.

## Store balance and withdrawing

Every sale adds the money to the shop's **Store Balance**. The owner sees it on the shop screen and withdraws it to their CobbleDollars balance with the withdraw button next to it.

## Trusted players

Only the owner sees the **Settings** button. In Settings, add a player by name and use the two toggles:

* **Inventory:** the player can open the shop's stock.
* **Withdraw:** the player can withdraw the store balance.

Trusted players see the store balance, plus the **Withdraw** and **Inventory** buttons according to their toggles. They cannot edit offers or open Settings. Admins and ops cannot bypass this either.

## Hopper input

The owner can turn on hopper input in Settings. Hoppers can then insert items into the shop's stock, but they can never take items out. Admin Shops have no hopper input.

## Breaking a shop and claims

When a shop is removed, by any means, its stock drops and the store balance is paid to the owner. The owner is paid even if they are offline, and the player who broke the shop gets nothing from it. The mod does not stop other players from breaking shops. Use a claim mod to protect them.

---

# Commands

`/ps` is a shorthand for `/playershops`.

Reload the config:

```text
/playershops reload
/ps reload
```

Show info about the shop you are looking at (owner, vault, offers, trusted players):

```text
/playershops info
/ps info
```

# Permissions

Permissions use [fabric-permissions-api](https://modrinth.com/mod/fabric-permissions-api), so LuckPerms works. Without a permissions mod, the default op level applies.

| Node                                  | Default | Grants                                |
| ------------------------------------- | ------- | ------------------------------------- |
| `cobblemon_player_shops.admin`        | op 2    | Place, use and edit Admin Shops       |
| `cobblemon_player_shops.command.reload` | op 2  | `/playershops reload`                 |
| `cobblemon_player_shops.command.info` | op 2    | `/playershops info`                   |

# Configuration

The config is `config/cobblemon_player_shops.json`. It is created on first start and is server-authoritative. Values are clamped to their range, and `/playershops reload` applies changes.

| Key                    | Default            | Range                     | Description                                                         |
| ---------------------- | ------------------ | ------------------------- | ------------------------------------------------------------------- |
| `maxPrice`             | 1,000,000,000      | 1 to 1,000,000,000,000    | Highest price of a single offer                                     |
| `maxTransaction`       | 1,000,000,000,000  | 1 to 10^15                | Highest total value of one purchase                                 |
| `maxVault`             | 10^15              | 1 to 10^18                | Highest store balance a shop can hold                               |
| `maxOffersPerShop`     | 24                 | 1 to 54                   | Offers per shop                                                     |
| `maxTrustedPerShop`    | 8                  | 0 to 32                   | Trusted players per shop                                            |
| `maxPurchaseQuantity`  | 64                 | 1 to 2304                 | Highest quantity per purchase                                       |
| `requireInventorySpace`| true               | true or false             | Require room in the buyer's inventory                               |
| `rateLimitActions`     | 10                 | 1 to 200                  | Shop actions allowed per rate-limit window                          |
| `rateLimitWindowTicks` | 20                 | 1 to 200                  | Length of the rate-limit window in ticks                            |
| `interactionRange`     | 8.0                | 2.0 to 64.0               | How far a player can be from a shop to keep using it, in blocks     |
| `logTransactions`      | true               | true or false             | Write purchases and shop changes to the transaction log             |

Defaults:

```json
{
  "maxPrice": 1000000000,
  "maxTransaction": 1000000000000,
  "maxVault": 1000000000000000,
  "maxOffersPerShop": 24,
  "maxTrustedPerShop": 8,
  "maxPurchaseQuantity": 64,
  "requireInventorySpace": true,
  "rateLimitActions": 10,
  "rateLimitWindowTicks": 20,
  "interactionRange": 8.0,
  "logTransactions": true
}
```

---

## Community

Need help, found a bug, or have a suggestion?

Join the Discord server:

**Discord:** <https://discord.gg/pFSDchJruc>

## Issues

If you encounter a bug, please open an issue on GitHub and include:

* Minecraft version
* Fabric Loader version
* Fabric API version
* Cobblemon version
* CobbleDollars version
* Mod version
* Crash log (if applicable)
* Steps to reproduce the issue

## Credits

* **[CobbleDollars](https://modrinth.com/mod/cobbledollars) by Harmex:** the shop screens' UI elements (backgrounds, buttons, offer rows, money pill, stock tag, arrows) are CobbleDollars' own textures. They are loaded at runtime from the installed CobbleDollars mod and are not redistributed in this mod. The screen layout is modelled on CobbleDollars' CobbleMerchant. The economy is CobbleDollars.
* **[fabric-permissions-api](https://modrinth.com/mod/fabric-permissions-api) by lucko (MIT):** bundled in the `.jar` for permission checks.
* **[Numismatic Overhaul](https://modrinth.com/mod/numismatic-overhaul) by glisco, Pois1x, Noaaan (MIT):** parts of the shop block code (the block, the floating-item renderer and the offers) are adapted from it. Its license is included in the `.jar` as `LICENSE_numismatic-overhaul`.

## Disclaimer

This is an unofficial fan project. It is not affiliated with or endorsed by Nintendo, Game Freak, Creatures Inc., The Pokémon Company, the Cobblemon team, or the CobbleDollars author. Pokémon and all related names are trademarks of their respective owners.

## License

This project is **All Rights Reserved**. See the `LICENSE` file.

Third-party assets and credits are listed in the Credits section above.
