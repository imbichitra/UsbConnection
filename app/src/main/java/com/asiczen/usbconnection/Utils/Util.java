package com.asiczen.usbconnection.Utils;

import com.asiczen.usbconnection.interfaces.OnDataSendLinstner;

public class Util {
    private static Util ourInstance = new Util();
    private OnDataSendLinstner dataSendLinstner;

    public OnDataSendLinstner getDataSendLinstner() {
        return dataSendLinstner;
    }

    public void setDataSendLinstner(OnDataSendLinstner dataSendLinstner) {
        this.dataSendLinstner = dataSendLinstner;
    }

    public static Util getContext(){
        return ourInstance;
    }
}
