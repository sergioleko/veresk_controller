package ru.linkos.veresk_controller;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Linkos.RTC.Message.AEF.Aef;
import Linkos.RTC.Message.AXS.Axs;
import Linkos.RTC.Message.GenericOuterClass;
import Linkos.RTC.Message.Range;

public class protobufOperations {

    boolean xPosLoop;
    boolean yPosLoop;
    double xposmin;
    double xposmax;
    double yposmin;
    double yposmax;
    double curXpos;
    double curYpos;
    double xspdmax;
    double yspdmax;
    List<Integer> dataList = new ArrayList<>();
    List<Integer> IROMUdataList = new ArrayList<>();


    public byte[] makeSreqProto () throws IOException {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();

        gocB.getDefaultInstanceForType();
        gocB.setMid(1398292803);


        GenericOuterClass.SREQ.Builder srqB = GenericOuterClass.SREQ.newBuilder();
        srqB.getDefaultInstanceForType();
        gocB.setSreq(srqB);
        srqB.clear();
        gocB.setSreq(srqB);



        byte[] os = gocB.build().toByteArray();

        return os;


    }

    public int parseSrepProto(byte[] incoming) throws InvalidProtocolBufferException {

        GenericOuterClass.Generic input = GenericOuterClass.Generic.parseFrom(incoming);
//        int gotmid = input.getMid();

        GenericOuterClass.SREP srep = input.getSrep();



        int statusOK = 0;
        if (srep.getReady()){
            statusOK = 1;
            if (srep.getBusy()){
                statusOK = 2;
            }
        }
        //Log.i("status: ", String.valueOf(statusOK));
        return statusOK;
    }

    public byte[] makeCreq() {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();

        gocB.getDefaultInstanceForType();
        gocB.setMid(1398292803);
        GenericOuterClass.CREQ.Builder creq = GenericOuterClass.CREQ.newBuilder();
        creq.getDefaultInstanceForType();
        gocB.setCreq(creq);
        Log.i("Creq ", Arrays.toString(gocB.build().toByteArray()));
        return gocB.build().toByteArray();

    }

    public void parseCrep(byte[] incoming) throws InvalidProtocolBufferException, NoSuchAlgorithmException {
        GenericOuterClass.Generic input = GenericOuterClass.Generic.parseFrom(incoming);

        if (input.hasCrep()) {
            Log.i("Crep", "Yes");
            GenericOuterClass.CREP crep = input.getCrep();
            Axs.CREP AxsCrep = crep.getAxs();

            xPosLoop = AxsCrep.getXpositionLoop();
            yPosLoop = AxsCrep.getYpositionLoop();
            Range.range_d rangex = AxsCrep.getXposition();
            Range.range_d rangey = AxsCrep.getYposition();
            Range.range_d spdx = AxsCrep.getXspeed();
            Range.range_d spdy = AxsCrep.getYspeed();
            xposmin = rangex.getMin();
            xposmax = rangex.getMax();
            yposmin = rangey.getMin();
            yposmax = rangey.getMax();
            xspdmax = spdx.getMax();
            yspdmax = spdy.getMax();

            byte[] hash = MessageDigest.getInstance("MD5").digest(incoming);
            dataList.clear();
            for (int i = 0; i < hash.length; i += 4) {
                int floatBits = hash[i] & 0xFF |
                        (hash[i + 1] & 0xFF) << 8 |
                        (hash[i + 2] & 0xFF) << 16 |
                        (hash[i + 3] & 0xFF) << 24;

                dataList.add(floatBits);




            }


        }
    }

    public String parseSrep(byte[] bytes) throws InvalidProtocolBufferException {


        GenericOuterClass.Generic input = GenericOuterClass.Generic.parseFrom(bytes);
        if (input.hasSrep()){
            GenericOuterClass.SREP srep = input.getSrep();
            Axs.SREP axsSrep = srep.getAxs();

            curXpos = axsSrep.getXposition();
            curYpos = axsSrep.getYposition();

            Log.i ("Cur pos:", curXpos + "\t" + curYpos);
        }
        return String.valueOf(curXpos) +";"+ String.valueOf(curYpos);
    }

    public byte[] makeSreq() {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();

        gocB.getDefaultInstanceForType();
        gocB.setMid(1398292803);
        GenericOuterClass.SREQ.Builder sreq = GenericOuterClass.SREQ.newBuilder();
        sreq.getDefaultInstanceForType();
        gocB.setSreq(sreq);

        byte[] sreqpacket = gocB.build().toByteArray();
        return sreqpacket;


    }

