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
	final private int mDisplayWidth;
	final private int mDisplayHeight;
	final private Paint paint = new Paint();
	private Coordinates mCurrent;
	private int pointerRadius = 30;

	public MapPointer(Context context) {
		super(context);

		//Display Width and Height
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics displayMetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(displayMetrics);
		mDisplayWidth = displayMetrics.widthPixels;
		mDisplayHeight = displayMetrics.heightPixels;

		//Initial Pointer position on the screen
		float x = 580.0f;
		float y = 1550.0f;
		mCurrent = new Coordinates(x, y);

		//Distance covered by pointer
		float dx = ((float) 400 / mDisplayWidth) * 100;
		float dy = ((float) 400 / mDisplayHeight) * 100;
		mDxDy = new Coordinates(dx, dy);
	}

	public void setPointerPosition(Coordinates position) {
		mCurrent = position;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Coordinates tmp = mCurrent.getCoords();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		paint.setColor(Color.RED);
		canvas.drawCircle(tmp.mX, tmp.mY, pointerRadius, paint);
	}

	protected boolean move(String direction, String movement) {
		switch (direction) {
			case "E":
				if (movement.equals("Forth")) {
					mCurrent = mCurrent.moveY(mDxDy, 1);
					return true;
				} else if (movement.equals("Back")) {
					mCurrent = mCurrent.moveY(mDxDy, -1);
					return true;
				} else {
					return false;
				}

			case "W":
				//mCurrent = mCurrent.moveY(mDxDy, -1);
				if (movement.equals("Forth")) {
					mCurrent = mCurrent.moveY(mDxDy, -1);
					return true;
				} else if (movement.equals("Back")) {
					mCurrent = mCurrent.moveY(mDxDy, 1);
					return true;
				} else {
					return false;
				}



			case "N":
				//mCurrent = mCurrent.moveX(mDxDy, 1);
				if (movement.equals("Forth")) {
					mCurrent = mCurrent.moveX(mDxDy, 1);
					return true;
				} else if (movement.equals("Back")) {
					mCurrent = mCurrent.moveX(mDxDy, -1);
					return true;
				} else {
					return false;
				}



			default:
				//mCurrent = mCurrent.moveX(mDxDy, -1);
				if (movement.equals("Forth")) {
					mCurrent = mCurrent.moveX(mDxDy, -1);
					return true;
				} else if (movement.equals("Back")) {
					mCurrent = mCurrent.moveX(mDxDy, 1);
					return true;
				} else {
					return false;
				}
		}
	}
}
