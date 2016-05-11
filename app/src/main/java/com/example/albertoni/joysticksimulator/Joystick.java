package com.example.albertoni.joysticksimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Joystick extends Activity {
    private TextView debugText;
    private int centerWidth;
    private int centerHeight;

    // How far from the center should be a neutral zone
    private final int neutralZoneSize = 40;
    // precalculate the square of the radius for later neutral zone circle
    private final int neutralZoneCircle = neutralZoneSize * neutralZoneSize;

    // We have 8 directions, 360° / 8 = 45°
    // It's divided by two because later on we'll divide the screen in 4 parts to make the math
    // easier; Each part has a whole diagonal direction and two halves of the straight directions.
    private final double firstAngle = 22.5;
    private final double secondAngle = 22.5 + 45.0;

    // Precalculate the tangents dividing the areas because performance
    // This isn't the demoscene but we have battery to save here
    private final double tangentFirstQuadrant = Math.tan(Math.toRadians(firstAngle));
    private final double tangentSecondQuadrant = Math.tan(Math.toRadians(secondAngle));

    private ConnectionManager connection = new ConnectionManager();

    public enum Direction {
      LEFT(0), UPLEFT(1), UP(2), UPRIGHT(3), RIGHT(4), DOWNRIGHT(5), DOWN(6), DOWNLEFT(7), NEUTRAL(8);

        private final byte dir;
        Direction(int dir) {
            this.dir = (byte) dir;
        }
        public byte getValue(){
            return dir;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_joystick);

        debugText = (TextView) findViewById(R.id.textView);

        // Getting the size of the screen
        Point size = new Point();
        WindowManager w = getWindowManager();
        w.getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        // The program can only be used in landscape mode, so the width must be the bigger one:
        if(screenWidth < screenHeight){
            int temp = screenHeight;
            // Thanks, intellij, but in this case this suspicious assignment is actually correct :B
            //noinspection SuspiciousNameCombination
            screenHeight = screenWidth;
            screenWidth = temp;
        }
        centerHeight = screenHeight / 2;
        centerWidth = screenWidth / 2;

        // connection button
        Button connect = (Button) findViewById(R.id.button);
        if (connect != null) {
            connect.setOnClickListener(new Button.OnClickListener(){
                public void onClick(View v){
                    EditText in = (EditText) findViewById(R.id.editText);
                    boolean result = connection.connect(in.getText().toString());
                    if (!result){
                        Toast toast = Toast.makeText(getApplicationContext(), "Unable to connect to this IP!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), "Button is null, panic", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    /**
     * This is where we get the touch and process it to figure out where it is on the screen
     * @param event A MotionEvent from the API
     * @return True if the listener has consumed the event, false otherwise. We always return true
     *         except in case of errors.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // No reason to do anything while not connected
        if (!connection.isConnected()){
            return false;
        }
        // some constants to make the following code easier to follow
        final int LEFT = 1;
        final int RIGHT = 0;
        final int TOP = 2;
        final int BOTTOM = 0;

        int quadrant = 0;
        Direction pressedDirection = Direction.NEUTRAL;

        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Figuring out which corner we are makes things easier
                quadrant += (x > centerWidth ? RIGHT : LEFT);
                quadrant += (y > centerHeight ? BOTTOM : TOP);

                // Now we get the x/y distance from the center so we can calculate in which of the 8 directions we are
                int xDist = Math.abs(x - centerWidth);
                int yDist = Math.abs(y - centerHeight);

                if ((xDist * xDist) + (yDist * yDist) < neutralZoneCircle){ // If it's inside the neutral zone circle...
                    pressedDirection = Direction.NEUTRAL;
                }else{
                    boolean isPastFirstAngle = isInsideTriangle(xDist, yDist, tangentFirstQuadrant);
                    boolean isPastSecondAngle = isInsideTriangle(xDist, yDist, tangentSecondQuadrant);

                    switch (quadrant){
                        case (LEFT + TOP):
                            if(isPastFirstAngle){
                                if (isPastSecondAngle){
                                    pressedDirection = Direction.UP;
                                }else{
                                    pressedDirection = Direction.UPLEFT;
                                }
                            }else{
                                pressedDirection = Direction.LEFT;
                            }
                            break;

                        case (LEFT + BOTTOM):
                            if(isPastFirstAngle){
                                if (isPastSecondAngle){
                                    pressedDirection = Direction.DOWN;
                                }else{
                                    pressedDirection = Direction.DOWNLEFT;
                                }
                            }else{
                                pressedDirection = Direction.LEFT;
                            }
                            break;

                        case (RIGHT + TOP):
                            if(isPastFirstAngle){
                                if (isPastSecondAngle){
                                    pressedDirection = Direction.UP;
                                }else{
                                    pressedDirection = Direction.UPRIGHT;
                                }
                            }else{
                                pressedDirection = Direction.RIGHT;
                            }
                            break;

                        case (RIGHT + BOTTOM):
                            if(isPastFirstAngle){
                                if (isPastSecondAngle){
                                    pressedDirection = Direction.DOWN;
                                }else{
                                    pressedDirection = Direction.DOWNRIGHT;
                                }
                            }else{
                                pressedDirection = Direction.RIGHT;
                            }
                            break;

                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                pressedDirection = Direction.NEUTRAL;
                break;
        }

        // DEBUG
        switch (pressedDirection){
            case DOWN:
                debugText.setText("DOWN");
                break;
            case DOWNLEFT:
                debugText.setText("DOWNLEFT");
                break;
            case DOWNRIGHT:
                debugText.setText("DOWNRIGHT");
                break;
            case LEFT:
                debugText.setText("LEFT");
                break;
            case NEUTRAL:
                debugText.setText("NEUTRAL");
                break;
            case RIGHT:
                debugText.setText("RIGHT");
                break;
            case UP:
                debugText.setText("UP");
                break;
            case UPLEFT:
                debugText.setText("UPLEFT");
                break;
            case UPRIGHT:
                debugText.setText("UPRIGHT");
                break;
            default:
                debugText.setText("FUUUUUUUUUUUUCK");
        }

        connection.sendState(pressedDirection);

        return true;
    }

    private boolean isInsideTriangle(int x, int y, double tangent){
        return y > x * tangent;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Exit?");
        builder.setMessage("Do you want to exit? ");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                connection.close();
                Joystick.super.onBackPressed();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        builder.show();
    }



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}

