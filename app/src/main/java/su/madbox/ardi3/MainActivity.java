package su.madbox.ardi3;

import android.app.ActivityOptions;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "su.madbox.Ardi3.MESSAGE";
    public static final String TAG = "Bluetooth";
    private final static int REQUEST_ENABLE_BT = 4324;
    // UUID for Serial port service
    // http://www.bluecove.org/bluecove/apidocs/javax/bluetooth/UUID.html
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //Экземпляры классов наших кнопок.
    Button getDistanceButton;
    ArrayList<MyBluetoothDevice> mListItemsPaired = new ArrayList<MyBluetoothDevice>();
    ArrayAdapter<MyBluetoothDevice> mArrayAdapterPaired;
    ArrayList<MyBluetoothDevice> mListItemsFound = new ArrayList<MyBluetoothDevice>();
    ArrayAdapter<MyBluetoothDevice> mArrayAdapterFound;
    private final BroadcastReceiver blueToothDiscoveryReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive Device found: " + device.getName() + " - " + device.getAddress());

                addDiscoveredDevice(mArrayAdapterFound, device);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                setBluetoothStateMarkerText(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                setBluetoothDiscoveryMarkerText(true);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setBluetoothDiscoveryMarkerText(false);
            }
        }
    };
    private Set<BluetoothDevice> pairedDevices;
    private Set<BluetoothDevice> foundDevices;
    private BluetoothAdapter mBluetoothAdapter;
    //Сокет, с помощью которого мы будем отправлять данные на Arduino
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        Transition exitTrans = new Slide(Gravity.RIGHT);
        exitTrans.excludeTarget(toolbar, true);
        getWindow().setExitTransition(exitTrans);

        Transition reenterTrans = new Slide(Gravity.RIGHT);
        reenterTrans.excludeTarget(toolbar, true);
        getWindow().setReenterTransition(reenterTrans);


        setSupportActionBar(toolbar);

