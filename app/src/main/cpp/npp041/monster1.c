/* File: monster1.c */

/*
 * Copyright (c) 1997 Ben Harrison, James E. Wilson, Robert A. Koeneke
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.  Other copyrights may also apply.
 */

#include "angband.h"


/*
 * Pronoun arrays, by gender.
 */
static cptr wd_he[3] =
{ "it", "he", "she" };
static cptr wd_his[3] =
{ "its", "his", "her" };


/*
 * Pluralizer.  Args(count, singular, plural)
 */
#define plural(c,s,p) \
	(((c) == 1) ? (s) : (p))

#define PLAYER_GHOST_TRIES_MAX 30

/*
 * Determine if the "armor" is known
 * The higher the level, the fewer kills needed.
 */
static bool know_armour(int r_idx, s32b kills)
{
	const monster_race *r_ptr = &r_info[r_idx];

	s32b level = r_ptr->level;

	/* Normal monsters */
	if (kills > 304 / (4 + level)) return (TRUE);

	/* Skip non-uniques */
	if (!(r_ptr->flags1 & (RF1_UNIQUE))) return (FALSE);

	/* Unique monsters */
	if (kills > 304 / (38 + (5*level) / 4)) return (TRUE);

	/* Assume false */
	return (FALSE);
}


/*
 * Determine if the "mana" is known
 * The higher the level, the fewer kills needed.
 */
static bool know_mana_or_spells(int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];

	s32b level = r_ptr->level;

	s32b kills = l_list[r_idx].tkills;

	/*Hack - always know about ghosts*/
	if (r_ptr->flags2 & (RF2_PLAYER_GHOST)) return (TRUE);

	/* Mages learn quickly. */
	if (cp_ptr->spell_book == TV_MAGIC_BOOK) kills *= 2;

	/* Normal monsters */
	if (kills > 304 / (4 + level)) return (TRUE);

	/* Skip non-uniques */
	if (!(r_ptr->flags1 & RF1_UNIQUE)) return (FALSE);

	/* Unique monsters */
	if (kills > 304 / (38 + (5 * level) / 4)) return (TRUE);

	/* Assume false */
	return (FALSE);
}


/*
 * Determine if the "damage" of the given attack is known
 * the higher the level of the monster, the fewer the attacks you need,
 * the more damage an attack does, the more attacks you need
 */
static bool know_damage(int r_idx, const monster_lore *l_ptr, int i)
{
	const monster_race *r_ptr = &r_info[r_idx];

	s32b level = r_ptr->level;

	s32b a = l_ptr->blows[i];

	s32b d1 = r_ptr->blow[i].d_dice;
	s32b d2 = r_ptr->blow[i].d_side;

	s32b d = d1 * d2;

	/* Hack - keep the target number reasonable */
	if (d > 100) d = 100;

	/* Normal monsters */
	if ((4 + level) * a > 80 * d) return (TRUE);

	/* Skip non-uniques */
	if (!(r_ptr->flags1 & RF1_UNIQUE)) return (FALSE);

	/* Unique monsters */
	if ((4 + level) * (2 * a) > 80 * d) return (TRUE);

	/* Assume false */
	return (FALSE);
}


static void describe_monster_desc(int r_idx)
{
	const monster_race *r_ptr = &r_info[r_idx];
	char buf[2048];

	/* Simple method */
	my_strcpy(buf, r_text + r_ptr->text, sizeof(buf));

	/* Dump it */
	text_out(buf);
	text_out("\n");
}


