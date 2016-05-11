package com.example.albertoni.joysticksimulator;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.Socket;

public class Connection extends AsyncTask<String, Void, Socket> {
    Socket socket = null;

    @Override
    protected Socket doInBackground(String... address) {
        try {
            final int portNumber = 49000;
            socket = new Socket(address[0], portNumber);
            socket.setKeepAlive(true);
            return socket;
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
