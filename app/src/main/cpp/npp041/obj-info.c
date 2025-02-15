/* File: obj-info.c */

/*
 * Copyright (c) 2002 Andrew Sidwell, Robert Ruehlmann
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.  Other copyrights may also apply.
 */

#include "angband.h"

/* TRUE if a paragraph break should be output before next p_text_out() */
static bool new_paragraph = FALSE;


static void p_text_out(cptr str)
{
	if (new_paragraph)
	{
		text_out("\n\n   ");
		new_paragraph = FALSE;
	}

	text_out(str);
}

static void output_list(cptr list[], int n)
{
	int i;

	char *conjunction = "and ";
	if (n < 0)
	{
		n = -n;
		conjunction = "or ";
	}


	for (i = 0; i < n; i++)
	{
		if (i != 0)

		{
			p_text_out((i == 1 && i == n - 1) ? " " : ", ");
			if (i == n - 1)	p_text_out(conjunction);

		}
		p_text_out(list[i]);
	}
}


static void output_desc_list(cptr intro, cptr list[], int n)
{
	if (n != 0)
	{
		/* Output intro */
		p_text_out(intro);

		/* Output list */
		output_list(list, n);

		/* Output end */
		p_text_out(".  ");
	}
}


/*
 * Describe stat modifications.
 */
static bool describe_stats(const object_type *o_ptr, u32b f1)
{
	cptr descs[A_MAX];
	int cnt = 0;
	int pval = (o_ptr->pval > 0 ? o_ptr->pval : -o_ptr->pval);

	/* Abort if the pval is zero */
	if (!pval) return (FALSE);

	/* Collect stat bonuses */
	if (f1 & (TR1_STR)) descs[cnt++] = stat_names_full[A_STR];
	if (f1 & (TR1_INT)) descs[cnt++] = stat_names_full[A_INT];
	if (f1 & (TR1_WIS)) descs[cnt++] = stat_names_full[A_WIS];
	if (f1 & (TR1_DEX)) descs[cnt++] = stat_names_full[A_DEX];
	if (f1 & (TR1_CON)) descs[cnt++] = stat_names_full[A_CON];
	if (f1 & (TR1_CHR)) descs[cnt++] = stat_names_full[A_CHR];

	/* Skip */
	if (cnt == 0) return (FALSE);

	/* Shorten to "all stats", if appropriate. */
	if (cnt == A_MAX)
	{
		p_text_out(format("It %s all your stats", (o_ptr->pval > 0 ? "increases" : "decreases")));
	}
	else
	{
		p_text_out(format("It %s your ", (o_ptr->pval > 0 ? "increases" : "decreases")));

		/* Output list */
		output_list(descs, cnt);
	}

	/* Output end */
	p_text_out(format(" by %i.  ", pval));

	/* We found something */
	return (TRUE);
}


/*
 * Describe "secondary bonuses" of an item.
 */
static bool describe_secondary(const object_type *o_ptr, u32b f1)
{
	cptr descs[8];
	int cnt = 0;
	int pval = (o_ptr->pval > 0 ? o_ptr->pval : -o_ptr->pval);

	/* Collect */
	if (f1 & (TR1_STEALTH)) descs[cnt++] = "stealth";
	if (f1 & (TR1_SEARCH))  descs[cnt++] = "searching";
	if (f1 & (TR1_INFRA))   descs[cnt++] = "infravision";
	if (f1 & (TR1_TUNNEL))  descs[cnt++] = "tunneling";
	if (f1 & (TR1_SPEED))   descs[cnt++] = "speed";
	if (f1 & (TR1_BLOWS))   descs[cnt++] = "attack speed";
	if (f1 & (TR1_SHOTS))   descs[cnt++] = "shooting speed";
	if (f1 & (TR1_MIGHT))   descs[cnt++] = "shooting power";

	/* Skip */
	if (!cnt) return (FALSE);

	/* Start */
	p_text_out(format("It %s your ", (o_ptr->pval > 0 ? "increases" : "decreases")));

	/* Output list */
	output_list(descs, cnt);

	/* Output end */
	p_text_out(format(" by %i.  ", pval));

	/* We found something */
	return (TRUE);
}