static void describe_monster_spells(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];
	int m, n;
	int msex = 0;
	int spower;
	bool breath = FALSE;
	bool magic = FALSE;
	int vn;
	cptr vp[64];

	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;

	/* Get spell power */
	spower = r_ptr->spell_power;

	/* Collect innate attacks */
	vn = 0;

	if (l_ptr->flags4 & (RF4_SHRIEK))		vp[vn++] = "shriek for help";

	if (l_ptr->flags4 & (RF4_LASH))
	{
		if (l_ptr->flags3 & (RF3_ANIMAL) || (r_ptr->blow[0].effect == RBE_ACID))
			vp[vn++] = "spit at you from a distance";
		else
			vp[vn++] = "lash you if nearby";
	}

	if (l_ptr->flags4 & (RF4_BOULDER))
	{
		if (spower < 8) vp[vn++] = "throw rocks";
		else vp[vn++] = "throw boulders";
	}

	if (l_ptr->flags4 & (RF4_SHOT))
	{
		if (spower < 4) vp[vn++] = "sling pebbles";
		else if (spower < 10) vp[vn++] = "sling leaden pellets";
		else vp[vn++] = "sling seeker shots";
	}

	if (l_ptr->flags4 & (RF4_ARROW))
	{
		if (spower < 4) vp[vn++] = "shoot little arrows";
		else if (spower < 10) vp[vn++] = "shoot arrows";
		else vp[vn++] = "shoot seeker arrows";
	}

	if (l_ptr->flags4 & (RF4_BOLT))
	{
		if (spower < 4) vp[vn++] = "fire bolts";
		else if (spower < 10) vp[vn++] = "fire crossbow quarrels";
		else vp[vn++] = "fire seeker bolts";
	}

	if (l_ptr->flags4 & (RF4_MISSL))
	{
		if (spower < 4) vp[vn++] = "fire little missiles";
		else if (spower < 10) vp[vn++] = "fire missiles";
		else vp[vn++] = "fire heavy missiles";
	}

	if (l_ptr->flags4 & (RF4_PMISSL)) vp[vn++] = "whip poisoned darts";

	/* Describe innate attacks */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0)
			{
				text_out(" may ");
			}

			else if (n < vn-1) text_out(", ");
			else if (n == 1) text_out(" or ");
			else text_out(", or ");


			/* Dump */
			text_out_c(TERM_L_RED, vp[n]);
		}

		/* End */
		text_out(".  ");
	}

	/* Collect breaths */
	vn = 0;

	if (l_ptr->flags4 & (RF4_BRTH_ACID))       vp[vn++] = "acid";
	if (l_ptr->flags4 & (RF4_BRTH_ELEC))       vp[vn++] = "lightning";
	if (l_ptr->flags4 & (RF4_BRTH_FIRE))       vp[vn++] = "fire";
	if (l_ptr->flags4 & (RF4_BRTH_COLD))       vp[vn++] = "frost";
	if (l_ptr->flags4 & (RF4_BRTH_POIS))       vp[vn++] = "poison";
	if (l_ptr->flags4 & (RF4_BRTH_PLAS))       vp[vn++] = "plasma";

	if (l_ptr->flags4 & (RF4_BRTH_LITE))       vp[vn++] = "light";
	if (l_ptr->flags4 & (RF4_BRTH_DARK))	   vp[vn++] = "darkness";
	if (l_ptr->flags4 & (RF4_BRTH_CONFU))      vp[vn++] = "confusion";
	if (l_ptr->flags4 & (RF4_BRTH_SOUND))      vp[vn++] = "sound";
	if (l_ptr->flags4 & (RF4_BRTH_SHARD))      vp[vn++] = "shards";
	if (l_ptr->flags4 & (RF4_BRTH_INER))       vp[vn++] = "inertia";
	if (l_ptr->flags4 & (RF4_BRTH_GRAV))       vp[vn++] = "gravity";
	if (l_ptr->flags4 & (RF4_BRTH_FORCE))      vp[vn++] = "force";

	if (l_ptr->flags4 & (RF4_BRTH_NEXUS))      vp[vn++] = "nexus";
	if (l_ptr->flags4 & (RF4_BRTH_NETHR))      vp[vn++] = "nether";
	if (l_ptr->flags4 & (RF4_BRTH_CHAOS))      vp[vn++] = "chaos";
	if (l_ptr->flags4 & (RF4_BRTH_DISEN))      vp[vn++] = "disenchantment";
	if (l_ptr->flags4 & (RF4_BRTH_TIME))       vp[vn++] = "time";
	if (l_ptr->flags4 & (RF4_BRTH_MANA))       vp[vn++] = "mana";

	if (l_ptr->flags4 & (RF4_RF4XXX1))         vp[vn++] = "something";
	if (l_ptr->flags4 & (RF4_RF4XXX2))            vp[vn++] = "something";
	if (l_ptr->flags4 & (RF4_RF4XXX3))            vp[vn++] = "something";

	/* Describe breaths */
	if (vn)
	{
		/* Note breath */
		breath = TRUE;

		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" may breathe ");
			else if (n < vn-1) text_out(", ");
			else if (n == 1) text_out(" or ");
			else text_out(", or ");

			/* Dump */
			text_out_c(TERM_L_RED, vp[n]);
		}

		/*note powerful*/
		if (l_ptr->flags2 & (RF2_POWERFUL)) text_out(" powerfully");
	}


	/* Collect spells */
	vn = 0;

	if (l_ptr->flags5 & (RF5_BALL_ACID))
	{
		if (spower < 40) vp[vn++] = "produce acid balls";
		else vp[vn++] = "produce acid storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_ELEC))
	{
		if (spower < 40) vp[vn++] = "produce lightning balls";
		else vp[vn++] = "produce lightning storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_FIRE))
	{
		if (spower < 40) vp[vn++] = "produce fire balls";
		else vp[vn++] = "produce fire storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_COLD))
	{
		if (spower < 40) vp[vn++] = "produce frost balls";
		else vp[vn++] = "produce frost storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_POIS))
	{
		if (spower < 10) vp[vn++] = "produce stinking clouds";
		else if (spower < 40) vp[vn++] = "produce poison balls";
		else vp[vn++] = "produce storms of poison";
	}

	if (l_ptr->flags5 & (RF5_BALL_LITE))
	{
		if (spower < 10) vp[vn++] = "produce spheres of light";
		else if (spower < 40) vp[vn++] = "produce explosions of light";
		else vp[vn++] = "invoke starbursts";
	}

	if (l_ptr->flags5 & (RF5_BALL_DARK))
	{
		if (spower < 40) vp[vn++] = "produce balls of darkness";
		else vp[vn++] = "produce storms of darkness";
	}

	if (l_ptr->flags5 & (RF5_BALL_CONFU))
	{
		if (spower < 40) vp[vn++] = "produce confusion balls";
		else vp[vn++] = "produce storms of confusion";
	}

	if (l_ptr->flags5 & (RF5_BALL_SOUND))
	{
		if (spower < 10) vp[vn++] = "produce explosions of sound";
		else if (spower < 40) vp[vn++] = "produce thunderclaps";
		else vp[vn++] = "unleash storms of sound";
	}

	if (l_ptr->flags5 & (RF5_BALL_SHARD))
	{
		if (spower < 10) vp[vn++] = "produce blasts of shards";
		else if (spower < 50) vp[vn++] = "produce whirlwinds of shards";
		else vp[vn++] = "call up storms of knives";
	}

	if (l_ptr->flags5 & (RF5_BALL_STORM))
	{
		if (spower < 22) vp[vn++] = "produce little storms";
		else if (spower < 40) vp[vn++] = "produce whirlpools";
		else vp[vn++] = "call up raging storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_NETHR))
	{
		if (spower < 22) vp[vn++] = "produce nether orbs";
		else if (spower < 40) vp[vn++] = "produce nether balls";
		else vp[vn++] = "invoke nether storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_CHAOS))
	{
		if (spower < 13) vp[vn++] = "produce spheres of chaos";
		else if (spower < 40) vp[vn++] = "produce explosions of chaos";
		else vp[vn++] = "call up maelstroms of raw chaos";
	}

	if (l_ptr->flags5 & (RF5_BALL_MANA))
	{
		if (spower < 25) vp[vn++] = "produce manabursts";
		else if (spower < 50) vp[vn++] = "produce balls of mana";
		else vp[vn++] = "invoke mana storms";
	}

	if (l_ptr->flags5 & (RF5_BALL_WATER))
	{
		if (spower < 16) vp[vn++] = "produce water balls";
		else if (spower < 40) vp[vn++] = "produce water balls";
		else vp[vn++] = "produce storms of water balls";
	}

	if (l_ptr->flags5 & (RF5_HOLY_ORB))
	{
		if (spower < 25) vp[vn++] = "produce orbs of draining";
		else if (spower < 50) vp[vn++] = "produce powerful orbs of draining";
		else vp[vn++] = "produce large orbs of holy might";
	}

	if (l_ptr->flags5 & (RF5_BOLT_ACID))		vp[vn++] = "produce acid bolts";
	if (l_ptr->flags5 & (RF5_BOLT_ELEC))		vp[vn++] = "produce lightning bolts";
	if (l_ptr->flags5 & (RF5_BOLT_FIRE))		vp[vn++] = "produce fire bolts";
	if (l_ptr->flags5 & (RF5_BOLT_COLD))		vp[vn++] = "produce frost bolts";
	if (l_ptr->flags5 & (RF5_BOLT_POIS))		vp[vn++] = "produce poison bolts";
	if (l_ptr->flags5 & (RF5_BOLT_PLAS))		vp[vn++] = "produce plasma bolts";
	if (l_ptr->flags5 & (RF5_BOLT_ICE))		vp[vn++] = "produce ice bolts";
	if (l_ptr->flags5 & (RF5_BOLT_WATER))	vp[vn++] = "produce water bolts";
	if (l_ptr->flags5 & (RF5_BOLT_NETHR))
	{
		if (spower < 40) vp[vn++] = "casts a nether bolt";
		else vp[vn++] = "hurls black bolts of nether";
	}

	if (l_ptr->flags5 & (RF5_BOLT_MANA))
	{
		if (spower < 5) vp[vn++] = "casts magic missiles";
		else vp[vn++] = "cast mana bolts";
	}

	if (l_ptr->flags5 & (RF5_BEAM_ELEC))	vp[vn++] = "shoot sparks of lightning";
	if (l_ptr->flags5 & (RF5_BEAM_ICE))		vp[vn++] = "cast lances of ice";

	if (l_ptr->flags5 & (RF5_BEAM_NETHR))
	{
		if (spower < 25) vp[vn++] = "cast beams of nether";
		else if (spower < 50) vp[vn++] = "hurl lances of nether";
		else vp[vn++] = "shoot rays of death";
	}

	if (l_ptr->flags6 & RF6_HASTE)       vp[vn++] = "haste-self";
	if (l_ptr->flags6 & (RF6_ADD_MANA))		vp[vn++] = "restore mana";
	if (l_ptr->flags6 & RF6_HEAL)        vp[vn++] = "heal-self";
	if (l_ptr->flags6 & (RF6_CURE))		vp[vn++] = "cure what ails it";
	if (l_ptr->flags6 & RF6_BLINK)       vp[vn++] = "blink-self";
	if (l_ptr->flags6 & RF6_TPORT)       vp[vn++] = "teleport-self";
	if (l_ptr->flags6 & (RF6_TELE_SELF_TO))	vp[vn++] = "teleport toward you";
	if (l_ptr->flags6 & RF6_TELE_TO)     vp[vn++] = "teleport to";
	if (l_ptr->flags6 & RF6_TELE_AWAY)   vp[vn++] = "teleport away";
	if (l_ptr->flags6 & RF6_TELE_LEVEL)  vp[vn++] = "teleport level";
	if (l_ptr->flags6 & RF6_DARKNESS)    vp[vn++] = "create darkness";
	if (l_ptr->flags6 & RF6_TRAPS)       vp[vn++] = "create traps";
	if (l_ptr->flags6 & RF6_FORGET)      vp[vn++] = "cause amnesia";

	if (l_ptr->flags6 & (RF6_DRAIN_MANA))	vp[vn++] = "drain mana";
	if (l_ptr->flags6 & (RF6_MIND_BLAST))	vp[vn++] = "cause mind blasting";
	if (l_ptr->flags6 & (RF6_BRAIN_SMASH))	vp[vn++] = "cause brain smashing";
	if (l_ptr->flags6 & (RF6_WOUND))
	{
		if (spower < 4) vp[vn++] = "cause light wounds";
		else if (spower < 10) vp[vn++] = "cause medium wounds";
		else if (spower < 20) vp[vn++] = "cause serious wounds";
		else if (spower < 35) vp[vn++] = "cause critical wounds";
		else vp[vn++] = "cause mortal wounds";
	}
	if (l_ptr->flags6 & (RF6_HUNGER))		vp[vn++] = "cause hunger";
	if (l_ptr->flags6 & (RF6_SCARE))		vp[vn++] = "terrify";
	if (l_ptr->flags6 & (RF6_BLIND))		vp[vn++] = "blind";
	if (l_ptr->flags6 & (RF6_CONF))		vp[vn++] = "confuse";
	if (l_ptr->flags6 & (RF6_SLOW))		vp[vn++] = "slow";
	if (l_ptr->flags6 & (RF6_HOLD))		vp[vn++] = "paralyze";

	m = vn;

	/* Summons are described somewhat differently. */
	if (l_ptr->flags7)
	{

		/* Summons */
		if (l_ptr->flags7 & (RF7_S_KIN))
		{
			if (r_ptr->flags1 & (RF1_UNIQUE))
			{
				if (r_ptr->flags1 & (RF1_FEMALE)) vp[vn++] = "her minions";
				else if (r_ptr->flags1 & (RF1_MALE)) vp[vn++] = "his minions";
				else vp[vn++] = "its minions";
			}
			else
				vp[vn++] = "similar monsters";
		}
		if (l_ptr->flags7 & (RF7_S_MONSTER))		vp[vn++] = "a monster";
		if (l_ptr->flags7 & (RF7_S_MONSTERS))	vp[vn++] = "monsters";
		if (l_ptr->flags7 & (RF7_S_ANT))		vp[vn++] = "ants";
		if (l_ptr->flags7 & (RF7_S_SPIDER))		vp[vn++] = "spiders";
		if (l_ptr->flags7 & (RF7_S_HOUND))		vp[vn++] = "hounds";
		if (l_ptr->flags7 & (RF7_S_ANIMAL))		vp[vn++] = "natural creatures";
		if (l_ptr->flags7 & (RF7_S_HYDRA))		vp[vn++] = "hydras";
		if (l_ptr->flags7 & (RF7_S_THIEF))		vp[vn++] = "thieves";
		if (l_ptr->flags7 & (RF7_S_BERTBILLTOM))	vp[vn++] = "his friends";
		if (l_ptr->flags7 & (RF7_S_DRAGON))		vp[vn++] = "a dragon";
		if (l_ptr->flags7 & (RF7_S_HI_DRAGON))	vp[vn++] = "Ancient Dragons";
		if (l_ptr->flags7 & (RF7_S_AINU))		vp[vn++] = "a maia";
		if (l_ptr->flags7 & (RF7_S_DEMON))		vp[vn++] = "a demon";
		if (l_ptr->flags7 & (RF7_S_HI_DEMON))	vp[vn++] = "Greater Demons";
		if (l_ptr->flags7 & (RF7_S_UNIQUE))		vp[vn++] = "Unique Monsters";
		if (l_ptr->flags7 & (RF7_S_HI_UNIQUE))	vp[vn++] = "Greater Unique Monsters";
		if (l_ptr->flags7 & (RF7_S_UNDEAD))		vp[vn++] = "an undead";
		if (l_ptr->flags7 & (RF7_S_HI_UNDEAD))	vp[vn++] = "Greater Undead";
		if (l_ptr->flags7 & (RF7_S_WRAITH))		vp[vn++] = "the Ringwraiths";

	}



	/* Describe spells */
	if (vn)
	{
		/* Note magic */
		magic = TRUE;

		/* Intro */
		if (breath)
		{
			text_out(", and is also");
		}
		else
		{
			text_out(format("%^s is", wd_he[msex]));
		}

		/* Verb Phrase */
		text_out(" magical, casting spells");

		/* Adverb */
		if (l_ptr->flags2 & RF2_SMART) text_out_c(TERM_ORANGE, " intelligently");

		/* Normal spells */
		for (n = 0; n < m; n++)
		{
			if (n == 0)       text_out(" which ");
			else if (n < m-1) text_out(", ");
			else if (n != 1)  text_out(", or ");
			else              text_out(" or ");

			/* Dump */
			text_out_c(TERM_L_RED, vp[n]);
		}

		/* Summons */
		for (n = m; n < vn; n++)
		{
			if (n == 0) text_out(" which summon ");
			else if (n == m) text_out(", or summon ");
			else if (n < vn-1) text_out(", ");
			else if (n == m+1) text_out(" or ");
			else text_out(", or ");

			/* Dump */
			text_out_c(TERM_L_RED, vp[n]);
		}
	}


	/* End the sentence about innate/other spells */
	if (breath || magic)
	{
		/* Total casting */
		m = l_ptr->ranged;

		/* Average frequency */
		n = (r_ptr->freq_ranged);

		/*players don't hone in on spell frequency right away*/
		if (m < 75)
		{
			/*sometimes minus, sometimes plus*/
			if (n % 2) n -= ((100 - m) / 10);
			else n += ((100 - m) / 10);

			/*boundry control*/
			if (n > 100) n = 100;
			if (n < 1) n = 1;

		}

		/* Describe the spell frequency */
		if (m > 30)
		{
			text_out(format(" about %d percent of the time", n));
		}

		/* Describe monster mana and spellpower*/
		if (((r_ptr->mana) || (r_ptr->spell_power)) && know_mana_or_spells(r_idx))
		{
			text_out(" with");

			/* Mana */
			if (r_ptr->mana)
			{
				text_out(format(" a mana rating of %d", r_ptr->mana));

				if (r_ptr->spell_power) text_out(" and");
			}

			/* spell power */
			if (r_ptr->spell_power)
			{
				text_out(format(" a spell power of %d", r_ptr->spell_power));
			}
		}

		/* End this sentence */
		text_out(".  ");
	}
}


static void describe_monster_drop(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	bool sin = FALSE;

	int n;

	cptr p;

	int msex = 0;


	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;

	/* Drops gold and/or items */
	if (l_ptr->drop_gold || l_ptr->drop_item)
	{
		/* Intro */
		text_out(format("%^s may carry", wd_he[msex]));

		/* Count maximum drop */
		n = MAX(l_ptr->drop_gold, l_ptr->drop_item);

		/* One drop (may need an "n") */
		if (n == 1)
		{
			text_out(" a");
			sin = TRUE;
		}

		/* Two drops */
		else if (n == 2)
		{
			text_out(" one or two");
		}

		/* Many drops */
		else
		{
			text_out(format(" up to %d", n));
		}


		/* Chests are not noted as good or great
		 * (no "n" needed)
		 */
		if (l_ptr->flags1 & RF1_DROP_CHEST)
		{
			p = NULL;
			sin = FALSE;
		}

		/* Great */
		else if (l_ptr->flags1 & RF1_DROP_GREAT)
		{
			p = " exceptional";
		}

		/* Good (no "n" needed) */
		else if (l_ptr->flags1 & RF1_DROP_GOOD)
		{
			p = " good";
			sin = FALSE;
		}

		/* Okay */
		else
		{
			p = NULL;
		}


		/* Objects */
		if (l_ptr->drop_item)
		{
			/* Handle singular "an" */
			if (sin) text_out("n");
			sin = FALSE;

			/* Dump "object(s)" */
			if (p) text_out(p);

			/*specify chests where needed*/
			if (l_ptr->flags1 & RF1_DROP_CHEST) text_out(" chest");
			else text_out(" object");
			if (n != 1) text_out("s");

			/* Conjunction replaces variety, if needed for "gold" below */
			p = " or";
		}

		/* Treasures */
		if (l_ptr->drop_gold)
		{
			/* Cancel prefix */
			if (!p) sin = FALSE;

			/* Handle singular "an" */
			if (sin) text_out("n");

			/* Dump "treasure(s)" */
			if (p) text_out(p);
			text_out(" treasure");
			if (n != 1) text_out("s");
		}

		/* End this sentence */
		text_out(".  ");
	}
}


static void describe_monster_attack(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];
	int m, r, n;
	cptr p, q;

	int msex = 0;

	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;

	/* Count the number of "known" attacks */
	for (n = 0, m = 0; m < MONSTER_BLOW_MAX; m++)
	{
		/* Skip non-attacks */
		if (!r_ptr->blow[m].method) continue;

		/* Count known attacks */
		if ((l_ptr->blows[m]) || (l_ptr->sights == MAX_SHORT) ||
			                      (l_ptr->ranged == MAX_UCHAR)) n++;
	}

	/* Examine (and count) the actual attacks */
	for (r = 0, m = 0; m < MONSTER_BLOW_MAX; m++)
	{
		int method, effect, d1, d2;

		/* Skip non-attacks */
		if (!r_ptr->blow[m].method) continue;

		/* Skip unknown attacks */
		if (!l_ptr->blows[m]) continue;

		/* Extract the attack info */
		method = r_ptr->blow[m].method;
		effect = r_ptr->blow[m].effect;
		d1 = r_ptr->blow[m].d_dice;
		d2 = r_ptr->blow[m].d_side;

		/* No method yet */
		p = NULL;

		/* Get the method */
		switch (method)
		{
			case RBM_HIT:           p = "hit"; break;
			case RBM_TOUCH:         p = "touch"; break;
			case RBM_PUNCH:         p = "punch"; break;
			case RBM_KICK:          p = "kick"; break;
			case RBM_CLAW:          p = "claw"; break;
			case RBM_BITE:          p = "bite"; break;
			case RBM_PECK:          p = "peck"; break;
			case RBM_STING:         p = "sting"; break;
			case RBM_XXX1:          break;
			case RBM_BUTT:          p = "butt"; break;
			case RBM_CRUSH:         p = "crush"; break;
			case RBM_ENGULF:        p = "engulf"; break;
			case RBM_CRAWL:         p = "crawl on you"; break;
			case RBM_DROOL:         p = "drool on you"; break;
			case RBM_SPIT:          p = "spit"; break;
			case RBM_SLIME:         p = "slime"; break;
			case RBM_GAZE:          p = "gaze"; break;
			case RBM_WAIL:          p = "wail"; break;
			case RBM_SPORE:         p = "release spores"; break;
			case RBM_XXX4:          break;
			case RBM_BEG:           p = "beg"; break;
			case RBM_INSULT:        p = "insult"; break;
			case RBM_XXX5:          break;
			case RBM_XXX6:			break;
		}


		/* Default effect */
		q = NULL;

		/* Get the effect */
		switch (effect)
		{
			case RBE_HURT:          q = "attack"; break;
			case RBE_WOUND:         q = "wound"; break;
			case RBE_BATTER:        q = "stun"; break;
			case RBE_SHATTER:       q = "shatter"; break;

			case RBE_UN_BONUS:      q = "disenchant"; break;
			case RBE_UN_POWER:      q = "drain charges"; break;
			case RBE_LOSE_MANA:     q = "drain mana"; break;
			case RBE_EAT_GOLD:      q = "steal gold"; break;
			case RBE_EAT_ITEM:      q = "steal items"; break;
			case RBE_EAT_FOOD:      q = "eat your food"; break;
			case RBE_EAT_LITE:      q = "absorb light"; break;
			case RBE_HUNGER:        q = "cause hunger"; break;

			case RBE_POISON:        q = "poison"; break;
			case RBE_ACID:          q = "shoot acid"; break;
			case RBE_ELEC:          q = "electrocute"; break;
			case RBE_FIRE:          q = "burn"; break;
			case RBE_COLD:          q = "freeze"; break;

			case RBE_BLIND:         q = "blind"; break;
			case RBE_CONFUSE:       q = "confuse"; break;
			case RBE_TERRIFY:       q = "terrify"; break;
			case RBE_PARALYZE:      q = "paralyze"; break;
			case RBE_HALLU:         q = "induce hallucinations"; break;
			case RBE_DISEASE:       q = "cause disease"; break;

			case RBE_LOSE_STR:      q = "reduce strength"; break;
			case RBE_LOSE_INT:      q = "reduce intelligence"; break;
			case RBE_LOSE_WIS:      q = "reduce wisdom"; break;
			case RBE_LOSE_DEX:      q = "reduce dexterity"; break;
			case RBE_LOSE_CON:      q = "reduce constitution"; break;
			case RBE_LOSE_CHR:      q = "reduce charisma"; break;
			case RBE_LOSE_ALL:      q = "reduce all stats"; break;

			case RBE_EXP_10:        q = "lower experience (by 10d6+)"; break;
			case RBE_EXP_20:        q = "lower experience (by 20d6+)"; break;
			case RBE_EXP_40:        q = "lower experience (by 40d6+)"; break;
			case RBE_EXP_80:        q = "lower experience (by 80d6+)"; break;
		}

		/* Introduce the attack description */
		if (!r)
		{
			text_out(format("%^s can ", wd_he[msex]));
		}
		else if (r < n-1)
		{
			text_out(", ");
		}
		else
		{
			text_out(", and ");
		}


		/* Hack -- force a method */
		if (!p) p = "do something weird";

		/* Describe the method */
		text_out(p);


		/* Describe the effect (if any) */
		if (q)
		{
			/* Describe the attack type */
			text_out(" to ");
			text_out_c(TERM_L_RED, q);

			/* Describe damage (if known) */
			if (d1 && d2 && ((know_damage(r_idx, l_ptr, m)) || (l_ptr->sights == MAX_SHORT) ||
			                      (l_ptr->ranged == MAX_UCHAR)))
			{
				/* Display the damage */
				text_out(" with damage");
				text_out(format(" %dd%d", d1, d2));
			}
		}


		/* Count the attacks as printed */
		r++;
	}

	/* Finish sentence above */
	if (r)
	{
		text_out(".  ");
	}

	/* Notice lack of attacks */
	else if (l_ptr->flags1 & RF1_NEVER_BLOW)
	{
		text_out(format("%^s has no physical attacks.  ", wd_he[msex]));
	}

	/* Or describe the lack of knowledge */
	else
	{
		text_out(format("Nothing is known about %s attack.  ", wd_his[msex]));
	}

}


static void describe_monster_abilities(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	int n;

	int vn;
	cptr vp[64];

	int msex = 0;

	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;

	/* Collect special abilities. */
	vn = 0;
	if (l_ptr->flags2 & RF2_HAS_LITE)
	{
		/*humaniods carry torches, others glow*/
		if (!strchr("hkoOTtPp", r_ptr->d_char)) vp[vn++] = "radiate natural light";
		else vp[vn++] = "use a light source";
	}
	if (l_ptr->flags2 & RF2_EVASIVE) vp[vn++] = "dodge attacks";
	if (l_ptr->flags2 & RF2_OPEN_DOOR) vp[vn++] = "open doors";
	if (l_ptr->flags2 & RF2_BASH_DOOR) vp[vn++] = "bash down doors";
	if (l_ptr->flags2 & RF2_PASS_WALL) vp[vn++] = "pass through walls";
	if (l_ptr->flags2 & RF2_KILL_WALL) vp[vn++] = "bore through walls";
	if (l_ptr->flags2 & RF2_KILL_BODY) vp[vn++] = "destroy weaker monsters";
	if (l_ptr->flags2 & RF2_TAKE_ITEM) vp[vn++] = "pick up objects";
	if (l_ptr->flags2 & RF2_KILL_ITEM) vp[vn++] = "destroy objects";

	/* Describe special abilities. */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" can ");
			else if (n < vn-1) text_out(", ");
			else text_out(" and ");

			/* Dump */
			text_out(vp[n]);
		}

		/* End */
		text_out(".  ");
	}

	/*note if this is an unused ghost template*/
	if ((r_ptr->flags2 & (RF2_PLAYER_GHOST)) && (r_ptr->cur_num == 0))
	{
		text_out(format("%^s is a player ghost template.  ", wd_he[msex]));
	}

	/* Describe special abilities. */
	if (l_ptr->flags2 & RF2_INVISIBLE)
	{
		text_out(format("%^s is invisible.  ", wd_he[msex]));
	}
	if (l_ptr->flags2 & RF2_COLD_BLOOD)
	{
		text_out(format("%^s is cold blooded.  ", wd_he[msex]));
	}
	if (l_ptr->flags2 & RF2_EMPTY_MIND)
	{
		text_out(format("%^s is not detected by telepathy.  ", wd_he[msex]));
	}
	if (l_ptr->flags2 & RF2_WEIRD_MIND)
	{
		text_out(format("%^s is rarely detected by telepathy.  ", wd_he[msex]));
	}
	if (l_ptr->flags2 & RF2_MULTIPLY)
	{
		text_out(format("%^s breeds explosively.  ", wd_he[msex]));
	}
	if (l_ptr->flags2 & RF2_REGENERATE)
	{
		text_out(format("%^s regenerates quickly.  ", wd_he[msex]));
	}

	if (l_ptr->flags2 & (RF2_CLOUD_SURROUND))
	{
		int typ = 0, dam = 0, rad = 0;

		/* Get type of cloud */
		cloud_surround(r_idx, &typ, &dam, &rad);

		/*hack - alter type for char-attr monster*/

		if ((r_ptr->flags1 & (RF1_ATTR_MULTI)) &&
			(r_ptr->flags4 & (RF4_BRTH_FIRE)) &&
			(r_ptr->flags4 & (RF4_BRTH_POIS)) &&
			(r_ptr->flags4 & (RF4_BRTH_ACID)) &&
			(r_ptr->flags4 & (RF4_BRTH_ELEC)) &&
			(r_ptr->flags4 & (RF4_BRTH_COLD)))
			{
				text_out(format("%^s is surrounded by an ever-changing cloud of elements.  ", wd_he[msex]));
			}


		/* We emit something */
		else if (typ)
		{
			text_out(format("%^s is surrounded by ", wd_he[msex]));

			/* Describe cloud */
			if (typ == GF_FIRE)           text_out("fire");
			else if (typ == GF_COLD)      text_out("frost");
			else if (typ == GF_ELEC)      text_out("lightning");
			else if (typ == GF_ACID)      text_out("acidic smoke");
			else if (typ == GF_POIS)      text_out("noxious gases");
			else if (typ == GF_SOUND)     text_out("a cacophony of sound");
			else if (typ == GF_SPORE)     text_out("spores");
			else if (typ == GF_DARK)      text_out("darkness");
			else if (typ == GF_DARK_WEAK) text_out("darkness");
			else                          text_out("powerful forces");
			text_out(".  ");
		}
	}

	/* Collect susceptibilities */
	vn = 0;
	if (l_ptr->flags3 & RF3_HURT_ROCK) vp[vn++] = "rock remover";
	if (l_ptr->flags3 & RF3_HURT_LITE) vp[vn++] = "bright light";
	if (l_ptr->flags3 & RF3_HURT_FIRE) vp[vn++] = "fire";
	if (l_ptr->flags3 & RF3_HURT_COLD) vp[vn++] = "cold";

	/* Describe susceptibilities */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" is hurt by ");
			else if (n < vn-1) text_out(", ");
			else text_out(" and ");

			/* Dump */
			text_out_c(TERM_YELLOW, vp[n]);
		}

		/* End */
		text_out(".  ");
	}


	/* Collect immunities */
	vn = 0;
	if (l_ptr->flags3 & RF3_IM_ACID) vp[vn++] = "acid";
	if (l_ptr->flags3 & RF3_IM_ELEC) vp[vn++] = "lightning";
	if (l_ptr->flags3 & RF3_IM_FIRE) vp[vn++] = "fire";
	if (l_ptr->flags3 & RF3_IM_COLD) vp[vn++] = "cold";
	if (l_ptr->flags3 & RF3_IM_POIS) vp[vn++] = "poison";

	/* Describe immunities */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" resists ");
			else if (n < vn-1) text_out(", ");
			else text_out(" and ");

			/* Dump */
			text_out_c(TERM_ORANGE, vp[n]);
		}

		/* End */
		text_out(".  ");
	}


	/* Collect resistances */
	vn = 0;
	if (l_ptr->flags3 & RF3_RES_CHAOS) vp[vn++] = "chaos";
	if (l_ptr->flags3 & RF3_RES_NETHR) vp[vn++] = "nether";
	if (l_ptr->flags3 & RF3_RES_WATER) vp[vn++] = "water";
	if (l_ptr->flags3 & RF3_RES_PLAS) vp[vn++] = "plasma";
	if (l_ptr->flags3 & RF3_RES_NEXUS) vp[vn++] = "nexus";
	if (l_ptr->flags3 & RF3_RES_DISEN) vp[vn++] = "disenchantment";

	/* Describe resistances */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" resists ");
			else if (n < vn-1) text_out(", ");
			else text_out(" and ");

			/* Dump */
			text_out_c(TERM_ORANGE, vp[n]);
		}

		/* End */
		text_out(".  ");
	}


	/* Collect non-effects */
	vn = 0;
	if (l_ptr->flags3 & RF3_NO_SLOW) vp[vn++] = "slowed";
	if (l_ptr->flags3 & RF3_NO_STUN) vp[vn++] = "stunned";
	if (l_ptr->flags3 & RF3_NO_FEAR) vp[vn++] = "frightened";
	if (l_ptr->flags3 & RF3_NO_CONF) vp[vn++] = "confused";
	if (l_ptr->flags3 & RF3_NO_SLEEP) vp[vn++] = "slept";

	/* Describe non-effects */
	if (vn)
	{
		/* Intro */
		text_out(format("%^s", wd_he[msex]));

		/* Scan */
		for (n = 0; n < vn; n++)
		{
			/* Intro */
			if (n == 0) text_out(" cannot be ");
			else if (n < vn-1) text_out(", ");
			else text_out(" or ");

			/* Dump */
			text_out_c(TERM_YELLOW, vp[n]);
		}

		/* End */
		text_out(".  ");
	}


	/* Do we know how aware it is? */
	if ((((int)l_ptr->wake * (int)l_ptr->wake) > r_ptr->sleep) ||
	    (l_ptr->ignore == MAX_UCHAR) ||
	    ((r_ptr->sleep == 0) && (l_ptr->tkills >= 10)))
	{
		cptr act;

		if (r_ptr->sleep > 200)
		{
			act = "prefers to ignore";
		}
		else if (r_ptr->sleep > 95)
		{
			act = "pays very little attention to";
		}
		else if (r_ptr->sleep > 75)
		{
			act = "pays little attention to";
		}
		else if (r_ptr->sleep > 45)
		{
			act = "tends to overlook";
		}
		else if (r_ptr->sleep > 25)
		{
			act = "takes quite a while to see";
		}
		else if (r_ptr->sleep > 10)
		{
			act = "takes a while to see";
		}
		else if (r_ptr->sleep > 5)
		{
			act = "is fairly observant of";
		}
		else if (r_ptr->sleep > 3)
		{
			act = "is observant of";
		}
		else if (r_ptr->sleep > 1)
		{
			act = "is very observant of";
		}
		else if (r_ptr->sleep > 0)
		{
			act = "is vigilant for";
		}
		else
		{
			act = "is ever vigilant for";
		}

		text_out(format("%^s %s intruders, which %s may notice from %d feet.  ",
		            wd_he[msex], act, wd_he[msex], 10 * r_ptr->aaf));
	}

	/* Describe escorts */
	if ((l_ptr->flags1 & RF1_ESCORT) || (l_ptr->flags1 & RF1_ESCORTS))
	{
		text_out(format("%^s usually appears with escorts.  ",
		            wd_he[msex]));
	}

	/* Describe friends */
	else if ((l_ptr->flags1 & RF1_FRIEND) || (l_ptr->flags1 & RF1_FRIENDS))
	{
		text_out(format("%^s usually appears in groups.  ",
		            wd_he[msex]));
	}
}


