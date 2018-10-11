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

package br.com.kanamobi.wrappedbluetoothmessage

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import br.com.kanamobi.wrappedbluetoothmessage.callbacks.BluetoothAdapterListener
import br.com.kanamobi.wrappedbluetoothmessage.callbacks.BluetoothDeviceListener
import br.com.kanamobi.wrappedbluetoothmessage.callbacks.BluetoothMessageListener
import br.com.kanamobi.wrappedbluetoothmessage.exceptions.BluetoothNotAvailableException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class BluetoothMessageService {

    private lateinit var context: Context
    private lateinit var adapter: BluetoothAdapter

    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    /**
     * Return the current connection state.
     */
    @get:Synchronized
    var state: Int = 0
        private set
    private var mNewState: Int = 0

    private var mBluetoothDeviceListener: BluetoothDeviceListener? = null
    private var mBluetoothMessageListener: BluetoothMessageListener? = null
    private var mBluetoothAdapterListener: BluetoothAdapterListener? = null

    var mConnectedDeviceName: String? = null

    private val mResultHandler = Handler()

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothMessageService.STATE_CONNECTED -> mBluetoothDeviceListener?.onDeviceStateConnected(mConnectedDeviceName)
                    BluetoothMessageService.STATE_CONNECTING -> mBluetoothDeviceListener?.onDeviceStateConnecting()
                    BluetoothMessageService.STATE_LISTEN, BluetoothMessageService.STATE_NONE -> mBluetoothDeviceListener?.onDeviceStateNotConnected()
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
                    mBluetoothMessageListener?.onMessageWrite(writeMessage)
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    mBluetoothMessageListener?.onMessageRead(readMessage)
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    mBluetoothDeviceListener?.onDeviceStateConnected(mConnectedDeviceName)
                }
                Constants.MESSAGE_DISCONNECTED -> mBluetoothDeviceListener?.onDeviceStateDisconnected()
                Constants.MESSAGE_CONNECTION_FAILED -> mBluetoothDeviceListener?.onDeviceStateConnectionFailed()
            }
        }
    }


    private constructor(context: Context, adapter: BluetoothAdapter) {
        this.context = context
        this.adapter = adapter
        Toast.makeText(context, "Constructor", Toast.LENGTH_SHORT).show()
    }


    /**
     * Constructor. Prepares a new BluetoothChat session.
     */
    init {
        state = STATE_NONE
        mNewState = state
    }

    fun setBluetoothDeviceListener(bluetoothDeviceListener: BluetoothDeviceListener) {
        this.mBluetoothDeviceListener = bluetoothDeviceListener
    }

    fun setBluetoothMessageListener(bluetoothMessageListener: BluetoothMessageListener) {
        this.mBluetoothMessageListener = bluetoothMessageListener
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    @Synchronized
    private fun updateUserInterfaceTitle() {
        state = state
        Log.d(TAG, "updateUserInterfaceTitle() $mNewState -> $state")
        mNewState = state

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget()
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread?.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread?.start()
        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread?.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.start()
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type:$socketType")

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler.sendMessage(msg)
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }
        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(message: String) {
        val out = message.toByteArray()
        // Create temporary object
        var r: ConnectedThread? = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r?.write(out)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTION_FAILED)
        mHandler.sendMessage(msg)

        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothMessageService.start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DISCONNECTED)
        mHandler.sendMessage(msg)

        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothMessageService.start()
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread(secure: Boolean) : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Create a new listening server socket
            try {
                tmp = if (secure) {
                    adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE)
                } else {
                    adapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }

            mmServerSocket = tmp
            this@BluetoothMessageService.state = STATE_LISTEN
        }

        override fun run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this)
            name = "AcceptThread$mSocketType"

            var socket: BluetoothSocket?

            // Listen to the server socket if we're not connected
            while (this@BluetoothMessageService.state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized(this@BluetoothMessageService) {
                        when (this@BluetoothMessageService.state) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.remoteDevice,
                                        mSocketType)
                            STATE_NONE, STATE_CONNECTED ->
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }

                            else -> {
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: $mSocketType")

        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }

        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = if (secure) {
                    mmDevice.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE)
                } else {
                    mmDevice.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }

            mmSocket = tmp
            this@BluetoothMessageService.state = STATE_CONNECTING
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket?.connect()
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2)
                }

                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothMessageService) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket!!, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, socketType: String) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            this@BluetoothMessageService.state = STATE_CONNECTED
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (this@BluetoothMessageService.state == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected ->", e)
                    connectionLost()
                    break
                }

            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }

        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }

        }
    }

    fun setBluetoothAdapterListener(listener: BluetoothAdapterListener) {
        this.mBluetoothAdapterListener = listener
    }

    fun isBtEnabled(): Boolean {
        return adapter.isEnabled
    }

    fun requestEnableBt() {
        Toast.makeText(context, "requestEnableBt", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, BluetoothMessageActivity::class.java)
        intent.putExtra(BluetoothMessageActivity.ARG_ACTION,
                BluetoothMessageActivity.ACTION_ENABLE_BLUETOOTH)
        (context as Activity).startActivity(intent)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mResultHandler.postDelayed({
            Log.d(TAG, "onActivityResult() requestCode: $requestCode / resultCode $resultCode")

            when (requestCode) {
                REQUEST_CONNECT_DEVICE_SECURE -> {
                    // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK && data != null)
                        connectDevice(data, true)
                }
                REQUEST_CONNECT_DEVICE_INSECURE -> {
                    // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        connectDevice(data, false)
                    }
                }

                REQUEST_ENABLE_BT ->
                    // When the request to enable Bluetooth returns
                    if (resultCode == Activity.RESULT_OK) {
                        // Bluetooth is now enabled, so set up a chat session
                        mBluetoothAdapterListener?.onBtEnabled()
                    } else {
                        mBluetoothAdapterListener?.onBtDisabled()
                    }
            }
        }, 500)
    }

    /**
     * Establish connection with other device
     *
     * @param data   An [Intent] with [DeviceListActivity.EXTRA_DEVICE_ADDRESS] extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private fun connectDevice(data: Intent, secure: Boolean) {
        // Get the device MAC address
        val address = data.extras!!
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)

        Log.d(TAG, "Connect to Address " + address!!)

        // Get the BluetoothDevice object
        val device = adapter.getRemoteDevice(address)
        // Attempt to connect to the device
        connect(device, secure)
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    fun ensureDiscoverable() {
        if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            context.startActivity(discoverableIntent)
        }
    }

    /**
     * Starts new BT connection
     * @param secure boolean that define if new connection will be secure or not
     */
    fun startBtConnection(secure: Boolean) {
        Toast.makeText(context, "startBtConnection $secure", Toast.LENGTH_SHORT).show()
        val serverIntent = Intent(context, BluetoothMessageActivity::class.java)
        serverIntent.putExtra(BluetoothMessageActivity.ARG_ACTION,
                BluetoothMessageActivity.ACTION_CONNECT_DEVICE)
        serverIntent.putExtra(BluetoothMessageActivity.ARG_CONNECTION_SECURE, secure)
        context.startActivity(serverIntent)
    }

    companion object {

        var instance: BluetoothMessageService? = null

        // Debugging
        private const val TAG = "BluetoothChatService"

        // Intent request codes
        const val REQUEST_CONNECT_DEVICE_SECURE = 1
        const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        const val REQUEST_ENABLE_BT = 3

        // Name for the SDP record when creating server socket
        private const val NAME_SECURE = "BluetoothChatSecure"
        private const val NAME_INSECURE = "BluetoothChatInsecure"

        // Unique UUID for this application
        private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        // Constants that indicate the current connection state
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_LISTEN = 1     // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3  // now connected to a remote device

        @Throws(BluetoothNotAvailableException::class)
        fun init(context: Context): BluetoothMessageService {

            Toast.makeText(context, "init", Toast.LENGTH_SHORT).show()

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if(bluetoothAdapter == null){
                Toast.makeText(context, "Adapter not Present", Toast.LENGTH_SHORT).show()
                throw BluetoothNotAvailableException()
            }

            if (instance == null) {
                instance = BluetoothMessageService(context, bluetoothAdapter)
            }

            return instance!!
        }
    }

}
