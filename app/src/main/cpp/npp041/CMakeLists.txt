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
      npp041

      # Sets the library as a shared library.
      SHARED

      # Provides a relative path to your source file(s).
      angdroid_legacy.c
      ../curses/curses.c

      birth.c
      cave.c
      cmd1.c
      cmd2.c
      cmd3.c
      cmd4.c
      cmd5.c
      cmd6.c
      dump_items.c
      dungeon.c
      files.c
      generate.c
      init1.c
      init2.c
      load.c
      melee1.c
      melee2.c
      monster1.c
      monster2.c
      obj-info.c
      object1.c
      object2.c
      quest.c
      randart.c
      #readdib.c
      save.c
      spells1.c
      spells2.c
      squelch.c
      store.c
      tables.c
      use-obj.c
      util.c
      variable.c
      wizard1.c
      wizard2.c
      xtra1.c
      xtra2.c
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
      npp041

      # Links the target library to the log library
      # included in the NDK.
      ${log-lib} )
