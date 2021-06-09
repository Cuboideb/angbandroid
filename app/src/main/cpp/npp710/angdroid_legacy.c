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
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include "curses.h"

#include "angband.h"
#include "init.h" // init_file_paths()
#include "z-term.h"

#ifndef false
#	define false 0
#endif

#ifndef true
#	define true 1
#endif

#define TERM_CONTROL_LIST_KEYS 1
#define TERM_CONTROL_CONTEXT 2
#define TERM_CONTROL_VISUAL_STATE 4
#define TERM_CONTROL_SHOW_CURSOR 5

static char variant_name[100];
static char android_files_path[1024];
static char android_savefile[50];
static int turn_save = 0;
static int initial_width = 80;
static int initial_height = 24;

static int initial_tile_wid = 1;
static int initial_tile_hgt = 1;
static int initial_graphics = 0;
static int initial_pseudo_ascii = 0;

/*
 * Android's terms are boring
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

#define PLAYER_PLAYING (character_generated && p_ptr && p_ptr->playing && !p_ptr->is_dead)

#define IN_THE_DUNGEON (PLAYER_PLAYING \
&& character_dungeon && (character_icky == 0))

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
#define MAX_AND_TERM 4

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
			&& character_generated
			&& character_dungeon) {

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

		// Translate key
		if (key == 0x9c) key = '\r';
		else if (key == 0x9d) key = '\t';
		else if (key == 0x9f) key = '\b';
		else if (key == 0xE000) key = '\033';
		else if (key == 0x80) key = '2';
		else if (key == 0x81) key = '4';
		else if (key == 0x82) key = '6';
		else if (key == 0x83) key = '8';
		else if (key == 0x94) key = '7';
		else if (key == 0x95) key = '9';
		else if (key == 0x96) key = '1';
		else if (key == 0x97) key = '3';
		/*
		else if (key == 0x80) key = SKEY_DOWN;
		else if (key == 0x81) key = SKEY_LEFT;
		else if (key == 0x82) key = SKEY_RIGHT;
		else if (key == 0x83) key = SKEY_UP;
		else if (key == 0x94) key = SKEY_TOP;
		else if (key == 0x95) key = SKEY_PGUP;
		else if (key == 0x96) key = SKEY_BOTTOM;
		else if (key == 0x97) key = SKEY_PGDOWN;
		*/

		Term_keypress(key);
	}
}

