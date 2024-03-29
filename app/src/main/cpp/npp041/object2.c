
/* File: object2.c */

/*
 * Copyright (c) 1997 Ben Harrison, James E. Wilson, Robert A. Koeneke
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.  Other copyrights may also apply.
 */

#include "angband.h"


/*
 * Excise a dungeon object from any stacks
 */
void excise_object_idx(int o_idx)
{
	object_type *j_ptr;

	s16b this_o_idx, next_o_idx = 0;

	s16b prev_o_idx = 0;


	/* Object */
	j_ptr = &o_list[o_idx];

	/* Monster */
	if (j_ptr->held_m_idx)
	{
		monster_type *m_ptr;

		/* Monster */
		m_ptr = &mon_list[j_ptr->held_m_idx];

		/* Scan all objects in the grid */
		for (this_o_idx = m_ptr->hold_o_idx; this_o_idx; this_o_idx = next_o_idx)
		{
			object_type *o_ptr;

			/* Get the object */
			o_ptr = &o_list[this_o_idx];

			/* Get the next object */
			next_o_idx = o_ptr->next_o_idx;

			/* Done */
			if (this_o_idx == o_idx)
			{
				/* No previous */
				if (prev_o_idx == 0)
				{
					/* Remove from list */
					m_ptr->hold_o_idx = next_o_idx;
				}

				/* Real previous */
				else
				{
					object_type *i_ptr;

					/* Previous object */
					i_ptr = &o_list[prev_o_idx];

					/* Remove from list */
					i_ptr->next_o_idx = next_o_idx;
				}

				/* Forget next pointer */
				o_ptr->next_o_idx = 0;

				/* Done */
				break;
			}

			/* Save prev_o_idx */
			prev_o_idx = this_o_idx;
		}
	}

	/* Dungeon */
	else
	{
		int y = j_ptr->iy;
		int x = j_ptr->ix;

		/* Scan all objects in the grid */
		for (this_o_idx = cave_o_idx[y][x]; this_o_idx; this_o_idx = next_o_idx)
		{
			object_type *o_ptr;

			/* Get the object */
			o_ptr = &o_list[this_o_idx];

			/* Get the next object */
			next_o_idx = o_ptr->next_o_idx;

			/* Done */
			if (this_o_idx == o_idx)
			{
				/* No previous */
				if (prev_o_idx == 0)
				{
					/* Remove from list */
					cave_o_idx[y][x] = next_o_idx;
				}

				/* Real previous */
				else
				{
					object_type *i_ptr;

					/* Previous object */
					i_ptr = &o_list[prev_o_idx];

					/* Remove from list */
					i_ptr->next_o_idx = next_o_idx;
				}

				/* Forget next pointer */
				o_ptr->next_o_idx = 0;

				/* Done */
				break;
			}

			/* Save prev_o_idx */
			prev_o_idx = this_o_idx;
		}
	}
}


/*
 * Delete a dungeon object
 *
 * Handle "stacks" of objects correctly.
 */
void delete_object_idx(int o_idx)
{
	object_type *j_ptr;

	/* Excise */
	excise_object_idx(o_idx);

	/* Object */
	j_ptr = &o_list[o_idx];

	/* Dungeon floor */
	if (!(j_ptr->held_m_idx))
	{
		int y, x;

		/* Location */
		y = j_ptr->iy;
		x = j_ptr->ix;

		/* Visual update */
		lite_spot(y, x);
	}

	/* Wipe the object */
	object_wipe(j_ptr);

	/* Count objects */
	o_cnt--;
}


/*
 * Deletes all objects at given location
 */
void delete_object(int y, int x)
{
	s16b this_o_idx, next_o_idx = 0;


	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Scan all objects in the grid */
	for (this_o_idx = cave_o_idx[y][x]; this_o_idx; this_o_idx = next_o_idx)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[this_o_idx];

		/* Get the next object */
		next_o_idx = o_ptr->next_o_idx;

		/* Wipe the object */
		object_wipe(o_ptr);

		/* Count objects */
		o_cnt--;
	}

	/* Objects are gone */
	cave_o_idx[y][x] = 0;

	/* Visual update */
	lite_spot(y, x);
}



/*
 * Move an object from index i1 to index i2 in the object list
 */
static void compact_objects_aux(int i1, int i2)
{
	int i;

	object_type *o_ptr;


	/* Do nothing */
	if (i1 == i2) return;


	/* Repair objects */
	for (i = 1; i < o_max; i++)
	{
		/* Get the object */
		o_ptr = &o_list[i];

		/* Skip "dead" objects */
		if (!o_ptr->k_idx) continue;

		/* Repair "next" pointers */
		if (o_ptr->next_o_idx == i1)
		{
			/* Repair */
			o_ptr->next_o_idx = i2;
		}
	}


	/* Get the object */
	o_ptr = &o_list[i1];


	/* Monster */
	if (o_ptr->held_m_idx)
	{
		monster_type *m_ptr;

		/* Get the monster */
		m_ptr = &mon_list[o_ptr->held_m_idx];

		/* Repair monster */
		if (m_ptr->hold_o_idx == i1)
		{
			/* Repair */
			m_ptr->hold_o_idx = i2;
		}
	}

	/* Dungeon */
	else
	{
		int y, x;

		/* Get location */
		y = o_ptr->iy;
		x = o_ptr->ix;

		/* Repair grid */
		if (cave_o_idx[y][x] == i1)
		{
			/* Repair */
			cave_o_idx[y][x] = i2;
		}
	}


	/* Hack -- move object */
	COPY(&o_list[i2], &o_list[i1], object_type);

	/* Hack -- wipe hole */
	object_wipe(o_ptr);
}


/*
 * Compact and Reorder the object list
 *
 * This function can be very dangerous, use with caution!
 *
 * When actually "compacting" objects, we base the saving throw on a
 * combination of object level, distance from player, and current
 * "desperation".
 *
 * After "compacting" (if needed), we "reorder" the objects into a more
 * compact order, and we reset the allocation info, and the "live" array.
 */
void compact_objects(int size)
{
	int py = p_ptr->py;
	int px = p_ptr->px;

	int i, y, x, num, cnt;

	int cur_lev, cur_dis, chance;


	/* Compact */
	if (size)
	{
		/* Message */
		msg_print("Compacting objects...");

		/* Redraw map */
		p_ptr->redraw |= (PR_MAP);

		/* Window stuff */
		p_ptr->window |= (PW_OVERHEAD);
	}


	/* Compact at least 'size' objects */
	for (num = 0, cnt = 1; num < size; cnt++)
	{
		/* Get more vicious each iteration */
		cur_lev = 5 * cnt;

		/* Get closer each iteration */
		cur_dis = 5 * (20 - cnt);

		/* Examine the objects */
		for (i = 1; i < o_max; i++)
		{
			object_type *o_ptr = &o_list[i];

			object_kind *k_ptr = &k_info[o_ptr->k_idx];

			/* Skip dead objects */
			if (!o_ptr->k_idx) continue;

			/* Hack -- High level objects start out "immune" */
			if ((k_ptr->level > cur_lev) && (k_ptr->squelch != SQUELCH_ALWAYS)) continue;

			/* Monster */
			if (o_ptr->held_m_idx)
			{
				monster_type *m_ptr;

				/* Get the monster */
				m_ptr = &mon_list[o_ptr->held_m_idx];

				/* Get the location */
				y = m_ptr->fy;
				x = m_ptr->fx;

				/* Monsters protect their objects */
				if ((rand_int(100) < 90) && (k_ptr->squelch != SQUELCH_ALWAYS)) continue;
			}

			/* Dungeon */
			else
			{
				/* Get the location */
				y = o_ptr->iy;
				x = o_ptr->ix;
			}

			/* Nearby objects start out "immune" */
			if ((cur_dis > 0) && (distance(py, px, y, x) < cur_dis) &&
				(k_ptr->squelch != SQUELCH_ALWAYS)) continue;

			/* Saving throw */
			chance = 90;

			/* Squelched items get compacted */
			if ((k_ptr->aware) && (k_ptr->squelch == SQUELCH_ALWAYS)) chance = 0;

 			/* Hack -- only compact artifacts in emergencies */
			if (artifact_p(o_ptr) && (cnt < 1000)) chance = 100;

			/* Apply the saving throw */
			if (rand_int(100) < chance) continue;

			/* Delete the object */
			delete_object_idx(i);

			/* Count it */
			num++;
		}
	}


	/* Excise dead objects (backwards!) */
	for (i = o_max - 1; i >= 1; i--)
	{
		object_type *o_ptr = &o_list[i];

		/* Skip real objects */
		if (o_ptr->k_idx) continue;

		/* Move last object into open hole */
		compact_objects_aux(o_max - 1, i);

		/* Compress "o_max" */
		o_max--;
	}
}




/*
 * Delete all the items when player leaves the level
 *
 * Note -- we do NOT visually reflect these (irrelevant) changes
 *
 * Hack -- we clear the "cave_o_idx[y][x]" field for every grid,
 * and the "m_ptr->next_o_idx" field for every monster, since
 * we know we are clearing every object.  Technically, we only
 * clear those fields for grids/monsters containing objects,
 * and we clear it once for every such object.
 */
void wipe_o_list(void)
{
	int i;

	/* Delete the existing objects */
	for (i = 1; i < o_max; i++)
	{
		object_type *o_ptr = &o_list[i];

		/* Skip dead objects */
		if (!o_ptr->k_idx) continue;

		/* Mega-Hack -- preserve artifacts */
		if (!character_dungeon || adult_preserve)
		{
			/* Hack -- Preserve unknown artifacts */
			if (artifact_p(o_ptr) && !object_known_p(o_ptr))
			{
				/* Mega-Hack -- Preserve the artifact */
				a_info[o_ptr->name1].cur_num = 0;
			}
		}

		/* Monster */
		if (o_ptr->held_m_idx)
		{
			monster_type *m_ptr;

			/* Monster */
			m_ptr = &mon_list[o_ptr->held_m_idx];

			/* Hack -- see above */
			m_ptr->hold_o_idx = 0;
		}

		/* Dungeon */
		else
		{
			/* Get the location */
			int y = o_ptr->iy;
			int x = o_ptr->ix;

			/* Hack -- see above */
			cave_o_idx[y][x] = 0;
		}

		/*Wipe the randart if necessary*/
		if (o_ptr->name1) artifact_wipe(o_ptr->name1, FALSE);

		/* Wipe the object */
		(void)WIPE(o_ptr, object_type);
	}

	/* Reset "o_max" */
	o_max = 1;

	/* Reset "o_cnt" */
	o_cnt = 0;
}


/*
 * Get and return the index of a "free" object.
 *
 * This routine should almost never fail, but in case it does,
 * we must be sure to handle "failure" of this routine.
 */
s16b o_pop(void)
{
	int i;


	/* Initial allocation */
	if (o_max < z_info->o_max)
	{
		/* Get next space */
		i = o_max;

		/* Expand object array */
		o_max++;

		/* Count objects */
		o_cnt++;

		/* Use this object */
		return (i);
	}


	/* Recycle dead objects */
	for (i = 1; i < o_max; i++)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[i];

		/* Skip live objects */
		if (o_ptr->k_idx) continue;

		/* Count objects */
		o_cnt++;

		/* Use this object */
		return (i);
	}


	/* Warn the player (except during dungeon creation) */
	if (character_dungeon) msg_print("Too many objects!");

	/* Oops */
	return (0);
}


/*
 * Get the first object at a dungeon location
 * or NULL if there isn't one.
 */
object_type* get_first_object(int y, int x)
{
	s16b o_idx = cave_o_idx[y][x];

	if (o_idx) return (&o_list[o_idx]);

	/* No object */
	return (NULL);
}


/*
 * Get the next object in a stack or
 * NULL if there isn't one.
 */
object_type* get_next_object(const object_type *o_ptr)
{
	if (o_ptr->next_o_idx) return (&o_list[o_ptr->next_o_idx]);

	/* No more objects */
	return (NULL);
}


/*
 * Apply a "object restriction function" to the "object allocation table"
 */
errr get_obj_num_prep(void)
{
	int i;

	/* Get the entry */
	alloc_entry *table = alloc_kind_table;

	/* Scan the allocation table */
	for (i = 0; i < alloc_kind_size; i++)
	{
		/* Accept objects which pass the restriction, if any */
		if (!get_obj_num_hook || (*get_obj_num_hook)(table[i].index))
		{
			/* Accept this object */
			table[i].prob2 = table[i].prob1;
		}

		/* Do not use this object */
		else
		{
			/* Decline this object */
			table[i].prob2 = 0;
		}
	}

	/* Success */
	return (0);
}



/*
 * Choose an object kind that seems "appropriate" to the given level
 *
 * This function uses the "prob2" field of the "object allocation table",
 * and various local information, to calculate the "prob3" field of the
 * same table, which is then used to choose an "appropriate" object, in
 * a relatively efficient manner.
 *
 * It is (slightly) more likely to acquire an object of the given level
 * than one of a lower level.  This is done by choosing several objects
 * appropriate to the given level and keeping the "hardest" one.
 *
 * Note that if no objects are "appropriate", then this function will
 * fail, and return zero, but this should *almost* never happen.
 * (but it does happen with certain themed items occasionally). -JG
 */
s16b get_obj_num(int level)
{
	int i, j, p;

	int k_idx;

	long value, total;

	object_kind *k_ptr;

	alloc_entry *table = alloc_kind_table;

	/* Boost level */
	if (level > 0)
	{
		/* Occasional "boost" */
		if (one_in_(GREAT_OBJ))
		{
			/* What a bizarre calculation */
			level = 1 + (level * MAX_DEPTH    / randint(MAX_DEPTH));
		}
	}

	/* Reset total */
	total = 0L;

	/* Process probabilities */
	for (i = 0; i < alloc_kind_size; i++)
	{
		/* Objects are sorted by depth */
		if (table[i].level > level) break;

		/* Default */
		table[i].prob3 = 0;

		/* Get the index */
		k_idx = table[i].index;

		/* Get the actual kind */
		k_ptr = &k_info[k_idx];

		/* Hack -- prevent embedded chests, but allow them for quests*/
		if ((object_generation_mode == OB_GEN_MODE_CHEST)
			    && (k_ptr->tval == TV_CHEST)) continue;

		/* Accept */
		table[i].prob3 = table[i].prob2;

		/* Total */
		total += table[i].prob3;
	}

	/* No legal objects */
	if (total <= 0) return (0);

	/* Pick an object */
	value = rand_int(total);

	/* Find the object */
	for (i = 0; i < alloc_kind_size; i++)
	{
		/* Found the entry */
		if (value < table[i].prob3) break;

		/* Decrement */
		value = value - table[i].prob3;
	}


	/* Power boost */
	p = rand_int(100);

	/* Try for a "better" object once (50%) or twice (10%) */
	if (p < 60)
	{
		/* Save old */
		j = i;

		/* Pick a object */
		value = rand_int(total);

		/* Find the monster */
		for (i = 0; i < alloc_kind_size; i++)
		{
			/* Found the entry */
			if (value < table[i].prob3) break;

			/* Decrement */
			value = value - table[i].prob3;
		}

		/* Keep the "best" one */
		if (table[i].level < table[j].level) i = j;
	}

	/* Try for a "better" object twice (10%) */
	if (p < 10)
	{
		/* Save old */
		j = i;

		/* Pick a object */
		value = rand_int(total);

		/* Find the object */
		for (i = 0; i < alloc_kind_size; i++)
		{
			/* Found the entry */
			if (value < table[i].prob3) break;

			/* Decrement */
			value = value - table[i].prob3;
		}

		/* Keep the "best" one */
		if (table[i].level < table[j].level) i = j;
	}


	/* Result */
	return (table[i].index);
}


/*
 * Known is true when the "attributes" of an object are "known".
 *
 * These attributes include tohit, todam, toac, cost, and pval (charges).
 *
 * Note that "knowing" an object gives you everything that an "awareness"
 * gives you, and much more.  In fact, the player is always "aware" of any
 * item which he "knows", except items in stores.
 *
 * But having full knowledge of, say, one "wand of wonder", does not, by
 * itself, give you knowledge, or even awareness, of other "wands of wonder".
 * It happens that most "identify" routines (including "buying from a shop")
 * will make the player "aware" of the object as well as "know" it.
 *
 * This routine also removes any inscriptions generated by "feelings".
 */
void object_known(object_type *o_ptr)
{
	/* Remove special inscription, if any */
	if (o_ptr->discount >= INSCRIP_NULL) o_ptr->discount = 0;

	/* The object is not "sensed" */
	o_ptr->ident &= ~(IDENT_SENSE);

	/* Clear the "Empty" info */
	o_ptr->ident &= ~(IDENT_EMPTY);

	/* Now we know about the item */
	o_ptr->ident |= (IDENT_KNOWN);
}



/*
 * The player is now aware of the effects of the given object.
 */
void object_aware(object_type *o_ptr)
{
	int x, y;
	bool flag = k_info[o_ptr->k_idx].aware;

 	/* Fully aware of the effects */
 	k_info[o_ptr->k_idx].aware = TRUE;

	/* If newly aware and squelched, must rearrange stacks */
	if ((!flag) && (k_info[o_ptr->k_idx].squelch == SQUELCH_ALWAYS))
	{
		for (x = 0; x < p_ptr->cur_map_wid; x++)
		{
			for (y = 0; y < p_ptr->cur_map_hgt; y++)
			{
				rearrange_stack(y, x);
			}
		}
	}
}



/*
 * Something has been "sampled"
 */
void object_tried(object_type *o_ptr)
{
	/* Mark it as tried (even if "aware") */
	k_info[o_ptr->k_idx].tried = TRUE;
}


/*
 * Determine if a weapon is 'blessed'
 */
bool is_blessed(const object_type *o_ptr)
{
	u32b f1, f2, f3;

	/* Get the flags */
	object_flags(o_ptr, &f1, &f2, &f3);

	/* Is the object blessed? */
	return ((f3 & TR3_BLESSED) ? TRUE : FALSE);
}



/*
 * Return the "value" of an "unknown" item
 * Make a guess at the value of non-aware items
 */
static s32b object_value_base(const object_type *o_ptr)
{
	object_kind *k_ptr = &k_info[o_ptr->k_idx];

	/* Use template cost for aware objects */
	if (object_aware_p(o_ptr)) return (k_ptr->cost);

	/* Analyze the type */
	switch (o_ptr->tval)
	{
		/* Un-aware Food */
		case TV_FOOD: return (5L);

		/* Un-aware Potions */
		case TV_POTION: return (20L);

		/* Un-aware Scrolls */
		case TV_SCROLL: return (20L);

		/* Un-aware Staffs */
		case TV_STAFF: return (70L);

		/* Un-aware Wands */
		case TV_WAND: return (50L);

		/* Un-aware Rods */
		case TV_ROD: return (90L);

		/* Un-aware Rings */
		case TV_RING: return (45L);

		/* Un-aware Amulets */
		case TV_AMULET: return (45L);
	}

	/* Paranoia -- Oops */
	return (0L);
}


/*
 * Return the "real" price of a "known" item, not including discounts.
 *
 * Wand and staffs get cost for each charge.
 *
 * Armor is worth an extra 100 gold per bonus point to armor class.
 *
 * Weapons are worth an extra 100 gold per bonus point (AC,TH,TD).
 *
 * Missiles are only worth 5 gold per bonus point, since they
 * usually appear in groups of 20, and we want the player to get
 * the same amount of cash for any "equivalent" item.  Note that
 * missiles never have any of the "pval" flags, and in fact, they
 * only have a few of the available flags, primarily of the "slay"
 * and "brand" and "ignore" variety.
 *
 * Weapons with negative hit+damage bonuses are worthless.
 *
 * Every wearable item with a "pval" bonus is worth extra (see below).
 */
