package com.mediatek.connectivity.EpdgTestApp;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.ToggleButton;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.connectivity.EpdgTestApp.TestEpdgConnection.PdnConnection;
import com.mediatek.internal.telephony.DefaultBearerConfig;
import com.mediatek.internal.telephony.QosStatus;
import com.mediatek.internal.telephony.ITelephonyEx;

public class PdnListAdapter extends BaseAdapter {

	private static List<PdnConnection> mPdnConnsList;
	private Activity mActivity;
	private static LayoutInflater inflater;
	private static PdnConnection mPdnConn;
	private static Spinner[] mSpinner;
	private static RadioButton[] mRadioBtn;
	private static ListView mListView;
	private static View mOldView = null;
	private static final String[] APN_LIST = new String[] { "MMS", "SUPL",
			"IMS", "XCAP", "RCS" };
	// private String TAG = "PdnListAdapter";
	private String TAG = "EpdgTestApp";
	private Context mContext;
	private static int TIME_OUT = 10 * 1000; // 10 sec timeout
	private static final int MAX_CONNECTION_TIME = 3 * 60 * 1000;
	private static final int STATUS_START_CONNECTION = 1;
	private static final int STATUS_START_DISCONNECTION = -1;
	private static int selectedItemIndex;
	private static boolean isTabChanged = false;

	private static final int[] APN_CAP_LIST = new int[] {
			NetworkCapabilities.NET_CAPABILITY_MMS,
			NetworkCapabilities.NET_CAPABILITY_SUPL,
			NetworkCapabilities.NET_CAPABILITY_IMS,
			NetworkCapabilities.NET_CAPABILITY_XCAP,
			NetworkCapabilities.NET_CAPABILITY_RCS };

	private static ConnectivityManager mConnMgr = null;

	public PdnListAdapter(Activity activity, List<PdnConnection> pdnConns,
			ListView lv) {
		mPdnConnsList = pdnConns;
		mActivity = activity;
		mListView = lv;// (ListView)activity.findViewById(R.id.list);
		mSpinner = new Spinner[2];
		mRadioBtn = new RadioButton[2];
		mConnMgr = (ConnectivityManager) activity
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		selectedItemIndex = -1;
	}

	@Override
	public int getCount() {
		return mPdnConnsList.size();
	}

