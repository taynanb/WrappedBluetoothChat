package br.com.kanamobi.wrappedbluetoothmessage;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import br.com.kanamobi.wrappedbluetoothmessage.exceptions.BluetoothNotAvailableException;

public class BluetoothMessageInstance {

    static BluetoothMessageService instance;

    public static BluetoothMessageService init(Context context) throws BluetoothNotAvailableException{

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            throw new BluetoothNotAvailableException();
        }

        if (instance == null) {
            instance = new BluetoothMessageService(context, bluetoothAdapter);
        }

        return instance;
    }

    public static BluetoothMessageService getInstance(){
        return instance;
    }

}
