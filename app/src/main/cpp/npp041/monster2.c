/* File: monster2.c */

/*
 * Copyright (c) 1997 Ben Harrison, James E. Wilson, Robert A. Koeneke
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.  Other copyrights may also apply.
 */

#include "angband.h"




/*
 * Return another race for a monster to polymorph into.  -LM-
 *
 * Perform a modified version of "get_mon_num()", with exact minimum and
 * maximum depths and preferred monster types.
 */
s16b poly_r_idx(const monster_type *m_ptr)
{
	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	s16b base_idx = m_ptr->r_idx;

	alloc_entry *table = alloc_race_table;

	int i, min_lev, max_lev, r_idx;
	long total, value;

	/* Source monster's level and symbol */
	int r_lev = r_ptr->level;
	char d_char = r_ptr->d_char;

	/* Hack -- Uniques and quest monsters never polymorph */
	if ((r_ptr->flags1 & (RF1_UNIQUE)) || (m_ptr->mflag & (MFLAG_QUEST)))
	{
		return (base_idx);
	}

	/* Allowable level of new monster */
	min_lev = (MAX(        1, r_lev - 1 - r_lev / 5));
	max_lev = (MIN(MAX_DEPTH, r_lev + 1 + r_lev / 5));

	/* Reset sum */
	total = 0L;

	/* Process probabilities */
	for (i = 0; i < alloc_race_size; i++)
	{
		/* Assume no probability */
		table[i].prob3 = 0;

		/* Ignore illegal monsters - only those that don't get generated. */
		if (!table[i].prob1) continue;

		/* Not below the minimum base depth */
		if (table[i].level < min_lev) continue;

		/* Not above the maximum base depth */
		if (table[i].level > max_lev) continue;

		/* Get the monster index */
		r_idx = table[i].index;

		/* We're polymorphing -- we don't want the same monster */
		if (r_idx == base_idx) continue;

		/* Do not polymorph into a quest monster */
		/* if ((q_idx) && (r_idx == q_idx)) continue; */

		/* Get the actual race */
		r_ptr = &r_info[r_idx];

		/* Hack -- No uniques */
		if (r_ptr->flags1 & (RF1_UNIQUE)) continue;

		/* Accept */
		table[i].prob3 = table[i].prob2;

		/* Bias against monsters far from initial monster's depth */
		if (table[i].level < (min_lev + r_lev) / 2) table[i].prob3 /= 4;
		if (table[i].level > (max_lev + r_lev) / 2) table[i].prob3 /= 4;

		/* Bias against monsters not of the same symbol */
		if (r_ptr->d_char != d_char) table[i].prob3 /= 4;

		/* Sum up probabilities */
		total += table[i].prob3;
	}

	/* No legal monsters */
	if (total == 0)
	{
		return (base_idx);
	}


	/* Pick a monster */
	value = rand_int(total);

	/* Find the monster */
	for (i = 0; i < alloc_race_size; i++)
	{
		/* Found the entry */
		if (value < table[i].prob3) break;

		/* Decrement */
		value = value - table[i].prob3;
	}

	/* Result */
	return (table[i].index);
}




/*
 * Delete a monster by index.
 *
 * When a monster is deleted, all of its objects are deleted.
 */
void delete_monster_idx(int i)
{
	int x, y;

	monster_type *m_ptr = &mon_list[i];

	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	monster_lore *l_ptr = &l_list[m_ptr->r_idx];

	s16b this_o_idx, next_o_idx = 0;


	/* Get location */
	y = m_ptr->fy;
	x = m_ptr->fx;


	/* Hack -- Reduce the racial counter */
	r_ptr->cur_num--;

	/* Hack -- count the number of "reproducers" */
	if (r_ptr->flags2 & (RF2_MULTIPLY)) num_repro--;

	/* Hack -- remove target monster */
	if (p_ptr->target_who == i) target_set_monster(0);

	/* Hack -- remove tracked monster */
	if (p_ptr->health_who == i) health_track(0);


	/* Monster is gone */
	cave_m_idx[y][x] = 0;

	/* Total Hack -- If the monster was a player ghost, remove it from
	 * the monster memory, ensure that it never appears again, and clear
	 * its bones file selector.
	 */
	if (r_ptr->flags2 & (RF2_PLAYER_GHOST))
	{
		l_ptr->sights = 0;
		l_ptr->pkills = 1;
		l_ptr->tkills = 0;
		bones_selector = 0;
	}

	/* Delete objects */
	for (this_o_idx = m_ptr->hold_o_idx; this_o_idx; this_o_idx = next_o_idx)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[this_o_idx];

		/* Get the next object */
		next_o_idx = o_ptr->next_o_idx;

		/* Hack -- efficiency */
		o_ptr->held_m_idx = 0;

		/* Delete the object */
		delete_object_idx(this_o_idx);
	}


	/* Wipe the Monster */
	(void)WIPE(m_ptr, monster_type);

	/* Count monsters */
	mon_cnt--;


	/* Visual update */
	lite_spot(y, x);
}


/*
 * Delete the monster, if any, at a given location
 */
void delete_monster(int y, int x)
{
	/* Paranoia */
	if (!in_bounds(y, x)) return;

	/* Delete the monster (if any) */
	if (cave_m_idx[y][x] > 0) delete_monster_idx(cave_m_idx[y][x]);
}


/*
 * Move an object from index i1 to index i2 in the object list
 */
static void compact_monsters_aux(int i1, int i2)
{
	int y, x;

	monster_type *m_ptr;

	s16b this_o_idx, next_o_idx = 0;


	/* Do nothing */
	if (i1 == i2) return;


	/* Old monster */
	m_ptr = &mon_list[i1];

	/* Location */
	y = m_ptr->fy;
	x = m_ptr->fx;

	/* Update the cave */
	cave_m_idx[y][x] = i2;

	/* Repair objects being carried by monster */
	for (this_o_idx = m_ptr->hold_o_idx; this_o_idx; this_o_idx = next_o_idx)
	{
		object_type *o_ptr;

		/* Get the object */
		o_ptr = &o_list[this_o_idx];

		/* Get the next object */
		next_o_idx = o_ptr->next_o_idx;

		/* Reset monster pointer */
		o_ptr->held_m_idx = i2;
	}

	/* Hack -- Update the target */
	if (p_ptr->target_who == i1) p_ptr->target_who = i2;

	/* Hack -- Update the health bar */
	if (p_ptr->health_who == i1) p_ptr->health_who = i2;

	/* Hack -- move monster */
	COPY(&mon_list[i2], &mon_list[i1], monster_type);

	/* Hack -- wipe hole */
	(void)WIPE(&mon_list[i1], monster_type);
}


/*
 * Compact and Reorder the monster list
 *
 * This function can be very dangerous, use with caution!
 *
 * When compacting monsters, we first delete far away monsters without
 * objects, starting with those of lowest level.  Then nearby monsters and
 * monsters with objects get compacted, then unique monsters, and only then
 * are quest monsters affected.  -LM-
 *
 * After "compacting" (if needed), we "reorder" the monsters into a more
 * compact order, and we reset the allocation info, and the "live" array.
 */

void compact_monsters(int size)
{
	int i, j, cnt;

	monster_type *m_ptr;
	monster_race *r_ptr;

	/* Paranoia -- refuse to wipe too many monsters at one time */
	if (size > z_info->m_max / 2) size = z_info->m_max / 2;

	/* Paranoia -- refuse to wipe too many monsters at one time */
	if (size > z_info->m_max / 2) size = z_info->m_max / 2;

	/* Compact */
	if (size)
	{
		/* Get quest monster index (if any) */
		/* int q_idx = q_info[quest_num(p_ptr->depth)].r_idx; */

		s16b *mon_lev;
		s16b *mon_index;

		/* Allocate the "mon_lev and mon_index" arrays */
		C_MAKE(mon_lev, mon_max, s16b);
		C_MAKE(mon_index, mon_max, s16b);

		/* Message */
		msg_print("Compacting monsters...");

		/* Redraw map */
		p_ptr->redraw |= (PR_MAP);

		/* Window stuff */
		p_ptr->window |= (PW_OVERHEAD);


		/* Scan the monster list */
		for (i = 1; i < mon_max; i++)
		{
			m_ptr = &mon_list[i];
			r_ptr = &r_info[m_ptr->r_idx];

			/* Dead monsters have minimal level (but are counted!) */
			if (!m_ptr->r_idx) mon_lev[i] = -1L;

			/* Get the monster level */
			else
			{
				mon_lev[i] = r_ptr->level;

				/* Quest monsters always get compacted last */
				if ((r_ptr->flags1 & (RF1_QUESTOR)) || (m_ptr->mflag & (MFLAG_QUEST)))
					mon_lev[i] += MAX_DEPTH * 3;

				/*same with active quest monsters*/
				else if (q_info[quest_num(p_ptr->depth)].mon_idx == m_ptr->r_idx)
					mon_lev[i] += MAX_DEPTH * 3;
				/* Uniques are protected */
				else if (r_ptr->flags1 & (RF1_UNIQUE)) mon_lev[i] += MAX_DEPTH * 2;

				/* Nearby monsters are protected */
				else if ((character_dungeon) && (m_ptr->cdis < MAX_SIGHT))
					mon_lev[i] += MAX_DEPTH;

				/* Monsters with objects are protected */
				else if (m_ptr->hold_o_idx) mon_lev[i] += MAX_DEPTH;
			}

			/* Save this monster index */
			mon_index[i] = i;
		}

	/* Sort all the monsters by (adjusted) level */
		for (i = 0; i < mon_max - 1; i++)
		{
			for (j = 0; j < mon_max - 1; j++)
			{
				int j1 = j;
				int j2 = j + 1;

				/* Bubble sort - ascending values */
				if (mon_lev[j1] > mon_lev[j2])
				{
					s16b tmp_lev = mon_lev[j1];
					u16b tmp_index = mon_index[j1];

					mon_lev[j1] = mon_lev[j2];
					mon_index[j1] = mon_index[j2];

					mon_lev[j2] = tmp_lev;
					mon_index[j2] = tmp_index;
				}
			}
		}

		/* Delete monsters until we've reached our quota */
		for (cnt = 0, i = 0; i < mon_max; i++)
		{
			/* We've deleted enough monsters */
			if (cnt >= size) break;
			/* Get this monster, using our saved index */
			m_ptr = &mon_list[mon_index[i]];

			/* "And another one bites the dust" */
			cnt++;

			/* No need to delete dead monsters again */
			if (!m_ptr->r_idx) continue;

			/* Delete the monster */
			delete_monster_idx(mon_index[i]);
		}

		/* Free the "mon_lev and mon_index" arrays */
		FREE(mon_lev);
		FREE(mon_index);
	}

	/* Excise dead monsters (backwards!) */
	for (i = mon_max - 1; i >= 1; i--)
	{
		/* Get the i'th monster */
		monster_type *m_ptr = &mon_list[i];

		/* Skip real monsters */
		if (m_ptr->r_idx) continue;

		/* Move last monster into open hole */
		compact_monsters_aux(mon_max - 1, i);

		/* Compress "mon_max" */
		mon_max--;
	}
}


/*
 * Delete/Remove all the monsters when the player leaves the level
 *
 * This is an efficient method of simulating multiple calls to the
 * "delete_monster()" function, with no visual effects.
 */
void wipe_mon_list(void)
{
	int i;

	/* Delete all the monsters */
	for (i = mon_max - 1; i >= 1; i--)
	{
		monster_type *m_ptr = &mon_list[i];

		monster_race *r_ptr = &r_info[m_ptr->r_idx];

		monster_lore *l_ptr = &l_list[m_ptr->r_idx];

		/* Skip dead monsters */
		if (!m_ptr->r_idx) continue;

		/* Total Hack -- Clear player ghost information. */
		if (r_ptr->flags2 & (RF2_PLAYER_GHOST))
		{
			l_ptr->sights = 0;
			l_ptr->pkills = 1;
			l_ptr->tkills = 0;
			bones_selector = 0;

			/* Hack -
			 * if player is just leaving the level, but not quitting the game,
			 * we don't want the ghost to appear again
		     */
			if ((p_ptr->playing) && (p_ptr->leaving)) r_ptr->max_num = 0;
		}

		/* Hack -- Reduce the racial counter */
		r_ptr->cur_num--;

		/* Monster is gone */
		cave_m_idx[m_ptr->fy][m_ptr->fx] = 0;

		/* Wipe the Monster */
		(void)WIPE(m_ptr, monster_type);
	}

	/* Reset "mon_max" */
	mon_max = 1;

	/* Reset "mon_cnt" */
	mon_cnt = 0;

	/* Hack -- reset "reproducer" count */
	num_repro = 0;

	/* Hack -- no more target */
	target_set_monster(0);

	/* Hack -- no more tracking */
	health_track(0);

	/* Hack -- make sure there is no player ghost */
	bones_selector = 0;
}


/*
 * Get and return the index of a "free" monster.
 *
 * This routine should almost never fail, but it *can* happen.
 */
