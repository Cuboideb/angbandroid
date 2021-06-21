/*
 * File: angdroid.c
 * Purpose: Native Angband port for Android platform
 *
 * Copyright (c) 2009 David Barr, Sergey N. Belinsky
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

#include <sys/time.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include "ui-keymap.h"
#include "ui-output.h"
#include "curses.h"
#include "grafmode.h"

#include "angband.h"
#include "cave.h"
#include "init.h" // init_file_paths()
#include "game-world.h"
#include "ui-term.h"
#include "ui-init.h"
#include "ui-display.h"
#include "ui-input.h"
#include "ui-prefs.h"
#include "ui-command.h"
#include "ui-game.h" // save_game()
#include "player-timed.h"
#include "monster.h"

static char variant_name[100];
static char android_files_path[1024];
static char android_savefile[50];
static int turn_save = 0;
static int initial_width = 80;
static int initial_height = 24;

static int initial_tile_wid = 1;
static int initial_tile_hgt = 1;
static int initial_graphics = 0;
static int initial_top_bar = 0;

/*
 * Android's terms are boring.
 */
typedef struct {
	term t;
	WINDOW *win;
} term_data;

static int set_bit(int n) {
	return (1 << (n));
}

static int make_mask(int n) {
	int i, mask = 0;
	for (i = 0; i < n; i++) {
		mask |= set_bit(i);
	}
	return mask;
}

#define MOUSE_TAG set_bit(30)

#define SPECIAL_CMD (-200)

typedef struct {
	int y;
	int x;
	int button;
} mouse_data_t;

#define PLAYER_PLAYING (player && player->upkeep && player->upkeep->playing && !player->is_dead)

#define IN_THE_DUNGEON (PLAYER_PLAYING && character_dungeon \
&& screen_save_depth == 0 && !player->is_dead)

static mouse_data_t mouse_data;

/*
 * Number of "term_data" structures to support XXX XXX XXX
 *
 * You MUST support at least one "term_data" structure, and the
 * game will currently use up to eight "term_data" structures if
 * they are available.
 *
 * Actually, MAX_TERM_DATA is now defined as eight in 'defines.h'.
 *
 * If only one "term_data" structure is supported, then a lot of
 * the things that would normally go into a "term_data" structure
 * could be made into global variables instead.
 */
#define MAX_AND_TERM 5

/*
 * An array of "term_data" structures, one for each "sub-window"
 */
static term_data data[MAX_AND_TERM];


/*** Function hooks needed by "Term" ***/

/*
 * Init a new "term"
 *
 * This function should do whatever is necessary to prepare a new "term"
 * for use by the "z-term.c" package.  This may include clearing the window,
 * preparing the cursor, setting the font/colors, etc.  Usually, this
 * function does nothing, and the "init_android()" function does it all.
 */
static void Term_init_android(term *t)
{
	term_data *td = (term_data*)(t->data);
}

/*
 * Nuke an old "term"
 *
 * This function is called when an old "term" is no longer needed.  It should
 * do whatever is needed to clean up before the program exits, such as wiping
 * the screen, restoring the cursor, fixing the font, etc.  Often this function
 * does nothing and lets the operating system clean up when the program quits.
 */
static void Term_nuke_android(term *t)
{
	/*
	term_data *td = (term_data*)(t->data);
	if (td->win != stdscr) {
		delwin(td->win);
	}
	*/
}

void try_save(void) {
	/* flag to prevent re-entrant saving due to Term_xtra
	   being called in display functions during save */
	static bool save_in_progress = false;

	int should_save = (turn_save != turn && turn > 1);

	if (!save_in_progress
			&& should_save
			&& player != NULL
			// Save dead characters
			//&& !player->is_dead
			) {

		LOGD("TERM_XTRA_EVENT trying to save");

		save_in_progress = true;
		save_game();
		turn_save = turn;
		save_in_progress = false;
	} else {
		LOGD("TERM_XTRA_EVENT.save skipped");
	}
}

static bool only_text_variant()
{
	return (strcmp(variant_name, "angband") != 0);
		/*&& (strcmp(variant_name, "faangband") != 0);*/
}

