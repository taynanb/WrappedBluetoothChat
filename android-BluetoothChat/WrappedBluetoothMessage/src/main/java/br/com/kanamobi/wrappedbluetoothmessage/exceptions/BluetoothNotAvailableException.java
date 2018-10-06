package br.com.kanamobi.wrappedbluetoothmessage.exceptions;

public class BluetoothNotAvailableException extends Exception {

    public BluetoothNotAvailableException() {
        super("BluetoothNotAvailableException: Bluetooth is not available");
    }
}
