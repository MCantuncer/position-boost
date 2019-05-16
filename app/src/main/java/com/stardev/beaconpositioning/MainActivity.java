package com.stardev.beaconpositioning;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.github.pwittchen.reactivebeacons.library.rx2.Beacon;

public class MainActivity extends BeaconActivity {
    private final String TAG = "MainActivity";
    private ImageView imageView;

    public static final float REAL_WIDTH = 8.24f;
    public static final float REAL_HEIGHT = 8.68f;
    public static final int BEACON_ICON_SIZE = 320;

    public PointF[] fixedBeaconCoordinates = {
            new PointF(2,4),
            new PointF(7.5f,7.5f),
            new PointF(7.5f,2)
    };
    private Bitmap planBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        imageView = findViewById(R.id.imageView);

        planBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floor_plan);
        placeFixedBeacons();

        drawUserLocation(new PointF(4,4));
    }

    private void placeFixedBeacons(){
        // Create a temporary bitmap to create a canvas with
        Bitmap tempBitmap = Bitmap.createBitmap(planBitmap.getWidth(), planBitmap.getHeight(), Bitmap.Config.RGB_565);

        // Create canvas
        Canvas canvas = new Canvas(tempBitmap);

        // Draw plan bitmap
        canvas.drawBitmap(planBitmap, 0, 0, null);

        // Save canvas to restore it later
        canvas.save();

        // Reset where 0,0 is located
        canvas.translate(0, canvas.getHeight());

        // Invert canvas
        canvas.scale(1, -1);

        // Create beacon bitmap from resource
        Bitmap beaconBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.beacon);

        // Resize the beacon bitmap
        beaconBitmap = getResizedBitmap(beaconBitmap, BEACON_ICON_SIZE, BEACON_ICON_SIZE);

        Paint beaconPaint = new Paint();
        beaconPaint.setAntiAlias(true);
        beaconPaint.setFilterBitmap(true);
        beaconPaint.setDither(true);


        // For each beacon coordinate
        for (PointF point : fixedBeaconCoordinates) {
            // Convert meters to pixels
            int pixelX = (int) (point.x * planBitmap.getWidth() / REAL_WIDTH);
            int pixelY = (int) (point.y * planBitmap.getHeight() / REAL_HEIGHT);

            int centerX = pixelX - (beaconBitmap.getWidth() /2 );
            int centerY = pixelY - (beaconBitmap.getHeight() / 2);

            // Draw beacon bitmap
            canvas.drawBitmap(beaconBitmap, centerX, centerY, beaconPaint);
        }

        // Restore canvas
        canvas.restore();

        // Show new bitmap
        planBitmap = tempBitmap;
        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        imageView.invalidate();

    }

    private void drawUserLocation(PointF userCoordinates){

        // Convert meters to pixels
        int pixelX = (int) (userCoordinates.x * planBitmap.getWidth() / REAL_WIDTH);
        int pixelY = (int) (userCoordinates.y * planBitmap.getHeight() / REAL_HEIGHT);

        // Create paint for the location circle
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.RED);

        // Create a temporary bitmap create canvas with
        Bitmap tempBitmap = Bitmap.createBitmap(planBitmap.getWidth(), planBitmap.getHeight(), Bitmap.Config.RGB_565);

        // Create canvas
        Canvas canvas = new Canvas(tempBitmap);

        // Draw plan bitmap
        canvas.drawBitmap(planBitmap, 0, 0, null);

        // Save canvas to restore it later
        canvas.save();
        // Reset where 0,0 is located
        canvas.translate(0, canvas.getHeight());
        // Invert canvas
        canvas.scale(1, -1);

        // Draw location circle
        canvas.drawCircle(pixelX , pixelY,100, circlePaint);

        // Restore canvas
        canvas.restore();

        // Show new bitmap
        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        // Invalidate
        imageView.invalidate();
    }

    @Override
    protected void beaconDataArrived(Beacon beacon) {
        Log.d(TAG,"Address: "+ beacon.macAddress + " RSSI: " + beacon.rssi + " Distance: " + beacon.getDistance());
    }

}
