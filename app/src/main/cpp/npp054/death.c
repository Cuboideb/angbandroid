/*
 * File: death.c
 * Purpose: Handle the UI bits that happen after the character dies.
 *
 * Copyright (c) 1987 - 2007 Angband contributors, Diego Gonzalez, Jeff Greene
 *
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of either:
 *
 * a) the GNU General Public License as published by the Free Software
 *    Foundation, version 2, or
 *
 * b) the "Angband licence":
 *    This software may be copied and distributed for educational, research,
 *    and not for profit purposes provided that this copyright and statement
 *    are included in all such copies.  Other copyrights may also apply.
 */
#include "angband.h"
#include "ui-menu.h"
#include "cmds.h"


/*
 * Hack - save the time of death
 */
static time_t death_time = (time_t)0;

/*
 * Write formatted string `fmt` on line `y`, centred between points x1 and x2.
 */
static void put_str_centred(int y, int x1, int x2, const char *fmt, ...)
{
	va_list vp;
	char *tmp;
	size_t len;
	int x;

	/* Format into the (growable) tmp */
	va_start(vp, fmt);
	tmp = vformat(fmt, vp);
	va_end(vp);

	/* Centre now */
	len = strlen(tmp);
	x = x1 + ((x2-x1)/2 - len/2);

	put_str(tmp, y, x);
}


/*
 * Display the tombstone
 */
static void print_tomb(void)
{
	ang_file *fp;
	char buf[1024];
	int line = 0;


	Term_clear();

	/* Open the death file */
	path_build(buf, sizeof(buf), ANGBAND_DIR_FILE, "dead.txt");
	fp = file_open(buf, MODE_READ, -1);

	if (fp)
	{
		while (file_getl(fp, buf, sizeof(buf)))
			put_str(buf, line++, 0);

		file_close(fp);
	}

	line = 7;

	put_str_centred(line++, 8, 8+31, "%s", op_ptr->full_name);
	put_str_centred(line++, 8, 8+31, "the");
	if (p_ptr->total_winner)
		put_str_centred(line++, 8, 8+31, "Magnificent");
	else
		put_str_centred(line++, 8, 8+31, "%s", c_text + cp_ptr->title[(p_ptr->lev - 1) / 5]);

	line++;

	put_str_centred(line++, 8, 8+31, "%s", c_name + cp_ptr->name);
	put_str_centred(line++, 8, 8+31, "Level: %d", (int)p_ptr->lev);
	put_str_centred(line++, 8, 8+31, "Exp: %d", (int)p_ptr->exp);
	put_str_centred(line++, 8, 8+31, "AU: %d", (int)p_ptr->au);
	put_str_centred(line++, 8, 8+31, "Killed on Level %d", p_ptr->depth);
	put_str_centred(line++, 8, 8+31, "by %s.", p_ptr->died_from);

	line++;

	put_str_centred(line++, 8, 8+31, "on %-.24s", ctime(&death_time));
}

/*
 * Save a "bones" file for a dead character.  Now activated and (slightly)
 * altered (from Oangband).  Allows the inclusion of personalized strings.
 */
static void make_bones(void)
{
	ang_file *fp;

	char str[1024];

	int i;

	/* Ignore wizards */
	if (!(p_ptr->noscore & 0x00FF))
	{
		/* Ignore people who die in town */
		if (p_ptr->depth)
		{
			int level;
			char tmp[128];

			/* Slightly more tenacious saving routine. */
			for (i = 0; i < 10; i++)
			{
				/* Ghost hovers near level of death. */
				if (i == 0) level = p_ptr->depth;
				else level = p_ptr->depth + 5 - damroll(2, 4);
				if (level < 1) level = randint(4);

				/* XXX XXX XXX "Bones" name */
				sprintf(tmp, "bone.%03d", level);

				/* Build the filename */
				path_build(str, 1024, ANGBAND_DIR_BONE, tmp);

				/* Do not over-write a previous ghost */
				if (file_exists(str)) continue;

				/* If no file by that name exists, we can make a new one. */
				break;
			}

			/* Try to write a new "Bones File" */
			fp = file_open(str, MODE_WRITE, FTYPE_TEXT);

			/* Not allowed to write it?  Weird. */
			if (!fp) return;

			/* Save the info */
			if (op_ptr->full_name[0] != '\0')
			{
				char esc_name[80];

				/* Escape non-ascii characters */
				escape_latin1(esc_name, sizeof(esc_name), op_ptr->full_name);

				/* Store the converted name */
				file_putf(fp, "%s\n", esc_name);
			}

			else file_putf(fp, "Anonymous\n");

			file_putf(fp, "%d\n", p_ptr->psex);
			file_putf(fp, "%d\n", p_ptr->prace);
			file_putf(fp, "%d\n", p_ptr->pclass);

			/* Clear screen */
			Term_clear();

			/*Mark end of file*/
			file_putf(fp, "\n");

			/* Close and save the Bones file */
			file_close(fp);

			return;
		}
	}
}