//        View.OnClickListener getDistanceOnClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("onClick", v.toString());
//
//                if (mSocket != null) {
//                    //Пытаемся послать данные
//                    try {
//                        //Получаем выходной поток для передачи данных
//                        OutputStream outStream = mSocket.getOutputStream();
//                        final InputStream inStream = mSocket.getInputStream();
//
//                        //В зависимости от того, какая кнопка была нажата,
//                        //изменяем данные для посылки
//                        if (v == getDistanceButton) {
//                            //Пишем данные в выходной поток
//                            outStream.write("get_dist\n".getBytes(Charset.forName("US-ASCII")));
//                            Handler handler = new Handler();
//                            handler.postDelayed(new Runnable() {
//                                public void run() {
//                                    try {
//                                        byte[] buffer = new byte[256];  // buffer store for the stream
//                                        int bytes; // bytes returned from read()
//
//                                        Log.d(TAG, "available: " + Integer.toString(inStream.available()));
//                                        bytes = inStream.read(buffer);
//                                        Log.d(TAG, "read bytes: " + Integer.toString(bytes));
//                                        Toast.makeText(getApplicationContext(), new String(buffer, "US-ASCII").trim(), Toast.LENGTH_LONG).show();
//                                    } catch (IOException e) {
//                                        //Если есть ошибки, выводим их в лог
//                                        Log.d("onClick", e.getMessage());
//                                    }
//                                }
//                            }, 1000);
//                        }
//
//                    } catch (IOException e) {
//                        //Если есть ошибки, выводим их в лог
//                        Log.d("onClick", e.getMessage());
//                    }
//                } else {
//                    Snackbar.make(v, "Should be connected to some device first", Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
//                }
//
//
//
//            }
//        };

        //"Соединям" вид кнопки в окне приложения с реализацией
        // getDistanceButton = (Button) findViewById(R.id.getDistance);

        //Добавлем "слушатель нажатий" к кнопке
        //getDistanceButton.setOnClickListener(getDistanceOnClickListener);


        AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothConnect((MyBluetoothDevice) parent.getItemAtPosition(position));
            }
        };

        ListView listViewPaired = (ListView) findViewById(R.id.list_paired_bluetooth_devices);
        mArrayAdapterPaired = new ArrayAdapter<MyBluetoothDevice>(this,
                android.R.layout.simple_list_item_1,
                mListItemsPaired);
        listViewPaired.setAdapter(mArrayAdapterPaired);
        listViewPaired.setOnItemClickListener(mOnItemClickListener);

        ListView listViewFound = (ListView) findViewById(R.id.list_found_bluetooth_devices);
        mArrayAdapterFound = new ArrayAdapter<MyBluetoothDevice>(this,
                android.R.layout.simple_list_item_1,
                mListItemsFound);
        listViewFound.setAdapter(mArrayAdapterFound);
        listViewFound.setOnItemClickListener(mOnItemClickListener);

        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);

        IntentFilter filter = new IntentFilter(); //(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(blueToothDiscoveryReciever, filter);

        bluetoothEnableAndDiscover(this.getCurrentFocus());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueToothDiscoveryReciever);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "fired! req:" + requestCode + " res: " + resultCode);
        //Выводим сообщение об успешном подключении bluetooth
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_LONG).show();
                bluetoothDiscover(this.getCurrentFocus());
            } else {
                Toast.makeText(getApplicationContext(), "Failed to enable bluetooth", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void addDiscoveredDevice(ArrayAdapter<MyBluetoothDevice> arrayAdapter, BluetoothDevice device) {
        boolean found = false;
        for (int i = 0; i<arrayAdapter.getCount(); i++) {
            if (Objects.equals(arrayAdapter.getItem(i).getBluetoothDevice().getAddress(),
                               device.getAddress())) return;
        }

        arrayAdapter.add(new MyBluetoothDevice(device));
    }

    public void clearDiscoveredDevicesList() {
        mArrayAdapterPaired.clear();
        mArrayAdapterFound.clear();
    }

    /**
     * Called when the user clicks the Send button
     */
//    public void sendMessage(View view) {
//        Intent intent = new Intent(this, DisplayMessageActivity.class);
//        EditText editText = (EditText) findViewById(R.id.edit_message);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
//    }


    public void makeBluetoothConnection(View view) {
        Intent intent = new Intent(this, BluetoothConnectionActivity.class);
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }

    public void bluetoothEnableAndDiscover(View view) {
        //Мы хотим использовать тот bluetooth-адаптер, который задается по умолчанию
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) Log.d(TAG, "mBluetoothAdapter == null");
        else Log.d(TAG, "mBluetoothAdapter != null");

        setBluetoothStateMarkerText(mBluetoothAdapter.getState());

        // Если адаптер включен - начнем поиск устройств, иначе - запустим включение адаптера
        // (поиск запстится в onActivityResult, если успешно включен bluetooth).
        if (mBluetoothAdapter.isEnabled()) {
            bluetoothDiscover(this.getCurrentFocus());
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void bluetoothDiscover(View view) {
        clearDiscoveredDevicesList();

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                addDiscoveredDevice(mArrayAdapterPaired, device);
                Log.d(TAG, "Paired device: " + device.getName() + " - " + device.getAddress());
            }
        }

        Log.d(TAG, "startDiscovery()");
        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }

    public void bluetoothConnect(MyBluetoothDevice device) {
        Log.d(TAG, "Starting connection process");
        mBluetoothAdapter.cancelDiscovery();
        mDevice = mBluetoothAdapter.getRemoteDevice(device.getBluetoothDevice().getAddress());

        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d(TAG, "createRfcommSocketToServiceRecord, IOException " + e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, "createRfcommSocketToServiceRecord, SecurityException " + e.getMessage());
        }

        mSocket = tmp;

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d(TAG, "isConnected: " + mSocket.isConnected());
            mSocket.connect();
            Toast.makeText(getApplicationContext(), "Connection established", Toast.LENGTH_LONG).show();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.d(TAG, "connect, IOException " + connectException.getMessage());
            try {
                mSocket.close();
            } catch (IOException closeException) {
                Log.d(TAG, "close, IOException " + closeException.getMessage());
            }

            Toast.makeText(getApplicationContext(), "Unable to connect", Toast.LENGTH_LONG).show();

            return;
        }

        // Do work to manage the connection (in a separate thread)

    }

    public void setBluetoothStateMarkerText(int state) {
        TextView text = (TextView) findViewById(R.id.bluetooth_value);
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                text.setHint(R.string.title_bluetooth_state_off); break;
            case BluetoothAdapter.STATE_ON:
                text.setHint(R.string.title_bluetooth_state_on); break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                text.setHint(R.string.title_bluetooth_state_turning_off); break;
            case BluetoothAdapter.STATE_TURNING_ON:
                text.setHint(R.string.title_bluetooth_state_turning_on); break;
            default:
                text.setHint(R.string.title_bluetooth_state_unknown); break;
        }
    }

    public void setBluetoothDiscoveryMarkerText(boolean discoveryInProgress) {
        TextView text = (TextView) findViewById(R.id.bluetooth_discovery_value);
        if (discoveryInProgress) {
            text.setHint(R.string.title_bluetooth_discovery_in_progress);
        } else {
            text.setHint(R.string.title_bluetooth_discovery_disabled);
        }
    }
}
