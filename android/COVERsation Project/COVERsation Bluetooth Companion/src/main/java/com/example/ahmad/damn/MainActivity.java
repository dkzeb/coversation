package com.example.ahmad.damn;
/*
    The bluetooth connectivity in this app is based on this demo https://github.com/hmartiro/android-arduino-bluetooth
    and all credit goes to the author behind that repository.
 */


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    IntentFilter intentFilter;
    // Tag for logging
    private static final String TAG = "BluetoothActivity";
    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    private final String address = "00:06:66:7d:7f:15";
    // The thread that does all the work
    BluetoothThread btt;
    // Handler for writing messages to the Bluetooth connection
    Handler writeHandler;
    private boolean pickupBool;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG,"Launch successful");
        final Button bConnect = (Button) findViewById(R.id.buttonC);
        bConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectToBluetooth();
            }
        });
        final Button bDisconnect = (Button) findViewById(R.id.buttonD);
        bDisconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectFromBluetooth();
            }
        });
        final Button b1 = (Button) findViewById(R.id.button1);
        b1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendText("IDLE");
            }
        });
        final Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendText("RING");
            }
        });final Button b3 = (Button) findViewById(R.id.button3);
        b3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendText("OFFH");
            }
        });
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            bluetoothChecker(context, intent, action);
            checkCallState(context, intent, action);
        }
    };

    private void bluetoothChecker(Context context, Intent intent, String action){
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device connected, device ID is: " + device.getName());
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device disconnected, device ID is: " + device.getName());
            }

    }

    public void connectToBluetooth() {
        Log.v(TAG, "Attempting to connect to bluetooth.");
        // Only one thread at a time
        if (btt != null) {
            Log.w(TAG, "Already connected!");
            return;
        }

        // Initialize the Bluetooth thread, passing in a MAC address
        // and a Handler that will receive incoming messages
        btt = new BluetoothThread(address, new Handler() {

            @Override
            public void handleMessage(Message message) {
                String s = (String) message.obj;
                if (s.equals("CONNECTED")) {
                    TextView tv = (TextView) findViewById(R.id.textView3);
                    tv.setText("Connected.");
                } else if (s.equals("DISCONNECTED")) {
                    TextView tv = (TextView) findViewById(R.id.textView3);
                    tv.setText("Disconnected.");
                } else if (s.equals("CONNECTION FAILED")) {
                    TextView tv = (TextView) findViewById(R.id.textView3);
                    tv.setText("Connection failed!");
                } else {
                    TextView tv = (TextView) findViewById(R.id.textView3);
                    tv.setText(s);
                    if(s.contains("PICKUP")){
                        pickupBool = true;
                        Log.v(TAG, "YOU SPEAK DA TRUE TRUE");
                        try {
                            Process proc = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(proc.getOutputStream());

                            os.writeBytes("input keyevent 5\n");
                            os.flush();

                            os.writeBytes("exit\n");
                            os.flush();

                            if (proc.waitFor() == 255) {
                                // TODO handle being declined root access
                                // 255 is the standard code for being declined root for SU
                            }

                        } catch (IOException e) {
                            // TODO handle I/O going wrong
                            // this probably means that the device isn't rooted
                        } catch (InterruptedException e) {
                            // don't swallow interruptions
                            Thread.currentThread().interrupt();
                        }


                    }
                    else{
                        pickupBool = false;
                        Log.v(TAG, "WHY THE FUCK YOU LYING");
                    }
                }
            }
        });

        // Get the handler that is used to send messages
        writeHandler = btt.getWriteHandler();

        // Run the thread
        btt.start();
        Log.v("TAG", "Connection successful");
    }

    /**
     * Kill the Bluetooth thread.
     */
    public void disconnectFromBluetooth() {
        Log.v(TAG, "Attempting to disconnect from bluetooth");

        if(btt != null) {
            btt.interrupt();
            btt = null;
            Log.v(TAG, "Disconnection successful");
        }
    }

    /**
     * Send a message using the Bluetooth thread's write handler.
     */
    public void sendText(String string) {
        Log.v(TAG, "Attempting to send text.");
        String data = string;
        Message msg = Message.obtain();
        msg.obj = data;
        writeHandler.sendMessage(msg);
        Log.v(TAG, "Sending text was successful");
    }

    public void checkCallState(Context context, Intent intent, String action) {
        if(btt != null){
        if(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)){
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.v(TAG, "Call state changed.");
            //Checker om telefonen ringer
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                Toast.makeText(context, "Incoming Call State", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "INCOMING CALL");
                sendText("RING");
                sendText("RING");
                if(pickupBool){

                }
            }
            //Checker om opkaldet blev taget
            if ((state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))) {
                Toast.makeText(context, "Call Received State", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "CALL RECEIVED");
                sendText("OFFH");
                sendText("OFFH");

            }
            //Checker om telefon, hverken ringer eller er i et opkald
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Toast.makeText(context, "Call Idle State", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "CALL ENDED");
                connectToBluetooth();
                sendText("IDLE");
                sendText("IDLE");
            }}}
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromBluetooth();
        unregisterReceiver(mReceiver);
    }
}