package ru.linkos.veresk_controller;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class getDeviceListAsyncTask extends AsyncTask<List<Socket>,Void,List<List<Integer>>> {
List<Socket> sockets;
List<byte[]> creps;
TCPOperations tcpo = new TCPOperations();
protobufOperations pto = new protobufOperations();

    @Override
    protected List<List<Integer>> doInBackground(List<Socket>... lists) {

            sockets = lists[0];
            for (int i =0; i < sockets.size(); i++ ){
              if (sockets.get(i) != null){
                  try {
                      tcpo.sendTCP(sockets.get(i), pto.makeAXSCreq());
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                 // creps.add(i, pto.pa );
              }

            }



        return null;
    }
}