static int get_input_from_ui(int wait)
{
	int key = angdroid_getch(wait);

	// Detect mouse press, be careful with negative numbers
	if ((key > 0) && ((key & MOUSE_TAG) != 0)) {

		// Get data, 3 bits for mods, 2 bits for button
		// and 10 bits for each coordinate
		int mods = ((key >> 22) & make_mask(3));
		// Just two buttons
		mouse_data.button = ((key >> 20) & make_mask(2));
		mouse_data.x = ((key >> 10) & make_mask(10));
		mouse_data.y = (key & make_mask(10));

		/*
		plog_fmt("Mouse - key:%X y:%d x:%d button:%d mods:%d",
			key, mouse_data.y, mouse_data.x,
			mouse_data.button, mods);
		*/

		// Encode mods in the button
		mouse_data.button |= (mods << 4);

		// Get rid of anything except the mouse bit
		key &= MOUSE_TAG;
	}

	return key;
}

void send_key_to_term(int key) {
	if (key == MOUSE_TAG) {
		Term_mousepress(mouse_data.x, mouse_data.y, mouse_data.button);
	}
	else {

		if (key == KTRL('X')) {
			LOGD("Quit and save");
		}

		if (key == -2) {
			LOGD("Quitting");
			quit(NULL);
		}

		Term_keypress(key, 0);
	}
}

int process_special_command(int key)
{
	char buf[2048] = "";
	char tmp[200] = "";
	char *pbuf = buf;
	int graf, trows, tcols, top_bar;

	key = 0;

	while (true) {
		key = angdroid_getch(0);
		if (key == 0 || key == SPECIAL_CMD) break;
		*pbuf++ = key;
	}
	pbuf = 0;

	if (sscanf(buf, "graphics:%d:%d:%d:%d",
		&graf, &trows, &tcols, &top_bar) == 4) {

		//if (!PLAYER_PLAYING) return 0;

		if (only_text_variant()) graf = 0;

		use_graphics = graf;
		current_graphics_mode = get_graphics_mode(use_graphics);
		tile_width = tcols;
		tile_height = trows;
#if defined(SIDEBAR_MODE)
		SIDEBAR_MODE = (top_bar ? SIDEBAR_NONE: SIDEBAR_LEFT);
#endif

		reset_visuals(true);
		return KTRL('R');
		//return 0;
	}

#if defined(HAS_FEED_KEYMAP)
	if (strncmp(buf, "macro:", 6) == 0) {

		my_strcpy(tmp, buf+6, sizeof(tmp));

		/*LOGD("Macro: %s", tmp);*/

		/* Just a simple character */
		if (strlen(tmp) <= 1) {
			return tmp[0];
		}

		feed_keymap(tmp);

		/* Return the first code */
		if (inkey_next) {
			keycode_t code = inkey_next->code;
			if (code) {
				++inkey_next;
			}
			if (!inkey_next->code) {
				inkey_next = NULL;
			}
			/*LOGD("Code: %u", code);*/
			return code;
		}

		return 0;
	}
#endif

	if (sscanf(buf, "resize:%d:%d", &tcols, &trows) == 2) {

		//if (!PLAYER_PLAYING) return 0;

		LOGD("TERM RESIZE trying to save %d %d", tcols, trows);

		Term_resize(tcols, trows);
		//return KTRL('R');
		return 0;
	}

	if (strcmp(buf, "redraw") == 0) {

		//if (!PLAYER_PLAYING) return 0;

		LOGD("TERM REDRAW");

		Term_redraw();
		return 0;
	}

	return 0;
}

/*
 * Do a "special thing" to the current "term"
 *
 * This function must react to a large number of possible arguments, each
 * corresponding to a different "action request" by the "z-term.c" package,
 * or by the application itself.
 *
 * The "action type" is specified by the first argument, which must be a
 * constant of the form "TERM_XTRA_*" as given in "z-term.h", and the second
 * argument specifies the "information" for that argument, if any, and will
 * vary according to the first argument.
 *
 * In general, this function should return zero if the action is successfully
 * handled, and non-zero if the action is unknown or incorrectly handled.
 */
