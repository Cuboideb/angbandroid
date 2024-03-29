------------------------------------------------------------------------------
----------------------- Hook to create birth objects -------------------------
------------------------------------------------------------------------------


function __birth_hook_objects()

	-- Grace delay for adding piety
	GRACE_DELAY = 0

	-- Provide a book of Geyser to Geomancers
	if get_class_name() == "Geomancer" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Geyser")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Provide a book of prayer to priests
	if get_class_name() == "Priest(Eru)" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("See the Music")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Priest(Manwe)" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Manwe's Blessing")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Druid" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Charm Animal")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Dark-Priest" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Curse")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Paladin" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Divine Aim")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Stonewright" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Firebrand")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Priest(Varda)" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Light of Valinor")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Priest(Ulmo)" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Song of Belegaer")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end
	if get_class_name() == "Priest(Mandos)" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Tears of Luthien")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	if get_class_name() == "Mimic" then
		local obj = create_object(TV_CLOAK, 100);
		obj.pval2 = resolve_mimic_name("Mouse")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the undeads, as undeads with the corruptions
	if get_subrace_name() == "Vampire" then
		player.corruption(CORRUPT_VAMPIRE_TEETH, TRUE)
		player.corruption(CORRUPT_VAMPIRE_STRENGTH, TRUE)
		player.corruption(CORRUPT_VAMPIRE_VAMPIRE, TRUE)
	end

	-- Start the Red (Fire) dragons with a book of Light (Theme)
	if get_subrace_name() == "Red" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Globe of Light")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the Black (Water) dragons with a book of Geyser (Theme)
	if get_subrace_name() == "Black" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Geyser")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the Green (Air) dragons with a book of Noxious Cloud (Theme)
	if get_subrace_name() == "Green" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Noxious Cloud")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the Blue (Earth) dragons with a book of Stone Skin (Theme)
	if get_subrace_name() == "Blue" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Stone Skin")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the White dragons with a book of Sense Monsters (Theme)
	if get_subrace_name() == "White" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Sense Monsters")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the Ethereal dragons with a book of Recharge (Theme)
	if get_subrace_name() == "Ethereal" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Recharge")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Start the Peace-mages with a book of Blink (Theme)
	if get_class_name() == "Peace-mage" then
		local obj = create_object(TV_BOOK, 255);
		obj.pval = find_spell("Phase Door")
		obj.ident = bor(obj.ident, IDENT_MENTAL, IDENT_KNOWN)
		inven_carry(obj, FALSE)
		end_object(obj)
	end

	-- Provide everyone with a scroll of WoR (Theme)
	if get_race_name() == "Human" then
	local obj = create_object(TV_TOOL, SV_PORTABLE_HOLE);
		inven_carry(obj, FALSE)
		end_object(obj)
		identify_pack_fully()
       end
       
       -- Provide everyone with a scroll of WoR (Theme)

        if get_subrace_name() == "Aragorn" then
        local artifact = create_artifact(164);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
        if get_subrace_name() == "Aragorn" then
        local artifact = create_artifact(8);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
        if get_subrace_name() == "Pippin" then
        local artifact = create_artifact(184);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
      
          if get_subrace_name() == "Pippin" then
        local artifact = create_artifact(67);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
         
         if get_subrace_name() == "Pippin" then
        local artifact = create_artifact(165);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
         
               
         if get_subrace_name() == "Frodo" then
        local artifact = create_artifact(88);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
        if get_subrace_name() == "Frodo" then
        local artifact = create_artifact(207);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
        if get_subrace_name() == "Frodo" then
        local artifact = create_artifact(1);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
      if get_subrace_name() == "Frodo" then
        local artifact = create_artifact(13);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end  

  
            if get_subrace_name() == "Gimli" then
        local artifact = create_artifact(180);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
         if get_subrace_name() == "Gimli" then
        local artifact = create_artifact(174);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
         if get_subrace_name() == "Gimli" then
        local artifact = create_artifact(105);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
         if get_subrace_name() == "Legolas" then
        local artifact = create_artifact(239);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
        if get_subrace_name() == "Legolas" then
        local artifact = create_artifact(214);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
           if get_subrace_name() == "Legolas" then
        local artifact = create_artifact(70);
        inven_carry(artifact, FALSE) 
        identify_pack_fully()
        end
        
    --    add_hooks
	--add_hook_script(HOOK_BIRTH_OBJECTS, "__birth_hook_objects", "__birth_hook_objects")
end

add_hooks
{
 [HOOK_BIRTH_OBJECTS] = function()
    if player.last_rewarded_level >= 1
       then player.last_rewarded_level = 1
      else
      end
   end
}
add_hooks
{

[HOOK_BIRTH_OBJECTS] = function()
if get_class_name() == "Hunter" then
	player.wilderness_y = 3;
	player.wilderness_x = 3;
	player.py = 3
	player.px = 3
	end
	end
}


-- Register in the hook list
    --    add_hooks
	add_hook_script(HOOK_BIRTH_OBJECTS, "__birth_hook_objects", "__birth_hook_objects")