/*
 * Describe the special slays and executes of an item.
 */
static bool describe_slay(const object_type *o_ptr, u32b f1)
{
	cptr slays[8], execs[3];
	int slcnt = 0, excnt = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect brands */
	if (f1 & (TR1_SLAY_ANIMAL)) slays[slcnt++] = "animals";
	if (f1 & (TR1_SLAY_ORC))    slays[slcnt++] = "orcs";
	if (f1 & (TR1_SLAY_TROLL))  slays[slcnt++] = "trolls";
	if (f1 & (TR1_SLAY_GIANT))  slays[slcnt++] = "giants";

	/* Dragon slay/execute */
	if (f1 & TR1_KILL_DRAGON)
		execs[excnt++] = "dragons";
	else if (f1 & TR1_SLAY_DRAGON)
		slays[slcnt++] = "dragons";

	/* Demon slay/execute */
	if (f1 & TR1_KILL_DEMON)
		execs[excnt++] = "demons";
	else if (f1 & TR1_SLAY_DEMON)
		slays[slcnt++] = "demons";

	/* Undead slay/execute */
	if (f1 & TR1_KILL_UNDEAD)
		execs[excnt++] = "undead";
	else if (f1 & TR1_SLAY_UNDEAD)
		slays[slcnt++] = "undead";

	if (f1 & (TR1_SLAY_EVIL)) slays[slcnt++] = "all evil creatures";

	/* Describe */
	if (slcnt)
	{
		/* Output intro */
		p_text_out("It slays ");

		/* Output list */
		output_list(slays, slcnt);

		/* Output end (if needed) */
		if (!excnt) p_text_out(".  ");
	}

	if (excnt)
	{
		/* Output intro */
		if (slcnt) p_text_out(", and it is especially deadly against ");
		else p_text_out("It is especially deadly against ");

		/* Output list */
		output_list(execs, excnt);

		/* Output end */
		p_text_out(".  ");
	}

	/* We are done here */
	return ((excnt || slcnt) ? TRUE : FALSE);
}


/*
 * Describe elemental brands.
 */
static bool describe_brand(const object_type *o_ptr, u32b f1)
{
	cptr descs[5];
	int cnt = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect brands */
	if (f1 & (TR1_BRAND_ACID)) descs[cnt++] = "acid";
	if (f1 & (TR1_BRAND_ELEC)) descs[cnt++] = "electricity";
	if (f1 & (TR1_BRAND_FIRE)) descs[cnt++] = "fire";
	if (f1 & (TR1_BRAND_COLD)) descs[cnt++] = "frost";
	if (f1 & (TR1_BRAND_POIS)) descs[cnt++] = "poison";

	/* Describe brands */
	output_desc_list("It is branded with ", descs, cnt);

	/* We are done here */
	return (cnt ? TRUE : FALSE);
}


/*
 * Describe immunities granted by an object.
 *
 * ToDo - Merge intro describe_resist() below.
 */
static bool describe_immune(const object_type *o_ptr, u32b f2)
{
	cptr descs[5];
	int cnt = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect immunities */
	if (f2 & (TR2_IM_ACID)) descs[cnt++] = "acid";
	if (f2 & (TR2_IM_ELEC)) descs[cnt++] = "lightning";
	if (f2 & (TR2_IM_FIRE)) descs[cnt++] = "fire";
	if (f2 & (TR2_IM_COLD)) descs[cnt++] = "cold";
	if (f2 & (TR2_IM_POIS)) descs[cnt++] = "poison";

	/* Describe immunities */
	output_desc_list("It provides immunity to ", descs, cnt);

	/* We are done here */
	return (cnt ? TRUE : FALSE);
}


