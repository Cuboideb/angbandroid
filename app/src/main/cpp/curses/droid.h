#ifndef INCLUDED_DROID_H
#define INCLUDED_DROID_H

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
#define TERM_CONTROL_DEBUG 6

extern int Term_control(int what, const char *msg);
#define Term_control_keys(msg) Term_control(TERM_CONTROL_LIST_KEYS,msg)
/*extern int Term_control_ws(int what, int n, const wchar_t *msg);*/
extern int Term_control_context();
extern int Term_control_visuals();

/* For control keys in android */
extern void soft_kbd_flash(const char *keys);
extern void soft_kbd_linger(const char *keys);
extern void soft_kbd_clear(int force);
extern void soft_kbd_flush();

extern void strdeldup(char *str);

extern void feed_keymap(const char *buf);

#endif
