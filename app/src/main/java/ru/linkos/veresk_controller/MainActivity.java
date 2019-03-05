package ru.linkos.veresk_controller;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ERR_NO = "Error_number";
    String stationIP;
    Handler supportHandler;
    TextInputLayout IpInputLayout;
    String error = "No error";
    InetAddress stationAddress;
    Socket AXSSocket;
    String AXSPort = "55555";
    getSocketAsyncTask AXSgsat;
    TCPOperations tcpo;
    protobufOperations po;
    Thread WiFiThread;
    TextView positionText;
    Handler positionTextHandler;
    boolean killWiFiRunnable;
    boolean targetspeedx;
    boolean targetspeedy;
    boolean targetpositivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IpInputLayout = findViewById(R.id.ipInputBox);
        positionText = findViewById(R.id.textViewPosition);
        tcpo = new TCPOperations();
        po = new protobufOperations();
        supportHandler = new Handler();
        positionTextHandler = new Handler();
        killWiFiRunnable = false;

    }

    public void startConnection(View view) throws ExecutionException, InterruptedException {
        View starter = findViewById(R.id.starter);
        starter.setVisibility(View.INVISIBLE);
        View controls = findViewById(R.id.controls);
        controls.setVisibility(View.VISIBLE);

        Button up = findViewById(R.id.upButton);
        Button right = findViewById(R.id.rightButton);
        Button down = findViewById(R.id.downButton);
        Button left = findViewById(R.id.leftButton);

        targetspeedx = false;
        targetspeedy = false;
        targetpositivity = false;


        up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        targetspeedy = true;
                        targetpositivity = false;

                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        targetspeedy = false;

                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
        down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        targetspeedy = true;
                        targetpositivity = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        targetspeedy = false;
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        targetspeedx = true;
                        targetpositivity = false;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        targetspeedx = false;
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        targetspeedx = true;
                        targetpositivity = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        targetspeedx = false;
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });






        stationIP = IpInputLayout.getEditText().getText().toString();
        AXSgsat = new getSocketAsyncTask(stationIP, AXSPort);
        AXSSocket = AXSgsat.execute().get();
        if (AXSSocket == null){
            startError(getApplicationContext(), "Can't start connection on Socket");
        }
        else {
            WiFiThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        stationAddress = InetAddress.getByName(stationIP);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    WifiManager wfm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    if (wfm != null && wfm.isWifiEnabled()) {

                            try {
                                if (stationAddress.isReachable(200)) {

                                    tcpo.sendTCP(AXSSocket, po.makeSreqProto());
                                    switch (po.parseSrepProto(tcpo.recieveTCP(AXSSocket))) {
                                        case 0:
                                            Log.i("Device is: ", "not ready");
                                            killWiFiRunnable = true;
                                            startError(getApplicationContext(), "Station is not ready");
                                            //WiFiThread.interrupt();
                                            break;
                                        case 1:
                                            Log.i("Device is: ", "ready");
                                            tcpo.sendTCP(AXSSocket, po.makeCreq());
                                            po.parseCrep(tcpo.recieveTCP(AXSSocket));
                                            AXSControl();
                                            break;
                                        case 2:
                                            Log.i("Device is: ", "ready but busy");
                                            killWiFiRunnable = true;
                                            startError(getApplicationContext(), "Station is busy");
                                            break;
                                    }









                                    }
                                 else {
                                    killWiFiRunnable = true;
                                    startError(getApplicationContext(), "Station unreachable");
                                    //break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }


                    } else {
                        //killWiFiRunnable = true;
                        startError(getApplicationContext(), "WiFi not enabled.");

                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            WiFiThread.setDaemon(true);
            WiFiThread.start();

        }
    }

    public void AXSControl() throws IOException, NoSuchAlgorithmException {

        while (!killWiFiRunnable) {
           // tcpo.sendTCP(AXSSocket, po.makeSreq());



            tcpo.sendTCP(AXSSocket, po.makeMreq(2, targetspeedx, targetspeedy, targetpositivity));
            final String recievedPosition = po.parseMrep(tcpo.recieveTCP(AXSSocket));
            positionTextHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateCurrentPosition(recievedPosition);
                }
            });
            //po.parseMrep(tcpo.recieveTCP(AXSSocket));


        }
    }



    public void startError(Context curContext, String error) {
        Intent errorIntent = new Intent(curContext, error_window.class);
        errorIntent.putExtra(EXTRA_MESSAGE_ERR_NO, error);
        curContext.startActivity(errorIntent);
    }

    public void updateCurrentPosition (String curPos){
        String[] positions = curPos.split(";");
        String curPosX = positions[0];
        String curPosY = positions[1];
        positionText.setText("X position: " + curPosX + "\t" + "Y position: " + curPosY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WiFiThread.interrupt();
    }
}
