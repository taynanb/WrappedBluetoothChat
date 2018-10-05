package br.com.kanamobi.bluetoothlemessagelib.callbacks;

public interface BluetoothMessageListener {

    void onMessageWrite(String message);
    void onMessageRead(String message);

}