/*
 * Describe resistances granted by an object.
 */
static bool describe_resist(const object_type *o_ptr, u32b f2, u32b f3)
{
	cptr vp[17];
	int vn = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect resistances */
	if ((f2 & (TR2_RES_ACID)) && !(f2 & (TR2_IM_ACID)))
		vp[vn++] = "acid";
	if ((f2 & (TR2_RES_ELEC)) && !(f2 & (TR2_IM_ELEC)))
		vp[vn++] = "lightning";
	if ((f2 & (TR2_RES_FIRE)) && !(f2 & (TR2_IM_FIRE)))
		vp[vn++] = "fire";
	if ((f2 & (TR2_RES_COLD)) && !(f2 & (TR2_IM_COLD)))
		vp[vn++] = "cold";
	if ((f2 & (TR2_RES_POIS)) && !(f2 & (TR2_IM_POIS)))
		vp[vn++] = "poison";

	if (f2 & (TR2_RES_FEAR))  vp[vn++] = "fear";
	if (f2 & (TR2_RES_LITE))  vp[vn++] = "light";
	if (f2 & (TR2_RES_DARK))  vp[vn++] = "dark";
	if (f2 & (TR2_RES_BLIND)) vp[vn++] = "blindness";
	if (f2 & (TR2_RES_CONFU)) vp[vn++] = "confusion";
	if (f2 & (TR2_RES_SOUND)) vp[vn++] = "sound";
	if (f2 & (TR2_RES_SHARD)) vp[vn++] = "shards";
	if (f2 & (TR2_RES_NEXUS)) vp[vn++] = "nexus" ;
	if (f2 & (TR2_RES_NETHR)) vp[vn++] = "nether";
	if (f2 & (TR2_RES_CHAOS)) vp[vn++] = "chaos";
	if (f2 & (TR2_RES_DISEN)) vp[vn++] = "disenchantment";
	if (f3 & (TR3_HOLD_LIFE)) vp[vn++] = "life draining";

	/* Describe resistances */
	output_desc_list("It provides resistance to ", vp, vn);

	/* We are done here */
	return (vn ? TRUE : FALSE);
}



