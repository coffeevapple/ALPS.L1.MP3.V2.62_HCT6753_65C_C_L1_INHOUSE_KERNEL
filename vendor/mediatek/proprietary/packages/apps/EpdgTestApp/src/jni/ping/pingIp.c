#include <sys/socket.h>   
#include <netinet/in.h>   
#include <arpa/inet.h>   
#include <unistd.h>   
#include <stdlib.h>   
#include <string.h>   
#include <stdio.h>
#include <errno.h>
#include <jni.h>
#include "cutils/log.h"

#define LOG_TAG "EpdgTestApp"
#define PORT 1111   

JNIEXPORT jint JNICALL Java_com_mediatek_connectivity_EpdgTestApp_PingTestJni_pingIpv4(
		JNIEnv *env, jobject obj, jstring str) {

	const char *address = (*env)->GetStringUTFChars(env, str, NULL);
	if (address == NULL)
		return -1;
	char ip_addr[128];
	strcpy(ip_addr, address);
	(*env)->ReleaseStringUTFChars(env, str, address);

	int s = -1, len;
	struct timeval tv;
	struct sockaddr_in addr;
	int addr_len = sizeof(struct sockaddr_in);
	char buffer[256];

	int icmp_sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_ICMP);

	if (icmp_sock <= 0) {
		printf("icmp_sock: %s\r\n", strerror(errno));
		ALOGE("[pingIp]icmp_sock: %s\r\n", strerror(errno));
		return -2;
	}
	printf("sock fd:%d", icmp_sock);
	ALOGE("[pingIp]sock fd:%d\n", icmp_sock);

	tv.tv_sec = 30 * 1000;
	tv.tv_usec = 0;
	setsockopt(icmp_sock, SOL_SOCKET, SO_RCVTIMEO, (char*) &tv, sizeof(tv));

	memset((char *) &addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(PORT);

	ALOGE("[pingIp]Ip address:%s\n", ip_addr);

	if (inet_aton(ip_addr, &addr.sin_addr) == 0) {
		printf("error");
		ALOGE("[pingIp]inet_aton : Address Invalid Error");
		return -5;
	}

	bzero(buffer, sizeof(buffer));
	buffer[0] = 0x08;
	buffer[1] = 0x00;
	buffer[2] = 0x00;
	buffer[3] = 0x00;
	printf("%d", addr_len);

	len = sendto(icmp_sock, buffer, sizeof(buffer), 0,
			(struct sockaddr *) &addr, addr_len);
	if (len < 0) {
		printf("send to error: %d:%s\r\n", errno, strerror(errno));
		ALOGE("[pingIp]send to error: %d:%s\r\n", errno, strerror(errno));
		return -3;
	}
	ALOGE("[pingIp]sendto => len: %d\r\n", len);
	len = recvfrom(icmp_sock, buffer, sizeof(buffer), 0,
			(struct sockaddr *) &addr, &addr_len);
	ALOGE("[pingIp]recvfrom => len: %d\r\n", len);
	buffer[len];
	printf("receive: %s\r\n", buffer);
	ALOGE("[pingIp]receive: %s\r\n", buffer);
	if (len < 0) {
		printf("receive to error: %s\r\n", strerror(errno));
		ALOGE("[pingIp]receive to error: %s\r\n", strerror(errno));
		return -4;
	}
	close(icmp_sock);
	return len;
}
