package ru.linkos.veresk_controller;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class TCPOperations {

    public void sendTCP(Socket targetSocket, byte[] data) throws IOException {

        int len = data.length;
        int sen = Integer.reverseBytes(len);
        OutputStream out = targetSocket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(sen);
        if (len > 0) {
            dos.write(data, 0, len);
           // Log.i("Data sent: ", Arrays.toString(data));
            dos.flush();
        }

    }


    public byte[] recieveTCP(Socket targetSocket) throws IOException {
        InputStream in = targetSocket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        int buf = dis.readInt();
        int len = Integer.reverseBytes(buf);
        byte[] data = new byte[len];
        if (len > 0) {
            dis.readFully(data);
        }
       // Log.i("Data sent: ", Arrays.toString(data));
        return data;

    }

}
