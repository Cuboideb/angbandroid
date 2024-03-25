# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format-security -Wno-parentheses-equality -Wno-logical-not-parentheses -Wno-deprecated-declarations -Wno-constant-conversion")

include_directories(../common ../curses ./lua .)

add_library( # Sets the name of the library.
            tome23x

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
			./lua_bind.c

			birth.c
			bldg.c
			cave.c
			cmd1.c
			cmd2.c
			cmd3.c
			cmd4.c
			cmd5.c
			cmd6.c
			cmd7.c
			cmovie.c
			dungeon.c
			files.c
			gen_evol.c
			gen_maze.c
			generate.c
			gods.c
			help.c
			init1.c
			init2.c
			irc.c
			levels.c
			#load_gif.c
			loadsave.c			
			#maid-x11.c
			#maim-iso.c			
			melee1.c
			melee2.c
			modules.c
			monster1.c
			monster2.c
			monster3.c
			notes.c
			object1.c
			object2.c
			plots.c
			powers.c
			randart.c
			readdib.c
			script.c
			skills.c
			spells1.c
			spells2.c
			squeltch.c
			status.c
			store.c
			tables.c
			traps.c
			util.c
			variable.c
			w_dun.c
			w_mnster.c
			w_obj.c
			w_play_c.c
			w_player.c
			w_quest.c
			w_spells.c
			w_util.c
			w_z_pack.c
			wild.c
			wizard1.c
			wizard2.c
			xtra1.c
			xtra2.c
			z-form.c
			z-rand.c
			z-sock.c
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
                       tome23x

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
