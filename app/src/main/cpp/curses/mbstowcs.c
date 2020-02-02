#include <stdlib.h>
#include <wchar.h>

#if defined (ANGDROID_FAANGBAND_PLUGIN)
	#define restrict __restrict
#endif

size_t mbstowcs(wchar_t *restrict ws, const char *restrict s, size_t wn)
{
	return mbsrtowcs(ws, (void*)&s, wn, 0);
}