s16b mon_pop(void)
{
	int i;

	/* Normal allocation */
	if (mon_max < z_info->m_max)
	{
		/* Get the next hole */
		i = mon_max;

		/* Expand the array */
		mon_max++;

		/* Count monsters */
		mon_cnt++;

		/* Return the index */
		return (i);
	}


	/* Recycle dead monsters */
	for (i = 1; i < mon_max; i++)
	{
		monster_type *m_ptr;

		/* Get the monster */
		m_ptr = &mon_list[i];

		/* Skip live monsters */
		if (m_ptr->r_idx) continue;

		/* Count monsters */
		mon_cnt++;

		/* Use this monster */
		return (i);
	}


	/* Warn the player (except during dungeon creation) */
	if (character_dungeon) msg_print("Too many monsters!");

	/* Try not to crash */
	return (0);
}


/*
 * Apply a "monster restriction function" to the "monster allocation table"
 */
errr get_mon_num_prep(void)
{
	int i;

	/* Scan the allocation table */
	for (i = 0; i < alloc_race_size; i++)
	{
		/* Get the entry */
		alloc_entry *entry = &alloc_race_table[i];

		/* Accept monsters which pass the restriction, if any */
		if (!get_mon_num_hook || (*get_mon_num_hook)(entry->index))
		{
			/* Accept this monster */
			entry->prob2 = entry->prob1;
		}

		/* Do not use this monster */
		else
		{
			/* Decline this monster */
			entry->prob2 = 0;
		}
	}

	/* Success */
	return (0);
}



/*
 * Choose a monster race that seems "appropriate" to the given level
 *
 * This function uses the "prob2" field of the "monster allocation table",
 * and various local information, to calculate the "prob3" field of the
 * same table, which is then used to choose an "appropriate" monster, in
 * a relatively efficient manner.
 *
 * Note that "town" monsters will *only* be created in the town, and
 * "normal" monsters will *never* be created in the town, unless the
 * "level" is "modified", for example, by polymorph or summoning.
 *
 * There is a small chance (1/50) of "boosting" the given depth by
 * a small amount (up to four levels), except in the town, and
 * a minimum depth enforcer for creature (unless specific monsters
 * are being called)
 *
 * It is (slightly) more likely to acquire a monster of the given level
 * than one of a lower level.  This is done by choosing several monsters
 * appropriate to the given level and keeping the "hardest" one.
 *
 * Note that if no monsters are "appropriate", then this function will
 * fail, and return zero, but this should *almost* never happen.
 */
s16b get_mon_num(int level)
{
	int i, p, j, x, mindepth;

	int r_idx;

	long value, total;

	monster_race *r_ptr;

	alloc_entry *table = alloc_race_table;

	quest_type *q_ptr = &q_info[GUILD_QUEST_SLOT];
	bool quest_level = FALSE;

	if (((q_ptr->type == QUEST_THEMED_LEVEL) ||
		 (q_ptr->type == QUEST_PIT) ||
		 (q_ptr->type == QUEST_NEST)) &&
		(p_ptr->cur_quest == p_ptr->depth)) quest_level = TRUE;

	/* Boost the level, but not for quest levels.  That has already been done */
	if ((level > 0) && (!quest_level))
	{
		/* Occasional "nasty" monste */
		if (one_in_(NASTY_MON))
		{
			/* Pick a level bonus */
			int d = level / 4 + 2;

			/* Boost the level */
			level += ((d < 5) ? d : 5);
		}

		/* Occasional "nasty" monster */
		if (one_in_(NASTY_MON))
		{
			/* Pick a level bonus */
			int d = level / 4 + 2;

			/* Boost the level */
			level += ((d < 5) ? d : 5);
		}
	}

	/* Reset total */
	total = 0L;

	/*enforce a mininum depth on monsters,
	 *which slowly drops if no monsters are available.
	 */
	if ((!(get_mon_num_hook)) || (quest_level)) mindepth = level / 5;
	else mindepth = level / 7;

	do
	{
		/* Process probabilities */
		for (i = 0; i < alloc_race_size; i++)
		{
			/* Monsters are sorted by depth */
			if (table[i].level > level) break;

			/* Default */
			table[i].prob3 = 0;

			/* Hack -- No town monsters in dungeon */
			if ((level > 0) && (table[i].level <= 0)) continue;

			/* Get the "r_idx" of the chosen monster */
			r_idx = table[i].index;

			/* Get the actual race */
			r_ptr = &r_info[r_idx];

			if (r_ptr->cur_num >= r_ptr->max_num) continue;

			/* Hack -- No low depth monsters monsters in deeper
			 * parts of the dungeon, except uniques,
			 * Note mindepth is given a lower value just before this "for loop")
			 * when using the get_mon_num_hook to limit the creature species.
			 */
			if ((level > 0) && (table[i].level < mindepth) &&
				(!(r_ptr->flags1 & (RF1_UNIQUE)))) continue;

			/* Hack -- "unique" monsters must be "unique" */
			if (r_ptr->flags1 & (RF1_UNIQUE))
			{
				bool do_continue = FALSE;

				/*No player ghosts if the option is set*/
				if ((r_ptr->flags2 & RF2_PLAYER_GHOST) &&
					(adult_no_player_ghosts)) continue;

				/* Check quests for uniques*/
				for (x = 0; x < z_info->q_max; x++)
				{
					if ((q_info[x].type == QUEST_UNIQUE) || (q_info[x].type == QUEST_FIXED_U))
					{
						/*is this unique marked for a quest?*/
						if (q_info[x].mon_idx == table[i].index)
						{
							/*Is it at this depth?*/
							if (p_ptr->depth != q_info[x].base_level)  do_continue = TRUE;
						}
					}
				}

				if (do_continue) continue;
			}

			/* Depth Monsters never appear out of depth */
			if ((r_ptr->flags1 & (RF1_FORCE_DEPTH)) && (r_ptr->level > p_ptr->depth))
			{
				continue;
			}

			/* Accept */
			table[i].prob3 = table[i].prob2;

			/* Total */
			total += table[i].prob3;

			/*slowly reduce if the mindepth is too high*/
			if (mindepth <= 6) mindepth --;
			else mindepth -= 5;
		}

	}

	while ((total <= 0) && (mindepth > 0));

	/* No legal monsters */
	if (total <= 0) return (0);

	/* Pick a monster */
	value = rand_int(total);

	/* Find the monster */
	for (i = 0; i < alloc_race_size; i++)
	{
		/* Found the entry */
		if (value < table[i].prob3) break;

		/* Decrement */
		value = value - table[i].prob3;
	}


	/* Power boost */
	p = rand_int(100);

	/* Try for a "harder" monster once (50%) or twice (10%) */
	if (p < 60)
	{
		/* Save old */
		j = i;

		/* Pick a monster */
		value = rand_int(total);

		/* Find the monster */
		for (i = 0; i < alloc_race_size; i++)
		{
			/* Found the entry */
			if (value < table[i].prob3) break;

			/* Decrement */
			value = value - table[i].prob3;
		}

		/* Keep the "best" one */
		if (table[i].level < table[j].level) i = j;
	}

	/* Try for a "harder" monster twice (10%) */
	if (p < 10)
	{
		/* Save old */
		j = i;

		/* Pick a monster */
		value = rand_int(total);

		/* Find the monster */
		for (i = 0; i < alloc_race_size; i++)
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
 * Display visible monsters in a window
 */
void display_monlist(void)
{
	int idx, n;
	int line = 0;

	char *m_name;
	char buf[80];

	monster_type *m_ptr;
	monster_race *r_ptr;

	u16b *race_counts;

	/* Allocate the array */
	C_MAKE(race_counts, z_info->r_max, u16b);

	/* Iterate over mon_list */
	for (idx = 1; idx < mon_max; idx++)
	{
		m_ptr = &mon_list[idx];

		/* Only visible monsters */
		if (!m_ptr->ml) continue;

		/*hidden mimics don't count*/
		if (m_ptr->mimic_k_idx) continue;

		/* Bump the count for this race */
		race_counts[m_ptr->r_idx]++;
	}


	/* Iterate over mon_list ( again :-/ ) */
	for (idx = 1; idx < mon_max; idx++)
	{
		m_ptr = &mon_list[idx];

		/* Only visible monsters */
		if (!m_ptr->ml) continue;

		/* Do each race only once */
		if (!race_counts[m_ptr->r_idx]) continue;

		/* Get monster race */
		r_ptr = &r_info[m_ptr->r_idx];

		/* Get the monster name */
		m_name = r_name + r_ptr->name;

		/* Obtain the length of the description */
		n = strlen(m_name);

		/* Display the entry itself */
		Term_putstr(0, line, n, TERM_WHITE, m_name);

		/* Append the "standard" attr/char info */
		Term_addstr(-1, TERM_WHITE, " ('");
		Term_addch(r_ptr->d_attr, r_ptr->d_char);
		Term_addstr(-1, TERM_WHITE, "')");
		n += 6;

		/* Append the "optional" attr/char info */
		Term_addstr(-1, TERM_WHITE, "/('");

		Term_addch(r_ptr->x_attr, r_ptr->x_char);

		if (use_bigtile)
		{
			if (r_ptr->x_attr & 0x80)
				Term_addch(255, -1);
			else
				Term_addch(0, ' ');

			n++;
		}

		Term_addstr(-1, TERM_WHITE, "'):");
		n += 7;

		/* Add race count */
		sprintf(buf, "%d", race_counts[m_ptr->r_idx]);
		Term_addch(TERM_WHITE, '[');
		Term_addstr(strlen(buf), TERM_WHITE, buf);
		Term_addch(TERM_WHITE, ']');
		n += strlen(buf) + 2;

		/* Don't do this race again */
		race_counts[m_ptr->r_idx] = 0;

		/* Erase the rest of the line */
		Term_erase(n, line, 255);

		/* Bump line counter */
		line++;
	}

	/* Free the race counters */
	FREE(race_counts);

	/* Erase the rest of the window */
	for (idx = line; idx < Term->hgt; idx++)
	{
		/* Erase the line */
		Term_erase(0, idx, 255);
	}
}

/*
 * Build a string describing a monster in some way.
 *
 * We can correctly describe monsters based on their visibility.
 * We can force all monsters to be treated as visible or invisible.
 * We can build nominatives, objectives, possessives, or reflexives.
 * We can selectively pronominalize hidden, visible, or all monsters.
 * We can use definite or indefinite descriptions for hidden monsters.
 * We can use definite or indefinite descriptions for visible monsters.
 *
 * Pronominalization involves the gender whenever possible and allowed,
 * so that by cleverly requesting pronominalization / visibility, you
 * can get messages like "You hit someone.  She screams in agony!".
 *
 * Reflexives are acquired by requesting Objective plus Possessive.
 *
 * I am assuming that no monster name is more than 65 characters long,
 * so that "char desc[80];" is sufficiently large for any result, even
 * when the "offscreen" notation is added.
 *
 * Note that the "possessive" for certain unique monsters will look
 * really silly, as in "Morgoth, King of Darkness's".  We should
 * perhaps add a flag to "remove" any "descriptives" in the name.
 *
 * Note that "offscreen" monsters will get a special "(offscreen)"
 * notation in their name if they are visible but offscreen.  This
 * may look silly with possessives, as in "the rat's (offscreen)".
 * Perhaps the "offscreen" descriptor should be abbreviated.
 *
 * Mode Flags:
 *   0x01 --> Objective (or Reflexive)
 *   0x02 --> Possessive (or Reflexive)
 *   0x04 --> Use indefinites for hidden monsters ("something")
 *   0x08 --> Use indefinites for visible monsters ("a kobold")
 *   0x10 --> Pronominalize hidden monsters
 *   0x20 --> Pronominalize visible monsters
 *   0x40 --> Assume the monster is hidden
 *   0x80 --> Assume the monster is visible
 *
 * Useful Modes:
 *   0x00 --> Full nominative name ("the kobold") or "it"
 *   0x04 --> Full nominative name ("the kobold") or "something"
 *   0x80 --> Banishment resistance name ("the kobold")
 *   0x88 --> Killing name ("a kobold")
 *   0x22 --> Possessive, genderized if visable ("his") or "its"
 *   0x23 --> Reflexive, genderized if visable ("himself") or "itself"
 */
void monster_desc(char *desc, size_t max, const monster_type *m_ptr, int mode)
{
	cptr res;

	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	cptr name = (r_name + r_ptr->name);

	char racial_name[40] = "oops";

	bool seen, pron;

	/*
	 * Is it a hidden mimic? If so, describe
	 * it as an object and exit.
	 */

	/* Can we "see" it (forced, or not hidden + visible) */
	seen = ((mode & (0x80)) || (!(mode & (0x40)) && m_ptr->ml));

	/* Sexed Pronouns (seen and forced, or unseen and allowed) */
	pron = ((seen && (mode & (0x20))) || (!seen && (mode & (0x10))));

	/* First, try using pronouns, or describing hidden monsters */
	if (!seen || pron)
	{
		/* an encoding of the monster "sex" */
		int kind = 0x00;

		/* Extract the gender (if applicable) */
		if (r_ptr->flags1 & (RF1_FEMALE)) kind = 0x20;
		else if (r_ptr->flags1 & (RF1_MALE)) kind = 0x10;

		/* Ignore the gender (if desired) */
		if (!m_ptr || !pron) kind = 0x00;

		/* Assume simple result */
		res = "it";

		/* Brute force: split on the possibilities */
		switch (kind + (mode & 0x07))
		{
			/* Neuter, or unknown */
			case 0x00: res = "it"; break;
			case 0x01: res = "it"; break;
			case 0x02: res = "its"; break;
			case 0x03: res = "itself"; break;
			case 0x04: res = "something"; break;
			case 0x05: res = "something"; break;
			case 0x06: res = "something's"; break;
			case 0x07: res = "itself"; break;

			/* Male (assume human if vague) */
			case 0x10: res = "he"; break;
			case 0x11: res = "him"; break;
			case 0x12: res = "his"; break;
			case 0x13: res = "himself"; break;
			case 0x14: res = "someone"; break;
			case 0x15: res = "someone"; break;
			case 0x16: res = "someone's"; break;
			case 0x17: res = "himself"; break;

			/* Female (assume human if vague) */
			case 0x20: res = "she"; break;
			case 0x21: res = "her"; break;
			case 0x22: res = "her"; break;
			case 0x23: res = "herself"; break;
			case 0x24: res = "someone"; break;
			case 0x25: res = "someone"; break;
			case 0x26: res = "someone's"; break;
			case 0x27: res = "herself"; break;
		}

		/* Copy the result */
		my_strcpy(desc, res, max);
	}


	/* Handle visible monsters, "reflexive" request */
	else if ((mode & 0x02) && (mode & 0x01))
	{
		/* The monster is visible, so use its gender */
		if (r_ptr->flags1 & (RF1_FEMALE)) my_strcpy(desc, "herself", max);
		else if (r_ptr->flags1 & (RF1_MALE)) my_strcpy(desc, "himself", max);
		else my_strcpy(desc, "itself", max);
	}


	/* Handle all other visible monster requests */
	else
	{
		/* It could be a player ghost. */
		if (r_ptr->flags2 & (RF2_PLAYER_GHOST))
		{
			/* Get the ghost name. */
			strcpy(desc, ghost_name);

			/* Get the racial name. */
			strcpy(racial_name, r_name + r_ptr->name);

			/* Build the ghost name. */
			strcat(desc, ", the ");
			strcat(desc, racial_name);
		}

		/* It could be a Unique */
		else if (r_ptr->flags1 & (RF1_UNIQUE))
		{
			/* Start with the name (thus nominative and objective) */
			my_strcpy(desc, name, max);
		}

		/* It could be an indefinite monster */
		else if (mode & 0x08)
		{
			/* XXX Check plurality for "some" */

			/* Indefinite monsters need an indefinite article */
			my_strcpy(desc, is_a_vowel(name[0]) ? "an " : "a ", max);
			my_strcat(desc, name, max);
		}

		/* It could be a normal, definite, monster */
		else
		{
			/* Definite monsters need a definite article */
			my_strcpy(desc, "the ", max);
			my_strcat(desc, name, max);
		}

		/* Handle the Possessive as a special afterthought */
		if (mode & 0x02)
		{
			/* XXX Check for trailing "s" */

			/* Simply append "apostrophe" and "s" */
			my_strcat(desc, "'s", max);
		}

		/* Mention "offscreen" monsters XXX XXX */
		if (!panel_contains(m_ptr->fy, m_ptr->fx))
		{
			/* Append special notation */
			my_strcat(desc, " (offscreen)", max);
		}
	}
}


/*
 * Build a string describing a monster race, currently used for quests.
 *
 * Assumes a singular monster.  This may need to be run through the
 * plural_aux function in the quest.c file.  (Changes "wolf" to
 * wolves, etc.....)
 *
 * I am assuming that no monster name is more than 65 characters long,
 * so that "char desc[80];" is sufficiently large for any result, even
 * when the "offscreen" notation is added.
 *
 */
void monster_desc_race(char *desc, size_t max, int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];

	cptr name = (r_name + r_ptr->name);

	/* Write the name */
	my_strcpy(desc, name, max);
}

/*
 * Learn about a monster (by "probing" it)
 */
void lore_probe_aux(int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];
	monster_lore *l_ptr = &l_list[r_idx];

	int i;

	i = randint (3);

	/*learn 2 out of three of their basic flags.....*/
	switch (i)
	{
		case 1:
		{

			l_ptr->flags1 = r_ptr->flags1;
			l_ptr->flags2 = r_ptr->flags2;
			break;
		}

		case 2:
		{
			l_ptr->flags1 = r_ptr->flags1;
			l_ptr->flags3 = r_ptr->flags3;
			break;
		}

		default:
		{
			l_ptr->flags2 = r_ptr->flags2;
			l_ptr->flags3 = r_ptr->flags3;
			break;
		}
	}

	/*probing is now much more informative*/
	i = randint (4);

	switch (i)
	{
		case 1:
		{
			/*learn their breaths, and shrieking, firing arrows, etc.....*/
			l_ptr->flags4 = r_ptr->flags4;
			break;
		}

		case 2:
		{
			/*learn many of monster's offensive spells*/
			l_ptr->flags5 = r_ptr->flags5;
			break;
		}

		case 3:
		{
			/*learn many of monster's offensive spells*/
			l_ptr->flags6 = r_ptr->flags6;
			break;
		}

		default:
		{
			/*learn many of their other spells*/
			l_ptr->flags7 = r_ptr->flags7;
			break;
		}
	}

	/* Hack -- Increse the sightings, and ranged attacks around 50% of the time */
	if (l_ptr->sights < MAX_SHORT)	l_ptr->sights += (MAX_SHORT - l_ptr->sights) / 100;
	if (l_ptr->ranged < MAX_UCHAR)	l_ptr->ranged += (MAX_UCHAR - l_ptr->ranged) / 5;

	i = randint (3);
	switch (i)
	{
		case 1:
		{
			/* Observe "maximal" attacks */
			for (i = 0; i < MONSTER_BLOW_MAX; i++)
			{
				/* Examine "actual" blows */
				if (r_ptr->blow[i].effect || r_ptr->blow[i].method)
				{
					/* Hack -- increase observations */
					l_ptr->blows[i] += (MAX_UCHAR - l_ptr->blows[i]) / 2;
				}
			}
			break;
		}

		case 2:
		{
			/* Hack -- Maximal info */
			l_ptr->wake = l_ptr->ignore = MAX_UCHAR;

			break;
		}

		default:
		{
			/* Hack -- know the treasure drops*/
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

			break;

		}

	}

}

