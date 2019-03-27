package ru.linkos.veresk_controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import Linkos.RTC.Message.AEF.Aef;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ERR_NO = "Error_number";
   // private static final Object Lid;
    String stationIP;
    Handler supportHandler;
    TextInputLayout IpInputLayout;
    String error = "No error";
    InetAddress stationAddress;
    Socket AXSSocket;
    Socket IRVideoSocket;
    Socket IROMUSocket;
    String AXSPort = "55555";
    String IRVideoPort = "30001";
    String IROMUPort = "30100";
    getSocketAsyncTask AXSgsat;
    getSocketAsyncTask IRVideogsat;
    getSocketAsyncTask IROMUgsat;
    TCPOperations tcpo;
    protobufOperations po;
    Thread WiFiThread;
    Thread VideoThread;
    TextView positionText;
    Handler positionTextHandler;
    boolean killWiFiRunnable;
    boolean targetspeedx;
    boolean targetspeedy;
    boolean targetpositivity;
    ImageView frameHolder;
    Handler picHandler;
    Bitmap frames;
    Bitmap bufFrame;
    boolean IRclosed;
    IRCoverAsyncTask IRCsgatO, IRCsgatC;
    List<Integer> IROMUdata = new ArrayList<>();




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
        frameHolder = findViewById(R.id.picPlace);
        picHandler = new Handler();
        IRclosed = true;
       

    }

    public void startConnection(View view) throws ExecutionException, InterruptedException, IOException {
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
        IRVideogsat = new getSocketAsyncTask(stationIP, IRVideoPort);
        IRVideoSocket = IRVideogsat.execute().get();
        IROMUgsat = new getSocketAsyncTask(stationIP, IROMUPort);
        IROMUSocket = IROMUgsat.execute().get();





        if (AXSSocket == null){
            startError(getApplicationContext(), "Can't start connection on AXS Socket");
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
                                            tcpo.sendTCP(IROMUSocket, po.makeIROMUCreq());
                                           IROMUdata = po.parseIROMUCrep(tcpo.recieveTCP(IROMUSocket));
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


            VideoThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        final Bitmap settingFrame = getVideo();
                        bufFrame = null;
                        if (IRclosed)

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                    if (bufFrame != settingFrame) {
                                        setFrame(settingFrame);
                                        bufFrame = settingFrame;
                                    }
                                }


                        });
                    }
                }
                }
            );

            WiFiThread.setDaemon(true);
            WiFiThread.start();
            VideoThread.setDaemon(true);
            VideoThread.start();

        }
    }

    public void AXSControl() throws IOException, NoSuchAlgorithmException {

        while (!killWiFiRunnable) {




            tcpo.sendTCP(AXSSocket, po.makeMreq(2, targetspeedx, targetspeedy, targetpositivity));
            final String recievedPosition = po.parseMrep(tcpo.recieveTCP(AXSSocket));
            positionTextHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateCurrentPosition(recievedPosition);
                }
            });





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

    public Bitmap getVideo (){
        if (IRVideoSocket != null){
            byte[] pic = new byte[0];
            try {
                pic = tcpo.recieveTCP(IRVideoSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }

            frames = BitmapFactory.decodeByteArray(pic, 0, pic.length);

    }
        return frames;
}

    public void setFrame (Bitmap picture){

        frameHolder.setImageBitmap(picture);
    }

    public void openIRCover (View view){

        new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_OPEN);

    }

    public void closeIRCover (View view){

        new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_CLOSE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WiFiThread.interrupt();
        VideoThread.interrupt();
    }
}