static errr Term_xtra_android(int n, int v)
{
	term_data *td = (term_data*)(Term->data);
	jint ret;

	switch (n)
	{
		case TERM_XTRA_EVENT:
		{
			/*
			 * Process some pending events XXX XXX XXX
			 *
			 * Wait for at least one event if "v" is non-zero
			 * otherwise, if no events are ready, return at once.
			 * When "keypress" events are encountered, the "ascii"
			 * value corresponding to the key should be sent to the
			 * "Term_keypress()" function.  Certain "bizarre" keys,
			 * such as function keys or arrow keys, may send special
			 * sequences of characters, such as control-underscore,
			 * plus letters corresponding to modifier keys, plus an
			 * underscore, plus carriage return, which can be used by
			 * the main program for "macro" triggers.  This action
			 * should handle as many events as is efficiently possible
			 * but is only required to handle a single event, and then
			 * only if one is ready or "v" is true.
			 *
			 * This action is required.
			 */

			/*LOGD("Waiting key v: %d", v);*/

			/*
			if (inkey_next) {
				LOGD("has inkey_next");
				keycode_t code = inkey_next->code;
				if (code) {
					++inkey_next;
				}
				if (!inkey_next->code) {
					inkey_next = NULL;
				}
				return code;
			}
			*/

			int key = get_input_from_ui(v);

			/*LOGD("Initial key: %d", key);*/

			if (key == -1) {
				try_save();
			}
			else if (key == SPECIAL_CMD) {
				/*LOGD("Special command %d", key);*/
				key = process_special_command(key);
				if (key != 0) {
					send_key_to_term(key);
				}
			}
			else if (v == 0) {
				/*LOGD("v == 0");*/
				while (key != 0) {
					/*LOGD("Sending key %d", key);*/
					send_key_to_term(key);
					key = get_input_from_ui(v);
				}
			} else {
				/*LOGD("Common case %d", key);*/
				send_key_to_term(key);
			}

			return 0;
		}

		case TERM_XTRA_FLUSH:
		{
			flushinp();
			return 0;
		}

		case TERM_XTRA_CLEAR:
		{
			//clear();
			wclear(td->win);
			return 0;
		}

		case TERM_XTRA_SHAPE:
		{
			curs_set(v);
			return 0;
		}

		case TERM_XTRA_FRESH:
		{
			//refresh();
			wrefresh(td->win);
			return 0;
		}

		case TERM_XTRA_NOISE:
		{
			noise();
			return 0;
		}

		case TERM_XTRA_REACT:
		case TERM_XTRA_FROSH:
		case TERM_XTRA_BORED:
		case TERM_XTRA_ALIVE:
		case TERM_XTRA_LEVEL:
		{
			return 0;
		}

		case TERM_XTRA_DELAY:
		{
			/*
			 * Delay for some milliseconds XXX XXX XXX
			 */
			if (v > 0)
				usleep((unsigned int)1000 * v);

			return 0;
		}
	}

	/* Unknown or Unhandled action */
	return 1;
}

static errr Term_control_android(int what, const char *msg)
{
	const char *pbuf = msg;
	char buf[2048] = "";
	int n;

	if (what == TERM_CONTROL_VISUAL_STATE) {
		strnfmt(buf, sizeof(buf), "visual:%d:%d:%d",
			tile_height, tile_width, use_graphics);
		pbuf = buf;
	}

	if (what == TERM_CONTROL_CONTEXT && IN_THE_DUNGEON) {
		strnfmt(buf, sizeof(buf), "in_dungeon:1");

#if defined(HAS_KEYMAP_PACK)
		n = strlen(buf);
		keymap_pack(buf+n, sizeof(buf)-n);
#endif

		pbuf = buf;
	}

	return (errr)control_msg(what, pbuf);
}

/*
 * Display the cursor
 *
 * This routine should display the cursor at the given location
 * (x,y) in some manner.  On some machines this involves actually
 * moving the physical cursor, on others it involves drawing a fake
 * cursor in some form of graphics mode.  Note the "soft_cursor"
 * flag which tells "z-term.c" to treat the "cursor" as a "visual"
 * thing and not as a "hardware" cursor.
 */
static errr Term_curs_android(int x, int y)
{
	move(y, x);

	Term_control_android(TERM_CONTROL_SHOW_CURSOR, "small");

	return 0;
}

static errr Term_bigcurs_android(int x, int y)
{
	move(y, x);

	Term_control_android(TERM_CONTROL_SHOW_CURSOR, "big");

	return 0;
}

