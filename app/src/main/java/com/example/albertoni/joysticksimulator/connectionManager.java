package com.example.albertoni.joysticksimulator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionManager {
    private Socket socket = null;
    private Joystick.Direction previousDirection = Joystick.Direction.NEUTRAL;
    private OutputStream exit = null;
    public boolean isConnected(){
        return (socket == null);
    }

    /**
     * Connects to an IP
     * @param address A string representing an IP, as in "192.168.0.2" or a hostname like "example.org"
     * @return True if succeeded, false if not
     */
    public boolean connect(String address){
        try {
            int portNumber = 49000;
            socket = new Socket(address, portNumber);
            socket.setKeepAlive(true);
            exit = socket.getOutputStream();
        } catch (IOException e){
            socket = null;
            return false;
        }
        return true;
    }

    public void sendState(Joystick.Direction direction) throws IllegalStateException {
        if (!isConnected()){throw new IllegalStateException();}

        if (direction != previousDirection){ // if it's the same we don't need to send it
            try {
                exit.write(direction.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
