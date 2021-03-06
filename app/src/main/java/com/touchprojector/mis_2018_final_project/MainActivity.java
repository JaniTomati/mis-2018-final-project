package com.touchprojector.mis_2018_final_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

// import com.github.chrisbanes.photoview.OnViewDragListener;
// import com.github.chrisbanes.photoview.OnViewTapListener;
// import com.github.chrisbanes.photoview.PhotoView;
// import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final String TOUCH_TAG = "TouchEvent";
    private static final String CONNECTION_TAG = "Connection";
    private static final String IMAGE_DATA_TAG = "ImageData";
    private static final String SERVER_COMMUNICTION_TAG = "ClientServerCommunication";

    Context context;

    private Socket socket;
    private Button connectBtn;
    private Button scanBtn;

    private PrintWriter outData;
    private ImageView image;
    private Bitmap bMap;

//    // PHOTOVIEW
//    private PhotoView image;
//    private PhotoViewAttacher mAttacher;

    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;

    private float initialX;
    private float initialY;
    private float finalX;
    private float finalY;

    private float mTranslateX;
    private float mTranslateY;
    private float mMinZoomFactor = 1.0f;
    private float mMaxZoomFactor = 5.0f;

    private IntentIntegrator qrScan;
    private String server_ip;
    private int server_port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        context = this;
        image = (ImageView) findViewById(R.id.imageView);

//        // PHOTOVIEW: Extension of ImageView Class. Allows zooming and panning.
//        // Creates complications with other functionalities
//        image = (PhotoView) findViewById(R.id.imageView);
//        mAttacher = new PhotoViewAttacher(image);

//        // PHOTOVIEW: OnViewTapListener messes with panning and zoom while zoomed in!
//        // Clicking while zoomed in not possible!
//        mAttacher.setOnViewTapListener(new OnViewTapListener() {
//            @Override
//            public void onViewTap(View view, float x, float y) {
//                Log.d(TOUCH_TAG, "x: " + x + " y: " + y);
//
//                float displayWidth = image.getDisplayRect().top;
//                float displayHeight = image.getDisplayRect().left;
//
//                if (socket != null && socket.isConnected() && outData != null) {
//                    outData.println("MouseClick:" + finalX + ":" + finalY);
//                    Log.v(TOUCH_TAG, "MouseClick: " + finalX + " " + finalY);
//                }
//            }
//        });