/*
 * Erase some characters
 *
 * This function should erase "n" characters starting at (x,y).
 *
 * You may assume "valid" input if the window is properly sized.
 */
static errr Term_wipe_android(int x, int y, int n)
{
	term_data *td = (term_data*)(Term->data);

	/*LOGD("Term_wipe_and");*/

	/* XXX XXX XXX */

	/* Place cursor */
	wmove(td->win, y, x);

	if (x + n >= td->t.wid)
		/* Clear to end of line */
		wclrtoeol(td->win);
	else
		/* Clear some characters */
		whline(td->win, ' ', n);

	/* Success */
	return 0;
}

/*
 * Draw some text on the screen
 */
static errr Term_text_android(int x, int y, int n, int a, const wchar_t *cp)
{
	term_data *td = (term_data*)(Term->data);

	int fg = a % MAX_COLORS;
	int bg;

	/* Handle background */
	switch (a / MAX_COLORS) {
		case BG_BLACK:  bg = COLOUR_DARK;  break;
		case BG_SAME:   bg = fg;           break;
		case BG_DARK:	bg = COLOUR_SHADE; break;
	}

	wmove(td->win, y, x);
	wattrset(td->win, fg);
	wbgattrset(td->win, bg);
	waddnwstr(td->win, n, cp);

	/* Success */
	return 0;
}

/*
 * Draw some attr/char pairs on the screen
 */
static errr Term_pict_android(int x, int y, int n,
	const int *ap, const wchar_t *cp,
	const int *tap, const wchar_t *tcp)
{
	int resu;
	int i;

	term_data *td = (term_data*)(Term->data);

	for (i = 0; i < n; i++) {

		int a = ap[i];
		byte c = (byte)cp[i];

		/* Ascii text */
		if ((c & 0x80) == 0) {
			int ta = COLOUR_DARK;
			byte tc = (byte)' ';
			switch ((a >> 8) & 0x03) {
				case BG_SAME: ta = a & 0x7F; break;
				case BG_DARK: ta = COLOUR_SHADE; break;
			}
			resu = waddtile(td->win, x, y, a, c,
				ta, tc);
		}
		/* Graphic tile */
		else {
			resu = waddtile(td->win, x, y, a, c,
				tap[i], (byte)tcp[i]);
		}
		if (resu) break;
	}
	return resu;
}

/*** Internal Functions ***/


/*
 * Instantiate a "term_data" structure
 *
 * This is one way to prepare the "term_data" structures and to
 * "link" the various informational pieces together.
 *
 * This function assumes that every window should be 80x24 in size
 * (the standard size) and should be able to queue 256 characters.
 * Technically, only the "main screen window" needs to queue any
 * characters, but this method is simple.  One way to allow some
 * variation is to add fields to the "term_data" structure listing
 * parameters for that window, initialize them in the "init_android()"
 * function, and then use them in the code below.
 *
 * Note that "activation" calls the "Term_init_android()" hook for
 * the "term" structure, if needed.
 */
static void term_data_link(int i)
{
	term_data *td = &data[i];
	term *t = &td->t;

	int tw = initial_width;
	int th = initial_height;

	if (i > 0) {
		tw = 80;
		th = 24;
	}

	/* Initialize the term */
	term_init(t, tw, th, 256);

	tile_width = initial_tile_wid;
	tile_height = initial_tile_hgt;

	t->complex_input = true;

	/* Choose "soft" or "hard" cursor XXX XXX XXX */
	/* A "soft" cursor must be explicitly "drawn" by the program */
	/* while a "hard" cursor has some "physical" existance and is */
	/* moved whenever text is drawn on the screen.  See "z-term.c". */
	t->soft_cursor = true;

	/* Use "Term_text()" even for "black" text XXX XXX XXX */
	/* See the "Term_text_android()" function above. */
	t->always_text = true;

	t->higher_pict = true;

	/* Ignore the "TERM_XTRA_BORED" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	 t->never_bored = true;

	/* Ignore the "TERM_XTRA_FROSH" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	t->never_frosh = true;

	/* Erase with "white space" XXX XXX XXX */
	t->attr_blank = COLOUR_WHITE;
	t->char_blank = ' ';

	/* Prepare the init/nuke hooks */
	t->init_hook = Term_init_android;
	t->nuke_hook = Term_nuke_android;

	/* Prepare the template hooks */
	t->xtra_hook = Term_xtra_android;
	t->curs_hook = Term_curs_android;
	t->bigcurs_hook = Term_bigcurs_android;
	t->wipe_hook = Term_wipe_android;
	t->text_hook = Term_text_android;
	t->pict_hook = Term_pict_android;
	t->control_hook = Term_control_android;
	t->dblh_hook = is_dh_tile;

#if defined(SIDEBAR_MODE)
	t->sidebar_mode = (initial_top_bar ? SIDEBAR_NONE: SIDEBAR_LEFT);
#endif

	/* Remember where we came from */
	t->data = td;

	/* Create record for window */
	td->win = getwin(i);

	/* Activate it */
	Term_activate(t);

	/* Global pointer */
	angband_term[i] = t;
}

