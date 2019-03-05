package ru.linkos.veresk_controller;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;

public class getSocketAsyncTask extends AsyncTask<Void, Void, Socket> {

    private final String targetIp;
    private final String targetPort;
    Socket conSocket;
    public Socket connectTCP(String ip, String targetPort) throws IOException {
        // Log.i("Socket ", String.valueOf(mySocket));
        return new Socket(ip, Integer.decode(targetPort));
    }

    public getSocketAsyncTask(final String ip, final String port) {

        targetIp = ip;
        targetPort = port;

    }



    @Override
    protected Socket doInBackground(Void... voids) {
        try {
            conSocket = connectTCP(targetIp,targetPort);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //Log.i("Socket: ", String.valueOf(conSocket));
        return conSocket;
    }

    @Override
    protected void onPostExecute(Socket socket) {
        super.onPostExecute(socket);
        //  start_window.updateSocket(socket);

    }

}
