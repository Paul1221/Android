package com.example.licienta;

import androidx.annotation.ContentView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BL = 0;
    static int fillColor = R.color.grey;
    static int selectedPixel;
    static int linie;
    static int coloana;
    String set_file_name;
    BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket mmSocket;
    public static Handler handler;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;


    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                       // handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
                Log.e("Status", "Uploaded");


            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work for different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e("TAG", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
             //   handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
//                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e("TAG", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the client socket", e);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BL);
        }


        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if(device.getName().equals("SRS-XB12")){
                        createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceHardwareAddress);
                        createConnectThread.start();
                }
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.grey));

        final Button choose_red = findViewById(R.id.RED);
        choose_red.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
            fillColor = R.color.red;

        });
        final Button choose_orange = findViewById(R.id.ORANGE);
        choose_orange.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.orange));
            fillColor = R.color.orange;

        });
        final Button choose_green = findViewById(R.id.GREEN);
        choose_green.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.green));
            fillColor = R.color.green;

        });
        final Button choose_blank = findViewById(R.id.GREY);
        choose_blank.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.grey));
            fillColor = R.color.grey;

        });
        final Button choose_blue = findViewById(R.id.BLUE);
        choose_blue.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.blue));
            fillColor = R.color.blue;
        });
        final Button choose_yellow = findViewById(R.id.YELLOW);
        choose_yellow.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.yellow));
            fillColor = R.color.yellow;

        });
        final Button choose_pink = findViewById(R.id.PINK);
        choose_pink.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.pink));
            fillColor = R.color.pink;

        });
        final Button choose_cyan = findViewById(R.id.CYAN);
        choose_cyan.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.cyan));
            fillColor = R.color.cyan;

        });
        final Button choose_violet = findViewById(R.id.VIOLET);
        choose_violet.setOnClickListener(v -> {
            findViewById(R.id.selected_color).setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.violet));
            fillColor = R.color.violet;

        });


        Class<?> clz = com.example.licienta.R.id.class;
        for ( linie = 0; linie < 8; linie++) {
            for ( coloana = 0; coloana < 8; coloana++)
                try {
                    selectedPixel = (int) clz.getField("pixel" + String.valueOf(linie) + String.valueOf(coloana)).get(null);
                    final Button aux = findViewById(selectedPixel);
                    aux.setOnClickListener(v -> {
                        v.setBackgroundColor(getResources().getColor(fillColor));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

        }


        final Button save_to_preset = findViewById(R.id.save_to_preset);
        save_to_preset.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Title");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    set_file_name = input.getText().toString();

                    File path = getFilesDir();

                    File file = new File(path, set_file_name+".txt");

                    try {
                        FileWriter writer = new FileWriter(file);

                        for ( linie = 0; linie < 8; linie++) {
                            for ( coloana = 0; coloana < 8; coloana++)
                                try {
                                    selectedPixel = (int) clz.getField("pixel" + String.valueOf(linie) + String.valueOf(coloana)).get(null);
                                    int color = ((ColorDrawable)findViewById(selectedPixel).getBackground()).getColor();
                                    writer.append(String.valueOf(color)).append(" ");

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();




        });

        final Button upload = findViewById((R.id.upload));
        upload.setOnClickListener(v -> {
            if(connectedThread != null) {
                String to_be_uploaded = "";

                for (linie = 0; linie < 8; linie++) {
                    for (coloana = 0; coloana < 8; coloana++)
                        try {
                            selectedPixel = (int) clz.getField("pixel" + String.valueOf(linie) + String.valueOf(coloana)).get(null);
                            int color = ((ColorDrawable) findViewById(selectedPixel).getBackground()).getColor();
                            if(color == R.color.red){
                                to_be_uploaded.concat("1");
                            }
                            if(color == R.color.orange){
                                to_be_uploaded.concat("2");
                            }
                            if(color == R.color.yellow){
                                to_be_uploaded.concat("3");
                            }
                            if(color == R.color.green){
                                to_be_uploaded.concat("4");
                            }
                            if(color == R.color.cyan){
                                to_be_uploaded.concat("5");
                            }
                            if(color == R.color.blue){
                                to_be_uploaded.concat("6");
                            }
                            if(color == R.color.violet){
                                to_be_uploaded.concat("7");
                            }
                            if(color == R.color.pink){
                                to_be_uploaded.concat("8");
                            }
                            if(color == R.color.grey){
                                to_be_uploaded.concat("0");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
                connectedThread.write(to_be_uploaded);

            }
            else
                Toast.makeText(this, "Please connect to HC-05", Toast.LENGTH_SHORT).show();
        });

        final Button load_preset = findViewById(R.id.load_from_preset);
        load_preset.setOnClickListener(v->{


            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.load_preset_alert);

            LinearLayout scrollView = null;
            File path = getFilesDir();
            Button auxButton = null;
            TableRow auxRow = null;
            if(path != null) {
                for (File file : path.listFiles()) {
                    if (file.getName().contains(".txt")) {

                        scrollView = (LinearLayout) dialog.findViewById(R.id.presetList);

                        auxButton = new Button(this);
                        auxButton.setText(file.getName());
                        auxButton.setTextColor(getResources().getColor(R.color.black));
                        auxButton.setTextSize(10);
                        auxButton.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                        auxButton.setOnClickListener(l->{
                            File preset = new File(path, ((Button) l).getText().toString());
                            Log.d("DA",preset.getName());

                            StringBuilder text = new StringBuilder();


                            BufferedReader br = null;
                            try {
                                br = new BufferedReader(new FileReader(preset));
                                String line;

                                while ((line = br.readLine()) != null) {
                                    text.append(line);
                                    text.append('\n');
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                br.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            int it = 0;
                            String[] sa = text.toString().split(" ");
                            for ( linie = 0; linie < 8; linie++) {
                                for ( coloana = 0; coloana < 8; coloana++) {
                                    try {
                                        selectedPixel = (int) clz.getField("pixel" + String.valueOf(linie) + String.valueOf(coloana)).get(null);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (NoSuchFieldException e) {
                                        e.printStackTrace();
                                    }
                                    findViewById(selectedPixel).setBackgroundColor(Integer.parseInt(sa[it]));
                                    it++;
                                }

                            }

                            dialog.cancel();

                        });

                        scrollView.addView(auxButton);
                        dialog.show();


                    }
                }
            }

        });

    }

//    @Override
//    public void onBackPressed() {
//        // Terminate Bluetooth Connection and close app
//        if (createConnectThread != null){
//            createConnectThread.cancel();
//        }
//        Intent a = new Intent(Intent.ACTION_MAIN);
//        a.addCategory(Intent.CATEGORY_HOME);
//        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(a);
//    }

    private Context getActivity() {
        return this;
    }


}