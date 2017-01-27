#include <cutils/xlog.h>

int is_xlog_enable() {
#ifdef HAVE_XLOG_FEATURE
    return 1;
#else
    return 0;
#endif
}