static void hook_plog(const char* str)
{
	angdroid_warn(str);
}

/*
 * Hook to tell the user something, and then quit
 */
static void hook_quit(const char* str)
{
	LOGD("hook_quit()");
	angdroid_quit(str);
}

/*
 * Initialization function
 */
errr init_android(void)
{
	int i;

	/* Initialize globals XXX XXX XXX */

	/* Initialize "term_data" structures XXX XXX XXX */

	/* Initialize the "color_data" array XXX XXX XXX */

	/* Create windows (backwards!) */
	for (i = MAX_AND_TERM; i-- > 0; )
	//for (i = 0; i < MAX_AND_TERM; i++)
	{
		/* Link */
		term_data_link(i);
	}

	/* Success */
	return 0;
}

/*
 * Initialise colors
 */
void init_colors(void)
{
	int i;
	uint32_t color_data[BASIC_COLORS];

	for (i = 0; i < BASIC_COLORS; i++) {
		color_data[i] = ((uint32_t)(0xFF << 24))
			| ((uint32_t)(angband_color_table[i][1] << 16))
			| ((uint32_t)(angband_color_table[i][2] << 8))
			| ((uint32_t)(angband_color_table[i][3]));
	}

	initscr();
	for (i = 0; i < BASIC_COLORS; i++) {
		init_color(i, color_data[i]);
	}
	for (i = 0; i < BASIC_COLORS; i++) {
		init_pair(i, i, 0);
	}
}

/*
 * Init some stuff
 *
 * This function is used to keep the "path" variable off the stack.
 */

void init_android_stuff(void)
{
	LOGD("angdroid.init_android_stuff");

	/* Hack -- Add a path separator (only if needed) */
	if (!suffix(android_files_path, PATH_SEP))
		my_strcat(android_files_path, PATH_SEP, sizeof(android_files_path));

	LOGD("android_files_path %s", android_files_path);

	/* Prepare the filepaths */
	init_file_paths(android_files_path, android_files_path, android_files_path);
	if (!file_exists(android_files_path)) {
		/* Warning */
		plog_fmt("Unable to open the '%s' file.", android_files_path);
		quit("The Angband 'lib' folder is probably missing or misplaced.");
	}

	savefile_set_name(android_savefile, false, false);
}

static int translate_to_rogue(int key)
{
	size_t i, j;

	/* Go through all generic commands */
	for (j = 0; cmds_all[j].name != NULL; j++)
	{
		struct cmd_info *commands = cmds_all[j].list;
		/* Look into every group */
		for (i = 0; i < cmds_all[j].len; i++) {
		    // Original keymap
			if ((unsigned char)commands[i].key[0] == key) {
			    // Rogue keymap
				return (unsigned char)commands[i].key[1];
			}
		}
	}
	return 0;
}

static const char *get_command_desc(int key)
{
	size_t i, j;
	int mode = 0;

	/* Roguelike ? */
	//if (player && OPT(player, rogue_like_commands)) mode = 1;

	/* Go through all generic commands */
	for (j = 0; cmds_all[j].name != NULL; j++)
	{
		struct cmd_info *commands = cmds_all[j].list;
		/* Look into every group */
		for (i = 0; i < cmds_all[j].len; i++) {
			// Original keymap
			if ((unsigned char)commands[i].key[mode] == key) {
				// Rogue keymap
				return commands[i].desc;
			}
		}
	}

	return 0;
}

