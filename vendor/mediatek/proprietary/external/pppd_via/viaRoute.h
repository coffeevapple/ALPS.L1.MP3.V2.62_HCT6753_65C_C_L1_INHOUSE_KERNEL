#ifndef __VIA_ROUTE_H__
#define __VIA_ROUTE_H__

/* route add default gw 192.168.1.1 dev wlan0 */
int route_main_via(const char* gwAddr, const char* devName);

#endif /* __VIA_ROUTE_H__ */