/*
 * Know inventory and home items upon death
 */
static void death_knowledge(void)
{
	store_type *st_ptr = &store[STORE_HOME];
	object_type *o_ptr;

	int i;

	/* Know everything in the inven/equip */
	for (i = 0; i < ALL_INVEN_TOTAL; i++)
	{
		o_ptr = &inventory[i];
		if (!o_ptr->k_idx) continue;

		object_aware(o_ptr);
		object_known(o_ptr);
	}

	/* Know everything in the home */
	for (i = 0; i < st_ptr->stock_num; i++)
	{
		o_ptr = &st_ptr->stock[i];
		if (!o_ptr->k_idx) continue;

		object_aware(o_ptr);
		object_known(o_ptr);

		/* Fully known */
		o_ptr->ident |= (IDENT_MENTAL);
	}

	/* Hack -- Recalculate bonuses */
	p_ptr->update |= (PU_BONUS);
	handle_stuff();
}



/*
 * Display the winner crown
 */
static void display_winner(void)
{
	char buf[1024];
	ang_file *fp;

	int wid, hgt;
	int i = 2;
	int width = 0;


	path_build(buf, sizeof(buf), ANGBAND_DIR_FILE, "crown.txt");
	fp = file_open(buf, MODE_READ, -1);

	Term_clear();
	Term_get_size(&wid, &hgt);

	if (fp)
	{
		/* Get us the first line of file, which tells us how long the */
		/* longest line is */
		file_getl(fp, buf, sizeof(buf));
		sscanf(buf, "%d", &width);
		if (!width) width = 25;

		/* Dump the file to the screen */
		while (file_getl(fp, buf, sizeof(buf)))
			put_str(buf, i++, (wid/2) - (width/2));

		file_close(fp);
	}

	put_str_centred(i, 0, wid, "All Hail the Mighty %s!", sp_ptr->winner);

	flush();
	pause_line(Term->hgt - 1);
}


/*
 * Menu command: dump character dump to file.
 */
static void death_file(void *unused, const char *title)
{
	char buf[1024];
	char ftmp[80];

	(void)unused;
	(void)title;

	strnfmt(ftmp, sizeof(ftmp), "%s.txt", op_ptr->base_name);

	if (get_file(ftmp, buf, sizeof buf))
	{
		errr err;

		/* Dump a character file */
		screen_save();
		err = file_character(buf, FALSE);
		screen_load();

		/* Check result */
		if (err)
			msg_print("Character dump failed!");
		else
			msg_print("Character dump successful.");

		/* Flush messages */
		message_flush();
	}
}

/*
 * Menu command: view character dump and inventory.
 */
static void death_info(void *unused, const char *title)
{
	int i, j, k;
	object_type *o_ptr;
	store_type *st_ptr = &store[STORE_HOME];

	ui_event_data ke;

	bool done = FALSE;

	(void)unused;
	(void)title;

	screen_save();

	/* Display player */
	display_player(0);

	/* Prompt for inventory */
	prt("Hit any key to see more information (esc to abort): ", 0, 0);

	/* Buttons */
	button_backup_all();
	button_kill_all();
	button_add("ESC", ESCAPE);
	button_add("Continue", 'q');

	/* Allow abort at this point */
	ke = inkey_ex();
	if (ke.key == ESCAPE) done = TRUE;

	/* Show equipment and inventory */

	/* Equipment -- if any */
	if ((p_ptr->equip_cnt) && (!done))
	{
		Term_clear();
		item_tester_full = TRUE;
		show_equip(OLIST_WEIGHT);
		prt("You are using: -more-", 0, 0);
		ke = inkey_ex();
		if (ke.key == ESCAPE) done = TRUE;
	}

	/* Inventory -- if any */
	if ((p_ptr->inven_cnt)  && (!done))
	{
		Term_clear();
		item_tester_full = TRUE;
		show_inven(OLIST_WEIGHT);
		prt("You are carrying: -more-", 0, 0);
		(void)anykey();
	}

	/* Home -- if anything there */
	if ((st_ptr->stock_num) && (!done))
	{
		/* Display contents of the home */
		for (k = 0, i = 0; i < st_ptr->stock_num; k++)
		{
			/* Clear screen */
			Term_clear();

			/* Show 12 items */
			for (j = 0; (j < 12) && (i < st_ptr->stock_num); j++, i++)
			{
				byte attr;

				char o_name[80];
				char tmp_val[80];

				/* Get the object */
				o_ptr = &st_ptr->stock[i];

				/* Print header, clear line */
				strnfmt(tmp_val, sizeof(tmp_val), "%c) ", I2A(j));
				prt(tmp_val, j+2, 4);

				/* Get the object description */
				object_desc(o_name, sizeof(o_name), o_ptr, ODESC_PREFIX | ODESC_FULL);

				/* Get the inventory color */
				attr = tval_to_attr[o_ptr->tval % N_ELEMENTS(tval_to_attr)];

				/* Display the object */
				c_put_str(attr, o_name, j+2, 7);
			}

			/* Caption */
			prt(format("Your home contains (page %d): -more-", k+1), 0, 0);

			/* Wait for it */
			ke = inkey_ex();
			if (ke.key == ESCAPE)
			done = TRUE;
		}
	}

	screen_load();
}

