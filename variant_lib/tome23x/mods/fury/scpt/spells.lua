--
-- This file takes care of the schools of magic
--
BOOK_FURYAIR = 67
BOOK_NINJA = 99
SKILL_CONST = 88
SKILL_HUNTER = 96
SKILL_JEDI = 89
SKILL_DARK = 90
SKILL_LIGHT = 91
SKILL_NINJA = 92
SKILL_PHASING = 93
SKILL_MINDBLADE = 94


-- Create the schools
SCHOOL_MANA = add_school
{
	["name"] = "Energy", 
	["skill"] = SKILL_MANA,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Varda provides the Mana school at 1/4 the prayer skill
		[GOD_VARDA] = 
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 4,
		},
		-- Eru Iluvatar provides the Mana school at half the prayer skill
		[GOD_ERU] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		}
	},
	["hooks"] =
	{
		[HOOK_CALC_MANA] = function(msp)
			if get_skill(SKILL_MANA) >= 35 then
				msp = msp + (msp * ((get_skill(SKILL_MANA) - 34)) / 100)
				return TRUE, msp
			end
		end
	},
}
SCHOOL_FIRE = add_school
{
	["name"] = "Fire", 
	["skill"] = SKILL_FIRE,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["hooks"] =
	{
		[HOOK_CALC_BONUS] = function()
			if get_skill(SKILL_FIRE) >= 35 then
			end
		end,
		[HOOK_CALC_POWERS] = function()
			if get_skill(SKILL_FIRE) >= 50 then
--				player.add_power(PWR_FIRE_SHAPE)
			end
		end,
	},
	["gods"] = 
	{
		-- Aule provides the Fire school at 3/5 the prayer skill
		[GOD_AULE] = 
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 3,
			["div"] = 5,
		},
	},
}
SCHOOL_AIR = add_school
{
	["name"] = "Air", 
	["skill"] = SKILL_AIR,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["hooks"] =
	{
		[HOOK_CALC_BONUS] = function()
			if get_skill(SKILL_AIR) >= 50 then
				player.magical_breath = TRUE
			end
		end,
		[HOOK_CALC_POWERS] = function()
			if get_skill(SKILL_AIR) >= 50 then
--				player.add_powe(PWR_AIR_SHAPE)
			end
		end,
	},
	["gods"] =
	{
		-- Manwe Sulimo provides the Air school at 2/3 the prayer skill
		[GOD_MANWE] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 2,
			["div"] = 3,
		},
	},
}
SCHOOL_WATER = add_school
{
	["name"] = "Water", 
	["skill"] = SKILL_WATER,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["hooks"] =
	{
		[HOOK_CALC_BONUS] = function()
			if get_skill(SKILL_WATER) >= 30 then
				player.water_breath = TRUE
			end
		end,
		[HOOK_CALC_POWERS] = function()
			if get_skill(SKILL_WATER) >= 50 then
--				player.add_powe(PWR_WATER_SHAPE)
			end
		end,
	},
	["gods"] =
	{
		-- Yavanna Kementari provides the Water school at 1/2 the prayer skill
		[GOD_YAVANNA] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		},
		-- Ulmo provides the Water school at 3/5 the prayer skill
		[GOD_ULMO] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 3,
			["div"] = 5,
		},
	},
}
SCHOOL_EARTH = add_school
{
	["name"] = "Earth", 
	["skill"] = SKILL_EARTH,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["hooks"] =
	{
		[HOOK_CALC_POWERS] = function()
			if get_skill(SKILL_EARTH) >= 50 then
--				player.add_powe(PWR_EARTH_SHAPE)
			end
		end,
	},
	["gods"] =
	{
		-- Tulkas provides the Earth school at 4/5 the prayer skill
		[GOD_TULKAS] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 4,
			["div"] = 5,
		},
		-- Yavanna Kementari provides the Earth school at 1/2 the prayer skill
		[GOD_YAVANNA] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		},

		-- Aule provides the Earth school at 1/3 the prayer skill
		[GOD_AULE] = 
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 3,
		},
	},
}
SCHOOL_CONVEYANCE = add_school
{
	["name"] = "Movement", 
	["skill"] = SKILL_CONVEYANCE,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Manwe Sulimo provides the Conveyance school at 1/2 the prayer skill
		[GOD_MANWE] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		},
	},
}
SCHOOL_GEOMANCY = add_school
{
	["name"] = "Geomancy",
	["skill"] = SKILL_GEOMANCY,
	["spell_power"] = TRUE,
	-- Require to wield a Mage Staff, as the spells requries the caster to stomp the floor with it
	["depend"] = function()
		-- Require at least one point in each school
		if get_skill(SKILL_FIRE) == 0 then return end
		if get_skill(SKILL_AIR) == 0 then return end
		if get_skill(SKILL_EARTH) == 0 then return end
		if get_skill(SKILL_WATER) == 0 then return end

		local obj = get_object(INVEN_WIELD)
		if (obj.k_idx > 0) and (obj.tval == TV_MSTAFF) then return TRUE end
	end,
}
SCHOOL_DIVINATION = add_school
{
	["name"] = "Knowledge", 
	["skill"] = SKILL_DIVINATION,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Eru Iluvatar provides the Divination school at 2/3 the prayer skill
		[GOD_ERU] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 2,
			["div"] = 3,
		},
		-- Mandos the Divination school at 1/3 the prayer skill
		[GOD_MANDOS] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 3,
		},
	},
}
SCHOOL_TEMPORAL = add_school
{
	["name"] = "Temporal", 
	["skill"] = SKILL_TEMPORAL,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Yavanna Kementari provides the Temporal school at 1/6 the prayer skill
		[GOD_YAVANNA] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 6,
		},
		-- Mandos provides the Temporal school at 1/4 the prayer skill
		[GOD_MANDOS] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 4,
		},
	},
}
SCHOOL_NATURE = add_school
{
	["name"] = "Nature", 
	["skill"] = SKILL_NATURE,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Yavanna Kementari provides the Nature school at 1/2 the prayer skill
		[GOD_YAVANNA] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		},
		-- Ulmo provides the Nature school at 1/2 the prayer skill
		[GOD_ULMO] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		},
	},
}
SCHOOL_META = add_school
{
	["name"] = "Meta", 
	["skill"] = SKILL_META,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Manwe Sulimo provides the Meta school at 1/3 the prayer skill
		[GOD_MANWE] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 3,
		},
		-- Varda provides the Meta school at 1/2 the prayer skill
		[GOD_VARDA] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 2,
		}
	},
}
SCHOOL_MIND = add_school
{
	["name"] = "Mind",
	["skill"] = SKILL_MIND,
	["spell_power"] = TRUE,
	["sorcery"] = TRUE,
	["gods"] =
	{
		-- Eru Iluvatar provides the Mind school at 1/3 the prayer skill
		[GOD_ERU] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 3,
		},
		-- Melkor Bauglir provides the Mind school at 1/3 the prayer skill
		[GOD_MELKOR] =
		{
			["skill"] = SKILL_PRAY,
			["mul"] = 1,
			["div"] = 3,
		},
	},
}
SCHOOL_UDUN = add_school
{
	["name"] = 		"Udun",
	["skill"] = 		SKILL_UDUN,
        ["sorcery"] = TRUE,
	["bonus_level"] = 	function()
					return ((player.lev * 2) / 3)
				end,
}
SCHOOL_DEMON = add_school
{
	["name"] = "Maia-lore",
	["skill"] = SKILL_DAEMON,
	["no_random"] = TRUE,
        ["sorcery"] = TRUE,
}