static void describe_monster_kills(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	int msex = 0;

	bool out = TRUE;


	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;


	/* Treat uniques differently */
	if (l_ptr->flags1 & RF1_UNIQUE)
	{
		/* Hack -- Determine if the unique is "dead" */
		bool dead = (r_ptr->max_num == 0) ? TRUE : FALSE;

		/* We've been killed... */
		if (l_ptr->deaths)
		{
			/* Killed ancestors */
			text_out(format("%^s has slain %d of your ancestors",
			            wd_he[msex], l_ptr->deaths));

			/* But we've also killed it */
			if (dead)
			{
				text_out(", but you have taken revenge!  ");
			}

			/* Unavenged (ever) */
			else
			{
				text_out(format(", who %s unavenged.  ",
				            plural(l_ptr->deaths, "remains", "remain")));
			}
		}

		/* Dead unique who never hurt us */
		else if (dead)
		{
			text_out("You have slain this foe.  ");
		}
		else
		{
			/* Alive and never killed us */
			out = FALSE;
		}
	}

	/* Not unique, but killed us */
	else if (l_ptr->deaths)
	{
		/* Dead ancestors */
		text_out(format("%d of your ancestors %s been killed by this creature, ",
		            l_ptr->deaths, plural(l_ptr->deaths, "has", "have")));

		/* Some kills this life */
		if (l_ptr->pkills)
		{
			text_out(format("and you have exterminated at least %d of the creatures.  ",
			            l_ptr->pkills));
		}

		/* Some kills past lives */
		else if (l_ptr->tkills)
		{
			text_out(format("and %s have exterminated at least %d of the creatures.  ",
			            "your ancestors", l_ptr->tkills));
		}

		/* No kills */
		else
		{
			text_out_c(TERM_RED, format("and %s is not ever known to have been defeated.  ",
			            wd_he[msex]));
		}
	}

	/* Normal monsters */
	else
	{
		/* Killed some this life */
		if (l_ptr->pkills)
		{
			text_out(format("You have killed at least %d of these creatures.  ",
			            l_ptr->pkills));
		}

		/* Killed some last life */
		else if (l_ptr->tkills)
		{
			text_out(format("Your ancestors have killed at least %d of these creatures.  ",
			            l_ptr->tkills));
		}

		/* Killed none */
		else
		{
			text_out("No battles to the death are recalled.  ");
		}
	}

	/* Separate */
	if (out) text_out("\n");
}


