package com.touchprojector.mis_2018_final_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final String TOUCH_TAG = "TouchEvent";

    Context context;

    private boolean isConnected = false;
    private boolean mouseMoved = false;
    private Socket socket;
    private Button connectBtn;
    private Button scanBtn;

    private PrintWriter outData;
    private ImageView image;
    private Bitmap bMap;

    private float initialX;
    private float initialY;
    private float finalX;
    private float finalY;

    private IntentIntegrator qrScan;
    private String server_ip = "192.168.0.32";
    private int server_port = 8090;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        context = this;

        image = (ImageView) findViewById(R.id.testImage);

        connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
                connectPhoneTask.execute(server_ip); //try to connect to server in another thread
            }
        });

        scanBtn = (Button) findViewById(R.id.qr_scan);
        scanBtn.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (socket != null && socket.isConnected()) {
                    Log.i("Disconnect", "Disconnected from server!");
                    try {
                        socket.shutdownInput();
                        socket.shutdownOutput();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                qrScan.initiateScan();
                ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
                if (server_ip != null)
                    connectPhoneTask.execute(server_ip); //try to connect to server in another thread
            }
        });

        qrScan = new IntentIntegrator(this); // initialize QR scanner

        // check whether camera permission is granted
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }
    }

    /**
     * Request camera permission.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted! :-)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Camera permission denied. :-(", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     *  https://www.simplifiedcoding.net/android-qr-code-scanner-tutorial/
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                try {
                    //converting the data to json
                    JSONObject obj = new JSONObject(result.getContents()); // read server_ip and server_port from scanned json
                    Log.i("ServerIP", obj.getString("server_ip"));
                    Log.i("ServerPort", obj.getString("server_port"));
                    server_ip = obj.getString("server_ip");
                    server_port = Integer.parseInt(obj.getString("server_port"));
                } catch (JSONException e) {
                    e.printStackTrace(); // QR code format does not match JSON
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show(); // display content in a toast
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * http://codetheory.in/android-ontouchevent-ontouchlistener-motionevent-to-detect-common-gestures/
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                initialY = event.getY();

                Log.d(TOUCH_TAG, "Action was DOWN");
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d(TOUCH_TAG, "Action was MOVE");
                break;

            case MotionEvent.ACTION_UP:
                finalX = event.getX();
                finalY = event.getY();

                if (socket.isConnected() && outData != null) {
                    outData.println("MouseClick:" + finalX + ":" + finalY);
                    Log.i("OutStream", "Sendet Daten " + finalX + " " + finalY);
                }

                Log.d(TOUCH_TAG, "Action was UP");

                if (initialX < finalX) {
                    Log.d(TOUCH_TAG, "Left to Right swipe performed");
                    if (socket.isConnected() && outData != null) {
                        outData.println("SwipeLeft");
                    }
                }

                if (initialX > finalX) {
                    Log.d(TOUCH_TAG, "Right to Left swipe performed");
                    if (socket.isConnected() && outData != null) {
                        outData.println("SwipeRight");
                    }
                }

                if (initialY < finalY) {
                    Log.d(TOUCH_TAG, "Up to Down swipe performed");
                    if (socket.isConnected() && outData != null) {
                        outData.println("SwipeUp");
                    }
                }

                if (initialY > finalY) {
                    Log.d(TOUCH_TAG, "Down to Up swipe performed");
                    if (socket.isConnected() && outData != null) {
                        outData.println("SwipeDown");
                    }
                }

                break;

            case MotionEvent.ACTION_CANCEL:
                Log.d(TOUCH_TAG,"Action was CANCEL");
                break;

            case MotionEvent.ACTION_OUTSIDE:
                Log.d(TOUCH_TAG, "Movement occurred outside bounds of current screen element");
                break;
        }

        return super.onTouchEvent(event);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(socket != null && socket.isConnected()) {
            Log.i("Disconnect", "Disconnected from server!");
            outData.println("exit"); //tell server to exit
            try {
                socket.close(); //close socket
            } catch (IOException e) {
                Log.e("remotedroid", "Error in closing socket", e);
            }
        }
    }
    
    public class ConnectPhoneTask extends AsyncTask<String,Void,Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                socket = new Socket(server_ip, server_port); //Open socket on server IP and port

                // try thread
                MessageThread client_message = new MessageThread();
                Log.i("MessageThread", "doInBackground");
                client_message.start();

                while(socket.isConnected()) {
                    bMap = BitmapFactory.decodeStream(socket.getInputStream());
                    Log.d("ImageData", "Receive image data.");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("ImageData", "Display image data.");
                            image.setImageBitmap(bMap);
                        }
                    });

                    Log.v("InputData", socket.getInputStream().toString());
                }

            } catch (IOException e) {
                Log.e("Connection", "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("OnPostExecute", "Called.");
            isConnected = result;
            //Toast.makeText(context, isConnected ? "Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            /*try {
                if(socket.isConnected()) {
                    outData = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                    //outData.println("Connected to Server.");
                }
            }catch (IOException e){
                Log.e("remotedroid", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }*/
        }
    }

    class MessageThread extends Thread {
        @Override
        public void run(){
            Log.i("MessageThread", "prepare");
            Looper.prepare();
            Log.i("MessageThread", "prepared");

            Toast.makeText(context, socket.isConnected() ? "Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            Log.i("MessageThread", "Called.");

            try {
                if(socket != null && socket.isConnected()) {
                    outData = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                    outData.println("Connected to Server.");
                    Log.i("OutData", "Data " + outData.toString());
                }
            } catch (IOException e){
                Log.e("OutWriter", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
            Looper.loop();

        }
    }
}