SCHOOL_JEDI = add_school
{
	["name"] = "Jedi", 
	["skill"] = SKILL_JEDI,
	["spell_power"] = TRUE,

	
}

SCHOOL_DARK = add_school
{
	["name"] = "Dark", 
	["skill"] = SKILL_DARK,
	["spell_power"] = TRUE,
	
	
}

SCHOOL_LIGHT = add_school
{
	["name"] = "Light", 
	["skill"] = SKILL_LIGHT,
	["spell_power"] = TRUE,
	
	
}

SCHOOL_NINJA = add_school
{
	["name"] = "Ninja", 
	["skill"] = SKILL_NINJA,
	["spell_power"] = FALSE,
	
	
}

SCHOOL_PHASING = add_school
{
	["name"] = "Phasing", 
	["skill"] = SKILL_PHASING,
	["spell_power"] = FALSE,

	
}

SCHOOL_MINDBLADE = add_school
{
	["name"] = "Mindblades", 
	["skill"] = SKILL_MINDBLADE,
	["spell_power"] = FALSE,

	
}

SCHOOL_CONST = add_school
{
	["name"] = "Construction", 
	["skill"] = SKILL_CONST,
	["spell_power"] = FALSE,

	
}
SCHOOL_HUNTING = add_school
{
	["name"] = "Hunter", 
	["skill"] = SKILL_HUNTER,
	["spell_power"] = FALSE,

	
}

