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

#include "config.hpp"
#include "defines.hpp"
#include "frontend.hpp"
#include "loadsave.hpp"
#include "main.hpp"
#include "util.hpp"
#include "variable.hpp"
#include "z-form.hpp"
#include "z-util.hpp"
#include "player_type.hpp"
#include "game.hpp"
#include "dungeon.hpp"
#include "init2.hpp"
#include "files.hpp"

#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

#include <boost/algorithm/string/predicate.hpp>

using boost::algorithm::equals;
using boost::algorithm::starts_with;

extern "C" {
#include "curses.h"
}

#ifndef false
#	define false 0
#endif

#ifndef true
#	define true 1
#endif

#define MAX_COLORS 256

#define TERM_CONTROL_LIST_KEYS 1
#define TERM_CONTROL_CONTEXT 2
#define TERM_CONTROL_VISUAL_STATE 4
#define TERM_CONTROL_SHOW_CURSOR 5

static char android_files_path[2048];
static char android_savefile[100];
static int turn_save = 0;
static int initial_width = 80;
static int initial_height = 24;

/*
 * Android's terms are boring
 */
typedef struct {
	term *t;
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

#define PLAYER_PLAYING (character_generated && alive && !death)

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

		save_dungeon();
		save_player();

		turn_save = turn;
		save_in_progress = false;
	} else {
		LOGD("TERM_XTRA_EVENT.save skipped");
	}
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

void send_key_to_term(term_data *td, int key) {

	if (key == MOUSE_TAG) {
		/*Term_mousepress(mouse_data.x, mouse_data.y, mouse_data.button);*/
	}
	else {

		if (key == KTRL('X')) {
			LOGD("Quit and save");
		}

		if (key == -2) {
			LOGD("Quitting");
			quit(NULL);
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

		Term_activate(td->t);

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

	control_msg(TERM_CONTROL_SHOW_CURSOR, "small");

	return 0;
}

/*
 * Draw some text on the screen
 */
static errr Term_text_android(WINDOW *win, int x, int y, int n,
	byte a, const char *cp)
{
	int fg = a;
	int bg = TERM_DARK;

	wmove(win, y, x);
	wattrset(win, fg);
	wbgattrset(win, bg);
	waddnstr(win, n, cp);

	/* Success */
	return 0;
}

static errr CheckEvent(term_data *td, bool wait)
{
	int v = wait ? 1: 0;

	int key = get_input_from_ui(v);

	if (key == -1) {
		try_save();
	}
	else if (key == SPECIAL_CMD) {
		key = process_special_command(key);
		if (key != 0) {
			send_key_to_term(td, key);
		}
	}
	else if (v == 0) {
		while (key != 0) {
			send_key_to_term(td, key);
			key = get_input_from_ui(v);
		}
	} else {
		send_key_to_term(td, key);
	}

	return 0;
}

/*** Internal Functions ***/
class AndroidFrontend final : public Frontend {

private:
	term_data *m_term_data;

public:
	explicit AndroidFrontend(term_data *term_data)
		: m_term_data(term_data)
	{
	}

	void init() final
	{
		// No action necessary
	}

	bool soft_cursor() const final
	{
		return true;
	}

	bool icky_corner() const final
	{
		return false;
	}

	void nuke() final
	{
		// No action necessary
	}

	void process_event(bool wait) final
	{
		CheckEvent(m_term_data, wait);
	}

	void flush_events() final
	{
		flushinp();
	}

	void clear() final
	{
		wclear(m_term_data->win);
	}

	void flush_output() final
	{
		wrefresh(m_term_data->win);
	}

	void noise() final
	{
	}

	void process_queued_events() final
	{
		CheckEvent(m_term_data, false);
	}

	void react() final
	{
	}

	void rename_main_window(std::string_view sv) final
	{
	}

	void draw_cursor(int x, int y) final
	{
		Term_curs_android(x, y);
	}

	void draw_text(int x, int y, int n, byte a, const char *s) final
	{
		Term_text_android(m_term_data->win, x, y, n, a, s);
	}
};

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
static term *term_data_init(term_data *td, int i)
{
	int tw = initial_width;
	int th = initial_height;

	if (i > 0) {
		tw = 80;
		th = 24;
	}

	/* Hack -- key buffer size */
	int num = (i == 0 ? 256 : 16);

	/* Initialize the term */
	td->t = term_init(tw, th, num, std::make_shared<AndroidFrontend>(td));

	/* Create record for window */
	td->win = getwin(i);

	/* Activate it */
	Term_activate(td->t);

	return td->t;
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

	/* Create windows (backwards!) */
	for (i = MAX_AND_TERM; i-- > 0; )
	{
		term_data *td = &data[i];

		/* Initialize the term_data */
		term *t = term_data_init(td, i);

		/* Save global entry */
		angband_term[i] = t;
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

static int translate_to_rogue(int key)
{
	return 0;
}

static const char *get_command_desc(int key)
{
	return 0;
}

static char *get_command_list()
{
	char buf[4096] = "";
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
		if (options && options->rogue_like_commands) result = 1;
	}
	else if (strcmp(argv0, "life_pct") == 0 && p_ptr) {
		result = p_ptr->chp * 10 / std::max((int)p_ptr->mhp, 1);
	}
	else if (strcmp(argv0, "cant_run") == 0) {
		result = 0;
		if (!IN_THE_DUNGEON ||
			command_cmd != 0 ||
			p_ptr->confused ||
			p_ptr->paralyzed) {
		 	result = 1;
		}
	}
	else if (strcmp(argv0, "msg_subw") == 0) {
		result = 0;
		if (PLAYER_PLAYING) {
			for (int i = 1; i < MAX_AND_TERM; i++) {
				if (window_flag[i] & (PW_MESSAGE)) {
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
			strcpy(android_files_path, argv);
			break;
		case 1: //savefile
			strcpy(android_savefile, argv);
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
			break;
		default:
			break;
	}
}

void angdroid_main()
{
	// Initialize game structure
	game = new Game();

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

	/* Append trailing separator */
	{
		int n = strlen(android_files_path);
		if (n > 0 && android_files_path[n-1] != *PATH_SEP) {
			strcat(android_files_path, PATH_SEP);
		}
	}

	init_file_paths(android_files_path);

	game->player_name = std::string(android_savefile);

	/* Process the player name */
	set_player_base(game->player_name);

	program_args program_args;

	/* Initialize */
	init_angband(program_args);

	/* Wait for response */
	pause_line(23);

	/* Hack -- Force graphics reload */
	control_msg(TERM_CONTROL_CONTEXT, "dummy");

	/* Play the game */
	play_game(program_args);

	quit(NULL);
}