/*
 * Learn about a monster (by "probing" it)
 */
void lore_do_probe(int m_idx)
{
	monster_type *m_ptr = &mon_list[m_idx];

	/*increase the information*/
	lore_probe_aux(m_ptr->r_idx);

	/* Update monster recall window */
	if (p_ptr->monster_race_idx == m_ptr->r_idx)
	{
		/* Window stuff */
		p_ptr->window |= (PW_MONSTER);
	}
}


/*
 * Take note that the given monster just dropped some treasure
 *
 * Note that learning the "CHEST/GOOD"/"GREAT" flags gives information
 * about the treasure (even when the monster is killed for the first
 * time, such as uniques, and the treasure has not been examined yet).
 *
 * This "indirect" method is used to prevent the player from learning
 * exactly how much treasure a monster can drop from observing only
 * a single example of a drop.  This method actually observes how much
 * gold and items are dropped, and remembers that information to be
 * described later by the monster recall code.
 */
void lore_treasure(int m_idx, int num_item, int num_gold)
{
	monster_type *m_ptr = &mon_list[m_idx];
	monster_race *r_ptr = &r_info[m_ptr->r_idx];
	monster_lore *l_ptr = &l_list[m_ptr->r_idx];


	/* Note the number of things dropped */
	if (num_item > l_ptr->drop_item) l_ptr->drop_item = num_item;
	if (num_gold > l_ptr->drop_gold) l_ptr->drop_gold = num_gold;

	/* Hack -- memorize the good/great flags */
	if (r_ptr->flags1 & (RF1_DROP_CHEST)) l_ptr->flags1 |= (RF1_DROP_CHEST);
	if (r_ptr->flags1 & (RF1_DROP_GOOD)) l_ptr->flags1 |= (RF1_DROP_GOOD);
	if (r_ptr->flags1 & (RF1_DROP_GREAT)) l_ptr->flags1 |= (RF1_DROP_GREAT);

	/* Update monster recall window */
	if (p_ptr->monster_race_idx == m_ptr->r_idx)
	{
		/* Window stuff */
		p_ptr->window |= (PW_MONSTER);
	}
}



/*
 * This function updates the monster record of the given monster
 *
 * This involves extracting the distance to the player (if requested),
 * and then checking for visibility (natural, infravision, see-invis,
 * telepathy), updating the monster visibility flag, redrawing (or
 * erasing) the monster when its visibility changes, and taking note
 * of any interesting monster flags (cold-blooded, invisible, etc).
 *
 * Note the new "mflag" field which encodes several monster state flags,
 * including "view" for when the monster is currently in line of sight,
 * and "mark" for when the monster is currently visible via detection.
 *
 * The only monster fields that are changed here are "cdis" (the
 * distance from the player), "ml" (visible to the player), and
 * "mflag" (to maintain the "MFLAG_VIEW" flag).
 *
 * Note the special "update_monsters()" function which can be used to
 * call this function once for every monster.
 *
 * Note the "full" flag which requests that the "cdis" field be updated,
 * this is only needed when the monster (or the player) has moved.
 *
 * Every time a monster moves, we must call this function for that
 * monster, and update the distance, and the visibility.  Every time
 * the player moves, we must call this function for every monster, and
 * update the distance, and the visibility.  Whenever the player "state"
 * changes in certain ways ("blindness", "infravision", "telepathy",
 * and "see invisible"), we must call this function for every monster,
 * and update the visibility.
 *
 * Routines that change the "illumination" of a grid must also call this
 * function for any monster in that grid, since the "visibility" of some
 * monsters may be based on the illumination of their grid.
 *
 * Note that this function is called once per monster every time the
 * player moves.  When the player is running, this function is one
 * of the primary bottlenecks, along with "update_view()" and the
 * "process_monsters()" code, so efficiency is important.
 *
 * Note the optimized "inline" version of the "distance()" function.
 *
 * A monster is "visible" to the player if (1) it has been detected
 * by the player, (2) it is close to the player and the player has
 * telepathy, or (3) it is close to the player, and in line of sight
 * of the player, and it is "illuminated" by some combination of
 * infravision, torch light, or permanent light (invisible monsters
 * are only affected by "light" if the player can see invisible).
 *
 * Monsters which are not on the current panel may be "visible" to
 * the player, and their descriptions will include an "offscreen"
 * reference.  Currently, offscreen monsters cannot be targetted
 * or viewed directly, but old targets will remain set.  XXX XXX
 *
 * The player can choose to be disturbed by several things, including
 * "disturb_move" (monster which is viewable moves in some way), and
 * "disturb_near" (monster which is "easily" viewable moves in some
 * way).  Note that "moves" includes "appears" and "disappears".
 */