-- The God specific schools, all tied to the prayer skill
SCHOOL_ERU = add_school
{
	["name"] = "Eru Iluvatar",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_ERU,
}
SCHOOL_MANWE = add_school
{
	["name"] = "Manwe Sulimo",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_MANWE,
}
SCHOOL_TULKAS = add_school
{
	["name"] = "Tulkas",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_TULKAS,
}
SCHOOL_MELKOR = add_school
{
	["name"] = "Melkor Bauglir",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_MELKOR,
}
SCHOOL_YAVANNA = add_school
{
	["name"] = "Yavanna Kementari",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_YAVANNA,
}

-- New schools
SCHOOL_AULE = add_school
{
	["name"] = "Aule the Smith",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_AULE,
}
SCHOOL_VARDA = add_school
{
	["name"] = "Varda Elentari",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_VARDA,
}

SCHOOL_ULMO = add_school
{
	["name"] = "Ulmo",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_ULMO,
}

SCHOOL_MANDOS = add_school
{
	["name"] = "Mandos",
	["skill"] = SKILL_PRAY,
	["spell_power"] = TRUE,
	["god"] = GOD_MANDOS,
}

-- Not a real school, rather a palcehodler for stick only spells
SCHOOL_DEVICE = add_school
{
	["name"] = "Device",
	["skill"] = SKILL_DEVICE,
}

-- Music "spells"
SCHOOL_MUSIC = add_school
{
	["name"] = "Music",
	["skill"] = SKILL_MUSIC,
	
}

-- Put some spells
tome_dofile("s_fire.lua")
tome_dofile("s_mana.lua")
tome_dofile("s_water.lua")
tome_dofile("s_air.lua")
tome_dofile("s_earth.lua")
tome_dofile("s_convey.lua")
tome_dofile("s_nature.lua")
tome_dofile("s_divin.lua")
tome_dofile("s_tempo.lua")
tome_dofile("s_meta.lua")
tome_dofile("s_mind.lua")
tome_dofile("s_udun.lua")
tome_dofile("s_geom.lua")
tome_dofile("s_jedi.lua")
tome_dofile("s_ninja.lua")
tome_dofile("s_phase.lua")
tome_dofile("s_const.lua")
tome_dofile("s_mblade.lua")

-- Gods' specific spells
tome_dofile("s_eru.lua")
tome_dofile("s_manwe.lua")
tome_dofile("s_tulkas.lua")
tome_dofile("s_melkor.lua")
tome_dofile("s_yavann.lua")

-- New gods' spells
tome_dofile("s_aule.lua")
tome_dofile("s_varda.lua")
tome_dofile("s_ulmo.lua")
tome_dofile("s_mandos.lua")
tome_dofile("s_hunter.lua")
-- Specific schools
tome_dofile("s_demon.lua")

-- Device spells
tome_dofile("s_stick.lua")

-- Musics
tome_dofile("s_music.lua")

-- List of spellbooks

-- Create the crystal of mana
school_book[68] = {
	MANATHRUST, DELCURSES, RESISTS, MANASHIELD, MANASTORM
}

-- The book of the eternal flame
school_book[1] = {
	GLOBELIGHT, GLOBELIGHT2, FIREGOLEM, FIREFLASH, FIREWALL, FIERYAURA,
}

