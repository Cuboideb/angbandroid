#include "angdroid.h"
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <setjmp.h>

#define COLOR_PAIR(x) (x)

#define LINES 24
#define COLS 80

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Angband", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Angband", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Angband", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Angband", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Angband", __VA_ARGS__) 
#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG  , "Angband", __VA_ARGS__)

typedef struct WINDOW_s {
	int w;
} WINDOW;
extern WINDOW* stdscr;

#define ERR 1
#define getyx(w, y, x)     (y = getcury(w), x = getcurx(w))

int attrset(int);
int wattrset(WINDOW*, int);
int bgattrset(int);
int wbgattrset(WINDOW*, int);
int attrget(int, int);
int wattrget(WINDOW*, int, int);
int addch(const char);
int addwch(const wchar_t);
int delch();
int waddch(WINDOW*, const char);
int waddwch(WINDOW*, const wchar_t);
int addstr(const char *);
int addwstr(const wchar_t *);
int waddstr(WINDOW*, const char *);
int waddwstr(WINDOW * , const wchar_t *);
int addnstr(int, const char *);
int addnwstr(int, const wchar_t *);
int waddnstr(WINDOW*, int, const char *);
int waddnwstr(WINDOW*, int, const wchar_t *);
int move(int, int);
int mvaddch(int, int, const char);
int mvaddwch(int, int, const wchar_t);
int mvwaddch(WINDOW *,int, int, const char);
int mvwaddwch(WINDOW *,int, int, const wchar_t);
int mvaddstr(int, int, const char *);
int mvaddwstr(int, int, const wchar_t *);
int whline(WINDOW*, const char, int);
int hline(const char, int);
int wclrtobot(WINDOW*);
int clrtobot(void);
int clrtoeol(void);
int wclrtoeol(WINDOW*);
#ifndef NO_CLEAR
int clear(void);
#endif
int wclear(WINDOW*);
int initscr(void);
int curs_set(int);
WINDOW* newwin(int,int,int,int);
int getcurx(WINDOW *);
int getcury(WINDOW *);
int overwrite(const WINDOW *, WINDOW *);
int touchwin(WINDOW *);
int delwin(WINDOW *);
int refresh(void);
int mvinch(int, int);
int mvwinch(WINDOW*, int, int);
int crmode();
int nonl();
int noecho();
int nl();
int echo();
int cbreak();
int nocbreak();
int notimeout(WINDOW *, int);
int endwin();
int has_colors();
int start_color();
int scrollok(WINDOW *, int); 
int scroll(WINDOW *); 
int intrflush(WINDOW *, int);
int beep(void);
int keypad(WINDOW *, int); 
int init_color(int, int);
int init_pair(int, int, int); 

int angdroid_getch(int v);
int flushinp(void);
int noise(void);

void angdroid_quit(const char*);
void angdroid_warn(const char*);

/* game must implement these */
void angdroid_process_argv(int, const char*);
void angdroid_main(void);
int queryInt(const char* argv0);
int queryResize(int width, int height);

/* game may implement these */
extern void (*angdroid_quit_hook)(void);