    public byte[] makeMreq(int Kspeed, boolean x, boolean y, boolean up)  {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();
        gocB.getDefaultInstanceForType();
        gocB.setMid(1398292803);
        GenericOuterClass.MREQ.Builder mreq = GenericOuterClass.MREQ.newBuilder();

        mreq.setMd5A(dataList.get(0));
         //Log.i("MD5A", String.valueOf(dataList.get(0)));
        mreq.setMd5B(dataList.get(1));
        //Log.i("MD5B", String.valueOf(dataList.get(1)));
        mreq.setMd5C(dataList.get(2));
        //Log.i("MD5C", String.valueOf(dataList.get(2)));
        mreq.setMd5D(dataList.get(3));
        //Log.i("MD5D", String.valueOf(dataList.get(3)));
        mreq.setPriority(0);


        Axs.MREQ.Builder axsMreq = Axs.MREQ.newBuilder();
        double xpseed = xspdmax / Kspeed;
        double yspeed = yspdmax / Kspeed;
        if (x){
            if (up){
                axsMreq.setXspeed(xpseed);
            }
            else {
                axsMreq.setXspeed(-xpseed);
            }
        }
        else {
            axsMreq.setXspeed(0);
        }
        if (y){
            if (up){
                axsMreq.setYspeed(yspeed);
            }
            else {
                axsMreq.setYspeed(-yspeed);
            }
        }
        else {
            axsMreq.setYspeed(0);
        }

        mreq.setAxs(axsMreq.build());
        gocB.setMreq(mreq.build());
        return gocB.build().toByteArray();

    }


    public String parseMrep(byte[] bytes) throws InvalidProtocolBufferException {


        GenericOuterClass.Generic input = GenericOuterClass.Generic.parseFrom(bytes);
        if (input.hasMrep()) {
            GenericOuterClass.SREP mrep = input.getMrep();

            Axs.SREP axsSrep = mrep.getAxs();

            curXpos = axsSrep.getXposition();
            curYpos = axsSrep.getYposition();

            //Log.i("Cur pos:", curXpos + "\t" + curYpos);

            return String.valueOf(curXpos) + ";" + String.valueOf(curYpos);
        }

        // Log.i("Status", String.valueOf(input.getMrep().getReady()) +  String.valueOf(input.getMrep().getBusy()));
        //  Log.i ("Status:", String.valueOf(mrep.getReady()) + "\t" + String.valueOf(mrep.getBusy()));

        else{
            Log.i("Status:", "No mrep");
            return "0;0";
        }
    }

    public byte[] sendAEFMreq (Aef.MREQ.Lid lid, List<Integer> IROMUdataList) throws InvalidProtocolBufferException {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();
        gocB.getDefaultInstanceForType();
        gocB.setMid(1178943811);
        GenericOuterClass.MREQ.Builder mreq = GenericOuterClass.MREQ.newBuilder();
        mreq.setMd5A(IROMUdataList.get(0));
        //Log.i("MD5A", String.valueOf(dataList.get(0)));
        mreq.setMd5B(IROMUdataList.get(1));
        //Log.i("MD5B", String.valueOf(dataList.get(1)));
        mreq.setMd5C(IROMUdataList.get(2));
        //Log.i("MD5C", String.valueOf(dataList.get(2)));
        mreq.setMd5D(IROMUdataList.get(3));
        //Log.i("MD5D", String.valueOf(dataList.get(3)));
        mreq.setPriority(0);
        Aef.MREQ.Builder aefMreq = Aef.MREQ.newBuilder();
       // Aef.MREQ.Lid lidlid = Aef.MREQ.Lid.valueOf(lid);
        aefMreq.setLid(lid);
        Log.i("Lid: ", String.valueOf(lid));
        mreq.setAef(aefMreq.build());
        gocB.setMreq(mreq.build());
        Log.i("return: ", Arrays.toString(gocB.build().toByteArray()));
        return gocB.build().toByteArray();




    }

    public List<Integer> parseIROMUCrep(byte[] incoming) throws InvalidProtocolBufferException, NoSuchAlgorithmException {
        GenericOuterClass.Generic input = GenericOuterClass.Generic.parseFrom(incoming);

        if (input.hasCrep()) {
            Log.i("Crep", "Yes");
            GenericOuterClass.CREP crep = input.getCrep();
            Aef.CREP AefCrep = crep.getAef();



            byte[] hash = MessageDigest.getInstance("MD5").digest(incoming);
            Log.i("Hash len: ", String.valueOf(hash.length));
            IROMUdataList.clear();
            for (int i = 0; i < hash.length; i += 4) {
                Log.i("i: ", String.valueOf(i));
                int floatBits = hash[i] & 0xFF |
                        (hash[i + 1] & 0xFF) << 8 |
                        (hash[i + 2] & 0xFF) << 16 |
                        (hash[i + 3] & 0xFF) << 24;

                IROMUdataList.add(floatBits);





            }
            Log.i("dataListlen: ", String.valueOf(IROMUdataList.size()));


        }
        else {
            Log.e("No crep", " recieved");
        }
        return IROMUdataList;
    }


    public byte[] makeIROMUCreq() {

        GenericOuterClass.Generic.Builder gocB = GenericOuterClass.Generic.newBuilder();

        gocB.getDefaultInstanceForType();
        gocB.setMid(1178943811);
        GenericOuterClass.CREQ.Builder creq = GenericOuterClass.CREQ.newBuilder();
        creq.getDefaultInstanceForType();
        gocB.setCreq(creq);
        Log.i("Creq ", Arrays.toString(gocB.build().toByteArray()));
        return gocB.build().toByteArray();

    }

    }


