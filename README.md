# Angband for Android 4.2.0

An unofficial port of the roguelike Angband to Android.

This work is based on the excellent port of **Angband** found [here](https://github.com/takkaria/angband-android).

The transparent keyboard was borrowed from [here](https://github.com/Shaosil/Android-Sil) (with modifications).

Recommended game experience: Landscape Orientation and center_player option on.

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

If you feel that some action should be in the ribbon anyway, every keymap defined inside the core Angband is displayed in the button ribbon. This is also handy for spells and repetitive keystrokes.

Besides the touch directionals, every touch in the term view is sent to the core as a mouse press, allowing movement and menu selection.

Enjoy! 

Angband homepage: http://rephial.org  
Forum and resources: http://angband.oook.cz