static void describe_monster_toughness(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	int msex = 0;

	/* Extract a gender (if applicable) */
	if (r_ptr->flags1 & RF1_FEMALE) msex = 2;
	else if (r_ptr->flags1 & RF1_MALE) msex = 1;

	/* Describe monster "toughness" */
	if ((know_armour(r_idx, l_ptr->tkills)) || (l_ptr->sights == MAX_SHORT) ||
			 (l_ptr->ranged == MAX_UCHAR))
	{
		/* Armor */
		text_out(format("%^s has an armor rating of %d",
		            wd_he[msex], r_ptr->ac));

		/* Maximized hitpoints */
		if (l_ptr->flags1 & (RF1_FORCE_MAXHP))
		{
			text_out(format(" and a life rating of %d.  ",
			            r_ptr->hdice * r_ptr->hside));
		}

		/* Variable hitpoints */
		else
		{
			text_out(format(" and a life rating of %dd%d.  ",
			            r_ptr->hdice, r_ptr->hside));
		}
	}
}


static void describe_monster_exp(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	cptr p, q;

	long i, j;


	/* Describe experience if known */
	if (l_ptr->tkills)
	{
		/* Introduction */
		if (l_ptr->flags1 & RF1_UNIQUE)
			text_out("Killing");
		else
			text_out("A kill of");

		text_out(" this creature");

		/* calculate the integer exp part */
		i = (long)r_ptr->mexp * r_ptr->level / p_ptr->lev;

		/* calculate the fractional exp part scaled by 100, */
		/* must use long arithmetic to avoid overflow */
		j = ((((long)r_ptr->mexp * r_ptr->level % p_ptr->lev) *
			  (long)1000 / p_ptr->lev + 5) / 10);

		/* Mention the experience */
		text_out(format(" is worth %ld.%02ld point%s",
			        (long)i, (long)j,
			        (((i == 1) && (j == 0)) ? "" : "s")));

		/* Take account of annoying English */
		p = "th";
		i = p_ptr->lev % 10;
		if ((p_ptr->lev / 10) == 1) /* nothing */;
		else if (i == 1) p = "st";
		else if (i == 2) p = "nd";
		else if (i == 3) p = "rd";

		/* Take account of "leading vowels" in numbers */
		q = "";
		i = p_ptr->lev;
		if ((i == 8) || (i == 11) || (i == 18)) q = "n";

		/* Mention the dependance on the player's level */
		text_out(format(" for a%s %lu%s level character.  ",
			        q, (long)i, p));
	}
}