void update_mon(int m_idx, bool full)
{
	monster_type *m_ptr = &mon_list[m_idx];

	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	monster_lore *l_ptr = &l_list[m_ptr->r_idx];

	int d;

	/* Current location */
	int fy = m_ptr->fy;
	int fx = m_ptr->fx;

	/* Seen at all */
	bool flag = FALSE;

	/* Seen by vision */
	bool easy = FALSE;

	/* Compute distance */
	if (full)
	{
		int py = p_ptr->py;
		int px = p_ptr->px;

		/* Distance components */
		int dy = (py > fy) ? (py - fy) : (fy - py);
		int dx = (px > fx) ? (px - fx) : (fx - px);

		/* Approximate distance */
		d = (dy > dx) ? (dy + (dx>>1)) : (dx + (dy>>1));

		/* Restrict distance */
		if (d > 255) d = 255;

		/* Save the distance */
		m_ptr->cdis = d;
	}

	/* Extract distance */
	else
	{
		/* Extract the distance */
		d = m_ptr->cdis;
	}


	/* Detected */
	if ((m_ptr->mflag & (MFLAG_MARK)) ||
		(m_ptr->mflag & (MFLAG_MIMIC))) flag = TRUE;

	/* Nearby */
	if (d <= MAX_SIGHT)
	{
		/* Basic telepathy */
		if (p_ptr->telepathy)
		{
			/* Empty mind, no telepathy */
			if (r_ptr->flags2 & (RF2_EMPTY_MIND))
			{
				/* Memorize flags */
				l_ptr->flags2 |= (RF2_EMPTY_MIND);
			}

			/* Weird mind, occasional telepathy */
			else if (r_ptr->flags2 & (RF2_WEIRD_MIND))
			{
				/* Monster is rarely detectable */
				if (((turn / 10) % 10) == (m_idx % 10))
				{
					/* Detectable */
					flag = TRUE;

					/* Memorize flags */
					l_ptr->flags2 |= (RF2_WEIRD_MIND);

					/* Hack -- Memorize mental flags */
					if (r_ptr->flags2 & (RF2_SMART)) l_ptr->flags2 |= (RF2_SMART);
					if (r_ptr->flags2 & (RF2_STUPID)) l_ptr->flags2 |= (RF2_STUPID);
				}
			}

			/* Normal mind, allow telepathy */
			else
			{
				/* Detectable */
				flag = TRUE;

				/* Hack -- Memorize mental flags */
				if (r_ptr->flags2 & (RF2_SMART)) l_ptr->flags2 |= (RF2_SMART);
				if (r_ptr->flags2 & (RF2_STUPID)) l_ptr->flags2 |= (RF2_STUPID);
			}
		}

		/* Normal line of sight, and not blind */
		if (player_has_los_bold(fy, fx) && !p_ptr->blind)
		{
			bool do_invisible = FALSE;
			bool do_cold_blood = FALSE;

			/* Use "infravision" */
			if (d <= p_ptr->see_infra)
			{
				/* Handle "cold blooded" monsters */
				if (r_ptr->flags2 & (RF2_COLD_BLOOD))
				{
					/* Take note */
					do_cold_blood = TRUE;
				}

				/* Handle "warm blooded" monsters */
				else
				{
					/* Easy to see */
					easy = flag = TRUE;
				}
			}

	        /* Use "lite carriers" */
		    if ((r_ptr->flags2 & (RF2_HAS_LITE)) &&
				!(r_ptr->flags2 & (RF2_INVISIBLE))) easy=flag=TRUE;

			/* Use "illumination" */
			if (player_can_see_bold(fy, fx))
			{
				/* Handle "invisible" monsters */
				if (r_ptr->flags2 & (RF2_INVISIBLE))
				{
					/* Take note */
					do_invisible = TRUE;

					/* See invisible */
					if (p_ptr->see_inv)
					{
						/* Easy to see */
						easy = flag = TRUE;
					}
				}

				/* Handle "normal" monsters */
				else
				{
					/* Easy to see */
					easy = flag = TRUE;
				}
			}

			/* Visible */
			if (flag)
			{
				/* Memorize flags */
				if (do_invisible) l_ptr->flags2 |= (RF2_INVISIBLE);
				if (do_cold_blood) l_ptr->flags2 |= (RF2_COLD_BLOOD);
			}
		}
	}


	/* The monster is now visible */
	if (flag)
	{
		/* It was previously unseen */
		if (!m_ptr->ml)
		{
			/* Mark as visible */
			m_ptr->ml = TRUE;

			/* Draw the monster */
			lite_spot(fy, fx);

			/* Update health bar as needed */
			if (p_ptr->health_who == m_idx) p_ptr->redraw |= (PR_HEALTH | PR_MON_MANA);

			/* Hack -- Count "fresh" sightings */
			if (l_ptr->sights < MAX_SHORT) l_ptr->sights++;

			/* Player knows if it has light */
            if (r_ptr->flags2 & (RF2_HAS_LITE)) l_ptr->flags2 |= RF2_HAS_LITE;

			/* Disturb on visibility change */
			if (disturb_move)
			{
				/* Disturb if monster is not a townsman, or if fairly weak */
				if (!(m_ptr->mflag & (MFLAG_TOWN)) || (p_ptr->lev < 10))
				{
					disturb(1, 0);
				}
			}

			/* Window stuff */
			p_ptr->window |= PW_MONLIST;

		}
	}

	/* The monster is not visible */
	else
	{
		/* It was previously seen */
		if (m_ptr->ml)
		{
			/* Mark as not visible */
			m_ptr->ml = FALSE;

			/* Erase the monster */
			lite_spot(fy, fx);

			/* Update health bar as needed */
			if (p_ptr->health_who == m_idx) p_ptr->redraw |= (PR_HEALTH | PR_MON_MANA);

			/* Disturb on visibility change */
			if (disturb_move)
			{
				/* Disturb if monster is not a townsman, or if fairly weak */
				if (!(m_ptr->mflag & (MFLAG_TOWN)) || (p_ptr->lev < 10))
				{
					disturb(1, 0);
				}
			}

			/* Window stuff */
			p_ptr->window |= PW_MONLIST;

		}
	}

	/* The monster is now easily visible */
	if (easy)
	{
		/* Change */
		if (!(m_ptr->mflag & (MFLAG_VIEW)))
		{
			/* Mark as easily visible */
			m_ptr->mflag |= (MFLAG_VIEW);

			/* Disturb on appearance */
			if (disturb_near)
			{
				/* Disturb if monster is not a townsman, or if fairly weak */
				if (!(m_ptr->mflag & (MFLAG_TOWN)) || (p_ptr->lev < 10))
				{
					disturb(1, 0);
				}
			}

		}
	}

	/* The monster is not easily visible */
	else
	{
		/* Change */
		if (m_ptr->mflag & (MFLAG_VIEW))
		{
			/* Mark as not easily visible */
			m_ptr->mflag &= ~(MFLAG_VIEW);

			/* Disturb on disappearance */
			if (disturb_near)
			{
				/* Disturb if monster is not a townsman, or if fairly weak */
				if (!(m_ptr->mflag & (MFLAG_TOWN)) || (p_ptr->lev < 10))
				{
					disturb(1, 0);
				}
			}

		}
	}
}




/*
 * This function simply updates all the (non-dead) monsters (see above).
 */
void update_monsters(bool full)
{
	int i;

	/* Update each (live) monster */
	for (i = 1; i < mon_max; i++)
	{
		monster_type *m_ptr = &mon_list[i];

		/* Skip dead monsters */
		if (!m_ptr->r_idx) continue;

		/* Update the monster */
		update_mon(i, full);
	}
}


/*
 * Find the right object for a mimic
 * note: lurkers/trappers should return 0
 */
static s16b get_mimic_k_idx(const monster_race *r_ptr)
{
	int i;
	int final_value = 0;


	/* Hack - look at default character */
	switch (r_ptr->d_char)
	{

		/*trappers and lurkers don't act this way*/
		case '.':
		{
			return(0);
		}


		case '$':
		{
			cptr name = (r_name + r_ptr->name);

			/* Look for textual clues */
			if (strstr(name, " copper "))     	return (lookup_kind(TV_GOLD, SV_GOLD_COPPER));
			if (strstr(name, " silver "))     	return (lookup_kind(TV_GOLD, SV_GOLD_SILVER));
			if (strstr(name, " gold"))       	return (lookup_kind(TV_GOLD, SV_GOLD_GOLD));
			if (strstr(name, " mithril"))    	return (lookup_kind(TV_GOLD, SV_GOLD_MITHRIL));
			if (strstr(name, " opal"))    		return (lookup_kind(TV_GOLD, SV_GOLD_OPALS));
			if (strstr(name, " sapphire"))    	return (lookup_kind(TV_GOLD, SV_GOLD_SAPPHIRES));
			if (strstr(name, " ruby"))    		return (lookup_kind(TV_GOLD, SV_GOLD_RUBIES));
			if (strstr(name, " diamond"))    	return (lookup_kind(TV_GOLD, SV_GOLD_DIAMOND));
			if (strstr(name, " adamantite ")) 	return (lookup_kind(TV_GOLD, SV_GOLD_ADAMANTITE));
			break;
		}

		/*assumes scroll, this would have to be altered for magic books*/
		case '?':
		{
			/* Analyze every object */
			for (i = 1; i < z_info->k_max; i++)
			{
				object_kind *k_ptr = &k_info[i];

				/* Skip "empty" objects */
				if (!k_ptr->name) continue;

				/*skip all non-scrolls*/
				if (k_ptr->tval != TV_SCROLL) continue;

				/*don't mimic known items*/
				if (k_ptr->aware) continue;

				/*skip artifacts, let's not annoy the player*/
				if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

				/*we have a suitable object to mimic*/
				if ((final_value == 0) || (one_in_(3))) final_value = i;

			}

			/*can be 0 if all items are identified*/
			return(final_value);
		}

		case '!':
		{
			/* Analyze every object */
			for (i = 1; i < z_info->k_max; i++)
			{
				object_kind *k_ptr = &k_info[i];

				/* Skip "empty" objects */
				if (!k_ptr->name) continue;

				/*skip all non-potions*/
				if (k_ptr->tval != TV_POTION) continue;

				/*don't mimic known items*/
				if (k_ptr->aware) continue;

				/*skip artifacts, let's not annoy the player*/
				if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

				/*we have a suitable object to mimic*/
				if ((final_value == 0) || (one_in_(3))) final_value = i;

			}

			/*can be 0 if all items are identified*/
			return(final_value);


		}

		case '=':
		{
			/* Analyze every object */
			for (i = 1; i < z_info->k_max; i++)
			{
				object_kind *k_ptr = &k_info[i];

				/* Skip "empty" objects */
				if (!k_ptr->name) continue;

				/*skip all non-rings*/
				if (k_ptr->tval != TV_RING) continue;

				/*don't mimic known items*/
				if (k_ptr->aware) continue;

				/*skip artifacts, let's not annoy the player*/
				if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

				/*we have a suitable object to mimic*/
				if ((final_value == 0) || (one_in_(3))) final_value = i;

			}

			/*can be 0 if all items are identified*/
			return(final_value);
		}

		/*staffs*/
		case '_':
		{
			/* Analyze every object */
			for (i = 1; i < z_info->k_max; i++)
			{
				object_kind *k_ptr = &k_info[i];

				/* Skip "empty" objects */
				if (!k_ptr->name) continue;

				/*skip all non-staffs*/
				if (k_ptr->tval != TV_STAFF) continue;

				/*don't mimic known items*/
				if (k_ptr->aware) continue;

				/*skip artifacts, let's not annoy the player*/
				if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

				/*we have a suitable object to mimic*/
				if ((final_value == 0) || (one_in_(3))) final_value = i;

			}

			/*can be 0 if all items are identified*/
			return(final_value);
		}

		/*rods and wands*/
		case '-':
		{
			cptr name = (r_name + r_ptr->name);

			if (strstr(name, "Wand mimic"))
			{
				/* Analyze every object */
				for (i = 1; i < z_info->k_max; i++)
				{
					object_kind *k_ptr = &k_info[i];

					/* Skip "empty" objects */
					if (!k_ptr->name) continue;

					/*skip all non-wands*/
					if (k_ptr->tval != TV_WAND) continue;

					/*don't mimic known items*/
					if (k_ptr->aware) continue;

					/*skip artifacts, let's not annoy the player*/
					if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

					/*we have a suitable object to mimic*/
					if ((final_value == 0) || (one_in_(3))) final_value = i;

				}

				return(final_value);
			}

			if (strstr(name, "Rod mimic"))
			{
				/* Analyze every object */
				for (i = 1; i < z_info->k_max; i++)
				{
					object_kind *k_ptr = &k_info[i];

					/* Skip "empty" objects */
					if (!k_ptr->name) continue;

					/*skip all non-rods*/
					if (k_ptr->tval != TV_ROD) continue;

					/*don't mimic known items*/
					if (k_ptr->aware) continue;

					/*skip artifacts, let's not annoy the player*/
					if (k_ptr->flags3 & (TR3_INSTA_ART)) continue;

					/*we have a suitable object to mimic*/
					if ((final_value == 0) || (one_in_(3))) final_value = i;

				}

				return(final_value);
			}

			/*can be 0 if all items are identified*/
			return(final_value);
		}


		/*chests*/
		case '~':
		{
			i = randint(10);

			/* Look for textual clues */
			if (i <  7) return (lookup_kind(TV_CHEST, (SV_CHEST_MIN_SMALL + rand_int (3))));
			else if (i <  10) return (lookup_kind(TV_CHEST, (SV_CHEST_MIN_LARGE + rand_int (3))));
			else return (lookup_kind(TV_CHEST, SV_CHEST_JEWELED_LARGE));

			}

		default:
		{
			return (TRUE);
		}
	}


	/* Result */
	return (0);
}

/*
 * Make a monster carry an object
 */
s16b monster_carry(int m_idx, object_type *j_ptr)
{
	s16b o_idx;

	s16b this_o_idx, next_o_idx = 0;

	monster_type *m_ptr = &mon_list[m_idx];

	/* Scan objects already being held for combination */
	for (this_o_idx = m_ptr->hold_o_idx; this_o_idx; this_o_idx = next_o_idx)
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
	}


	/* Make an object */
	o_idx = o_pop();

	/* Success */
	if (o_idx)
	{
		object_type *o_ptr;

		/* Get new object */
		o_ptr = &o_list[o_idx];

		/* Copy object */
		object_copy(o_ptr, j_ptr);

		/* Forget mark */
		o_ptr->marked = FALSE;

		/* Forget location */
		o_ptr->iy = o_ptr->ix = 0;

		/* Link the object to the monster */
		o_ptr->held_m_idx = m_idx;

		/* Link the object to the pile */
		o_ptr->next_o_idx = m_ptr->hold_o_idx;

		/* Link the monster to the object */
		m_ptr->hold_o_idx = o_idx;
	}

	/* Result */
	return (o_idx);
}


