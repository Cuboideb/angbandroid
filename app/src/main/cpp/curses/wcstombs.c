#include <stdlib.h>
#include <wchar.h>

#if defined (ANGDROID_FAANGBAND_PLUGIN) || defined (ANGDROID_TOME_DOC_WORKAROUND) || defined (ANGDROID_NPP_PLUGIN) || defined (ANGDROID_POSCHENG_DOC_WORKAROUND)
	#define restrict __restrict
#endif

size_t wcstombs(char *restrict s, const wchar_t *restrict ws, size_t n)
{
	return wcsrtombs(s, &(const wchar_t *){ws}, n, 0);
}