/*return the number of blows a player gets with a weapon*/
int get_num_blows(const object_type *o_ptr, u32b f1)
{

	int i, str_index, dex_index, blows, div_weight;

	int str = 0;
	int dex = 0;
	int str_add = 0;
	int dex_add = 0;

	/* Scan the equipment */
	for (i = INVEN_WIELD; i < INVEN_TOTAL; i++)
	{

		u32b wf1, wf2, wf3;
		object_type *i_ptr = &inventory[i];

		/* Hack -- do not apply wielded "weapon" bonuses */
		if (i == INVEN_WIELD) continue;

		/* Skip non-objects */
		if (!i_ptr->k_idx) continue;

		/* Extract the item flags */
		object_flags_known(i_ptr, &wf1, &wf2, &wf3);

		/* Affect stats */
		if (wf1 & (TR1_STR)) str_add += i_ptr->pval;
		if (wf1 & (TR1_DEX)) dex_add += i_ptr->pval;
	}

	/* add in the strength of the examined weapon*/
	if (f1 & (TR1_STR)) str_add += o_ptr->pval;

	/* add in the dex of the examined weapon*/
	if (f1 & (TR1_DEX)) dex_add += o_ptr->pval;

	/* Calculate stats */
	for (i = 0; i < A_MAX; i++)
	{
		int add, use, ind;

		/*stat modifiers from equipment later*/
		if 		(i == A_STR) add = str_add;
		else if (i == A_DEX) add = dex_add;
		/*only do dex and strength*/
		else continue;

		/* Maximize mode */
		if (adult_maximize)
		{
			/* Modify the stats for race/class */
			add += (rp_ptr->r_adj[i] + cp_ptr->c_adj[i]);
		}

		/* Extract the new "stat_use" value for the stat */
		use = modify_stat_value(p_ptr->stat_cur[i], add);

		/* Values: 3, 4, ..., 17 */
		if (use <= 18) ind = (use - 3);

		/* Ranges: 18/00-18/09, ..., 18/210-18/219 */
		else if (use <= 18+219) ind = (15 + (use - 18) / 10);

		/* Range: 18/220+ */
		else ind = (37);

		/*record the values*/
		if (i == A_STR) str = ind;
		else dex = ind;
	}

	/* Enforce a minimum "weight" (tenth pounds) */
	div_weight = ((o_ptr->weight < cp_ptr->min_weight) ? cp_ptr->min_weight : o_ptr->weight);

	/* Get the strength vs weight */
	str_index = (adj_str_blow[str] * cp_ptr->att_multiply / div_weight);

	/* Maximal value */
	if (str_index > 11) str_index = 11;
	if (str_index < 0) str_index = 0;

	/* Index by dexterity */
	dex_index = (adj_dex_blow[dex]);

	/* Maximal value */
	if (dex_index > 11) dex_index = 11;
	if (dex_index < 0) dex_index = 0;

	/* Use the blows table */
	blows = blows_table[str_index][dex_index];

	/* Maximal value */
	if (blows > cp_ptr->max_attacks) blows = cp_ptr->max_attacks;

	/* Add in the "bonus blows"*/
	if (f1 & (TR1_BLOWS)) blows += o_ptr->pval;

	/* Require at least one blow */
	if (blows < 1) blows = 1;

	/*add extra attack for those who have the flag*/
	if ((p_ptr->lev > 25) && (cp_ptr->flags & CF_EXTRA_ATTACK))
		blows += 1;

	return(blows);
}

/*
 * Describe 'number of attacks recieved with a weapon
 */
static bool describe_attacks (const object_type *o_ptr, u32b f1)
{
	int n = o_ptr->tval;

	if ((n == TV_DIGGING) || (n == TV_HAFTED) ||
		(n == TV_POLEARM) || (n == TV_SWORD))
	{
		/*get the number of blows*/
		n = get_num_blows(o_ptr, f1);

		/*print out the number of attacks*/
		if (n == 1) p_text_out("It gives you one attack per turn.  ");
		else p_text_out(format("It gives you %d attacks per turn.  ", n));

		return (TRUE);
	}

	return (FALSE);
}



/*
 * Describe 'ignores' of an object.
 */
static bool describe_ignores(const object_type *o_ptr, u32b f3)
{
	cptr list[4];
	int n = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect the ignores */
	if (f3 & (TR3_IGNORE_ACID)) list[n++] = "acid";
	if (f3 & (TR3_IGNORE_ELEC)) list[n++] = "electricity";
	if (f3 & (TR3_IGNORE_FIRE)) list[n++] = "fire";
	if (f3 & (TR3_IGNORE_COLD)) list[n++] = "cold";

	/* Describe ignores */
	if (n == 4)
		p_text_out("It cannot be harmed by the elements.  ");
	else
		output_desc_list("It cannot be harmed by ", list, - n);

	return ((n > 0) ? TRUE : FALSE);
}


/*
 * Describe stat sustains.
 */
static bool describe_sustains(const object_type *o_ptr, u32b f2)
{
	cptr list[A_MAX];
	int n = 0;

	/* Unused parameter */
	(void)o_ptr;

	/* Collect the sustains */
	if (f2 & (TR2_SUST_STR)) list[n++] = stat_names_full[A_STR];
	if (f2 & (TR2_SUST_INT)) list[n++] = stat_names_full[A_INT];
	if (f2 & (TR2_SUST_WIS)) list[n++] = stat_names_full[A_WIS];
	if (f2 & (TR2_SUST_DEX)) list[n++] = stat_names_full[A_DEX];
	if (f2 & (TR2_SUST_CON)) list[n++] = stat_names_full[A_CON];
	if (f2 & (TR2_SUST_CHR)) list[n++] = stat_names_full[A_CHR];

	/* Describe immunities */
	if (n == A_MAX)
		p_text_out("It sustains all your stats.  ");
	else
		output_desc_list("It sustains your ", list, n);

	/* We are done here */
	return (n ? TRUE : FALSE);
}


