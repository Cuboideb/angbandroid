/* File: h-type.h */

#ifndef INCLUDED_H_TYPE_H
#define INCLUDED_H_TYPE_H

/*
 * Basic "types".
 *
 * Note the attempt to make all basic types have 4 letters.
 * This improves readibility and standardizes the code.
 *
 * Likewise, all complex types are at least 4 letters.
 * Thus, almost every 1 to 3 letter word is a legal variable,
 * except for certain reserved words ('for' and 'if' and 'do').
 *
 * Note that the type used in structures for bit flags should be uint.
 * As long as these bit flags are sequential, they will be space smart.
 *
 * Note that on some machines, apparently "signed char" is illegal.
 *
 * A char/byte takes exactly 1 byte
 * A s16b/u16b takes exactly 2 bytes
 * A s32b/u32b takes exactly 4 bytes
 *
 * A sint/uint takes at least 2 bytes
 * A long/huge takes at least 4 bytes
 *
 * A real normally takes from 4 to 10 bytes
 * A vptr normally takes 4 (rarely 8) bytes
 *
 * Note that some files have already been included by "h-include.h"
 * These include <stdio.h> and <sys/types>, which define some types
 * In particular, "bool", "byte", "uint", and "huge" may be defined
 * already, possibly using "typedefs" of various kinds, and possibly
 * being defined to something other than required by my code.  So we
 * simply redefine them all using a stupid "_hack" suffix.
 *
 * Also, see <limits.h> for min/max values for sint, uint, long, huge
 * (INT_MIN, INT_MAX, 0, UINT_MAX, LONG_MIN, LONG_MAX, 0, ULONG_MAX).
 * These limits should be verified and coded into "h-constant.h", or
 * perhaps not, since those types have "unknown" length by definition.
 */



/*** Special 4 letter names for some standard types ***/


/* A generic pointer */
typedef void *vptr;


/*
 * Hack -- prevent problems with non-MACINTOSH
 */
#undef uint
#define uint uint_hack

/*
 * Hack -- prevent problems with MSDOS and WINDOWS
 */
#undef huge
#define huge huge_hack

/*
 * Hack -- prevent problems with AMIGA
 */
#undef byte
#define byte byte_hack

/*
 * Hack -- prevent problems with C++
 */
#undef bool
#define bool bool_hack


/* Note that "signed char" is not always "defined" */
/* So always use "s16b" to hold small signed values */
/* A signed byte of memory */
/* typedef signed char syte; */

/* Note that unsigned values can cause math problems */
/* An unsigned byte of memory */
typedef unsigned char byte;

/* Note that a bool is smaller than a full "int" */
/* Simple True/False type */
typedef char bool;


/* A signed, standard integer (at least 2 bytes) */
typedef int sint;

/* An unsigned, "standard" integer (often pre-defined) */
typedef unsigned int uint;

/* The largest possible unsigned integer */
typedef unsigned long huge;


/* Signed/Unsigned 16 bit value */
typedef signed short s16b;
typedef unsigned short u16b;

/* Signed/Unsigned 32 bit value */
#ifdef L64						/* 64 bit longs */
typedef signed int s32b;
typedef unsigned int u32b;

#ifdef USE_64B
/* Signed/Unsigned 64bit value */
typedef long u64b;
typedef unsigned long s64b;
#endif /* USE_64B */

#else  /* L64 */

typedef signed long s32b;
typedef unsigned long u32b;

#ifdef USE_64B

/* Try to get a 64 bit type */
# if defined __STDC_VERSION__ && __STDC_VERSION__ >= 199901L
#  include <stdint.h>
#  define ANG_U64B uint64_t
#  define ANG_S64B int64_t
# endif	/* __STDC__ && __STDC_VERSION__ */

/* Define this for Microsoft Dev Studio C++ 6.0 */
# ifdef MSDEV
#  define ANG_U64B unsigned __int64
#  define ANG_S64B __int64
# endif	/* MSDEV */

/* Define this if you have <sys/types.h> with an old compiler */
# if defined HAS_SYS_TYPES && !defined ANG_U64B
#  include <sys/types.h>
#  define ANG_U64B u_int64_t
#  define ANG_S64B int64_t
# endif	/* HAS_SYS_TYPES */

/* Attempt to use "long long" which is semi-standard for older compilers */
# ifndef ANG_U64B
#  define ANG_U64B unsigned long long
#  define ANG_S64B long long
# endif	/* ANG_U64B */

/* Define the 64bit types */
typedef ANG_U64B u64b;
typedef ANG_S64B s64b;

#endif /* USE_64B */

#endif /* L64 */

#endif