//        // PHOTOVIEW: Drag sends data multiple times while the finger is touching the display
//        // No way to detect when finger is touching the display / lifted
//        mAttacher.setOnViewDragListener(new OnViewDragListener() {
//            @Override
//            public void onDrag(float dx, float dy) {
//                float threshold = 35.0f;
//
//                if (dx > 0 && Math.abs(dx) > threshold) {
//                    Log.d(TOUCH_TAG, "Left to Right swipe performed");
//                    if (socket != null && socket.isConnected() && outData != null) {
//                        outData.println("SwipeLeft");
//                    }
//                }
//
//                if (dx > 0 && Math.abs(dx) > threshold) {
//                    Log.d(TOUCH_TAG, "Right to Left swipe performed");
//                    if (socket != null && socket.isConnected() && outData != null) {
//                        outData.println("SwipeRight");
//                    }
//                }
//
//                if (dy > 0 && Math.abs(dy) > threshold) {
//                    Log.d(TOUCH_TAG, "Up to Down swipe performed");
//                    if (socket != null && socket.isConnected() && outData != null) {
//                        outData.println("SwipeUp");
//                    }
//                }
//
//                if (dy < 0 && Math.abs(dy) > threshold) {
//                    Log.d(TOUCH_TAG, "Down to Up swipe performed");
//                    if (socket != null && socket.isConnected() && outData != null) {
//                        outData.println("SwipeDown");
//                    }
//                }
//            }
//        });

        // used for pinch zoom
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // shut down old connection if it exists
                if (socket != null && socket.isConnected()) {
                    Log.i(CONNECTION_TAG, "Disconnected from server!");
                    try {
                        outData.println("exit"); // tell server to exit
                        Thread.sleep(1000);
                        socket.shutdownInput();
                        socket.shutdownOutput();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
                if (server_ip != null)
                    connectPhoneTask.execute(server_ip); // try to connect to server in another thread
            }
        });

        scanBtn = (Button) findViewById(R.id.qr_scan);
        scanBtn.setOnClickListener( new View.OnClickListener() {

            // get server ip and server port from qr code
            @Override
            public void onClick(View v) {
                qrScan.initiateScan();
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
     * Hide Toolbars
     * Fullscreen mode
     * https://stackoverflow.com/questions/31482522/immersive-mode-android-studio
     * */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {

            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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
     *  Get the data from the QR-Code
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            // if QR-Code is empty
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                // if QR-Code contains data
                try {
                    // converting the data to json
                    JSONObject obj = new JSONObject(result.getContents()); // read server_ip and server_port from scanned json
                    Log.v(SERVER_COMMUNICTION_TAG, obj.toString());
                    Log.i(SERVER_COMMUNICTION_TAG, "ServerIP: " + obj.getString("server_ip"));
                    Log.i(SERVER_COMMUNICTION_TAG, "ServerPort: " +obj.getString("server_port"));
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
     * Detect swipe and click gestures
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean swipe = false; // if swipe was performed
        int threshold = 200;
        int action = event.getActionMasked();

        mScaleGestureDetector.onTouchEvent(event);

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                initialY = event.getY();

                Log.v(TOUCH_TAG, "Action was DOWN");
                break;

            case MotionEvent.ACTION_MOVE:

                // panning interferes with swipes
                // problematic when zooming out since the picture does center itself
                if (mScaleFactor != 1.0) {
                    mTranslateX = event.getX() - initialX;
                    mTranslateY = event.getY() - initialY;

                    image.setTranslationX(mTranslateX / mScaleFactor);
                    image.setTranslationY(mTranslateY / mScaleFactor);
                }

                Log.v(TOUCH_TAG, "Action was MOVE");
                break;

            case MotionEvent.ACTION_UP:
                finalX = event.getX();
                finalY = event.getY();

                Log.v(TOUCH_TAG, "Action was UP");

                // only while not zoomed out!
                if (mScaleFactor ==  1.0) {
                    if (initialX < finalX && Math.abs(finalX - initialX) > threshold) {
                        swipe = true;
                        Log.d(TOUCH_TAG, "Left to Right swipe performed");
                        if (socket != null && socket.isConnected() && outData != null) {
                            outData.println("SwipeLeft");
                        }
                    }

                    else if (initialX > finalX && Math.abs(finalX - initialX) > threshold) {
                        swipe = true;
                        Log.d(TOUCH_TAG, "Right to Left swipe performed");
                        if (socket != null && socket.isConnected() && outData != null) {
                            outData.println("SwipeRight");
                        }
                    }

                    else if (initialY < finalY && Math.abs(finalY - initialY) > threshold) {
                        swipe = true;
                        Log.d(TOUCH_TAG, "Up to Down swipe performed");
                        if (socket != null && socket.isConnected() && outData != null) {
                            outData.println("SwipeUp");
                        }
                    }

                    else if (initialY > finalY && Math.abs(finalY - initialY) > threshold) {
                        swipe = true;
                        Log.d(TOUCH_TAG, "Down to Up swipe performed");
                        if (socket != null && socket.isConnected() && outData != null) {
                            outData.println("SwipeDown");
                        }
                    }

                    // send click data only if no swipe was detected
                    if (!swipe && socket != null && socket.isConnected() && outData != null) {
                        outData.println("MouseClick:" + finalX + ":" + finalY);
                        Log.v(TOUCH_TAG, "MouseClick: " + finalX / mScaleFactor + " " + finalY / mScaleFactor);
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
            Log.i(CONNECTION_TAG, "Disconnected from server!");
            try {
                outData.println("exit"); //tell server to exit
                Thread.sleep(1000);
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close(); //close socket
            } catch (IOException e) {
                Log.e(CONNECTION_TAG, "Error in closing socket", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public class ConnectPhoneTask extends AsyncTask<String,Void,Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                socket = new Socket(server_ip, server_port); // Open socket on server IP and port

                // try thread
                MessageThread client_message = new MessageThread();
                client_message.start();

                while(socket.isConnected()) {
                    bMap = BitmapFactory.decodeStream(socket.getInputStream());
                    Log.v(IMAGE_DATA_TAG, "Receive image data.");

                    // load new image data to create continuous stream
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(IMAGE_DATA_TAG, "Display image data.");
//                          // PHOTOVIEW: Disables zoom reset after loading new image
//                          // https://github.com/chrisbanes/PhotoView/issues/505
//                          Matrix matrix = new Matrix();
//                          image.getAttacher().getSuppMatrix(matrix);
                            image.setImageBitmap(bMap);
//                          image.getAttacher().setDisplayMatrix(matrix);
                        }
                    });
                }

            } catch (IOException e) {
                Log.e(CONNECTION_TAG, "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("OnPostExecute", "Called.");
        }
    }

    class MessageThread extends Thread {
        @Override
        public void run(){
            Looper.prepare();
            Toast.makeText(context, socket.isConnected() ? "Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();

            // creates streams for client-server-communication
            try {
                if(socket != null && socket.isConnected()) {
                    outData = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); // create output stream to send data to server
                    outData.println("Connected to Server.");
                }
            } catch (IOException e){
                Log.e(SERVER_COMMUNICTION_TAG, "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
            Looper.loop();
        }
    }

    /**
     * Pinch zoom
     * https://medium.com/quick-code/pinch-to-zoom-with-multi-touch-gestures-in-android-d6392e4bf52d
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(mMinZoomFactor,
                    Math.min(mScaleFactor, mMaxZoomFactor));
            image.setScaleX(mScaleFactor);
            image.setScaleY(mScaleFactor);
            return true;
        }
    }
}
