# Angband for Android 4.2.2

An unofficial port of the roguelike Angband to Android.

This work is based on the excellent port of **Angband** found [here](https://github.com/takkaria/angband-android).

The transparent keyboard was borrowed from [here](https://github.com/Shaosil/Android-Sil) (with modifications).

To customize your game experience, please go to settings and preferences by long-pressing the screen. The port has many options and you have to play a bit with them to find your optimal setup.

Recommended game experience: Landscape Orientation, duplicate tiles on, center_player option on (this one inside Angband).

The game has two input methods: the soft keyboard and the button ribbon.

### The soft keyboard

It has three modes (alphanumeric, symbols and control-keys) and several opacity settings to make playing a bit easier on small devices.

Some keyboard shorcuts:

    Long-Press over game view -> Show Quick Settings
    Long-Press "m" -> Show Quick Settings
    "..." -> Hide some keys (pressing a hidden key displays the keyboard again)
    Long-Press "..." -> Hide all keys excluding this one
    Long-Press "Ctrl^" -> Make the keyboard almost opaque
    Long-Press "Sym" -> Show the button ribbon
    Long-Press "p" -> Ctrl+P (recall messages)
    Long-Press "o" -> Ctrl+F (recall last message)
    Long-Press "f" -> Ctrl+F (recall level feeling)
    Long-Press "u" -> U (use item)
    Long-Press "q" -> Escape
    Long-Press "~" -> Escape
    Long-Press over other keys (most of them) -> shorcut for Hide some keys

### The button ribbon

Accessed when the soft keyboard is hidden, the button ribbon has two modes (command and full).

In command mode, the ribbon contains icons for the most used commands in the game. Some commands perhaps are missing in this mode (zap a rod, aim a wand, etc.), but most of them can be triggered using the Inventory, Alter and Use commands.

It's important to remember that directions can be used to cycle between equipment, inventory, floor and quiver.

To reduce the number of buttons in command mode, the Alter command can also open chests and close doors.

![alt text](https://github.com/Cuboideb/angbandroid/blob/master/app/src/main/assets/ribbon_help.jpg?raw=true)

In the preferences section, you can find some useful options for the ribbon: "Ribbon button size multiplier" and "Auto display long lists".

### Custom keymaps

While using the button ribbon, the settings menu gets the item "Manage keymaps". It can be used to quickly define common keymaps shortcuts as you like.

![alt text](https://github.com/Cuboideb/angbandroid/blob/master/app/src/main/assets/keymap_editor.jpg?raw=true)

### The touch directionals

The game view can be configured to display 9 squares for moving the character in the dungeon. The action of the center square can be set to do different things.

The touch d. can be displayed to the right, all together. In that case the center square has another use. A long-press allows you to drag and drop the whole set to another zone of the screen.

Besides the touch directionals, every touch in the term view is sent to the core as a mouse press, allowing movement and menu selection.

### Graphics modes

This port support the usual graphic tilesets of Angband (go to Preferences). You can also set pseudo-ascii mode. In that mode monsters and player are shown as ascii letters and everything else with graphic tiles.

Enjoy!

Angband homepage [http://rephial.org](http://rephial.org)

Forum and resources [http://angband.oook.cz](http://angband.oook.cz)

Online manual [https://angband.readthedocs.io/en/latest/index.html](https://angband.readthedocs.io/en/latest/index.html)

### Changelog:

Version 1.26 - 2020-11-01
- From now on, subwindows do not overlap the dungeon, this is useful for
text mode.
- New quick settings option: Reset layout (Portrait or Landscape). It gives you a tool to restore recommended parameters after experimenting with the settings.
- The sidebar of Angband can be displayed in vertical fashion, becoming the topbar. It is shown in a different subwindow so it has its own font multiplier (check out Preferences). IMPORTANT: the term #4 in the in-game option screen is by default set to "Display player (topbar)" to make this work.
- The port now tries to adjust the layout after modifiyng certain preferences (subwindows, topbar). You can use the "Fit Width", "Fit Height" and the volume keys to finetune the layout.
- The mouse icon at the top can be hidden.
- Advanced keyboard is the default keyboard for portrait mode. If you want, set the classic keyboard in Preferences.

Version 1.25 - 2020-08-20
- Added an advanced soft keyboard. It has only 2 pages, supports key resizing and custom keymaps actions. Includes an option for vertical layout. Ctrl sequences are activated by pressing the shift key for a second time..
- Added 2 variants, FrogComposband 7.1-salmiak and NPPAngband 0.4.1. Toggle is in Preferences. In a near future we will have FAangband 2.0.
- Each game Profile now has its own keymaps.
- Changed a couple of buttons in the ribbon. (Merged the shift and control modifiers into one button).
- Added a toogle in the Quick Setting to activate Running mode.
- Plenty of fixes and code refactoring.

Version 1.24 - 2020-07-22:
- Added a mouse toggle. Turn it on to access useful commands when pressing
over a grid. The grid occupied by the player has special actions.

Version 1.23 - 2020-07-12:
- Reworked the ordering of some preferences.
- More tile multipliers (they have their own preference option now).
- Sub-windows (up to 3, map views are not supported).
- Added toggle for sub-windows in the quick settings menu.
- Option to keep touch directionals fixed in place.
- Position of touch directionals are saved between games.

Version 1.22 - 2020-07-05:
- Ribbon buttons size multiplier.
- Auto display long lists of choices for commands and shops.