-- The book of the blowing winds
school_book[2] = {
	NOXIOUSCLOUD, ELECBALL, POISONBLOOD, INVISIBILITY, STERILIZE, AIRWINGS, THUNDERSTORM,
}

-- The book of the impenetrable earth
school_book[3] = {
	STONESKIN, DIG, STONEPRISON, SHAKE, STRIKE, SHARDSTORM, REBUILD
}

-- The book of the unstopable wave
school_book[4] = {
	GEYSER, VAPOR, ICEBALL, ENTPOTION, TIDALWAVE, ICESTORM
}

-- Create the book of translocation
school_book[5] = {
	DISARM, BLINK, TELEPORT, TELEAWAY, RECALL, PROBABILITY_TRAVEL,
}

-- Create the book of the tree
school_book[6] = {
	GROWTREE, HEALING, RECOVERY, REGENERATION, SUMMONANNIMAL, GROW_ATHELAS,
}

-- Create the book of Knowledge
school_book[7] = {
	SENSEMONSTERS, SENSEHIDDEN, REVEALWAYS, IDENTIFY, VISION, STARIDENTIFY,
}

-- Create the book of the Time
school_book[8] = {
	TIMEBEAM, MAGELOCK, SLOWMONSTER, TIMEBALL, ESSENCESPEED, BANISHMENT,
}

-- Create the book of meta spells
school_book[9] = {
	RECHARGE, DISPERSEMAGIC, SPELLBINDER, TRACKER, INERTIA_CONTROL,
}

-- Create the book of the mind
school_book[10] = {
	CHARM, MINDBLAST2, CONFUSE, ARMOROFFEAR, STUN,
}

-- Create the book of hellflame
school_book[11] = {
	DRAIN, GENOCIDE, WRAITHFORM, FLAMEOFUDUN,
}

-- Create the book of eru
school_book[20] = {
	ERU_SEE, ERU_LISTEN, ERU_UNDERSTAND, ERU_PROT,
}

-- Create the book of manwe
school_book[21] = {
	MANWE_BLESS, MANWE_SHIELD, MANWE_CALL, MANWE_AVATAR,
}

-- Create the book of tulkas
school_book[22] = {
	TULKAS_AIM, TULKAS_SPIN, TULKAS_WAVE,
}

-- Create the book of melkor
school_book[23] = {
	MELKOR_CURSE, MELKOR_CORPSE_EXPLOSION, MELKOR_MIND_STEAL,
}

-- Create the book of yavanna
school_book[24] = {
	YAVANNA_CHARM_ANIMAL, YAVANNA_GROW_GRASS, YAVANNA_TREE_ROOTS, YAVANNA_WATER_BITE, YAVANNA_UPROOT,
}

-- Create the book of beginner's cantrip
school_book[50] = {
	MANATHRUST, GLOBELIGHT, ENTPOTION, BLINK, SENSEMONSTERS, SENSEHIDDEN,
}

-- Create the book of teleporatation
school_book[51] = {
	BLINK, TELEPORT, TELEAWAY
}

-- Create the book of summoning
school_book[52] = {
	FIREGOLEM, SUMMONANNIMAL
}


-- Create the Armageddon Demonblade
school_book[55] = {
	DEMON_BLADE, DEMON_MADNESS, DEMON_FIELD, 
}

-- Create the Shield Demonblade
school_book[56] = {
	DOOM_SHIELD, DEMON_CLOAK, UNHOLY_WORD,
}

-- Create the Control Demonblade
school_book[57] = {
	DEMON_SUMMON, DISCHARGE_MINION, CONTROL_DEMON,
}

-- Create the Drums
school_book[58] = {
	MUSIC_STOP, MUSIC_HOLD, MUSIC_CONF, MUSIC_STUN, 
}

-- Create the Harps
school_book[59] = {
	MUSIC_STOP, MUSIC_LITE, MUSIC_HERO, MUSIC_HEAL, MUSIC_TIME, MUSIC_MIND,
}

