package com.mediatek.connectivity.EpdgTestApp;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.LinkAddress;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.NetworkRequest;
import android.widget.LinearLayout;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ToggleButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
//import com.mediatek.connectivity.EpdgTestApp.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.widget.AdapterView.OnItemSelectedListener;
import android.graphics.Color;
import static android.net.ConnectivityManager.TYPE_NONE;
import android.view.LayoutInflater;

public class TestEpdgConnection extends Fragment implements
		View.OnClickListener {
	// private static final String TAG = "TestEpdgConnection";
	private static final String TAG = "EpdgTestApp";
	private static Context mContext;
	private static Toast mToast;
	private static TextView mConnStatus = null;
	private static int mCurrentPosition = -1;
	private static Button mAddPdn = null;
	private static Button mPingPcscf = null;
	private static Button mPingMMS = null;
	private static Button mSend;
	private static LinearLayout mPdnRow;
	private static List<PdnConnection> mPdnConnList = new ArrayList<PdnConnection>();
	private static ListView mPdnListView;
	private static PdnListAdapter adapter;
	private static PingTestJni mPingJni;
	private static int pdnCount = 0;
	private static TextView mOutputScreen;
	private static EditText mCommand;
	private static final int MAX_APN_TYPE = 5;
	private static ConnectivityManager mConnMgr = null;
	private static ProgressDialog mProgressDialog;
	private static Spinner mApnSpinner = null;
	private static String[] mStatus = new String[2];
	private static PingTest mPing;
	private static PdnConnection mCurrentPdn = null;
	private static InetAddress mPcscfAddr = null;
	private static final int EVENT_ON_PRECHECK = 1001;
	private static final int EVENT_ON_AVAILABLE = 1002;
	private static final int EVENT_ON_UNAVAILABLE = 1003;
	private static final int EVENT_ON_LOSING = 1004;
	private static final int EVENT_ON_LOST = 1005;
	public static final int EVENT_PRINT_TOAST = 1006;
	public static final int EVENT_UPDATE_OUTPUT = 1007;
	private static final int BUFFER_SIZE = 4096;
	private static final int STATUS_START_CONNECTION = 1;
	private static final int STATUS_START_DISCONNECTION = -1;
	private static final int STATUS_UNAVAILABLE = -2;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_epdg_test_app,
				container, false);

		mContext = getActivity().getApplicationContext();

		mConnStatus = (TextView) view.findViewById(R.id.conn_status);

		mAddPdn = (Button) view.findViewById(R.id.add_pdn);
		mAddPdn.setOnClickListener(this);

		mPingPcscf = (Button) view.findViewById(R.id.pingPcscf);
		mPingPcscf.setOnClickListener(this);
		mPingPcscf.setEnabled(false);

		mPingMMS = (Button) view.findViewById(R.id.pingMMS);
		mPingMMS.setOnClickListener(this);
		mPingMMS.setEnabled(false);

		mPdnListView = (ListView) view.findViewById(R.id.list);
		mPdnListView.setOnItemClickListener(Listener);
		adapter = new PdnListAdapter(getActivity(), mPdnConnList, mPdnListView);
		mPingJni = new PingTestJni();
		mPdnListView.setAdapter(adapter);
		mCommand = (EditText) view.findViewById(R.id.command);
		mCommand.addTextChangedListener(mTextWatcher);
		mSend = (Button) view.findViewById(R.id.send);
		mSend.setOnClickListener(this);
		mSend.setEnabled(false);
		mOutputScreen = (TextView) view.findViewById(R.id.output);
		mOutputScreen.setMovementMethod(ScrollingMovementMethod.getInstance());
		mConnMgr = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		mPing = new PingTest();
		mProgressDialog = new ProgressDialog(getActivity());
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mProgressDialog.dismiss();
					}
				});
		showEpdgConnection();
		return view;
	}

	private void showEpdgConnection() {
		if (mCurrentPosition != -1) {
			adapter.setTabChanged(true);
			mConnStatus.setText(mStatus[mCurrentPosition]);
			mConnStatus.invalidate();
			adapter.setSelectedIndex(mCurrentPosition);
			if (mCurrentPdn.isConnected) {
				if (mCurrentPdn.APN == "MMS")
					mPingMMS.setEnabled(true);
				if (mCurrentPdn.APN == "IMS")
					mPingPcscf.setEnabled(true);
			}
		}
	}

	private OnItemClickListener Listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			long viewId = view.getId();
			mCurrentPosition = position;
			mConnStatus.setText(mStatus[position]);
			mConnStatus.invalidate();
			adapter.setSelectedIndex(position);
			mCurrentPdn = mPdnConnList.get(position);

			if (mCurrentPdn.APN == "MMS") {
				if (mPingPcscf.isEnabled())
					mPingPcscf.setEnabled(false);
				if (mCurrentPdn.isConnected && !mPingMMS.isEnabled())
					mPingMMS.setEnabled(true);
			} else if (mCurrentPdn.APN == "IMS") {
				if (mPingMMS.isEnabled())
					mPingMMS.setEnabled(false);
				if (mCurrentPdn.isConnected && !mPingPcscf.isEnabled())
					mPingPcscf.setEnabled(true);
			}

		}
	};

	private TextWatcher mTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence charSequence, int i, int i2,
				int i3) {
		}

		@Override
		public void onTextChanged(CharSequence charSequence, int i, int i2,
				int i3) {
		}

		@Override
		public void afterTextChanged(Editable editable) {
			// check Fields For Empty Values
			checkFieldsForEmptyValues();

		}
	};

	void checkFieldsForEmptyValues() {

		String s1 = mCommand.getText().toString();

		if (s1.equals("")) {
			mSend.setEnabled(false);
		} else
			mSend.setEnabled(true);
	}

	@Override
	public void onClick(View v) {
		int buttonId = v.getId();
		boolean isUrl = false;
		switch (buttonId) {
		case R.id.add_pdn:
			Log.i(TAG, "[TestEpdgConnection]Add Pdn connection");
			if (pdnCount < 2) {
				PdnConnection p = new PdnConnection(false, 0);
				mPdnConnList.add(p);
				adapter.notifyDataSetChanged();
				pdnCount++;
			} else {
				Toast.makeText(mContext, "Only Two connection are allowed",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.send:
			String command = mCommand.getText().toString();
			StringTokenizer tokens = new StringTokenizer(command, " ");

			if (tokens.nextToken().equalsIgnoreCase("ping")) {
				if (tokens.countTokens() != 3) {
					showToast("Enter correct ping command with 4 parameters. command contains :"
							+ tokens.countTokens());
					return;
				}
				String first = tokens.nextToken(); // this will contain
													// "destination"
				String second = tokens.nextToken(); // this will contain
													// "counter"
				String third = tokens.nextToken(); // this will contain
													// "packet size"
				pingParser(first, second, third);
			} else {
				try {
					URI uri = new URI(command);
					try {
						isUrl = uri.getScheme().equalsIgnoreCase("http");
					} catch (Exception e) {
						showToast("Exception" + e);
						isUrl = false;
					}
					if (isUrl) {
						if (mCurrentPdn.isConnected) {
							sendHttpRequest(mCurrentPdn);
							Log.i(TAG, "[TestEpdgConnection]" + command
									+ " command sent");
							Toast.makeText(mContext, command + " command sent",
									Toast.LENGTH_SHORT).show();
							mCommand.clearComposingText();
							mCommand.setHint(R.string.ping_command);
							mCommand.invalidate();
						} else {
							Toast.makeText(mContext, "No Connection",
									Toast.LENGTH_SHORT).show();
						}
					} else {
						InetAddress inet = InetAddress.getByName(command);
						System.out
								.println("Sending Ping Request to " + command);
						showToast("Sending Ping Request to " + command);
						pingIpAddr(inet);

					}

				} catch (Exception e) {
					showToast("Unknown Host" + e);
				}
			}

			break;
		case R.id.pingPcscf:
			if (mCurrentPdn != null && mPcscfAddr != null
					&& mCurrentPdn.APN == "IMS") {
				Log.d(TAG, "[TestEpdgConnection]setProcessDefaultNetwork --> "
						+ mCurrentPdn.APN + ":" + mCurrentPdn.currentNetwork);
				// mConnMgr.setProcessDefaultNetwork(mCurrentPdn.currentNetwork);
				showToast("Pinging pcscf Server");
				pingPcscfServer(mPcscfAddr);
			}
			break;
		case R.id.pingMMS:
			if (mCurrentPdn != null && mCurrentPdn.APN == "MMS") {
				Log.d(TAG, "[TestEpdgConnection]setProcessDefaultNetwork --> "
						+ mCurrentPdn.APN + ":" + mCurrentPdn.currentNetwork);
				// mConnMgr.setProcessDefaultNetwork(mCurrentPdn.currentNetwork);
				showToast("Pinging MMS Server");
				final String mmsAddress = "mms.msg.eng.t-mobile.com";
				pingMmsServer(mmsAddress);
			}
			break;
		default:
			Log.e(TAG, "[TestEpdgConnection]Error button");
			break;
		}
	}

	private static void pingParser(String first, String second, String third) {
		if (mCurrentPdn != null) {
			final int counter = Integer.parseInt(second);
			final int size = Integer.parseInt(third);
			final String addr = first;
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						InetAddress dst_addr;
						if (mPcscfAddr != null
								&& addr.equalsIgnoreCase("pcscf")) {
							dst_addr = mPcscfAddr;
						} else if (addr.equalsIgnoreCase("mmsc")) {
							mConnMgr.setProcessDefaultNetworkForHostResolution(mCurrentPdn.currentNetwork);
							dst_addr = InetAddress
									.getByName("mms.msg.eng.t-mobile.com");
						} else {
							mConnMgr.setProcessDefaultNetwork(mCurrentPdn.currentNetwork);
							dst_addr = InetAddress.getByName(addr);
						}
						Log.i(TAG,
								"[TestEpdgConnection]pingParser ping destination:"
										+ dst_addr);
						if (mCurrentPdn.isIPv4) {
							pingIpv4address(dst_addr, counter);
						} else {
							mPing.testCustomInetAddrPing(dst_addr, counter,
									size, mCurrentPdn.isIPv4,
									mCurrentPdn.currentNetwork);
						}
					} catch (Exception e) {
						String str = "ping " + addr + " Server Exception:" + e;
						Log.i(TAG, "[TestEpdgConnection]ping " + addr
								+ " Server Exception:" + e);
						Message msg = mHandler.obtainMessage(EVENT_PRINT_TOAST,
								(Object) str);
						mHandler.sendMessage(msg);
					}
				}
			}).start();
		}
	}

	private static void pingIpv4address(InetAddress dst_addr, int counter) {
		int resp = 0;
		StringBuffer StrBuff = new StringBuffer();
		for (int i = 0; i < counter; i++) {
			mConnMgr.setProcessDefaultNetwork(mCurrentPdn.currentNetwork);
			String address = dst_addr.getHostAddress();
			resp = mPingJni.pingIpv4(address);
			Log.i(TAG, "[TestEpdgConnection]Ping IPv4 :" + resp);
			String str = "Response received: " + resp + "\n";
			StrBuff.append(" C:" + (i + 1) + " /" + counter + ":" + str);
			Message msg = mHandler.obtainMessage(EVENT_UPDATE_OUTPUT,
					(Object) StrBuff);
			mHandler.sendMessage(msg);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				Log.i(TAG, "[TestEpdgConnection]Exception " + e);
			}

		}
	}

	private static void updateConnectStatus(MsgObject obj) {

		StringBuffer output = null;
		String str = "";
		boolean shouldUpdate = true;
		PdnConnection pc;
		int pos = adapter.getSelectedIndex();
		if (mPdnConnList.get(pos).APN != obj.APN) {
			pos = 1 - pos;
			shouldUpdate = false;
		}
		pc = mPdnConnList.get(pos);

		Network network = obj.network;
		if (network != null) {
			NetworkInfo nwInfo = mConnMgr.getNetworkInfo(network);
			if (nwInfo != null) {
				if (nwInfo.getState() == NetworkInfo.State.CONNECTED
						|| nwInfo.getState() == NetworkInfo.State.CONNECTING) {
					Toast.makeText(mContext, getNetworkState(nwInfo),
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "[TestEpdgConnection]Network state: "
							+ getNetworkState(nwInfo));
					LinkProperties link = mConnMgr.getLinkProperties(network);
					if (link != null) {
						List<LinkAddress> addresses = link.getLinkAddresses();
						pc.isIPv4 = link.hasIPv4Address();
						NetworkCapabilities cap = mConnMgr
								.getNetworkCapabilities(network);
						List<InetAddress> PcscfAddr = link.getPcscfServers();
						Log.i(TAG, "[TestEpdgConnection]updateConnectStatus:"
								+ nwInfo + "\n" + PcscfAddr);
						str = "Status:" + getNetworkState(nwInfo) + "; APN:"
								+ pc.APN + "\r\n" + "address:" + addresses
								+ "type:" + nwInfo.getType()
								+ ", interfaceName:" + link.getInterfaceName()
								+ ", PcscfAddresses:" + PcscfAddr;

						if (!PcscfAddr.isEmpty() && PcscfAddr.get(0) != null) {
							// if(!addresses.isEmpty()){
							Log.i(TAG,
									"[TestEpdgConnection]PCSC-F is Available"
											+ PcscfAddr);
							if (pc.APN == "IMS") {
								Log.i(TAG,
										"[TestEpdgConnection]Enabling Ping PCSCF button");
								mPcscfAddr = PcscfAddr.get(0);
								mPingPcscf.setEnabled(true);
							} else if (pc.APN == "MMS") {
								Log.i(TAG,
										"[TestEpdgConnection]Enabling Ping MMSC button");
								mPingMMS.setEnabled(true);
							}
						}
					}
				}
			} else {
				if (pc.APN == "IMS") {
					mPcscfAddr = null;
					mPingPcscf.setEnabled(false);
					str = "Connection with IMS lost";
					mOutputScreen.setText("");
					mOutputScreen.postInvalidate();
					showToast(str);
				}
				if (pc.APN == "MMS") {
					mPingMMS.setEnabled(false);
					str = "Connection with MMS lost";
					showToast(str);
					mOutputScreen.setText("");
					mOutputScreen.postInvalidate();
				}
			}
		}
		output = new StringBuffer(str);
		mStatus[pos] = str;
		if (output != null) {
			mConnStatus.setText(output.toString());
		} else {
			if (!pc.isConnected)
				mConnStatus.setText("Not connected");
		}
		if (shouldUpdate) {
			mConnStatus.invalidate();
		}
		adapter.notifyDataSetChanged();
	}

	private static void pingMmsServer(String str) {
		final String MmsServerName = str;
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					mConnMgr.setProcessDefaultNetworkForHostResolution(mCurrentPdn.currentNetwork);
					InetAddress MmsServerAddr = InetAddress
							.getByName(MmsServerName);
					if (mCurrentPdn.isIPv4) {
						pingIpv4address(MmsServerAddr, 60);
					} else {
						mPing.testInetAddrPing(MmsServerAddr,
								mCurrentPdn.isIPv4, mCurrentPdn.currentNetwork);
					}
				} catch (Exception e) {
					Log.i(TAG, "[TestEpdgConnection]pingMmsServer Exception:"
							+ e);
				}
			}
		}).start();
	}

	private static void pingPcscfServer(InetAddress addr) {
		final InetAddress Addr = addr;
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (mCurrentPdn.isIPv4) {
						pingIpv4address(Addr, 62);
					} else {
						mPing.testInetAddrPing(Addr, mCurrentPdn.isIPv4,
								mCurrentPdn.currentNetwork);
					}
				} catch (Exception e) {
					Log.i(TAG, "[TestEpdgConnection]pingPcscfServer Exception:"
							+ e);
				}
			}
		}).start();
	}

	private static void pingIpAddr(InetAddress addr) {
		mConnMgr.setProcessDefaultNetwork(mCurrentPdn.currentNetwork);

		final InetAddress Addr = addr;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					try {
						String str = "";
						if (Addr.isReachable(5000))
							str = "Host is Reachable";
						else
							str = "Host is not Reachable";
						Message msg = mHandler.obtainMessage(EVENT_PRINT_TOAST,
								(Object) str);
						mHandler.sendMessage(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private static String getNetworkState(NetworkInfo ni) {
		switch (ni.getState()) {
		case CONNECTED:
			return "CONNECTED";

		case CONNECTING:
			return "CONNECTING";

		case DISCONNECTING:
			return "DISCONNECTING";

		case DISCONNECTED:
			return "DISCONNECTED";

		default:
			return "UNKNOWN";

		}
	}

	public class MsgObject {
		Network network;
		String APN;
	}

	public class PdnConnection {

		String APN;
		boolean isConnected;
		boolean isIPv4;
		int selectedApnPos;
		NetworkCapabilities networkCapabilities;
		NetworkRequest networkRequest;
		Network currentNetwork;
		NetworkCallback networkCallback = new NetworkCallback() {

			@Override
			public void onPreCheck(Network network) {
				String info = "onPreCheck(" + APN + ":" + network + ")";
				Log.d(TAG, "[TestEpdgConnection]" + info);
				Message msg = mHandler.obtainMessage(EVENT_ON_PRECHECK,
						(Object) network);
				mHandler.sendMessage(msg);
			}

			@Override
			public void onLosing(Network network, int maxMsToLive) {
				String info = "onLosing(" + APN + ":" + network + ")";
				Log.d(TAG, "[TestEpdgConnection]" + info);
				Message msg = mHandler.obtainMessage(EVENT_ON_LOSING,
						(Object) network);
				mHandler.sendMessage(msg);
			}

			@Override
			public void onUnavailable() {
				MsgObject obj = new MsgObject();
				obj.APN = APN;
				obj.network = null;
				String info = "onUnavailable(" + APN + ":" + networkRequest
						+ ")";
				Log.d(TAG, "[TestEpdgConnection]" + info);
				isConnected = false;
				if (networkCallback != null) {
					Log.d(TAG,
							"[TestEpdgConnection]"
									+ "Network initiated => Release network Request for : "
									+ APN);
					mConnMgr.unregisterNetworkCallback(networkCallback);
				}
				Message msg = mHandler.obtainMessage(EVENT_ON_UNAVAILABLE,
						(Object) obj);
				mHandler.sendMessage(msg);
			}

			@Override
			public void onAvailable(Network network) {
				currentNetwork = network;
				isConnected = true;
				String info = "onAvailable(" + APN + ":" + network + ")";
				Log.d(TAG, "[TestEpdgConnection]" + info);
				MsgObject obj = new MsgObject();
				obj.APN = APN;
				obj.network = network;
				Message msg = mHandler.obtainMessage(EVENT_ON_AVAILABLE,
						(Object) obj);
				mHandler.sendMessage(msg);
			}

			@Override
			public void onLost(Network network) {
				isConnected = false;
				currentNetwork = null;
				String info = "onLost(" + APN + ":" + network + ")";
				Log.d(TAG, "[TestEpdgConnection]" + info);
				MsgObject obj = new MsgObject();
				obj.APN = APN;
				obj.network = network;

				if (networkCallback != null) {
					Log.d(TAG,
							"[TestEpdgConnection]"
									+ "Network initiated => Release network Request for : "
									+ APN);
					mConnMgr.unregisterNetworkCallback(networkCallback);
				}
				Message msg = mHandler.obtainMessage(EVENT_ON_LOST,
						(Object) obj);
				mHandler.sendMessage(msg);
			}
		};

		public PdnConnection(boolean connect, int apnPos) {
			this.isConnected = connect;
			this.selectedApnPos = apnPos;
			isIPv4 = false;
		}

		public void setApn(String apn) {
			APN = apn;
		}

		public void setApnPos(int pos) {
			selectedApnPos = pos;
		}

		public int getApnPos() {
			return selectedApnPos;
		}

		public String getApn() {
			return APN;
		}

		public boolean getConnectStatus() {
			return isConnected;
		}

		public void setConnectStatus(String apn, int status) {

			switch (status) {

			case STATUS_START_DISCONNECTION:
				isConnected = false;
				mConnStatus.setText(apn + " DisConnected");
				mStatus[adapter.getSelectedIndex()] = apn + " DisConnected";
				if (apn == "IMS")
					mPingPcscf.setEnabled(false);
				if (apn == "MMS")
					mPingMMS.setEnabled(false);
				break;
			case STATUS_START_CONNECTION:
				isConnected = true;
				mConnStatus.setText("Starting Connection for " + apn);
				mStatus[adapter.getSelectedIndex()] = "Starting Connection for "
						+ apn;
				break;
			case STATUS_UNAVAILABLE:
				isConnected = false;
				mConnStatus.setText("RAT UnAvailable for " + apn);
				mStatus[adapter.getSelectedIndex()] = "RAT UnAvailable for  "
						+ apn;
				break;
			}
			mConnStatus.invalidate();
		}

	} // "PdnConnection" end of class

	public static void updateStatus(String status) {
		mConnStatus.setText(status);
		mConnStatus.invalidate();
	}

	public static Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_ON_PRECHECK:
				updateStatus("Prechecking");
				break;
			case EVENT_ON_AVAILABLE:
				MsgObject obj = (MsgObject) msg.obj;
				updateStatus("Available");
				updateConnectStatus(obj);

				break;
			case EVENT_ON_UNAVAILABLE:
				// updateStatus("UnAvailable");
				obj = (MsgObject) msg.obj;
				// setConnectStatus(obj.APN, STATUS_UNAVAILABLE);
				mConnStatus.setText("RAT UnAvailable for " + obj.APN);
				mStatus[adapter.getSelectedIndex()] = "RAT UnAvailable for  "
						+ obj.APN;
				break;
			case EVENT_ON_LOSING:
				updateStatus("Losing");
				break;
			case EVENT_ON_LOST:
				obj = (MsgObject) msg.obj;
				updateConnectStatus(obj);
				break;
			case EVENT_PRINT_TOAST:
				String str = (String) msg.obj;
				showToast(str);
				break;
			case EVENT_UPDATE_OUTPUT:
				StringBuffer strBuff = new StringBuffer();
				strBuff = (StringBuffer) msg.obj;
				mOutputScreen.setText(strBuff);
				mOutputScreen.postInvalidate();
				break;
			default:
				break;
			}
		}
	};

	private static void sendHttpRequest(PdnConnection pconn) {
		mConnMgr.setProcessDefaultNetwork(pconn.currentNetwork);

		try {
			sendGet();
			downloadFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(mContext, "IOException caught in getResponse code",
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "[TestEpdgConnection]" + "Exception caught :" + e);
			Toast.makeText(mContext,
					"Exception caught in getResponse code" + e,
					Toast.LENGTH_LONG).show();
		}

	}

	private static void sendGet() throws Exception {

		Toast.makeText(mContext, "Into sendGet()", Toast.LENGTH_LONG).show();
		// add request header

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					try {
						String url = mCommand.getText().toString();
						URL obj = new URL(url);
						HttpURLConnection con = (HttpURLConnection) obj
								.openConnection();
						// set up some things on the connection
						con.setRequestMethod("GET");
						con.setUseCaches(false);
						// and connect!
						con.connect();
						readStream(con.getInputStream());
					} catch (Exception e) {
						Log.e(TAG, "[TestEpdgConnection]"
								+ "Exception caught :" + e);
						// Toast.makeText(mContext, "Exception: "+e,
						// Toast.LENGTH_SHORT).show();
						e.printStackTrace();
					}

				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "[TestEpdgConnection]" + "Exception caught :"
							+ e);
				}
			}
		});

		thread.start();

	}

	private static void readStream(InputStream in) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			StringBuffer strBuff = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				strBuff.append(line);
			}
			Message msg = mHandler.obtainMessage(EVENT_UPDATE_OUTPUT,
					(Object) strBuff);
			mHandler.sendMessage(msg);

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void showToast(String s) {
		Toast toast = Toast.makeText(mContext, s, Toast.LENGTH_LONG);
		toast.show();
	}

	/****************** Start Download Task ********************************/

	private static void downloadFile() {
		final DownloadTask downloadTask = new DownloadTask(mContext);
		String url = mCommand.getText().toString();
		downloadTask.execute(url);

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						downloadTask.cancel(true);
						showToast("Download cancelled");
					}
				});
	}

	private static class DownloadTask extends
			AsyncTask<String, Integer, String> {

		private Context context;
		private PowerManager.WakeLock mWakeLock;

		public DownloadTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(String... sUrl) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection httpConn = null;
			boolean isBig = false;
			try {
				URL url = new URL(sUrl[0]);
				httpConn = (HttpURLConnection) url.openConnection();
				httpConn.connect();

				// expect HTTP 200 OK, so we don't mistakenly save error report
				// instead of the file
				if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return "Server returned HTTP " + httpConn.getResponseCode()
							+ " " + httpConn.getResponseMessage();
				}

				// this will be useful to display download percentage
				// might be -1: server did not report the length
				float fileLength = httpConn.getContentLength();
				input = httpConn.getInputStream();
				output = mContext.openFileOutput("EpdgDownloadFile",
						Context.MODE_PRIVATE);
				// output = new FileOutputStream("/sdcard/file_name.extension");

				byte data[] = new byte[BUFFER_SIZE];
				float total = 0;
				int count;
				float temp = 0;

				while ((count = input.read(data)) != -1) {
					// allow canceling with back button
					if (isCancelled()) {
						input.close();
						return null;
					}
					total += count;
					temp = total / fileLength;
					// publishing the progress....
					if (fileLength > 0) // only if total length is known
						publishProgress((int) (temp * 100));
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				return e.toString();
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (httpConn != null)
					httpConn.disconnect();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLock.acquire();
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// if we get here, length is known, now set indeterminate to false
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			mWakeLock.release();
			mProgressDialog.dismiss();
			if (result != null)
				Toast.makeText(context, "Download error: " + result,
						Toast.LENGTH_LONG).show();
			else
				Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT)
						.show();
		}
	}
	/******************* end download task ************************/
}
