/* File: h-system.h */

#ifndef INCLUDED_H_SYSTEM_H
#define INCLUDED_H_SYSTEM_H

/*
 * Include the basic "system" files.
 *
 * Make sure all "system" constants/macros are defined.
 * Make sure all "system" functions have "extern" declarations.
 *
 * This file is a big hack to make other files less of a hack.
 * This file has been rebuilt -- it may need a little more work.
 */


#include <stdio.h>
#include <ctype.h>
#include <errno.h>

#if defined(NeXT)
# include <libc.h>
#else
# include <stdlib.h>
#endif


#ifdef SET_UID

# include <sys/types.h>

# if defined(Pyramid) || defined(NeXT) || defined(SUNOS) || \
     defined(NCR3K) || defined(SUNOS) || defined(ibm032) || \
     defined(__osf__) || defined(ISC) || defined(SGI) || \
     defined(linux)
#  include <sys/time.h>
# endif

# if !defined(SGI) && !defined(ULTRIX) && !defined(ANDROID)
#  include <sys/timeb.h>
# endif

#endif


#include <time.h>



#ifdef MACINTOSH
# include <unix.h>
#endif

#if defined(WINDOWS) || defined(MSDOS) || defined(USE_EMX)
# include <io.h>
#endif

#if !defined(MACINTOSH) && \
    !defined(__MWERKS__)
# if defined(__TURBOC__) || defined(__WATCOMC__)
#  include <mem.h>
# else
#  include <memory.h>
# endif
#endif


#if !defined(NeXT) && !defined(__MWERKS__)
# include <fcntl.h>
#endif


#ifdef SET_UID

# ifndef USG
#  include <sys/param.h>
#  include <sys/file.h>
# endif

# ifdef linux
#  include <sys/file.h>
# endif

# include <pwd.h>

# include <unistd.h>

# include <sys/stat.h>

# if defined(SOLARIS)
#  include <netdb.h>
# endif

#endif

#ifdef __DJGPP__
#include <unistd.h>
#endif /* __DJGPP__ */

#if defined(SET_UID) && !defined(ANDROID)

#ifdef USG
# include <string.h>
#else
# include <strings.h>
# ifndef strstr
extern char *strstr();
# endif
# ifndef strchr
extern char *strchr();
# endif
# ifndef strrchr
extern char *strrchr();
# endif
#endif

#else

# include <string.h>

#endif



#if !defined(linux) && !defined(__MWERKS__)
extern long atol();
#endif


#include <stdarg.h>


#endif

/* There was a bug introduced in 10.4.11; working around it */
#ifdef __APPLE__
#define GETLOGIN_BROKEN
#endif
