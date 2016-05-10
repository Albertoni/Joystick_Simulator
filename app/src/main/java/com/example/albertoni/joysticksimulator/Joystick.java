package com.example.albertoni.joysticksimulator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Joystick extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            /*ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }*/
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

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

    // To ignore inputs while not connected
    private boolean isConnected = false;

    private enum Direction {
      LEFT, UPLEFT, UP, UPRIGHT, RIGHT, DOWNRIGHT, DOWN, DOWNLEFT, NEUTRAL
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_joystick);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

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
        if (!isConnected){
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

        // TODO: Send direction to server

        return true;
    }

    private boolean isInsideTriangle(int x, int y, double tangent){
        return y > x * tangent;
    }



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        /*ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);*/
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}

