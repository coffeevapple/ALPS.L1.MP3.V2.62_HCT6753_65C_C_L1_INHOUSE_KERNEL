/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.mediatek.connectivity.EpdgTestApp;

import android.test.AndroidTestCase;
import android.util.Log;

import android.system.ErrnoException;
import android.system.Os;
import android.system.StructTimeval;
import android.util.Log;
import static android.system.OsConstants.*;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import java.net.Socket;
import android.os.Handler;
import android.os.Message;
import android.net.Network;
import android.net.NetworkUtils;
import com.mediatek.connectivity.EpdgTestApp.TestEpdgConnection;

/**
 * Checks that the device has kernel support for the IPv6 ping socket. This
 * allows ping6 to work without root privileges. The necessary kernel code is in
 * Linux 3.11 or above, or the <code>common/android-3.x</code> kernel trees. If
 * you are not running one of these kernels, the functionality can be obtained
 * by cherry-picking the following patches from David Miller's
 * <code>net-next</code> tree:
 * <ul>
 * <li>6d0bfe2 net: ipv6: Add IPv6 support to the ping socket.
 * <li>c26d6b4 ping: always initialize ->sin6_scope_id and ->sin6_flowinfo
 * <li>fbfe80c net: ipv6: fix wrong ping_v6_sendmsg return value
 * <li>a1bdc45 net: ipv6: add missing lock in ping_v6_sendmsg
 * <li>cf970c0 ping: prevent NULL pointer dereference on write to msg_name
 * </ul>
 * or the equivalent backports to the <code>common/android-3.x</code> trees.
 */
public class PingTest extends AndroidTestCase {
	/** Maximum size of the packets we're using to test. */
	private static final int MAX_SIZE = 8192;
	private static final int SIZE = 128;
	private static boolean connectionActive = true;
	/** Number of packets to test. */
	private static final int NUM_PACKETS = 60;
	private static StringBuffer mStrBuff;
	private static NetworkUtils mNetworkUtil = new NetworkUtils();
	/**
	 * The beginning of an ICMPv6 echo request: type, code, and uninitialized
	 * checksum.
	 */
	private static final byte[] PING_HEADER_IPV6 = new byte[] { (byte) 0x80,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };
	/**
	 * The beginning of an ICMPv4 echo request: type, code, and uninitialized
	 * checksum.
	 */
	private static final byte[] PING_HEADER_IPV4 = new byte[] { (byte) 0x08,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };
	// private static final String TAG = "PingTest";
	private static final String TAG = "EpdgTestApp";

	/**
	 * Returns a byte array containing an ICMPv6/ICMPv4 echo request with the
	 * specified payload length.
	 */
	private byte[] pingPacket(int payloadLength, boolean isIPv4) {
		byte[] packet = new byte[payloadLength + 8];
		new Random().nextBytes(packet);
		if (!isIPv4)
			System.arraycopy(PING_HEADER_IPV6, 0, packet, 0,
					PING_HEADER_IPV6.length);
		else
			System.arraycopy(PING_HEADER_IPV4, 0, packet, 0,
					PING_HEADER_IPV4.length);

		return packet;
	}

	/**
	 * Checks that the first length bytes of two byte arrays are equal.
	 */
	private void assertArrayBytesEqual(byte[] expected, byte[] actual,
			int length) {
		for (int i = 0; i < length; i++) {
			assertEquals("Arrays differ at index " + i + ":", expected[i],
					actual[i]);
		}
	}

	/**
	 * Creates an IPv6 ping socket and sets a receive timeout of 100ms.
	 */
	private FileDescriptor createPingSocket(boolean isIPv4)
			throws ErrnoException {
		FileDescriptor s;
		if (!isIPv4)
			s = Os.socket(AF_INET6, SOCK_DGRAM, IPPROTO_ICMPV6);
		else
			s = Os.socket(AF_INET, SOCK_DGRAM, IPPROTO_ICMP);
		Os.setsockoptTimeval(s, SOL_SOCKET, SO_RCVTIMEO,
				StructTimeval.fromMillis(1000));
		return s;
	}

