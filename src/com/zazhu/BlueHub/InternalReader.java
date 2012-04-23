package com.zazhu.BlueHub;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/*
 * 
 * Reads The following sensors:
 * 1. Accelerometer
 * 
 */
public class InternalReader extends Activity implements SensorEventListener {
	
	boolean accelerometerAvailable = false;
	boolean accEnabled = false;
	
	private final SensorManager mSensorManager;
	private final Sensor mAccelerometer;
	
	public InternalReader(){
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
		
		
	protected void onResume(){
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	protected void onPause(){
		super.onPause();
		mSensorManager.unregisterListener(this);	
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		
		
	}
	public void onSensorChanged(SensorEvent event){
		
	}
}
