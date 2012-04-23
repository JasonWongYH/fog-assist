/*
  Copyright (C) 2011-2012:
        Michael Hardegger and Daniel Roggen, Wearable Computing Laboratory, ETH Zurich

   All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY COPYRIGHT HOLDERS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
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