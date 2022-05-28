# mono
Repository for most plugins, installing via /plu i [Name] <br />
The configurations for the plugins are in the folder conf <br />

# Simple plugins
autorespawn - Automatic respawn after death <br />
boosters - Discharges active boosters <br />
chancecmd - Executes commands with some chance, /chancecmd <br />
chat - Replaces vanilla chat <br />
clearchat - Replaces words, and makes it a bit of a hindrance to flubbing <br />
closegui - Closes the player's inventory /closegui <br />
commandcooldown - Delays for commands <br />
commandsbytime - Executing commands once in a while  <br />
disableanycraft - Disable crafting <br />
disablechunkunload - Disables chunk unloading <br />
disablecorus - Prohibits the use of a corus <br />
disablefeed - Disable feed <br />
disablejoin - Prohibits entry if there are insufficient statistics <br />
disableitemdrop - Prohibits throwing things away <br />
disablemobexp - Disables experience from mobs <br />
disablepearl - Prohibits use of ender pearls <br />
disableshalkerinec - Prohibits putting shulkers in the ender chest <br />
guicommand - Custom GUI's <br />
hideall - Hide all players <br />
itemcommand - Executing commands when the item is clicked <br />
iteminfo - Outputs information about the item in hand, /iteminfo <br />
lowtps - Executes commands when tps is low <br />
mobpickupitems - Prohibits mobs from picking up objects <br />
morestatschat - Adds statistics to the chat <br />
morestatscmd - /mystats, shows a player's statistics <br />
moretext - Commands for prefixes to messages, /et [Player] [Message] <br />
randombylist - Executes a random command from a list, /randombylist <br />
randommoney - Give a random percentage of the current amount of money <br />
removeitem - Removes the specified number of items from the hand <br />
removequitmsg - Disable quit message <br />
scrmsg - Command to send out titles and action bars <br />
setcustomname - Set custom name for a player <br />
setnbt - Add NBT to item in hand, /setnbt <br />
sound - Playing sound to the player <br />
statsdeath - Adds to the statistics deaths <br />
statskill - Adds to the statistics murders <br />
texttower - Interserver chat <br />
trash - /trash, clutter removal <br />
triggermsg - Executing commands when keywords are spoken in chat <br />
withoutsnow - Disables the appearance of snow blocks when snow falls <br />
worldgen - Generate world <br />
<br />
# Libs
configs - Configurations for plugins, used literally everywhere<br />
cuboid - Cuboid with two points<br />
donate - Interfaces for donation, require implementation<br />
gui - Create in-game GUI, ConfigInventory <br />
money - Balance management, Money <br />
booster - Boosters, /booster <br />
playerinfo - Getting a readable player name, suffixes, prefixes, nickname. PlayerReadable <br />
provide - See plugin text <br />
pstore - Stores temp information in player <br />
text - Anything to do with sending a text <br />
cmdexec - ```Commands(listOf("text %player% lol")).exec(plugin, player)``` <br />
readablelong - ```100000000L.readable()``` -> 100,000,000 <br />
morestats - Statistics for each player /morestats, @StatsAPI <br />
spi - Store information about player, see morestats <br />
sspi - Simpler version of spi <br />
tower - Used to link several servers <br />