static s32b object_value_real(const object_type *o_ptr)
{
	s32b value;

	u32b f1, f2, f3;

	object_kind *k_ptr = &k_info[o_ptr->k_idx];

	/* Hack -- "worthless" items */
	if (!k_ptr->cost) return (0L);

	/* Base cost */
	value = k_ptr->cost;

	/* Extract some flags */
	object_flags(o_ptr, &f1, &f2, &f3);

	/* Artifact */
	if (o_ptr->name1)
	{
		artifact_type *a_ptr = &a_info[o_ptr->name1];

		/* Hack -- "worthless" artifacts */
		if (!a_ptr->cost) return (0L);

		/* Hack -- Use the artifact cost instead */
		value = a_ptr->cost;
	}

	/* Ego-Item */
	else if (o_ptr->name2)
	{
		ego_item_type *e_ptr = &e_info[o_ptr->name2];

		/* Hack -- "worthless" ego-items */
		if (!e_ptr->cost) return (0L);

		/* Hack -- Reward the ego-item with a bonus */
		value += e_ptr->cost;
	}


	/* Analyze pval bonus */
	switch (o_ptr->tval)
	{
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		case TV_BOW:
		case TV_DIGGING:
		case TV_HAFTED:
		case TV_POLEARM:
		case TV_SWORD:
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_HELM:
		case TV_CROWN:
		case TV_SHIELD:
		case TV_CLOAK:
		case TV_SOFT_ARMOR:
		case TV_HARD_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		case TV_LITE:
		case TV_AMULET:
		case TV_RING:
		{
			/* Hack -- Negative "pval" is always bad */
			if (o_ptr->pval < 0) return (0L);

			/* No pval */
			if (!o_ptr->pval) break;

			/* Give credit for stat bonuses */
			if (f1 & (TR1_STR)) value += (o_ptr->pval * 200L);
			if (f1 & (TR1_INT)) value += (o_ptr->pval * 200L);
			if (f1 & (TR1_WIS)) value += (o_ptr->pval * 200L);
			if (f1 & (TR1_DEX)) value += (o_ptr->pval * 200L);
			if (f1 & (TR1_CON)) value += (o_ptr->pval * 200L);
			if (f1 & (TR1_CHR)) value += (o_ptr->pval * 200L);

			/* Give credit for stealth and searching */
			if (f1 & (TR1_STEALTH)) value += (o_ptr->pval * 100L);
			if (f1 & (TR1_SEARCH)) value += (o_ptr->pval * 100L);

			/* Give credit for infra-vision and tunneling */
			if (f1 & (TR1_INFRA)) value += (o_ptr->pval * 50L);
			if (f1 & (TR1_TUNNEL)) value += (o_ptr->pval * 50L);

			/* Give credit for perfect balance. */
			if (o_ptr->ident & IDENT_PERFECT_BALANCE) value += o_ptr->dd * 200L;

			/* Give credit for extra attacks */
			if (f1 & (TR1_BLOWS)) value += (o_ptr->pval * 2000L);

			/* Give credit for speed bonus */
			if (f1 & (TR1_SPEED)) value += (o_ptr->pval * 30000L);

			break;
		}
	}


	/* Analyze the item */
	switch (o_ptr->tval)
	{
		/* Wands/Staffs */
		case TV_WAND:
		case TV_STAFF:
		{
			/* Pay extra for charges, depending on standard number of
			 * charges.  Handle new-style wands correctly.
			 */
			value += ((value / 20) * (o_ptr->pval / o_ptr->number));

			/* Done */
			break;
		}

		/* Rings/Amulets */
		case TV_RING:
		case TV_AMULET:
		{
			/* Hack -- negative bonuses are bad */
			if (o_ptr->to_a < 0) return (0L);
			if (o_ptr->to_h < 0) return (0L);
			if (o_ptr->to_d < 0) return (0L);

			/* Give credit for bonuses */
			value += ((o_ptr->to_h + o_ptr->to_d + o_ptr->to_a) * 100L);

			/* Done */
			break;
		}

		/* Armor */
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_CLOAK:
		case TV_CROWN:
		case TV_HELM:
		case TV_SHIELD:
		case TV_SOFT_ARMOR:
		case TV_HARD_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		{
			/* Give credit for hit bonus */
			value += ((o_ptr->to_h - k_ptr->to_h) * 100L);

			/* Give credit for damage bonus */
			value += ((o_ptr->to_d - k_ptr->to_d) * 100L);

			/* Give credit for armor bonus */
			value += (o_ptr->to_a * 100L);

			/* Done */
			break;
		}

		/* Bows/Weapons */
		case TV_BOW:
		case TV_DIGGING:
		case TV_HAFTED:
		case TV_SWORD:
		case TV_POLEARM:
		{
			/* Hack -- negative hit/damage bonuses */
			if (o_ptr->to_h + o_ptr->to_d < 0) return (0L);

			/* Factor in the bonuses */
			value += ((o_ptr->to_h + o_ptr->to_d + o_ptr->to_a) * 100L);

			/* Hack -- Factor in extra damage dice */
			if ((o_ptr->dd > k_ptr->dd) && (o_ptr->ds == k_ptr->ds))
			{
				value += (o_ptr->dd - k_ptr->dd) * o_ptr->ds * 100L;
			}

			/* Done */
			break;
		}

		/* Ammo */
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		{
			/* Hack -- negative hit/damage bonuses */
			if (o_ptr->to_h + o_ptr->to_d < 0) return (0L);

			/* Factor in the bonuses */
			value += ((o_ptr->to_h + o_ptr->to_d) * 5L);

			/* Hack -- Factor in extra damage dice */
			if ((o_ptr->dd > k_ptr->dd) && (o_ptr->ds == k_ptr->ds))
			{
				value += (o_ptr->dd - k_ptr->dd) * o_ptr->ds * 5L;
			}

			/* Done */
			break;
		}
	}

	/* No negative value */
	if (value < 0) value = 0;

	/* Return the value */
	return (value);
}


/*
 * Return the price of an item including plusses (and charges).
 *
 * This function returns the "value" of the given item (qty one).
 *
 * Never notice "unknown" bonuses or properties, including "curses",
 * since that would give the player information he did not have.
 *
 * Note that discounted items stay discounted forever.
 */
s32b object_value(const object_type *o_ptr)
{
	s32b value;


	/* Unknown items -- acquire a base value */
	if (object_known_p(o_ptr))
	{
		/* Broken items -- worthless */
		if (broken_p(o_ptr)) return (0L);

		/* Cursed items -- worthless */
		if (cursed_p(o_ptr)) return (0L);

		/* Real value (see above) */
		value = object_value_real(o_ptr);
	}

	/* Known items -- acquire the actual value */
	else
	{
		/* Hack -- Felt broken items */
		if ((o_ptr->ident & (IDENT_SENSE)) && broken_p(o_ptr)) return (0L);

		/* Hack -- Felt cursed items */
		if ((o_ptr->ident & (IDENT_SENSE)) && cursed_p(o_ptr)) return (0L);

		/* Base value (see above) */
		value = object_value_base(o_ptr);
	}


	/* Apply discount (if any) */
	if (o_ptr->discount > 0 && o_ptr->discount < INSCRIP_NULL)
	{
		value -= (value * o_ptr->discount / 100L);
	}


	/* Return the final value */
	return (value);
}





/*
 * Determine if an item can "absorb" a second item
 *
 * See "object_absorb()" for the actual "absorption" code.
 *
 * If permitted, we allow wands/staffs (if they are known to have equal
 * charges) and rods (if fully charged) to combine.  They will unstack
 * (if necessary) when they are used.
 *
 * If permitted, we allow weapons/armor to stack, if fully "known".
 *
 * Missiles will combine if both stacks have the same "known" status.
 * This is done to make unidentified stacks of missiles useful.
 *
 * Food, potions, scrolls, and "easy know" items always stack.
 *
 * Chests, and activatable items, except rods, never stack (for various reasons).
 */
bool object_similar(const object_type *o_ptr, const object_type *j_ptr)
{
	int total = o_ptr->number + j_ptr->number;

	/* Require identical object types */
	if (o_ptr->k_idx != j_ptr->k_idx) return (0);

	/* Analyze the items */
	switch (o_ptr->tval)
	{
		/* Chests */
		case TV_CHEST:
		{
			/* Never okay */
			return (0);
		}

		/* Food and Potions and Scrolls */
		case TV_FOOD:
		case TV_POTION:
		case TV_SCROLL:
		{
			/* Assume okay */
			break;
		}

		/* Staves and wands*/
		case TV_STAFF:
		case TV_WAND:
		{
			/* Require either knowledge or known empty for both wands and staffs. */
			if ((!(o_ptr->ident & (IDENT_EMPTY)) &&
				!object_known_p(o_ptr)) ||
				(!(j_ptr->ident & (IDENT_EMPTY)) &&
				!object_known_p(j_ptr))) return(0);

			/* Wand/Staffs charges combine in NPPangband.  */

			/* Assume okay */
			break;

		}

		/* Staffs and Wands and Rods */
		case TV_ROD:
		{

			/* Assume okay */
			break;
		}

		/* Weapons and Armor */
		case TV_BOW:
		case TV_DIGGING:
		case TV_HAFTED:
		case TV_POLEARM:
		case TV_SWORD:
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_HELM:
		case TV_CROWN:
		case TV_SHIELD:
		case TV_CLOAK:
		case TV_SOFT_ARMOR:
		case TV_HARD_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		{
			/* Fall through */
		}

		/* Rings, Amulets, Lites and Books */
		case TV_RING:
		case TV_AMULET:
		case TV_LITE:
		case TV_MAGIC_BOOK:
		case TV_PRAYER_BOOK:
		{
			/* Require both items to be known */
			if (!object_known_p(o_ptr) || !object_known_p(j_ptr)) return (0);

			/* Fall through */
		}

		/* Missiles */
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			/* Require identical knowledge of both items */
			if (object_known_p(o_ptr) != object_known_p(j_ptr)) return (0);

			/* Require identical "bonuses" */
			if (o_ptr->to_h != j_ptr->to_h) return (FALSE);
			if (o_ptr->to_d != j_ptr->to_d) return (FALSE);
			if (o_ptr->to_a != j_ptr->to_a) return (FALSE);

			/* Require identical "pval" code */
			if (o_ptr->pval != j_ptr->pval) return (FALSE);

			/* Require identical "artifact" names */
			if (o_ptr->name1 != j_ptr->name1) return (FALSE);

			/* Require identical "ego-item" names */
			if (o_ptr->name2 != j_ptr->name2) return (FALSE);

			/* Hack -- Never stack "powerful" items */
			if (o_ptr->xtra1 || j_ptr->xtra1) return (0);

			/* Mega-Hack -- Handle lites */
			if (fuelable_lite_p(o_ptr))
			{
				if (o_ptr->timeout != j_ptr->timeout) return (0);
			}

			/* Hack -- Never stack recharging items */
			else if (o_ptr->timeout || j_ptr->timeout) return (FALSE);

			/* Require identical "values" */
			if (o_ptr->ac != j_ptr->ac) return (FALSE);
			if (o_ptr->dd != j_ptr->dd) return (FALSE);
			if (o_ptr->ds != j_ptr->ds) return (FALSE);


			/*Allow well balanced items to stack only with other
			 *well balanced items*/
			if ((o_ptr->ident & IDENT_PERFECT_BALANCE) !=
			    (j_ptr->ident & IDENT_PERFECT_BALANCE)) return (FALSE);

			/* Probably okay */
			break;
		}

		/* Various */
		default:
		{
			/* Require knowledge */
			if (!object_known_p(o_ptr) || !object_known_p(j_ptr)) return (0);

			/* Probably okay */
			break;
		}
	}


	/* Hack -- Require identical "cursed" and "broken" status */
	if (((o_ptr->ident & (IDENT_CURSED)) != (j_ptr->ident & (IDENT_CURSED))) ||
	    ((o_ptr->ident & (IDENT_BROKEN)) != (j_ptr->ident & (IDENT_BROKEN))))
	{
		return (0);
	}


	/* Hack -- Require compatible inscriptions */
	if (o_ptr->obj_note != j_ptr->obj_note)
	{
		/* Normally require matching inscriptions */
		if (!stack_force_notes) return (0);

		/* Never combine different inscriptions */
		if (o_ptr->obj_note && j_ptr->obj_note) return (0);
	}


	/* Hack -- Require compatible "discount" fields */
	if (o_ptr->discount != j_ptr->discount)
	{
		/* Both are (different) special inscriptions */
		if ((o_ptr->discount >= INSCRIP_NULL) &&
		    (j_ptr->discount >= INSCRIP_NULL))
		{
			/* Normally require matching inscriptions */
			return (0);
		}

		/* One is a special inscription, one is a discount or nothing */
		else if ((o_ptr->discount >= INSCRIP_NULL) ||
		         (j_ptr->discount >= INSCRIP_NULL))
		{
			/* Normally require matching inscriptions */
			if (!stack_force_notes) return (0);

			/* Hack -- Never merge a special inscription with a discount */
			if ((o_ptr->discount > 0) && (j_ptr->discount > 0)) return (0);
		}

		/* One is a discount, one is a (different) discount or nothing */
		else
		{
			/* Normally require matching discounts */
			if (!stack_force_costs) return (0);
		}
	}


	/* Maximal "stacking" limit */
	if (total >= MAX_STACK_SIZE) return (0);


	/* They match, so they must be similar */
	return (TRUE);
}


/*
 * Allow one item to "absorb" another, assuming they are similar.
 *
 * The blending of the "note" field assumes that either (1) one has an
 * inscription and the other does not, or (2) neither has an inscription.
 * In both these cases, we can simply use the existing note, unless the
 * blending object has a note, in which case we use that note.
 *
 * The blending of the "discount" field assumes that either (1) one is a
 * special inscription and one is nothing, or (2) one is a discount and
 * one is a smaller discount, or (3) one is a discount and one is nothing,
 * or (4) both are nothing.  In all of these cases, we can simply use the
 * "maximum" of the two "discount" fields.
 *
 * These assumptions are enforced by the "object_similar()" code.
 */
void object_absorb(object_type *o_ptr, const object_type *j_ptr)
{
	object_kind *k_ptr = &k_info[o_ptr->k_idx];

	int total = o_ptr->number + j_ptr->number;

	/* Add together the item counts */
	o_ptr->number = ((total < MAX_STACK_SIZE) ? total : (MAX_STACK_SIZE - 1));

	/* Hack -- Blend "known" status */
	if (object_known_p(j_ptr)) object_known(o_ptr);

	/* Hack -- Blend store status */
	if (j_ptr->ident & (IDENT_STORE)) o_ptr->ident |= (IDENT_STORE);

	/* Hack -- Blend "mental" status */
	if (j_ptr->ident & (IDENT_MENTAL)) o_ptr->ident |= (IDENT_MENTAL);

	/* Hack -- Blend "notes" */
	if (j_ptr->obj_note != 0) o_ptr->obj_note = j_ptr->obj_note;

	/* Mega-Hack -- Blend "discounts" */
	if (o_ptr->discount < j_ptr->discount) o_ptr->discount = j_ptr->discount;

	/* Hack -- if rods are stacking, re-calculate the
	 * pvals (maximum timeouts) and current timeouts together.
	 */
	if (o_ptr->tval == TV_ROD)
	{
		o_ptr->pval = total * k_ptr->pval;
		o_ptr->timeout += j_ptr->timeout;
	}

	/* Hack -- if wands or staffsare stacking, combine the charges. */
	if ((o_ptr->tval == TV_WAND) || (o_ptr->tval == TV_STAFF))
	{
		o_ptr->pval += j_ptr->pval;
	}

}



/*
 * Find the index of the object_kind with the given tval and sval
 */
s16b lookup_kind(int tval, int sval)
{
	int k;

	/* Look for it */
	for (k = 1; k < z_info->k_max; k++)
	{
		object_kind *k_ptr = &k_info[k];

		/* Found a match */
		if ((k_ptr->tval == tval) && (k_ptr->sval == sval)) return (k);
	}

	/* Oops */
	msg_format("No object (%d,%d)", tval, sval);

	/* Oops */
	return (0);
}


/*
 * Wipe an object clean.
 */
void object_wipe(object_type *o_ptr)
{
	/* Wipe the structure */
	(void)WIPE(o_ptr, object_type);
}


/*
 * Prepare an object based on an existing object
 */
void object_copy(object_type *o_ptr, const object_type *j_ptr)
{
	/* Copy the structure */
	COPY(o_ptr, j_ptr, object_type);
}


/*
 * Prepare an object based on an object kind.
 */
void object_prep(object_type *o_ptr, int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Clear the record */
	(void)WIPE(o_ptr, object_type);

	/* Save the kind index */
	o_ptr->k_idx = k_idx;

	/* Efficiency -- tval/sval */
	o_ptr->tval = k_ptr->tval;
	o_ptr->sval = k_ptr->sval;

	/* Default "pval" */
	o_ptr->pval = k_ptr->pval;

	/* Default number */
	o_ptr->number = 1;

	/* Default weight */
	o_ptr->weight = k_ptr->weight;

	/* Default magic */
	o_ptr->to_h = k_ptr->to_h;
	o_ptr->to_d = k_ptr->to_d;
	o_ptr->to_a = k_ptr->to_a;

	/* Default power */
	o_ptr->ac = k_ptr->ac;
	o_ptr->dd = k_ptr->dd;
	o_ptr->ds = k_ptr->ds;

	/* Hack -- worthless items are always "broken" */
	if (k_ptr->cost <= 0) o_ptr->ident |= (IDENT_BROKEN);

	/* Hack -- cursed items are always "cursed" */
	if (k_ptr->flags3 & (TR3_LIGHT_CURSE)) o_ptr->ident |= (IDENT_CURSED);

	/* Hack -- extract the perfect_balance flag */
	if (k_ptr->flags3 & (TR3_PERFECT_BALANCE)) o_ptr->ident |= (IDENT_PERFECT_BALANCE);
}


/*
 * Help determine an "enchantment bonus" for an object.
 *
 * To avoid floating point but still provide a smooth distribution of bonuses,
 * we simply round the results of division in such a way as to "average" the
 * correct floating point value.
 *
 * This function has been changed.  It uses "Rand_normal()" to choose values
 * from a normal distribution, whose mean moves from zero towards the max as
 * the level increases, and whose standard deviation is equal to 1/4 of the
 * max, and whose values are forced to lie between zero and the max, inclusive.
 *
 * Since the "level" rarely passes 100 before Morgoth is dead, it is very
 * rare to get the "full" enchantment on an object, even a deep levels.
 *
 * It is always possible (albeit unlikely) to get the "full" enchantment.
 *
 * A sample distribution of values from "m_bonus(10, N)" is shown below:
 *
 *   N       0     1     2     3     4     5     6     7     8     9    10
 * ---    ----  ----  ----  ----  ----  ----  ----  ----  ----  ----  ----
 *   0   66.37 13.01  9.73  5.47  2.89  1.31  0.72  0.26  0.12  0.09  0.03
 *   8   46.85 24.66 12.13  8.13  4.20  2.30  1.05  0.36  0.19  0.08  0.05
 *  16   30.12 27.62 18.52 10.52  6.34  3.52  1.95  0.90  0.31  0.15  0.05
 *  24   22.44 15.62 30.14 12.92  8.55  5.30  2.39  1.63  0.62  0.28  0.11
 *  32   16.23 11.43 23.01 22.31 11.19  7.18  4.46  2.13  1.20  0.45  0.41
 *  40   10.76  8.91 12.80 29.51 16.00  9.69  5.90  3.43  1.47  0.88  0.65
 *  48    7.28  6.81 10.51 18.27 27.57 11.76  7.85  4.99  2.80  1.22  0.94
 *  56    4.41  4.73  8.52 11.96 24.94 19.78 11.06  7.18  3.68  1.96  1.78
 *  64    2.81  3.07  5.65  9.17 13.01 31.57 13.70  9.30  6.04  3.04  2.64
 *  72    1.87  1.99  3.68  7.15 10.56 20.24 25.78 12.17  7.52  4.42  4.62
 *  80    1.02  1.23  2.78  4.75  8.37 12.04 27.61 18.07 10.28  6.52  7.33
 *  88    0.70  0.57  1.56  3.12  6.34 10.06 15.76 30.46 12.58  8.47 10.38
 *  96    0.27  0.60  1.25  2.28  4.30  7.60 10.77 22.52 22.51 11.37 16.53
 * 104    0.22  0.42  0.77  1.36  2.62  5.33  8.93 13.05 29.54 15.23 22.53
 * 112    0.15  0.20  0.56  0.87  2.00  3.83  6.86 10.06 17.89 27.31 30.27
 * 120    0.03  0.11  0.31  0.46  1.31  2.48  4.60  7.78 11.67 25.53 45.72
 * 128    0.02  0.01  0.13  0.33  0.83  1.41  3.24  6.17  9.57 14.22 64.07
 */
