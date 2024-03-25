-- tolua: define class
-- Written by Waldemar Celes
-- TeCGraf/PUC-Rio
-- Jul 1998
-- $Id: define.lua,v 1.3 2003/12/04 17:49:53 sfuerst Exp $

-- This code is free software; you can redistribute it and/or modify it.
-- The software provided hereunder is on an "as is" basis, and
-- the author has no obligation to provide maintenance, support, updates,
-- enhancements, or modifications. 


-- Define class
-- Represents a numeric const definition
-- The following filds are stored:
--   name = constant name
classDefine = {
 name = '',
 _base = classFeature,
}
settag(classDefine,tolua_tag)

-- register define
function classDefine:register ()
 local p = self:inmodule()
 if p then
  output(' tolua_constant(tolua_S,"'..p..'","'..self.lname..'",'..self.name..');') 
 else
  output(' TOLUA_DEF('..self.name..');') 
 end
end

-- unregister define
function classDefine:unregister ()
 if not self:inmodule() then
  output(' TOLUA_UNDEF('..self.lname..');') 
 end
end

-- Print method
function classDefine:print (ident,close)
 print(ident.."Define{")
 print(ident.." name = '"..self.name.."',")
 print(ident.." lname = '"..self.lname.."',")
 print(ident.."}"..close)
end


-- Internal constructor
function _Define (t)
 t._base = classDefine
 settag(t,tolua_tag)

 if t.name == '' then
  error("#invalid define")
 end

 append(t)
 return t
end

-- Constructor
-- Expects a string representing the constant name
function Define (n)
 local t = split(n,'@')
 return _Define {
  name = t[1],
  lname = t[2] or t[1]
 }
end


