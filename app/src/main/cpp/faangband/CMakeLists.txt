# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_AND")

include_directories(../common ../curses .)

add_library( # Sets the name of the library.
            faangband

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            ../common/angdroid.c
            ../curses/curses.c
            ../curses/droid.c

            buildid.c
            cave-map.c
            cave-square.c
            cave-view.c
            cave.c
            cmd-cave.c
            cmd-core.c
            cmd-misc.c
            cmd-obj.c
            cmd-pickup.c
            cmd-spoil.c
            cmd-wizard.c
            datafile.c
            debug.c
            effect-handler-attack.c
            effect-handler-general.c
            effects.c
            effects-info.c
            game-event.c
            game-input.c
            game-world.c
            gen-cave.c
            gen-chunk.c
            gen-monster.c
            gen-room.c
            gen-util.c
            gen-wilderness.c
            generate.c
            grafmode.c
            guid.c
            init.c
            load.c
            message.c
            mon-attack.c
            mon-blows.c
            mon-desc.c
            mon-group.c
            mon-init.c
            mon-list.c
            mon-lore.c
            mon-make.c
            mon-move.c
            mon-msg.c
            mon-predicate.c
            mon-spell.c
            mon-summon.c
            mon-timed.c
            mon-util.c
            obj-chest.c
            obj-curse.c
            obj-desc.c
            obj-design.c
            obj-gear.c
            obj-ignore.c
            obj-info.c
            obj-init.c
            obj-knowledge.c
            obj-list.c
            obj-make.c
            obj-pile.c
            obj-properties.c
            obj-slays.c
            obj-tval.c
            obj-util.c
            option.c
            parser.c
            player-attack.c
            player-birth.c
            player-calcs.c
            player-class.c
            player-history.c
            player-path.c
            player-properties.c
            player-quest.c
            player-race.c
            player-spell.c
            player-timed.c
            player-util.c
            player.c
            project-feat.c
            project-mon.c
            project-obj.c
            project-player.c
            project.c
            randname.c
            save-charoutput.c
            save.c
            savefile.c
            score.c
            sound-core.c
            source.c
            store.c
            target.c
            trap.c
            ui-birth.c
            ui-command.c
            ui-context.c
            ui-curse.c
            ui-death.c
            ui-display.c
            ui-effect.c
            ui-entry-combiner.c
            ui-entry-renderers.c
            ui-entry.c
            ui-equip-cmp.c
            ui-event.c
            ui-game.c
            ui-help.c
            ui-history.c
            ui-init.c
            ui-input.c
            ui-keymap.c
            ui-knowledge.c
            ui-map.c
            ui-menu.c
            ui-mon-list.c
            ui-mon-lore.c
            ui-obj-list.c
            ui-object.c
            ui-options.c
            ui-output.c
            ui-player.c
            ui-prefs.c
            ui-recall.c
            ui-score.c
            ui-signals.c
            ui-spell.c
            ui-spoil.c
            ui-store.c
            ui-target.c
            ui-term.c
            ui-visuals.c
            ui-wizard.c
            wiz-debug.c
            wiz-spoil.c
            wiz-stats.c
            z-bitflag.c
            z-color.c
            z-dice.c
            z-expression.c
            z-file.c
            z-form.c
            z-quark.c
            z-queue.c
            z-rand.c
            z-set.c
            z-textblock.c
            z-type.c
            z-util.c
            z-virt.c
            )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       faangband

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