static s16b m_bonus(int max, int level)
{
	int bonus, stand, extra, value;


	/* Paranoia -- enforce maximal "level" */
	if (level > MAX_DEPTH - 1) level = MAX_DEPTH - 1;


	/* The "bonus" moves towards the max */
	bonus = ((max * level) / MAX_DEPTH);

	/* Hack -- determine fraction of error */
	extra = ((max * level) % MAX_DEPTH);

	/* Hack -- simulate floating point computations */
	if (rand_int(MAX_DEPTH) < extra) bonus++;

	/* The "stand" is equal to one quarter of the max */
	stand = (max / 4);

	/* Hack -- determine fraction of error */
	extra = (max % 4);

	/* Hack -- simulate floating point computations */
	if (rand_int(4) < extra) stand++;


	/* Choose an "interesting" value */
	value = Rand_normal(bonus, stand);

	/* Enforce the minimum value */
	if (value < 0) return (0);

	/* Enforce the maximum value */
	if (value > max) return (max);

	/* Result */
	return (value);
}




/*
 * Cheat -- describe a created object for the user
 */
static void object_mention(const object_type *o_ptr)
{
	char o_name[80];

	/* Describe */
	object_desc_spoil(o_name, sizeof(o_name), o_ptr, FALSE, 0);

	/* Artifact */
	if (artifact_p(o_ptr))
	{
		/* Silly message */
		msg_format("Artifact (%s)", o_name);
	}

	/* Ego-item */
	else if (ego_item_p(o_ptr))
	{
		/* Silly message */
		msg_format("Ego-item (%s)", o_name);
	}

	/* Normal item */
	else
	{
		/* Silly message */
		msg_format("Object (%s)", o_name);
	}
}


/*
 * Attempt to change an object into an ego-item -MWK-
 * Better only called by apply_magic().
 * The return value says if we picked a cursed item (if allowed) and is
 * passed on to a_m_aux1/2().
 * If no legal ego item is found, this routine returns 0, resulting in
 * an unenchanted item.
 */
static int make_ego_item(object_type *o_ptr, bool only_good)
{
	int i, j, level;

	int e_idx;

	long value, total;

	ego_item_type *e_ptr;

	alloc_entry *table = alloc_ego_table;

	/* Fail if object already is ego or artifact */
	if (o_ptr->name1) return (FALSE);
	if (o_ptr->name2) return (FALSE);

	level = object_level;

	/* Boost level (like with object base types) */
	if (level > 0)
	{
		/* Occasional "boost" */
		if (rand_int(GREAT_EGO) == 0)
		{
			/* The bizarre calculation again */
			level = 1 + (level * MAX_DEPTH / randint(MAX_DEPTH));
		}
	}

	/* Reset total */
	total = 0L;

	/* Process probabilities */
	for (i = 0; i < alloc_ego_size; i++)
	{
		/* Default */
		table[i].prob3 = 0;

		/* Objects are sorted by depth */
		if (table[i].level > level) continue;

		/* Get the index */
		e_idx = table[i].index;

		/* Get the actual kind */
		e_ptr = &e_info[e_idx];

		/* If we force good/great, don't create cursed */
		if (only_good && (e_ptr->flags3 & TR3_LIGHT_CURSE)) continue;

		/* Test if this is a legal ego-item type for this object */
		for (j = 0; j < EGO_TVALS_MAX; j++)
		{
			/* Require identical base type */
			if (o_ptr->tval == e_ptr->tval[j])
			{
				/* Require sval in bounds, lower */
				if (o_ptr->sval >= e_ptr->min_sval[j])
				{
					/* Require sval in bounds, upper */
					if (o_ptr->sval <= e_ptr->max_sval[j])
					{
						/* Accept */
						table[i].prob3 = table[i].prob2;
					}
				}
			}
		}

		/* Total */
		total += table[i].prob3;
	}

	/*enforce a true rarity if there are only one or a few rare ego items*/
	if (randint(100) > total) return (0);

	/* Pick an ego-item */
	value = rand_int(total);

	/* Find the object */
	for (i = 0; i < alloc_ego_size; i++)
	{
		/* Found the entry */
		if (value < table[i].prob3) break;

		/* Decrement */
		value = value - table[i].prob3;
	}

	/* We have one */
	e_idx = (byte)table[i].index;
	o_ptr->name2 = e_idx;

	return ((e_info[e_idx].flags3 & TR3_LIGHT_CURSE) ? -2 : 2);
}


/*
 * Mega-Hack -- Attempt to create one of the "Special Objects".
 *
 * We are only called from "make_object()", and we assume that
 * "apply_magic()" is called immediately after we return.
 *
 * Note -- see "make_artifact()" and "apply_magic()".
 *
 * We *prefer* to create the special artifacts in order, but this is
 * normally outweighed by the "rarity" rolls for those artifacts.  The
 * only major effect of this logic is that the Phial (with rarity one)
 * is always the first special artifact created.
 */
static bool make_artifact_special(object_type *o_ptr)
{
	int i;

	int k_idx;

	int depth_check = ((object_generation_mode) ?  object_level : p_ptr->depth);

	/*no artifacts while making items for stores*/
	if ((object_generation_mode >= OB_GEN_MODE_GEN_ST) &&
		(object_generation_mode <= OB_GEN_MODE_BLACK_MARK)) return (FALSE);

	/* No artifacts, do nothing */
	if (adult_no_artifacts) return (FALSE);

	/* No artifacts in the town, unless opening a chest or creating chest item */
	if (!depth_check) return (FALSE);

	/* Check the special artifacts */
	for (i = 0; i < z_info->art_spec_max; ++i)
	{
		artifact_type *a_ptr = &a_info[i];

		/* Skip "empty" artifacts */
		if (a_ptr->tval + a_ptr->sval == 0) continue;

		/* Cannot make an artifact twice */
		if (a_ptr->cur_num) continue;

		/*Hack - don't allow cursed artifacts as quest items*/
		if (object_generation_mode == OB_GEN_MODE_QUEST)
		{
			if (a_ptr->flags3 & (TR3_LIGHT_CURSE)) continue;
			if (a_ptr->flags3 & (TR3_HEAVY_CURSE)) continue;
			if (a_ptr->flags3 & (TR3_PERMA_CURSE)) continue;
		}

		/* Enforce minimum "depth" (loosely) */
		if (a_ptr->level > depth_check)
		{
			/* Get the "out-of-depth factor" */
			int d = (a_ptr->level - depth_check) * 2;

			/* Roll for out-of-depth creation */
			if (rand_int(d) != 0) continue;
		}

		/* Artifact "rarity roll" */
		if (rand_int(a_ptr->rarity) != 0) continue;

		/* Find the base object */
		k_idx = lookup_kind(a_ptr->tval, a_ptr->sval);

		/* Enforce minimum "object" level (loosely) */
		if (k_info[k_idx].level > depth_check)
		{
			/* Get the "out-of-depth factor" */
			int d = (k_info[k_idx].level - depth_check) * 5;

			/* Roll for out-of-depth creation */
			if (rand_int(d) != 0) continue;
		}

		/* Assign the template */
		object_prep(o_ptr, k_idx);

		/* Mark the item as an artifact */
		o_ptr->name1 = i;

		/* Success */
		return (TRUE);
	}

	/* Failure */
	return (FALSE);
}


/*
 * Attempt to change an object into an artifact
 *
 * This routine should only be called by "apply_magic()"
 *
 * Note -- see "make_artifact_special()" and "apply_magic()"
 */
static bool make_artifact(object_type *o_ptr)
{
	int i;

	int depth_check = ((object_generation_mode) ?  object_level : p_ptr->depth);

	/*no artifacts while making items for stores, this is a double-precaution*/
	if ((object_generation_mode >= OB_GEN_MODE_GEN_ST) &&
		(object_generation_mode <= OB_GEN_MODE_BLACK_MARK)) return (FALSE);

	/* No artifacts, do nothing */
	if (adult_no_artifacts) return (FALSE);

	/* No artifacts in the town, unless opening a chest or creating chest item */
	if (!depth_check) return (FALSE);

	/* First try to create a randart, if allowed */
	if ((can_be_randart(o_ptr)) && (!adult_no_xtra_artifacts))
	{
		/*occasionally make a randart*/
		if(one_in_(depth_check + 50))
		{
			/*artifact power is based on depth*/
			int randart_power = 10 + depth_check;

			/*occasional power boost*/
			while (one_in_(25)) randart_power += 25;

			/*
			 * Make a randart.  This should always succeed, unless
			 * there is no space for another randart
		     */
			if (make_one_randart(o_ptr, randart_power, FALSE)) return (TRUE);
		}
	}

	/* Paranoia -- no "plural" artifacts */
	if (o_ptr->number != 1) return (FALSE);

	/* Check the artifact list (skip the "specials" and randoms) */
	for (i = z_info->art_spec_max; i < z_info->art_norm_max; i++)
	{
		artifact_type *a_ptr = &a_info[i];

		/* Skip "empty" items */
		if (a_ptr->tval + a_ptr->sval == 0) continue;

		/* Cannot make an artifact twice */
		if (a_ptr->cur_num) continue;

		/* Must have the correct fields */
		if (a_ptr->tval != o_ptr->tval) continue;
		if (a_ptr->sval != o_ptr->sval) continue;

		/*Hack - don't allow cursed artifacts as quest items*/
		if (object_generation_mode == OB_GEN_MODE_QUEST)
		{
			if (a_ptr->flags3 & (TR3_LIGHT_CURSE)) continue;
			if (a_ptr->flags3 & (TR3_HEAVY_CURSE)) continue;
			if (a_ptr->flags3 & (TR3_PERMA_CURSE)) continue;
		}

		/* XXX XXX Enforce minimum "depth" (loosely) */
		if (a_ptr->level > depth_check)
		{
			/* Get the "out-of-depth factor" */
			int d = (a_ptr->level - depth_check) * 2;

			/* Roll for out-of-depth creation */
			if (rand_int(d) != 0) continue;
		}

		/* We must make the "rarity roll" */
		if (!one_in_(a_ptr->rarity)) continue;

		/* Mark the item as an artifact */
		o_ptr->name1 = i;

		/* Success */
		return (TRUE);
	}

	/* Failure */
	return (FALSE);
}


/*
 * Charge a new wand.
 */
static void charge_wand(object_type *o_ptr)
{
	switch (o_ptr->sval)
	{
		case SV_WAND_HEAL_MONSTER:		o_ptr->pval = randint(20) + 8; break;
		case SV_WAND_HASTE_MONSTER:		o_ptr->pval = randint(20) + 8; break;
		case SV_WAND_CLONE_MONSTER:		o_ptr->pval = randint(5)  + 3; break;
		case SV_WAND_TELEPORT_AWAY:		o_ptr->pval = randint(5)  + 6; break;
		case SV_WAND_DISARMING:			o_ptr->pval = randint(5)  + 4; break;
		case SV_WAND_TRAP_DOOR_DEST:	o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_STONE_TO_MUD:		o_ptr->pval = randint(4)  + 3; break;
		case SV_WAND_LITE:				o_ptr->pval = randint(10) + 6; break;
		case SV_WAND_SLEEP_MONSTER:		o_ptr->pval = randint(15) + 8; break;
		case SV_WAND_SLOW_MONSTER:		o_ptr->pval = randint(10) + 6; break;
		case SV_WAND_CONFUSE_MONSTER:	o_ptr->pval = randint(12) + 6; break;
		case SV_WAND_FEAR_MONSTER:		o_ptr->pval = randint(5)  + 3; break;
		case SV_WAND_DRAIN_LIFE:		o_ptr->pval = randint(3)  + 3; break;
		case SV_WAND_POLYMORPH:			o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_STINKING_CLOUD:	o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_MAGIC_MISSILE:		o_ptr->pval = randint(10) + 6; break;
		case SV_WAND_ACID_BOLT:			o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_ELEC_BOLT:			o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_FIRE_BOLT:			o_ptr->pval = randint(8)  + 6; break;
		case SV_WAND_COLD_BOLT:			o_ptr->pval = randint(5)  + 6; break;
		case SV_WAND_ACID_BALL:			o_ptr->pval = randint(5)  + 2; break;
		case SV_WAND_ELEC_BALL:			o_ptr->pval = randint(8)  + 4; break;
		case SV_WAND_FIRE_BALL:			o_ptr->pval = randint(4)  + 2; break;
		case SV_WAND_COLD_BALL:			o_ptr->pval = randint(6)  + 2; break;
		case SV_WAND_WONDER:			o_ptr->pval = randint(15) + 8; break;
		case SV_WAND_ANNIHILATION:		o_ptr->pval = randint(2)  + 1; break;
		case SV_WAND_DRAGON_FIRE:		o_ptr->pval = randint(3)  + 1; break;
		case SV_WAND_DRAGON_COLD:		o_ptr->pval = randint(3)  + 1; break;
		case SV_WAND_DRAGON_BREATH:		o_ptr->pval = randint(3)  + 1; break;
	}
}



/*
 * Charge a new staff.
 */
static void charge_staff(object_type *o_ptr)
{
	switch (o_ptr->sval)
	{
		case SV_STAFF_DARKNESS:			o_ptr->pval = randint(8)  + 8; break;
		case SV_STAFF_SLOWNESS:			o_ptr->pval = randint(8)  + 8; break;
		case SV_STAFF_HASTE_MONSTERS:	o_ptr->pval = randint(8)  + 8; break;
		case SV_STAFF_SUMMONING:		o_ptr->pval = randint(3)  + 1; break;
		case SV_STAFF_TELEPORTATION:	o_ptr->pval = randint(4)  + 5; break;
		case SV_STAFF_IDENTIFY:			o_ptr->pval = randint(15) + 5; break;
		case SV_STAFF_STARLITE:			o_ptr->pval = randint(5)  + 6; break;
		case SV_STAFF_LITE:				o_ptr->pval = randint(20) + 8; break;
		case SV_STAFF_MAPPING:			o_ptr->pval = randint(5)  + 5; break;
		case SV_STAFF_DETECT_GOLD:		o_ptr->pval = randint(20) + 8; break;
		case SV_STAFF_DETECT_ITEM:		o_ptr->pval = randint(15) + 6; break;
		case SV_STAFF_DETECT_TRAP:		o_ptr->pval = randint(5)  + 6; break;
		case SV_STAFF_DETECT_DOOR:		o_ptr->pval = randint(8)  + 6; break;
		case SV_STAFF_DETECT_INVIS:		o_ptr->pval = randint(15) + 8; break;
		case SV_STAFF_DETECT_EVIL:		o_ptr->pval = randint(15) + 8; break;
		case SV_STAFF_CURE_LIGHT:		o_ptr->pval = randint(5)  + 6; break;
		case SV_STAFF_CURING:			o_ptr->pval = randint(3)  + 4; break;
		case SV_STAFF_HEALING:			o_ptr->pval = randint(2)  + 1; break;
		case SV_STAFF_THE_MAGI:			o_ptr->pval = randint(2)  + 2; break;
		case SV_STAFF_SLEEP_MONSTERS:	o_ptr->pval = randint(5)  + 6; break;
		case SV_STAFF_SLOW_MONSTERS:	o_ptr->pval = randint(5)  + 6; break;
		case SV_STAFF_SPEED:			o_ptr->pval = randint(3)  + 4; break;
		case SV_STAFF_PROBING:			o_ptr->pval = randint(6)  + 2; break;
		case SV_STAFF_DISPEL_EVIL:		o_ptr->pval = randint(3)  + 4; break;
		case SV_STAFF_POWER:			o_ptr->pval = randint(3)  + 1; break;
		case SV_STAFF_HOLINESS:			o_ptr->pval = randint(2)  + 2; break;
		case SV_STAFF_BANISHMENT:		o_ptr->pval = randint(2)  + 1; break;
		case SV_STAFF_EARTHQUAKES:		o_ptr->pval = randint(5)  + 3; break;
		case SV_STAFF_DESTRUCTION:		o_ptr->pval = randint(3)  + 1; break;
		case SV_STAFF_MASS_IDENTIFY:	o_ptr->pval = randint(5) + 5; break;
	}
}

/*
 *
 * Determines the theme of a chest.  This function is called
 * from chest_death when the chest is being opened. JG
 *
 */
static int choose_chest_contents (void)
{
	int chest_theme; /*the returned chest theme*/

	int minlevel; /*helps keep low level themes from appearing at higher levels*/

	int chestlevel; /* random number which determines type of chest theme*/

	int num; /*number used in random section*/

	/*keep weaker themes out of deeper levels*/
	minlevel = object_level / 4;

	/*Hack - don't wan't results over 100 to keep dragon armor themed chests rare*/
	if ((object_level + minlevel) > 100) num = 100 - minlevel;

	else num = object_level;

	chestlevel = randint (num) + minlevel;

	/*now determine the chest theme*/

	/* chest theme #1 is treasure, theme 16 is a chest, not used here.  */
	if (chestlevel <= 10) chest_theme = DROP_TYPE_GOLD;

	/*
	 * from 500' to 1100", treasure begins to give way to
	 * potions, rods/wands/staffs, and scrolls all with almost equal chances.
	 * chest theme #16 is reserved generating an actual chest, it shouldn't be returned here
	 *     which returns the object *nothing* while opening a chest.
	 * chest theme #2 is potions  (+ mushroom of restoring)
	 * chest theme #3 is rods/wands/staffs
	 * chest theme #4 is scrolls
	 * with gold, these are the themes up to 1100', where the weapons and
	 * armor gradually begin to take over.
	 * JG
	 */
	else if (chestlevel <=25) chest_theme = (randint (3)) + 1;

	/*
	 * The next nine themes are armor/weapons,
	 * along with the potions, scrolls, and rods, all with equal chances
	 *
	 * chest theme # 5 is shields
	 * chest theme # 6 is weapons
	 * chest theme # 7 is armor (includes dragon armor)
	 * chest theme # 8 is boots
	 * chest theme # 9 is bow
	 * chest theme #10 is cloak
	 * chest theme #11 is gloves
	 * chest theme #12 is hafted weapons (for the priests)
	 * chest theme #13 is headgear (including crowns)
	 * JG
	 */
	else if (chestlevel <=60) chest_theme = (randint (12)) + 1;

 	/*
	 * Now 10 themes are available, with
	 * jewlery (rings of speed, amulets, and crowns) added.
	 * Equal probability for all themes.
	 *
	 * chest theme # 14 is jewelery
	 * JG
	 */

	else if (chestlevel <=99) chest_theme = (randint (10)) + 4;

	/*
	 * If 100, chest theme # 15 is exclusively
	 * dragon armor scale mail.
	 */
	else chest_theme = DROP_TYPE_DRAGON_ARMOR;

return(chest_theme);
}

/*
 * Apply magic to an item known to be a "weapon"
 *
 * Hack -- note special base to hit and damage dice boosting
 * Hack -- note special processing for weapon/digger
 * Hack -- note special rating boost for dragon scale mail
 */
