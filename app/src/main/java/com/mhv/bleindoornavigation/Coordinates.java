package com.mhv.bleindoornavigation;

public class Coordinates {
	float mX;
	float mY;

	public Coordinates(float x, float y) {
		mX = x;
		mY = y;
	}

	synchronized Coordinates moveX(Coordinates dxdy, int direction) {
		return new Coordinates(mX + dxdy.mX * direction, mY);
	}

	synchronized Coordinates moveY(Coordinates dxdy, int direction) {
		return new Coordinates(mX, mY + dxdy.mY * direction);
	}

	synchronized Coordinates getCoords() {
		return new Coordinates(mX, mY);
	}

	@Override
	public String toString() {
		return "(" + mX + "," + mY + ")";
	}
}