static void describe_monster_movement(int r_idx, const monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	bool old = FALSE;

	text_out("This");

	if (l_ptr->flags3 & RF3_ANIMAL) text_out_c(TERM_L_BLUE, " natural");
	if (l_ptr->flags3 & RF3_EVIL) text_out_c(TERM_L_BLUE, " evil");
	if (l_ptr->flags3 & RF3_UNDEAD) text_out_c(TERM_L_BLUE, " undead");

	if (l_ptr->flags3 & RF3_DRAGON) text_out_c(TERM_L_BLUE, " dragon");
	else if (l_ptr->flags3 & RF3_DEMON) text_out_c(TERM_L_BLUE, " demon");
	else if (l_ptr->flags3 & RF3_GIANT) text_out_c(TERM_L_BLUE, " giant");
	else if (l_ptr->flags3 & RF3_TROLL) text_out_c(TERM_L_BLUE, " troll");
	else if (l_ptr->flags3 & RF3_ORC) text_out_c(TERM_L_BLUE, " orc");
	else text_out(" creature");

	/* Describe location */
	if (r_ptr->level == 0)
	{
		text_out_c(TERM_SLATE, " lives in the town");
		old = TRUE;
	}
	else if ((l_ptr->tkills)  || (l_ptr->sights == MAX_SHORT) ||
			 (l_ptr->ranged == MAX_UCHAR))
	{
		if (l_ptr->flags1 & RF1_FORCE_DEPTH)
			text_out_c(TERM_SLATE, " is found ");
		else
			text_out_c(TERM_SLATE, " is normally found ");

		if (depth_in_feet)
		{
			text_out_c(TERM_SLATE, format("at depths of %d feet",
			                            r_ptr->level * 50));
		}
		else
		{
			text_out_c(TERM_SLATE, format("on dungeon level %d",
			                            r_ptr->level));
		}
		old = TRUE;
	}

	if (old) text_out(", and");

	text_out(" moves");

	/* Random-ness */
	if ((l_ptr->flags1 & RF1_RAND_50) || (l_ptr->flags1 & RF1_RAND_25))
	{
		/* Adverb */
		if ((l_ptr->flags1 & RF1_RAND_50) && (l_ptr->flags1 & RF1_RAND_25))
		{
			text_out(" extremely");
		}
		else if (l_ptr->flags1 & RF1_RAND_50)
		{
			text_out(" somewhat");
		}
		else if (l_ptr->flags1 & RF1_RAND_25)
		{
			text_out(" a bit");
		}

		/* Adjective */
		text_out(" erratically");

		/* Hack -- Occasional conjunction */
		if (r_ptr->speed != 110) text_out(", and");
	}

	/* Speed */
	if (r_ptr->speed > 110)
	{

		if (r_ptr->speed > 139) text_out_c(TERM_GREEN, " incredibly");
		else if (r_ptr->speed > 134) text_out_c(TERM_GREEN, " extremely");
		else if (r_ptr->speed > 129) text_out_c(TERM_GREEN, " very");
		else if (r_ptr->speed > 124) text_out_c(TERM_GREEN, " exceedingly");
		else if (r_ptr->speed < 120) text_out_c(TERM_GREEN, " somewhat");
		text_out_c(TERM_GREEN, " quickly");

	}
	else if (r_ptr->speed < 110)
	{
		if (r_ptr->speed < 90) text_out_c(TERM_GREEN, " incredibly");
		else if (r_ptr->speed < 100) text_out_c(TERM_GREEN, " very");
		text_out_c(TERM_GREEN, " slowly");
	}
	else
	{
		text_out_c(TERM_GREEN, " at normal speed");
	}

	/* The code above includes "attack speed" */
	if (l_ptr->flags1 & RF1_NEVER_MOVE)
	{
		text_out(", but does not deign to chase intruders");
	}

	/* End this sentence */
	text_out(".  ");
}



/*
 * Learn everything about a monster (by cheating)
 */
