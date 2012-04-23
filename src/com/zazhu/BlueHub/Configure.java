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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.content.*;


public class Configure extends ListActivity{
	

	private ListView mainListView = null;
	
	
	CustomSensorListAdapter sensorList = null;
	
	Configure() {
		//TODO initialize sensor list
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super .onCreate(savedInstanceState);
		setContentView(R.layout.configure);
		
		Button btnClear = (Button) findViewById(R.id.btnClear);
		
		btnClear.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "Sensors Cleared",
						Toast.LENGTH_SHORT).show();
				ClearSelections();
			}
		});
		
		this.mainListView = getListView();
		mainListView.setCacheColorHint(0);
		
		//TODO Bind the data with the list initialized in the constructor
		
		
		
		
		
		
		
		
	}


	private void ClearSelections() {
		// TODO Auto-generated method stub
		for (int i = 0; i < this.sensorList.getCount(); i++) {
			LinearLayout v = (LinearLayout) this.mainListView.getChildAt(i);
			CheckBox chk = (CheckBox) v.getChildAt(0);
			chk.setChecked(false);
		}
		this.sensorList.ClearSelections();
	}
}
