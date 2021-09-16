# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format-security -Wno-parentheses-equality -Wno-logical-not-parentheses -Wno-deprecated-declarations")

include_directories(../common ../curses .)

add_library( # Sets the name of the library.
            frogcomposband

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            main-android.c
            ../curses/curses.c
            ../curses/droid.c

            alky.c
            angband.c
            archer.c
            arky.c
            artifact.c
            autopick.c
            bard.c
            beastmaster.c
            beorning.c
            berserker.c
            birth.c
            bldg.c
            blood_knight.c
            blood_mage.c
            bluemage.c
            c-string.c
            c-vec.c
            cavalry.c
            cave.c
            chaoswar.c
            classes.c
            cmd1.c
            cmd2.c
            cmd3.c
            cmd4.c
            cmd5.c
            cmd6.c
            combat.c
            corny.c
            demigod.c
            devicemaster.c
            devices.c
            disciple.c
            do-spell.c
            doppelganger.c
            duelist.c
            dungeon.c
            effects.c
            ego.c
            equip.c
            fear.c
            files.c
            flavor.c
            floors.c
            force_trainer.c
            generate.c
            gf.c
            graymage.c
            grid.c
            hex.c
            hex_mage.c
            highmage.c
            hissatsu.c
            home.c
            igor.c
            init1.c
            init2.c
            int-map.c
            inv.c
            karrot.c
            lawyer.c
            load.c
            mage.c
            magic_eater.c
            mauler.c
            melee1.c
            melee2.c
            menu.c
            message.c
            mindcrafter.c
            mirror_master.c
            mon.c
            mon_info.c
            monk.c
            monspell.c
            monster.c
            monster1.c
            monster2.c
            mspells1.c
            mspells2.c
            mut.c
            mut_a.c
            mystic.c
            necromancer.c
            ninja.c
            ninjalaw.c
            obj.c
            obj_info.c
            obj_kind.c
            obj_prompt.c
            object1.c
            object2.c
            object3.c
            pack.c
            paladin.c
            personality.c
            polly.c
            priest.c
            psion.c
            py_birth.c
            py_info.c
            py_throw.c
            quest.c
            quiver.c
            r_angel.c
            r_centip.c
            r_demon.c
            r_dragon.c
            r_elem.c
            r_eye.c
            r_ghost.c
            r_giant.c
            r_golem.c
            r_hound.c
            r_hydra.c
            r_jelly.c
            r_leprec.c
            r_lich.c
            r_mimic.c
            r_mummy.c
            r_orc.c
            r_poss.c
            r_pump.c
            r_quyl.c
            r_rag.c
            r_ring.c
            r_spider.c
            r_sword.c
            r_troll.c
            r_vamp.c
            r_vortex.c
            r_xorn.c
            r_zombie.c
            races.c
            races_a.c
            races_k.c
            races_mf.c
            racial.c
            ragemage.c
            randname.c
            ranger.c
            #readdib.c
            rect.c
            red_mage.c
            resist.c
            rogue.c
            rooms.c
            rune_kt.c
            samurai.c
            save.c
            savefile.c
            scores.c
            scout.c
            shop.c
            skillmaster.c
            skills.c
            sniper.c
            sorcerer.c
            spells.c
            spells1.c
            spells2.c
            spells3.c
            spells_a.c
            spells_c.c
            spells_h.c
            spells_m.c
            spells_s.c
            spoilers.c
            str-map.c
            streams.c
            tables.c
            timelord.c
            tourist.c
            troika.c
            util.c
            variable.c
            variant.c
            virtue.c
            warlock.c
            warrior.c
            warrior_mage.c
            weaponmaster.c
            weaponsmith.c
            werewolf.c
            wild.c
            wild_realm.c
            wild_talent.c
            wiz_obj.c
            wizard1.c
            wizard2.c
            xtra1.c
            xtra2.c
            yellow_mage.c
            yeqrezh.c
            #z-doc-test.c
            z-doc.c
            z-form.c
            z-rand.c
            z-term.c
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
                       frogcomposband

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