static void cheat_monster_lore(int r_idx, monster_lore *l_ptr)
{
	const monster_race *r_ptr = &r_info[r_idx];

	int i;

	/* Hack -- Maximal kills */
	l_ptr->tkills = MAX_SHORT;

	/* Hack -- Maximal info */
	l_ptr->wake = l_ptr->ignore = MAX_UCHAR;

	/* Observe "maximal" attacks */
	for (i = 0; i < MONSTER_BLOW_MAX; i++)
	{
		/* Examine "actual" blows */
		if (r_ptr->blow[i].effect || r_ptr->blow[i].method)
		{
			/* Hack -- maximal observations */
			l_ptr->blows[i] = MAX_UCHAR;
		}
	}

	/* Hack -- maximal drops */
	l_ptr->drop_gold = l_ptr->drop_item =
	(((r_ptr->flags1 & RF1_DROP_4D2) ? 8 : 0) +
	 ((r_ptr->flags1 & RF1_DROP_3D2) ? 6 : 0) +
	 ((r_ptr->flags1 & RF1_DROP_2D2) ? 4 : 0) +
	 ((r_ptr->flags1 & RF1_DROP_1D2) ? 2 : 0) +
	 ((r_ptr->flags1 & RF1_DROP_90)  ? 1 : 0) +
	 ((r_ptr->flags1 & RF1_DROP_60)  ? 1 : 0));

	/* Hack -- but only "valid" drops */
	if (r_ptr->flags1 & RF1_ONLY_GOLD) l_ptr->drop_item = 0;
	if (r_ptr->flags1 & RF1_ONLY_ITEM) l_ptr->drop_gold = 0;

	/* Hack -- observe many spells */
	l_ptr->ranged = MAX_UCHAR;

	/* Hack -- know all the flags */
	l_ptr->flags1 = r_ptr->flags1;
	l_ptr->flags2 = r_ptr->flags2;
	l_ptr->flags3 = r_ptr->flags3;
	l_ptr->flags4 = r_ptr->flags4;
	l_ptr->flags5 = r_ptr->flags5;
	l_ptr->flags6 = r_ptr->flags6;
	l_ptr->flags7 = r_ptr->flags7;
}


/*
 * Hack -- display monster information using "roff()"
 *
 * Note that there is now a compiler option to only read the monster
 * descriptions from the raw file when they are actually needed, which
 * saves about 60K of memory at the cost of disk access during monster
 * recall, which is optional to the user.
 *
 * This function should only be called with the cursor placed at the
 * left edge of the screen, on a cleared line, in which the recall is
 * to take place.  One extra blank line is left after the recall.
 */
void describe_monster(int r_idx, bool spoilers)
{
	monster_lore lore;

	monster_lore save_mem;

	/* Get the race and lore */
	const monster_race *r_ptr = &r_info[r_idx];
	monster_lore *l_ptr = &l_list[r_idx];

	/* Cheat -- know everything */
	if ((cheat_know) || (r_ptr->flags2 & (RF2_PLAYER_GHOST)))
	{
		/* XXX XXX XXX */

		/* Hack -- save memory */
		COPY(&save_mem, l_ptr, monster_lore);
	}

	/* Hack -- create a copy of the monster-memory */
	COPY(&lore, l_ptr, monster_lore);

	/* Assume some "obvious" flags */
	lore.flags1 |= (r_ptr->flags1 & RF1_OBVIOUS_MASK);

	/* Killing a monster reveals some properties */
	if (lore.tkills)
	{
		/* Know "race" flags */
		lore.flags3 |= (r_ptr->flags3 & RF3_RACE_MASK);

		/* Know "forced" flags */
		lore.flags1 |= (r_ptr->flags1 & (RF1_FORCE_DEPTH | RF1_FORCE_MAXHP));
	}

	/* Cheat -- know everything */
	if (cheat_know || spoilers || (r_ptr->flags2 & (RF2_PLAYER_GHOST)))
	{
		cheat_monster_lore(r_idx, &lore);
	}

	/* Show kills of monster vs. player(s) */
	if (!spoilers && show_details)
		describe_monster_kills(r_idx, &lore);

	/* Monster description */
	if (spoilers || show_details)
		describe_monster_desc(r_idx);

	/* Describe the movement and level of the monster */
	describe_monster_movement(r_idx, &lore);

	/* Describe experience */
	if (!spoilers) describe_monster_exp(r_idx, &lore);

	/* Describe spells and innate attacks */
	describe_monster_spells(r_idx, &lore);

	/* Describe monster "toughness" */
	if (!spoilers) describe_monster_toughness(r_idx, &lore);

	/* Describe the abilities of the monster */
	describe_monster_abilities(r_idx, &lore);

	/* Describe the monster drop */
	describe_monster_drop(r_idx, &lore);

	/* Describe the known attacks */
	describe_monster_attack(r_idx, &lore);

	/* Notice "Quest" monsters */
	if (lore.flags1 & RF1_QUESTOR)
	{
		text_out("You feel an intense desire to kill this monster...  ");
	}

	/* All done */
	text_out("\n");

	/* Cheat -- know everything */
	if ((cheat_know) || (r_ptr->flags2 & (RF2_PLAYER_GHOST)))
	{
		/* Hack -- restore memory */
		COPY(l_ptr, &save_mem, monster_lore);
	}
}





/*
 * Hack -- Display the "name" and "attr/chars" of a monster race
 */
void roff_top(int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];

	byte a1, a2;
	char c1, c2;

	/* Get the chars */
	c1 = r_ptr->d_char;
	c2 = r_ptr->x_char;

	/* Get the attrs */
	a1 = r_ptr->d_attr;
	a2 = r_ptr->x_attr;

	/* Clear the top line */
	Term_erase(0, 0, 255);

	/* Reset the cursor */
	Term_gotoxy(0, 0);

	/* A title (use "The" for non-uniques) */
	if (!(r_ptr->flags1 & RF1_UNIQUE))
	{
		Term_addstr(-1, TERM_WHITE, "The ");
	}

	/* Special treatment for player ghosts. */
	if (r_ptr->flags2 & (RF2_PLAYER_GHOST))
	{
		Term_addstr(-1, TERM_WHITE, ghost_name);
		Term_addstr(-1, TERM_WHITE, ", the ");
	}

	/* Dump the name */
	Term_addstr(-1, TERM_WHITE, (r_name + r_ptr->name));

	/* Append the "standard" attr/char info */
	Term_addstr(-1, TERM_WHITE, " ('");
	Term_addch(a1, c1);
	Term_addstr(-1, TERM_WHITE, "')");

	/* Append the "optional" attr/char info */
	Term_addstr(-1, TERM_WHITE, "/('");
	Term_addch(a2, c2);
	if (use_bigtile && (a2 & 0x80)) Term_addch(255, -1);
	Term_addstr(-1, TERM_WHITE, "'):");
}



/*
 * Hack -- describe the given monster race at the top of the screen
 */
void screen_roff(int r_idx)
{
	/* Flush messages */
	message_flush();

	/* Begin recall */
	Term_erase(0, 1, 255);

	/* Output to the screen */
	text_out_hook = text_out_to_screen;

	/* Recall monster */
	describe_monster(r_idx, FALSE);

	/* Describe monster */
	roff_top(r_idx);
}




/*
 * Hack -- describe the given monster race in the current "term" window
 */
void display_roff(int r_idx)
{
	int y;

	/* Erase the window */
	for (y = 0; y < Term->hgt; y++)
	{
		/* Erase the line */
		Term_erase(0, y, 255);
	}

	/* Begin recall */
	Term_gotoxy(0, 1);

	/* Output to the screen */
	text_out_hook = text_out_to_screen;

	/* Recall monster */
	describe_monster(r_idx, FALSE);

	/* Describe monster */
	roff_top(r_idx);
}


/*
 * Add various player ghost attributes depending on race. -LM-
 */