/*
 * Swap the players/monsters (if any) at two locations XXX XXX XXX
 */
void monster_swap(int y1, int x1, int y2, int x2)
{
	int m1, m2;

	monster_type *m_ptr;

	/* Monsters */
	m1 = cave_m_idx[y1][x1];
	m2 = cave_m_idx[y2][x2];

	/* Update grids */
	cave_m_idx[y1][x1] = m2;
	cave_m_idx[y2][x2] = m1;

	/* Monster 1 */
	if (m1 > 0)
	{
		m_ptr = &mon_list[m1];

		/* Move monster */
		m_ptr->fy = y2;
		m_ptr->fx = x2;

		/* Update monster */
		(void)update_mon(m1, TRUE);
	}

	/* Player 1 */
	else if (m1 < 0)
	{
		/* Move player */
		p_ptr->py = y2;
		p_ptr->px = x2;

		/* Update the panel */
		p_ptr->update |= (PU_PANEL);

		/* Update the visuals (and monster distances) */
		p_ptr->update |= (PU_UPDATE_VIEW | PU_DISTANCE);

		/* Window stuff */
		p_ptr->window |= (PW_OVERHEAD);
	}

	/* Monster 2 */
	if (m2 > 0)
	{
		m_ptr = &mon_list[m2];

		/* Move monster */
		m_ptr->fy = y1;
		m_ptr->fx = x1;

		/* Update monster */
		(void)update_mon(m2, TRUE);
	}

	/* Player 2 */
	else if (m2 < 0)
	{
		/* Move player */
		p_ptr->py = y1;
		p_ptr->px = x1;

		/* Update the panel */
		p_ptr->update |= (PU_PANEL);

		/* Update the visuals (and monster distances) */
		p_ptr->update |= (PU_UPDATE_VIEW | PU_DISTANCE);

		/* Window stuff */
		p_ptr->window |= (PW_OVERHEAD);
	}


	/* Redraw */
	lite_spot(y1, x1);
	lite_spot(y2, x2);
}


/*
 * Place the player in the dungeon XXX XXX
 */
s16b player_place(int y, int x)
{

	/* Paranoia XXX XXX */
	if (cave_m_idx[y][x] != 0) return (0);

	/* Save player location */
	p_ptr->py = y;
	p_ptr->px = x;

	/* Mark cave grid */
	cave_m_idx[y][x] = -1;

	/* Success */
	return (-1);
}


/*
 * Place a copy of a monster in the dungeon XXX XXX
 */
s16b monster_place(int y, int x, monster_type *n_ptr)
{
	s16b m_idx;

	monster_type *m_ptr;
	monster_race *r_ptr;

	/* Paranoia XXX XXX */
	if (cave_m_idx[y][x] != 0) return (0);

	/* Get a new record */
	m_idx = mon_pop();

	/* Oops */
	if (m_idx)
	{
		/* Make a new monster */
		cave_m_idx[y][x] = m_idx;

		/* Get the new monster */
		m_ptr = &mon_list[m_idx];

		/* Copy the monster XXX */
		COPY(m_ptr, n_ptr, monster_type);

		/* Location */
		m_ptr->fy = y;
		m_ptr->fx = x;

		/* Update the monster */
		update_mon(m_idx, TRUE);

		/* Get the new race */
		r_ptr = &r_info[m_ptr->r_idx];

		/* Hack -- Notice new multi-hued monsters */
		if (r_ptr->flags1 & (RF1_ATTR_MULTI)) shimmer_monsters = TRUE;

		/* Hack -- Count the number of "reproducers" */
		if (r_ptr->flags2 & (RF2_MULTIPLY)) num_repro++;

		/* Count racial occurances */
		r_ptr->cur_num++;
	}

	/* Result */
	return (m_idx);
}

/*
 * Determine if a town-dweller is not a threat.
 *
 * Freely admitted:  this whole concept is a hack.
 */
static bool no_threat(const monster_race *r_ptr)
{
	int i;

	/*if they can cast spells, they are a threat*/
	if (r_ptr->freq_ranged) return (TRUE);

	/* Scan blows */
	for (i = 0; i < MONSTER_BLOW_MAX; i++)
	{
		/* Extract the attack information */
		int effect = r_ptr->blow[i].effect;
		int method = r_ptr->blow[i].method;
		int d_dice = r_ptr->blow[i].d_dice;
		int d_side = r_ptr->blow[i].d_side;

		/* Hack -- no more attacks */
		if (!method) break;

		/* Can hurt the character (more than a little bit)  XXX XXX */
		if (d_dice * d_side > 2) return (FALSE);

		/* Can steal from the character */
		if ((effect == RBE_EAT_GOLD) || (effect == RBE_EAT_ITEM)) return (FALSE);

	}

	/* Harmless */
	return (TRUE);
}

/*calculate the monster_speed of a monster at a given location*/
void calc_monster_speed(int y, int x)
{
	int speed, i;

	/*point to the monster at the given location & the monster race*/
	monster_type *m_ptr = &mon_list[cave_m_idx[y][x]];
	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	/* Paranoia XXX XXX */
	if (cave_m_idx[y][x] == 0) return;

	/* Get the monster base speed */
	speed = r_ptr->speed;

	/*note: a monster should only have one of these flags*/
	if (m_ptr->mflag & (MFLAG_SLOWER))
	{
		/* Allow some small variation each time to make pillar dancing harder */
		i = extract_energy[r_ptr->speed] / 10;
		speed -= rand_spread(0, i);
	}
	else if (m_ptr->mflag & (MFLAG_FASTER))
	{
		/* Allow some small variation each time to make pillar dancing harder */
		i = extract_energy[r_ptr->speed] / 10;
		speed += rand_spread(0, i);
	}

	/*factor in the hasting and slowing counters*/
	if (m_ptr->hasted) speed += 10;
	if (m_ptr->slowed) speed -= 10;

	/*set the speed and return*/
	m_ptr->mspeed = speed;

	return;
}

void set_monster_haste(s16b m_idx, s16b counter, bool message)
{
	/*get the monster at the given location*/
	monster_type *m_ptr = &mon_list[m_idx];

	bool recalc = FALSE;

	char m_name[80];

	/* Get monster name*/
	monster_desc(m_name, sizeof(m_name), m_ptr, 0);

	/*see if we need to recalculate speed*/
	if (m_ptr->hasted)
	{
		/*monster is no longer hasted and speed needs to be recalculated*/
		if (counter == 0)
		{
			recalc = TRUE;

			/*give a message*/
			if (message) msg_format("%^s slows down.", m_name);
		}
	}
	else
	{
		/*monster is now hasted and speed needs to be recalculated*/
		if (counter > 0)
		{
			recalc = TRUE;

			/*give a message*/
			if (message) msg_format("%^s starts moving faster.", m_name);
		}
	}

	/*update the counter*/
	m_ptr->hasted = counter;

	/*re-calculate speed if necessary*/
	if (recalc) calc_monster_speed(m_ptr->fy, m_ptr->fx);

	return;
}

void set_monster_slow(s16b m_idx, s16b counter, bool message)
{
	/*get the monster at the given location*/
	monster_type *m_ptr = &mon_list[m_idx];

	bool recalc = FALSE;

	char m_name[80];

	/* Get monster name*/
	monster_desc(m_name, sizeof(m_name), m_ptr, 0);

	/*see if we need to recalculate speed*/
	if (m_ptr->slowed)
	{
		/*monster is no longer slowed and speed needs to be recalculated*/
		if (counter == 0)
		{
			recalc = TRUE;

			/*give a message*/
			if (message) msg_format("%^s speeds up.", m_name);
		}
	}
	else
	{
		/*monster is now slowed and speed needs to be recalculated*/
		if (counter > 0)
		{
			recalc = TRUE;

			/*give a message*/
			if (message) msg_format("%^s starts moving slower.", m_name);
		}
	}

	/*update the counter*/
	m_ptr->slowed = counter;

	/*re-calculate speed if necessary*/
	if (recalc) calc_monster_speed(m_ptr->fy, m_ptr->fx);

	return;
}

/*
 * Attempt to place a monster of the given race at the given location.
 *
 * To give the player a sporting chance, any monster that appears in
 * line-of-sight and is extremely dangerous can be marked as
 * "FORCE_SLEEP", which will cause them to be placed with low energy,
 * which often (but not always) lets the player move before they do.
 *
 * This routine refuses to place out-of-depth "FORCE_DEPTH" monsters.
 *
 * XXX XXX XXX Use special "here" and "dead" flags for unique monsters,
 * remove old "cur_num" and "max_num" fields.
 *
 * XXX XXX XXX Actually, do something similar for artifacts, to simplify
 * the "preserve" mode, and to make the "what artifacts" flag more useful.
 *
 * This is the only function which may place a monster in the dungeon,
 * except for the savefile loading code.
 */
static bool place_monster_one(int y, int x, int r_idx, bool slp)
{

	monster_race *r_ptr;

	monster_type *n_ptr;
	monster_type monster_type_body;

	cptr name;

	/* Paranoia */
	if (!in_bounds(y, x)) return (FALSE);

	/* Require empty space */
	if (!cave_empty_bold(y, x)) return (FALSE);

	/* Hack -- no creation on glyph of warding */
	if (cave_feat[y][x] == FEAT_GLYPH) return (FALSE);

	/* Handle failure of the "get_mon_num()" function */
	if (!r_idx) return (FALSE);

	if ((feeling >= LEV_THEME_HEAD) && (character_dungeon == TRUE)) return (FALSE);

	/* Race */
	r_ptr = &r_info[r_idx];

	/* The monster must be able to exist in this grid */
	if (!cave_exist_mon(r_ptr, y, x, FALSE, FALSE)) return (FALSE);

	/* Paranoia */
	if (!r_ptr->name) return (FALSE);

	/*limit the population*/
	if (r_ptr->cur_num >= r_ptr->max_num)
	{
		return (FALSE);
	}

	/* Name */
	name = (r_name + r_ptr->name);

	/* Hack -- "unique" monsters must be "unique" */
	if (r_ptr->flags1 & (RF1_UNIQUE))
	{
		int i;

		/* Check quests for uniques*/
		for (i = 0; i < z_info->q_max; i++)
		{
			if ((q_info[i].type == QUEST_UNIQUE) || (q_info[i].type == QUEST_FIXED_U))
			{
				/*is this unique marked for a quest?*/
				if (q_info[i].mon_idx == r_idx)
				{
					/*Is it at the proper depth?*/
					if(p_ptr->depth != q_info[i].base_level)  return (FALSE);

				}
			}
		}

	}


	/* Hack -- only 1 player ghost at a time */
	if ((r_ptr->flags2 & (RF2_PLAYER_GHOST)) && bones_selector)
	{
		/* Cannot create */
		return (FALSE);
	}


	/* Depth monsters may NOT be created out of depth */
	if ((r_ptr->flags1 & (RF1_FORCE_DEPTH)) && (p_ptr->depth < r_ptr->level))
	{
		/* Cannot create */
		return (FALSE);
	}

	/* Get local monster */
	n_ptr = &monster_type_body;

	/* Clean out the monster */
	(void)WIPE(n_ptr, monster_type);

	/* Save the race */
	n_ptr->r_idx = r_idx;

	/*
	 * If the monster is a player ghost, perform various manipulations
	 * on it, and forbid ghost creation if something goes wrong.
	 */
	if (r_ptr->flags2 & (RF2_PLAYER_GHOST))
	{

		if (!prepare_ghost(r_idx, FALSE))
		{
			return (FALSE);
		}

		name = format("%s, the %s", ghost_name, name);
	}

	/* Town level has some special rules */
	if ((!p_ptr->depth) && (!r_ptr->level))
	{
		/* Hack -- harmless townsmen are not threatening */
		if ((r_ptr->d_char == 't') && (no_threat(r_ptr)))
			n_ptr->mflag |= (MFLAG_TOWN);

		/* Hack -- town dogs and city cats are not out for blood */
		else if ((r_ptr->d_char == 'f') || (r_ptr->d_char == 'C'))
			n_ptr->mflag |= (MFLAG_TOWN);

		/* Mega-hack -- no nasty town dwellers when starting out */
		else if (!p_ptr->lev) return (FALSE);
	}

	/* Enforce sleeping if needed */
	if (slp && r_ptr->sleep)
	{
		n_ptr->csleep = rand_range((r_ptr->sleep + 1) / 2, r_ptr->sleep);
	}

	/* Assign maximal hitpoints */
	if (r_ptr->flags1 & (RF1_FORCE_MAXHP))
	{
		n_ptr->maxhp = (r_ptr->hdice * r_ptr->hside);
	}
	/*assign hitpoints using dice rolls*/
	else
	{
		n_ptr->maxhp = damroll(r_ptr->hdice, r_ptr->hside);
	}

	/* Mark minimum range for recalculation */
	n_ptr->min_range = 0;

	/* Initialize mana */
	n_ptr->mana = r_ptr->mana;

	/* And start out fully healthy */
	n_ptr->hp = n_ptr->maxhp;

	/* Hack -- in the dungeon, aggravate every monster as long as there
	 * are too many recent thefts
	 */
	if ((p_ptr->depth) && (recent_failed_thefts > 30)
		&& ((randint(5) + 30) > recent_failed_thefts))
	{
		/*make them all awake.....*/
		n_ptr->csleep = 0;

		/*and wary*/
		n_ptr->mflag |= (MFLAG_WARY);

		/*Everybody but uniques are always hasted*/
		if (!(r_ptr->flags1 & (RF1_UNIQUE)))
		{
			n_ptr->mflag |= (MFLAG_FASTER);
		}

		/*make all monster's faster*/
		n_ptr->hasted = (recent_failed_thefts * 10) + rand_int(10);

	}

	/* 75% non-unique monsters vary their speed*/
	else if (!(r_ptr->flags1 & (RF1_UNIQUE)))
	{
		if (!(one_in_(4)))
		{
			if (one_in_(2))
			n_ptr->mflag |= (MFLAG_SLOWER);
			else n_ptr->mflag |= (MFLAG_FASTER);
		}
	}


	/* Force monster to wait for player */
	if (r_ptr->flags1 & (RF1_FORCE_SLEEP))
	{
		/* Give almost no starting energy (avoids clumped movement) */
		n_ptr->energy = (byte)rand_int(10);
	}

	else
	{
		/* Give a random starting energy */
		n_ptr->energy = (byte)rand_int(25);
	}

	/* Mimics (except lurkers, trappers) start out hidden.*/
	if (r_ptr->flags1 & (RF1_CHAR_MIMIC))
	{
		n_ptr->mimic_k_idx = get_mimic_k_idx(r_ptr);
	}

	else n_ptr->mimic_k_idx = 0;

	/* Place the monster in the dungeon */
	if (!monster_place(y, x, n_ptr)) return (FALSE);

	/*calculate the monster_speed*/
	calc_monster_speed(y, x);

	/* Powerful monster */
	if (r_ptr->level > p_ptr->depth)
	{
		/* Unique monsters */
		if (r_ptr->flags1 & (RF1_UNIQUE))
		{
			/* Message for cheaters */
			if (cheat_hear) msg_format("Deep Unique (%s).", name);

			/* Boost rating by twice delta-depth */
			rating += (r_ptr->level - p_ptr->depth) * 2;
		}

		/* Normal monsters */
		else
		{
			/* Message for cheaters */
			if (cheat_hear) msg_format("Deep Monster (%s).", name);
			/* Boost rating by half delta-depth */
			rating += (r_ptr->level - p_ptr->depth) / 2;
		}
	}

	/* Note the monster */
	else if (r_ptr->flags1 & (RF1_UNIQUE))
	{
		/* Unique monsters induce message */
		if (cheat_hear) msg_format("Unique (%s).", name);
	}

	/* Success */
	return (TRUE);
}


