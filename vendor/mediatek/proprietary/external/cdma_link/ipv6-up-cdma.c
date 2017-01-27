/* hardware/cdma_link/ip-up-cdma.c
 *
 * Copyright (C) 2009 Viatelecom Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <linux/route.h>

#include <android/log.h>
#include <cutils/properties.h>

int main(int argc, char **argv)
{
    char *dns1 = getenv("DNS1");
    char *dns2 = getenv("DNS2");
    char *iplocal = getenv("LLLOCAL");
    char *ipremote = getenv("LLREMOTE");
    char *ifname = getenv("IFNAME");
    char *renegotiation = "on";
    char tmp[128];

    __android_log_print(ANDROID_LOG_INFO, "ipv6-up-cdma", "suffix Version: %s", VIA_SUFFIX_VERSION);
    //Dont'set defualt dns properties, or we can't restore wlan dns when ppp disconnected
    //property_set("net.dns1", dns1 ? dns1 : "2001:470:20::2");
    //property_set("net.dns2", dns2 ? dns2 : "2001:0c68:0300:0104:0200");
    //the flowing ppp0 should replace with  ifname?
    property_set("net.ppp0.dns1", dns1 ? dns1 : "2001:470:20::2");
    property_set("net.ppp0.dns2", dns2 ? dns2 : "2001:0c68:0300:0104:0200");
    sprintf(tmp, "net.%s.gw", ifname? ifname : "");
    property_set(tmp, ipremote ? ipremote : "");

    property_set("net.cdma.local-ip", iplocal ? iplocal : "");
    property_set("net.cdma.remote-ip", ipremote ? ipremote : "");

    //set ifname of cdma ppp
    property_set("net.cdma.ppp.ifname", ifname ? ifname : "");
    if(NULL != argv[6])
    {
        if(!strncmp(argv[6], renegotiation, 2))
        {
            property_set("net.dns1", dns1 ? dns1 : "");
            property_set("net.dns2", dns2 ? dns2 : "");
            sprintf(tmp,"/system/bin/route add default gw %s dev %s",iplocal,ifname);
            __android_log_print(ANDROID_LOG_INFO, "ip-up-cdma", "set net.dns1 %s net.dns2 %s\n----%s\n"
                , dns1, dns2, tmp);
            system(tmp);
        }
    }
    //While the right ifname set, the default route will be set in ConnectivityService
    /*
    sprintf(tmp,"/bin/route -A inet6 add default gw %s dev %s",iplocal,ifname);
    __android_log_print(ANDROID_LOG_INFO, "ipv6-up-cdma", "----%s\n", tmp);
    system(tmp);
    */

    property_set("net.cdma.linkup", iplocal ? "yes" : "no");

    if(iplocal)
    {
        __android_log_print(ANDROID_LOG_INFO, "ipv6-up-cdma", "Cdma ppp link UP OK, IP=%s", iplocal);
    }
    else
    {
        __android_log_print(ANDROID_LOG_INFO, "ipv6-up-cdma", "Cdma ppp link UP error!");
    }

    return 0;
}
