#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <linux/route.h>

static inline int set_address(const char *address, struct sockaddr *sa) {
    return inet_aton(address, &((struct sockaddr_in *)sa)->sin_addr);
}


/* route add default gw 192.168.1.1 dev wlan0 */
int route_main_via(const char* gwAddr, const char* devName)
{
    struct rtentry rt = {
        .rt_dst     = {.sa_family = AF_INET},
        .rt_genmask = {.sa_family = AF_INET},
        .rt_gateway = {.sa_family = AF_INET},
    };

    errno = EINVAL;

/* route add default gw 192.168.1.1 dev wlan0 */
    rt.rt_flags = RTF_UP | RTF_GATEWAY;
    rt.rt_dev = devName;
    if (set_address(gwAddr, &rt.rt_gateway)) {
        errno = 0;
    }

apply:
    if (!errno) {
        int s = socket(AF_INET, SOCK_DGRAM, 0);
        if (s != -1 && (ioctl(s, SIOCADDRT, &rt) != -1 || errno == EEXIST)) {
            return 0;
        }
    }
    puts(strerror(errno));
    return errno;
}

