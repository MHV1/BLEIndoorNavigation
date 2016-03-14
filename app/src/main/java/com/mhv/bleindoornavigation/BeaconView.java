package com.mhv.bleindoornavigation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class BeaconView extends ImageView {
    RelativeLayout.LayoutParams layoutParams;

    public BeaconView(Context context) {
        super(context);

        setImageDrawable(getResources().getDrawable(R.drawable.triangle, context.getTheme()));
        setScaleType(ScaleType.CENTER_INSIDE);
        setBackgroundColor(Color.GRAY);
        layoutParams = new RelativeLayout.LayoutParams(150, 150);
        setLayoutParams(layoutParams);
    }

    public void setPosition(PointF position) {
        int x = (int) position.x;
        int y = (int) position.y;
        layoutParams.setMargins(x - layoutParams.width, y - layoutParams.height, 0, 0);
        setLayoutParams(layoutParams);
    }
}
