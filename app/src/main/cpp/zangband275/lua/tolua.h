/* tolua - Support code for Lua bindings.
** Written by Waldemar Celes (celes@tecgraf.puc-rio.br)
** TeCGraf/PUC-Rio
** http://www.tecgraf.puc-rio.br/~celes/tolua
** Jul 1998
** $Id: tolua.h,v 1.6 2003/12/21 11:05:40 sfuerst Exp $
*/

/* This code is free software; you can redistribute it and/or modify it.
** The software provided hereunder is on an "as is" basis, and
** the author has no obligation to provide maintenance, support, updates,
** enhancements, or modifications.
*/


#ifndef tolua_h
#define tolua_h

#define TOLUA_VERSION       "tolua 4.0a - angband"


#include <stdlib.h>		/* NULL, malloc, free */

#ifdef __cplusplus
extern "C" {
#endif

#include "lua.h"


/*************************************** Exported functions */

int   tolua_open (lua_State* L);
void  tolua_using (lua_State* L, int module);
void  tolua_class (lua_State* L, int derived, int base);
void  tolua_instance (lua_State* L, int instance, int classobj);
void  tolua_foreach (lua_State* L, int lo, int f);
int   tolua_tag (lua_State* L, const char* type);
const char* tolua_type (lua_State* L, int lo);
int   tolua_base (lua_State* L, int lo);
int   tolua_cast (lua_State* L, int lo, const char* type);
void  tolua_takeownership (lua_State* L, int lo);



/*************************************** Support functions for binding code */

#define LUA_VALUE int
#define LUA_NIL 0 /* TODO */
/*#define TOLUA_NIL	(lua_pushnil(),lua_pop())*/

/* Register functions */
void tolua_globalvar (lua_State* L, const char* name, lua_CFunction get, lua_CFunction set);
void tolua_globalarray (lua_State* L, const char* name, lua_CFunction get, lua_CFunction set);
void tolua_module (lua_State* L, const char* name);
void tolua_cclass (lua_State* L, const char* name, const char* base);
void tolua_function (lua_State* L, const char* parent, const char* name, lua_CFunction func);
void tolua_constant (lua_State* L, const char* parent, const char* name, long value);
void tolua_tablevar
(lua_State* L, const char* table, const char* name, lua_CFunction get, lua_CFunction set);
void tolua_tablearray
(lua_State* L, const char* table, const char* name, lua_CFunction get, lua_CFunction set);


/* Get and push functions */
long tolua_getnumber (lua_State* L, int narg, long def);
const char* tolua_getstring (lua_State* L, int narg, const char* def);
void* tolua_getuserdata (lua_State* L, int narg, void* def);
void* tolua_getusertype (lua_State* L, int narg, void* def);
int   tolua_getvalue (lua_State* L, int narg, int def);
int   tolua_getbool (lua_State* L, int narg, int def);
long tolua_getfieldnumber (lua_State* L, int lo, int index, long def);
const char* tolua_getfieldstring (lua_State* L, int lo, int index, const char* def);
void* tolua_getfielduserdata (lua_State* L, int lo, int index, void* def);
void* tolua_getfieldusertype (lua_State* L, int lo, int index, void* def);
int tolua_getfieldvalue (lua_State* L, int lo, int index, int def);
int tolua_getfieldbool (lua_State* L, int lo, int index, int def);

void tolua_pushnumber (lua_State* L, long value);
void tolua_pushstring (lua_State* L, const char* value);
void tolua_pushuserdata (lua_State* L, void* value);
void tolua_pushusertype (lua_State* L, void* value, int tag);
void tolua_pushvalue (lua_State* L, int lo);
void tolua_pushbool (lua_State* L, int value);
void tolua_pushfieldnumber (lua_State* L, int lo, int index, long v);
void tolua_pushfieldstring (lua_State* L, int lo, int index, char* v);
void tolua_pushfielduserdata (lua_State* L, int lo, int index, void* v);
void tolua_pushfieldusertype (lua_State* L, int lo, int index, void* v, int tag);
void tolua_pushfieldvalue (lua_State* L, int lo, int index, int v);
void tolua_pushfieldbool (lua_State* L, int lo, int index, int v);


/* Type & tag manipulation */
void tolua_usertype (lua_State* L, const char* type);
#if 0
void tolua_settag (lua_State* L, char* type, int* tag);
#endif
int  tolua_istype (lua_State* L, int narg, int tag, int dim);
int  tolua_arrayistype (lua_State* L, int narg, int tag, int dim, int def);

int  tolua_isnoobj (lua_State* L, int narg);

/* Tag method manipulation */
void* tolua_doclone (lua_State* L, void* value, int tag);
void* tolua_copy (lua_State* L, void* value, unsigned int size);

/* Error handling */
void tolua_error (lua_State* L, const char* msg);

/* Exported variables */
extern int tolua_tag_nil;
extern int tolua_tag_number;
extern int tolua_tag_string;
extern int tolua_tag_userdata;
extern int tolua_tag_table;
extern int tolua_tag_function;

/* Extra defines */
#define TOLUA_GET_SELF(C) \
  C* self = (C *) tolua_getusertype(tolua_S,1,0); \
  if (!self) tolua_error(tolua_S,"invalid 'self'")
#define TOLUA_ARRAY_SELF(C) \
  C* self; \
  lua_pushstring(tolua_S,".self"); \
  lua_rawget(tolua_S,1); \
  self = (C *)  lua_touserdata(tolua_S,-1)
#define TOLUA_ARRAY_INDEX(S,D) \
  if (!tolua_istype(tolua_S,2,LUA_TNUMBER,0)) \
  tolua_error(tolua_S,"invalid type in array indexing."); \
  toluaI_index = (int)tolua_getnumber(tolua_S,2,0); \
  if (toluaI_index<0 || toluaI_index>=D) \
  tolua_error(tolua_S,"array:" S " indexing out of range.");

#define TOLUA_ERR_ASSIGN tolua_error(tolua_S,"#vinvalid type in variable assignment.")
#define TOLUA_ERR_FN(F) tolua_error(tolua_S,"#ferror in function" #F); return 0;
#define TOLUA_DEF(N) tolua_constant(tolua_S,NULL, #N, N)
#define TOLUA_FUN(L,N) tolua_function(tolua_S,NULL, #L, N)
#define TOLUA_UNDEF(L) lua_pushnil(tolua_S); lua_setglobal(tolua_S,#L)

#ifdef __cplusplus
}
#endif

#endif 

