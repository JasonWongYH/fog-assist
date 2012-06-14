/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zazhu.BlueHub;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.Vector;



import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.fogAssist.Feature;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;


/**
 * This is the main Activity that displays the current chat session.
 */

public class BlueHub extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Variables for multiple connections
	private static ArrayList<String> mSensorList = new ArrayList<String>();
	private static ArrayList<String> mDisplayList = new ArrayList<String>();
	private static HashMap<String, Integer> mSensorPosition = new HashMap<String, Integer>();
	private static int mSensorCounter = 0;

	
	

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_CONNECT_SERVICE = 3;

	// Layout Views
	private TextView mTitle;
	private ListView mMainView;
	// private EditText mOutEditText;
	// private Button mSendButton;

	// Name of the connected device
	
	
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	

	
	// Database Variables
	private static final String DATABASE_NAME = "BlueHubDB";
	private static final int DATABASE_VERSION = 1;
	private static final String DB_PATH = "/sdcard/BlueHubDB.db";
	
	private SQLiteDatabase mDb;
	
	
	// separators inside the message
	private static final String FIELD_SEP = " ";
	
	// variables for options TODO to be changed when having the UI
	private static final char INTERNAL    = '1';
	private static final char EXTERNAL    = '2';
	private static final char EXT_INT     = '3';
	
	private static char mCurrentClassifier;
	
	// FoG variables
	private static final double FOG    = 1.0;
	private static final double NO_FOG = 2.0;
	
	
	// weka classifier
    weka.classifiers.Classifier mCls = null;
    
    // thread for classification variable
    static boolean mShouldContinue = true;
			
	// Write to file variables
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	
	//TODO: Most declarations above are out of date, should be cleaned up
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	/** Some text view we are using to show state information. */
	//TextView mCallbackText;
	// Array adapter for the sensor list
	private ArrayAdapter<String> mSensorDisplayArrayAdapter;
	
	private LinkedList<String> mSensorQueue = new LinkedList<String>();
	// default
	int mQueueSize = 64;
	
	
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    
		@Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case BlueHubService.MSG_SET_DEVICE:
	                //mTitle.setText("Received from service: " + msg.arg1);
	                Bundle b = msg.getData();
	                String readings = b.getString(BlueHubService.READINGS);
	                //Log.d(TAG, Integer.toString(mSensorQueue.size())+ " " + readings);
	                
	                //External sensor reading format:
	                //"Time_SensorName_SensorNumber_Counter_Xacc_Yacc_Zacc_Xgyro_Ygyro_checksum"
	                
	                //Internal sensor reading format:
	                //"Xacc_Yacc_Zacc"
	                
	                // "_" -> represents a space
	                
	                //Determine if msg is of the right type to be stuffed into the queue
	                char mode = gui_sharedPrefs.getSensorValue(BlueHub.this);
	        		StringTokenizer st;
	        		st = new StringTokenizer(readings, FIELD_SEP);
	        		int toks = st.countTokens();
	                if (mode==EXTERNAL & toks < 4) {
	                	break;
	                }
	                
	                if (mode==INTERNAL & toks > 3) {
	                	break;
	                }
	                
	                
	                //If current queue size is less than specified size:
	                //	add an additional messages to queue (enlarge queue)
	                //If current queue size is larger than specified size:
	                // 	remove TWO elements from the head of the queue and
	                //  insert the incoming message (reduce queue size by 1)
	                //If current queue size is at the specified size:
	                //	remove head element and add to tail (maintain queue size)
	               
	                if (mSensorQueue.size()<mQueueSize) {
	                	mSensorQueue.offer(readings);
//	                } else if (mSensorQueue.size()>mQueueSize) {
//	                	mSensorQueue.poll();
//	                	mSensorQueue.poll();
//	                	mSensorQueue.offer(readings);
	                } else {
	                	mSensorQueue.poll();
	                	mSensorQueue.offer(readings);
	                }
	                
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	}
	
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
	        mTitle.setText("Attached.");
	        
	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	        
	    	    	Message msg = Message.obtain(null,
	    	    		BlueHubService.MSG_REGISTER_CLIENT);
	    	    	msg.replyTo = mMessenger;
	            
	    			mService.send(msg);
	    		
	            /*            
	            // Give it some value as an example.
	            msg = Message.obtain(null,
	                    BlueHubService.MSG_SET_DEVICE, this.hashCode(), 0);
	            mService.send(msg);*/
	            
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	         
	        // As part of the sample, tell the user what happened.
	        Toast.makeText(BlueHub.this, R.string.remote_service_connected,
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        mTitle.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(BlueHub.this, R.string.remote_service_disconnected,
	                Toast.LENGTH_SHORT).show();
	    }
	};
	

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
		
		//This intent specifies BlueHubService.class will be the receiver
	    bindService(new Intent(BlueHub.this, 
	            BlueHubService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	    mTitle.setText("Binding.");
	    
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                        BlueHubService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        mTitle.setText("Unbinding.");
	    }
	}
	
	private void sendDeviceToService(BluetoothDevice device) {
		if(mIsBound) {
			if(mService != null) {
				try {
					Message msg = Message.obtain(null, BlueHubService.MSG_SET_DEVICE, device);
					Log.d(TAG, device.getName());
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					//doUnbindService();
				}
			}
		}
	}
	private void initSetup() {
		Log.d(TAG, "setupService()");

		mSensorList.clear();
		mDisplayList.clear();
		mSensorCounter = 0;
		// Initialize the array adapter and bind it to mSensorList
		mSensorDisplayArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message, mDisplayList);
		mMainView = (ListView) findViewById(R.id.listViewMain);
		mMainView.setAdapter(mSensorDisplayArrayAdapter);

		// Initialize the BluetoothChatService to perform bluetooth connections
		//mHubService = new BlueHubCommunicator(this, mHandler);

		// Initialize the buffer for outgoing messages
		//mOutStringBuffer = new StringBuffer("");
	}
	
	
	
	/**
	 * receives the last reads from the sensors and creates the features we use
	 * only the acc x,y,z (either from internal or external sensor)
	 * 
	 * @param sensorQueue
	 * @throws Exception 
	 */
	private Instance processingSenseData(Queue<String> sensorQueue,
			char whatSensor) throws Exception {
		
		BufferedReader reader;
		Instances format;
		Instance newInstance = null;

		Log.d(TAG, "Queue size = " + mQueueSize);
		
		if (sensorQueue.size() <= 0 )
			throw new Exception("Queue empty");
		
		// create the arrays that will contain the accelerometer data
		// s.x s.y s.z
		double[] sx = new double[sensorQueue.size()];
		double[] sy = new double[sensorQueue.size()];
		double[] sz = new double[sensorQueue.size()];

		String rawReading;
		StringTokenizer st;

		int index;

		if (D)
			Log.e(TAG, "+++ COMPUTING FEATURES +++");

		// 1. collect raw data. what kind of sensing data? external vs. internal
		switch (whatSensor) {
		case EXTERNAL:
			index = 0;
			while ((rawReading = sensorQueue.poll()) != null) {
				// FORMAT:
				// "Time_SensorName_SensorNumber_Counter_Xacc_Yacc_Zacc_Xgyro_Ygyro_checksum"
				// position of the values needed: s.x = 4, s.y = 5, s.z = 6
				st = new StringTokenizer(rawReading, FIELD_SEP);
				// not needed data
				for (int i = 0; i < 4; i++)
					st.nextToken();
				// s.x, s.y, s.z
				sx[index] = Double.valueOf(st.nextToken());
				sy[index] = Double.valueOf(st.nextToken());
				sz[index] = Double.valueOf(st.nextToken());

				index += 1;
			}
			
			// 2. process raw data
			// 2.1 read the input format for the instance (TODO must be changed to
			// use weka classes)
			reader = new BufferedReader(new InputStreamReader(
					getResources().openRawResource(R.raw.format_extern)));

			try {
				format = new Instances(reader);

				if (format.classIndex() == -1)
					format.setClassIndex(format.numAttributes() - 1);

				// 2.2 create a new instance
				newInstance = new DenseInstance(7);
				newInstance.setDataset(format);
				// set attributes
				newInstance.setValue(format.attribute(0), Feature.getStd(sx));
				newInstance.setValue(format.attribute(1), Feature.getStd(sy));
				newInstance.setValue(format.attribute(2), Feature.getStd(sz));
				newInstance.setValue(format.attribute(3), Feature.getMean(sx));
				newInstance.setValue(format.attribute(4), Feature.getMean(sy));
				newInstance.setValue(format.attribute(5), Feature.getMean(sz));
				// set unknown class
				newInstance.setMissing(format.attribute(6));

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			break;
		case INTERNAL:
			
			index = 0;
			while ((rawReading = sensorQueue.poll()) != null) {
				
				// FORMAT "Xacc_Yacc_Zacc"
				// position of the values needed: s.x = 0, s.y = 1, s.z = 2
				st = new StringTokenizer(rawReading, FIELD_SEP);

				// s.x, s.y, s.z
				sx[index] = Double.valueOf(st.nextToken());
				sy[index] = Double.valueOf(st.nextToken());
				sz[index] = Double.valueOf(st.nextToken());

				index += 1;
			}
			
			// 2. process raw data
			// 2.1 read the input format for the instance (TODO must be changed to
			// use weka classes)
			reader = new BufferedReader(new InputStreamReader(
					getResources().openRawResource(R.raw.format_intern)));

			try {
				format = new Instances(reader);

				if (format.classIndex() == -1)
					format.setClassIndex(format.numAttributes() - 1);

				// 2.2 create a new instance
				newInstance = new DenseInstance(7);
				newInstance.setDataset(format);
				// set attributes
				newInstance.setValue(format.attribute(0), Feature.getStd(sx));
				newInstance.setValue(format.attribute(1), Feature.getStd(sy));
				newInstance.setValue(format.attribute(2), Feature.getStd(sz));
				newInstance.setValue(format.attribute(3), Feature.getMean(sx));
				newInstance.setValue(format.attribute(4), Feature.getMean(sy));
				newInstance.setValue(format.attribute(5), Feature.getMean(sz));
				// set unknown class
				newInstance.setMissing(format.attribute(6));

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			break;
		default:
			if (D)
				Log.e(TAG,
						"+++ COMPUTING FEATURES: NO VALUE FOR THE SENSOR READING +++");
			break;
		}


		return newInstance;

	}
	
	
	
	/**
	 * block of instructions to read the weka serialized classifier
	 */
	public void readClassifier(final char model) {

		Runnable task = new Runnable() {

			public void run() {

				try {

					/* reading the classifier */
					BufferedInputStream bfi = null;
					switch (model) {
					case EXTERNAL:
						bfi = new BufferedInputStream(getResources()
								.openRawResource(R.raw.j48));
						break;
					case INTERNAL:
						bfi = new BufferedInputStream(getResources()
								.openRawResource(R.raw.j48));
						break;
					case EXT_INT:
						bfi = new BufferedInputStream(getResources()
								.openRawResource(R.raw.j48));
						break;
					default:
						Log.i("status",
								"+++ NO CLASSIFIER FILE TO READ FROM +++");
						break;
					}
					ObjectInputStream ois;
					ois = new ObjectInputStream(bfi);
					Log.i("status", "+++ READING THE SERIALIZED CLASSIFIER +++"); 
					// this will take a while -- be patient :)
					mCls = (weka.classifiers.Classifier) ois.readObject();
					ois.close();

				} catch (StreamCorruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			
		};
		// classifier has been read
		
		
		ThreadGroup testGroup = new ThreadGroup("fog");
		// create a thread with increased stack size to read the complex object
		Thread th = new Thread(testGroup, task, "fog classifier thread",
				1024 * 128);
		
		th.start();
		
		
		try {
			th.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	public void reinitializeUponSensorChange(){
		System.out.println("+++REINITIALIZING");

		char newMode = gui_sharedPrefs.getSensorValue(BlueHub.this);
		mSensorQueue.clear();
	
		switch(newMode){
		case '1':
			mQueueSize = 80;
			break;
		case '2':
			mQueueSize = 64;
			break;
		case '3':
			mQueueSize = 150;
			break;
		default:
			break;
		}
		
		readClassifier(newMode);
		mCurrentClassifier=newMode;
		
		
		Toast.makeText(this, "New classifier loaded", Toast.LENGTH_LONG);
		
	}
//	//private PopupWindow pw;
//	public void InitiatePopupWindow(String displayText) {
//		LayoutInflater inflater = (LayoutInflater) BlueHub.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View layout = inflater.inflate(R.layout.popup, (ViewGroup) findViewById(R.id.popup_element));
//		TextView t = (TextView) findViewById(R.id.ModalityChange);
//		t.setText(displayText);
//		
//		PopupWindow pw = new PopupWindow(layout,200,200,true);
//		pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
//	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
				
		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		ImageButton run_button  = (ImageButton) findViewById(R.id.run_button);
        ImageButton stop_button = (ImageButton) findViewById(R.id.stop_button);
        ImageButton info_button = (ImageButton) findViewById(R.id.info_button);
		run_button.setEnabled(true); 
		stop_button.setEnabled(false);  
		
		
		
        run_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		
        		// for using threads: http://android-developers.blogspot.com/2009/05/painless-threading.html
        		
        		ImageButton run_button = (ImageButton) findViewById(R.id.run_button);
        		ImageButton stop_button = (ImageButton) findViewById(R.id.stop_button);
        		run_button.setEnabled(false); 
        		stop_button.setEnabled(true); 
        		
        		// creates the bio-feedback variables
        		final Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        		
        		//boolean acousticFeedback = gui_sharedPrefs.getAcousticFlag(getApplicationContext());
        		//boolean tactileFeedback = gui_sharedPrefs.getTactileFlag(getApplicationContext());
        		//char listselection = gui_sharedPrefs.getSensorValue(getApplicationContext());        		
        		
        		
        		Log.i("classifier", "Current classifier: " + Character.toString(mCurrentClassifier) 
        				+ " Preferences: " + gui_sharedPrefs.getSensorValue(BlueHub.this));
        		if (mCurrentClassifier == gui_sharedPrefs.getSensorValue(BlueHub.this)) {
        			// this is useful for the thread that is doing the classification when the start button is pressed
        			mShouldContinue = true;
        			mTitle.setText("Classifying..."); 
        	       		
        			Thread background = new Thread(new Runnable() {

        			public void run() {
        				while (mShouldContinue & mCurrentClassifier==gui_sharedPrefs.getSensorValue(BlueHub.this)) 
        				{
        					try {
        						
    	     			    System.out.println("Sleeping beauty ... for 0.5 s"); 
    		     			Thread.sleep(500); //sleeps for 0.5s   
    		     			
	     			    	// getting the instance from the last data (taking into account the sensor selected)
        					// create a copy to avoid to empty the original queue
        					LinkedList<String> mSensorQueueCopy = new LinkedList<String>();
        					mSensorQueueCopy.addAll(mSensorQueue);
        					
        					System.out.println(mSensorQueueCopy.toString());
        					
	     			    	Instance instToBeClassified =  processingSenseData(mSensorQueueCopy, gui_sharedPrefs.getSensorValue(getApplicationContext()) );
	     			    	System.out.println(instToBeClassified.toString());
	     			    		
	     			      				
	     			    	// classify
	     			    	double classRes = mCls.classifyInstance(instToBeClassified);
	     			    	System.out.println( String.valueOf(classRes) );
	     			    	System.out.println( instToBeClassified.classAttribute().value((int) classRes));
	     			    	
	     			    	if (classRes == FOG) {
		     			    	// what kind of feedback was selected 
		     			    	if( gui_sharedPrefs.getAcousticFlag(getApplicationContext()) ) {
		     			    	// TODO here to put sound
		     			    	} else if (gui_sharedPrefs.getTactileFlag(getApplicationContext())) {
		     			    		vib.vibrate(2000); // vibrates for 1s
		     			    	}
	     			    	}
		     			     
        					} catch (Exception e) {
        						Log.v("Error", e.toString());
        					}
        				}
     			    
     			   	}
        			
        		});
        	
        			background.start();

        		} else {
        			
        			mTitle.setText("Reloading classifier...");
        			
        			        			
        			reinitializeUponSensorChange();

            		run_button.setEnabled(true); 
            		stop_button.setEnabled(false);
        			mTitle.setText("Reinitialized, press play to begin.");
        		}
        	}
        });

        stop_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		ImageButton run_button = (ImageButton) findViewById(R.id.run_button);
        		ImageButton stop_button = (ImageButton) findViewById(R.id.stop_button);
        		run_button.setEnabled(true); 
        		stop_button.setEnabled(false);
        		
        		//Sinziana's stop classifier code
        		// here is not need to do nothing -- the scenario will change and the classifier code won't run (being in the the run_button loop)
        		mShouldContinue = false;
        		mTitle.setText("Classifier paused!");
        	}
        });
        
        info_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		Toast.makeText(getApplicationContext(), "The research leading to these results has received funding from the European Union - Seventh Framework Programme (FP7/2007-2013) under grant agreement n288516 (CuPiD project).",
        				Toast.LENGTH_LONG).show();
        		
        		
        		
        		
        				
        				//.getSensorValue(getApplicationContext())));
        	}
        });
        
		//TODO: check to see if service has already been started by another app
		Intent intent = new Intent(this, BlueHubService.class);
		
		startService(intent);
		doBindService();	
		
		
		//gui_sharedPrefs prefs = new gui_sharedPrefs();
		// read classifier from serialized object
		// DEFAULT reading -- external
		readClassifier(INTERNAL);
		//gui_sharedPrefs.setSensorValue(this, INTERNAL);
		//Log.i(TAG, "Preference upon creation: " + Character.toString(gui_sharedPrefs.getSensorValue(BlueHub.this)));
		mCurrentClassifier = INTERNAL;
		
		//Toast.makeText(this, "Classifier for internal sensors loaded", Toast.LENGTH_SHORT).show();
		// test if I read the classifier
		System.out.println(mCls.toString());
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} /*else {
			if (mHubService == null)
				initSetup();
		}*/
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		/*if (mHubService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mHubService.getState() == BlueHubCommunicator.STATE_NONE) {
				// Start the Bluetooth chat services
				mHubService.start();
			}
		}*/
	}

	
	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
		
		doUnbindService();
		stopService(new Intent(BlueHub.this, BlueHubService.class));
		
		
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {

		case REQUEST_CONNECT_DEVICE:
			// Check if the intent actually exists
			if (data!=null) { 
				// Get the device MAC address
				String address = data.getExtras().getString(
					DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				sendDeviceToService(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				initSetup();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
			
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		
		
		// bind activity to menu item
		Intent prefsIntent = new Intent(this.getApplicationContext(),
		        gui_prefsActivity.class);
		 
		MenuItem preferences = menu.findItem(R.id.settings);
		preferences.setIntent(prefsIntent);
		
		//Gui_v1.this.startActivity(new Intent(Gui_v1.this.getApplicationContext(),gui_prefsActivity.class));
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.device:
			Intent deviceIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(deviceIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.disconnect:
			//stop classifier
			mShouldContinue = false;
			
			//disconnect from service
			doUnbindService();
			stopService(new Intent(BlueHub.this, BlueHubService.class));
			

			return true;
		
		/*case R.id.db:
			// DataBaseHelper mDbHelper = new DataBaseHelper();
			
			
			mDb = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY
					+ SQLiteDatabase.CREATE_IF_NECESSARY + SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			String [] columns = {"time","label"};
			Cursor mExampleQuery = mDb.rawQuery("SELECT time,label" +
											 " FROM " + "sub01_01" + " LIMIT 10;", null);
			int timeColumn = mExampleQuery.getColumnIndex("time");
			int labelColumn = mExampleQuery.getColumnIndex("label");
			
			
			if (!mExampleQuery.isFirst()) mExampleQuery.moveToFirst();
			do {
				Log.i("data", "Time: " + Float.toString(mExampleQuery.getFloat(timeColumn)) 
						+ " Label: " + Integer.toString(mExampleQuery.getInt(labelColumn)));
			} while (mExampleQuery.moveToNext());
			
			
			//Log.i("data",Integer.toString(timeColumn)+Integer.toString(labelColumn));
			
			return true;*/
		case R.id.settings:
	    	startActivity(item.getIntent());
	        break;
		}
		return false;
	}
	
//	private void writeToExternal(String message) {
//		String state = Environment.getExternalStorageState();
//		
//		if (Environment.MEDIA_MOUNTED.equals(state)) {
//			mExternalStorageAvailable = mExternalStorageWriteable = true;
//		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//		    // We can only read the media
//		    mExternalStorageAvailable = true;
//		    mExternalStorageWriteable = false;
//		} else {
//		    // Something else is wrong. It may be one of many other states, but all we need
//		    //  to know is we can neither read nor write
//		    mExternalStorageAvailable = mExternalStorageWriteable = false;
//		}
//		
//		if (mExternalStorageAvailable == false || mExternalStorageWriteable == false) return;
//		
//		char[] charMessage = message.toCharArray();
//		// TODO: finish code to write to external storage
//		
//		
//	}

}