	/**
	 * Sends a ping packet to a random port on the specified address on the
	 * specified socket.
	 */
	private void sendPing(FileDescriptor s, InetAddress address, byte[] packet)
			throws ErrnoException, IOException {
		// Pick a random port. Choose a range that gives a reasonable chance of
		// picking a low port.
		int port = (int) (Math.random() * 2048);
		int ret = -1;
		// Send the packet.
		try {
			ret = Os.sendto(s, ByteBuffer.wrap(packet), 0, address, port);
			Log.i(TAG, "[PingTest]sending Ping " + s + " " + address + " "
					+ packet);
			assertEquals(packet.length, ret);
		} catch (ErrnoException errnoException) {
			Log.i(TAG, " [PingTest]Exception : " + errnoException);
			String str = " Exception : " + errnoException;
			Message msg = TestEpdgConnection.mHandler.obtainMessage(
					TestEpdgConnection.EVENT_PRINT_TOAST, (Object) str);
			TestEpdgConnection.mHandler.sendMessage(msg);
		}

	}

	/**
	 * Checks that a socket has received a response appropriate to the specified
	 * packet.
	 */
	private void checkResponse(FileDescriptor s, InetAddress dest, byte[] sent,
			boolean useRecvfrom) throws ErrnoException, IOException {
		ByteBuffer responseBuffer = ByteBuffer.allocate(MAX_SIZE);
		int bytesRead = 0;
		String str = "";
		Log.i(TAG, "[PingTest]checkResponse for destination " + dest);
		// Receive the response.
		if (useRecvfrom) {
			try {
				InetSocketAddress from = new InetSocketAddress();
				bytesRead = Os.recvfrom(s, responseBuffer, 0, from);

				// Check the source address and scope ID.
				assertTrue(from.getAddress() instanceof Inet6Address);
				Inet6Address fromAddress = (Inet6Address) from.getAddress();
				assertEquals(0, fromAddress.getScopeId());
				assertNull(fromAddress.getScopedInterface());
				assertEquals(dest.getHostAddress(),
						fromAddress.getHostAddress());

				str = bytesRead + "\tbytes from\t" + dest.getHostAddress()
						+ "\n";

				Log.i(TAG, "[PingTest]Response Received: " + "Bytes read:"
						+ bytesRead + "\n" + from.getAddress() + "\n"
						+ fromAddress.getScopedInterface());

			} catch (Exception e) {
				connectionActive = false;
				// str = "Readfailed Exception"+e+"\n";
				Log.i(TAG, "[PingTest]" + useRecvfrom + " Exception" + e);
			}

		} else {
			try {
				bytesRead = Os.read(s, responseBuffer);
			} catch (Exception e) {
				str = "Readfailed Exception" + e + "\n";
				Log.i(TAG, "[PingTest]" + useRecvfrom + " Exception" + e);
			}
		}

		mStrBuff.append(" " + str);
		Message msg = TestEpdgConnection.mHandler.obtainMessage(
				TestEpdgConnection.EVENT_UPDATE_OUTPUT, (Object) mStrBuff);
		TestEpdgConnection.mHandler.sendMessage(msg);
		// Check the packet length.
		if (false) {
			assertEquals(sent.length, bytesRead);
			Log.i(TAG, "[PingTest]Packet Length " + bytesRead);
			// Check the response is an echo reply.
			byte[] response = new byte[bytesRead];
			responseBuffer.get(response, 0, bytesRead);
			assertEquals((byte) 0x81, response[0]);

			// Find out what ICMP ID was used in the packet that was sent.
			int id = ((InetSocketAddress) Os.getsockname(s)).getPort();
			sent[4] = (byte) (id / 256);
			sent[5] = (byte) (id % 256);

			// Ensure the response is the same as the packet, except for the
			// type (which is 0x81)
			// and the ID and checksum, which are set by the kernel.
			response[0] = (byte) 0x80; // Type.
			response[2] = response[3] = (byte) 0x00; // Checksum.
			assertArrayBytesEqual(response, sent, bytesRead);
		}
	}