static char *get_command_list()
{
	char buf[4096] = "";
	char temp[50];

	size_t i, j;
	int mode = 0;

	/* Roguelike ? */
	//if (player && OPT(player, rogue_like_commands)) mode = 1;

	/* Go through all generic commands */
	for (j = 0; cmds_all[j].name != NULL; j++)
	{
		struct cmd_info *commands = cmds_all[j].list;
		/* Look into every group */
		for (i = 0; i < cmds_all[j].len; i++) {

			int key0 = (unsigned char)commands[i].key[0];
			int key1 = (unsigned char)commands[i].key[1];

			/* Add separator */
			if (buf[0] != 0) {
				my_strcat(buf, ",", sizeof(buf));
			}

			strnfmt(temp, sizeof(temp), "%d:%d", key0, key1);
			my_strcat(buf, temp, sizeof(buf));
		}
	}

	return strdup(buf);
}

static char *get_tilesets(void)
{
	char buf[2048] = "";
	char str[1024] = "";
	char sep[2] = {0,0};
	char *dirname;
	int i;
	graphics_mode *mode;

	for (i = 1; true; i++)  {

		mode = get_graphics_mode(i);

		if (mode == NULL) break;

		dirname = strrchr(mode->path, PATH_SEP[0]);
		if (dirname == NULL) {
			dirname = mode->path;
		}
		else {
			++dirname;
		}

		strnfmt(str, sizeof(str), "%s%d:%s:%s:%s:%d:%d:%d:%d:%d",
			sep, i, mode->menuname, dirname, mode->file,
			mode->cell_height, mode->cell_width,
			mode->alphablend, mode->overdrawRow, mode->overdrawMax);

		sep[0] = '#';

		my_strcat(buf, str, sizeof(buf));
	}

	return strdup(buf);
}

static char *get_gx_ascii(int ga, int gc)
{
	int i;
	char buf[100] = "";

	/*LOGD("r_max: %d", z_info->r_max);*/

	for (i = 0; i < z_info->r_max; i++) {

		struct monster_race *race = &r_info[i];

		/* Retrieve attr/char and compare */
		if (race->name
			&& monster_x_attr[race->ridx] == ga
			&& monster_x_char[race->ridx] == gc) {

			sprintf(buf, "monster;%d;%d", race->d_attr, race->d_char);
        	break;
        }
	}

	return strdup(buf);
}

static char *get_grid_info(int row, int col)
{
	char buf[1024] = "";

	if (cave == NULL) return 0;

	/*LOGD("row: %d, col: %d", row, col);*/

	row = (row - ROW_MAP) / MAX(tile_height,1) + angband_term[0]->offset_y;
	col = (col - COL_MAP) / MAX(tile_width,1) + angband_term[0]->offset_x;

	/*LOGD("row ------ : %d, col: %d", row, col);*/

	if (row < 0 || row >= cave->height) return 0;
	if (col < 0 || col >= cave->width) return 0;

	struct loc grid;
	grid.y = row;
	grid.x = col;

	int m_idx = square(cave, grid)->mon;

	/*LOGD("m_idx: %d", m_idx);*/

	if (m_idx < 0) {
		int pct = player->chp * 10 / MAX(player->mhp,1);
		sprintf(buf, "player_%d", pct);
	}
	else if (m_idx > 0) {
		struct monster *mon = cave_monster(cave, m_idx);
		if (mon != NULL && mon->race != NULL) {
			int pct = mon->hp * 10 / MAX(mon->maxhp,1);
			sprintf(buf, "monster_%d", pct);
		}
	}

	return strdup(buf);
}

char* queryString(const char* argv0)
{
	const char *CMD_DESC = "cmd_desc_";

	char *buf = 0;
	int ga, gc;
	int row, col;

	if (strncmp(argv0, CMD_DESC, strlen(CMD_DESC)) == 0) {
		// Find the respective key in roguelike mode
		int n = strlen(CMD_DESC);
		int key = atoi(argv0 + n);
		if (key == 0) return buf;
		const char *tmp = get_command_desc(key);
		if (tmp == 0) return buf;
		buf = strdup(tmp);
	}
	else if (strcmp(argv0, "cmd_list") == 0) {
		buf = get_command_list();
	}
	else if (strcmp(argv0, "get_tilesets") == 0) {
		buf = get_tilesets();
	}
	else if (sscanf(argv0, "get_grid_info_%d_%d", &row, &col) == 2) {
		buf = get_grid_info(row, col);
	}
	else if (sscanf(argv0, "get_gx_ascii_%d_%d", &ga, &gc) == 2) {
		buf = get_gx_ascii(ga, gc);
	}

	return buf;
}

