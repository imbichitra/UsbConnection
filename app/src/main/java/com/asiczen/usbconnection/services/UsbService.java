package com.asiczen.usbconnection.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbService extends Service {
    public static final String TAG = UsbService.class.getSimpleName();
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    private static final int BAUD_RATE = 115200;
    private Handler mHandler;
    private UsbManager usbManager;

    private List<UsbSerialDriver> availableDrivers;
    private List<UsbSerialPort> mEntries = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final ExecutorService mExecutor1 = Executors.newCachedThreadPool();
    private UsbSerialPort port,port1;

    private SerialInputOutputManager mSerialIoManager;
    private SerialInputOutputManager mSerialIoManager1;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    try {
                        String mSata = new String(data, "UTF-8");
                        if (mHandler != null)
                            mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT,0,0, mSata).sendToTarget();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            };

    private final SerialInputOutputManager.Listener mListener1 =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    try {
                        String mSata = new String(data, "UTF-8");
                        if (mHandler != null)
                            mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT,1,0, mSata).sendToTarget();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            };

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public UsbService getService(){
            return UsbService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public List<UsbSerialPort> findDevice(){
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (availableDrivers != null)
            availableDrivers.clear();
        if (mEntries != null)
            mEntries.clear();
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        Log.d(TAG, "findDevice: "+availableDrivers);
        for (final UsbSerialDriver driver : availableDrivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            Log.d(TAG, String.format("+ %s: %s port%s",
                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            mEntries.addAll(ports);
        }

        return mEntries;
    }

    public boolean openPorts(){
        port = mEntries.get(0);
        UsbDeviceConnection connection = usbManager.openDevice(port.getDriver().getDevice());

        port1 = mEntries.get(1);
        UsbDeviceConnection connection1 = usbManager.openDevice(port1.getDriver().getDevice());
        try {
            port.open(connection);
            port.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            port1.open(connection1);
            port1.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            try {
                port.close();
                port1.close();
            } catch (IOException e2) {
                // Ignore.
            }
            port = null;
            port1 = null;
            return false;
        }
        onDeviceStateChange();
        return true;
    }
    public void closePorts(){
        stopIoManager();
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                // Ignore.
            }
            port = null;
        }

        if (port1 != null) {
            try {
                port1.close();
            } catch (IOException e) {
                // Ignore.
            }
            port1 = null;
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }

        if (mSerialIoManager1 != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager1.stop();
            mSerialIoManager1 = null;
        }
    }

    private void startIoManager() {
        if (port != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(port, mListener);
            mExecutor.submit(mSerialIoManager);
        }

        if (port1 != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager1 = new SerialInputOutputManager(port1, mListener1);
            mExecutor1.submit(mSerialIoManager1);
        }
    }

    public void writeData(byte[] data,int mport){
        try {
            if (mport == 0)
                port.write(data,2000);
            else
                port1.write(data,2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