	/**
	 * Sends NUM_PACKETS random ping packets to ::1 and checks the replies.
	 */
	/*
	 * public void testLoopbackPing() throws ErrnoException, IOException { //
	 * Generate a random ping packet and send it to localhost. InetAddress
	 * ipv6Loopback = InetAddress.getByName(null); assertEquals("localhost/::1",
	 * ipv6Loopback.toString());
	 * 
	 * for (int i = 0; i < NUM_PACKETS; i++) { byte[] packet = pingPacket((int)
	 * (Math.random() * MAX_SIZE)); FileDescriptor s = createPingSocket(); //
	 * Use both recvfrom and read(). sendPing(s, ipv6Loopback, packet);
	 * checkResponse(s, ipv6Loopback, packet, true); sendPing(s, ipv6Loopback,
	 * packet); checkResponse(s, ipv6Loopback, packet, false); // Check closing
	 * the socket doesn't raise an exception. Os.close(s); } }
	 */

	public void testInetAddrPing(InetAddress Addr, boolean isIPv4,
			Network netobj) {

		InetAddress addr = Addr;
		mStrBuff = new StringBuffer();
		connectionActive = true;
		if (addr != null) {
			for (int i = 0; i < NUM_PACKETS; i++) {

				if (!connectionActive) {
					break;
				}
				byte[] packet = pingPacket((int) (Math.random() * SIZE), isIPv4);
				try {
					FileDescriptor s = createPingSocket(isIPv4);
					Method getInt = FileDescriptor.class
							.getDeclaredMethod("getInt$");
					int socketfd = (Integer) getInt.invoke(s);
					if (netobj != null)
						mNetworkUtil
								.bindSocketToNetwork(socketfd, netobj.netId);
					Thread.sleep(1000);
					mStrBuff.append(" A:" + (i + 1) + " /" + NUM_PACKETS + ":");
					TestEpdgConnection.mHandler
							.sendMessage(TestEpdgConnection.mHandler
									.obtainMessage(
											TestEpdgConnection.EVENT_UPDATE_OUTPUT,
											(Object) mStrBuff));

					sendPing(s, addr, packet);
					// Thread.sleep(1000);
					checkResponse(s, addr, packet, true);
					// sendPing(s, addr, packet);
					// checkResponse(s, addr, packet, false);
					// Check closing the socket doesn't raise an exception.
					Os.close(s);
				} catch (Exception e) {
					Log.i(TAG, "[PingTest]Exception " + e);
					e.printStackTrace();
				}
			}
		}
	}

	public void testCustomInetAddrPing(InetAddress Addr, int counter, int size,
			boolean isIPv4, Network netobj) {

		InetAddress dst_addr = Addr;
		mStrBuff = new StringBuffer();
		if (dst_addr != null) {
			for (int i = 0; i < counter; i++) {
				byte[] packet = pingPacket((int) size, isIPv4);
				try {
					FileDescriptor s = createPingSocket(isIPv4);
					// Use both recvfrom and read().
					Method getInt = FileDescriptor.class
							.getDeclaredMethod("getInt$");
					int socketfd = (Integer) getInt.invoke(s);
					if (netobj != null)
						mNetworkUtil
								.bindSocketToNetwork(socketfd, netobj.netId);
					mStrBuff.append(" B:" + (i + 1) + " /" + NUM_PACKETS + ":");
					TestEpdgConnection.mHandler
							.sendMessage(TestEpdgConnection.mHandler
									.obtainMessage(
											TestEpdgConnection.EVENT_UPDATE_OUTPUT,
											(Object) mStrBuff));
					Thread.sleep(1000);
					sendPing(s, dst_addr, packet);
					// Thread.sleep(1000);
					checkResponse(s, dst_addr, packet, true);
					Os.close(s);
				} catch (Exception e) {
					Log.i(TAG, "[PingTest]Exception " + e);
					e.printStackTrace();
				}
			}
		}
	}

}
