package com.asiczen.usbconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.asiczen.usbconnection.Utils.Util;
import com.asiczen.usbconnection.interfaces.DataAvailableLinstner;
import com.asiczen.usbconnection.interfaces.OnDataSendLinstner;

public class SecondActivity extends AppCompatActivity {

    private Util util;
    private OnDataSendLinstner linstner;
    private TextView display1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        util = Util.getContext();
        linstner = util.getDataSendLinstner();
        display1 = findViewById(R.id.display1);
    }

    public void sendData(View view) {
        if (linstner !=null )
        linstner.sendData("PUB_KEY_8\n".getBytes(),1, new DataAvailableLinstner() {
            @Override
            public void onDataAvailable(String data, int port) {
                display1.append(data+" "+port);
            }
        });
    }
}