static void a_m_aux_1(object_type *o_ptr, int level, int power)
{
	int tohit1 = randint(5) + m_bonus(5, level);
	int todam1 = randint(5) + m_bonus(5, level);

	int tohit2 = m_bonus(10, level);
	int todam2 = m_bonus(10, level);


	/* Good */
	if (power > 0)
	{
		/* Enchant */
		o_ptr->to_h += tohit1;
		o_ptr->to_d += todam1;

		/* Very good */
		if (power > 1)
		{
			/* Enchant again */
			o_ptr->to_h += tohit2;
			o_ptr->to_d += todam2;
		}
	}

	/* Cursed */
	else if (power < 0)
	{
		/* Penalize */
		o_ptr->to_h -= tohit1;
		o_ptr->to_d -= todam1;

		/* Very cursed */
		if (power < -1)
		{
			/* Penalize again */
			o_ptr->to_h -= tohit2;
			o_ptr->to_d -= todam2;
		}

		/* Cursed (if "bad") */
		if (o_ptr->to_h + o_ptr->to_d < 0) o_ptr->ident |= (IDENT_CURSED);
	}


	/* Analyze type */
	switch (o_ptr->tval)
	{
		case TV_DIGGING:
		{
			/* Very bad */
			if (power < -1)
			{
				/* Hack -- Horrible digging bonus */
				o_ptr->pval = 0 - (5 + randint(5));
			}

			/* Bad */
			else if (power < 0)
			{
				/* Hack -- Reverse digging bonus */
				o_ptr->pval = 0 - (o_ptr->pval);
			}

			break;
		}

		case TV_HAFTED:
		case TV_POLEARM:
		case TV_SWORD:
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			/*average items*/
			int chance = 50;

			if (power < 0) break;

			/* Very Good */
			if (power > 1) 	chance = 15;
			else if (power > 0) chance = 35;

			/* Hack -- Super-charge the damage dice */
			while ((o_ptr->dd * o_ptr->ds > 0) &&
			       (one_in_(chance)))
			{
				o_ptr->dd++;
			}

			/* Hack -- Limit the damage dice to max of 9*/
			if (o_ptr->dd > 9) o_ptr->dd = 9;

			/* Hack -- Super-charge the damage sides */
			while ((o_ptr->dd * o_ptr->ds > 0) &&
			       (one_in_(chance)))
			{
				o_ptr->ds++;
			}

			/* Hack -- Limit the damage dice to max of 9*/
			if (o_ptr->ds > 9) o_ptr->ds = 9;

			break;
		}

	}
}


/*
 * Apply magic to an item known to be "armor"
 *
 * Hack -- note special processing for crown/helm
 * Hack -- note special processing for robe of permanence
 */
static void a_m_aux_2(object_type *o_ptr, int level, int power)
{
	int toac1 = randint(5) + m_bonus(5, level);

	int toac2 = m_bonus(10, level);


	/* Good */
	if (power > 0)
	{
		/* Enchant */
		o_ptr->to_a += toac1;

		/* Very good */
		if (power > 1)
		{
			/* Enchant again */
			o_ptr->to_a += toac2;
		}
	}

	/* Cursed */
	else if (power < 0)
	{
		/* Penalize */
		o_ptr->to_a -= toac1;

		/* Very cursed */
		if (power < -1)
		{
			/* Penalize again */
			o_ptr->to_a -= toac2;
		}

		/* Cursed (if "bad") */
		if (o_ptr->to_a < 0) o_ptr->ident |= (IDENT_CURSED);
	}


	/* Analyze type */
	switch (o_ptr->tval)
	{
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		{
			/* Mention the item */
			if (cheat_peek) object_mention(o_ptr);

			break;
		}
	}
}



/*
 * Apply magic to an item known to be a "ring" or "amulet"
 *
 * Hack -- note special rating boost for ring of speed
 * Hack -- note special rating boost for certain amulets
 * Hack -- note special "pval boost" code for ring of speed
 * Hack -- note that some items must be cursed (or blessed)
 */
static void a_m_aux_3(object_type *o_ptr, int level, int power)
{
	/* Apply magic (good or bad) according to type */
	switch (o_ptr->tval)
	{
		case TV_RING:
		{
			/* Analyze */
			switch (o_ptr->sval)
			{
				/* Strength, Constitution, Dexterity, Intelligence */
				case SV_RING_STR:
				case SV_RING_CON:
				case SV_RING_DEX:
				case SV_RING_INT:
				{
					/* Stat bonus */
					o_ptr->pval = 1 + m_bonus(5 + (level / 35), level);

					/*cut it off at 6*/
					if (o_ptr->pval > 6) o_ptr->pval = 6;

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse pval */
						o_ptr->pval = 0 - (o_ptr->pval);
					}

					break;
				}

				/* Ring of Speed! */
				case SV_RING_SPEED:
				{
					/* Base speed (1 to 10) */
					o_ptr->pval = randint(5) + m_bonus(5, level);

					/* Super-charge the ring */
					while (one_in_(2)) o_ptr->pval++;

					/* Cursed Ring */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse pval */
						o_ptr->pval = 0 - (o_ptr->pval);

						break;
					}

					/* Rating boost for rings of speed that are not cursed */
					else rating += 25;

					/* Mention the item */
					if (cheat_peek) object_mention(o_ptr);

					break;
				}

				/* Searching */
				case SV_RING_SEARCHING:
				{
					/* Bonus to searching */
					o_ptr->pval = 1 + m_bonus(5, level);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse pval */
						o_ptr->pval = 0 - (o_ptr->pval);
					}

					break;
				}

				/* Searching */
				case SV_RING_AGGRAVATION:
				{
					/* Cursed */
					o_ptr->ident |= (IDENT_CURSED);

					break;
				}


				/* Flames, Acid, Ice, Lightning */
				case SV_RING_FLAMES:
				case SV_RING_ACID:
				case SV_RING_ICE:
				case SV_RING_LIGHTNING:
				{
					/* Bonus to armor class */
					o_ptr->to_a = 5 + randint(5) + m_bonus(10, level) + (level / 10);
					break;
				}

				/* Weakness, Stupidity */
				case SV_RING_WEAKNESS:
				case SV_RING_STUPIDITY:
				{
					/* Broken */
					o_ptr->ident |= (IDENT_BROKEN);

					/* Cursed */
					o_ptr->ident |= (IDENT_CURSED);

					/* Penalize */
					o_ptr->pval = 0 - (1 + m_bonus(5, level));

					break;
				}

				/* WOE, Stupidity */
				case SV_RING_WOE:
				{
					/* Broken */
					o_ptr->ident |= (IDENT_BROKEN);

					/* Cursed */
					o_ptr->ident |= (IDENT_CURSED);

					/* Penalize */
					o_ptr->to_a = 0 - (5 + m_bonus(10, level));
					o_ptr->pval = 0 - (1 + m_bonus(5, level));

					break;
				}

				/* Ring of damage */
				case SV_RING_DAMAGE:
				{
					/* Bonus to damage */
					o_ptr->to_d = 5 + randint(3) + m_bonus(7, level) + (level / 10);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse bonus */
						o_ptr->to_d = 0 - (o_ptr->to_d);
					}

					break;
				}

				/* Ring of Accuracy */
				case SV_RING_ACCURACY:
				{
					/* Bonus to hit */
					o_ptr->to_h = 5 + randint(3) + m_bonus(7, level) + (level / 10);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse tohit */
						o_ptr->to_h = 0 - (o_ptr->to_h);
					}

					break;
				}

				/* Ring of Protection */
				case SV_RING_PROTECTION:
				{
					/* Bonus to armor class */
					o_ptr->to_a = 5 + randint(5) + m_bonus(10, level) + (level / 5);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse toac */
						o_ptr->to_a = 0 - (o_ptr->to_a);
					}

					break;
				}

				/* Ring of Slaying */
				case SV_RING_SLAYING:
				{
					/* Bonus to damage and to hit */
					o_ptr->to_d = randint(5) + m_bonus(5, level) + (level / 10);
					o_ptr->to_h = randint(5) + m_bonus(5, level) + (level / 10);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse bonuses */
						o_ptr->to_h = 0 - (o_ptr->to_h);
						o_ptr->to_d = 0 - (o_ptr->to_d);
					}

					break;
				}
			}

			break;
		}

		case TV_AMULET:
		{
			/* Analyze */
			switch (o_ptr->sval)
			{
				/* Amulet of wisdom/charisma/infravision */
				case SV_AMULET_WISDOM:
				case SV_AMULET_CHARISMA:
				case SV_AMULET_INFRAVISION:
				{
					/* Stat bonus */
					o_ptr->pval = 1 + m_bonus(5 + (level / 35), level);

					/*cut it off at 6*/
					if (o_ptr->pval > 6) o_ptr->pval = 6;

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse bonuses */
						o_ptr->pval = 0 - (o_ptr->pval);
					}

					break;
				}

				/* Amulet of searching */
				case SV_AMULET_SEARCHING:
				{
					o_ptr->pval = randint(5) + m_bonus(5, level);

					/* Cursed */
					if (power < 0)
					{
						/* Broken */
						o_ptr->ident |= (IDENT_BROKEN);

						/* Cursed */
						o_ptr->ident |= (IDENT_CURSED);

						/* Reverse bonuses */
						o_ptr->pval = 0 - (o_ptr->pval);
					}

					break;
				}

				/* Amulet of ESP -- never cursed */
				case SV_AMULET_ESP:
				{
					o_ptr->pval = randint(5) + m_bonus(5, level);

					break;
				}

				/* Amulet of the Magi -- never cursed */
				case SV_AMULET_THE_MAGI:
				{
					o_ptr->pval = 1 + m_bonus(3, level);
					o_ptr->to_a = randint(5) + m_bonus(5, level);

					/* Boost the rating */
					rating += 10;

					/* Mention the item */
					if (cheat_peek) object_mention(o_ptr);

					break;
				}

				/* Amulet of Devotion -- never cursed */
				case SV_AMULET_DEVOTION:
				{
					o_ptr->pval = 1 + m_bonus(3, level);

					/* Boost the rating */
					rating += 10;

					/* Mention the item */
					if (cheat_peek) object_mention(o_ptr);

					break;
				}

				/* Amulet of Weaponmastery -- never cursed */
				case SV_AMULET_WEAPONMASTERY:
				{
					o_ptr->to_h = 1 + m_bonus(4, level);
					o_ptr->to_d = 1 + m_bonus(4, level);
					o_ptr->pval = 1 + m_bonus(2, level);

					/* Boost the rating */
					rating += 10;

					/* Mention the item */
					if (cheat_peek) object_mention(o_ptr);

					break;
				}

				/* Amulet of Trickery -- never cursed */
				case SV_AMULET_TRICKERY:
				{
					o_ptr->pval = randint(1) + m_bonus(3, level);

					/* Boost the rating */
					rating += 10;

					/* Mention the item */
					if (cheat_peek) object_mention(o_ptr);

					break;
				}

				/* Amulet of Doom -- always cursed */
				case SV_AMULET_DOOM:
				{
					/* Broken */
					o_ptr->ident |= (IDENT_BROKEN);

					/* Cursed */
					o_ptr->ident |= (IDENT_CURSED);

					/* Penalize */
					o_ptr->pval = 0 - (randint(5) + m_bonus(5, level));
					o_ptr->to_a = 0 - (randint(5) + m_bonus(5, level));

					break;
				}
			}

			break;
		}
	}
}


/*
 * Apply magic to an item known to be "boring"
 *
 * Hack -- note the special code for various items
 */
static void a_m_aux_4(object_type *o_ptr, int level, int power, bool good, bool great)
{
	object_kind *k_ptr = &k_info[o_ptr->k_idx];

	/* Unused parameters */
	(void)level;
	(void)power;

	/* Apply magic (good or bad) according to type */
	switch (o_ptr->tval)
	{
		case TV_LITE:
		{
			/* Hack -- Torches -- random fuel */
			if (o_ptr->sval == SV_LITE_TORCH)
			{
				o_ptr->timeout = 500 + randint(FUEL_TORCH / 2);
			}

			/* Hack -- Lanterns -- random fuel */
			else if (o_ptr->sval == SV_LITE_LANTERN)
			{
				o_ptr->timeout = 500 + randint(FUEL_LAMP / 2);
			}

			break;
		}

		case TV_WAND:
		{
			/* Hack -- charge wands */
			charge_wand(o_ptr);

			break;
		}

		case TV_STAFF:
		{
			/* Hack -- charge staffs */
			charge_staff(o_ptr);

			break;
		}

		case TV_ROD:
		{
			/* Transfer the pval. */
			o_ptr->pval = k_ptr->pval;
			break;
		}

		case TV_CHEST:
		{
			/* Hack -- chest level is fixed at player level at time of generation */
			o_ptr->pval = object_level;

			/*chest created with good flag get a level boost*/
			if (good) o_ptr->pval += 5;

			/*chest created with great flag also gets a level boost*/
			if (great) o_ptr->pval += 5;

			/*chests now increase level rating*/
			rating += 5;

			/* Don't exceed "chest level" of 110 */
			if (o_ptr->pval > 110) o_ptr->pval = 110;

			/*a minimum pval of 1, or else it will be empty in the town*/
			if (o_ptr->pval < 1) o_ptr->pval = 1;

			/*a guild reward chest shouldn't be trapped*/
			if (object_generation_mode == OB_GEN_MODE_QUEST) o_ptr->pval = (0 - o_ptr->pval);

			/*save the chest theme in xtra1, used in chest death*/
			o_ptr->xtra1 = choose_chest_contents ();

			break;
		}
	}
}

void object_into_artifact(object_type *o_ptr, artifact_type *a_ptr)
{

	/* Extract the other fields */
	o_ptr->pval = a_ptr->pval;
	o_ptr->ac = a_ptr->ac;
	o_ptr->dd = a_ptr->dd;
	o_ptr->ds = a_ptr->ds;
	o_ptr->to_a = a_ptr->to_a;
	o_ptr->to_h = a_ptr->to_h;
	o_ptr->to_d = a_ptr->to_d;
	o_ptr->weight = a_ptr->weight;

	/* Hack - mark the depth of artifact creation for the notes function
	 * probably a bad idea to use this flag.  It is used when making ego-items,
	 * which currently fails when an item is an artifact.  If this was changed
	 * this would be the cause of some major bugs.
	 */
	if (p_ptr->depth)
	{
		o_ptr->xtra1 = p_ptr->depth;
	}

	/*hack - mark chest items with a special level so the notes patch
	 * knows where it is coming from.
	 */
	else if (object_generation_mode == OB_GEN_MODE_CHEST) o_ptr->xtra1 = CHEST_LEVEL;
	else if (object_generation_mode == OB_GEN_MODE_QUEST) o_ptr->xtra1 = QUEST_LEVEL;

	/* Hack -- extract the "broken" flag */
	if (!a_ptr->cost) o_ptr->ident |= (IDENT_BROKEN);

	/* Hack -- extract the "cursed" flag */
	if (a_ptr->flags3 & (TR3_LIGHT_CURSE)) o_ptr->ident |= (IDENT_CURSED);

	/* Hack -- extract the "perfect balance" flag */
	if (a_ptr->flags3 & (TR3_PERFECT_BALANCE)) o_ptr->ident |= (IDENT_PERFECT_BALANCE);
}

/*
 * Complete the "creation" of an object by applying "magic" to the item
 *
 * This includes not only rolling for random bonuses, but also putting the
 * finishing touches on ego-items and artifacts, giving charges to wands and
 * staffs, giving fuel to lites, and placing traps on chests.
 *
 * In particular, note that "Instant Artifacts", if "created" by an external
 * routine, must pass through this function to complete the actual creation.
 *
 * The base "chance" of the item being "good" increases with the "level"
 * parameter, which is usually derived from the dungeon level, being equal
 * to the level plus 10, up to a maximum of 75.  If "good" is true, then
 * the object is guaranteed to be "good".  If an object is "good", then
 * the chance that the object will be "great" (ego-item or artifact), also
 * increases with the "level", being equal to half the level, plus 5, up to
 * a maximum of 20.  If "great" is true, then the object is guaranteed to be
 * "great".  At dungeon level 65 and below, 15/100 objects are "great".
 *
 * If the object is not "good", there is a chance it will be "cursed", and
 * if it is "cursed", there is a chance it will be "broken".  These chances
 * are related to the "good" / "great" chances above.
 *
 * Otherwise "normal" rings and amulets will be "good" half the time and
 * "cursed" half the time, unless the ring/amulet is always good or cursed.
 *
 * If "okay" is true, and the object is going to be "great", then there is
 * a chance that an artifact will be created.  This is true even if both the
 * "good" and "great" arguments are false.  Objects which are forced "great"
 * get three extra "attempts" to become an artifact.
 */
