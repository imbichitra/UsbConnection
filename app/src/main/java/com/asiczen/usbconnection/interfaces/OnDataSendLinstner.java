package com.asiczen.usbconnection.interfaces;

public interface OnDataSendLinstner {
    void sendData(byte[] data,int port,DataAvailableLinstner dataAvailableLinstner);
}
