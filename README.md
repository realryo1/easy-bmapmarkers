
## Easy BlueMap Sign Markers & Lines

![plugin_icon](Icon_EasyBMSignMarkers.png)

Japanese version: [README_ja.md](README_ja.md)

This plugin lets you place signs in-game and display both markers and lines on BlueMap.

This plugin is based on the unmaintained [BlueMapSignMarkers](https://modrinth.com/plugin/bluemapsignmarkers), and also extends [EasyBlueMapSignMarkers](https://modrinth.com/plugin/easy-bluemap-sign-markers) with additional features and quality-of-life improvements.

Tested server: Paper
Probably works on: Folia / Spigot / Purpur

## Setup
You need to have **BlueMap** installed on your server. The plugin depends on it.
If you already have **BlueMap**, put the jar file into `plugins` folder on your server... and that's it!

On startup, legacy marker files are copied from `/plugins/EasyBMSignMarkers/marker-set-<world>.json` into this plugin folder and then migrated to `marker-set-<world>.yml`.

## Config
You can configure the tag wrappers used on sign line 1 in `plugins/EasyBlueMapSignMarkersAndLines/config.yml`.

- `startprefix`: start wrapper for sign tags
- `endprefix`: end wrapper for sign tags

Defaults:

- `startprefix: "="`
- `endprefix: "="`

With defaults, valid examples are `=map=`, `=BMLine=`, `=BMLineUnder=`.

## Edit mode (show hidden marker signs)
By default, marker signs are hidden. You can reveal them per-player only while editing.

Behavior:

- Edit mode is **player-specific**.
- While edit mode is ON, marker signs are visible to that player.
- While edit mode is OFF, marker signs are hidden for that player.
- If you try to place/interact on a location where a marker sign exists while edit mode is OFF, the action is canceled and a warning message is shown.

## How to use
Place any sign in the game. Fill the sign as follows:

- **1st line**: `=marker_icon_name=` (please see [Marker names](#marker-names) for available name tags)
- **2nd line:** text
- **3rd line:** text
- **4th line:** text

First line **MUST** be filled. If the 1st line does not contain a valid name, but still uses configured wrappers, the fallback value `map` is assigned.
At least one of lines 2-4 must contain text. If all are empty, the marker is not created.
Do not put spaces inside wrapped tags (valid with defaults: `=map=`, invalid: `= map =`).

## Example
Below example will create marker on your map with `star` icon assigned.

First, you build the sign

![sign1](example1.png)

![sign2](example2.png)

After a while, you will see the marker on your BlueMap

![sign3](example3.png)

And when you click it - the popup containing your sign description will appear

![sign4](example4.png)

## BMLine (connect multiple signs with a line)
You can also create a line marker by linking multiple signs.

Fill signs as follows:

- **1st line**: `=BMLine=` or `=BMLineUnder=`
- **2nd line**: line ID (e.g. `road-main`)
- **3rd line**: order number (e.g. `1`, `2`, `3`)
- **4th line**: optional color code (`#RRGGBB` or `#RRGGBBAA`)

How it works:

- Points are grouped by line ID and sorted by the numeric order.
- If a line has **2 or more points**, a BlueMap `LineMarker` is rendered.
- If a point-sign is broken and the line drops below 2 points, the line marker is removed.
- Rendering mode is decided by the first point (lowest order):
- If the first point is `=BMLineUnder=` (with default wrappers), the whole line is rendered as underground style (not hidden by terrain).
- If the first point is `=BMLine=` (with default wrappers), the whole line uses normal style.
- Line color is also decided by the first point (lowest order):
- If first point line 4 has `#RRGGBB`, the whole line uses that RGB with default alpha.
- If first point line 4 has `#RRGGBBAA`, the whole line uses that RGBA (including alpha override).
- Invalid color values fall back to default line color.

Notes:

- Line IDs are scoped per world.

Notes:

- There are currently no other plugin commands.
- Existing sign workflows (`=icon=`, `=BMLine=`, `=BMLineUnder=` with default wrappers) are event-driven and do not require commands.

## Permissions

| Permission | Default | Description |
|---|---|---|

| `easybmsignmarkers.edit` | op | Allows using `/bmedit` to show hidden marker signs |

Notes:

- No additional permission nodes are required for placing or removing sign markers in the current implementation.

## Marker names
These are the names that are available for the markers, plus the corresponding icons. Just use any of the values between brackets - `[` and `]`, e.g. `[bank]` - to place the marker on the **BlueMap**.
It does not matter if you use lowercase or uppercase. The plugin will handle it.

| Name         |                                Icon                                 | Note           | Name        |                                                               Icon                                                                | Note           |
|--------------|:-------------------------------------------------------------------:|----------------|-------------|:---------------------------------------------------------------------------------------------------------------------------------:|----------------|
| anchor       |       ![anchor](src/main/resources/markers/dynmap/anchor.png)       |                | key         |                                         ![key](src/main/resources/markers/dynmap/key.png)                                         |                |
| bank         |         ![bank](src/main/resources/markers/dynmap/bank.png)         |                | king        |                                        ![king](src/main/resources/markers/dynmap/king.png)                                        |                |
| basket       |       ![basket](src/main/resources/markers/dynmap/basket.png)       |                | left        |                                        ![left](src/main/resources/markers/dynmap/left.png)                                        |                |
| bed          |          ![bed](src/main/resources/markers/dynmap/bed.png)          |                | lightbulb   |                                   ![lightbulb](src/main/resources/markers/dynmap/lightbulb.png)                                   |                |
| beer         |         ![beer](src/main/resources/markers/dynmap/beer.png)         |                | lighthouse  |                                  ![lighthouse](src/main/resources/markers/dynmap/lighthouse.png)                                  |                |
| bighouse     |     ![bighouse](src/main/resources/markers/dynmap/bighouse.png)     |                | lock        |                                        ![lock](src/main/resources/markers/dynmap/lock.png)                                        |                |
| blueflag     |     ![blueflag](src/main/resources/markers/dynmap/blueflag.png)     |                | **map**         |                                         ![map](src/main/resources/markers/dynmap/map.png)                                         | Fallback value |
| bomb         |         ![bomb](src/main/resources/markers/dynmap/bomb.png)         |                | minecart    |                                    ![minecart](src/main/resources/markers/dynmap/minecart.png)                                    |                |
| bookshelf    |    ![bookshelf](src/main/resources/markers/dynmap/bookshelf.png)    |                | offlineuser |                                 ![offlineuser](src/main/resources/markers/dynmap/offlineuser.png)                                 |                |
| bricks       |       ![bricks](src/main/resources/markers/dynmap/bricks.png)       |                | orangeflag  |                                  ![orangeflag](src/main/resources/markers/dynmap/orangeflag.png)                                  |                |
| bronzemedal  |  ![bronzemedal](src/main/resources/markers/dynmap/bronzemedal.png)  |                | pinkflag    |                                    ![pinkflag](src/main/resources/markers/dynmap/pinkflag.png)                                    |                |
| bronzestar   |   ![bronzestar](src/main/resources/markers/dynmap/bronzestar.png)   |                | pirateflag  |                                  ![pirateflag](src/main/resources/markers/dynmap/pirateflag.png)                                  |                |
| building     |     ![building](src/main/resources/markers/dynmap/building.png)     |                | pointdown   |                                   ![pointdown](src/main/resources/markers/dynmap/pointdown.png)                                   |                |
| cake         |         ![cake](src/main/resources/markers/dynmap/cake.png)         |                | pointleft   |                                   ![pointleft](src/main/resources/markers/dynmap/pointleft.png)                                   |                |
| camera       |       ![camera](src/main/resources/markers/dynmap/camera.png)       |                | pointright  |                                  ![pointright](src/main/resources/markers/dynmap/pointright.png)                                  |                |
| cart         |         ![cart](src/main/resources/markers/dynmap/cart.png)         |                | pointup     |                                     ![pointup](src/main/resources/markers/dynmap/pointup.png)                                     |                |
| caution      |      ![caution](src/main/resources/markers/dynmap/caution.png)      |                | portal      |                                      ![portal](src/main/resources/markers/dynmap/portal.png)                                      |                |
| chest        |        ![chest](src/main/resources/markers/dynmap/chest.png)        |                | purpleflag  |                                  ![purpleflag](src/main/resources/markers/dynmap/purpleflag.png)                                  |                |
| church       |       ![church](src/main/resources/markers/dynmap/church.png)       |                | queen       |                                       ![queen](src/main/resources/markers/dynmap/queen.png)                                       |                |
| coins        |        ![coins](src/main/resources/markers/dynmap/coins.png)        |                | redflag     |                                     ![redflag](src/main/resources/markers/dynmap/redflag.png)                                     |                |
| comment      |      ![comment](src/main/resources/markers/dynmap/comment.png)      |                | right       |                                       ![right](src/main/resources/markers/dynmap/right.png)                                       |                |
| compass      |      ![compass](src/main/resources/markers/dynmap/compass.png)      |                | ruby        |                                        ![ruby](src/main/resources/markers/dynmap/ruby.png)                                        |                |
| construction | ![construction](src/main/resources/markers/dynmap/construction.png) |                | scales      |                                      ![scales](src/main/resources/markers/dynmap/scales.png)                                      |                |
| cross        |        ![cross](src/main/resources/markers/dynmap/cross.png)        |                | shield      |                                      ![shield](src/main/resources/markers/dynmap/shield.png)                                      |                |
| cup          |          ![cup](src/main/resources/markers/dynmap/cup.png)          |                | sign        |                                        ![sign](src/main/resources/markers/dynmap/sign.png)                                        |                |
| cutlery      |      ![cutlery](src/main/resources/markers/dynmap/cutlery.png)      |                | silvermedal |                                 ![silvermedal](src/main/resources/markers/dynmap/silvermedal.png)                                 |                |
| diamond      |      ![diamond](src/main/resources/markers/dynmap/diamond.png)      |                | silverstar  |                                  ![silverstar](src/main/resources/markers/dynmap/silverstar.png)                                  |                |
| dog          |          ![dog](src/main/resources/markers/dynmap/dog.png)          |                 | skull       |                                       ![skull](src/main/resources/markers/dynmap/skull.png)                                       |                   |
| door         |         ![door](src/main/resources/markers/dynmap/door.png)         |                | star        |                                        ![star](src/main/resources/markers/dynmap/star.png)                                        |                |
| down         |         ![down](src/main/resources/markers/dynmap/down.png)         |                | sun         |                                         ![sun](src/main/resources/markers/dynmap/sun.png)                                         |                |
| drink        |        ![drink](src/main/resources/markers/dynmap/drink.png)        |                | temple      |                                      ![temple](src/main/resources/markers/dynmap/temple.png)                                      |                |
| exclamation  |  ![exclamation](src/main/resources/markers/dynmap/exclamation.png)  |                | theater     |                                     ![theater](src/main/resources/markers/dynmap/theater.png)                                     |                |
| factory      |      ![factory](src/main/resources/markers/dynmap/factory.png)      |                | tornado     |                                     ![tornado](src/main/resources/markers/dynmap/tornado.png)                                     |                |
| fire         |         ![fire](src/main/resources/markers/dynmap/fire.png)         |                | tower       |                                       ![tower](src/main/resources/markers/dynmap/tower.png)                                       |                |
| flower       |       ![flower](src/main/resources/markers/dynmap/flower.png)       |                | tree        |                                        ![tree](src/main/resources/markers/dynmap/tree.png)                                        |                |
| gear         |         ![gear](src/main/resources/markers/dynmap/gear.png)         |                | truck       |                                       ![truck](src/main/resources/markers/dynmap/truck.png)                                       |                |
| goldmedal    |    ![goldmedal](src/main/resources/markers/dynmap/goldmedal.png)    |                | up          |                                          ![up](src/main/resources/markers/dynmap/up.png)                                          |                |
| goldstar     |     ![goldstar](src/main/resources/markers/dynmap/goldstar.png)     |                  | walk        |                                        ![walk](src/main/resources/markers/dynmap/walk.png)                                        |               |
| greenflag    |    ![greenflag](src/main/resources/markers/dynmap/greenflag.png)    |                | warning     |                                     ![warning](src/main/resources/markers/dynmap/warning.png)                                     |                |
| hammer       |       ![hammer](src/main/resources/markers/dynmap/hammer.png)       |                | world       |                                       ![world](src/main/resources/markers/dynmap/world.png)                                       |                |
| heart        |        ![heart](src/main/resources/markers/dynmap/heart.png)        |                | wrench      |                                      ![wrench](src/main/resources/markers/dynmap/wrench.png)                                      |                |
| house        |        ![house](src/main/resources/markers/dynmap/house.png)        |                | yellowflag  |                                  ![yellowflag](src/main/resources/markers/dynmap/yellowflag.png)                                  |                |


The images used are modified versions of original Dynmap assets. You can find them here:

- [Dynmap on github](https://github.com/webbukkit/dynmap)
- [Original resources](https://github.com/webbukkit/dynmap/tree/v3.0/DynmapCore/src/main/resources/markers)