int process_special_command(int key)
{
	char buf[2048] = "";
	char *pbuf = buf;
	int trows, tcols;

	key = 0;

	while (true) {
		key = angdroid_getch(0);
		if (key == 0 || key == SPECIAL_CMD) break;
		*pbuf++ = key;
	}
	pbuf = 0;

	if (sscanf(buf, "resize:%d:%d", &tcols, &trows) == 2) {

		//if (!PLAYER_PLAYING) return 0;

		Term_resize(tcols, trows);
		//return KTRL('R');
		return 0;
	}

	if (strcmp(buf, "redraw") == 0) {

		//if (!PLAYER_PLAYING) return 0;

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

			int key = get_input_from_ui(v);

			if (key == -1) {
				try_save();
			}
			else if (key == SPECIAL_CMD) {
				key = process_special_command(key);
				if (key != 0) {
					send_key_to_term(key);
				}
			}
			else if (v == 0) {
				while (key != 0) {
					send_key_to_term(key);
					key = get_input_from_ui(v);
				}
			} else {
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
static errr Term_text_android(int x, int y, int n, byte a, cptr cp)
{
	term_data *td = (term_data*)(Term->data);

	/*int fg = GET_BASE_COLOR(a);*/
	int fg = a;
	int bg = TERM_DARK;

	/* Handle background */
	/*
	switch (a / MAX_COLOR) {
		case BG_BLACK:  bg = COLOUR_DARK;  break;
		case BG_SAME:   bg = fg;           break;
		case BG_DARK:	bg = COLOUR_SHADE; break;
	}
	*/

	wmove(td->win, y, x);
	wattrset(td->win, fg);
	wbgattrset(td->win, bg);
	waddnstr(td->win, n, cp);

	/* Success */
	return 0;
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

	/* Choose "soft" or "hard" cursor XXX XXX XXX */
	/* A "soft" cursor must be explicitly "drawn" by the program */
	/* while a "hard" cursor has some "physical" existance and is */
	/* moved whenever text is drawn on the screen.  See "z-term.c". */
	t->soft_cursor = true;

	/* Use "Term_text()" even for "black" text XXX XXX XXX */
	/* See the "Term_text_android()" function above. */
	t->always_text = true;

	/* Ignore the "TERM_XTRA_BORED" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	 t->never_bored = true;

	/* Ignore the "TERM_XTRA_FROSH" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	t->never_frosh = true;

	/* Erase with "white space" XXX XXX XXX */
	t->attr_blank = TERM_WHITE;
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
	//t->control_hook = Term_control_android;

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
	uint32_t color_data[MAX_COLORS];

	for (i = 0; i < MAX_COLORS; i++) {
		color_data[i] = ((uint32_t)(0xFF << 24))
			| ((uint32_t)(angband_color_table[i][1] << 16))
			| ((uint32_t)(angband_color_table[i][2] << 8))
			| ((uint32_t)(angband_color_table[i][3]));
	}

	initscr();
	for (i = 0; i < MAX_COLORS; i++) {
		init_color(i, color_data[i]);
	}
	for (i = 0; i < MAX_COLORS; i++) {
		init_pair(i, i, 0);
	}
}

/*
 * Init some stuff
 *
 * This function is used to keep the "path" variable off the stack.
 */

/*
bool file_exists(const char *fname)
{
	struct stat st;
	return (stat(fname, &st) == 0);
}
*/

void init_android_stuff(void)
{
	char temp[2048];

	LOGD("angdroid.init_android_stuff");

	/* Hack -- Add a path separator (only if needed) */
	if (!suffix(android_files_path, PATH_SEP))
		my_strcat(android_files_path, PATH_SEP, sizeof(android_files_path));

	LOGD("android_files_path %s", android_files_path);

	// HACK - NPP NEEDS TO WRITE HERE
	my_strcpy(temp, android_files_path, sizeof(temp));

	/* Prepare the filepaths */
	init_file_paths(temp, temp, temp);

	if (!file_exists(android_files_path)) {
		/* Warning */
		plog_fmt("Unable to open the '%s' file.", android_files_path);
		quit("The Angband 'lib' folder is probably missing or misplaced.");
	}

	my_strcpy(op_ptr->full_name, android_savefile, sizeof(op_ptr->full_name));
}

static int translate_to_rogue(int key)
{
#if 0
	// TODO
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
#endif

	return 0;
}

static const char *get_command_desc(int key)
{
#if 0
	// TODO
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
#endif

	return 0;
}

static char *get_command_list()
{
	char buf[4096] = "";

#if 0
	//TODO
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
#endif

	return strdup(buf);
}

static char *get_tilesets(void)
{
	char buf[2048] = "";
	return strdup(buf);
}

char* queryString(const char* argv0)
{
	const char *CMD_DESC = "cmd_desc_";

	char *buf = 0;

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

	return buf;
}

int queryInt(const char* argv0) {
	int result = -1;
	const char *ROGUE_KEY = "rogue_key_";

	if (strcmp(argv0, "pv") == 0) {
		result = 1;
	}
	else if (strcmp(argv0, "px") == 0) {
		//result = player->grid.x;
		result = p_ptr->px;
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
		if (rogue_like_commands) result = 1;
	}
	else if (strcmp(argv0, "life_pct") == 0 && p_ptr) {
		result = p_ptr->chp * 10 / MAX(p_ptr->mhp, 1);
	}
	else if (strcmp(argv0, "msg_subw") == 0) {
		result = 0;
		if (PLAYER_PLAYING && op_ptr) {
			for (int i = 1; i < MAX_AND_TERM; i++) {
				if (op_ptr->window_flag[i] & (PW_MESSAGE)) {
					result = i;
					break;
				}
			}
		}
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
			break;
		case 5:
			my_strcpy(variant_name, argv, sizeof(variant_name));
			break;
		default:
			break;
	}
}

static bool new_game = false;

/*
 * Pass the appropriate "Initialisation screen" command to the game,
 * getting user input if needed.
 */
static errr get_init_cmd(void)
{
	/* Wait for response */
	pause_line(Term->hgt - 1);

	if (new_game)
		cmd_insert(CMD_NEWGAME);
	else
		/* This might be modified to supply the filename in future. */
		cmd_insert(CMD_LOADFILE);

	/* Everything's OK. */
	return 0;
}

static errr default_get_cmd(cmd_context context, bool wait)
{
	if (context == CMD_INIT)
		return get_init_cmd();
	else
		return textui_get_cmd(context, wait);
}

void angdroid_main()
{
	init_colors();

	plog_aux = hook_plog;
	quit_aux = hook_quit;

	#ifdef SET_UID
		/* Default permissions on files */
		(void)umask(022);
		/* Get the user id (?) */
		player_uid = getuid();
		#ifdef SAFE_SETUID
			#ifdef _POSIX_SAVED_IDS
				/* Save some info for later */
				player_euid = geteuid();
				player_egid = getegid();
			#endif
		#endif
	#endif

	if (init_android() != 0) quit("Oops!");

	ANGBAND_SYS = "android";

	/* Initialize some stuff */
	init_android_stuff();

	process_player_name(true);

	/* Catch nasty signals */
	signals_init();

	/* Set up the command hook */
	cmd_get_hook = default_get_cmd;

	/* Set up the display handlers and things. */
	init_display();

	/* Erase screen */
	Term_clear();
	Term_fresh();

	/* Wait for response */
	/*pause_line(Term->hgt - 1);*/

	/* Hack -- Force graphics reload */
	control_msg(TERM_CONTROL_CONTEXT, "dummy");

	/* Play game */
	play_game();

	/* Free resources */
	cleanup_angband();

	quit(NULL);
}