	@Override
	public Object getItem(int location) {
		return mPdnConnsList.get(location);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void setSelectedIndex(int index) {
		selectedItemIndex = index;
		notifyDataSetChanged();
	}

	public int getSelectedIndex() {
		return selectedItemIndex;
	}

	public void setTabChanged(boolean val) {
		isTabChanged = val;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		PdnConnection pdnConn;
		Spinner apnSpinner;
		RadioButton selectBtn;
		ToggleButton connectBtn;

		if (inflater == null)
			inflater = (LayoutInflater) mActivity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (row == null) {
			row = inflater.inflate(R.layout.list_row, null);
		}
		if (selectedItemIndex == -1)
			selectedItemIndex = position;

		pdnConn = mPdnConnsList.get(position);
		mPdnConn = pdnConn;
		row.setTag(mListView);
		// getting APN connection data for the row
		row.setTag(position);

		apnSpinner = (Spinner) row.findViewById(R.id.apnTypeSpinnner);
		connectBtn = (ToggleButton) row.findViewById(R.id.connect);
		selectBtn = (RadioButton) row.findViewById(R.id.selectPdnBtn);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
				android.R.layout.simple_spinner_item, APN_LIST);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		apnSpinner.setAdapter(adapter);
		apnSpinner.setTag(pdnConn);
		apnSpinner.setSelection(pdnConn.getApnPos());
		mSpinner[position] = apnSpinner;
		apnSpinner
				.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> spinner,
							View arg1, int position, long arg3) {
						// TODO Auto-generated method stub
						// mListView.performItemClick((View)spinner, position,
						// 0);
						PdnConnection pdnConn = (PdnConnection) spinner
								.getTag();
						pdnConn.setApnPos(position);
						pdnConn.setApn(spinner.getSelectedItem().toString());

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

		mRadioBtn[position] = selectBtn;
		if (isTabChanged && mRadioBtn[selectedItemIndex] != null) {
			mRadioBtn[selectedItemIndex].setChecked(true);
			isTabChanged = false;
		}
		selectBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final int position = mListView.getPositionForView((View) view
						.getParent());
				mListView.performItemClick(view, position, 0);
				setRadioView(position);
			}
		});

		boolean connStatus = (boolean) pdnConn.getConnectStatus();
		connectBtn.setChecked(pdnConn.isConnected);
		mSpinner[position].setEnabled(!pdnConn.isConnected);
		// setRadioView(mRadioBtn[selectedItemIndex]);
		// selectBtn.setChecked(checked)
		if (mOldView == null) {
			selectBtn.setChecked(true);
			mListView.performItemClick(row, position, 0);
			mOldView = selectBtn;
		}
		connectBtn.setTag(pdnConn);
		connectBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				PdnConnection pdnConn = (PdnConnection) view.getTag();
				mPdnConn = pdnConn;
				final int position = mListView.getPositionForView((View) view
						.getParent());
				mListView.performItemClick(view, position, 0);
				mRadioBtn[position].setChecked(true);
				setRadioView(position);

				if (((ToggleButton) view).isChecked()) {
					pdnConn.isConnected = true;
					// handle toggle On
					createConnection(pdnConn, position);
					mSpinner[position].setEnabled(false);
					pdnConn.setConnectStatus(pdnConn.APN,
							STATUS_START_CONNECTION);
					Toast.makeText(view.getContext(),
							"start connection for " + pdnConn.getApn(),
							Toast.LENGTH_SHORT).show();
				} else {
					// handle toggle Off
					pdnConn.setConnectStatus(pdnConn.APN,
							STATUS_START_DISCONNECTION);
					Toast.makeText(view.getContext(),
							"stop connection for " + pdnConn.getApn(),
							Toast.LENGTH_SHORT).show();
					mSpinner[position].setEnabled(true);
					stopConnection(pdnConn);
				}
			}
		});
		return row;
	}

	public void setRadioView(int pos) {
		if (mRadioBtn[1] != null) {
			if (pos == 0) {
				mRadioBtn[0].setChecked(true);
				mRadioBtn[1].setChecked(false);
			} else {
				mRadioBtn[1].setChecked(true);
				mRadioBtn[0].setChecked(false);
			}
		}
	}

	private void createConnection(PdnConnection pdnConn, int pos) {
		int apnPos = pdnConn.getApnPos();
		NetworkCapabilities netCap = new NetworkCapabilities();
		netCap.addCapability(APN_CAP_LIST[apnPos]);
		netCap.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
		netCap.addTransportType(NetworkCapabilities.TRANSPORT_EPDG);
		NetworkRequest netRequest = new NetworkRequest(netCap,
				legacyTypeForNetworkCapabilities(APN_CAP_LIST[pos]), pos);
		pdnConn.networkRequest = netRequest;
		pdnConn.networkCapabilities = netCap;
		/* specific to IMS */
		if (pdnConn.APN == "IMS") {
			QosStatus qosStatus = new QosStatus();
			qosStatus.reset();
			qosStatus.qci = 5;
			DefaultBearerConfig defaultBearerConfig = new DefaultBearerConfig(
					1, qosStatus, 0, 1, 1);
			try {
				getITelephonyEx().setDefaultBearerConfig(
						PhoneConstants.APN_TYPE_IMS, defaultBearerConfig, 0);
			} catch (Exception e) {
				Log.d(TAG, "[PdnListAdaptor]Exception caught" + e);
			}
		}
		mConnMgr.requestNetwork(netRequest, pdnConn.networkCallback,
				MAX_CONNECTION_TIME);
		com.mediatek.connectivity.EpdgTestApp.TestEpdgConnection
				.updateStatus("Start" + netRequest);
		Log.d(TAG, "[PdnListAdaptor]send connection request for:" + pdnConn.APN);

	}

	private ITelephonyEx getITelephonyEx() {
		return ITelephonyEx.Stub.asInterface(ServiceManager
				.getService(Context.TELEPHONY_SERVICE_EX));
	}

	private void stopConnection(PdnConnection pdnConn) {
		Log.d(TAG,
				"[PdnListAdaptor]UE initiated => Release network Request for : "
						+ pdnConn.APN);
		mConnMgr.unregisterNetworkCallback(pdnConn.networkCallback);
	}

	private int legacyTypeForNetworkCapabilities(int capability) {
		if (capability == NetworkCapabilities.NET_CAPABILITY_IMS) {
			return ConnectivityManager.TYPE_MOBILE_IMS;
		}

		if (capability == NetworkCapabilities.NET_CAPABILITY_MMS) {
			return ConnectivityManager.TYPE_MOBILE_MMS;
		}

		if (capability == NetworkCapabilities.NET_CAPABILITY_SUPL) {
			return ConnectivityManager.TYPE_MOBILE_SUPL;
		}

		return ConnectivityManager.TYPE_NONE;
	}

}