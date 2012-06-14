package com.zazhu.BlueHub;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;

public class gui_sharedPrefs {
	public final static String PREFS_NAME = "gui_prefs";
	
	public static boolean getAcousticFlag(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(context.getString(R.string.pref_key_flag_acoustic),
				false);
		
	}
	
	public static void setAcousticFlag(Context context, boolean newValue) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(
				context.getString(R.string.pref_key_flag_acoustic), newValue);
		prefsEditor.commit();
	}
	
	//----------------------------------------
	
	public static boolean getTactileFlag(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(context.getString(R.string.pref_key_flag_tactile),
				false);
	}
	
	public static void setTactileFlag(Context context, boolean newValue) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(
				context.getString(R.string.pref_key_flag_tactile), newValue);
		prefsEditor.commit();
	}

	//----------------------------------------
		
	public static char getSensorValue(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		//Log.d("adf",(prefs.getString(context.getString(R.string.pref_key_list_preference),
		//		"1")));
		//prefs.getString(context.getString(R.string.pref_key_list_preference),"1").charAt(0);
		return prefs.getString(context.getString(R.string.pref_key_list_preference),"1").charAt(0);
	}

//	public static void setSensorValue(Context context, int newValue) {
//		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//		Editor prefsEditor = prefs.edit();
//		prefsEditor.putInt(
//				context.getString(R.string.pref_key_list_preference), newValue);
//		
//		prefsEditor.commit();
//		Toast.makeText(context, "Sensor modality changed, reloading the appropriate classifier...", Toast.LENGTH_LONG).show();
//	}
}