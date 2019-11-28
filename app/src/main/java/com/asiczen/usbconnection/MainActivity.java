package com.asiczen.usbconnection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.usbconnection.Utils.Util;
import com.asiczen.usbconnection.interfaces.DataAvailableLinstner;
import com.asiczen.usbconnection.interfaces.OnDataSendLinstner;
import com.asiczen.usbconnection.services.UsbService;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnDataSendLinstner {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private MyHandler mHandler;
    private UsbService usbService;
    private boolean isBind = false;
    private UsbManager usbManager;

    private TextView displaye1;

    private List<UsbSerialPort> ports;
    private Util util;
    private static DataAvailableLinstner mDataAvailableLinstner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displaye1 = findViewById(R.id.display1);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mHandler = new MyHandler(this);

        util = Util.getContext();
        util.setDataSendLinstner(this);
        bindService();
        registerReceiver();
    }

    public void sendData(View view) {
        String data = "HEALTH\n";
        sendData(data.getBytes(),0, new DataAvailableLinstner() {
            @Override
            public void onDataAvailable(String data, int port) {
                Log.d(TAG, "onDataAvailable: ");
               displaye1.append(data+" "+port);
            }
        });
    }

    public void go(View view) {
        Intent i = new Intent(this,SecondActivity.class);
        startActivity(i);
    }


    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        MyHandler(MainActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:

                    String buffer = (String) msg.obj;
                    //Log.d(TAG, "handleMessage: "+buffer);
                    if(msg.arg1 == 0){
                        if (mDataAvailableLinstner != null)
                            mDataAvailableLinstner.onDataAvailable(buffer,0);
                    }else if(msg.arg1 == 1){
                        if (mDataAvailableLinstner != null)
                            mDataAvailableLinstner.onDataAvailable(buffer,1);
                    }

                    break;
            }
        }
    }
    private BroadcastReceiver usbBroadCastReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case INTENT_ACTION_GRANT_USB:
                    if (usbService != null) {
                        boolean b = usbService.openPorts();
                        if (!b){
                            Toast.makeText(context, "Unable to open port", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    askPermission();
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    if (usbService != null)
                        usbService.closePorts();
                    break;
            }
        }
    };
    private void askPermission() {
        try {
            ports = usbService.findDevice();
            UsbDevice device = ports.get(0).getDriver().getDevice();
            if (!usbManager.hasPermission(device)) {
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                usbManager.requestPermission(device, usbPermissionIntent);
            }else {
                boolean b = usbService.openPorts();
                if (!b){
                    Toast.makeText(this, "Unable to open port", Toast.LENGTH_SHORT).show();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            usbService = ((UsbService.LocalBinder)iBinder).getService();
            usbService.setHandler(mHandler);
            isBind = true;
            askPermission();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBind =false;
        }
    };
    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_ACTION_GRANT_USB);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbBroadCastReciver,intentFilter);
    }

    private void bindService(){
        Intent intent = new Intent(this, UsbService.class);
        bindService(intent,connection,Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (usbService != null) {
            usbService.closePorts();
            unbindService(connection);
            usbService.stopSelf();
            usbService = null;
        }
        unregisterReceiver(usbBroadCastReciver);
        Runtime.getRuntime().gc();
        super.onDestroy();
    }

    @Override
    public void sendData(byte[] data,int port, DataAvailableLinstner dataAvailableLinstner) {
        mDataAvailableLinstner = dataAvailableLinstner;
        if (usbService != null)
            usbService.writeData(data,port);
    }
}
