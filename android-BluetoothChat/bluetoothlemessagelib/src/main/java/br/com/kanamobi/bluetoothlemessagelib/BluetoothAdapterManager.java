/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.kanamobi.bluetoothlemessagelib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import br.com.kanamobi.bluetoothlemessagelib.callbacks.BluetoothAdapterListener;
import br.com.kanamobi.bluetoothlemessagelib.exceptions.BluetoothNotAvailableException;

public class BluetoothAdapterManager {

    private static final int REQUEST_ENABLE_BT = 3;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapterListener listener;

    private static BluetoothAdapterManager instance;

    public static BluetoothAdapterManager init(Context context)
            throws BluetoothNotAvailableException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            throw new BluetoothNotAvailableException();
        }

        if(instance == null){
            instance = new BluetoothAdapterManager(context, bluetoothAdapter);
        }

        return instance;
    }

    public static BluetoothAdapterManager getInstance() {
        return instance;
    }

    private BluetoothAdapterManager(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public void setBluetoothWrapperListener(BluetoothAdapterListener listener){
        this.listener = listener;
    }

    public boolean isBtEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public void requestEnableBt(){
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ((Activity)context).startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    if(listener != null)
                        listener.onBtEnabled();
                } else {
                    if(listener != null)
                        listener.onBtDisabled();
                }
        }
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    public void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivity(discoverableIntent);
        }
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return bluetoothAdapter.getRemoteDevice(address);
    }
}