/*
 * Describe miscellaneous powers such as see invisible, free action,
 * permanent light, etc; also note curses and penalties.
 */
static bool describe_misc_magic(const object_type *o_ptr, u32b f3)
{
	cptr good[7], bad[4];
	int gc = 0, bc = 0;
	bool something = FALSE;

	/* Throwing weapons. */
	if (f3 & (TR3_THROWING))
	{
		if (o_ptr->ident & IDENT_PERFECT_BALANCE)
		{
			good[gc++] = ("can be thrown hard and fast");
		}
		else good[gc++] = ("can be thrown effectively");
	}

	/* Collect stuff which can't be categorized */
	if (f3 & (TR3_BLESSED))     good[gc++] = "is blessed by the gods";
	if (f3 & (TR3_IMPACT))      good[gc++] = "creates earthquakes on impact";
	if (f3 & (TR3_SLOW_DIGEST)) good[gc++] = "slows your metabolism";
	if (f3 & (TR3_FEATHER))     good[gc++] = "makes you fall like a feather";
	if (((o_ptr->tval == TV_LITE) && artifact_p(o_ptr)) || (f3 & (TR3_LITE)))
		good[gc++] = "lights the dungeon around you";
	if (f3 & (TR3_REGEN))       good[gc++] = "speeds your regeneration";

	/* Describe */
	output_desc_list("It ", good, gc);

	/* Set "something" */
	if (gc) something = TRUE;

	/* Collect granted powers */
	gc = 0;
	if (f3 & (TR3_FREE_ACT))  good[gc++] = "immunity to paralysis";
	if (f3 & (TR3_TELEPATHY)) good[gc++] = "the power of telepathy";
	if (f3 & (TR3_SEE_INVIS)) good[gc++] = "the ability to see invisible things";

	/* Collect penalties */
	if (f3 & (TR3_AGGRAVATE)) bad[bc++] = "aggravates creatures around you";
	if (f3 & (TR3_DRAIN_EXP)) bad[bc++] = "drains experience";
	if (f3 & (TR3_TELEPORT))  bad[bc++] = "induces random teleportation";

	/* Deal with cursed stuff */
	if (cursed_p(o_ptr))
	{
		if (f3 & (TR3_PERMA_CURSE)) bad[bc++] = "is permanently cursed";
		else if (f3 & (TR3_HEAVY_CURSE)) bad[bc++] = "is heavily cursed";
		else if (object_known_p(o_ptr)) bad[bc++] = "is cursed";
	}

	/* Describe */
	if (gc)
	{
		/* Output intro */
		p_text_out("It grants you ");

		/* Output list */
		output_list(good, gc);

		/* Output end (if needed) */
		if (!bc) p_text_out(".  ");
	}

	if (bc)
	{
		/* Output intro */
		if (gc) p_text_out(", but it also ");
		else p_text_out("It ");

		/* Output list */
		output_list(bad, bc);

		/* Output end */
		p_text_out(".  ");
	}

	/* Set "something" */
	if (gc || bc) something = TRUE;

	/* Return "something" */
	return (something);
}


