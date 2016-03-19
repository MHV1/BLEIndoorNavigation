package com.mhv.bleindoornavigation;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BeaconView extends LinearLayout {
    private RelativeLayout.LayoutParams layoutParams;
    private TextView distanceToUser;
    private TextView beaconDescription;
    private String address;
    private int positionX;
    private int positionY;

    public BeaconView(Context context) {
        super(context);
        inflate(getContext(), R.layout.beacon_view, this);
        distanceToUser = (TextView) this.findViewById(R.id.distanceToUser);
        beaconDescription = (TextView) this.findViewById(R.id.beaconDescription);
        layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
    }

    public void setPosition(MotionEvent event) {
        int offset = 250;
        positionX = (int) event.getX();
        positionY = (int) event.getY();
        layoutParams.setMargins(positionX - offset, positionY - offset, 0, 0);
        setLayoutParams(layoutParams);
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setBeaconDescription(String beaconDescription) {
        this.beaconDescription.setText(beaconDescription);
    }

    public void setDistanceToUser(String distanceToUser) {
        this.distanceToUser.setText(distanceToUser);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
