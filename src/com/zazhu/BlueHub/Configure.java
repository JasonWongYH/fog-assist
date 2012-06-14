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