-- Create the Horns
school_book[60] = {
	MUSIC_STOP, MUSIC_BLOW, MUSIC_WIND, MUSIC_YLMIR, MUSIC_AMBARKANTA,
}

-- Book of the Player, filled in by the Library Quest
school_book[61] = { }

-- Geomancy spells, not a real book
school_book[62] = {
	CALL_THE_ELEMENTS, CHANNEL_ELEMENTS, ELEMENTAL_WAVE, VAPORIZE, GEOLYSIS, DRIPPING_TREAD, GROW_BARRIER, ELEMENTAL_MINION
}

-- Aule book [63]
school_book[BOOK_AULE] =
{ 
	AULE_FIREBRAND, AULE_ENCHANT_WEAPON, AULE_ENCHANT_ARMOUR, AULE_CHILD,
} 

-- Varda book [64]
school_book[BOOK_VARDA] =
{ 
	VARDA_LIGHT_VALINOR, VARDA_CALL_ALMAREN, VARDA_EVENSTAR, VARDA_STARKINDLER,
} 

-- Ulmo book [65]
school_book[BOOK_ULMO] =
{ 	
	ULMO_BELEGAER, ULMO_DRAUGHT_ULMONAN, ULMO_CALL_ULUMURI, ULMO_WRATH,
} 

-- Mandos book [66]
school_book[BOOK_MANDOS] =
{ 
	MANDOS_TEARS_LUTHIEN, MANDOS_SPIRIT_FEANTURI, MANDOS_TALE_DOOM, MANDOS_CALL_HALLS
}
-- Air Book 2K

school_book[67] = {
	MANATHRUST, GLOBELIGHT, FIREFLASH, FIREWALL, NOXIOUSCLOUD, STONESKIN, DIG, GEYSER, VAPOR, ENTPOTION, TIDALWAVE, ICESTORM, DISARM, BLINK, TELEPORT, SENSEMONSTERS, SENSEHIDDEN, REVEALWAYS, IDENTIFY, VISION, STARIDENTIFY, GROWTREE, HEALING, RECOVERY, SUMMONANNIMAL, CHARM, 
}

school_book[79] =
{ 
	HUNTER_FLAMEBEAM, HSENSEMONSTERS, HSENSEHIDDEN, HUNTER_POISDART, HUNTER_FLAMETHROW, HUNTER_ROCKET, HUNTER_MISSILE, HUNTER_NUKE
}


school_book[89] =
{ 
	JSENSEMONSTERS, JSENSEHIDDEN, JVISION, FORCEPUSH, FORCEPULL, JDISARM, ELECTRICBOLT, FORCESTORM,  FORCECRUSH, FORCEWAVE,  FORCEGRIP, FORCEKILL, FORCEMIND, JHEALING, JRECOVERY,  JSTARIDENTIFY, FESSENCESPEED, JREBUILD
}

school_book[99] =
{ 
	THROWDAGGER, POISONSTAR, IRONDEFENSE, NINJADISARM, KNOCKOUTSPIN, SETMASSTRAPS, NINJAKNOCKBACK
}

school_book[77] =
{ 
	PHASEBOLT, PHASEBEAM, PHASEBALL, PHASEWAVE, PHASECLOUD, PHASESHIELD
}

school_book[78] =
{
	CLAW_NETHERBOLT, CLAWS_SLASH, CLAW_NETHERCLOUD
}

school_book[97] =
{
	HOOVE_NEXUSBOLT, HOOVE_WALL, HOOVE_SPEED
}

school_book[96] =
{
	ARMOUR_SUPERAC, ARMOUR_NETHWAVE, ARMOUR_NEXSHIELD
}

school_book[97] =
{
	THROWMINDBLADE, MIND_BLADE, MINDSPEED, MINDSHIELD
}

school_book[98] =
{
	SURVEY_AREA, FIND_HIDDEN, DISMANTLE, SPARKY_SKILLS, BUILD_DOOR, KNOCK_DOWN_WALL, PLUMBERS_MATE, BUILD_WALL, BUILD_STAIR, NAIL_GUNS, DEMOLITION, REBUILD_DUNGEON  
}