void apply_magic(object_type *o_ptr, int lev, bool okay, bool good, bool great)
{
	int i, rolls, test_good, test_great, power;

	/* Maximum "level" for various things */
	if (lev > MAX_DEPTH - 1) lev = MAX_DEPTH - 1;

	/* Base chance of being "good" */
	test_good = lev + 10;

	/* Maximal chance of being "good" */
	if (test_good > 75) test_good = 75;

	/* Base chance of being "great" */
	test_great = test_good / 2;

	/* Maximal chance of being "great" */
	if (test_great > 20) test_great = 20;

	/* Assume normal */
	power = 0;

	/* Roll for "good", notice that great items don't necessarily need the good flag */
	if ((good) || (great) || (rand_int(100) < test_good))
	{
		/* Assume "good" */
		power = 1;

		/* Roll for "great" */
		if (great || (rand_int(100) < test_great)) power = 2;
	}

	/* Roll for "cursed if not opening a chest" */
	else if ((rand_int(100) < test_good) &&
		     (object_generation_mode != OB_GEN_MODE_CHEST))
	{
		/* Assume "cursed" */
		power = -1;

		/* Roll for "broken" */
		if (rand_int(100) < test_great) power = -2;
	}

	/* Assume no rolls */
	rolls = 0;

	/* Get one roll if excellent */
	if (power >= 2) rolls = 1;

	/*
	 * Get four rolls if good and great flags are true,
	 * only 2 for quests ince they are so repetitive
	 */
	if ((good) && (great))
	{
		if (object_generation_mode == OB_GEN_MODE_QUEST) rolls = 2;
		else rolls = 4;
	}

	/* Get no rolls if not allowed */
	if (!okay || o_ptr->name1) rolls = 0;

	/* Roll for artifacts if allowed */
	for (i = 0; i < rolls; i++)
	{
		/* Roll for an artifact */
		if (make_artifact(o_ptr)) break;
	}

	/* Hack -- analyze artifacts */
	if (o_ptr->name1)
	{
		artifact_type *a_ptr = &a_info[o_ptr->name1];

		/* Hack -- Mark the artifact as "created" */
		a_ptr->cur_num = 1;

		object_into_artifact(o_ptr, a_ptr);

		/* Mega-Hack -- increase the rating */
		rating += 10;

		/* Mega-Hack -- increase the rating again */
		if (a_ptr->cost > 50000L) rating += 10;

		/* Set the good item flag */
		good_item_flag = TRUE;

		/* Cheat -- peek at the item */
		if (cheat_peek) object_mention(o_ptr);

		/* Done */
		return;
	}


	/* Apply magic */
	switch (o_ptr->tval)
	{
		case TV_DIGGING:
		case TV_HAFTED:
		case TV_POLEARM:
		case TV_SWORD:
		case TV_BOW:
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		{
			if ((power > 1) || (power < -1))
			{
				int ego_power;

				ego_power = make_ego_item(o_ptr, (bool)(good || great));

				if (ego_power) power = ego_power;
			}

			if (power) a_m_aux_1(o_ptr, lev, power);

			break;
		}

		case TV_HARD_ARMOR:
		case TV_SOFT_ARMOR:
		case TV_SHIELD:
		case TV_HELM:
		case TV_CROWN:
		case TV_CLOAK:
		case TV_GLOVES:
		case TV_BOOTS:
		{
			if ((power > 1) || (power < -1))
			{
				int ego_power;

				ego_power = make_ego_item(o_ptr, (bool)(good || great));

				if (ego_power) power = ego_power;
			}

			if (power) a_m_aux_2(o_ptr, lev, power);

			break;
		}

		/*Dragon Armor or shield is always an ego-item*/
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		{
			/*Always great*/
			power = 2;

			/*Continue until success*/
			while (!make_ego_item(o_ptr, TRUE)) continue;

			/*add the power*/
			a_m_aux_2(o_ptr, lev, power);
		}


		case TV_RING:
		case TV_AMULET:
		{
			if (!power && (rand_int(100) < 50)) power = -1;
			a_m_aux_3(o_ptr, lev, power);
			break;
		}

		case TV_LITE:
		{
			if ((power > 1) || (power < -1))
			{
				make_ego_item(o_ptr, (bool)(good || great));
			}

			/* Fuel it */
			a_m_aux_4(o_ptr, lev, power, good, great);
			break;
		}

		case TV_MAGIC_BOOK:
		case TV_PRAYER_BOOK:
		{
		  	if ((power > 1) || (power < -1))
			{
			  	make_ego_item(o_ptr, (bool)(good || great));
			}

			a_m_aux_4(o_ptr, lev, power, good, great);
 			break;
		}

		default:
		{
			a_m_aux_4(o_ptr, lev, power, good, great);
			break;
		}
	}


	/* Hack -- analyze ego-items */
	if (o_ptr->name2)
	{
		ego_item_type *e_ptr = &e_info[o_ptr->name2];
		u32b f1, f2, f3;

		/* Examine the item */
		object_flags(o_ptr, &f1, &f2, &f3);

		/* Extra powers */
		if (e_ptr->xtra)
		{
			byte size = 0;
			byte max_flags;
			u32b flag;

			/*Mark what type of extra feature we have here*/
			o_ptr->xtra1 = e_ptr->xtra;

			switch (o_ptr->xtra1)
			{
				case OBJECT_XTRA_STAT_SUSTAIN:
				{
					size = OBJECT_XTRA_SIZE_SUSTAIN;
					break;
				}

				case OBJECT_XTRA_TYPE_HIGH_RESIST:
				{
					size = OBJECT_XTRA_SIZE_HIGH_RESIST;
					break;
				}

				case OBJECT_XTRA_TYPE_POWER:
				{
					size = OBJECT_XTRA_SIZE_POWER;
					break;
				}
				case OBJECT_XTRA_TYPE_IMMUNITY:
				{
					size = OBJECT_XTRA_SIZE_IMMUNITY;
					break;
				}
				case OBJECT_XTRA_TYPE_STAT_ADD:
				{
					size = OBJECT_XTRA_SIZE_STAT_ADD;

					/* Calculate Stat bonus */
					o_ptr->pval = 1 + m_bonus(5 + (lev / 35), lev);

					/*cut it off at 6*/
					if (o_ptr->pval > 6) o_ptr->pval = 6;
					break;
				}
				case OBJECT_XTRA_TYPE_SLAY:
				{
					size = OBJECT_XTRA_SIZE_SLAY;
					break;
				}
				case OBJECT_XTRA_TYPE_KILL:
				{
					size = OBJECT_XTRA_SIZE_KILL;
					break;
				}
				case OBJECT_XTRA_TYPE_BRAND:
				{
					size = OBJECT_XTRA_SIZE_BRAND;
					break;
				}
				case OBJECT_XTRA_TYPE_LOW_RESIST:
				{
					size = OBJECT_XTRA_SIZE_LOW_RESIST;
					break;
				}
			}

			/*start with a clean slate*/
			o_ptr->xtra2 = 0;

			/* Mark when there are no more flags to give */
			max_flags = size;

			while (max_flags)
			{
				/* Make a random flag */
				flag = 0x00000001L << rand_int(size);

				/* Duplicated flag? */
				if (o_ptr->xtra2 & flag)	continue;

				/* Assign the flag */
				o_ptr->xtra2 |= flag;

				/* Note how many we have left */
				max_flags--;

			  	/* Another flag sometimes? */
			  	if (!one_in_(EXTRA_FLAG_CHANCE)) break;

			}
		}

		/* Ego-item throwing weapons may sometimes be perfectly
		 * balanced.
		 */
		if ((f3 & (TR3_THROWING)) && (randint(3) == 1))
		{
			(o_ptr->ident |= IDENT_PERFECT_BALANCE);
		}

		if (f3 & (TR3_PERFECT_BALANCE))
		{
			(o_ptr->ident |= IDENT_PERFECT_BALANCE);
		}

		/* Hack -- acquire "broken" flag */
		if (!e_ptr->cost) o_ptr->ident |= (IDENT_BROKEN);

		/* Hack -- acquire "cursed" flag */
		if (e_ptr->flags3 & (TR3_LIGHT_CURSE)) o_ptr->ident |= (IDENT_CURSED);

		/* Hack -- apply extra penalties if needed */
		if (cursed_p(o_ptr) || broken_p(o_ptr))
		{
			/* Hack -- obtain bonuses */
			if (e_ptr->max_to_h > 0) o_ptr->to_h -= randint(e_ptr->max_to_h);
			if (e_ptr->max_to_d > 0) o_ptr->to_d -= randint(e_ptr->max_to_d);
			if (e_ptr->max_to_a > 0) o_ptr->to_a -= randint(e_ptr->max_to_a);

			/* Hack -- obtain pval, unless one has already been assigned */
			if ((e_ptr->max_pval > 0) && (o_ptr->pval == 0))
				  o_ptr->pval -= randint(e_ptr->max_pval);
		}

		/* Hack -- apply extra bonuses if needed */
		else
		{
			/* Hack -- obtain bonuses */
			if (e_ptr->max_to_h > 0) o_ptr->to_h += randint(e_ptr->max_to_h);
			if (e_ptr->max_to_d > 0) o_ptr->to_d += randint(e_ptr->max_to_d);
			if (e_ptr->max_to_a > 0) o_ptr->to_a += randint(e_ptr->max_to_a);

			/* Hack -- obtain pval */
			if (e_ptr->max_pval > 0)
			{
				/*Handle stat pvals differently*/
				if (e_ptr->flags1 & TR1_ALL_STATS)
				{
					byte bonus = m_bonus(e_ptr->max_pval, lev);

					/*min of 1*/
					if (bonus < 1) bonus = 1;

					o_ptr->pval += bonus;

					/*hard limit*/
					if(o_ptr->pval > 6) o_ptr->pval = 6;
				}
				else o_ptr->pval += randint(e_ptr->max_pval);
			}
		}

		/* Hack -- apply rating bonus */
		rating += e_ptr->rating;

		/* Cheat -- describe the item */
		if (cheat_peek) object_mention(o_ptr);

		/* Done */
		return;
	}


	/* Examine real objects */
	if (o_ptr->k_idx)
	{
		object_kind *k_ptr = &k_info[o_ptr->k_idx];

		/* Hack -- acquire "broken" flag */
		if (!k_ptr->cost) o_ptr->ident |= (IDENT_BROKEN);

		/* Hack -- acquire "cursed" flag */
		if (k_ptr->flags3 & (TR3_LIGHT_CURSE)) o_ptr->ident |= (IDENT_CURSED);
	}
}

/*
 * Hack -- determine if a template is suitable for a general store.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_gen_store(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Certain kinds of food is sold there*/
		case TV_FOOD:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_FOOD_RATION) return (TRUE);
			return (FALSE);
		}

		/* Non artifact Lite Sources are sold there*/
		case TV_LITE:
		{
			if (k_ptr->sval == SV_LITE_TORCH) return (TRUE);
			if (k_ptr->sval == SV_LITE_LANTERN) return (TRUE);
			return (FALSE);
		}

		/* Flasks and Spikes are sold there*/
		case TV_FLASK:
		case TV_SPIKE:
		{
			return (TRUE);
		}

		/*Normal ammo is sold there*/
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_AMMO_NORMAL) return (TRUE);
			if (k_ptr->sval == SV_AMMO_LIGHT) return (TRUE);
			return (FALSE);
		}

		/* Shovels and Picks are sold there*/
		case TV_DIGGING:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SHOVEL) return (TRUE);
			if (k_ptr->sval == SV_PICK) return (TRUE);
			return (FALSE);
		}

		/*
		 *  Normal Cloaks are sold there
		 */
		case TV_CLOAK:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_CLOAK) return (TRUE);
			return (FALSE);
		}
	}

	/* Assume not good */
	return (FALSE);
}



/*
 * Hack -- determine if a template is suitable for the armoury.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_armoury(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Armor -- Good unless damaged */
		case TV_HARD_ARMOR:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_METAL_SCALE_MAIL) return (TRUE);
			if (k_ptr->sval == SV_CHAIN_MAIL) return (TRUE);
			if (k_ptr->sval == SV_AUGMENTED_CHAIN_MAIL) return (TRUE);
			if (k_ptr->sval == SV_DOUBLE_CHAIN_MAIL) return (TRUE);
			if (k_ptr->sval == SV_METAL_BRIGANDINE_ARMOUR) return (TRUE);
			return(FALSE);

		}
		case TV_SOFT_ARMOR:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_ROBE) return (TRUE);
			if (k_ptr->sval == SV_SOFT_LEATHER_ARMOR) return (TRUE);
			if (k_ptr->sval == SV_HARD_LEATHER_ARMOR) return (TRUE);
			if (k_ptr->sval == SV_HARD_STUDDED_LEATHER) return (TRUE);
			if (k_ptr->sval == SV_LEATHER_SCALE_MAIL) return (TRUE);
			return(FALSE);
		}

		case TV_SHIELD:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SMALL_LEATHER_SHIELD) return (TRUE);
			if (k_ptr->sval == SV_SMALL_METAL_SHIELD) return (TRUE);
			return(FALSE);
		}
		case TV_BOOTS:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_PAIR_OF_SOFT_LEATHER_BOOTS) return (TRUE);
			if (k_ptr->sval == SV_PAIR_OF_HARD_LEATHER_BOOTS) return (TRUE);
			return(FALSE);
		}
		case TV_GLOVES:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SET_OF_LEATHER_GLOVES) return (TRUE);
			if (k_ptr->sval == SV_SET_OF_GAUNTLETS) return (TRUE);
			return(FALSE);
		}
		case TV_HELM:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_HARD_LEATHER_CAP) return (TRUE);
			if (k_ptr->sval == SV_IRON_HELM) return (TRUE);
			return(FALSE);
		}
	}

	/* Assume not good */
	return (FALSE);
}

/*
 * Hack -- determine if a template is suitable for the weaponsmith.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_weaponsmith(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Weapons -- suitable  unless damaged */
		case TV_SWORD:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_DAGGER) return (TRUE);
			if (k_ptr->sval == SV_MAIN_GAUCHE) return (TRUE);
			if (k_ptr->sval == SV_RAPIER) return (TRUE);
			if (k_ptr->sval == SV_SMALL_SWORD) return (TRUE);
			if (k_ptr->sval == SV_SHORT_SWORD) return (TRUE);
			if (k_ptr->sval == SV_SABRE) return (TRUE);
			if (k_ptr->sval == SV_CUTLASS) return (TRUE);
			if (k_ptr->sval == SV_BROAD_SWORD) return (TRUE);
			if (k_ptr->sval == SV_LONG_SWORD) return (TRUE);
			if (k_ptr->sval == SV_SCIMITAR) return (TRUE);
			if (k_ptr->sval == SV_KATANA) return (TRUE);
			if (k_ptr->sval == SV_BASTARD_SWORD) return (TRUE);
			return (FALSE);
		}
		case TV_HAFTED:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_WHIP) return (TRUE);
			return (FALSE);
		}
		case TV_POLEARM:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SPEAR) return (TRUE);
			if (k_ptr->sval == SV_AWL_PIKE) return (TRUE);
			if (k_ptr->sval == SV_TRIDENT) return (TRUE);
			if (k_ptr->sval == SV_PIKE) return (TRUE);
			if (k_ptr->sval == SV_BEAKED_AXE) return (TRUE);
			if (k_ptr->sval == SV_BROAD_AXE) return (TRUE);
			if (k_ptr->sval == SV_BATTLE_AXE) return (TRUE);
			return (FALSE);
		}
		case TV_BOW:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SLING) return (TRUE);
			if (k_ptr->sval == SV_SHORT_BOW) return (TRUE);
			if (k_ptr->sval == SV_LONG_BOW) return (TRUE);
			if (k_ptr->sval == SV_LIGHT_XBOW) return (TRUE);
			return (FALSE);
		}
		/*Normal ammo is sold there*/
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_AMMO_NORMAL) return (TRUE);
			if (k_ptr->sval == SV_AMMO_LIGHT) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is suitable for the temple.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_temple(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Hafted weapons only in the temple*/
		case TV_HAFTED:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_WHIP) return (TRUE);
			if (k_ptr->sval == SV_QUARTERSTAFF) return (TRUE);
			if (k_ptr->sval == SV_MACE) return (TRUE);
			if (k_ptr->sval == SV_BALL_AND_CHAIN) return (TRUE);
			if (k_ptr->sval == SV_WAR_HAMMER) return (TRUE);
			if (k_ptr->sval == SV_LUCERN_HAMMER) return (TRUE);
			if (k_ptr->sval == SV_MORNING_STAR) return (TRUE);
			if (k_ptr->sval == SV_FLAIL) return (TRUE);
			if (k_ptr->sval == SV_LEAD_FILLED_MACE) return (TRUE);
			return (FALSE);
		}
		/*scrolls suitable for the temple*/
		case TV_SCROLL:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_REMOVE_CURSE) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_IDENTIFY) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_BLESSING) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_HOLY_CHANT) return (TRUE);
			return (FALSE);
		}
		case TV_POTION:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_POTION_BOLDNESS) return (TRUE);
			if (k_ptr->sval == SV_POTION_HEROISM) return (TRUE);
			if (k_ptr->sval == SV_POTION_CURE_LIGHT) return (TRUE);
			if (k_ptr->sval == SV_POTION_CURE_SERIOUS) return (TRUE);
			if (k_ptr->sval == SV_POTION_CURE_CRITICAL) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESTORE_EXP) return (TRUE);
			return (FALSE);
		}

		/* Books -- HACK - High level books are good only
		 * if within 5 levels of being out of depth */
		case TV_PRAYER_BOOK:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval < SV_BOOK_MIN_GOOD) return (TRUE);
			/*Dungeon spellbooks*/
			return (FALSE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is suitable for the alchemy shop.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_alchemy(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/*scrolls suitable for the alchemy shop*/
		case TV_SCROLL:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_ENCHANT_WEAPON_TO_HIT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_ENCHANT_WEAPON_TO_DAM) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_ENCHANT_ARMOR) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_IDENTIFY) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_LIGHT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_PHASE_DOOR) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_MONSTER_CONFUSION) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_MAPPING) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_DETECT_TRAP) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_DETECT_ITEM) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_DETECT_DOOR) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_DETECT_INVIS) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_RECHARGING) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_SATISFY_HUNGER) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_WORD_OF_RECALL) return (TRUE);
			return (FALSE);
		}
		case TV_POTION:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESIST_HEAT) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESIST_COLD) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESIST_ELECTRICITY) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESIST_ACID) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_STR) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_INT) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_WIS) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_DEX) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_CON) return (TRUE);
			if (k_ptr->sval == SV_POTION_RES_CHR) return (TRUE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is suitable for the magic_shop.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to be cursed.
 */
static bool kind_is_magic_shop(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* No "worthless" items */
	if (k_ptr->cost < 0) return (FALSE);

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/*Rings suitable for the magic_shop*/
		case TV_RING:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_RING_SEARCHING) return (TRUE);
			if (k_ptr->sval == SV_RING_FEATHER_FALL) return (TRUE);
			if (k_ptr->sval == SV_RING_PROTECTION) return (TRUE);
			return (FALSE);
		}
		/*Amulets suitable for the magic_shop*/
		case TV_AMULET:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_AMULET_CHARISMA) return (TRUE);
			if (k_ptr->sval == SV_AMULET_SLOW_DIGEST) return (TRUE);
			if (k_ptr->sval == SV_AMULET_RESIST_ACID) return (TRUE);
			return (FALSE);
		}
		/*Amulets suitable for the magic_shop*/
		case TV_WAND:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_WAND_SLOW_MONSTER) return (TRUE);
			if (k_ptr->sval == SV_WAND_CONFUSE_MONSTER) return (TRUE);
			if (k_ptr->sval == SV_WAND_SLEEP_MONSTER) return (TRUE);
			if (k_ptr->sval == SV_WAND_MAGIC_MISSILE) return (TRUE);
			if (k_ptr->sval == SV_WAND_STINKING_CLOUD) return (TRUE);
			if (k_ptr->sval == SV_WAND_WONDER) return (TRUE);
			return (FALSE);
		}
		/*Staves suitable for the magic_shop*/
		case TV_STAFF:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval == SV_STAFF_LITE) return (TRUE);
			if (k_ptr->sval == SV_STAFF_MAPPING) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_TRAP) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_DOOR) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_GOLD) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_ITEM) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_INVIS) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DETECT_EVIL) return (TRUE);
			if (k_ptr->sval == SV_STAFF_TELEPORTATION) return (TRUE);
			if (k_ptr->sval == SV_STAFF_IDENTIFY) return (TRUE);
			return (FALSE);
		}

		/* Books -- HACK - High level books are good only
		 * if within 5 levels of being out of depth */
		case TV_MAGIC_BOOK:
		{
			if (allow_altered_inventory) return (TRUE);
			if (k_ptr->sval < SV_BOOK_MIN_GOOD) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is suitable for the black_market.
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to cause the object to less valuable later.
 */
static bool kind_is_black_market(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* First non-damaged weapons and armor are all good..... */
	switch (k_ptr->tval)
	{
		/* Armor -- Good unless damaged */
		case TV_HARD_ARMOR:
		case TV_SOFT_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		case TV_SHIELD:
		case TV_CLOAK:
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_HELM:
		case TV_CROWN:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}

		case TV_CHEST:
		{
			return (FALSE);
		}

		/* Weapons -- Good unless damaged */
		case TV_BOW:
		case TV_SWORD:
		case TV_HAFTED:
		case TV_POLEARM:
		case TV_DIGGING:
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Otherwise -- No "cheap" base items */
	if (k_ptr->cost < 10) return (FALSE);

	/* Otherwise suitable at this point*/
	return (TRUE);
}

/*
 * Hack -- determine if a template is "a priestly dungeon spellbook".
 *
 */
static bool kind_is_dungeon_prayer_book(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Books **/

		case TV_PRAYER_BOOK:
		{
			if (k_ptr->sval >= SV_BOOK_MIN_GOOD) return (TRUE);
			return(FALSE);
		}

	}
	return(FALSE);

}

/*
 * Hack -- determine if a template is "a priestly dungeon spellbook".
 *
 */
static bool kind_is_dungeon_magic_book(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Books **/

		case TV_MAGIC_BOOK:
		{
			if (k_ptr->sval >= SV_BOOK_MIN_GOOD) return (TRUE);
			return(FALSE);
		}

	}
	return (FALSE);
}


/*
 * Hack -- determine if a template is "great".
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to choose a kind which is "great", and then later cause
 * the actual object to be cursed.  We do explicitly forbid objects
 * which are known to be boring or which start out somewhat damaged.
 */
static bool kind_is_great(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Armor -- great unless damaged */
		case TV_HARD_ARMOR:
		case TV_SOFT_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		case TV_SHIELD:
		case TV_CLOAK:
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_HELM:
		case TV_CROWN:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}

		/* Weapons -- great unless damaged */
		case TV_BOW:
		case TV_SWORD:
		case TV_HAFTED:
		case TV_POLEARM:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}

		/* Ammo -- Arrows/Bolts are great, unless quest item */
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			if ((object_generation_mode == OB_GEN_MODE_QUEST) ||
			    (object_generation_mode == OB_GEN_MODE_RANDART)) return (FALSE);
			return (TRUE);
		}

		/* Rings -- Rings of Speed are great */
		case TV_RING:
		{
			if (k_ptr->sval == SV_RING_SPEED) return (TRUE);
			return (FALSE);
		}

		/*scrolls of "*Acquirement*" are great*/
		case TV_SCROLL:
		{
			if (k_ptr->sval == SV_SCROLL_STAR_ACQUIREMENT) return (TRUE);
			if ((k_ptr->sval == SV_SCROLL_CREATE_RANDART) &&
			    (!adult_no_xtra_artifacts))   return (TRUE);
			return (FALSE);
		}

		/* Chests -- Chests are great, except for quests.*/
		case TV_CHEST:
		{
			if ((object_generation_mode == OB_GEN_MODE_QUEST) ||
			    (object_generation_mode == OB_GEN_MODE_RANDART))  return (FALSE);
			return (TRUE);
		}

	}

	/* Assume not great */
	return (FALSE);
}


