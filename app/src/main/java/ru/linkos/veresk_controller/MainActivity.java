package ru.linkos.veresk_controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import Linkos.RTC.Message.AEF.Aef;
import Linkos.RTC.Message.Lens.Lens;

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
    String TVOOMUPort = "30100";
    String TVIVideoPort = "30002";
    String TVICamPort = "30302";
    String TVILensPort = "30202";
    String TVIOMUPort = "30102";

    String curpossum;
    String recievedPosition = "No position recieved";


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
    View starter, controls, chancontrols;;
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

    activeChan activeCam = activeChan.TVO;

    int videoMid = 1398292803,
        aefMid = 1178943811,
        lensMid = 1313164355,
        cameraMid = 1296122691;

    int TVOZoom = 0;
    int TVOFocus = 0;

    Handler mHandler;

    Runnable r;
    Runnable run;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //switching on fullscreen, setting display always on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //system needed stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        positionTextHandler = new Handler(){
            public void handleMessage(Message msg){
                Log.i("msg ", (String) msg.obj);
                positionText.setText((String) msg.obj);
            }
        };

        IpInputLayout = findViewById(R.id.ipInputBox);
        positionText = findViewById(R.id.textViewPosition);
        tcpo = new TCPOperations();
        po = new protobufOperations();
        supportHandler = new Handler();

        killWiFiRunnable = false;
        frameHolder = findViewById(R.id.picPlace);
        picHandler = new Handler();


        lp = frameHolder.getLayoutParams();

        comparator = new ArrayList<>(4);
        comparator.add(0, 0.0f);
        comparator.add(1, 0.0f);
        comparator.add(2, 0.0f);
        comparator.add(3, 0.0f);


    }

    public void startConnection(View view) throws ExecutionException, InterruptedException, IOException, NoSuchAlgorithmException {

        stationIP = IpInputLayout.getEditText().getText().toString();

        starter = findViewById(R.id.starter);
        starter.setVisibility(View.INVISIBLE);
        controls = findViewById(R.id.controls);
        chancontrols = findViewById(R.id.chanControls);
        chancontrols.setVisibility(View.VISIBLE);


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
                        if (stationAddress.isReachable(1000)) {

                            stationreachable = true;



                            AXSSocket = tcpo.getSocket(stationIP, AXSPort);

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


                        } else {
                            stationreachable = false;
                            startError(getApplicationContext(), "Check your Wi-Fi connection");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    if (AXSSocket == null) {

                        AXSexists = false;
//
                    } else {
                        AXSexists = true;
                        try {
                            tcpo.sendTCP(AXSSocket, po.makeAXSCreq());
                            AXSdata = po.parseAXSCrep(tcpo.recieveTCP(AXSSocket));

                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    IRexists = IRVideoSocket != null;

                    TVOexists = TVOVideoSocket != null;

                    TVIexists = TVIVideoSocket != null;

                    if (TVOexists) {

                        try {
                            tcpo.sendTCP(TVOOMUSocket, po.makeTVOCreq(aefMid));
                            TVOOMUdata = po.parseTVOOMUCrep(tcpo.recieveTCP(TVOOMUSocket));


                            tcpo.sendTCP(TVOLensSocket, po.makeTVOCreq(lensMid));
                            TVOLensdata = po.parseTVOLensCrep(tcpo.recieveTCP(TVOLensSocket));

                            tcpo.sendTCP(TVOCamSocket, po.makeTVOCreq(cameraMid));
                            TVOCameradata = po.parseTVOCamCrep(tcpo.recieveTCP(TVOCamSocket));

                            activeCam = activeChan.TVO;

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }


                    switch (activeCam) {

                        case TVO:
                            Button TVOZIB = findViewById(R.id.TVOZoomInButton);
                            Button TVOZOB = findViewById(R.id.TVOZoomOutButton);
                            Button TVOFIB = findViewById(R.id.TVOFocusInButton);
                            Button TVOFOB = findViewById(R.id.TVOFocusOutButton);
                            TVOZIB.setOnTouchListener(ZoomInTouchListener);
                            TVOZOB.setOnTouchListener(ZoomOutTouchListener);
                            TVOFIB.setOnTouchListener(FocusInTouchListener);
                            TVOFOB.setOnTouchListener(FocusOutTouchListener);
                    }



                } else {
                    stationreachable = false;
                    startError(getApplicationContext(), "WiFi not enabled.");
                }
                startexecuted = true;
                interStater();
            }

        });


        WiFiThread = new Thread(new Runnable() {

            @Override

            public void run() {



                while (wfm != null && wfm.isWifiEnabled()) {


                    if (stationreachable) {
                        try {
                            if (stationAddress.isReachable(1000)) {
                                stationreachable = true;

                            } else {
                                stationreachable = false;
                                startError(getApplicationContext(), "Station unreachable");


                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }


            }
        });


        controlThread = new Thread(new Runnable() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void run() {
                while (stationreachable){

                    try {
                        AXSControl();
                        LensControl();
                        Thread.sleep(100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


        VideoThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (stationreachable) {
                    switch (activeCam) {
                        case IR:
                            activeVideoSocket = IRVideoSocket;
                            break;
                        case TVI:
                            activeVideoSocket = TVIVideoSocket;
                            break;
                        case TVO:
                            activeVideoSocket = TVOVideoSocket;
                            break;
                    }

                    final Bitmap settingFrame = getVideo(activeVideoSocket);
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
        if (startexecuted) {
            if (TVOexists) {
                activeCam = activeChan.TVO;
            } else {
                if (TVIexists) {
                    activeCam = activeChan.TVI;
                } else {
                    activeCam = activeChan.IR;
                }
            }
            WiFiThread.setDaemon(true);
            WiFiThread.start();
            VideoThread.setDaemon(true);
            VideoThread.start();

            controlThread.start();


        }

      }


    public void AXSControl() throws IOException, NoSuchAlgorithmException {



        tcpo.sendTCP(AXSSocket, po.AXSmakeMreq(AXSdata, 1.2f, xs, xy));

        recievedPosition = po.parseMrep(tcpo.recieveTCP(AXSSocket));
        /*positionTextHandler.post(new Runnable() {
            @Override
            public void run() {*/
                updateCurrentPosition(recievedPosition);
            }
        /*});*/



    public void LensControl() throws IOException {
        switch (activeCam) {
            case TVO:
                tcpo.sendTCP(TVOLensSocket, po.makeTVOLensMreq(TVOLensdata, TVOZoom, TVOFocus));
                break;
        }
    }





    public void startError(Context curContext, String error) {
        Intent errorIntent = new Intent(curContext, error_window.class);
        errorIntent.putExtra(EXTRA_MESSAGE_ERR_NO, error);
        curContext.startActivity(errorIntent);
    }

    @SuppressLint("SetTextI18n")
    public void updateCurrentPosition(String curPos) {
        String[] positions = curPos.split(";");
        String curPosX = positions[0].substring(0, positions[0].indexOf(".") + 2);
        String curPosY = positions[1].substring(0, positions[1].indexOf(".") + 2);
        curpossum = "X position: " + curPosX + "\t" + "Y position: " + curPosY;
        Message message = Message.obtain();
        message.obj = curpossum;
    positionTextHandler.sendMessage(message);
    }

    public Bitmap getVideo(Socket inputSocket) {
        if (inputSocket != null) {
            byte[] pic = new byte[0];
            try {
                pic = tcpo.recieveTCP(inputSocket);
                }
                catch (IOException e) {
                e.printStackTrace();
            }

            frames = BitmapFactory.decodeByteArray(pic, 0, pic.length);

        }
        return frames;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setFrame(Bitmap picture) {

        frameHolder.setImageBitmap(picture);

        final GestureDetector gd = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                //your action here for double tap e.g.
                Log.d("OnDoubleTapListener", "onDoubleTap");


                return true;
            }


            @Override
            public boolean onDown(MotionEvent event) {
                Log.d("TAG", "onDown: ");

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
                    if (event.getX() < (((v.getWidth() / 2) - (v.getWidth() * 0.1))) || event.getX() > ((v.getWidth() / 2) + (v.getWidth() * 0.1))) {

                        if (event.getX() > (v.getWidth() / 2)) {
                            xs = 1;
                        } else {
                            xs = -1;
                        }
                    } else {
                        xs = 0;
                    }
                    if (event.getY() < (((v.getHeight() / 2) - (v.getHeight() * 0.1))) || event.getY() > ((v.getHeight() / 2) + (v.getHeight() * 0.1))) {

                        if (event.getY() > (v.getHeight() / 2)) {
                            xy = -1;
                        } else {
                            xy = 1;
                        }
                    } else {
                        xy = 0;
                    }

                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    Log.d("TouchTest", "Touch up");
                    xs = 0;
                    xy = 0;

                }

                return gd.onTouchEvent(event);

            }


        };

        frameHolder.setOnTouchListener(touchListener);
    }


    View.OnTouchListener ZoomInTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // pass the events to the gesture detector
            // a return value of true means the detector is handling it
            // a return value of false means the detector didn't
            // recognize the event

            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Log.d("TouchTest", "Touch down");
                TVOZoom = 1;
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Log.d("TouchTest", "Touch up");
                TVOZoom = 0;
            }


            // Log.i("touch", String.valueOf(gd.onTouchEvent(event)));
            return onTouchEvent(event);

        }
    };

        View.OnTouchListener ZoomOutTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // pass the events to the gesture detector
                // a return value of true means the detector is handling it
                // a return value of false means the detector didn't
                // recognize the event

                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    Log.d("TouchTest", "Touch down");
                    TVOZoom = -1;
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    Log.d("TouchTest", "Touch up");
                    TVOZoom = 0;
                }


                // Log.i("touch", String.valueOf(gd.onTouchEvent(event)));
                return onTouchEvent(event);

            }


        };


    View.OnTouchListener FocusInTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // pass the events to the gesture detector
            // a return value of true means the detector is handling it
            // a return value of false means the detector didn't
            // recognize the event

            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Log.d("TouchTest", "Touch down");
                TVOFocus = 1;
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Log.d("TouchTest", "Touch up");
                TVOFocus = 0;
            }


            // Log.i("touch", String.valueOf(gd.onTouchEvent(event)));
            return onTouchEvent(event);

        }
    };

    View.OnTouchListener FocusOutTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // pass the events to the gesture detector
            // a return value of true means the detector is handling it
            // a return value of false means the detector didn't
            // recognize the event

            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Log.d("TouchTest", "Touch down");
                TVOFocus = -1;
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Log.d("TouchTest", "Touch up");
                TVOFocus = 0;
            }


            // Log.i("touch", String.valueOf(gd.onTouchEvent(event)));
            return onTouchEvent(event);

        }


    };




        public void openIRCover(View view) {

            new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_OPEN);

        }

        public void closeIRCover(View view) {

            new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_CLOSE);

        }





        @Override
        protected void onDestroy() {
            super.onDestroy();
            new IRCoverAsyncTask(IROMUSocket, IROMUdata).execute(Aef.MREQ.Lid.LID_CLOSE);
            WiFiThread.interrupt();
            VideoThread.interrupt();
            controlThread.interrupt();
        }

void interStater (){
            startCheck.interrupt();
}

}

