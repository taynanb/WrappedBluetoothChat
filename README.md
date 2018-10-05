# WrappedBluetoothChat
Android-BluetoothChat sample wrapped to a library for purpose to simplify message exchange between devices over bluetooth. 

Usage
---------------
Declare into your class member object for Bluetooth and Message Services:
```
private BluetoothMessageService mMessageService;
private BluetoothAdapterManager mBluetoothAdapterManager;
```

Initialize BluetoothAdapterManager. The init() method will throw and BluetoothNotAvailableException if there's no Bluetooth present on device.
```
private void initBtAdapterWrapper() {
    FragmentActivity activity = getActivity();
    try {
        mBluetoothAdapterManager = BluetoothAdapterManager.init(activity);
    } catch (BluetoothNotAvailableException e) {
        e.printStackTrace();
        Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        activity.finish();
    }
}
```

Use BluetoothAdapterManager to check if bluetooth is enable. If is not, call requestEnableBt() to enable it. The method will call an Activity to list and pick available devices.
```
if (!mBluetoothAdapterManager.isBtEnabled()) {
    mBluetoothAdapterManager.requestEnableBt();
}
```

After user enable Blutooth, onActivityForResult() method will be called. Simply call the same method declared in BluetoothAdapterManager for handle result.
```
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    mBluetoothAdapterManager.onActivityResult(requestCode, resultCode, data);
}    
```

For listen to BluetoothAdapterManager callbacks, implement BluetoothAdapterListener on your class, override its methods and set the listener implementation into BluetoothAdapterManager.
```
public class BluetoothChatFragment extends Fragment
        implements BluetoothAdapterListener { // Implement the listener into your class
        
        ...
        
    @Override
    public void onResume() {
    super.onResume();

        if(mBluetoothAdapterManager != null){
            // Setup listener implementation into BluetoothAdapterManager 
            mBluetoothAdapterManager.setBluetoothAdapterListener(this);
        }
    }
    
    // Dont forget to remove listener when your screen will be remove
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapterManager != null) {
            mBluetoothAdapterManager.setBluetoothWrapperListener(null);
        }
    }
    
    // Override BluetoothAdapterListener methods
    @Override
    public void onBtEnabled() {
        // User did enable Bluetooth
        setupChat();
    }

    @Override
    public void onBtDisabled() {
        // User did not enable Bluetooth or an error occurred
        Toast.makeText(getActivity(), "BT not enabled",
                Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
        
}
```


After user selects a Device, onActivityForResult() method will be called with selected deviced mac address. Simply call the same method declared in BluetoothAdapterManager for handle result.
```
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    mBluetoothAdapterManager.onActivityResult(requestCode, resultCode, data);
}    
```

After user selects a Device, onActivityForResult() method will be called with selected deviced mac address. Simply call the same method declared in BluetoothAdapterManager for handle result.
```
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    mBluetoothAdapterManager.onActivityResult(requestCode, resultCode, data);
}    
```