static cptr act_description[ACT_MAX] =
{
	"illumination",
	"magic mapping",
	"clairvoyance",
	"protection from evil",
	"dispel evil (x5)",
	"heal (500)",
	"heal (1000)",
	"cure wounds (4d7)",
	"haste self (20+d20 turns)",
	"haste self (75+d75 turns)",
	"fire bolt (9d8)",
	"fire ball (72)",
	"large fire ball (120)",
	"frost bolt (6d8)",
	"frost ball (48)",
	"frost ball (100)",
	"frost bolt (12d8)",
	"large frost ball (200)",
	"acid bolt (5d8)",
	"recharge item I",
	"sleep II",
	"lightning bolt (4d8)",
	"large lightning ball (250)",
	"banishment",
	"mass banishment",
	"*identify*",
	"drain life (90)",
	"drain life (120)",
	"bizarre things",
	"star ball (150)",
	"berserk rage, bless, and resistance",
	"phase door",
	"door and trap destruction",
	"detection",
	"resistance (20+d20 turns)",
	"teleport",
	"restore life levels",
	"magic missile (2d6)",
	"a magical arrow (150)",
	"remove fear and cure poison",
	"stinking cloud (12)",
	"stone to mud",
	"teleport away",
	"word of recall",
	"confuse monster",
	"probing",
	"fire branding of bolts",
	"starlight (10d8)",
	"mana bolt (12d8)",
	"berserk rage (50+d50 turns)",
	"resist acid (20+d20 turns)",
	"resist electricity (20+d20 turns)",
	"resist fire (20+d20 turns)",
	"resist cold (20+d20 turns)",
	"resist poison (20+d20 turns)"
};

/*
 * Determine the "Activation" (if any) for an artifact
 */
