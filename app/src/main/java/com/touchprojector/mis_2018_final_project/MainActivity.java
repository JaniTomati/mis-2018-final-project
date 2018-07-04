package com.touchprojector.mis_2018_final_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.net.UnknownHostException;

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
                qrScan.initiateScan();
                ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
                if (server_ip != null)
                    connectPhoneTask.execute(server_ip); //try to connect to server in another thread
            }
        });

        qrScan = new IntentIntegrator(this); // scan object

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
                    JSONObject obj = new JSONObject(result.getContents());
                    //setting values to textviews
                    //server_ip = InetAddress.getByName(obj.getString("server_ip"));
                    server_ip = obj.getString("server_ip");
                    server_port = Integer.parseInt(obj.getString("server_port"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    //if control comes here
                    //that means the encoded format not matches
                    //in this case you can display whatever data is available on the qrcode
                    //to a toast
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
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
                }

                if (initialX > finalX) {
                    Log.d(TOUCH_TAG, "Right to Left swipe performed");
                }

                if (initialY < finalY) {
                    Log.d(TOUCH_TAG, "Up to Down swipe performed");
                }

                if (initialY > finalY) {
                    Log.d(TOUCH_TAG, "Down to Up swipe performed");
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
    public void onDestroy()
    {
        super.onDestroy();
        if(socket.isConnected() /*&& outData != null*/) {
            try {
                outData.println("exit"); //tell server to exit
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
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                socket = new Socket(server_ip, server_port); //Open socket on server IP and port
                //String inputStream = socket.getInputStream().toString();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, socket.isConnected() ? "Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
                        try {
                            if(socket.isConnected() && socket != null) {
                                outData = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                                        .getOutputStream())), true); //create output stream to send data to server
                                //outData.println("Connected to Server.");
                            }
                        }catch (IOException e){
                            Log.e("remotedroid", "Error while creating OutWriter", e);
                            Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
                        }
                    }
                });

                
                while(socket.isConnected()) {
                    bMap = BitmapFactory.decodeStream(socket.getInputStream());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            image.setImageBitmap(bMap);
                        }
                    });

                    Log.i("InputData", socket.getInputStream().toString());
                }

            } catch (IOException e) {
                Log.e("remotedroid", "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("OnPostExecute", "Called.");
            isConnected = result;
            Toast.makeText(context, isConnected ? "Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            try {
                if(socket.isConnected()) {
                    outData = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                    //outData.println("Connected to Server.");
                }
            }catch (IOException e){
                Log.e("remotedroid", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
        }
    }
}
