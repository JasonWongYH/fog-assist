/*
  Copyright (C) 2011-2012:
        Zack Zhu and Daniel Roggen, Wearable Computing Laboratory, ETH Zurich

   All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY COPYRIGHT HOLDERS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.zazhu.BlueHub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


	
public class BlueHubService extends Service implements SensorEventListener{
	private static final String TAG = "BlueHubService";
	public static final String READINGS = "Readings";
	private BlueHubCommunicator mCommunicator = null;
	//private ArrayList<String> mSensorList = new ArrayList<String>();
	private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	private int mSensorCount = 0;
	
	private Intent mStartIntent;
	
	// Message types sent from the BlueHubCommunicator Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	//public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_LIST_UPDATE = 3;
	public static final int MESSAGE_TOAST = 4;
	
	
    // Internal sensor fields
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private double[] mLinearAcceleration;
    private long mSensorTimeStamp;
    
    
	/** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
	
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, or to stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_DEVICE = 3;
	private static final boolean D = true;

    /**
     * Handler of incoming messages from clients.
     */
    class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                	Log.d(TAG, "Client Registered");
                    mClients.add(msg.replyTo);
                    //Log.d("REGISTER", Integer.toString(mClients.size()));
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_DEVICE:
                	//Log.d(TAG, "MSG_SET_DEVICE received: " + (BluetoothDevice) msg.obj);
                	
                	mDevices.add((BluetoothDevice) msg.obj);
            		mSensorCount++;      	
            	
            		//Log.d(TAG, mDevices.get(mDevices.size()-1).toString());
                	
                    
                    mCommunicator.connect(mDevices.get(mDevices.size()-1));
            		
                    break;
                
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new ClientHandler());

	

	
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
    	return mMessenger.getBinder();
    }
	
    
    // The Handler that gets information back from the BlueHubCommunicator
 	private final Handler mCommunicatorHandler = new Handler() {
 		

		@Override
 		public void handleMessage(Message msg) {
 			switch (msg.what) {
 			case MESSAGE_STATE_CHANGE:
 				// Optionally for keeping the UI updated for current state
 				/*if (D)
 					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
 				switch (msg.arg1) {
 				case BlueHubCommunicator.STATE_CONNECTED:
 					//TODO: GUI updates for the activity

 					//mTitle.setText(R.string.title_connected_to);
 					//mTitle.append(mSensorList.size() + " device(s)");
 					break;
 				case BlueHubCommunicator.STATE_CONNECTING:
 					//TODO: 
 					//mTitle.setText(R.string.title_connecting);
 					break;
 				case BlueHubCommunicator.STATE_LISTEN:
 				case BlueHubCommunicator.STATE_NONE:
 					mTitle.setText(R.string.title_not_connected);
 					break;
 				}*/
 				break;
 			/*case MESSAGE_WRITE: // never happens
 				
 				
 				byte[] writeBuf = (byte[]) msg.obj;
 				// construct a string from the buffer
 				String writeMessage = new String(writeBuf);
 				mSensorDisplayArrayAdapter.add("Me:  " + writeMessage);
 				break;
 			 */
 			case MESSAGE_READ:				
 				String readMessage = (String) msg.obj;
 				//Send device name and sensor reading as a message bundle to client
 				//received by client activity as (device,reading) key-val pair
 				
 				//sendMessagetoClients(mDevices.get(msg.arg2).getName(), readMessage); 
 				sendMessagetoClients(readMessage); 
 				
 				break;
 			case MESSAGE_DEVICE_LIST_UPDATE:
 				//TODO: update the list of connected devices
 				
 				//mSensorList = (ArrayList<String>) msg.obj;
 				Toast.makeText(getApplicationContext(),
 						"Connected to " + mDevices.get(msg.arg1).getName(),
 						Toast.LENGTH_SHORT).show();
 						
 				break;
 			case MESSAGE_TOAST:
 								
 				Toast.makeText(BlueHubService.this,
 						msg.getData().getString(BlueHub.TOAST), Toast.LENGTH_SHORT)
 						.show();
 				break;
 			}
 		}
 	};

	
 	
 	private void sendMessagetoClients(String sensorReading) {
 		//Log.d("sendMessagetoClients", Integer.toString(mClients.size()) + deviceId+ "::" + sensorReading);
 		
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	// This only sends the data as integer
                //mClients.get(i).send(Message.obtain(null,
                //        MSG_SET_VALUE, mValue, 0));
            	
            	Bundle b = new Bundle();
            	//b.putString(deviceId,sensorReading);
            	//b.putString(READINGS, deviceId + "\t" + sensorReading);
            	b.putString(READINGS, sensorReading);
            	Message msg = Message.obtain(null,MSG_SET_DEVICE);
            	msg.setData(b);
            	mClients.get(i).send(msg);
            	           	
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
 	}
	/*
 	private ArrayList<Messenger> mSensorList;
	private ArrayList<Messenger> mDisplayList;
	private int mSensorCounter;
	private ArrayAdapter<String> mSensorDisplayArrayAdapter;
    
 	private void initSetup() {
		Log.d(TAG, "setupChat()");

		mSensorList.clear();
		mDisplayList.clear();
		mSensorCounter = 0;
		// Initialize the array adapter and bind it to mSensorList
		mSensorDisplayArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message, mDisplayList);
		mMainView = (ListView) findViewById(R.id.listViewMain);
		mMainView.setAdapter(mSensorDisplayArrayAdapter);

		// Initialize the BluetoothChatService to perform bluetooth connections
		mHubService = new BlueHubCommunicator(this, mHandler);

		// Initialize the buffer for outgoing messages
		//mOutStringBuffer = new StringBuffer("");
	}
 	*/
 
 	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate...Service Started");
		
		//Internal Sensor initialization
		
     
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mLinearAcceleration = new double[3];
		
		//mCommunicator = new BlueHubCommunicator(handle);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();
        
        mCommunicator = new BlueHubCommunicator(mCommunicatorHandler);
        streamInternal();
	}
	

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		
		// Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);
        // Stop BlueHubCommunicator
        mCommunicator.stop();
        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
        
        mSensorManager.unregisterListener(this, mAccelerometer);
        
        stopSelf();
	}

	/**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.remote_service_started);

        // Set the icon, scrolling text and timestamp
        //Notification notification = new Notification(R.drawable.stat_sample, text,
        //        System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        //PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        //        new Intent(this, Controller.class), 0);

        // Set the info for the views that show in the notification panel.
        //notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
        //               text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        //mNM.notify(R.string.remote_service_started, notification);
    }


    public void streamInternal(){
    	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		final double alpha = 0.8;
		
		double [] gravity = new double[3];
		
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        
		mLinearAcceleration[0] = event.values[0] - gravity[0];
		mLinearAcceleration[1] = event.values[1] - gravity[1];
		mLinearAcceleration[2] = event.values[2] - gravity[2];
		
		mSensorTimeStamp = event.timestamp;
		sendMessagetoClients(Double.toString(mLinearAcceleration[0])
				+ " " + Double.toString(mLinearAcceleration[1]) 
				+ " " +	Double.toString(mLinearAcceleration[2]));
				
		//Log.d(TAG, Double.toString(mLinearAcceleration[0]) + " " + Double.toString(mLinearAcceleration[1])
		//		+ " " + Double.toString(mLinearAcceleration[2]));
	}
}
