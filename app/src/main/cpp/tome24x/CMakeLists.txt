cmake_minimum_required(VERSION 3.4.1)

#
# C++ standard
#
SET(CMAKE_CXX_STANDARD 17)

SET(COMMON_COMPILER_FLAGS "-pipe -Wall -Wextra -Wno-unused-value -Wno-unused-parameter")

SET(SANITIZER_FLAGS "-fsanitize=undefined -fsanitize=address")

#
# C Compiler Flags
#
SET(CMAKE_C_FLAGS           "${CMAKE_C_FLAGS} ${COMMON_COMPILER_FLAGS} -Wno-unused-variable")
#SET(CMAKE_C_FLAGS_RELEASE   "-O2")
#SET(CMAKE_C_FLAGS_DEBUG     "-O0 -g ${SANITIZER_FLAGS}")

#
# C++ Compiler Flags
#
SET(CMAKE_CXX_FLAGS         "${CMAKE_CXX_FLAGS} ${COMMON_COMPILER_FLAGS}")
SET(CMAKE_CXX_FLAGS         "${CMAKE_CXX_FLAGS} -DJSONCONS_NO_DEPRECATED")
#SET(CMAKE_CXX_FLAGS_RELEASE "-O2")
#SET(CMAKE_CXX_FLAGS_DEBUG   "-O0 -g ${SANITIZER_FLAGS} -D_GLIBCXX_DEBUG -D_GLIBCXX_DEBUG_PEDANTIC")

#
# fmt
#
ADD_DEFINITIONS(-DFMT_HEADER_ONLY)

# Add standard math library
SET(LIBS ${LIBS} m)

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/include)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/../common)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/../curses)
INCLUDE_DIRECTORIES(${MY_BOOST_INC_DIR})
#INCLUDE_DIRECTORIES(SYSTEM ${CMAKE_CURRENT_SOURCE_DIR}/vendor/bandit)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/vendor/fmt-4.1.0)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/vendor/pcg-cpp-0.98/include)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/vendor/jsoncons-0.104.0)
#INCLUDE_DIRECTORIES(SYSTEM ${CMAKE_CURRENT_SOURCE_DIR}/vendor/CppQuickCheck/include)

# Add subdirectories
ADD_SUBDIRECTORY (squelch)

# Sources (common)
SET(SRCS_COMMON

  # Provides a relative path to your source file(s).
  main-android.cc
  ../curses/curses.c

  birth.cc
  bldg.cc
  cave.cc
  cmd1.cc
  cmd2.cc
  cmd3.cc
  cmd4.cc
  cmd5.cc
  cmd6.cc
  cmd7.cc
  corrupt.cc
  device_allocation.cc
  dice.cc
  dungeon.cc
  files.cc
  frontend.cc
  format_ext.cc
  game.cc
  game_edit_data.cc
  gen_evol.cc
  gen_maze.cc
  generate.cc
  gods.cc
  help.cc
  hiscore.cc
  hooks.cc
  init1.cc
  init2.cc
  joke.cc
  key_queue.cc
  level_data.cc
  level_marker.cc
  levels.cc
  loadsave.cc
  lua_bind.cc
  melee1.cc
  melee2.cc
  message.cc
  messages.cc
  mimic.cc
  modules.cc
  monster_spell.cc
  monster_type.cc
  monster1.cc
  monster2.cc
  monster3.cc
  notes.cc
  object1.cc
  object2.cc
  object_filter.cc
  object_flag_meta.cc
  options.cc
  player_type.cc
  powers.cc
  program_args.cc
  q_betwen.cc
  q_bounty.cc
  q_dragons.cc
  q_eol.cc
  q_evil.cc
  q_fireprof.cc
  q_god.cc  
  q_haunted.cc
  q_hobbit.cc
  q_invas.cc
  q_library.cc
  q_main.cc
  q_narsil.cc
  q_nazgul.cc
  q_nirna.cc
  q_one.cc
  q_poison.cc
  q_rand.cc
  q_shroom.cc
  q_spider.cc
  q_thief.cc
  q_thrain.cc
  q_troll.cc
  q_ultrae.cc
  q_ultrag.cc
  q_wight.cc
  q_wolves.cc
  quest.cc
  randart.cc
  range.cc
  skills.cc
  spell_type.cc
  spells1.cc
  spells2.cc
  spells3.cc
  spells4.cc
  spells5.cc
  spells6.cc
  squeltch.cc
  store.cc
  tables.cc
  util.cc
  variable.cc
  wild.cc
  wizard2.cc
  seed.cc
  xtra1.cc
  xtra2.cc
  z-form.cc
  z-rand.cc
  z-term.cc
  z-util.cc
)

ADD_LIBRARY(
    tome24x
    
    SHARED
  
    ${SRCS_COMMON}
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
    log)

set(BOOST_LIBS ${CMAKE_CURRENT_SOURCE_DIR}/../../cmakelibs/boost/${ANDROID_ABI})
    
add_library (boost_system SHARED IMPORTED)

set_target_properties(boost_system PROPERTIES IMPORTED_LOCATION
    ${BOOST_LIBS}/libboost_system.so)

add_library (boost_filesystem SHARED IMPORTED)

set_target_properties(boost_filesystem PROPERTIES IMPORTED_LOCATION
    ${BOOST_LIBS}/libboost_filesystem.so)

TARGET_LINK_LIBRARIES(tome24x squelch ${log-lib} ${LIBS} boost_system boost_filesystem)
