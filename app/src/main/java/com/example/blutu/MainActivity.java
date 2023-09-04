package com.example.blutu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log; // Agregamos importación para usar Log
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private InputStream inputStream;
    private TextView receivedDataTextView;
    private StringBuilder data = new StringBuilder();
    private ScrollView dataScrollView; // Agregamos el ScrollView
    private TextView dateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedDataTextView = findViewById(R.id.receivedDataTextView);
        dataScrollView = findViewById(R.id.dataScrollView); // Enlazamos con el ScrollView
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        dateTextView = findViewById(R.id.dateTextView);

        // Obtener la fecha y hora actual y formatearla
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateTime = dateFormat.format(new Date());

        // Establecer la fecha actual en el TextView
        dateTextView.setText("Fecha:" + currentDateTime);

        if (bluetoothAdapter == null) {
            receivedDataTextView.setText("Bluetooth no disponible en este dispositivo.");
        } else {
            connectBluetooth();
        }
    }

    private void connectBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            receivedDataTextView.setText("Bluetooth está apagado. Enciéndelo e inténtalo nuevamente.");
            return;
        }

        bluetoothDevice = bluetoothAdapter.getRemoteDevice("00:22:04:00:26:4B"); // Reemplaza con la dirección MAC de tu módulo HC-06

        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                receivedDataTextView.setText("Permisos de Bluetooth no concedidos.");
                return;
            }

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            beginListenForData();
        } catch (IOException e) {
            receivedDataTextView.setText("Error al conectar con el dispositivo Bluetooth.");
        }
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; // Delimitador ASCII para una nueva línea

        Thread workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    String finalData = data.toString();
                                    handler.post(new Runnable() {
                                        public void run() {
                                            appendDataToTextView(finalData);
                                        }
                                    });
                                    data.delete(0, data.length()); // Limpiar los datos acumulados
                                } else {
                                    data.append((char) b);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        break;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void appendDataToTextView(String newData) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String hora = dateFormat.format(new Date());
        String currentText = receivedDataTextView.getText().toString();
        String updatedText = currentText + hora + "                                                   " + newData;
        receivedDataTextView.setText(updatedText+ "\n");
        // Hacer scroll hacia abajo para mostrar los datos más recientes
        dataScrollView.post(new Runnable() {
            @Override
            public void run() {
                dataScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        // Agregamos esto para mostrar los datos en el terminal
        Log.d("BluetoothData", "Datos recibidos: " + newData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