/*
 * Maximum size of a group of monsters
 */
#define GROUP_MAX	18

/*
 * Attempt to place a group of monsters around the given location.
 *
 * Hack -- A group of monsters counts as a single individual for the
 * level rating.
 */
static bool place_monster_group(int y, int x, int r_idx, bool slp, s16b group_size)
{
 	monster_race *r_ptr = &r_info[r_idx];

	int old, n, i;
	int start;
	int reduce;

	int hack_n = 0;

 	byte hack_y[GROUP_MAX];
 	byte hack_x[GROUP_MAX];

	/* Hard monsters, smaller groups */
 	if (r_ptr->level > p_ptr->depth)
 	{
		reduce = (r_ptr->level - p_ptr->depth) / 2;
		group_size -= randint(reduce);
 	}

	if (group_size < 2) group_size = 2;

 	/* Maximum size */
	if (group_size > GROUP_MAX) group_size = GROUP_MAX;

	/* Save the rating */
	old = rating;

	/* Start on the monster */
	hack_n = 1;
	hack_x[0] = x;
	hack_y[0] = y;

	/* Puddle monsters, breadth first, up to group_size */
	for (n = 0; (n < hack_n) && (hack_n < group_size); n++)
	{
		/* Grab the location */
		int hx = hack_x[n];
		int hy = hack_y[n];

		/* Random direction */
		start = rand_int(8);

		/* Check each direction, up to group_size */
		for (i = start; (i < 8 + start) && (hack_n < group_size); i++)
		{
			int mx = hx + ddx_ddd[i % 8];
			int my = hy + ddy_ddd[i % 8];

			/* Attempt to place another monster */
			if (place_monster_one(my, mx, r_idx, slp))
			{
				/* Add it to the "hack" set */
				hack_y[hack_n] = my;
				hack_x[hack_n] = mx;
				hack_n++;
			}
		}
	}

	/* Hack -- restore the rating */
	rating = old;

	/* Success */
	return (TRUE);
}

/*
 * Hack -- help pick an escort type
 */
static int place_monster_idx = 0;

/*
 * Hack -- help pick an escort type
 */
static bool place_monster_okay(int r_idx)
{
	monster_race *r_ptr = &r_info[place_monster_idx];

	monster_race *z_ptr = &r_info[r_idx];

	/* Require similar "race" */
	if (z_ptr->d_char != r_ptr->d_char) return (FALSE);

	/* Skip more advanced monsters */
	if (z_ptr->level > r_ptr->level) return (FALSE);

	/* Skip unique monsters */
	if (z_ptr->flags1 & (RF1_UNIQUE)) return (FALSE);

	/* Paranoia -- Skip identical monsters */
	if (place_monster_idx == r_idx) return (FALSE);

	/* Okay */
	return (TRUE);
}




/*
 * Attempt to place an escort of monsters around the given location
 */
static void place_monster_escort(int y, int x, int leader_idx, bool slp)
{
	int escort_size, escort_idx;
	int n, i;

	/* Random direction */
	int start;

	monster_race *r_ptr = &r_info[leader_idx];

	int level = r_ptr->level;

	int hack_n = 0;

	byte hack_y[GROUP_MAX];
	byte hack_x[GROUP_MAX];

	/* Save previous monster restriction value. */
	bool (*get_mon_num_hook_temp)(int r_idx) = get_mon_num_hook;

	/* Calculate the number of escorts we want. */
	if (r_ptr->flags1 & (RF1_ESCORTS)) escort_size = rand_range(12, 18);
	else escort_size = rand_range(4, 6);

	/* Can never have more escorts than maximum group size */
	if (escort_size > GROUP_MAX) escort_size = GROUP_MAX;

	/* Use the leader's monster type to restrict the escorts. */
	place_monster_idx = leader_idx;

	/* Set the escort hook */
	get_mon_num_hook = place_monster_okay;

	/* Prepare allocation table */
	get_mon_num_prep();

	/* Build monster table, get index of first escort */
	escort_idx = get_mon_num(monster_level);

	/* Start on the monster */
	hack_n = 1;
	hack_x[0] = x;
	hack_y[0] = y;

	/* Puddle monsters, breadth first, up to escort_size */
	for (n = 0; (n < hack_n) && (hack_n < escort_size); n++)
	{
		/* Grab the location */
		int hx = hack_x[n];
		int hy = hack_y[n];

		/* Random direction */
		start = rand_int(8);

		/* Check each direction, up to escort_size */
		for (i = start; (i < 8 + start) && (hack_n < escort_size); i++)
		{
			int mx = hx + ddx_ddd[i % 8];
			int my = hy + ddy_ddd[i % 8];

			/* Place a group of escorts if needed */
			if ((r_info[escort_idx].flags1 & (RF1_FRIENDS)) &&
				!place_monster_group(my, mx, escort_idx, slp, (rand_range(4, 8))))
			{
				continue;
			}

			/* Place a group of escorts if needed */
			else if ((r_info[escort_idx].flags1 & (RF1_FRIEND)) &&
				!place_monster_group(my, mx, escort_idx, slp, (rand_range(2, 3))))
			{
				continue;
			}

			/* Attempt to place another monster */
			else if (!place_monster_one(my, mx, escort_idx, slp))
			{
				continue;
			}

			/* Add grid to the "hack" set */
			hack_y[hack_n] = my;
			hack_x[hack_n] = mx;
			hack_n++;

			/* Get index of the next escort */
			escort_idx = get_mon_num(level);
		}
	}

	/* Return to previous monster restrictions (usually none) */
	get_mon_num_hook = get_mon_num_hook_temp;

	/* Prepare allocation table */
	get_mon_num_prep();

	/* XXX - rebuild monster table */
	(void)get_mon_num(monster_level);
}


/*
 * Attempt to place a monster of the given race at the given location
 *
 * Note that certain monsters are now marked as requiring "friends".
 * These monsters, if successfully placed, and if the "grp" parameter
 * is TRUE, will be surrounded by a "group" of identical monsters.
 *
 * Note that certain monsters are now marked as requiring an "escort",
 * which is a collection of monsters with similar "race" but lower level.
 *
 * Some monsters induce a fake "group" flag on their escorts.
 *
 * Note the "bizarre" use of non-recursion to prevent annoying output
 * when running a code profiler.
 *
 * Note the use of the new "monster allocation table" code to restrict
 * the "get_mon_num()" function to "legal" escort types.
 */
bool place_monster_aux(int y, int x, int r_idx, bool slp, bool grp)
{

	monster_race *r_ptr = &r_info[r_idx];

	/* Place one monster, or fail */
	if (!place_monster_one(y, x, r_idx, slp)) return (FALSE);

	/* Require the "group" flag */
	if (!grp) return (TRUE);

	/* Friends for certain monsters */
	if (r_ptr->flags1 & (RF1_FRIENDS))
	{
		(void)place_monster_group(y, x, r_idx, slp, (s16b)rand_range(6, 10));
	}

	else if (r_ptr->flags1 & (RF1_FRIEND))
	{
		/* Attempt to place a small group */
		(void)place_monster_group(y, x, r_idx, slp, (s16b)(rand_range(2, 3)));
	}

	/* Escorts for certain monsters */
	if ((r_ptr->flags1 & (RF1_ESCORT)) || (r_ptr->flags1 & (RF1_ESCORTS)))
	{
		place_monster_escort(y, x, r_idx, slp);
	}

	/* Success */
	return (TRUE);
}


/*
 * Hack -- attempt to place a monster at the given location
 *
 * Attempt to find a monster appropriate to the "monster_level"
 */
bool place_monster(int y, int x, bool slp, bool grp)
{
	int r_idx;

	/* Pick a monster */
	r_idx = get_mon_num(monster_level);

	/* Handle failure */
	if (!r_idx) return (FALSE);

	/* Attempt to place the monster */
	if (place_monster_aux(y, x, r_idx, slp, grp)) return (TRUE);

	/* Oops */
	return (FALSE);
}


/*
 * Attempt to allocate a random monster in the dungeon.
 *
 * Place the monster at least "dis" distance from the player.
 *
 * Use "slp" to choose the initial "sleep" status
 *
 * Use "monster_level" for the monster level
 */
bool alloc_monster(int dis, bool slp)
{
	int py = p_ptr->py;
	int px = p_ptr->px;

	int y, x;
	int	attempts_left = 10000;

	/* Find a legal, distant, unoccupied, space */
	while (attempts_left)
	{
		--attempts_left;

		/* Pick a location */
		y = rand_int(p_ptr->cur_map_hgt);
		x = rand_int(p_ptr->cur_map_wid);

		/* Require a grid that all monsters can exist in. */
		if (!cave_naked_bold(y, x)) continue;

		/* Accept far away grids */
		if (distance(y, x, py, px) > dis) break;
	}

	if (!attempts_left)
	{
		if (cheat_xtra || cheat_hear)
		{
			msg_print("Warning! Could not allocate a new monster.");
		}

		return FALSE;
	}

	/* Attempt to place the monster, allow groups */
	if (place_monster(y, x, slp, TRUE)) return (TRUE);

	/* Nope */
	return (FALSE);
}




/*
 * Hack -- the "type" of the current "summon specific"
 */
static int summon_specific_type = 0;


/*
 * Hack -- help decide if a monster race is "okay" to summon
 */
