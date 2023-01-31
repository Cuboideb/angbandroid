#include "angband.h"
#include "curses.h"
#include "droid.h"

static char soft_kbd_buffer[256] = "";
static bool flashing_keys = true;

#if 0
int Term_control_ws(int what, int n, const wchar_t *ws)
{
	if (n == 0) {
		return (-1);
	}

	/* Make a copy */
	wchar_t* wbuf = mem_alloc(sizeof(wchar_t)*(n+1));
	memcpy(wbuf,ws,sizeof(wchar_t)*n);
	wbuf[n]=0;

	/* Transform to multi-byte */
	size_t len = wcstombs((char*)NULL, wbuf, (size_t)32000);
	char* s = mem_alloc(len+1);
	wcstombs(s, wbuf, len+1);
	mem_free(wbuf);

	errr status = Term_control(what, s);
	mem_free(s);
	return (status);
}
#endif

int Term_control_context()
{
	return Term_control(TERM_CONTROL_CONTEXT,"dummy");
}

int Term_control_visuals()
{
	return Term_control(TERM_CONTROL_VISUAL_STATE,"dummy");
}

int Term_control_quantity(const char *msg, int max_value, int initial_value)
{
	char buffer[2048];

	strnfmt(buffer, sizeof(buffer), "%d:%d:%s", max_value, initial_value, msg);
	return Term_control(TERM_CONTROL_QUANTITY, buffer);
}

void soft_kbd_flash(const char *keys)
{
	flashing_keys = true;
	my_strcat(soft_kbd_buffer, keys, sizeof(soft_kbd_buffer));
	strdeldup(soft_kbd_buffer);
}

void soft_kbd_linger(const char *keys)
{
	soft_kbd_flash(keys);
	flashing_keys = false;
}

void soft_kbd_clear(int force)
{
	/*if (force) android_debug("Clearing!");*/

	if (flashing_keys || force) {
		soft_kbd_buffer[0] = 0;
		flashing_keys = true;
		Term_control_keys("${clear}");
	}
}

void soft_kbd_flush()
{
	if (soft_kbd_buffer[0] != 0) {
		/*android_debug("Flushing!");*/
		Term_control_keys(soft_kbd_buffer);
	}
}

void soft_kbd_append(const char *keys)
{
	my_strcat(soft_kbd_buffer, keys, sizeof(soft_kbd_buffer));
}

void strdeldup(char *str)
{
	int i, j, k;

	for (i = 0, j = 0; str[i] != 0; i++) {
		for (k = 0; k < j; k++) {
			if (str[i] == str[k]) {
				break;
			}
		}
		if (k >= j) {
			str[j++] = str[i];
		}
	}
	str[j] = 0;
}

void android_debug(const char *msg)
{
	LOGD("%s", msg);
}
