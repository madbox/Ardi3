package su.madbox.ardi3;

import android.app.ActivityOptions;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.AttrRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.ViewDebug;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "su.madbox.Ardi3.MESSAGE";

    //Экземпляры классов наших кнопок.
    Button getDistanceButton;

    private final static int REQUEST_ENABLE_BT = 4324;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter mBluetoothAdapter;
    //Сокет, с помощью которого мы будем отправлять данные на Arduino
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;

    // UUID for Serial port service
    // http://www.bluecove.org/bluecove/apidocs/javax/bluetooth/UUID.html
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    ArrayList<MyBluetoothDevice> mListItems = new ArrayList<MyBluetoothDevice>();
    ArrayAdapter<MyBluetoothDevice> mArrayAdapter;

    private final BroadcastReceiver blueToothDiscoveryReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Bluetooth", "onReceive");

            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("Bluetooth", "Device found: " + device.getName() + " - " + device.getAddress());

                addDiscoveredDevice(device);
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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

        View.OnClickListener getDistanceOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick", v.toString());

                if (mSocket != null) {
                    //Пытаемся послать данные
                    try {
                        //Получаем выходной поток для передачи данных
                        OutputStream outStream = mSocket.getOutputStream();
                        final InputStream inStream = mSocket.getInputStream();

                        //В зависимости от того, какая кнопка была нажата,
                        //изменяем данные для посылки
                        if (v == getDistanceButton) {
                            //Пишем данные в выходной поток
                            outStream.write("get_dist\n".getBytes(Charset.forName("US-ASCII")));
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    try {
                                        byte[] buffer = new byte[256];  // buffer store for the stream
                                        int bytes; // bytes returned from read()

                                        Log.d("BLUETOOTH", "available: " + Integer.toString(inStream.available()));
                                        bytes = inStream.read(buffer);
                                        Log.d("BLUETOOTH", "read bytes: " + Integer.toString(bytes));
                                        Toast.makeText(getApplicationContext(), new String(buffer, "US-ASCII").trim(), Toast.LENGTH_LONG).show();
                                    } catch (IOException e) {
                                        //Если есть ошибки, выводим их в лог
                                        Log.d("onClick", e.getMessage());
                                    }
                                }
                            }, 1000);
                        }

                    } catch (IOException e) {
                        //Если есть ошибки, выводим их в лог
                        Log.d("onClick", e.getMessage());
                    }
                } else {
                    Snackbar.make(v, "Should be connected to some device first", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }



            }
        };

        //"Соединям" вид кнопки в окне приложения с реализацией
        getDistanceButton = (Button) findViewById(R.id.getDistance);

        //Добавлем "слушатель нажатий" к кнопке
        getDistanceButton.setOnClickListener(getDistanceOnClickListener);

        ListView listView = (ListView) findViewById(R.id.list_bluetooth_devices);
        mArrayAdapter = new ArrayAdapter<MyBluetoothDevice>(this,
                android.R.layout.simple_list_item_1,
                mListItems);
        listView.setAdapter(mArrayAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d("LISTVIEW", "itemClick: position = " + position + ", id = "
                        + id);
                bluetoothConnect((MyBluetoothDevice) parent.getItemAtPosition(position));
            }
        });

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.d("LISTVIEW", "itemSelect: position = " + position + ", id = "
                        + id);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("LISTVIEW", "itemSelect: nothing");
            }
        });

        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(blueToothDiscoveryReciever, filter);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
            if (resultCode == RESULT_OK)
                Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(), "Failed to enable bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    public void addDiscoveredDevice(BluetoothDevice device) {
        boolean found = false;
        for (MyBluetoothDevice myDevice : mListItems) {
            if (Objects.equals(myDevice.getBluetoothDevice().getAddress(),
                               device.getAddress())) return;
        }

        mListItems.add(new MyBluetoothDevice(device));
        mArrayAdapter.notifyDataSetChanged();
    }

    public void clearDiscoveredDevicesList() {
        mListItems.clear();
        mArrayAdapter.notifyDataSetChanged();
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

    public void bluetoothDiscover(View view) {
        //Мы хотим использовать тот bluetooth-адаптер, который задается по умолчанию
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) Log.d("Bluetooth", "mBluetoothAdapter == null");
        else Log.d("Bluetooth", "mBluetoothAdapter != null");

        //Включаем bluetooth. Если он уже включен, то ничего не произойдет
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        clearDiscoveredDevicesList();

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                addDiscoveredDevice(device);
                Log.d("Bluetooth", "Paired device: " + device.getName() + " - " + device.getAddress());
                // mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }


        mBluetoothAdapter.startDiscovery();
    }

    public void bluetoothConnect(MyBluetoothDevice device) {
        Log.d("BLUETOOTH", "Starting connection process");
        mBluetoothAdapter.cancelDiscovery();
        mDevice = mBluetoothAdapter.getRemoteDevice(device.getBluetoothDevice().getAddress());

        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("BLUETOOTH", "createRfcommSocketToServiceRecord, IOException " + e.getMessage());
        } catch (SecurityException e) {
            Log.d("BLUETOOTH", "createRfcommSocketToServiceRecord, SecurityException " + e.getMessage());
        }

        mSocket = tmp;

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("BLUETOOTH", "isConnected: " + mSocket.isConnected());
            mSocket.connect();
            Toast.makeText(getApplicationContext(), "Connection established", Toast.LENGTH_LONG).show();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.d("BLUETOOTH", "connect, IOException " + connectException.getMessage());
            try {
                mSocket.close();
            } catch (IOException closeException) {
                Log.d("BLUETOOTH", "close, IOException " + closeException.getMessage());
            }

            Toast.makeText(getApplicationContext(), "Unable to connect", Toast.LENGTH_LONG).show();

            return;
        }

        // Do work to manage the connection (in a separate thread)

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://su.madbox.ardi3/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://su.madbox.ardi3/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
