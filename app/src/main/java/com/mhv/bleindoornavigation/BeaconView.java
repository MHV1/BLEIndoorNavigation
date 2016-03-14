package com.mhv.bleindoornavigation;

import android.content.Context;
import android.graphics.PointF;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BeaconView extends LinearLayout {
    private RelativeLayout.LayoutParams layoutParams;
    private TextView distanceToUser;
    private TextView beaconDescription;
    private String beaconAddress;

    public BeaconView(Context context) {
        super(context);
        inflate(getContext(), R.layout.beacon_view, this);
        distanceToUser = (TextView) this.findViewById(R.id.distanceToUser);
        beaconDescription = (TextView) this.findViewById(R.id.beaconDescription);
        layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
    }

    public void setPosition(PointF position) {
        int x = (int) position.x;
        int y = (int) position.y;
        layoutParams.setMargins(x - 250, y - 250, 0, 0);
        setLayoutParams(layoutParams);
    }

    public void setBeaconDescription(String beaconDescription) {
        this.beaconDescription.setText(beaconDescription);
    }

    public void setDistanceToUser(String distanceToUser) {
        this.distanceToUser.setText(distanceToUser);
    }

    public void setBeaconAddress(String beaconAddress) {
        this.beaconAddress = beaconAddress;
    }

    public String getBeaconAddress() {
        return beaconAddress;
    }
}