int queryInt(const char* argv0) {
	int result = -1;
	const char *ROGUE_KEY = "rogue_key_";

	if (strcmp(argv0, "pv") == 0) {
		result = 1;
	}
	else if (strcmp(argv0, "px") == 0) {
		result = player->grid.x;
	}
	else if (strcmp(argv0, "playing") == 0) {
		result = 0;
		if (PLAYER_PLAYING) result = 1;
	}
	else if (strcmp(argv0, "in_the_dungeon") == 0) {
		result = 0;
		if (IN_THE_DUNGEON) result = 1;
	}
	else if (strcmp(argv0, "rl") == 0) {
		result = 0;
		if (player && OPT(player, rogue_like_commands)) result = 1;
	}
	else if (strcmp(argv0, "cant_run") == 0) {
		result = 0;
		if (IN_THE_DUNGEON && (
			player->timed[TMD_CONFUSED] ||
			player->timed[TMD_PARALYZED])) {
		 	result = 1;
		}
	}
	else if (strcmp(argv0, "msg_subw") == 0) {
		result = 0;
		if (PLAYER_PLAYING) {
			for (int i = 1; i < MAX_AND_TERM-1; i++) {
				if (window_flag[i] & (PW_MESSAGE)) {
					result = i;
					break;
				}
			}
		}
	}
	else if (strcmp(argv0, "life_pct") == 0 && player) {
		result = player->chp * 10 / MAX(player->mhp, 1);
	}
	// Starts with this pattern?
	else if (strncmp(argv0, ROGUE_KEY, strlen(ROGUE_KEY)) == 0) {
		// Find the respective key in roguelike mode
		int n = strlen(ROGUE_KEY);
		// Get the "n" character (encoded key)
		unsigned char key = argv0[n];
		if (key == 0) return 0;
		key = translate_to_rogue(key);
		return key;
	}
	else {
		result = -1; //unknown command
	}

	return result;
}

int queryResize(int width, int height)
{
	Term_resize(width, height);
	return 0;
}

void angdroid_process_argv(int i, const char* argv)
{
    int aux;

	switch(i) {
		case 0: //files path
			my_strcpy(android_files_path, argv, sizeof(android_files_path));
			break;
		case 1: //savefile
			my_strcpy(android_savefile, argv, sizeof(android_savefile));
			break;
	    case 2: // width
			aux = atoi(argv);
			if (aux > initial_width) {
				initial_width = aux;
			}
        	break;
        case 3: // height
    		aux = atoi(argv);
    		if (aux > initial_height) {
        		initial_height = aux;
    		}
    		break;
		case 4:
			sscanf(argv, "%d:%d:%d:%d", &initial_graphics, &initial_tile_hgt,
				&initial_tile_wid, &initial_top_bar);
			break;
		case 5:
			my_strcpy(variant_name, argv, sizeof(variant_name));
			if (only_text_variant()) {
				initial_graphics = 0;
			}
			break;
		default:
			break;
	}
}

void angdroid_main()
{
	init_colors();

	plog_aux = hook_plog;
	quit_aux = hook_quit;

	if (init_android() != 0) quit("Oops!");

	ANGBAND_SYS = "android";

	create_needed_dirs();

	/* Initialize some stuff */
	init_android_stuff();

	init_graphics_modes();

	use_graphics = initial_graphics;
	current_graphics_mode = get_graphics_mode(use_graphics);

	cmd_get_hook = textui_get_cmd;

	init_display();
	init_angband();
	textui_init();

	/* Wait for response */
	pause_line(Term);

	/* Hack - Force grapchis reload */
	control_msg(TERM_CONTROL_CONTEXT, "dummy");

	/* Play game */
	play_game(false);

	/* Free resources */
	textui_cleanup();
	cleanup_angband();

	quit(NULL);
}