/*
 * Hack -- determine if a template is a chest.
 *
 */
static bool kind_is_chest(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Chests -- */
		case TV_CHEST:
		{
			return (TRUE);
		}

	}

	/* Assume not chest */
	return (FALSE);
}

/*
 * Hack -- determine if a template is footwear.
 *
 */
static bool kind_is_boots(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* footwear -- */
		case TV_BOOTS:
		{
			return (TRUE);
		}

	}

	/* Assume not footwear */
	return (FALSE);
}

/*
 * Hack -- determine if a template is headgear.
 *
 */
static bool kind_is_headgear(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Headgear -- Suitable unless damaged */
		case TV_HELM:
		case TV_CROWN:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not headgear */
	return (FALSE);
}

/*
 * Hack -- determine if a template is armor.
 *
 */
static bool kind_is_armor(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Armor -- suitable  unless damaged */
		case TV_HARD_ARMOR:
		case TV_SOFT_ARMOR:
		case TV_DRAG_ARMOR:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not armor */
	return (FALSE);
}

/*
 * Hack -- determine if a template is Dragon Scale Mail.
 *
 */
static bool kind_is_dragarmor(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Dragon Armor -- suitable  unless damaged */
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not Dragon Scale Mail */
	return (FALSE);
}

/*
 * Hack -- determine if a template is gloves.
 *
 */
static bool kind_is_gloves(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Gloves -- suitable  unless damaged */
		case TV_GLOVES:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not suitable  */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a cloak.
 *
 */
static bool kind_is_cloak(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Cloaks -- suitable  unless damaged */

		case TV_CLOAK:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not a suitable  */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a shield.
 *
 */
static bool kind_is_shield(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* shield -- suitable  Unless Damaged*/
		case TV_SHIELD:
		case TV_DRAG_SHIELD:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a bow/ammo.
 */

static bool kind_is_bow(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/* All firing weapons and Ammo are suitable  */
		case TV_BOW:
		{
			return (TRUE);
		}

		/*hack - don't allow ammo as a quest reward*/
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			if ((object_generation_mode == OB_GEN_MODE_QUEST) ||
			    (object_generation_mode == OB_GEN_MODE_RANDART))  return (FALSE);
			return (TRUE);

		}

	}

	/* Assume not suitable  */
	return (FALSE);
}


/*
 * Hack -- determine if a template is a hafted weapon.
 */

static bool kind_is_hafted(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Hafted Weapons -- suitable  unless damaged */
		case TV_HAFTED:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not suitable  */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a "good" digging tool
 *
 */
static bool kind_is_digging_tool(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/* Diggers -- Good unless damaged */
		case TV_DIGGING:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}

	}

	/* Assume not good */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a edged weapon.
 */
static bool kind_is_edged(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Edged Weapons -- suitable unless damaged */
		case TV_SWORD:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a polearm.
 */
static bool kind_is_polearm(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Weapons -- suitable  unless damaged */
		case TV_POLEARM:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a template is a weapon.
 */
static bool kind_is_weapon(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Weapons -- suitable  unless damaged */
		case TV_SWORD:
		case TV_HAFTED:
		case TV_POLEARM:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}
	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a scroll is suitable for a chest.
 *
 */
static bool kind_is_scroll(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/*scrolls suitable for a chest*/
		case TV_SCROLL:
		{
			if (k_ptr->sval == SV_SCROLL_ACQUIREMENT) return (TRUE);
			if ((k_ptr->sval == SV_SCROLL_CREATE_RANDART) &&
			    (!adult_no_xtra_artifacts))   return (TRUE);
			if (k_ptr->sval == SV_SCROLL_STAR_ACQUIREMENT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_BANISHMENT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_MASS_BANISHMENT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_RUNE_OF_PROTECTION) return (TRUE);
			if ((k_ptr->sval == SV_SCROLL_TELEPORT) &&
				((k_ptr->level + 15) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_STAR_IDENTIFY) return (TRUE);
			if ((k_ptr->sval == SV_SCROLL_RECHARGING) &&
				((k_ptr->level + 15) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_STAR_RECHARGING) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_CREATE_MONSTER_TRAP) return (TRUE);

			return (FALSE);
		}

		/* Books -- HACK - High level books are good only
		 * if within 5 levels of being out of depth */
		case TV_MAGIC_BOOK:
		case TV_PRAYER_BOOK:
		{
			if (((k_ptr->level - 5) < object_level ) &&
			(k_ptr->sval >= SV_BOOK_MIN_GOOD)) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a potion is good for a chest.
 * includes mushroom of restoring
 *
 */
static bool kind_is_potion(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/*potions suitable for a chest*/
		case TV_POTION:
		{
			if (k_ptr->sval == SV_POTION_SPEED) return (TRUE);
			if ((k_ptr->sval == SV_POTION_HEALING) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_POTION_STAR_HEALING) return (TRUE);
			if (k_ptr->sval == SV_POTION_LIFE) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_STR) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_INT) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_WIS) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_DEX) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_CON) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_INC_CHR) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_POTION_AUGMENTATION) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_POTION_EXPERIENCE) return (TRUE);
			if (k_ptr->sval == SV_POTION_ENLIGHTENMENT) return (TRUE);
			if (k_ptr->sval == SV_POTION_RESISTANCE) return (TRUE);

			return (FALSE);
		}

		case TV_FOOD:
		/* HACK -  Mushrooms of restoring can be with potions */
		{
			if ((k_ptr->sval == SV_FOOD_RESTORING) &&
				((k_ptr->level + 25) >= object_level )) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable */
	return (FALSE);
}

/*
 * Hack -- determine if a rod/wand/staff is good for a chest.
 *
 */
static bool kind_is_rod_wand_staff(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{

		/*wands suitable for a chest*/
		case TV_WAND:

		{
			if ((k_ptr->sval == SV_WAND_TELEPORT_AWAY) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_WAND_STONE_TO_MUD) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_WAND_ANNIHILATION) return (TRUE);
			if (k_ptr->sval == SV_WAND_DRAGON_FIRE) return (TRUE);
			if (k_ptr->sval == SV_WAND_DRAGON_COLD) return (TRUE);
			if (k_ptr->sval == SV_WAND_DRAGON_BREATH) return (TRUE);

			return (FALSE);

		}

		/*staffs suitable for a chest*/
		case TV_STAFF:

		{
			if ((k_ptr->sval == SV_STAFF_TELEPORTATION) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_STAFF_THE_MAGI) return (TRUE);
			if (k_ptr->sval == SV_STAFF_SPEED) return (TRUE);
			if (k_ptr->sval == SV_STAFF_DISPEL_EVIL) return (TRUE);
			if (k_ptr->sval == SV_STAFF_POWER) return (TRUE);
			if (k_ptr->sval == SV_STAFF_HOLINESS) return (TRUE);
			if (k_ptr->sval == SV_STAFF_BANISHMENT) return (TRUE);
			if (k_ptr->sval == SV_STAFF_MASS_IDENTIFY) return (TRUE);
			if ((k_ptr->sval == SV_STAFF_DESTRUCTION) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			return (FALSE);
		}

		/*rods suitable for a chest*/
		case TV_ROD:

		{
			if ((k_ptr->sval == SV_ROD_IDENTIFY) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_ROD_DETECTION) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			if ((k_ptr->sval == SV_ROD_STONE_TO_MUD) &&
				((k_ptr->level + 10) >= object_level )) return (TRUE);
			if (k_ptr->sval == SV_ROD_HEALING) return (TRUE);
			if (k_ptr->sval == SV_ROD_RESTORATION) return (TRUE);
			if (k_ptr->sval == SV_ROD_SPEED) return (TRUE);
			if (k_ptr->sval == SV_ROD_STAR_IDENTIFY) return (TRUE);
			if (k_ptr->sval == SV_ROD_MASS_IDENTIFY) return (TRUE);
			if ((k_ptr->sval == SV_ROD_TELEPORT_AWAY) &&
				((k_ptr->level + 20) >= object_level )) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable for a chest */
	return (FALSE);
}

/*
 * Hack -- determine if a template is "jewelry for chests".
 *
 */
static bool kind_is_jewelry(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Crowns are suitable for a chest */
		case TV_CROWN:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}

		/*  Rings of Speed are suitable for a chest */
		case TV_RING:
		{
			if (k_ptr->sval == SV_RING_SPEED) return (TRUE);
			return (FALSE);
		}

		/* Some Amulets are suitable for a chest*/
		case TV_AMULET:

		{
		  	if (k_ptr->sval == SV_AMULET_THE_MAGI) return (TRUE);
			if (k_ptr->sval == SV_AMULET_DEVOTION) return (TRUE);
			if (k_ptr->sval == SV_AMULET_WEAPONMASTERY) return (TRUE);
			if (k_ptr->sval == SV_AMULET_TRICKERY) return (TRUE);
			return (FALSE);
		}

	}

	/* Assume not suitable for a chest */
	return (FALSE);
}





/*
 * Hack -- determine if a template is "good".
 *
 * Note that this test only applies to the object *kind*, so it is
 * possible to choose a kind which is "good", and then later cause
 * the actual object to be cursed.  We do explicitly forbid objects
 * which are known to be boring or which start out somewhat damaged.
 */
static bool kind_is_good(int k_idx)
{
	object_kind *k_ptr = &k_info[k_idx];

	/* Analyze the item type */
	switch (k_ptr->tval)
	{
		/* Armor -- Good unless damaged */
		case TV_HARD_ARMOR:
		case TV_SOFT_ARMOR:
		case TV_DRAG_ARMOR:
		case TV_DRAG_SHIELD:
		case TV_SHIELD:
		case TV_CLOAK:
		case TV_BOOTS:
		case TV_GLOVES:
		case TV_HELM:
		case TV_CROWN:
		{
			if (k_ptr->to_a < 0) return (FALSE);
			return (TRUE);
		}

		/* Weapons -- Good unless damaged */
		case TV_BOW:
		case TV_SWORD:
		case TV_HAFTED:
		case TV_POLEARM:
		case TV_DIGGING:
		{
			if (k_ptr->to_h < 0) return (FALSE);
			if (k_ptr->to_d < 0) return (FALSE);
			return (TRUE);
		}

		/* Ammo -- Arrows/Bolts are good */
		case TV_BOLT:
		case TV_ARROW:
		case TV_SHOT:
		{
			if ((object_generation_mode == OB_GEN_MODE_QUEST) ||
			    (object_generation_mode == OB_GEN_MODE_RANDART))  return (FALSE);
			return (TRUE);
		}

		/* Books -- HACK - High level books are good only
		 * if within 15 levels of being out of depth
		 **/

		case TV_MAGIC_BOOK:
		case TV_PRAYER_BOOK:
		{
			if (((k_ptr->level - 15) < object_level ) &&
			(k_ptr->sval >= SV_BOOK_MIN_GOOD)) return (TRUE);
			return (FALSE);
		}

		/* Rings -- Rings of Speed are good */
		case TV_RING:
		{
			if (k_ptr->sval == SV_RING_SPEED) return (TRUE);
			return (FALSE);
		}

		/* Amulets -- Amulets are good*/
		case TV_AMULET:

		{
		  	if (k_ptr->sval == SV_AMULET_THE_MAGI) return (TRUE);
			if (k_ptr->sval == SV_AMULET_DEVOTION) return (TRUE);
			if (k_ptr->sval == SV_AMULET_WEAPONMASTERY) return (TRUE);
			if (k_ptr->sval == SV_AMULET_TRICKERY) return (TRUE);
			return (FALSE);
		}

		/*scrolls of "*acquirement*" and "acquirement" are good*/
		case TV_SCROLL:

		{
			if (k_ptr->sval == SV_SCROLL_ACQUIREMENT) return (TRUE);
			if (k_ptr->sval == SV_SCROLL_STAR_ACQUIREMENT) return (TRUE);
			if ((k_ptr->sval == SV_SCROLL_CREATE_RANDART) &&
			    (!adult_no_xtra_artifacts))   return (TRUE);
			return (FALSE);
		}

		/*the very powerful healing potions can be good*/
		case TV_POTION:
		{
			if ((k_ptr->sval == SV_POTION_STAR_HEALING) ||
				(k_ptr->sval == SV_POTION_LIFE))
		   	{
			    if (object_level > 85)

				return (TRUE);
			}
			return (FALSE);
		}

		/* Chests -- Chests are good. */
		case TV_CHEST:
		{
			return (TRUE);
		}

	}

	/* Assume not good */
	return (FALSE);
}



/*
 * Attempt to make an object (normal or good/great)
 *
 * This routine plays nasty games to generate the "special artifacts".
 *
 * This routine uses "object_level" for the "generation level".
 *
 * We assume that the given object has been "wiped".
 */
bool make_object(object_type *j_ptr, bool good, bool great, int objecttype)
{
	int prob, base;

	/* Chance of "special object" */
	prob = ((good || great) ? 10 : 1000);

	/*better chance to check special artifacts if there is a jewelery theme*/
	if (objecttype == DROP_TYPE_JEWELRY) prob /= 2;

	/* Base level for the object */
	base = ((good || great) ? (object_level + 10) : object_level);

	/* Attempt to generate a special artifact if prob = 0, or a normal object
	 * if not.
	 */
	if ((rand_int(prob) != 0) || (!make_artifact_special(j_ptr)))
	{
		int k_idx;

		/*
		 * Next check if it is a themed drop, and
		 * only include objects from a pre-set theme.  But, it can be
		 * called from anywhere.
		 * First check to skip all these checks when unnecessary.
		 */
		 if ((good) || (great) || (objecttype >= DROP_TYPE_POTION))
		{
			/*note - theme 1 is gold, sent to the make_gold function*/
			if (objecttype == DROP_TYPE_POTION)						get_obj_num_hook = kind_is_potion;
			else if (objecttype == DROP_TYPE_ROD_WAND_STAFF) 		get_obj_num_hook = kind_is_rod_wand_staff;
			else if (objecttype == DROP_TYPE_SCROLL) 				get_obj_num_hook = kind_is_scroll;
			else if (objecttype == DROP_TYPE_SHIELD) 				get_obj_num_hook = kind_is_shield;
			else if (objecttype == DROP_TYPE_WEAPON) 				get_obj_num_hook = kind_is_weapon;
			else if (objecttype == DROP_TYPE_ARMOR) 				get_obj_num_hook = kind_is_armor;
			else if (objecttype == DROP_TYPE_BOOTS) 				get_obj_num_hook = kind_is_boots;
			else if (objecttype == DROP_TYPE_BOW) 					get_obj_num_hook = kind_is_bow;
			else if (objecttype == DROP_TYPE_CLOAK)					get_obj_num_hook = kind_is_cloak;
			else if (objecttype == DROP_TYPE_GLOVES)				get_obj_num_hook = kind_is_gloves;
			else if (objecttype == DROP_TYPE_HAFTED)				get_obj_num_hook = kind_is_hafted;
			else if (objecttype == DROP_TYPE_HEADGEAR)				get_obj_num_hook = kind_is_headgear;
			else if (objecttype == DROP_TYPE_JEWELRY)				get_obj_num_hook = kind_is_jewelry;
			else if (objecttype == DROP_TYPE_DRAGON_ARMOR)			get_obj_num_hook = kind_is_dragarmor;
			else if (objecttype == DROP_TYPE_CHEST)					get_obj_num_hook = kind_is_chest;
			else if (objecttype == DROP_TYPE_DUNGEON_MAGIC_BOOK)	get_obj_num_hook = kind_is_dungeon_magic_book;
			else if (objecttype == DROP_TYPE_DUNGEON_PRAYER_BOOK)	get_obj_num_hook = kind_is_dungeon_prayer_book;
			else if (objecttype == DROP_TYPE_EDGED)					get_obj_num_hook = kind_is_edged;
			else if (objecttype == DROP_TYPE_POLEARM)				get_obj_num_hook = kind_is_polearm;
			else if (objecttype == DROP_TYPE_DIGGING)				get_obj_num_hook = kind_is_digging_tool;

			/*
			 *	If it isn't a chest, check good and great flags.
			 *  They each now have their own templates.
			 */
			else if (great)	get_obj_num_hook = kind_is_great;
			else if (good)	get_obj_num_hook = kind_is_good;
		}

		/* Prepare allocation table if needed*/
		if ((objecttype) || (good) || (great))
		{
			get_obj_num_prep();
		}

		/* Pick a random object */
		k_idx = get_obj_num(base);

		/* Clear the objects template*/
		if ((objecttype) ||	(good) || (great))
		{
			/* Clear restriction */
			get_obj_num_hook = NULL;

			/* Prepare allocation table */
			get_obj_num_prep();
		}


		/* Handle failure*/
		if (!k_idx) return (FALSE);

		/* Prepare the object */
		object_prep(j_ptr, k_idx);

	}

	/* Apply magic (allow artifacts) */
	apply_magic(j_ptr, object_level, TRUE, good, great);

	/* Hack -- generate multiple spikes/missiles */
	switch (j_ptr->tval)
	{
		case TV_SPIKE:
		case TV_SHOT:
		case TV_ARROW:
		case TV_BOLT:
		{
			j_ptr->number = damroll(6, 7);
		}
	}

	/* Notice "okay" out-of-depth objects */
	if (!cursed_p(j_ptr) && !broken_p(j_ptr) &&
	    (k_info[j_ptr->k_idx].level > p_ptr->depth))
	{
		/* Rating increase */
		rating += (k_info[j_ptr->k_idx].level - p_ptr->depth);

		/* Cheat -- peek at items */
		if (cheat_peek) object_mention(j_ptr);
	}

	/* Success */
	return (TRUE);
}

/*
 * Sets the object generation mode for the store, since in doing
 * store inventory we make many objects of the same type
 * The function is here rather than store.c because
 * This is where all the other needed functions are.
 */

bool prep_store_object(int storetype)
{
	/*get the store creation mode*/
	switch (storetype)
	{
		case STORE_GENERAL:
		{
			object_generation_mode = OB_GEN_MODE_GEN_ST;
			get_obj_num_hook = kind_is_gen_store;
			break;
		}
		case STORE_ARMOR:
		{
			object_generation_mode = OB_GEN_MODE_ARMOURY;
			get_obj_num_hook = kind_is_armoury;
			break;
		}
		case STORE_WEAPON:
		{
			object_generation_mode = OB_GEN_MODE_WEAPONSMITH;
			get_obj_num_hook = kind_is_weaponsmith;
			break;
		}
		case STORE_TEMPLE:
		{
			object_generation_mode = OB_GEN_MODE_TEMPLE;
			get_obj_num_hook = kind_is_temple;
			break;
		}
		case STORE_ALCHEMY:
		{
			object_generation_mode = OB_GEN_MODE_ALCHEMY;
			get_obj_num_hook = kind_is_alchemy;
			break;
		}
		case STORE_MAGIC:
		{
			object_generation_mode = OB_GEN_MODE_MAGIC_SHOP;
			get_obj_num_hook = kind_is_magic_shop;
			break;
		}
		case STORE_B_MARKET:
		{
			object_generation_mode = OB_GEN_MODE_BLACK_MARK;
			get_obj_num_hook = kind_is_black_market;
			break;
		}

		default: return (FALSE);
	}

	/*prepare the allocation table*/
	get_obj_num_prep();

	return(TRUE);

}

/*
 * Set the object theme
 */


/*
 * This is an imcomplete list of themes.  Returns false if theme not found.
 * Used primarily for Randarts
 */
