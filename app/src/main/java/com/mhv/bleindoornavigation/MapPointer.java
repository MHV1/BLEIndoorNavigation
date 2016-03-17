package com.mhv.bleindoornavigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

public class MapPointer extends View {

    final private Coordinates mDxDy;
    final private Paint paint = new Paint();
    private Coordinates currentCoords;

    public MapPointer(Context context) {
        super(context);

        //Display Width and Height
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int mDisplayWidth = displayMetrics.widthPixels;
        int mDisplayHeight = displayMetrics.heightPixels;

        //Initial Pointer position on the screen (entrance)
        float x = 580.0f;
        float y = 1550.0f;
        currentCoords = new Coordinates(x, y);

        //Distance covered by pointer
        float dx = ((float) 400 / mDisplayWidth) * 100;
        float dy = ((float) 400 / mDisplayHeight) * 100;
        mDxDy = new Coordinates(dx, dy);
    }

    public void setPointerPosition(Coordinates position) {
        currentCoords = position;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Coordinates tmp = currentCoords.getCoords();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        int pointerRadius = 30;
        canvas.drawCircle(tmp.mX, tmp.mY, pointerRadius, paint);
    }

    protected boolean move(String direction, String movement) {
        switch (direction) {
            case "E":
                switch (movement) {
                    case "Forth":
                        currentCoords = currentCoords.moveY(mDxDy, 1);
                        return true;
                    case "Back":
                        currentCoords = currentCoords.moveY(mDxDy, -1);
                        return true;
                    default:
                        return false;
                }

            case "W":
                switch (movement) {
                    case "Forth":
                        currentCoords = currentCoords.moveY(mDxDy, -1);
                        return true;
                    case "Back":
                        currentCoords = currentCoords.moveY(mDxDy, 1);
                        return true;
                    default:
                        return false;
                }


            case "N":
                switch (movement) {
                    case "Forth":
                        currentCoords = currentCoords.moveX(mDxDy, 1);
                        return true;
                    case "Back":
                        currentCoords = currentCoords.moveX(mDxDy, -1);
                        return true;
                    default:
                        return false;
                }


            default:
                switch (movement) {
                    case "Forth":
                        currentCoords = currentCoords.moveX(mDxDy, -1);
                        return true;
                    case "Back":
                        currentCoords = currentCoords.moveX(mDxDy, 1);
                        return true;
                    default:
                        return false;
                }
        }
    }
}
