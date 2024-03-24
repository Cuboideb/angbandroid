# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format-security -Wno-parentheses-equality -Wno-logical-not-parentheses -Wno-deprecated-declarations")

include_directories(../common ../curses ./lua .)

add_library( # Sets the name of the library.
            zangband275

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            main-android.c
            ../curses/curses.c
            ../curses/droid.c

            ./lua/lapi.c
            ./lua/lauxlib.c
            ./lua/lbaselib.c
            ./lua/lcode.c
            ./lua/ldblib.c
            ./lua/ldebug.c
            ./lua/ldo.c
            ./lua/lfunc.c
            ./lua/lgc.c
            ./lua/liolib.c
            ./lua/llex.c
            ./lua/lmem.c
            ./lua/lobject.c
            ./lua/lparser.c
            ./lua/lstate.c
            ./lua/lstring.c
            ./lua/lstrlib.c
            ./lua/ltable.c
            ./lua/ltests.c
            ./lua/ltm.c
            ./lua/lundump.c
            ./lua/lvm.c
            ./lua/lzio.c
            ./lua/tolua.c
            ./lua/tolua_bd.c
            ./lua/tolua_eh.c
            ./lua/tolua_gp.c
            ./lua/tolua_lb.c
            ./lua/tolua_rg.c
            ./lua/tolua_tm.c
            ./lua/tolua_tt.c
            ./lua/tolualua.c

            artifact.c
            avatar.c
            birth.c
            bldg.c
            cave.c
            cmd1.c
            cmd2.c
            cmd3.c
            cmd4.c
            cmd5.c
            cmd6.c
            dungeon.c
            effects.c
            fields.c
            files.c
            flavor.c
            generate.c
            grid.c
            init1.c
            init2.c
            l-field.c
            l-misc.c
            l-monst.c
            l-object.c
            l-player.c
            l-random.c
            l-spell.c
            l-ui.c
            load.c
            maid-grf.c
            melee1.c
            melee2.c
            mind.c
            monster1.c
            monster2.c
            mspells1.c
            mspells2.c
            mutation.c
            notes.c
            obj_kind.c
            object1.c
            object2.c
            quest.c
            racial.c
            #readdib.c
            rooms.c
            run.c
            save.c
            scores.c
            script.c
            spells1.c
            spells2.c
            spells3.c
            store.c
            streams.c
            tables.c
            ui.c
            util.c
            variable.c
            wild1.c
            wild2.c
            wild3.c
            wizard1.c
            wizard2.c
            xtra1.c
            xtra2.c
            z-form.c
            z-rand.c
            z-term.c
            z-util.c
            z-virt.c
            zbmagic1.c
            zbmagic2.c
            zbmagic3.c
            zborg1.c
            zborg2.c
            zborg3.c
            zborg4.c
            zborg5.c
            zborg6.c
            zborg7.c
            zborg8.c
            zborg9.c
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
                       zangband275

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