bool prep_object_theme(int themetype)
{
	/*get the store creation mode*/
	switch (themetype)
	{
		case DROP_TYPE_SHIELD:
		{
			get_obj_num_hook = kind_is_shield;
			break;
		}
		case DROP_TYPE_WEAPON:
		{
			get_obj_num_hook = kind_is_weapon;
			break;
		}
		case DROP_TYPE_EDGED:
		{
			get_obj_num_hook = kind_is_edged;
			break;
		}
		case DROP_TYPE_POLEARM:
		{
			get_obj_num_hook = kind_is_polearm;
			break;
		}
		case DROP_TYPE_ARMOR:
		{
			get_obj_num_hook = kind_is_armor;
			break;
		}
		case DROP_TYPE_BOOTS:
		{
			get_obj_num_hook = kind_is_boots;
			break;
		}
		case DROP_TYPE_BOW:
		{
			get_obj_num_hook = kind_is_bow;
			break;
		}
		case DROP_TYPE_CLOAK:
		{
			get_obj_num_hook = kind_is_cloak;
			break;
		}
		case DROP_TYPE_GLOVES:
		{
			get_obj_num_hook = kind_is_gloves;
			break;
		}
		case DROP_TYPE_HAFTED:
		{
			get_obj_num_hook = kind_is_hafted;
			break;
		}
		case DROP_TYPE_HEADGEAR:
		{
			get_obj_num_hook = kind_is_headgear;
			break;
		}
		case DROP_TYPE_DRAGON_ARMOR:
		{
			get_obj_num_hook = kind_is_dragarmor;
			break;
		}
		case DROP_TYPE_DIGGING:
		{
			get_obj_num_hook = kind_is_digging_tool;

			break;
		}

		default: return (FALSE);
	}

	/*prepare the allocation table*/
	get_obj_num_prep();

	return(TRUE);

}


/*
 * Make a treasure object
 *
 * The location must be a legal, clean, floor grid.
 */
bool make_gold(object_type *j_ptr)
{
	int sval;
	int k_idx;
	s32b base;


	/* Hack -- Pick a Treasure variety */
	sval = ((randint(object_level + 2) + 2) / 2);

	/* Apply "extra" magic */
	if (rand_int(GREAT_OBJ) == 0)
	{
		sval += randint(object_level + 1);
	}

	/* Hack -- Creeping Coins only generate "themselves" */
	if (coin_type) sval = coin_type;

	/* Do not create "illegal" Treasure Types */
	if (sval > MAX_GOLD) sval = MAX_GOLD;

	k_idx = lookup_kind(TV_GOLD, sval);

	/* Prepare a gold object */
	object_prep(j_ptr, k_idx);

	/* Hack -- Base coin cost */
	base = k_info[k_idx].cost;

	/* Determine how much the treasure is "worth" */
	j_ptr->pval = (base + (8L * randint(base)) + randint(8));

	/*chests containing gold are very lucritive*/
	if (object_generation_mode > 0)
	{
		j_ptr->pval += ((randint(4) + randint(4) + object_level / 4 ) * 50);
	}

	/* Success */
	return (TRUE);
}



/*
 * Let the floor carry an object
 */
s16b floor_carry(int y, int x, object_type *j_ptr)
{
	int n = 0;

	s16b o_idx;

	s16b this_o_idx, next_o_idx = 0;


	/* Scan objects in that grid for combination */
	for (this_o_idx = cave_o_idx[y][x]; this_o_idx; this_o_idx = next_o_idx)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[this_o_idx];

		/* Get the next object */
		next_o_idx = o_ptr->next_o_idx;

		/* Check for combination */
		if (object_similar(o_ptr, j_ptr))
		{
			/* Combine the items */
			object_absorb(o_ptr, j_ptr);

			/* Result */
			return (this_o_idx);
		}

		/* Count objects */
		n++;
	}

	/* The stack is already too large */
	if (n > MAX_FLOOR_STACK) return (0);

	/* Option -- disallow stacking */
	if (adult_no_stacking && n) return (0);

	/* Make an object */
	o_idx = o_pop();

	/* Success */
	if (o_idx)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[o_idx];

		/* Structure Copy */
		object_copy(o_ptr, j_ptr);

		/* Location */
		o_ptr->iy = y;
		o_ptr->ix = x;

		/* Forget monster */
		o_ptr->held_m_idx = 0;

		/* Link the object to the pile */
		o_ptr->next_o_idx = cave_o_idx[y][x];

		/* Link the floor to the object */
		cave_o_idx[y][x] = o_idx;

		/* Rearrange to reflect squelching */
		rearrange_stack(y, x);

		/* Notice */
		note_spot(y, x);

		/* Redraw */
		lite_spot(y, x);
	}

	/* Result */
	return (o_idx);
}


/*
 * Let an object fall to the ground at or near a location.
 *
 * The initial location is assumed to be "in_bounds_fully()".
 *
 * This function takes a parameter "chance".  This is the percentage
 * chance that the item will "disappear" instead of drop.  If the object
 * has been thrown, then this is the chance of disappearance on contact.
 *
 * Hack -- this function uses "chance" to determine if it should produce
 * some form of "description" of the drop event (under the player).
 *
 * We check several locations to see if we can find a location at which
 * the object can combine, stack, or be placed.  Artifacts will try very
 * hard to be placed, including "teleporting" to a useful grid if needed.
 */
void drop_near(object_type *j_ptr, int chance, int y, int x)
{
	int i, k, d, s;

	int bs, bn;
	int by, bx;
	int dy, dx;
	int ty, tx;

	object_type *o_ptr;

	char o_name[80];

	bool flag = FALSE;

	bool plural = FALSE;


	/* Extract plural */
	if (j_ptr->number != 1) plural = TRUE;

	/* Describe object */
	object_desc(o_name, sizeof(o_name), j_ptr, FALSE, 0);


	/* Handle normal "breakage" */
	if (!artifact_p(j_ptr) && (rand_int(100) < chance))
	{
		/* Message */
		msg_format("The %s disappear%s.",
		           o_name, (plural ? "" : "s"));

		/* Debug */
		if (p_ptr->wizard) msg_print("Breakage (breakage).");

		/* Failure */
		return;
	}


	/* Score */
	bs = -1;

	/* Picker */
	bn = 0;

	/* Default */
	by = y;
	bx = x;

	/* Scan local grids */
	for (dy = -3; dy <= 3; dy++)
	{
		/* Scan local grids */
		for (dx = -3; dx <= 3; dx++)
		{
			bool comb = FALSE;

			/* Calculate actual distance */
			d = (dy * dy) + (dx * dx);

			/* Ignore distant grids */
			if (d > 10) continue;

			/* Location */
			ty = y + dy;
			tx = x + dx;

			/* Skip illegal grids */
			if (!in_bounds_fully(ty, tx)) continue;

			/* Require line of sight */
			if (!los(y, x, ty, tx)) continue;

			/* Require floor space */
			if (cave_feat[ty][tx] != FEAT_FLOOR) continue;

			/* No objects */
			k = 0;

			/* Scan objects in that grid */
			for (o_ptr = get_first_object(ty, tx); o_ptr; o_ptr = get_next_object(o_ptr))
			{
				/* Check for possible combination */
				if (object_similar(o_ptr, j_ptr)) comb = TRUE;

				/* Count objects */
				k++;
			}

			/* Add new object */
			if (!comb) k++;

			/* Option -- disallow stacking */
			if (adult_no_stacking && (k > 1)) continue;

			/* Paranoia */
			if (k > MAX_FLOOR_STACK) continue;

			/* Calculate score */
			s = 1000 - (d + k * 5);

			/* Skip bad values */
			if (s < bs) continue;

			/* New best value */
			if (s > bs) bn = 0;

			/* Apply the randomizer to equivalent values */
			if ((++bn >= 2) && (rand_int(bn) != 0)) continue;

			/* Keep score */
			bs = s;

			/* Track it */
			by = ty;
			bx = tx;

			/* Okay */
			flag = TRUE;
		}
	}


	/* Handle lack of space */
	if (!flag && !artifact_p(j_ptr))
	{
		/* Message */
		msg_format("The %s disappear%s.",
		           o_name, (plural ? "" : "s"));

		/* Debug */
		if (p_ptr->wizard) msg_print("Breakage (no floor space).");

		/* Failure */
		return;
	}


	/* Find a grid */
	for (i = 0; !flag; i++)
	{
		/* Bounce around */
		if (i < 1000)
		{
			ty = rand_spread(by, 1);
			tx = rand_spread(bx, 1);
		}

		/* Random locations */
		else
		{
			ty = rand_int(p_ptr->cur_map_hgt);
			tx = rand_int(p_ptr->cur_map_wid);
		}

		/* Require floor space */
		if (cave_feat[ty][tx] != FEAT_FLOOR) continue;

		/* Bounce to that location */
		by = ty;
		bx = tx;

		/* Require floor space */
		if (!cave_clean_bold(by, bx)) continue;

		/* Okay */
		flag = TRUE;
	}


	/* Give it to the floor */
	if (!floor_carry(by, bx, j_ptr))
	{
		/* Message */
		msg_format("The %s disappear%s.",
		           o_name, (plural ? "" : "s"));

		/* Debug */
		if (p_ptr->wizard) msg_print("Breakage (too many objects).");

		/* Hack -- Preserve artifacts */
		a_info[j_ptr->name1].cur_num = 0;

		/* Failure */
		return;
	}


	/* Sound */
	sound(MSG_DROP);

	/* Mega-Hack -- no message if "dropped" by player */
	/* Message when an object falls under the player */
	if (chance && (cave_m_idx[by][bx] < 0))
	{
		msg_print("You feel something roll beneath your feet.");
	}
}


/*
 * Scatter some "great" objects near the player
 */
void acquirement(int y1, int x1, int num, bool great)
{
	object_type *i_ptr;
	object_type object_type_body;

	/* Acquirement */
	while (num--)
	{
		/* Get local object */
		i_ptr = &object_type_body;

		/* Wipe the object */
		object_wipe(i_ptr);

		/* Make a good (or great) object (if possible) */
		if (!make_object(i_ptr, TRUE, great, DROP_TYPE_UNTHEMED)) continue;

		/* Drop the object */
		drop_near(i_ptr, -1, y1, x1);
	}
}


/*
 * Attempt to place an object (normal or good/great) at the given location.
 */
void place_object(int y, int x, bool good, bool great, int droptype)
{
	object_type *i_ptr;
	object_type object_type_body;

	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Hack -- clean floor space */
	if (!cave_clean_bold(y, x)) return;

	/* Get local object */
	i_ptr = &object_type_body;

	/* Wipe the object */
	object_wipe(i_ptr);

	/* Make an object (if possible) */
	while (!make_object(i_ptr, good, great, droptype)) continue;

	/* Give it to the floor */
	if (!floor_carry(y, x, i_ptr))
	{
		/* Hack -- Preserve artifacts */
		a_info[i_ptr->name1].cur_num = 0;
	}
}

/*
 * Attempt to place a quest_chest at the given location.
 */
void place_quest_artifact(int y, int x)
{
	object_type *i_ptr;
	object_type object_type_body;

	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Hack -- clean floor space */
	if (!cave_clean_bold(y, x)) return;

	/* Get local object */
	i_ptr = &object_type_body;

	/* Wipe the object */
	object_wipe(i_ptr);

	/* Make a quest artifact (should never fail) */
	create_quest_artifact(i_ptr);

	/* Give it to the floor */
	floor_carry(y, x, i_ptr);

}



/*
 * Places a treasure (Gold or Gems) at given location
 */
void place_gold(int y, int x)
{
	object_type *i_ptr;
	object_type object_type_body;

	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Require clean floor space */
	if (!cave_clean_bold(y, x)) return;

	/* Get local object */
	i_ptr = &object_type_body;

	/* Wipe the object */
	object_wipe(i_ptr);

	/* Make some gold */
	if (make_gold(i_ptr))
	{
		/* Give it to the floor */
		(void)floor_carry(y, x, i_ptr);
	}
}



/*
 * Hack -- instantiate a trap
 *
 * XXX XXX XXX This routine should be redone to reflect trap "level".
 * That is, it does not make sense to have spiked pits at 50 feet.
 * Actually, it is not this routine, but the "trap instantiation"
 * code, which should also check for "trap doors" on quest levels.
 */
void pick_trap(int y, int x)
{
	int feat;

	/* Paranoia */
	if (cave_feat[y][x] != FEAT_INVIS) return;

	/* Pick a trap */
	while (1)
	{
		/* Hack -- pick a trap */
		feat = FEAT_TRAP_HEAD + rand_int(17);

		/* HACK - no trap doors on quest levels  */
		if ((feat == FEAT_TRAP_HEAD + 0x01) && (quest_check(p_ptr->depth))) continue;

		/* Hack -- no trap doors on the deepest level */
		if ((feat == FEAT_TRAP_HEAD + 0x01) && (p_ptr->depth >= MAX_DEPTH-1)) continue;

		/*Hack - no summoing traps on themed levels*/
		if ((feat == FEAT_TRAP_HEAD + 0x05) && (feeling >= LEV_THEME_HEAD)) continue;

		/* Done */
		break;
	}

	/* Activate the trap */
	cave_set_feat(y, x, feat);
}



/*
 * Places a random trap at the given location.
 *
 * The location must be a legal, naked, floor grid.
 *
 * Note that all traps start out as "invisible" and "untyped", and then
 * when they are "discovered" (by detecting them or setting them off),
 * the trap is "instantiated" as a visible, "typed", trap.
 */
void place_trap(int y, int x)
{
	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Require empty, clean, floor grid */
	if (!cave_naked_bold(y, x)) return;

	/* Place an invisible trap */
	cave_set_feat(y, x, FEAT_INVIS);
}


/*
 * Place a secret door at the given location
 */
void place_secret_door(int y, int x)
{
	/* Create secret door */
	cave_set_feat(y, x, FEAT_SECRET);
}


/*
 * Place a random type of closed door at the given location.
 */
void place_closed_door(int y, int x)
{
	int tmp;

	/* Choose an object */
	tmp = rand_int(400);

	/* Closed doors (300/400) */
	if (tmp < 300)
	{
		/* Create closed door */
		cave_set_feat(y, x, FEAT_DOOR_HEAD + 0x00);
	}

	/* Locked doors (99/400) */
	else if (tmp < 399)
	{
		/* Create locked door */
		cave_set_feat(y, x, FEAT_DOOR_HEAD + randint(7));
	}

	/* Stuck doors (1/400) */
	else
	{
		/* Create jammed door */
		cave_set_feat(y, x, FEAT_DOOR_HEAD + 0x08 + rand_int(8));
	}
}


/*
 * Place a random type of door at the given location.
 */
void place_random_door(int y, int x)
{
	int tmp;

	/* Choose an object */
	tmp = rand_int(1000);

	/* Open doors (300/1000) */
	if (tmp < 300)
	{
		/* Create open door */
		cave_set_feat(y, x, FEAT_OPEN);
	}

	/* Broken doors (100/1000) */
	else if (tmp < 400)
	{
		/* Create broken door */
		cave_set_feat(y, x, FEAT_BROKEN);
	}

	/* Secret doors (200/1000) */
	else if (tmp < 600)
	{
		/* Create secret door */
		cave_set_feat(y, x, FEAT_SECRET);
	}

	/* Closed, locked, or stuck doors (400/1000) */
	else
	{
		/* Create closed door */
		place_closed_door(y, x);
	}
}


/*
 * Describe the charges on an item in the inventory.
 */
void inven_item_charges(int item)
{
	object_type *o_ptr = &inventory[item];

	/* Require staff/wand */
	if ((o_ptr->tval != TV_STAFF) && (o_ptr->tval != TV_WAND)) return;

	/* Require known item */
	if (!object_known_p(o_ptr)) return;

	/* Print a message */
	msg_format("You have %d charge%s remaining.", o_ptr->pval,
	           (o_ptr->pval != 1) ? "s" : "");
}


/*
 * Describe an item in the inventory.
 */
void inven_item_describe(int item)
{
	object_type *o_ptr = &inventory[item];

	char o_name[80];

	if (artifact_p(o_ptr) && object_known_p(o_ptr))
	{
		/* Get a description */
		object_desc(o_name, sizeof(o_name), o_ptr, FALSE, 3);

		/* Print a message */
		msg_format("You no longer have the %s (%c).", o_name, index_to_label(item));
	}
	else
	{
		/* Get a description */
		object_desc(o_name, sizeof(o_name), o_ptr, TRUE, 3);

		/* Print a message */
		msg_format("You have %s (%c).", o_name, index_to_label(item));
	}
}


/*
 * Increase the "number" of an item in the inventory
 */
void inven_item_increase(int item, int num)
{
	object_type *o_ptr = &inventory[item];

	/* Apply */
	num += o_ptr->number;

	/* Bounds check */
	if (num > 255) num = 255;
	else if (num < 0) num = 0;

	/* Un-apply */
	num -= o_ptr->number;

	/* Change the number and weight */
	if (num)
	{
		/* Add the number */
		o_ptr->number += num;

		/* Add the weight */
		p_ptr->total_weight += (num * o_ptr->weight);

		/* Recalculate bonuses */
		p_ptr->update |= (PU_BONUS);

		/* Recalculate mana XXX */
		p_ptr->update |= (PU_MANA);

		/* Combine the pack */
		p_ptr->notice |= (PN_COMBINE);

		/* Window stuff */
		p_ptr->window |= (PW_INVEN | PW_EQUIP);
	}
}


/*
 * Erase an inventory slot if it has no more items
 */
void inven_item_optimize(int item)
{
	object_type *o_ptr = &inventory[item];

	/* Only optimize real items */
	if (!o_ptr->k_idx) return;

	/* Only optimize empty items */
	if (o_ptr->number) return;

	/* The item is in the pack */
	if (item < INVEN_WIELD)
	{
		int i;

		/* One less item */
		p_ptr->inven_cnt--;

		/* Slide everything down */
		for (i = item; i < INVEN_PACK; i++)
		{
			/* Hack -- slide object */
			COPY(&inventory[i], &inventory[i+1], object_type);
		}

		/* Hack -- wipe hole */
		(void)WIPE(&inventory[i], object_type);

		/* Window stuff */
		p_ptr->window |= (PW_INVEN);
	}

	/* The item is being wielded */
	else
	{
		/* One less item */
		p_ptr->equip_cnt--;

		/* Erase the empty slot */
		object_wipe(&inventory[item]);

		/* Recalculate bonuses */
		p_ptr->update |= (PU_BONUS);

		/* Recalculate torch */
		p_ptr->update |= (PU_TORCH);

		/* Recalculate mana XXX */
		p_ptr->update |= (PU_MANA);

		/* Window stuff */
		p_ptr->window |= (PW_EQUIP | PW_PLAYER_0 | PW_PLAYER_1);

		p_ptr->redraw |= (PR_EQUIPPY | PR_RESIST);
	}
}


/*
 * Describe the charges on an item on the floor.
 */
void floor_item_charges(int item)
{
	object_type *o_ptr = &o_list[item];

	/* Require staff/wand */
	if ((o_ptr->tval != TV_STAFF) && (o_ptr->tval != TV_WAND)) return;

	/* Require known item */
	if (!object_known_p(o_ptr)) return;

	/* Print a message */
	msg_format("There are %d charge%s remaining.", o_ptr->pval,
	           (o_ptr->pval != 1) ? "s" : "");
}



/*
 * Describe an item in the inventory.
 */
void floor_item_describe(int item)
{
	object_type *o_ptr = &o_list[item];

	char o_name[80];

	/* Get a description */
	object_desc(o_name, sizeof(o_name), o_ptr, TRUE, 3);

	/* Print a message */
	msg_format("You see %s.", o_name);
}


/*
 * Increase the "number" of an item on the floor
 */
void floor_item_increase(int item, int num)
{
	object_type *o_ptr = &o_list[item];

	/* Apply */
	num += o_ptr->number;

	/* Bounds check */
	if (num > 255) num = 255;
	else if (num < 0) num = 0;

	/* Un-apply */
	num -= o_ptr->number;

	/* Change the number */
	o_ptr->number += num;
}


/*
 * Optimize an item on the floor (destroy "empty" items)
 */
void floor_item_optimize(int item)
{
	object_type *o_ptr = &o_list[item];

	/* Paranoia -- be sure it exists */
	if (!o_ptr->k_idx) return;

	/* Only optimize empty items */
	if (o_ptr->number) return;

	/* Delete the object */
	delete_object_idx(item);
}


/*
 * Check if we have space for an item in the pack without overflow
 */