static bool summon_specific_okay(int r_idx)
{
	monster_race *r_ptr = &r_info[r_idx];

	bool okay = FALSE;
	int i;

	/* Player ghosts cannot be summoned. */
	if (r_ptr->flags2 & (RF2_PLAYER_GHOST)) return (FALSE);

	/* Hack -- no specific type specified */
	if (!summon_specific_type) return (TRUE);


	/* Check our requirements */
	switch (summon_specific_type)
	{

		case SUMMON_ANT:
		{
			okay = ((r_ptr->d_char == 'a') &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));
 			break;
 		}


		case SUMMON_SPIDER:
		{
			okay = ((r_ptr->d_char == 'S') &&
			        !(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_HOUND:
		{
			okay = (((r_ptr->d_char == 'C') || (r_ptr->d_char == 'Z')) &&
			        !(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_HYDRA:
		{
			okay = ((r_ptr->d_char == 'M') &&
			        !(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_AINU:
		{
			okay = ((r_ptr->d_char == 'A') &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));

			break;
		}

		case SUMMON_DEMON:
		{
			okay = ((r_ptr->flags3 & (RF3_DEMON)) &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}


		case SUMMON_UNDEAD:
		{
			okay = ((r_ptr->flags3 & (RF3_UNDEAD)) &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));

			break;
		}

		case SUMMON_DRAGON:
		{
			okay = ((r_ptr->flags3 & (RF3_DRAGON)) &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_HI_DEMON:
		{
			okay = (r_ptr->d_char == 'U');
			break;
		}

		case SUMMON_HI_UNDEAD:
		{
			okay = ((r_ptr->d_char == 'L') ||
			        (r_ptr->d_char == 'V') ||
			        (r_ptr->d_char == 'W'));
			break;
		}


		case SUMMON_HI_DRAGON:
		{
			okay = (r_ptr->d_char == 'D');
			break;
		}

		case SUMMON_WRAITH:
		{
			okay = ((r_ptr->d_char == 'W') &&
			        (r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_UNIQUE:
		{
			if ((r_ptr->flags1 & (RF1_UNIQUE)) != 0) okay = TRUE;
			break;
		}

		case SUMMON_HI_UNIQUE:
		{
			if (((r_ptr->flags1 & (RF1_UNIQUE)) != 0) &&
				(r_ptr->level > (MAX_DEPTH / 2))) okay = TRUE;
			break;
		}


		case SUMMON_KIN:
		{
			okay = ((r_ptr->d_char == summon_kin_type) &&
				!(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_ANIMAL:
		{
			okay = ((r_ptr->flags3 & (RF3_ANIMAL)) &&
			        !(r_ptr->flags1 & (RF1_UNIQUE)));
			break;
		}

		case SUMMON_BERTBILLTOM:
		{
			okay = ((r_ptr->d_char == 'T') &&
				(r_ptr->flags1 & (RF1_UNIQUE)) &&
				  ((strstr((r_name + r_ptr->name), "Bert")) ||
				   (strstr((r_name + r_ptr->name), "Bill")) ||
				   (strstr((r_name + r_ptr->name), "Tom" ))));
			break;
		}


		case SUMMON_THIEF:
		{
			int effect;

			/* Scan through all the blows */
			for (i = 0; i < MONSTER_BLOW_MAX; i++)
			{
				/* Extract information about the blow effect */
				effect = r_ptr->blow[i].effect;
				if (effect == RBE_EAT_GOLD) okay = TRUE;
				if (effect == RBE_EAT_ITEM) okay = TRUE;
			}
			break;
		}

		default:
		{
			break;
		}

	}

	/* Result */
	return (okay);
}


/*
 * Place a monster (of the specified "type") near the given
 * location.  Return TRUE if a monster was actually summoned.
 *
 * We will attempt to place the monster up to 20 times before giving up.
 *
 * Note: SUMMON_UNIQUE and SUMMON_WRAITH (XXX) will summon Uniques
 * Note: SUMMON_HI_UNDEAD and SUMMON_HI_DRAGON may summon Uniques
 * Note: None of the other summon codes will ever summon Uniques.
 *
 * We usually do not summon monsters greater than the given depth.  -LM-
 *
 * Note that we use the new "monster allocation table" creation code
 * to restrict the "get_mon_num()" function to the set of "legal"
 * monsters, making this function much faster and more reliable.
 *
 * Note that this function may not succeed, though this is very rare.
 */
bool summon_specific(int y1, int x1, int lev, int type)
{
	int i, x, y, r_idx;

	monster_type *m_ptr;

	/*hack - no summoning on themed levels*/
	if (feeling >= LEV_THEME_HEAD) return (FALSE);

	/* Look for a location */
	for (i = 0; i < 20; ++i)
	{
		/* Pick a distance */
		int d = (i / 15) + 1;

		/* Pick a location */
		scatter(&y, &x, y1, x1, d, 0);

		/* Require "empty" floor grid */
		if (!cave_empty_bold(y, x)) continue;

		/* Hack -- no summon on glyph of warding */
		if (cave_feat[y][x] == FEAT_GLYPH) continue;

		/* Okay */
		break;
	}

	/* Failure */
	if (i == 20) return (FALSE);

	/* Save the "summon" type */
	summon_specific_type = type;

	/* Require "okay" monsters */
	get_mon_num_hook = summon_specific_okay;

	/* Prepare allocation table */
	get_mon_num_prep();

	/* Pick a monster, using the given level */
	r_idx = get_mon_num(lev);

	/* Remove restriction */
	get_mon_num_hook = NULL;

	/* Prepare allocation table */
	get_mon_num_prep();

	/* Handle failure */
	if (!r_idx) return (FALSE);

	/* Attempt to place the monster (awake, allow groups) */
	if (!place_monster_aux(y, x, r_idx, FALSE, TRUE)) return (FALSE);

	/*hack - summoned monsters don't try to mimic*/
	m_ptr = &mon_list[cave_m_idx[y][x]];
	m_ptr->mimic_k_idx = 0;

	/* Success */
	return (TRUE);
}


/*
 * Change monster fear.
 *
 * Monsters can be frightened or panicking.  In both cases, they try to
 * retreat, but when actually panicking, they cannot cast spells that don't
 * either heal or move them.
 */
void set_mon_fear(monster_type *m_ptr, int v, bool panic)
{
	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	/*hack - monsters who cannot be scared are unaffected*/
	if (r_ptr->flags3 & (RF3_NO_FEAR)) v = 0;

	/* Set monfear */
	m_ptr->monfear = v;

	/* Monster is panicking */
	if ((m_ptr->monfear) && (panic)) m_ptr->min_range = PANIC_RANGE;

	/* Otherwise, reset monster combat ranges (later) */
	else m_ptr->min_range = 0;
}



/*
 * Let the given monster attempt to reproduce.
 *
 * Note that "reproduction" REQUIRES empty space.
 */
bool multiply_monster(int m_idx)
{
	monster_type *m_ptr = &mon_list[m_idx];
	monster_race *r_ptr = &r_info[m_ptr->r_idx];

 	int i, y, x;

 	bool result = FALSE;

	u16b grid[8];
	int grids = 0;

	/* Scan the adjacent floor grids */
	for (i = 0; i < 8; i++)
	{
		y = m_ptr->fy + ddy_ddd[i];
		x = m_ptr->fx + ddx_ddd[i];

		/* Must be fully in bounds */
		if (!in_bounds_fully(y, x)) continue;

		/* This grid is OK for this monster (should monsters be able to dig?) */
		if (cave_exist_mon(r_ptr, y, x, FALSE, FALSE))
		{
			/* Save this grid */
			grid[grids++] = GRID(y, x);
		}
	}

	/* No grids available */
	if (!grids) return (FALSE);

	/* Pick a grid at random */
	i = rand_int(grids);

	/* Get the coordinates */
	y = GRID_Y(grid[i]);
	x = GRID_X(grid[i]);

	/* Create a new monster (awake, no groups) */
	result = place_monster_aux(y, x, m_ptr->r_idx, FALSE, FALSE);

 	/* Result */
 	return (result);
}


/*
 * Dump a message describing a monster's reaction to damage
 *
 * Technically should attempt to treat "Beholder"'s as jelly's
 */
void message_pain(int m_idx, int dam)
{
	long oldhp, newhp, tmp;
	int percentage;

	monster_type *m_ptr = &mon_list[m_idx];
	monster_race *r_ptr = &r_info[m_ptr->r_idx];

	char m_name[80];


	/* Get the monster name */
	monster_desc(m_name, sizeof(m_name), m_ptr, 0);

	/* Notice non-damage */
	if (dam == 0)
	{
		msg_format("%^s is unharmed.", m_name);
		return;
	}

	/* Note -- subtle fix -CFT */
	newhp = (long)(m_ptr->hp);
	oldhp = newhp + (long)(dam);
	tmp = (newhp * 100L) / oldhp;
	percentage = (int)(tmp);


	/* Floating Eyes, Jelly's, Mold's, Vortex's, Quthl's */
	if (strchr("ejmvQ", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s barely notices.", m_name);
		else if (percentage > 75)
			msg_format("%^s flinches.", m_name);
		else if (percentage > 50)
			msg_format("%^s squelches.", m_name);
		else if (percentage > 35)
			msg_format("%^s quivers in pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s writhes about.", m_name);
		else if (percentage > 10)
			msg_format("%^s writhes in agony.", m_name);
		else
			msg_format("%^s jerks limply.", m_name);
	}

	/* Dogs and Hounds */
	else if (strchr("CZ", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s shrugs off the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s snarls with pain.", m_name);
		else if (percentage > 50)
			msg_format("%^s yelps in pain.", m_name);
		else if (percentage > 35)
			msg_format("%^s howls in pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s howls in agony.", m_name);
		else if (percentage > 10)
			msg_format("%^s writhes in agony.", m_name);
		else
			msg_format("%^s yelps feebly.", m_name);
	}

	/* Snakes, Reptiles, Centipedes, Mimics */
	else if (strchr("cJR", r_ptr->d_char) ||
	         r_ptr->flags1 & (RF1_CHAR_MIMIC))
	{
		if (percentage > 95)
			msg_format("%^s barely notices.", m_name);
		else if (percentage > 75)
			msg_format("%^s hisses.", m_name);
		else if (percentage > 50)
			msg_format("%^s rears up in anger.", m_name);
		else if (percentage > 35)
			msg_format("%^s hisses furiously.", m_name);
		else if (percentage > 20)
			msg_format("%^s writhes about.", m_name);
		else if (percentage > 10)
			msg_format("%^s writhes in agony.", m_name);
		else
			msg_format("%^s jerks limply.", m_name);
	}

	/* Felines */
	else if (strchr("f", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s shrugs off the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s snarls.", m_name);
		else if (percentage > 50)
			msg_format("%^s growls angrily.", m_name);
		else if (percentage > 35)
			msg_format("%^s hisses with pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s mewls in pain.", m_name);
		else if (percentage > 10)
			msg_format("%^s hisses in agony.", m_name);
		else
			msg_format("%^s mewls pitifully.", m_name);
	}

	/* Ants, Lice, Flies, Insects, Beetles, Spiders */
	else if (strchr("alFIKS", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s ignores the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s drones angrily.", m_name);
		else if (percentage > 50)
			msg_format("%^s scuttles about.", m_name);
		else if (percentage > 35)
			msg_format("%^s twitches in pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s jerks in pain.", m_name);
		else if (percentage > 10)
			msg_format("%^s jerks in agony.", m_name);
		else
			msg_format("%^s jerks feebly.", m_name);
	}

	/* Birds */
	else if (strchr("B", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s shrugs off the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s flaps angrily.", m_name);
		else if (percentage > 50)
			msg_format("%^s jeers in pain.", m_name);
		else if (percentage > 35)
			msg_format("%^s squawks with pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s twitters in agony.", m_name);
		else if (percentage > 10)
			msg_format("%^s flutters about.", m_name);
		else
			msg_format("%^s chirps feebly.", m_name);
	}

	/* Skeletons (ignore, rattle, stagger) */
	else if (strchr("s", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s ignores the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s jerks.", m_name);
		else if (percentage > 50)
			msg_format("%^s rattles.", m_name);
		else if (percentage > 35)
			msg_format("%^s clatters.", m_name);
		else if (percentage > 20)
			msg_format("%^s shakes.", m_name);
		else if (percentage > 10)
			msg_format("%^s staggers.", m_name);
		else
			msg_format("%^s crumples.", m_name);
	}

	/* Zombies and Mummies (ignore, groan, stagger) */
	else if (strchr("z", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s ignores the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s grunts.", m_name);
		else if (percentage > 50)
			msg_format("%^s jerks.", m_name);
		else if (percentage > 35)
			msg_format("%^s moans.", m_name);
		else if (percentage > 20)
			msg_format("%^s groans.", m_name);
		else if (percentage > 10)
			msg_format("%^s hesitates.", m_name);
		else
			msg_format("%^s staggers.", m_name);
	}

	/* One type of monsters (ignore,squeal,shriek) */
	else if (strchr("XMbqrt", r_ptr->d_char))
	{
		if (percentage > 95)
			msg_format("%^s ignores the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s grunts with pain.", m_name);
		else if (percentage > 50)
			msg_format("%^s squeals in pain.", m_name);
		else if (percentage > 35)
			msg_format("%^s shrieks in pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s shrieks in agony.", m_name);
		else if (percentage > 10)
			msg_format("%^s writhes in agony.", m_name);
		else
			msg_format("%^s cries out feebly.", m_name);
	}

	/* Another type of monsters (shrug,cry,scream) */
	else
	{
		if (percentage > 95)
			msg_format("%^s shrugs off the attack.", m_name);
		else if (percentage > 75)
			msg_format("%^s grunts with pain.", m_name);
		else if (percentage > 50)
			msg_format("%^s cries out in pain.", m_name);
		else if (percentage > 35)
			msg_format("%^s screams in pain.", m_name);
		else if (percentage > 20)
			msg_format("%^s screams in agony.", m_name);
		else if (percentage > 10)
			msg_format("%^s writhes in agony.", m_name);
		else
			msg_format("%^s cries out feebly.", m_name);
	}
}



/*
 * Monster learns about an "observed" resistance.
 *
 * The LRN_xxx const indicates the type of resistance to be
 * investigated.
 *
 * SM_xxx flags are set appropriately.
 *
 * -DRS-, -BR-
 */
 void update_smart_learn(int m_idx, int what)
 {
 	monster_type *m_ptr = &mon_list[m_idx];

 	monster_race *r_ptr = &r_info[m_ptr->r_idx];

 	/* Too stupid to learn anything */
 	if (r_ptr->flags2 & (RF2_STUPID)) return;

 	/* Not intelligent, only learn sometimes */
 	if (!(r_ptr->flags2 & (RF2_SMART)) && (rand_int(100) < 50)) return;

 	/* XXX XXX XXX */

 	/* Analyze the knowledge */
 	switch (what)
 	{

		/* Slow/paralyze attacks learn about free action and saving throws */
		case LRN_FREE_SAVE:
 		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
 			if (p_ptr->free_act) m_ptr->smart |= (SM_IMM_FREE);
			else m_ptr->smart &= ~(SM_IMM_FREE);
 			break;
 		}

		/* Mana attacks learn if you have any mana to attack */
		case LRN_MANA:
 		{
 			if (!p_ptr->msp) m_ptr->smart |= (SM_IMM_MANA);
			else m_ptr->smart &= ~(SM_IMM_MANA);
 			break;
 		}

		/* Acid attacks learn about Acid resists and immunities */
		case LRN_ACID:
 		{
 			if (p_ptr->resist_acid) m_ptr->smart |= (SM_RES_ACID);
			else m_ptr->smart &= ~(SM_RES_ACID);
 			if (p_ptr->oppose_acid) m_ptr->smart |= (SM_OPP_ACID);
			else m_ptr->smart &= ~(SM_OPP_ACID);
 			if (p_ptr->immune_acid) m_ptr->smart |= (SM_IMM_ACID);
			else m_ptr->smart &= ~(SM_IMM_ACID);
 			break;
 		}

		/* Electircal attacks learn about Electrical resists and immunities */
		case LRN_ELEC:
 		{
 			if (p_ptr->resist_elec) m_ptr->smart |= (SM_RES_ELEC);
			else m_ptr->smart &= ~(SM_RES_ELEC);
 			if (p_ptr->oppose_elec) m_ptr->smart |= (SM_OPP_ELEC);
			else m_ptr->smart &= ~(SM_OPP_ELEC);
 			if (p_ptr->immune_elec) m_ptr->smart |= (SM_IMM_ELEC);
			else m_ptr->smart &= ~(SM_IMM_ELEC);
 			break;
 		}

		/* Fire attacks learn about Fire resists and immunities */
		case LRN_FIRE:
 		{
 			if (p_ptr->resist_fire) m_ptr->smart |= (SM_RES_FIRE);
			else m_ptr->smart &= ~(SM_RES_FIRE);
 			if (p_ptr->oppose_fire) m_ptr->smart |= (SM_OPP_FIRE);
			else m_ptr->smart &= ~(SM_OPP_FIRE);
 			if (p_ptr->immune_fire) m_ptr->smart |= (SM_IMM_FIRE);
			else m_ptr->smart &= ~(SM_IMM_FIRE);
 			break;
 		}

		/* Cold attacks learn about Cold resists and immunities */
		case LRN_COLD:
 		{
 			if (p_ptr->resist_cold) m_ptr->smart |= (SM_RES_COLD);
			else m_ptr->smart &= ~(SM_RES_COLD);
			if (p_ptr->oppose_cold) m_ptr->smart |= (SM_OPP_COLD);
			else m_ptr->smart &= ~(SM_OPP_COLD);
 			if (p_ptr->immune_cold) m_ptr->smart |= (SM_IMM_COLD);
			else m_ptr->smart &= ~(SM_IMM_COLD);
 			break;
 		}

		/* Poison attacks learn about Poison resists */
		case LRN_POIS:
 		{
 			if (p_ptr->resist_pois) m_ptr->smart |= (SM_RES_POIS);
			else m_ptr->smart &= ~(SM_RES_POIS);
 			if (p_ptr->oppose_pois) m_ptr->smart |= (SM_OPP_POIS);
			else m_ptr->smart &= ~(SM_OPP_POIS);
			if (p_ptr->immune_pois) m_ptr->smart |= (SM_IMM_POIS);
			else m_ptr->smart &= ~(SM_IMM_POIS);
 			break;
 		}

		/* Fear attacks learn about resist fear and saving throws */
		case LRN_FEAR_SAVE:
 		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
 			if (p_ptr->resist_fear) m_ptr->smart |= (SM_RES_FEAR);
			else m_ptr->smart &= ~(SM_RES_FEAR);
 			break;
 		}

		/* Light attacks learn about light and blindness resistance */
		case LRN_LITE:
 		{
 			if (p_ptr->resist_lite) m_ptr->smart |= (SM_RES_LITE);
			else m_ptr->smart &= ~(SM_RES_LITE);
			if (p_ptr->resist_blind) m_ptr->smart |= (SM_RES_BLIND);
			else m_ptr->smart &= ~(SM_RES_BLIND);
 			break;
 		}

		/* Darkness attacks learn about dark and blindness resistance */
		case LRN_DARK:
 		{
 			if (p_ptr->resist_dark) m_ptr->smart |= (SM_RES_DARK);
			else m_ptr->smart &= ~(SM_RES_DARK);
			if (p_ptr->resist_blind) m_ptr->smart |= (SM_RES_BLIND);
			else m_ptr->smart &= ~(SM_RES_BLIND);
 			break;
 		}

		/*
		 * Some Blindness attacks learn about blindness resistance
		 * Others (below) do more
		 */
		case LRN_BLIND:
 		{
 			if (p_ptr->resist_blind) m_ptr->smart |= (SM_RES_BLIND);
			else m_ptr->smart &= ~(SM_RES_BLIND);
 			break;
 		}

		/*
		 * Some Confusion attacks learn about confusion resistance
		 * Others (below) do more
		 */
		case LRN_CONFU:
 		{
 			if (p_ptr->resist_confu) m_ptr->smart |= (SM_RES_CONFU);
			else m_ptr->smart &= ~(SM_RES_CONFU);
 			break;
 		}

		/*
		 * Some sound attacks learn about sound and confusion resistance, and saving throws
		 * Others (below) do less.
		 */
		case LRN_SOUND:
 		{
 			if (p_ptr->resist_sound) m_ptr->smart |= (SM_RES_SOUND);
			else m_ptr->smart &= ~(SM_RES_SOUND);
 			break;
 		}

		/* Shards attacks learn about shards resistance */
		case LRN_SHARD:
 		{
			if (p_ptr->resist_shard) m_ptr->smart |= (SM_RES_SHARD);
			else m_ptr->smart &= ~(SM_RES_SHARD);
 			break;
 		}

		/*
		 *  Some Nexus attacks learn about Nexus resistance only
		 *  Others (below) do more
		 */
		case LRN_NEXUS:
 		{
 			if (p_ptr->resist_nexus) m_ptr->smart |= (SM_RES_NEXUS);
			else m_ptr->smart &= ~(SM_RES_NEXUS);
 			break;
 		}

		/* Nether attacks learn about Nether resistance */
		case LRN_NETHR:
 		{
 			if (p_ptr->resist_nethr) m_ptr->smart |= (SM_RES_NETHR);
			else m_ptr->smart &= ~(SM_RES_NETHR);
 			break;
 		}

		/* Chaos attacks learn about Chaos, Confusion and Nether resistance */
		case LRN_CHAOS:
 		{
 			if (p_ptr->resist_chaos) m_ptr->smart |= (SM_RES_CHAOS);
			else m_ptr->smart &= ~(SM_RES_CHAOS);
			if (p_ptr->resist_nethr) m_ptr->smart |= (SM_RES_NETHR);
			else m_ptr->smart &= ~(SM_RES_NETHR);
			if (p_ptr->resist_confu) m_ptr->smart |= (SM_RES_CONFU);
			else m_ptr->smart &= ~(SM_RES_CONFU);
 			break;
 		}

		/* Disenchantment attacks learn about disenchantment resistance */
		case LRN_DISEN:
 		{
 			if (p_ptr->resist_disen) m_ptr->smart |= (SM_RES_DISEN);
			else m_ptr->smart &= ~(SM_RES_DISEN);
 			break;
 		}

		/* Some attacks learn only about saving throws (cause wounds, etc) */
		case LRN_SAVE:
		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
		}

		/* Archery attacks don't learn anything */
		case LRN_ARCH:
		{
			break;
		}

		/* Poison archery attacks learn about poison resists */
		case LRN_PARCH:
		{
			if (p_ptr->resist_pois) m_ptr->smart |= (SM_RES_POIS);
			else m_ptr->smart &= ~(SM_RES_POIS);
			if (p_ptr->oppose_pois) m_ptr->smart |= (SM_OPP_POIS);
			else m_ptr->smart &= ~(SM_OPP_POIS);
			if (p_ptr->immune_pois) m_ptr->smart |= (SM_IMM_POIS);
			else m_ptr->smart &= ~(SM_IMM_POIS);
			break;
		}

		/* Ice attacks learn aboyt sound/shards/cold resists and cold immunity */
		case LRN_ICE:
		{
			if (p_ptr->resist_cold) m_ptr->smart |= (SM_RES_COLD);
			else m_ptr->smart &= ~(SM_RES_COLD);
			if (p_ptr->oppose_cold) m_ptr->smart |= (SM_OPP_COLD);
			else m_ptr->smart &= ~(SM_OPP_COLD);
			if (p_ptr->immune_cold) m_ptr->smart |= (SM_IMM_COLD);
			else m_ptr->smart &= ~(SM_IMM_COLD);
			if (p_ptr->resist_sound) m_ptr->smart |= (SM_RES_SOUND);
			else m_ptr->smart &= ~(SM_RES_SOUND);
			if (p_ptr->resist_shard) m_ptr->smart |= (SM_RES_SHARD);
			break;
		}

		/* Plasma attacks learn about sound */
		case LRN_PLAS:
		{
			if (p_ptr->resist_sound) m_ptr->smart |= (SM_RES_SOUND);
			else m_ptr->smart &= ~(SM_RES_SOUND);
			break;
		}

		/*
		 * Some sounds attacks learna about sound resistance only
		 * Others (above) do more
		 */
		case LRN_SOUND2:
		{
			if (p_ptr->resist_sound) m_ptr->smart |= (SM_RES_SOUND);
			else m_ptr->smart &= ~(SM_RES_SOUND);
			break;
		}

		/* Water and storm attacks learn about sound/confusion resists */
		case LRN_STORM:
		case LRN_WATER:
		{
			if (p_ptr->resist_sound) m_ptr->smart |= (SM_RES_SOUND);
			else m_ptr->smart &= ~(SM_RES_SOUND);
			if (p_ptr->resist_confu) m_ptr->smart |= (SM_RES_CONFU);
			else m_ptr->smart &= ~(SM_RES_CONFU);
		}

		/*
		 * Some nexus attacks learn about Nexus resist and saving throws
		 * Others (above) do more
		 */
		case LRN_NEXUS_SAVE:
		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
			if (p_ptr->resist_nexus) m_ptr->smart |= (SM_RES_NEXUS);
			break;
		}

		/*
		 * Some Blindness attacks learn about blindness resistance and saving throws
		 * Others (above) do less
		 */
		case LRN_BLIND_SAVE:
		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
			if (p_ptr->resist_blind) m_ptr->smart |= (SM_RES_BLIND);
			break;
		}

		/*
		 * Some Confusion attacks learn about confusion resistance and saving throws
		 * Others (above) do less
		 */
		case LRN_CONFU_SAVE:
		{
			if (p_ptr->skill_sav >= 75) m_ptr->smart |= (SM_GOOD_SAVE);
			else m_ptr->smart &= ~(SM_GOOD_SAVE);
			if (p_ptr->skill_sav >= 100) m_ptr->smart |= (SM_PERF_SAVE);
			else m_ptr->smart &= ~(SM_PERF_SAVE);
			if (p_ptr->resist_confu) m_ptr->smart |= (SM_RES_CONFU);
			else m_ptr->smart &= ~(SM_RES_CONFU);
			break;
		}

	}
}




