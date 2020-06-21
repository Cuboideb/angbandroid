# Angband for Android 4.2.1

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

### Custom keymaps

While using the button ribbon, the settings menu gets the item "Manage keymaps". It can be used to quickly define common keymaps shortcuts as you like.

![alt text](https://github.com/Cuboideb/angbandroid/blob/master/app/src/main/assets/keymap_editor.jpg?raw=true)

### The touch directionals

The game view can be configured to display 9 squares for moving the character in the dungeon. The action of the center square can be set to do different things.

The touch d. can be displayed to the right, all together. In that case the center square has another use. A long-press allowes you to drag and drop the whole set to another zone of the screen.

Besides the touch directionals, every touch in the term view is sent to the core as a mouse press, allowing movement and menu selection.

### Graphics modes

This port support the usual graphic tilesets of Angband (go to Preferences). You can also set pseudo-ascii mode. In that mode monsters and player are shown as ascii letters and everything else with graphic tiles.

Enjoy! 

Angband homepage [http://rephial.org](http://rephial.org)

Forum and resources [http://angband.oook.cz](http://angband.oook.cz)

Online manual [https://angband.readthedocs.io/en/latest/index.html](https://angband.readthedocs.io/en/latest/index.html)
