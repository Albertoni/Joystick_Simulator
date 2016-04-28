package com.example.albertoni.joysticksimulator;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Joystick extends AppCompatActivity {
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
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
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
    private int screenWidth;
    private int screenHeight;
    private int centerWidth;
    private int centerHeight;

    // How far from the center should be a neutral zone
    private final int neutralZoneSize = 40;
    // precalculate the square for later neutral zone circle
    private final int neutralZoneCircle = neutralZoneSize * neutralZoneSize;

    private double tangentFirstQuadrant;
    private double tangentSecondQuadrant;

    // We have 8 directions, 360° / 8 = 45°
    // It's divided by two because later on we'll divide the screen in 4 parts to make the math
    // easier; Each part has a whole diagonal direction and two halves of the straight directions.
    private final double firstAngle = 22.5;
    private final double secondAngle = 22.5 + 45.0;

    private enum Direction {
      LEFT, UPLEFT, UP, UPRIGHT, RIGHT, DOWNRIGHT, DOWN, DOWNLEFT, NEUTRAL;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_joystick);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);


        debugText = (TextView) findViewById(R.id.textView);

        // Getting the size of the screen
        Point size = new Point();
        WindowManager w = getWindowManager();
        w.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // The program can only be used in landscape mode, so the width must be the bigger one:
        if(screenWidth < screenHeight){
            int temp = screenHeight;
            screenWidth = screenHeight;
            screenWidth = temp;
        }
        centerHeight = screenHeight / 2;
        centerWidth = screenWidth / 2;

        // Precalculate the tangents dividing the areas because performance
        // This isn't the demoscene but we have battery to save here
        tangentFirstQuadrant = Math.tan(Math.toRadians(firstAngle));
        tangentSecondQuadrant = Math.tan(Math.toRadians(secondAngle));
    }


    /**
     * This is where we get the touch and process it to figure out where it is on the screen
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
                quadrant += (y > centerHeight ? TOP : BOTTOM);

                // Now we get the x/y distance from the center so we can calculate in which of the 8 directions we are
                int xDist = Math.abs(x - centerWidth);
                int yDist = Math.abs(y - centerHeight);

                if ((xDist * xDist) + (yDist + yDist) < neutralZoneCircle){ // If it's inside the neutral zone circle...
                    pressedDirection = Direction.NEUTRAL;
                }else{
                    boolean isPastFirstAngle = isInsideTriangle(xDist, yDist, firstAngle);
                    boolean isPastSecondAngle = isInsideTriangle(xDist, yDist, secondAngle);

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
        }

        // TODO: Send direction to server

        return false;
    }

    private boolean isInsideTriangle(int x, int y, double tangent){
        return y < x * tangent;
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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
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