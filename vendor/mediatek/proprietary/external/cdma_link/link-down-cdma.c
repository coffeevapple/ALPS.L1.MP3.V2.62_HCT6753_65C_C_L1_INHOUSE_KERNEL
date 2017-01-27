/* hardware/cdma_link/link-down-cdma.c
 * Author: Qiang Fu
 *
 * Copyright (C) 2011 Viatelecom Inc.
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
#define LOG_TAG "LINK-DOWN-CDMA"
#include <utils/Log.h>
int main(int argc, char **argv)
{
    __android_log_print(ANDROID_LOG_INFO, "link-down-cdma", "suffix Version: %s", VIA_SUFFIX_VERSION);
    property_set("net.cdma.linkup", "no");
    //system("echo DATA > /sys/class/power_supply/twl4030_bci_bk_battery/device/status_off");

    return 0;
}

