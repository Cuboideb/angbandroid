/*
** $Id: lgc.h,v 1.2 2002/08/29 19:06:49 rr9 Exp $
** Garbage Collector
** See Copyright Notice in lua.h
*/

#ifndef lgc_h
#define lgc_h


#include "lobject.h"


void luaC_collect (lua_State *L, int all);
void luaC_collectgarbage (lua_State *L);
void luaC_checkGC (lua_State *L);


#endif
