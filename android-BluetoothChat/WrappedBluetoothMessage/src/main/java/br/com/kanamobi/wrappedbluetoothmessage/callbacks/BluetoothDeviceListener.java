package br.com.kanamobi.wrappedbluetoothmessage.callbacks;

public interface BluetoothDeviceListener {

    void onDeviceStateConnected(String deviceName);
    void onDeviceStateConnecting();
    void onDeviceStateNotConnected();
    void onDeviceStateDisconnected();
    void onDeviceStateConnectionFailed();

}
