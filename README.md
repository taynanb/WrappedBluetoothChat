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
