package com.mediatek.connectivity.EpdgTestApp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * 
 * Tab page for EPDG functions.
 * 
 */
public class EpdgTestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cds_epdg_tab);

		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ActionBar.Tab tab1 = actionBar.newTab().setText("Connection");
		ActionBar.Tab tab2 = actionBar.newTab().setText("Configuration");
		ActionBar.Tab tab3 = actionBar.newTab().setText("Handoff");

		Fragment fragmentTab1 = new TestEpdgConnection();
		Fragment fragmentTab2 = new TestEpdgConfig();
		Fragment fragmentTab3 = new TestEpdgHandoff();

		tab1.setTabListener(new EpdgTestTabListener(fragmentTab1));
		tab2.setTabListener(new EpdgTestTabListener(fragmentTab2));
		tab3.setTabListener(new EpdgTestTabListener(fragmentTab3));

		actionBar.addTab(tab1);
		actionBar.addTab(tab2);
		actionBar.addTab(tab3);
	}

}
