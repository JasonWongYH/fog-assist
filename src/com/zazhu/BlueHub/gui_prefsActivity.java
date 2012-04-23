package com.zazhu.BlueHub;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;

public class gui_prefsActivity extends PreferenceActivity {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName(gui_sharedPrefs.PREFS_NAME);
		addPreferencesFromResource(R.xml.prefs);
	}
	

}