/*
 * Menu command: peruse pre-death messages.
 */
static void death_messages(void *unused, const char *title)
{
	(void)unused;
	(void)title;

	screen_save();
	do_cmd_messages();
	screen_load();
}

/*
 * Menu command: see top twenty scores.
 */
static void death_scores(void *unused, const char *title)
{
	(void)unused;
	(void)title;

	screen_save();
	show_scores();
	screen_load();
}

/*
 * Menu command: examine items in the inventory.
 */
static void death_examine(void *unused, const char *title)
{
	int item;
	cptr q, s;

	(void)unused;
	(void)title;

	/* Get an item */
	q = "Examine which item? ";
	s = "You have nothing to examine.";

	while (get_item(&item, q, s, (USE_INVEN | USE_EQUIP | USE_QUIVER | IS_HARMLESS)))
	{
		/* Get the item */
		object_type *o_ptr = &inventory[item];

		/* Fully known */
		o_ptr->ident |= (IDENT_MENTAL);

		/* Describe */
		object_info_screen(o_ptr);
	}
}

/*
 * Menu command: allow spoiler generation (mainly for randarts).
 */
static void death_notes(void *unused, const char *title)
{
	(void)unused;
	(void)title;

	screen_save();
	do_cmd_knowledge_notes();
	screen_load();
}

/*
 * Menu command: allow spoiler generation (mainly for randarts).
 */
static void death_spoilers(void *unused, const char *title)
{
	(void)unused;
	(void)title;

	do_cmd_spoilers();
}

/*
 * Menu structures for the death menu. Note that Quit must always be the
 * last option, due to a hard-coded check in death_screen
 */
static menu_type death_menu;
static menu_action death_actions[] =
{
	{ 'i', "Information",   death_info,     NULL },
	{ 'm', "Messages",      death_messages, NULL },
	{ 'f', "File dump",     death_file,     NULL },
	{ 'v', "View scores",   death_scores,   NULL },
	{ 'x', "Examine items", death_examine,  NULL },
	{ 'n', "Notes",       	death_notes,  	NULL },
	{ 's', "Spoilers",	death_spoilers,	NULL },
	{ 'q', "Quit",          death_examine,  NULL },
};

/* Return the tag for a menu entry */
static char death_menu_tag(menu_type *menu, int oid)
{
	(void)menu;
	return death_actions[oid].id;
}

/* Display a menu entry */
static void death_menu_display(menu_type *menu, int oid, bool cursor, int row, int col, int width)
{
	byte attr = curs_attrs[CURS_KNOWN][(int)cursor];
	(void)menu;
	(void)width;
	c_prt(attr, death_actions[oid].name, row, col);
}

static const menu_iter death_iter =
{
	death_menu_tag,
	NULL,
	death_menu_display,
	NULL
};




/*
 * Handle character death
 */
void death_screen(void)
{
	menu_type *menu;
	const char cmd_keys[] = { ARROW_LEFT, ARROW_RIGHT, '\0' };
	const region area = { 51, 2, 0, N_ELEMENTS(death_actions) };

	int cursor = 0;
	ui_event_data c = EVENT_EMPTY;

	/* Dump bones file */
	make_bones();

	/* Retire in the town in a good state */
	if (p_ptr->total_winner)
	{
		p_ptr->depth = 0;
		my_strcpy(p_ptr->died_from, "Ripe Old Age", sizeof(p_ptr->died_from));
		p_ptr->exp = p_ptr->max_exp;
		p_ptr->lev = p_ptr->max_lev;
		p_ptr->au += 10000000L;

		display_winner();
	}

	/* Save dead player */
	if (!save_player())
	{
		msg_print("death save failed!");
		message_flush();
	}

	/* Get time of death */
	(void)time(&death_time);
	print_tomb();
	death_knowledge();
	enter_score(&death_time);

	/* Flush all input and output */
	flush();
	message_flush();


	/* Initialize the menu */
	menu = &death_menu;
	WIPE(menu, menu_type);
	menu->menu_data = death_actions;
	menu->flags = MN_CASELESS_TAGS;
	menu->cmd_keys = cmd_keys;
	menu->count = N_ELEMENTS(death_actions);

	menu_init(menu, MN_SKIN_SCROLL, &death_iter, &area);

	while (TRUE)
	{
		c = menu_select(&death_menu, &cursor, 0);

		if (c.key == ESCAPE || cursor == (menu->count -1))
		{
			if (get_check("Do you want to quit? "))
				break;
		}
		else if (c.type == EVT_SELECT && death_actions[cursor].action)
		{
			death_actions[cursor].action(death_actions[cursor].data, NULL);
		}
	}
}
