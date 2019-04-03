package ru.linkos.veresk_controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
    WifiManager wfm;
    String stationIP;
    Handler supportHandler;
    TextInputLayout IpInputLayout;
    String error = "No error";
    InetAddress stationAddress;
    Socket AXSSocket;
    Socket IRVideoSocket;
    Socket IROMUSocket, IRLensSocket, IRCamSocket, TVOVideoSocket, TVOCamSocket, TVOLensSocket, TVOOMUSocket, TVIVideoSocket, TVICamSocket, TVILensSocket, TVIOMUSocket;

    String AXSPort = "55555";
    String IRVideoPort = "30001";
    String IROMUPort = "30101";
    String IRLensPort = "30201";
    String IRCamPort = "30301";
    String TVOVideoPort = "30000";
    String TVOCamPort = "30300";
    String TVOLensPort = "30200";
    String TVOOMUPort ="30100";
    String TVIVideoPort = "30002";
    String TVICamPort = "30302";
    String TVILensPort = "30202";
    String TVIOMUPort = "30102";

    getSocketAsyncTask AXSgsat;
    getSocketAsyncTask IRVideogsat;
    getSocketAsyncTask IROMUgsat;
    getSocketAsyncTask IRLensgsat;
    getSocketAsyncTask IRCamgsat;
    getSocketAsyncTask TVOVideogsat;
    getSocketAsyncTask TVOOMUgsat;
    getSocketAsyncTask TVOLensgsat;
    getSocketAsyncTask TVOCamgsat;
    getSocketAsyncTask TVIVideogsat;
    getSocketAsyncTask TVIOMUgsat;
    getSocketAsyncTask TVILensgsat;
    getSocketAsyncTask TVICamgsat;
    TCPOperations tcpo;
    protobufOperations po;

    Thread WiFiThread;
    Thread VideoThread;
    Thread controlThread;
    Thread startCheck;

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

    List<Integer> AXSdata = new ArrayList<>();
    List<Integer> IRCameradata = new ArrayList<>();
    List<Integer> IROMUdata = new ArrayList<>();
    List<Integer> IRLensdata = new ArrayList<>();
    List<Integer> TVICameradata = new ArrayList<>();
    List<Integer> TVIOMUdata = new ArrayList<>();
    List<Integer> TVILensdata = new ArrayList<>();
    List<Integer> TVOCameradata = new ArrayList<>();
    List<Integer> TVOOMUdata = new ArrayList<>();
    List<Integer> TVOLensdata = new ArrayList<>();
    View starter, controls, IRcontrols, IZMcontrols, TVOcontrols;
    ViewGroup.LayoutParams lp;
    float xs = 0, xy = 0, bx = 0, by = 0;
    List<Float> comparator;
    List<Socket> componentSockets = new ArrayList<>();
    List<Integer> mids = new ArrayList<>();
     boolean AXSexists;
     boolean stationreachable;
     boolean IRexists;
     boolean TVIexists;
     boolean TVOexists;
    volatile boolean startexecuted = false;
    Socket activeVideoSocket;

    enum activeChan {TVO, TVI, IR}
    activeChan activeCam;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //switching on fullscreen, setting display always on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //system needed stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mids.add(1398292803);
        mids.add(1178943811); //AEF
        mids.add(2020177511); //Lens
        mids.add(2003134311); //Cam
        mids.add(1398292803);
        mids.add(1178943811);
        mids.add(2020177511);
        mids.add(2003134311);
        mids.add(1398292803);
        mids.add(1178943811);
        mids.add(2020177511);
        mids.add(2003134311);

        //startexecuted = false;

        IpInputLayout = findViewById(R.id.ipInputBox);
        positionText = findViewById(R.id.textViewPosition);
        tcpo = new TCPOperations();
        po = new protobufOperations();
        supportHandler = new Handler();
        positionTextHandler = new Handler();
        killWiFiRunnable = false;
        frameHolder = findViewById(R.id.picPlace);
        picHandler = new Handler();
       // IRclosed = true;

        lp =  frameHolder.getLayoutParams();
        comparator = new ArrayList<>(4);
        comparator.add(0,0.0f);
        comparator.add(1,0.0f);
        comparator.add(2, 0.0f);
        comparator.add(3, 0.0f);

    }

    public void startConnection(View view) throws ExecutionException, InterruptedException, IOException, NoSuchAlgorithmException {
        starter = findViewById(R.id.starter);
        starter.setVisibility(View.INVISIBLE);
        controls = findViewById(R.id.controls);
        //controls.setVisibility(View.VISIBLE);
        IRcontrols = findViewById(R.id.IRControls);
        TVOcontrols = findViewById(R.id.TVOControls);
        // IRcontrols.setVisibility(View.VISIBLE);


       /* Button up = findViewById(R.id.upButton);
        Button right = findViewById(R.id.rightButton);
        Button down = findViewById(R.id.downButton);
        Button left = findViewById(R.id.leftButton);

        targetspeedx = false;
        targetspeedy = false;
        targetpositivity = false;*/


        /*up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
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
                switch (event.getAction()) {
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
                switch (event.getAction()) {
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
                switch (event.getAction()) {
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
        });*/


        final WifiManager wfm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        startCheck = new Thread(new Runnable() {
            @Override
            public void run() {
                if (wfm != null && wfm.isWifiEnabled()) {

                    try {
                        stationAddress = InetAddress.getByName(stationIP);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (stationAddress.isReachable(200)) {

                            stationreachable = true;

                            stationIP = IpInputLayout.getEditText().getText().toString();
                            //                           AXSgsat = new getSocketAsyncTask(stationIP, AXSPort);
//                            AXSSocket = AXSgsat.execute().get();
                            AXSSocket = tcpo.getSocket(stationIP, AXSPort);
                            //Log.i("AXS ", String.valueOf(AXSSocket));
                            IRVideoSocket = tcpo.getSocket(stationIP, IRVideoPort);
                            IROMUSocket = tcpo.getSocket(stationIP, IROMUPort);
                            IRLensSocket = tcpo.getSocket(stationIP, IRLensPort);
                            IRCamSocket = tcpo.getSocket(stationIP, IRCamPort);

                            TVOVideoSocket = tcpo.getSocket(stationIP, TVOVideoPort);
                            TVOOMUSocket = tcpo.getSocket(stationIP, TVOOMUPort);
                            TVOLensSocket = tcpo.getSocket(stationIP, TVOLensPort);
                            TVOCamSocket = tcpo.getSocket(stationIP, TVOCamPort);

                            TVIVideoSocket = tcpo.getSocket(stationIP, TVIVideoPort);
                            TVIOMUSocket = tcpo.getSocket(stationIP, TVIOMUPort);
                            TVILensSocket = tcpo.getSocket(stationIP, TVILensPort);
                            TVICamSocket = tcpo.getSocket(stationIP, TVICamPort);
                       /*     IRVideogsat = new getSocketAsyncTask(stationIP, IRVideoPort);
                            IRVideoSocket = IRVideogsat.execute().get();
                            IROMUgsat = new getSocketAsyncTask(stationIP, IROMUPort);
                            IROMUSocket = IROMUgsat.execute().get();
                            IRLensgsat = new getSocketAsyncTask(stationIP, IRLensPort);
                            IRLensSocket = IRLensgsat.execute().get();
                            IRCamgsat = new getSocketAsyncTask(stationIP, IRCamPort);
                            IRCamSocket = IRCamgsat.execute().get();
                            TVOVideogsat = new getSocketAsyncTask(stationIP, TVOVideoPort);
                            TVOVideoSocket = TVOVideogsat.execute().get();
                            TVOCamgsat = new getSocketAsyncTask(stationIP, TVOCamPort);
                            TVOCamSocket = TVOCamgsat.execute().get();
                            TVOLensgsat = new getSocketAsyncTask(stationIP, TVOLensPort);
                            TVOLensSocket = TVOLensgsat.execute().get();
                            TVOOMUgsat = new getSocketAsyncTask(stationIP, TVOOMUPort);
                            TVOOMUSocket = TVOOMUgsat.execute().get();
                            TVIVideogsat = new getSocketAsyncTask(stationIP, TVIVideoPort);
                            TVIVideoSocket = TVIVideogsat.execute().get();
                            TVICamgsat = new getSocketAsyncTask(stationIP, TVICamPort);
                            TVICamSocket = TVICamgsat.execute().get();
                            TVILensgsat = new getSocketAsyncTask(stationIP, TVILensPort);
                            TVILensSocket = TVILensgsat.execute().get();
                            TVIOMUgsat = new getSocketAsyncTask(stationIP, TVIOMUPort);
                            TVIOMUSocket = TVIOMUgsat.execute().get();


                            componentSockets.add(AXSSocket);

                            // componentSockets.add(IRVideoSocket);
                            componentSockets.add(IROMUSocket);
                            componentSockets.add(IRLensSocket);
                            componentSockets.add(IRCamSocket);

                            //componentSockets.add(TVOVideoSocket);
                            componentSockets.add(TVOOMUSocket);
                            componentSockets.add(TVOLensSocket);
                            componentSockets.add(TVOCamSocket);

                            //componentSockets.add(TVIVideoSocket);
                            componentSockets.add(TVIOMUSocket);
                            componentSockets.add(TVILensSocket);
                            componentSockets.add(TVICamSocket);


                            Log.i("List ", String.valueOf(componentSockets));*/

                        } else {
                            stationreachable = false;
                            startError(getApplicationContext(), "Check your Wi-Fi connection");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } /*catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    // Log.i("AXS S ", String.valueOf(AXSSocket));
                    if (AXSSocket == null) {
                        //startError(getApplicationContext(), "Can't start connection on AXS Socket");
                        AXSexists = false;
//                        Toast.makeText(getApplicationContext(), "AXS not avaliable", Toast.LENGTH_SHORT).show();
                    } else {
                        AXSexists = true;
                        try {
                            tcpo.sendTCP(AXSSocket, po.makeAXSCreq());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            AXSdata = po.parseAXSCrep(tcpo.recieveTCP(AXSSocket));
                            //Log.i("AXS data ", String.valueOf(AXSdata));
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //Log.i("AXSexists ", String.valueOf(AXSexists));
                    IRexists = IRVideoSocket != null;


                    TVOexists = TVOVideoSocket != null;

                    TVIexists = TVIVideoSocket != null;

                } else {
                    stationreachable = false;
                    startError(getApplicationContext(), "WiFi not enabled.");
                }
                startexecuted = true;
            }
        });


        WiFiThread = new Thread(new Runnable() {
            @Override
            public void run() {


                if (wfm != null && wfm.isWifiEnabled()) {

                    try {
                        if (stationAddress.isReachable(200)) {
                            stationreachable = true;
                                    /*tcpo.sendTCP(AXSSocket, po.makeSreqProto());
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
                                            tcpo.sendTCP(TVOLensSocket, po.makeTVOLensCreq());
                                            TVOLensdata = po.parseTVOLensCrep(tcpo.recieveTCP(TVOLensSocket));
//                                            tcpo.sendTCP(IROMUSocket, po.makeIROMUCreq());
  //                                         IROMUdata = po.parseIROMUCrep(tcpo.recieveTCP(IROMUSocket));
    //                                        tcpo.sendTCP(IRLensSocket, po.makeIRLensCreq());


                                            AXSControl();
                                            break;
                                        case 2:
                                            Log.i("Device is: ", "ready but busy");
                                            killWiFiRunnable = true;
                                            startError(getApplicationContext(), "Station is busy");
                                            break;
                                    }
*/
                        } else {
                            stationreachable = false;
                            startError(getApplicationContext(), "Station unreachable");
                            //break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else {
                    //killWiFiRunnable = true;
                    stationreachable = false;
                    startError(getApplicationContext(), "WiFi not enabled.");

                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        controlThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (stationreachable) {

                    try {
                        AXSControl();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }

                    if (TVOexists){
                        try {
                            tcpo.sendTCP(TVOOMUSocket, po.makeTVOCreq());
                            TVOOMUdata = po.parseTVOOMUCrep(tcpo.recieveTCP(TVOOMUSocket));
                            tcpo.sendTCP(TVOLensSocket, po.makeTVOCreq());
                            TVOLensdata = po.parseTVOLensCrep(tcpo.recieveTCP(TVOLensSocket));
                            tcpo.sendTCP(TVOCamSocket, po.makeTVOCreq());
                            TVOCameradata = po.parseTVOCamCrep(tcpo.recieveTCP(TVOCamSocket));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                    if (IRexists){
                        try {
                            tcpo.sendTCP(IROMUSocket, po.makeIROMUCreq());
                            IROMUdata = po.parseIROMUCrep(tcpo.recieveTCP(IROMUSocket));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }





                } else {
                    //Thread.interrupted();
                }

            }
        });


        VideoThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (stationreachable) {
                    switch (activeCam){
                        case IR:
                            activeVideoSocket = IRVideoSocket;
                            break;
                        case TVI:
                            activeVideoSocket = TVIVideoSocket;
                            break;
                        case TVO:
                            activeVideoSocket = TVOVideoSocket;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TVOcontrols.setVisibility(View.VISIBLE);
                                }
                            });

                            break;
                    }

                    final Bitmap settingFrame = getVideo(activeVideoSocket);
                    //Log.i("Pic: ", String.valueOf(settingFrame));
                    bufFrame = null;


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
        startCheck.setDaemon(true);
        startCheck.start();
        startCheck.join();
        /*if (!startCheck.isAlive()){

            Log.i("Start ", String.valueOf(startexecuted));}*/
        if (startexecuted) {
            if (TVOexists){
                activeCam = activeChan.TVO;
            }
            else {
                if (TVIexists){
                    activeCam = activeChan.TVI;
                }
                else {
                    activeCam = activeChan.IR;
                }
            }
            WiFiThread.setDaemon(true);
            WiFiThread.start();
            VideoThread.setDaemon(true);
            VideoThread.start();
            controlThread.start();
            // Log.i("Check: ", String.valueOf(AXSexists));}
        }
    }


    public void AXSControl() throws IOException, NoSuchAlgorithmException {

        while (AXSexists) {


            //tcpo.sendTCP(AXSSocket, po.makeMreq(2, targetspeedx, targetspeedy, targetpositivity));
            tcpo.sendTCP(AXSSocket, po.AXSmakeMreq(AXSdata, 1.2f, xs, xy));
            // Log.d("x, y ", String.valueOf(xs) + String.valueOf(xy));
            final String recievedPosition = po.parseMrep(tcpo.recieveTCP(AXSSocket));
            positionTextHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateCurrentPosition(recievedPosition);
                }
            });
        }
    }






        //}




    public void startError(Context curContext, String error) {
        Intent errorIntent = new Intent(curContext, error_window.class);
        errorIntent.putExtra(EXTRA_MESSAGE_ERR_NO, error);
        curContext.startActivity(errorIntent);
    }

    public void updateCurrentPosition (String curPos){
        String[] positions = curPos.split(";");
        String curPosX = positions[0].substring(0, positions[0].indexOf(".")+2);
        String curPosY =  positions[1].substring(0, positions[1].indexOf(".")+2);
        positionText.setText("X position: " + curPosX + "\t" + "Y position: " + curPosY);
    }

    public Bitmap getVideo (Socket inputSocket){
        if (inputSocket != null){
            //Log.i("picsock ", String.valueOf(inputSocket));
            byte[] pic = new byte[0];
            try {
                pic = tcpo.recieveTCP(inputSocket);
                //Log.i("piccy ", String.valueOf(pic));
            } catch (IOException e) {
                e.printStackTrace();
            }

            frames = BitmapFactory.decodeByteArray(pic, 0, pic.length);

    }
        return frames;
}

    @SuppressLint("ClickableViewAccessibility")
    public void setFrame (Bitmap picture){

        frameHolder.setImageBitmap(picture);

        final GestureDetector gd = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener(){
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                //your action here for double tap e.g.
                Log.d("OnDoubleTapListener", "onDoubleTap");



                return true;
            }



            @Override
            public boolean onDown(MotionEvent event) {
                Log.d("TAG","onDown: ");

                // don't return false here or else none of the other
                // gestures will work
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.i("TAG", "onSingleTapConfirmed: ");
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
              /*  if (frameHolder.getLayoutParams() != controls.getLayoutParams()){
                  frameHolder.setLayoutParams(controls.getLayoutParams());
                }
                else {
                    frameHolder.setLayoutParams(lp);
                }*/
                Log.i("TAG", "onLongPress: ");
            }



            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                Log.i("TAG", "onScroll: ");
                //float cx = e2.getX();
                //float cy = e2.getY();

                /*comparator.set(0, bx);
                comparator.set(1, e2.getX());
                if (distanceX < 100 && distanceX > 10 || distanceX > -100 && distanceX < -10) {
                    if (comparator.get(0).equals(comparator.get(1))) {
                        xs = 0;
                    } else {
                        if (comparator.get(0) > comparator.get(1)) {

                            xs = -1;
                        } else {
                            xs = 1;

                        }
                    }
                }
                else {
                    xs = 0;
                }
                comparator.set(2, by);
                comparator.set(3, e2.getY());
                Log.i("DY ", String.valueOf(distanceY));
                Log.i("DX ", String.valueOf(distanceX));
                if (distanceY < 100 && distanceY > 10 || distanceY > -100 && distanceY < -10) {
                    if (comparator.get(2).equals(comparator.get(3))) {
                        xy = 0;
                    } else {
                        if (comparator.get(2) > comparator.get(3)) {
                            xy = 1;
                            //Log.i("DIS: ", String.valueOf(comparator.get(2) - comparator.get(3)));
                        } else {
                            xy = -1;
                            //Log.i("DIS: ", String.valueOf(comparator.get(2) - comparator.get(3)));
                        }
                    }
                }
                else {
                    xy = 0;
                }*/

                return true;
            }


            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2,
                                   float velocityX, float velocityY) {
                Log.d("TAG", "onFling: ");
                return true;
            }



        });
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // pass the events to the gesture detector
                // a return value of true means the detector is handling it
                // a return value of false means the detector didn't
                // recognize the event

                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    Log.d("TouchTest", "Touch down");
                    if (event.getX() < (((v.getWidth()/2) - (v.getWidth()*0.1))) || event.getX() > ((v.getWidth()/2) + (v.getWidth()*0.1))){
                   // if (event.getX() < (findViewById(R.id.father).getWidth()/2) - (findViewById(R.id.father).getWidth()*0.1) || event.getX() > (findViewById(R.id.father).getWidth()/2) + (findViewById(R.id.father).getWidth()*0.1)){
                        Log.i("event X: ", String.valueOf(event.getX()));
                        Log.i("event X: ", String.valueOf(findViewById(R.id.father).getWidth()/2));
                        if (event.getX() > (v.getWidth()/2)){
                            xs = 1;
                        }
                        else {
                            xs = -1;
                        }
                    }
                    else {
                        xs = 0;
                    }
                    if (event.getY() < (((v.getHeight()/2) - (v.getHeight()*0.1))) || event.getY() > ((v.getHeight()/2) + (v.getHeight()*0.1))){
                        // if (event.getX() < (findViewById(R.id.father).getWidth()/2) - (findViewById(R.id.father).getWidth()*0.1) || event.getX() > (findViewById(R.id.father).getWidth()/2) + (findViewById(R.id.father).getWidth()*0.1)){
                        //Log.i("event X: ", String.valueOf(event.getX()));
                        //Log.i("event X: ", String.valueOf(findViewById(R.id.father).getWidth()/2));
                        if (event.getY() > (v.getHeight()/2)){
                            xy = -1;
                        }
                        else {
                            xy = 1;
                        }
                    }
                    else {
                        xy = 0;
                    }

                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    Log.d("TouchTest", "Touch up");
                    xs = 0;
                    xy = 0;
                    /*bx = event.getX();
                    by = event.getY();*/
                }



               // Log.i("touch", String.valueOf(gd.onTouchEvent(event)));
                return gd.onTouchEvent(event);

            }


        };

        frameHolder.setOnTouchListener(touchListener);
    }

    public void openIRCover (View view){

        new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_OPEN);

    }

    public void closeIRCover (View view){

        new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_CLOSE);

    }


    public void TVOZoomIn (View view){


    }

    public void TVOZoomOut (View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_CLOSE);
        WiFiThread.interrupt();
        VideoThread.interrupt();
    }


}
