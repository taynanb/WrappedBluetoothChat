package br.com.kanamobi.wrappedbluetoothmessage.callbacks;

public interface BluetoothMessageListener {

    void onMessageWrite(String message);
    void onMessageRead(String message);

}
