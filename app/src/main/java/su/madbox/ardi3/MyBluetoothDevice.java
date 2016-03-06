package su.madbox.ardi3;

import android.bluetooth.BluetoothDevice;

/**
 * Created by madbox on 01.03.16.
 */
public class MyBluetoothDevice {
    private BluetoothDevice mBluetoothDevice;

    public MyBluetoothDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public String toString() {
        return mBluetoothDevice.getName() +
                " - " + mBluetoothDevice.getAddress() +
                " - " + mBluetoothDevice.getBluetoothClass();
    }
}