static void process_ghost_race(int ghost_race, int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];
	byte n;
	cptr racename;
	int i;

	int dun_level = r_ptr->level;

	int race_middle = 0;
	/*nonsense high amount which will be lowered later*/
	int race_min = 25000;
	int race_max = 0;

	/*adjust hit points for the ghost class*/
	r_ptr->hdice = (p_info[ghost_race].r_mhp * r_ptr->hdice)/10;

	/*increase searching range based on ghost race searching and infravision*/
	r_ptr->aaf += (p_info[ghost_race].r_srh + p_info[ghost_race].r_fos) / 2 + p_info[ghost_race].infra;

	/*Add in some intrinsic race abilities*/

	if (p_info[ghost_race].flags2 & TR2_RES_LITE) r_ptr->flags3 &= ~(RF3_HURT_LITE);

	if (p_info[ghost_race].flags3 & TR3_FREE_ACT) r_ptr->flags3 |= (RF3_NO_CHARM);

	if (p_info[ghost_race].flags2 & TR2_RES_POIS) r_ptr->flags3 |= (RF3_IM_POIS);

	if (p_info[ghost_race].flags3 & TR3_REGEN) r_ptr->flags2 |= (RF2_REGENERATE);

	/*extract the ghost name*/
	racename = p_name + p_info[ghost_race].name;

	/*is it an orc name?*/
	if ((strstr(racename, "orc")) || (strstr(racename, "Orc")))
	{
		r_ptr->flags3 |= (RF3_ORC);
	}

	/*is it a troll name?*/
	if ((strstr(racename, "troll")) || (strstr(racename, "Troll")))
	{
		r_ptr->flags4 |= (RF4_BOULDER);
		r_ptr->flags3 |= (RF3_TROLL);

	}

	/*is it an elf name?*/
	if ((strstr(racename, "elf")) || (strstr(racename, "ELF")))
	{
		r_ptr->flags3 &= ~(RF3_HURT_LITE);
	}

	/*go through the races, get average, min, and max abilities for fighting*/
	for(i = 0; i < z_info->p_max; i++)
	{

		if (p_info[i].r_thn < race_min)
		{
			race_min = p_info[i].r_thn;
		}
		if (p_info[i].r_thn > race_max)
		{
			race_max = p_info[i].r_thn;
		}
		race_middle += p_info[i].r_thn;

	}

	/*get the average fighting ability*/
	race_middle =  (race_middle / (z_info->p_max - 1));

	/*
	 * Get "quartiles" for race fighting ability.
	 * This isn't quite statistically correct, but close enough
	 */
	race_max =  (race_middle + race_max) / 2;
	race_min =  (race_middle + race_min) / 2;

	/*top quartile gets extra fighting ability*/
	if(p_info[ghost_race].r_thn > race_max)
	{

		for (n = 0; n < MONSTER_BLOW_MAX; n++)
		{
			r_ptr->blow[n].d_side = 4 * r_ptr->blow[n].d_side / 3;
			r_ptr->blow[n].d_dice = 5 * r_ptr->blow[n].d_dice / 4;
		}

		r_ptr->ac += r_ptr->ac / 3;

	}

	/*bottom quartile gets less fighting ability*/
	else if(p_info[ghost_race].r_thn < race_min)
	{

		for (n = 0; n < MONSTER_BLOW_MAX; n++)
		{
			r_ptr->blow[n].d_side = 4 * r_ptr->blow[n].d_side / 5;
		}

		r_ptr->ac -= r_ptr->ac / 3;

	}

	/*re-set for bows*/
	race_middle = 0;
	/*nonsense high amount which will be lowered later*/
	race_min = 25000;
	race_max = 0;

	/*go through the races, get average, min, and max abilities for bow ability*/
	for(i = 0; i < z_info->p_max; i++)
	{

		if (p_info[i].r_thb < race_min)
		{
			race_min = p_info[i].r_thb;
		}
		if (p_info[i].r_thb > race_max)
		{
			race_max = p_info[i].r_thb;
		}
		race_middle += p_info[i].r_thb;

	}

	/*get the average bow ability*/
	race_middle =  (race_middle / (z_info->p_max - 1));

	/*
	 * Get "quartiles" for race bow ability.
	 * This isn't quite statistically correct, but close enough
	 */
	race_max =  (race_middle + race_max) / 2;
	race_min =  (race_middle + race_min) / 2;

	/*top quartile gets extra bow ability*/
	if(p_info[ghost_race].r_thb > race_max)
	{

		r_ptr->freq_ranged += ((100 - r_ptr->freq_ranged) / 6);
		r_ptr->spell_power += r_ptr->spell_power / 5;

		if (dun_level > 54)
			r_ptr->flags4 |= (RF4_PMISSL);
		else if (dun_level > 44)
			r_ptr->flags4 |= (RF4_BOLT);
		else if (dun_level > 34)
			r_ptr->flags4 |= (RF4_MISSL);
		else if (dun_level > 24)
			r_ptr->flags4 |= (RF4_ARROW);
		else r_ptr->flags4 |= (RF4_SHOT);

	}

	/*bottom quartile gets no bow ability*/
	else if(p_info[ghost_race].r_thb < race_min)
	{

		r_ptr->flags4 &= ~(RF4_ARCHERY_MASK);
	}

}

/*
 * Add various player ghost attributes depending on class. -LM-
 */
static void process_ghost_class(int ghost_class, int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];
	int dun_level = r_ptr->level;
	byte i, n;
	int class_middle = 0;
	/*nonsense high amount which will be lowered later*/
	int class_min = 25000;
	int class_max = 0;

	/*adjust hit points for the ghost class*/
	r_ptr->hdice += (c_info[ghost_class].c_mhp * r_ptr->hdice)/20;

	/*spellcasters get magic ability*/
	if (c_info[ghost_class].spell_book)
	{

		r_ptr->freq_ranged += (100 - r_ptr->freq_ranged) / 6;
		r_ptr->spell_power += r_ptr->spell_power / 5;
	}

	/*pure spellcasters get even more magic ability*/
	if (c_info[ghost_class].flags & CF_ZERO_FAIL)
	{
		r_ptr->freq_ranged += (100 - r_ptr->freq_ranged) / 5;
		r_ptr->spell_power += r_ptr->spell_power / 4;
		r_ptr->mana += r_ptr->mana / 2;
	}

	/* adjust for bravery flag*/
	if ((c_info[ghost_class].flags & CF_BRAVERY_30) && (dun_level > 20))
	{
		r_ptr->flags3 |= (RF3_NO_FEAR);
	}

	/*gives ghost rogues stealing ability and other thief abilities*/
	if (c_info[ghost_class].flags & CF_ROGUE_COMBAT)
	{

		if (r_ptr->blow[0].effect == RBE_HURT) r_ptr->blow[0].effect = RBE_EAT_GOLD;
		if (r_ptr->blow[1].effect == RBE_HURT) r_ptr->blow[1].effect = RBE_EAT_ITEM;
		if (dun_level > 50) r_ptr->flags7 |= (RF7_S_THIEF);

		r_ptr->aaf += r_ptr->aaf / 3;
	}

	/*set trap setters set traps*/
	if (c_info[ghost_class].flags & CF_SET_TRAPS) r_ptr->flags6 |= (RF6_TRAPS);

	/*get extra frequency with arrows*/
	if (c_info[ghost_class].flags & CF_EXTRA_ARROW)
	{

		if (dun_level > 30) r_ptr->freq_ranged += ((100 - r_ptr->freq_ranged) / 3);
		r_ptr->flags4 |= (RF4_ARROW);

	}
	/*get extra frequency with shots*/
	if (c_info[ghost_class].flags & CF_EXTRA_SHOT)
	{

		if (dun_level > 30) r_ptr->freq_ranged += ((100 - r_ptr->freq_ranged) / 3);
		r_ptr->flags4 |= (RF4_SHOT);
	}

	/*add the mage_spells*/
	if (c_info[ghost_class].spell_book == TV_MAGIC_BOOK)
	{
		byte level_adj = MAX(dun_level - c_info[ghost_class].spell_stat, 0);


		if (level_adj > 25) r_ptr->flags2 |= (RF2_LOW_MANA_RUN);
		if (level_adj > 35) r_ptr->flags3 |= (RF3_IM_ELEM);
		if (level_adj > 11) r_ptr->flags5 |= (RF5_BALL_POIS);
		if (level_adj > 65) r_ptr->flags6 |= (RF6_TELE_AWAY);
		if (level_adj > 55) r_ptr->flags6 |= (RF6_TELE_TO);
		if (level_adj > 5) r_ptr->flags6 |= (RF6_BLINK);
		if (level_adj > 50) r_ptr->flags6 |= (RF6_TELE_SELF_TO);
		if (dun_level > 35) r_ptr->flags6 |= (RF6_HASTE);


		if (c_info[ghost_class].flags & CF_ZERO_FAIL)
		{
			if (level_adj > 0) r_ptr->flags5 |= (RF5_BOLT_MANA);
			if (level_adj > 25) r_ptr->flags5 |= (RF5_BALL_FIRE);
			if (level_adj > 35) r_ptr->flags5 |= (RF5_BALL_COLD);
			if (level_adj > 55) r_ptr->flags5 |= (RF5_BALL_STORM);
			if (level_adj > 65) r_ptr->flags5 |= (RF5_BALL_MANA);
			if (level_adj > 75) r_ptr->flags6 |= (RF6_MIND_BLAST);
			if (level_adj > 85) r_ptr->flags6 |= (RF6_BRAIN_SMASH);
		}

	}

	if (c_info[ghost_class].spell_book == TV_PRAYER_BOOK)
	{
		byte level_adj = MAX(dun_level - c_info[ghost_class].spell_stat, 0);

		if (level_adj > 25) r_ptr->flags2 |= (RF2_LOW_MANA_RUN);
		if (level_adj > 11) r_ptr->flags5 |= (RF5_HOLY_ORB);
		if (level_adj > 5)  r_ptr->flags6 |= (RF6_HEAL);
		if (level_adj > 15) r_ptr->flags6 |= (RF6_BLINK);
		if (level_adj > 55) r_ptr->flags6 |= (RF6_TELE_SELF_TO);
		if (dun_level > 45) r_ptr->flags6 |= (RF6_HASTE);
		if (dun_level > 65) r_ptr->flags5 |= (RF5_BEAM_NETHR);
		if (dun_level > 10) r_ptr->flags3 |= (RF3_IM_FIRE |	RF3_IM_COLD);

		if (c_info[ghost_class].flags & CF_ZERO_FAIL)
		{
			r_ptr->spell_power += r_ptr->spell_power / 4;

			if (level_adj > 25) r_ptr->flags5 |= (RF5_BALL_FIRE);
			if (level_adj > 35) r_ptr->flags5 |= (RF5_BALL_COLD);
			if (level_adj > 55) r_ptr->flags5 |= (RF5_BALL_STORM);
			if (level_adj > 65) r_ptr->flags5 |= (RF5_BALL_MANA);
			if (level_adj > 45) r_ptr->flags6 |= (RF6_WOUND);
			if (level_adj > 65) r_ptr->flags7 |= (RF7_S_UNDEAD);
			if (level_adj > 80) r_ptr->flags7 |= (RF7_S_HI_UNDEAD);
			if (level_adj > 50) r_ptr->flags7 |= (RF7_S_WRAITH);
		}

	}

	/*go through the classes, get average, min, and max abilities for fighting*/
	for(i = 0; i < z_info->c_max; i++)
	{
		int total = c_info[i].c_thn + ((c_info[i].x_thn * dun_level) / 20);

		if (total < class_min)
		{
			class_min = total;
		}
		if (total > class_max)
		{
			class_max = total;
		}
		class_middle += total;

	}

	/*get the average fighting ability*/
	class_middle =  (class_middle / (z_info->c_max - 1));

	/*
	 * Get "quartiles" for class fighting ability.
	 * This isn't quite statistically correct, but close enough
	 */
	class_max =  (class_middle + class_max) / 2;
	class_min =  (class_middle + class_min) / 2;

	/*top quartile gets extra fighting ability*/
	if((c_info[ghost_class].c_thn + (c_info[ghost_class].x_thn * dun_level / 20)) > class_max)
	{

		for (n = 0; n < MONSTER_BLOW_MAX; n++)
		{
			r_ptr->blow[n].d_side = 4 * r_ptr->blow[n].d_side / 3;
			r_ptr->blow[n].d_dice = 5 * r_ptr->blow[n].d_dice / 4;
		}

		r_ptr->ac += r_ptr->ac / 3;

	}

	/*bottom quartile gets less fighting ability*/
	else if((c_info[ghost_class].c_thn + ((c_info[ghost_class].x_thn * dun_level) / 20)) < class_min)
	{

		for (n = 0; n < MONSTER_BLOW_MAX; n++)
		{
			r_ptr->blow[n].d_side = 2 * r_ptr->blow[n].d_side / 3;
		}

		r_ptr->ac -= r_ptr->ac / 3;

	}

	/*re-set for bows*/
	class_middle = 0;
	/*nonsense high amount which will be lowered later*/
	class_min = 25000;
	class_max = 0;

	/*go through the classes, get average, min, and max abilities for bow ability*/
	for(i = 0; i < z_info->c_max; i++)
	{
		int total = c_info[i].c_thb + ((c_info[i].x_thb * dun_level) / 20);

		if (total < class_min)
		{
			class_min = total;
		}
		if (total > class_max)
		{
			class_max = total;
		}
		class_middle += total;

	}

	/*get the average bow ability*/
	class_middle =  (class_middle / (z_info->c_max - 1));

	/*
	 * Get "quartiles" for class bow ability.
	 * This isn't quite statistically correct, but close enough
	 */
	class_max =  (class_middle + class_max) / 2;
	class_min =  (class_middle + class_min) / 2;

	/*top quartile gets extra bow ability*/
	if((c_info[ghost_class].c_thb + (c_info[ghost_class].x_thb * dun_level / 20)) > class_max)
	{

		r_ptr->freq_ranged += ((100 - r_ptr->freq_ranged) / 6);
		r_ptr->spell_power += r_ptr->spell_power / 5;

		if (dun_level > 54)
			r_ptr->flags4 |= (RF4_PMISSL);
		else if (dun_level > 44)
			r_ptr->flags4 |= (RF4_BOLT);
		else if (dun_level > 34)
			r_ptr->flags4 |= (RF4_MISSL);
		else if (dun_level > 24)
			r_ptr->flags4 |= (RF4_ARROW);
		else r_ptr->flags4 |= (RF4_SHOT);

	}

	/*bottom quartile gets no bow ability*/
	else if((c_info[ghost_class].c_thb + (c_info[ghost_class].x_thb * dun_level / 20)) < class_min)
	{
		r_ptr->flags4 &= ~(RF4_ARCHERY_MASK);
	}

}

