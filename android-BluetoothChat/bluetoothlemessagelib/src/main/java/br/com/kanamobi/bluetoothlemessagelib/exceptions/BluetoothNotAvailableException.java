package br.com.kanamobi.bluetoothlemessagelib.exceptions;

public class BluetoothNotAvailableException extends Exception {

    public BluetoothNotAvailableException() {
        super("BluetoothNotAvailableException: Bluetooth is not available");
    }
}
