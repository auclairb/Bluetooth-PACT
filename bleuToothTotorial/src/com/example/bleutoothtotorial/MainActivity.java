package com.example.bleutoothtotorial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements OnItemClickListener{

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int MESSAGE_READ = 1;
	protected static final int SUCCESS_CONNECT = 0;
	ArrayAdapter<String> listAdapter;
	Button bSend;
	ListView listView;
	TextView textViewReceive;
	EditText editTextSend;
	
	BluetoothAdapter btAdapter;
	Set<BluetoothDevice> devicesArray;
	ArrayList<String> pairedDevices;
	ArrayList<BluetoothDevice> devices;
	IntentFilter filter;
	BroadcastReceiver receiver;
	Handler mHandler;
	
	BluetoothDevice myDevice;
	
	
	@SuppressLint("HandlerLeak")
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
		if(btAdapter==null){
			Toast.makeText(getApplicationContext(), "No bluetooth detected", 0).show();
			finish();
		}
		else{
			if(!btAdapter.isEnabled()){
				turnOnBT();
			}
			getPaireDevices();
			startDiscovery();
			
		}
		
		
		/*
		mHandler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch(msg.what){
				case SUCCESS_CONNECT:
					ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
					Toast.makeText(getApplicationContext(), "CONNECT", 0).show();
					String s = "cool";
					connectedThread.write(s.getBytes());
					break;
				case MESSAGE_READ:
					byte[] readBuf = (byte[])msg.obj;
					String string = new String(readBuf);
					Toast.makeText(getApplicationContext(), string, 0).show();
					break;
				}
			}
		};
		*/
	}
		
	

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onDestroy() {
	  super.onDestroy();
	  btAdapter.cancelDiscovery();
	  unregisterReceiver(receiver);
	  
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_CANCELED){
			Toast.makeText(getApplicationContext(), "Bluetooth must be enable", Toast.LENGTH_SHORT).show();
			finish();
		}
	}


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if(btAdapter.isDiscovering()){
			btAdapter.cancelDiscovery();
		}
		if(listAdapter.getItem(arg2).contains(myDevice.getName())){
			Toast.makeText(getApplicationContext(),"device is paried",0).show();
			//Object[] o = devicesArray.toArray();
			//BluetoothDevice selectDevice = devices.get(arg2);
			//ConnectThread connect = new ConnectThread(selectDevice);
			//connect.start();
			ConnectThread connect = new ConnectThread(myDevice);
			connect.start();
		}
		else{
			Toast.makeText(getApplicationContext(),"device is not paried",0).show();
		}
	}
	
	private void startDiscovery() {
		// TODO Auto-generated method stub
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
		
	}


	private void turnOnBT() {
		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(intent, 1);
	}


	private void getPaireDevices() {
		devicesArray = btAdapter.getBondedDevices();
		if(devicesArray.size()>0){
			for(BluetoothDevice device:devicesArray){
				//pairedDevices.add(device.getName());
				listAdapter.add(device.getName());
				if(device.getName().equals("PACT51"))
                {
                    myDevice = device;
                }
			}
		}
	}

	/*================================ init() ==========================================*/
	private void init(){
		bSend = (Button)findViewById(R.id.bSend);
		textViewReceive = (TextView)findViewById(R.id.textViewReceive);
		editTextSend = (EditText)findViewById(R.id.editTextSend);
		
		listView = (ListView)findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		
		
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
		listView.setAdapter(listAdapter);
		
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new ArrayList<String>();
		devices = new ArrayList<BluetoothDevice>();
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){
				String action = intent.getAction();
				
				if(BluetoothDevice.ACTION_FOUND.equals(action)){
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					devices.add(device);
					String s ="";
					for(int a=0; a<pairedDevices.size();a++){
						if(device.getName().equals(pairedDevices.get(a))){
							s = "(Paired)";
							break;
						}
						
					}
					
					listAdapter.add(device.getName()+"\n"+device.getAddress());
				}
				
				else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
					//run some code
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					//run some code			
				}
				else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
					if(btAdapter.getState()==btAdapter.STATE_OFF){
					turnOnBT();
					}
				}
			}
		};
		
		registerReceiver(receiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(receiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(receiver, filter);
		
	}
	
	
	/*=============================== Connect Thread =====================================*/
	private class ConnectThread extends Thread {
	    
		private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	    private InputStream mmInputStream;
	    private OutputStream mmOutputStream;
	    
	    private String msg;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	    	btAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            mmOutputStream = mmSocket.getOutputStream();
		        mmInputStream = mmSocket.getInputStream();
		        
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        
	        //manageConnectedSocket(mmSocket);
			//mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
	        
	        bSend.setOnClickListener(new View.OnClickListener() {
				 @Override
				public void onClick(View v) {
					  String editTextMsg = editTextSend.getText().toString();
					  msg = editTextMsg+"\n";
				        try {
							mmOutputStream.write(msg.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			});
	        
	    }
	 
	    private void manageConnectedSocket(BluetoothSocket mmSocket2) {
			// TODO Auto-generated method stub
	    	
	    	
		}

		/** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
	
	
	/*============================== Connected Thread ====================================*/
	private class ConnectedThread extends Thread {
	    
		private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
}
