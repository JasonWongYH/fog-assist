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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import com.zazhu.BlueHub.FrameParser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BlueHubCommunicator {
    // Debugging
    private static final String TAG = "BlueHubService";
    private static final boolean D = true;
    
    // -----Bluetooth Connection Variables------
    // Name for the SDP record when creating server socket
    private static final String NAME = "BlueHubServer";

    private static UUID mUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private HashMap<BluetoothDevice, BluetoothSocket> mBtDeviceSocket;
    private HashMap<BluetoothSocket, BluetoothDevice> mBtSocketDevice;
    private ArrayList<BluetoothDevice> mBtDevices;
    private ArrayList<String> mBtDevicesName;
        
    
    
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    //private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    
    private HashMap<BluetoothDevice, ConnectedThread> mBtDeviceThread;

    // ---Something about states... not sure if needed 
    //Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    
    
    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BlueHubCommunicator(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;

        mBtDevices = new ArrayList<BluetoothDevice>();
        mBtDevicesName = new ArrayList<String>();
        mBtDeviceSocket = new HashMap<BluetoothDevice, BluetoothSocket>();
        mBtSocketDevice = new HashMap<BluetoothSocket, BluetoothDevice>();
        mBtDeviceThread = new HashMap<BluetoothDevice, ConnectedThread>();
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BlueHubService.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the hub service. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        //if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        /*if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;}*/

        // Start the thread to listen on a BluetoothServerSocket
        /*
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }*/
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        //BluetoothDevice currentDevice = deviceList.get(deviceList.size()-1);
            
        if (D) Log.d(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection 
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        // Cancel any thread currently running a connection
        // if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        //if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        
        mConnectedThread = new ConnectedThread(socket, device);
        
        mBtDeviceThread.put(device, mConnectedThread);
        
        mConnectedThread.start();
        
        
        //mBtConnectedThreads.put(device, mConnectedThread);
        // Send the name of the connected device back to the UI Activity
        
//        Message msg = mHandler.obtainMessage(BlueHub.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(BlueHub.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        
        for (BluetoothDevice curKey : mBtDeviceThread.keySet()) {
        	if (mBtDeviceThread.get(curKey) != null) {
        		 mBtDeviceThread.get(curKey).cancel();
        		 
        	}
        }
        mBtDeviceThread.clear();
        
        //if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    */
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BlueHubService.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BlueHub.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(BluetoothSocket socket, BluetoothDevice device) {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        try {
           socket.close();
        } catch (IOException e) {
            Log.e(TAG, "unable to close() socket during connection failure", e);
        }
        Message msg = mHandler.obtainMessage(BlueHubService.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BlueHub.TOAST, "Connection was lost to device: " + device.getName() + ". reconnecting...");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            
        	
    		try {
    			tmp = device.createRfcommSocketToServiceRecord(mUuid);
            } catch (IOException e) {
            	Log.e(TAG, "create() failed", e);                    
            }

        	// if the socket is still not established after trying all uuids
    		// store the null socket and close it with the cancel() function.
        	if (tmp == null) {
        		mmSocket = tmp;
        		this.cancel();
        	} else {
                mmSocket = tmp;
        	}
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery when connecting because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();              
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BlueHubService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BlueHubCommunicator.this) {
            	
            	// add device to list if it doesn't exist, result of -1
            	if (mBtDevices.indexOf(mmDevice) == -1) {
            		mBtDevices.add(mmDevice);
            		mBtDevicesName.add(mmDevice.getName());
            	}
            	
            	mBtDeviceSocket.put(mmDevice, mmSocket);
            	mBtSocketDevice.put(mmSocket, mmDevice);
            	mConnectThread = null;
            }
          
            
            //send message with updated device list
            mHandler.obtainMessage(BlueHubService.MESSAGE_DEVICE_LIST_UPDATE, mBtDevices.size()-1, -1, mBtDevicesName).sendToTarget();
            
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread implements Observer{
    	private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        private FrameParser mmFrameParser;
        private String mmFormat;
        private String [] mmChannels;
        
        private boolean mmCancelSignal = false;
        
        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "create ConnectedThread");
            
            mmFormat = "DX9;cs-s-s-ssss;x";
            
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            mmDevice = device;
            
            // Initialize FrameParser
            mmFrameParser = new FrameParser(mmDevice.getName(), mmFormat,
            		 mmChannels);
            mmFrameParser.addObserver(this);
        }


        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int numBytes;

            // Keep listening to the InputStream while connected
            while (true) {
            	//try {
				//	if (mmInStream.available() > 0) {
					    try {
					        // Read from the InputStream
					        numBytes = mmInStream.read(buffer); //returns the number of bytes set into the buffer
					        //Log.i("FrameParser", "ConnectedThread: Calling read with " + Integer.toString(numBytes)
					        //		+ " in the buffer passed in");
					        // Parse buffer, which will call notifyObservers (ConnectedThread)
					        // and call the update() function below
					        if (numBytes != 0) mmFrameParser.read(buffer, numBytes);
					        		                
					    } catch (IOException e) {
					        Log.e(TAG, "disconnected", e);
					        
					        //reconnect if it's not an intentional disconnect
					        if (!mmCancelSignal) connectionLost(mmSocket, mmDevice);
					        break;
					    }
				//	} else {
					//	this.sleep(100);
				//	}
				//} catch (IOException e) {
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				//} catch (InterruptedException e) {
					// TODO Auto-generated catch block
			//		e.printStackTrace();
		//		}
            } // finishes while
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BlueHub.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        */
        public void cancel() {
        	mmCancelSignal = true;
            try {
                mmSocket.close();
                
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }


		public void update(Observable observable, Object data) {
			DataPacket dp = (DataPacket) data;
			// Send the obtained bytes to the BlueHub Service
            //Log.i(TAG, "Reading from Device: " + mBtSocketDevice.get(mmSocket));
//            mHandler.obtainMessage(BlueHub.MESSAGE_READ, bytes, mBtDevices.indexOf(mmDevice), buffer)
//                    .sendToTarget();
            mHandler.obtainMessage(BlueHubService.MESSAGE_READ, -1, mBtDevices.indexOf(mmDevice), dp.toString() )
            	.sendToTarget();
		}
    }


}
