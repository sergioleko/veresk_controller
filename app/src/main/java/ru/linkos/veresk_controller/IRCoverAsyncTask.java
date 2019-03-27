package ru.linkos.veresk_controller;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import Linkos.RTC.Message.AEF.Aef;

public class IRCoverAsyncTask extends AsyncTask<Aef.MREQ.Lid, Void, Void> {
    protobufOperations pto = new protobufOperations();
    Socket targetSocket;
    TCPOperations tcpo = new TCPOperations();
    List<Integer> hashList = new ArrayList<>();
    //Aef.MREQ.Lid = new Aef.MREQ.Lid;


    public IRCoverAsyncTask (Socket inSocket, List<Integer> hashCode){
        targetSocket = inSocket;
        hashList = hashCode;
    }


    @Override
    protected Void doInBackground(Aef.MREQ.Lid... lids) {
        try {

            tcpo.sendTCP(targetSocket, pto.sendAEFMreq(lids[0], hashList) );

        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

    }
}