bool inven_carry_okay(const object_type *o_ptr)
{
	int j;

	/* Empty slot? */
	if (p_ptr->inven_cnt < INVEN_PACK) return (TRUE);

	/* Similar slot? */
	for (j = 0; j < INVEN_PACK; j++)
	{
		object_type *j_ptr = &inventory[j];

		/* Skip non-objects */
		if (!j_ptr->k_idx) continue;

		/* Check if the two items can be combined */
		if (object_similar(j_ptr, o_ptr)) return (TRUE);
	}

	/* Nope */
	return (FALSE);
}

/*
 * Add an item to the players inventory, and return the slot used.
 *
 * If the new item can combine with an existing item in the inventory,
 * it will do so, using "object_similar()" and "object_absorb()", else,
 * the item will be placed into the "proper" location in the inventory.
 *
 * This function can be used to "over-fill" the player's pack, but only
 * once, and such an action must trigger the "overflow" code immediately.
 * Note that when the pack is being "over-filled", the new item must be
 * placed into the "overflow" slot, and the "overflow" must take place
 * before the pack is reordered, but (optionally) after the pack is
 * combined.  This may be tricky.  See "dungeon.c" for info.
 *
 * Note that this code must remove any location/stack information
 * from the object once it is placed into the inventory.
 */
s16b inven_carry(object_type *o_ptr)
{
	int i, j, k;
	int n = -1;

	object_type *j_ptr;

	/*paranoia, don't pick up "&nothings"*/
	if (!o_ptr->k_idx) return (-1);

	/* Check for combining */
	for (j = 0; j < INVEN_PACK; j++)
	{
		j_ptr = &inventory[j];

		/* Skip non-objects */
		if (!j_ptr->k_idx) continue;

		/* Hack -- track last item */
		n = j;

		/* Check if the two items can be combined */
		if (object_similar(j_ptr, o_ptr))
		{
			/* Combine the items */
			object_absorb(j_ptr, o_ptr);

			/* Increase the weight */
			p_ptr->total_weight += (o_ptr->number * o_ptr->weight);

			/* Recalculate bonuses */
			p_ptr->update |= (PU_BONUS);

			/* Window stuff */
			p_ptr->window |= (PW_INVEN);

			/* Success */
			return (j);
		}
	}


	/* Paranoia */
	if (p_ptr->inven_cnt > INVEN_PACK) return (-1);

	/* Find an empty slot */
	for (j = 0; j <= INVEN_PACK; j++)
	{
		j_ptr = &inventory[j];

		/* Use it if found */
		if (!j_ptr->k_idx) break;
	}

	/* Use that slot */
	i = j;

	/* Apply an autoinscription */
	apply_autoinscription(o_ptr);

	/* Reorder the pack */
	if (i < INVEN_PACK)
	{
		s32b o_value, j_value;

		/* Get the "value" of the item */
		o_value = object_value(o_ptr);

		/* Scan every occupied slot */
		for (j = 0; j < INVEN_PACK; j++)
		{
			j_ptr = &inventory[j];

			/* Use empty slots */
			if (!j_ptr->k_idx) break;

			/* Hack -- readable books always come first */
			if ((o_ptr->tval == cp_ptr->spell_book) &&
			    (j_ptr->tval != cp_ptr->spell_book)) break;
			if ((j_ptr->tval == cp_ptr->spell_book) &&
			    (o_ptr->tval != cp_ptr->spell_book)) continue;

			/* Objects sort by decreasing type */
			if (o_ptr->tval > j_ptr->tval) break;
			if (o_ptr->tval < j_ptr->tval) continue;

			/* Non-aware (flavored) items always come last */
			if (!object_aware_p(o_ptr)) continue;
			if (!object_aware_p(j_ptr)) break;

			/* Objects sort by increasing sval */
			if (o_ptr->sval < j_ptr->sval) break;
			if (o_ptr->sval > j_ptr->sval) continue;

			/* Unidentified objects always come last */
			if (!object_known_p(o_ptr)) continue;
			if (!object_known_p(j_ptr)) break;

			/* Lites sort by decreasing fuel */
			if (o_ptr->tval == TV_LITE)
			{
				if (o_ptr->timeout > j_ptr->timeout) break;
				if (o_ptr->timeout < j_ptr->timeout) continue;
			}

			/* Determine the "value" of the pack item */
			j_value = object_value(j_ptr);

			/* Objects sort by decreasing value */
			if (o_value > j_value) break;
			if (o_value < j_value) continue;
		}

		/* Use that slot */
		i = j;

		/* Slide objects */
		for (k = n; k >= i; k--)
		{
			/* Hack -- Slide the item */
			object_copy(&inventory[k+1], &inventory[k]);
		}

		/* Wipe the empty slot */
		object_wipe(&inventory[i]);
	}


	/* Copy the item */
	object_copy(&inventory[i], o_ptr);

	/* Get the new object */
	j_ptr = &inventory[i];

	/* Forget stack */
	j_ptr->next_o_idx = 0;

	/* Forget monster */
	j_ptr->held_m_idx = 0;

	/* Forget location */
	j_ptr->iy = j_ptr->ix = 0;

	/* No longer marked */
	j_ptr->marked = FALSE;

	/* Increase the weight */
	p_ptr->total_weight += (j_ptr->number * j_ptr->weight);

	/* Count the items */
	p_ptr->inven_cnt++;

	/* Recalculate bonuses */
	p_ptr->update |= (PU_BONUS);

	/* Combine and Reorder pack */
	p_ptr->notice |= (PN_COMBINE | PN_REORDER);

	/* Window stuff */
	p_ptr->window |= (PW_INVEN);

	/* Return the slot */
	return (i);
}


/*
 * Take off (some of) a non-cursed equipment item
 *
 * Note that only one item at a time can be wielded per slot.
 *
 * Note that taking off an item when "full" may cause that item
 * to fall to the ground.
 *
 * Return the inventory slot into which the item is placed.
 */
s16b inven_takeoff(int item, int amt)
{
	int slot;

	object_type *o_ptr;

	object_type *i_ptr;
	object_type object_type_body;

	cptr act;

	char o_name[80];


	/* Get the item to take off */
	o_ptr = &inventory[item];

	/* Paranoia */
	if (amt <= 0) return (-1);

	/* Verify */
	if (amt > o_ptr->number) amt = o_ptr->number;

	/* Get local object */
	i_ptr = &object_type_body;

	/* Obtain a local object */
	object_copy(i_ptr, o_ptr);

	/* Modify quantity */
	i_ptr->number = amt;

	/* Describe the object */
	object_desc(o_name, sizeof(o_name), i_ptr, TRUE, 3);

	/* Took off weapon */
	if (item == INVEN_WIELD)
	{
		act = "You were wielding";
	}

	/* Took off bow */
	else if (item == INVEN_BOW)
	{
		act = "You were holding";
	}

	/* Took off light */
	else if (item == INVEN_LITE)
	{
		act = "You were holding";
	}

	/* Took off something */
	else
	{
		act = "You were wearing";
	}

	/* Modify, Optimize */
	inven_item_increase(item, -amt);
	inven_item_optimize(item);

	/* Carry the object */
	slot = inven_carry(i_ptr);

	/* Message */
	msg_format("%s %s (%c).", act, o_name, index_to_label(slot));

	/* Return slot */
	return (slot);
}


/*
 * Drop (some of) a non-cursed inventory/equipment item
 *
 * The object will be dropped "near" the current location
 */
void inven_drop(int item, int amt)
{
	int py = p_ptr->py;
	int px = p_ptr->px;

	object_type *o_ptr;

	object_type *i_ptr;
	object_type object_type_body;

	char o_name[120];

	/* Get the original object */
	o_ptr = &inventory[item];

	/* Error check */
	if (amt <= 0) return;

	/* Not too many */
	if (amt > o_ptr->number) amt = o_ptr->number;


	/* Take off equipment */
	if (item >= INVEN_WIELD)
	{
		/* Take off first */
		item = inven_takeoff(item, amt);

		/* Get the original object */
		o_ptr = &inventory[item];
	}


	/* Get local object */
	i_ptr = &object_type_body;

	/* Obtain local object */
	object_copy(i_ptr, o_ptr);

	/* Distribute charges of wands or rods */
	distribute_charges(o_ptr, i_ptr, amt);

	/* Modify quantity */
	i_ptr->number = amt;

	/* Describe local object */
	object_desc(o_name, sizeof(o_name), i_ptr, TRUE, 3);

	/* Message */
	msg_format("You drop %s (%c).", o_name, index_to_label(item));

	/* Drop it near the player */
	drop_near(i_ptr, 0, py, px);

	/* Modify, Describe, Optimize */
	inven_item_increase(item, -amt);
	inven_item_describe(item);
	inven_item_optimize(item);
}



/*
 * Combine items in the pack
 *
 * Note special handling of the "overflow" slot
 */
void combine_pack(void)
{
	int i, j, k;

	object_type *o_ptr;
	object_type *j_ptr;

	bool flag = FALSE;


	/* Combine the pack (backwards) */
	for (i = INVEN_PACK; i > 0; i--)
	{
		/* Get the item */
		o_ptr = &inventory[i];

		/* Skip empty items */
		if (!o_ptr->k_idx) continue;

		/* Scan the items above that item */
		for (j = 0; j < i; j++)
		{
			/* Get the item */
			j_ptr = &inventory[j];

			/* Skip empty items */
			if (!j_ptr->k_idx) continue;

			/* Can we drop "o_ptr" onto "j_ptr"? */
			if (object_similar(j_ptr, o_ptr))
			{
				/* Take note */
				flag = TRUE;

				/* Add together the item counts */
				object_absorb(j_ptr, o_ptr);

				/* One object is gone */
				p_ptr->inven_cnt--;

				/* Slide everything down */
				for (k = i; k < INVEN_PACK; k++)
				{
					/* Hack -- slide object */
					COPY(&inventory[k], &inventory[k+1], object_type);
				}

				/* Hack -- wipe hole */
				object_wipe(&inventory[k]);

				/* Window stuff */
				p_ptr->window |= (PW_INVEN);

				/* Done */
				break;
			}
		}
	}

	/* Message */
	if (flag) msg_print("You combine some items in your pack.");
}


/*
 * Reorder items in the pack
 *
 * Note special handling of the "overflow" slot
 */
void reorder_pack(void)
{
	int i, j, k;

	s32b o_value;
	s32b j_value;

	object_type *o_ptr;
	object_type *j_ptr;

	object_type *i_ptr;
	object_type object_type_body;

	bool flag = FALSE;


	/* Re-order the pack (forwards) */
	for (i = 0; i < INVEN_PACK; i++)
	{
		/* Mega-Hack -- allow "proper" over-flow */
		if ((i == INVEN_PACK) && (p_ptr->inven_cnt == INVEN_PACK)) break;

		/* Get the item */
		o_ptr = &inventory[i];

		/* Skip empty slots */
		if (!o_ptr->k_idx) continue;

		/* Get the "value" of the item */
		o_value = object_value(o_ptr);

		/* Scan every occupied slot */
		for (j = 0; j < INVEN_PACK; j++)
		{
			/* Get the item already there */
			j_ptr = &inventory[j];

			/* Use empty slots */
			if (!j_ptr->k_idx) break;

			/* Hack -- readable books always come first */
			if ((o_ptr->tval == cp_ptr->spell_book) &&
			    (j_ptr->tval != cp_ptr->spell_book)) break;
			if ((j_ptr->tval == cp_ptr->spell_book) &&
			    (o_ptr->tval != cp_ptr->spell_book)) continue;

			/* Objects sort by decreasing type */
			if (o_ptr->tval > j_ptr->tval) break;
			if (o_ptr->tval < j_ptr->tval) continue;

			/* Non-aware (flavored) items always come last */
			if (!object_aware_p(o_ptr)) continue;
			if (!object_aware_p(j_ptr)) break;

			/* Objects sort by increasing sval */
			if (o_ptr->sval < j_ptr->sval) break;
			if (o_ptr->sval > j_ptr->sval) continue;

			/* Unidentified objects always come last */
			if (!object_known_p(o_ptr)) continue;
			if (!object_known_p(j_ptr)) break;

			/* Lites sort by decreasing fuel */
			if (o_ptr->tval == TV_LITE)
			{
				if (o_ptr->timeout > j_ptr->timeout) break;
				if (o_ptr->timeout < j_ptr->timeout) continue;
			}

			/* Determine the "value" of the pack item */
			j_value = object_value(j_ptr);

			/* Objects sort by decreasing value */
			if (o_value > j_value) break;
			if (o_value < j_value) continue;
		}

		/* Never move down */
		if (j >= i) continue;

		/* Take note */
		flag = TRUE;

		/* Get local object */
		i_ptr = &object_type_body;

		/* Save a copy of the moving item */
		object_copy(i_ptr, &inventory[i]);

		/* Slide the objects */
		for (k = i; k > j; k--)
		{
			/* Slide the item */
			object_copy(&inventory[k], &inventory[k-1]);
		}

		/* Insert the moving item */
		object_copy(&inventory[j], i_ptr);

		/* Window stuff */
		p_ptr->window |= (PW_INVEN);
	}

	/* Message */
	if (flag) msg_print("You reorder some items in your pack.");
}




/*
 * Distribute charges of rods or wands.
 *
 * o_ptr = source item
 * q_ptr = target item, must be of the same type as o_ptr
 * amt	 = number of items that are transfered
 */
void distribute_charges(object_type *o_ptr, object_type *i_ptr, int amt)
{
	/*
	 * Hack -- If rods, wands or staffs are dropped, the total maximum timeout or
	 * charges need to be allocated between the two stacks.   If all the items
	 * are being dropped, it makes for a neater message to leave the original
	 * stack's pval alone. -LM-
	 */
	if ((o_ptr->tval == TV_WAND) ||
		(o_ptr->tval == TV_ROD) ||
		(o_ptr->tval == TV_STAFF))
	{
		i_ptr->pval = o_ptr->pval * amt / o_ptr->number;

		if (amt < o_ptr->number) o_ptr->pval -= i_ptr->pval;

		/* Hack -- Rods also need to have their timeouts distributed.  The
		 * dropped stack will accept all time remaining to charge up to its
		 * maximum.
		 */
		if ((o_ptr->tval == TV_ROD) && (o_ptr->timeout))
		{
			if (i_ptr->pval > o_ptr->timeout)
				i_ptr->timeout = o_ptr->timeout;
			else
				i_ptr->timeout = i_ptr->pval;

			if (amt < o_ptr->number)
				o_ptr->timeout -= i_ptr->timeout;
		}
	}
}

void reduce_charges(object_type *o_ptr, int amt)
{
	/*
	 * Hack -- If rods or wand are destroyed, the total maximum timeout or
	 * charges of the stack needs to be reduced, unless all the items are
	 * being destroyed. -LM-
	 */
	if (((o_ptr->tval == TV_WAND) ||
		 (o_ptr->tval == TV_ROD) ||
		 (o_ptr->tval == TV_STAFF)) &&
		(amt < o_ptr->number))
	{
		o_ptr->pval -= o_ptr->pval * amt / o_ptr->number;
	}
}

/* Steal from monster and make an object in the player inventory.
 * This whole function is basically an abbreviated object creation
 * routine.  Much of the object creation code can't be called because
 * they all assume the object is destined for either the stores or
 * the dungeon floor.  This item is being created and handed directly
 * to the player.  We must create the item, give gold to
 * the player or create an item, update the lore, check if autosquelch
 * is appropriate, make sure no artifacts are squelched, place the item directly
 * in the player's inventory, if there is room.  If not, drop it to the
 * floor, and finally, wipe the object. -JG
 */


void steal_object_from_monster(int y, int x)
{
	monster_type *m_ptr = &mon_list[cave_m_idx[y][x]];
	monster_race *r_ptr = &r_info[m_ptr->r_idx];
	char o_name[80];

	bool chest = (r_ptr->flags1 & (RF1_DROP_CHEST)) ? TRUE : FALSE;
	bool good = (r_ptr->flags1 & (RF1_DROP_GOOD)) ? TRUE : FALSE;
	bool great = (r_ptr->flags1 & (RF1_DROP_GREAT)) ? TRUE : FALSE;

	bool do_gold = (!(r_ptr->flags1 & (RF1_ONLY_ITEM)));
	bool do_item = (!(r_ptr->flags1 & (RF1_ONLY_GOLD)));

	object_type *i_ptr;
	object_type object_type_body;

	/* Average dungeon and monster levels */
	object_level = (p_ptr->depth + r_ptr->level) / 2;

	/* Get local object */
	i_ptr = &object_type_body;

	/* Wipe the object */
	object_wipe(i_ptr);

	/* Make Gold */
	if (do_gold && (!chest) && (!do_item || (rand_int(100) < 50)))
	{

		/*get coin type "flavor" if appropriate*/
		coin_type = get_coin_type(r_ptr);

		/* Make some gold */
		while (!make_gold(i_ptr)) continue;

		/* Describe the object */
		object_desc(o_name, sizeof(o_name), i_ptr, TRUE, 3);

		/* Message */
		msg_format("You have stolen %ld gold pieces worth of %s.",
			           (long)i_ptr->pval, o_name);

		/* Collect the gold */
		p_ptr->au += i_ptr->pval;

		/* Delete the gold */
		object_wipe(i_ptr);

		/* Redraw gold */
		p_ptr->redraw |= (PR_GOLD);

		/* Window stuff */
		p_ptr->window |= (PW_PLAYER_0 | PW_PLAYER_1);

		/*update the monster lore*/
		lore_treasure(cave_m_idx[y][x], 0, 1);

		/* Reset "coin" type */
		coin_type = 0;

	}

	/* Make Object */
	else
	{
		bool sq_flag = FALSE;

		/*Make an object, but make a chest if that is the theme*/
		if (chest)
		{
			while (!make_object(i_ptr, good, great, DROP_TYPE_CHEST)) continue;

			/*clear the allocation table when done*/
		}

		/* Make an object */
		else while (!make_object(i_ptr, good, great,DROP_TYPE_UNTHEMED)) continue;

		/* Describe the object */
		object_desc(o_name, sizeof(o_name), i_ptr, TRUE, 0);

		/*update the monster lore*/
		lore_treasure(cave_m_idx[y][x], 1, 0);

		/*does the player want to squelch the item?*/
		sq_flag = ((k_info[i_ptr->k_idx].squelch == SQUELCH_ALWAYS) &&
 				   (k_info[i_ptr->k_idx].aware));

		if (!sq_flag)
		{
			/* Note that the pack is too full */
			if (!inven_carry_okay(i_ptr))
			{
				msg_format("You have no room in your backpack for %s.", o_name);

				msg_format("%s falls to the dungeon floor.", o_name);

				floor_carry(y, x, i_ptr);
			}

			/* Give it to the player */
			else
			{
				int item_new;

				/* Give it to the player */
				item_new = inven_carry(i_ptr);

				/* Message */
				msg_format("You have burgled %s (%c).",
			           o_name, index_to_label(item_new));
			}
		}

		/*squelch the item, unless artifact*/
		else if (artifact_p(i_ptr))
		{
			/* Mark the object as indestructible */
			i_ptr->discount = INSCRIP_INDESTRUCTIBLE;

			/* Update the name */
			object_desc(o_name, sizeof(o_name), i_ptr, TRUE, 0);

			/* Message */
			msg_format("You cannot squelch %s.", o_name);

			/* Now Check if the pack is too full */
			if (!inven_carry_okay(i_ptr))
			{
				msg_format("You have no room in your backpack for %s.", o_name);

				msg_format("%s falls the dungeon floor.", o_name);

				floor_carry(y, x, i_ptr);
			}

			/* Give it to the player */
			else
			{
				int item_new;

				/* Give it to the player */
				item_new = inven_carry(i_ptr);

				/* Message */
				msg_format("You have burgled %s (%c).",
			           o_name, index_to_label(item_new));
			}


		}

		/*squelch it*/
		else
		{

			/* At least let the player know they stole something */
			msg_format("You have burgled %s.{squelched}", o_name);

			/* Delete the object */
			object_wipe(i_ptr);

		}
	}

	/* Reset the object level */
	object_level = p_ptr->depth;

	/* Update monster recall window */
	if (p_ptr->monster_race_idx == m_ptr->r_idx)
	{
		/* Window stuff */
		p_ptr->window |= (PW_MONSTER);
	}

	return;
}


