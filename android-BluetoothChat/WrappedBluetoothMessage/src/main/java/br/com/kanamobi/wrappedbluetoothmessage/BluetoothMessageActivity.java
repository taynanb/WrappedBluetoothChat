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

package br.com.kanamobi.wrappedbluetoothmessage;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class BluetoothMessageActivity extends Activity {

    private static final String TAG = "BluetoothMessage";

    public static final String ARG_ACTION = "br.com.kanamobi.wrappedbluetoothmessage.ARG_ACTION";
    public static final String ARG_CONNECTION_SECURE = "br.com.kanamobi.wrappedbluetoothmessage.ARG_CONNECTION_SECURE";

    public static final int ACTION_ENABLE_BLUETOOTH = 42;
    public static final int ACTION_CONNECT_DEVICE = 84;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bluetooth_message);

        if(getIntent() == null || !getIntent().hasExtra(ARG_ACTION)){
            finish();
        }

        startAction();

    }

    private void startAction() {
        int action = getIntent().getIntExtra(ARG_ACTION, 0);

        switch (action){
            case ACTION_ENABLE_BLUETOOTH:
                enableBluetooth();
                break;
            case ACTION_CONNECT_DEVICE:
                startBtConnection();
                break;
            default:
                finish();
                return;
        }
    }

    private void enableBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, BluetoothMessageService.REQUEST_ENABLE_BT);
    }

    private void startBtConnection() {
        boolean secure = getIntent().getBooleanExtra(ARG_CONNECTION_SECURE, true);

        int requestCode = secure ? BluetoothMessageService.REQUEST_CONNECT_DEVICE_SECURE
                : BluetoothMessageService.REQUEST_CONNECT_DEVICE_INSECURE;

        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        finish();

        Log.d(TAG, "onActivityResult() requestCode: $requestCode / resultCode $resultCode");
        BluetoothMessageInstance.getInstance().onActivityResult(requestCode, resultCode, data);

    }
}
