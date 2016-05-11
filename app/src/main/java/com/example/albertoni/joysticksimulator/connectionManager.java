package com.example.albertoni.joysticksimulator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class ConnectionManager{
    private Socket socket = null;
    private Joystick.Direction previousDirection = Joystick.Direction.NEUTRAL;
    private OutputStream exit = null;
    public boolean isConnected(){
        return (socket != null);
    }

    /**
     * Connects to an IP
     * @param address A string representing an IP, as in "192.168.0.2" or a hostname like "example.org"
     * @return True if succeeded, false if not
     */
    public boolean connect(String address){
        try {
            socket = new Connection().execute(address).get();
            if (socket != null){
                exit = socket.getOutputStream();
                return true;
            }else{
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket = null;
        return false;
    }

    public void sendState(Joystick.Direction direction) throws IllegalStateException {
        if (!isConnected()){throw new IllegalStateException();}

        if (direction != previousDirection){ // if it's the same we don't need to send it
            previousDirection = direction;
            try {
                exit.write(direction.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close(){
        if(!isConnected()){return;}
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