static void describe_item_activation(const object_type *o_ptr, char *random_name, size_t max)
{
	u32b f1, f2, f3;

	u16b value;

	/* Extract the flags */
	object_flags(o_ptr, &f1, &f2, &f3);

	/* Require activation ability */
	if (!(f3 & TR3_ACTIVATE)) return;

	/* Artifact activations */
	if ((o_ptr->name1) && (o_ptr->name1 < z_info->art_norm_max))
	{
		artifact_type *a_ptr = &a_info[o_ptr->name1];

		bool drag_armor = FALSE;

		/* Paranoia */
		if (a_ptr->activation >= ACT_MAX)
		{
			if ((a_ptr->tval == TV_DRAG_ARMOR) ||
				(a_ptr->tval == TV_DRAG_SHIELD))
			     drag_armor = TRUE;
			else return;
		}

		/* Some artifacts can be activated */
		if (!drag_armor)
		{
			my_strcat(random_name, act_description[a_ptr->activation], max);

			/* Output the number of turns */
			if (a_ptr->time && a_ptr->randtime)
				my_strcat(random_name, format(" every %d+d%d turns", a_ptr->time, a_ptr->randtime), max);
			else if (a_ptr->time)
				my_strcat(random_name, format(" every %d turns", a_ptr->time), max);
			else if (a_ptr->randtime)
				my_strcat(random_name, format(" every d%d turns", a_ptr->randtime), max);

			return;
		}
	}

	/* Now do the rings */
	if (o_ptr->tval == TV_RING)
	{
		/* Branch on the sub-type */
		switch (o_ptr->sval)
		{
			case SV_RING_ACID:
			{
				my_strcat(random_name, "acid resistance (20+d20 turns) and acid ball (70) every 50+d50 turns", max);
				break;
			}
			case SV_RING_FLAMES:
			{
				my_strcat(random_name, "fire resistance (20+d20 turns) and fire ball (80) every 50+d50 turns", max);
				break;
			}
			case SV_RING_ICE:
			{
				my_strcat(random_name, "cold resistance (20+d20 turns) and cold ball (75) every 50+d50 turns", max);
				break;
			}

			case SV_RING_LIGHTNING:
			{
				my_strcat(random_name, "electricity resistance (20+d20 turns) and electricity ball (85) every 50+d50 turns", max);
				break;
			}
		}

		return;
	}

	/* Require dragon scale mail */
	if ((o_ptr->tval != TV_DRAG_ARMOR) &&
		(o_ptr->tval != TV_DRAG_SHIELD)) return;

	/*Bigger the dragon scale mail, the bigger the damage & re-charge*/
	value = o_ptr->sval;

	/*Armor is more powerful than shields*/
	if (o_ptr->tval == TV_DRAG_ARMOR) value *= 2;

	/* Branch on the sub-type */
	switch (o_ptr->name2)
	{

		case EGO_DRAGON_BLUE:
		{
			value *= 50;

			my_strcat(random_name, format("electricity resistance (10+d10 turns) and breathe lightning (%d) every %d+d%d turns", value, value, value), max);

			break;
		}
		case EGO_DRAGON_WHITE:
		{
			value *= 50;

			my_strcat(random_name, format("cold resistance (10+d10 turns) and breathe frost (%d) every %d+d%d turns", value, value, value), max);

			break;
		}
		case EGO_DRAGON_BLACK:
		{
			value *= 50;

			my_strcat(random_name, format("acid resistance (10+d10 turns) and breathe acid (%d) every %d+d%d turns", value, value, value), max);

			break;
		}
		case EGO_DRAGON_GREEN:
		{
			value *= 50;

			my_strcat(random_name, format("poison resistance (10+d10 turns) and breathe poison gas (%d) every %d+d%d turns", value, value, value), max);

			break;
		}
		case EGO_DRAGON_RED:
		{
			value *= 50;

			my_strcat(random_name, format("fire resistance (10+d10 turns) and breathe fire (%d) every %d+d%d turns", value, value, value), max);

			break;
		}
		case EGO_DRAGON_MULTIHUED:
		{
			value *= 75;

			my_strcat(random_name, format("resistance (20+d20 turns) and breathe multi-hued (%d) every %d+d%d turns", value,
 							(value * 3 / 4), (value * 3 / 4)), max);

			break;
		}
		case EGO_DRAGON_BRONZE:
		{
			value *= 50;

			my_strcat(random_name, format("breathe confusion (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_GOLD:
		{
			value *= 50;

			my_strcat(random_name, format("breathe sound (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_CHAOS:
		{
			value *= 60;

			my_strcat(random_name, format("breathe chaos/disenchant (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_LAW:
		{
			value *= 60;

			my_strcat(random_name, format("breathe sound/shards (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_BALANCE:
		{
			value *= 75;

			my_strcat(random_name, format("breathe balance (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_PSEUDO:
		{
			value *= 65;

			my_strcat(random_name, format("breathe light/darkness (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		case EGO_DRAGON_POWER:
		{
			value *= 100;

			my_strcat(random_name, format("breathe the elements (%d) every %d+d%d turns", value, value, value), max);
			break;
		}
		default:
		{
			break;
		}
	}
}



/*
 * Describe an object's activation, if any.
 */
static bool describe_activation(const object_type *o_ptr, u32b f3)
{
	/* Check for the activation flag */
	if (f3 & TR3_ACTIVATE)
	{
		char act_desc[120];

		u16b size;

		my_strcpy(act_desc, "It activates for ", sizeof(act_desc));

		/*get the size of the file*/
		size = strlen(act_desc);

		describe_item_activation(o_ptr, act_desc, sizeof(act_desc));

		/*if the previous function added length, we have an activation, so print it out*/
		if (strlen(act_desc) > size)
		{

			my_strcat(act_desc, format(".  "), sizeof(act_desc));

			/*print it out*/
			p_text_out(act_desc);

			return (TRUE);
		}
	}

	/* No activation */
	return (FALSE);
}


/*
 * Output object information
 */
bool object_info_out(const object_type *o_ptr)
{
	u32b f1, f2, f3;
	bool something = FALSE;

	/* Grab the object flags */
	object_info_out_flags(o_ptr, &f1, &f2, &f3);

	/* Describe the object */
	if (describe_stats(o_ptr, f1)) something = TRUE;
	if (describe_secondary(o_ptr, f1)) something = TRUE;
	if (describe_slay(o_ptr, f1)) something = TRUE;
	if (describe_brand(o_ptr, f1)) something = TRUE;
	if (describe_immune(o_ptr, f2)) something = TRUE;
	if (describe_resist(o_ptr, f2, f3)) something = TRUE;
	if (describe_sustains(o_ptr, f2)) something = TRUE;
	if (describe_misc_magic(o_ptr, f3)) something = TRUE;
	if (describe_activation(o_ptr, f3)) something = TRUE;
	if (describe_ignores(o_ptr, f3)) something = TRUE;
	if (describe_attacks(o_ptr, f1)) something = TRUE;



	/* Unknown extra powers (artifact) */
	if (object_known_p(o_ptr) && (!(o_ptr->ident & IDENT_MENTAL)) &&
	    (object_has_hidden_powers(o_ptr) || artifact_p(o_ptr)))
	{
		/* Hack -- Put this in a separate paragraph if screen dump */
		if (something && text_out_hook == text_out_to_screen)
			new_paragraph = TRUE;

		p_text_out("It might have hidden powers.");
 		something = TRUE;

	}

	/* We are done. */
	return something;
}


/*
 * Header for additional information when printing to screen.
 *
 * Header for additional information when printing to screen.
 */
static bool screen_out_head(const object_type *o_ptr)
{
	char *o_name;
	int name_size = Term->wid;

	bool has_description = FALSE;

	/* Allocate memory to the size of the screen */
	o_name = C_RNEW(name_size, char);

	/* Description */
	object_desc(o_name, name_size, o_ptr, TRUE, 3);

	/* Print, in colour */
	text_out_c(TERM_YELLOW, format("%^s", o_name));

	/* Free up the memory */
	FREE(o_name);

	/* Display the known artifact description */
	if (!adult_rand_artifacts && o_ptr->name1 &&
	    object_known_p(o_ptr) && a_info[o_ptr->name1].text)

	{
		p_text_out("\n\n   ");
		p_text_out(a_text + a_info[o_ptr->name1].text);
		has_description = TRUE;
	}
	/* Display the known object description */
	else if (object_aware_p(o_ptr) || object_known_p(o_ptr))
	{
		if (k_info[o_ptr->k_idx].text)
		{
			p_text_out("\n\n   ");
			p_text_out(k_text + k_info[o_ptr->k_idx].text);
			has_description = TRUE;
		}

		/* Display an additional ego-item description */
		if (o_ptr->name2 && object_known_p(o_ptr) && e_info[o_ptr->name2].text)
		{
			p_text_out("\n\n   ");
			p_text_out(e_text + e_info[o_ptr->name2].text);
			has_description = TRUE;
		}
	}

	return (has_description);
}


/*
 * Place an item description on the screen.
 */
void object_info_screen(const object_type *o_ptr)
{
	bool has_description, has_info;

	/* Redirect output to the screen */
	text_out_hook = text_out_to_screen;

	/* Save the screen */
	screen_save();

	has_description = screen_out_head(o_ptr);

	object_info_out_flags = object_flags_known;

	/* Dump the info */
	new_paragraph = TRUE;
	has_info = object_info_out(o_ptr);
	new_paragraph = FALSE;

	if (!object_known_p(o_ptr))
	{
		p_text_out("\n\n   This item has not been identified.");
	}
	else if ((!has_description) && (!has_info) && (o_ptr->tval != cp_ptr->spell_book))
	{
		p_text_out("\n\n   This item does not seem to possess any special abilities.");
	}

	if (o_ptr->tval != cp_ptr->spell_book)
	{
		text_out_c(TERM_L_BLUE, "\n\n[Press any key to continue]\n");

		/* Wait for input */
		(void)inkey();
	}

	/* Load the screen */
	screen_load();

	/* Hack -- Browse book, then prompt for a command */
	if (o_ptr->tval == cp_ptr->spell_book)
	{
		/* Call the aux function */
		do_cmd_browse_aux(o_ptr);
	}

	return;
}