/*
 * Once a monster with the flag "PLAYER_GHOST" is generated, it needs
 * to have a little color added, if it hasn't been prepared before.
 * This function uses a bones file to get a name, give the ghost a
 * gender, and add flags depending on the race and class of the slain
 * adventurer.  -LM-
 */
bool prepare_ghost(int r_idx, bool from_savefile)
{
	int		ghost_sex, ghost_race, ghost_class = 0;
	byte	try, i, backup_file_selector;
	bool prepare_new_template = FALSE;

	monster_race *r_ptr = &r_info[r_idx];
	monster_lore *l_ptr = &l_list[r_idx];

	FILE		*fp = FALSE;
	bool		err = FALSE;
	char		path[1024];

	/* Paranoia. */
	if (!(r_ptr->flags2 & (RF2_PLAYER_GHOST))) return (FALSE);
	if (adult_no_player_ghosts) return (FALSE);

	/* Hack -- If the ghost has a sex, then it must already have been prepared. */
	if ((r_ptr->flags1 & RF1_MALE) || (r_ptr->flags1 & RF1_FEMALE))
	{
		/* Hack - Ensure that ghosts are always "seen". */
		l_ptr->sights = 1;

		/* Nothing more to do. */
		return (TRUE);
	}

	/* Hack -- No easy player ghosts, unless the ghost is from a savefile.
	 * This also makes player ghosts much rarer, and effectively (almost)
	 * prevents more than one from being on a level.
	 * From 0.5.1, other code makes it is formally impossible to have more
	 * than one ghost at a time. -BR-
	 */
	if ((r_ptr->level < p_ptr->depth - 5) && (from_savefile == FALSE))
	{
		return (FALSE);
	}

	/* Store the index of the base race. */
	r_ghost = r_idx;

		/* Choose a bones file.  Use the variable bones_selector if it has any
	 * information in it (this allows saved ghosts to reacquire all special
	 * features), then use the current depth, and finally pick at random.
	 */
	for (try = 0; try < PLAYER_GHOST_TRIES_MAX + z_info->ghost_other_max; ++try)
	{
		/* Prepare a path, and store the file number for future reference. */
		if (try == 0)
		{
			if (bones_selector)
			{
				sprintf(path, "%s/bone.%03d", ANGBAND_DIR_BONE, bones_selector);
			}
			else
			{
				sprintf(path, "%s/bone.%03d", ANGBAND_DIR_BONE, p_ptr->depth);
				bones_selector = (byte)p_ptr->depth;
			}
		}
		else
		{
			if (try < PLAYER_GHOST_TRIES_MAX) backup_file_selector = randint(MAX_DEPTH - 1);
			else backup_file_selector = MAX_DEPTH + rand_int(z_info->ghost_other_max);
			sprintf(path, "%s/bone.%03d", ANGBAND_DIR_BONE, backup_file_selector);
			bones_selector = backup_file_selector;
		}

		/* Attempt to open the bones file. */
		fp = my_fopen(path, "r");

		/* No bones file with that number, try again. */
		if (!fp)
		{

			bones_selector = 0;

			/*try again*/
			continue;
		}

		/* Success. */
		if (fp) break;
	}

	/*function failed*/
	if (!fp) return (FALSE);

	/* XXX XXX XXX Scan the file */
	err = (fscanf(fp, "%[^\n]\n%d\n%d\n%d", ghost_name,
			&ghost_sex, &ghost_race, &ghost_class) != 4);

	/* Close the file */
	my_fclose(fp);

	/* Hack -- broken file */
	if (err)
	{
		bones_selector = 0;
		return (FALSE);
	}

	/*** Process the ghost name and store it in a global variable. ***/

	/* XXX XXX XXX Find the first comma, or end of string */
	for (i = 0; (i < 40) && (ghost_name[i]) && (ghost_name[i] != ','); i++);

	/* Terminate the name */
	ghost_name[i] = '\0';

	/* Force a name */
	if (!ghost_name[0]) strcpy(ghost_name, "Nobody");

	/* Capitalize the name */
	if (islower(ghost_name[0])) ghost_name[0] = toupper(ghost_name[0]);

	/*** Process sex. ***/

	/* Sanity check. */
	if ((ghost_sex >= MAX_SEXES) || (ghost_class < 0)) ghost_sex = rand_int(MAX_SEXES);

	/* And use that number to toggle on either the male or the female flag. */
	if (ghost_sex == 0) r_ptr->flags1 |= (RF1_FEMALE);
	if (ghost_sex == 1) r_ptr->flags1 |= (RF1_MALE);


	/*** Process race. ***/

	/* Sanity check. */
	if (ghost_race >= z_info->p_max)
	{
		prepare_new_template = TRUE;
		ghost_race = rand_int(z_info->p_max);
	}

	/* And use the ghost race to gain some flags. */
	process_ghost_race(ghost_race, r_idx);


	/*** Process class. ***/

	/* Sanity check. */
	if (ghost_class >= z_info->c_max)
	{
		prepare_new_template = TRUE;
		ghost_class = rand_int(z_info->c_max);
	}

	/* And use the ghost class to gain some flags. */
	process_ghost_class(ghost_class, r_idx);

	/* Hack -- increase the level feeling */
	rating += 10;

	/* A ghost makes the level special */
	good_item_flag = TRUE;

	/* Hack - Player ghosts are "seen" whenever generated, to conform with
	 * previous practice.
	 */
	l_ptr->sights = 1;

	/*
	 * If we used a random race/class, prepare a new template so the new
	 * ghost is consistent when the game is re-opened
	 */
	if (prepare_new_template)
	{
		/* Find a blank bones file*/
		for (try = 1; try < MAX_DEPTH; ++try)
		{
			/*make the path*/
			sprintf(path, "%s/bone.%03d", ANGBAND_DIR_BONE, try);

			/* Attempt to open the bones file. */
			fp = my_fopen(path, "r");

			/* Found a number to make a new bones file. */
			if (!fp)
			{
				/* Try to write a new "Bones File" */
				fp = my_fopen(path, "w");

				/*paranoia*/
				if (!fp) continue;

				/*use this number for the ghost*/
				bones_selector = try;

				/*now save the new file*/
				/* Save the info */
				fprintf(fp, "%s\n", ghost_name);
				fprintf(fp, "%d\n", ghost_sex);
				fprintf(fp, "%d\n", ghost_race);
				fprintf(fp, "%d\n", ghost_class);

				/*Mark end of file*/
				fprintf(fp, "\n");

				/* Close and save the Bones file */
				my_fclose(fp);

				/*done*/
				break;
			}

			/* This one is used, close it, and then continue*/
			/* Success. */
			else my_fclose(fp);
		}

	}

	/* Success */
	return (TRUE);
}
