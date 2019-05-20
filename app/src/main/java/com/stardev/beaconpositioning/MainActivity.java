package com.stardev.beaconpositioning;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;


import org.liquidplayer.javascript.JSContext;
import org.liquidplayer.javascript.JSValue;


//import com.github.pwittchen.reactivebeacons.library.rx2.Beacon;


public class MainActivity extends BeaconActivity {
    private final String TAG = "MainActivity";
    private ImageView imageView;

    private JSContext jsContext;
    private boolean filter_initialized = false;

    private ArrayList<String> macAddresses = new ArrayList<>(24);
    private ArrayList<Double> currentRssiValues = new ArrayList<>(Collections.nCopies(24, 0.0));
    private ArrayList<Double> distances = new ArrayList<>(Collections.nCopies(24, 10000.0));
    //private ArrayList<Double> distancesWithoutFilter = new ArrayList<>(Collections.nCopies(24, 10000.0));
    //private ArrayList<Integer> macCounts = new ArrayList<>(Collections.nCopies(24, 0));
    private ArrayList<Double> previousRssiValues = new ArrayList<>(Collections.nCopies(24, -9999.0));
    private ArrayList<Double> kalmanDistances = new ArrayList<>(Collections.nCopies(24, -9999.0));



    private double current_rssi1 = 99999;
    private double current_rssi2 = 99999;
    private double current_rssi3 = 99999;
    private double current_rssi4 = 99999;

    private double txPower = -65;


    private double distance1 = 0 ;
    private double distance2 = 0;
    private double distance3 = 0;
    private double distance4 = 0;

    private int mac1 = 0;
    private int mac2 = 0;
    private int mac3 = 0;
    private int mac4 = 0;


    private double previous_rssi_beacon1 = -9999999;
    private double previous_rssi_beacon2 = -9999999;
    private double previous_rssi_beacon3 = -9999999;
    private double previous_rssi_beacon4 = -9999999;

    public static final float REAL_WIDTH = 8.24f;
    public static final float REAL_HEIGHT = 8.68f;
    public static final int BEACON_ICON_SIZE = 100;

    // HashMap for all MacAddresses

    public PointF[] fixedBeaconCoordinates = {
            new PointF(2,4),
            new PointF(7.5f,7.5f),
            new PointF(7.5f,2),
            new PointF(1f,1f)


    };


    private PointF p1 = new PointF(2,4);
    private PointF p2 = new PointF(7.5f,7.5f);
    private PointF p3 = new PointF(7.5f,2);
    private PointF p4 = new PointF(1f,1f);


    private Bitmap planBitmap;


    public static String convertStreamToString(InputStream is) throws IOException {
        // http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        Boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if(firstLine){
                sb.append(line);
                firstLine = false;
            } else {
                sb.append("\n").append(line);
            }
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (InputStream in) throws IOException {
        String ret = convertStreamToString(in);
        in.close();
        return ret;
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MacAddressesInit();

        jsContext = new JSContext();

        try{

            // Load JS code
            InputStream in = getResources().openRawResource(R.raw.kalman);
            String kalman_code = getStringFromFile(in);
            jsContext.evaluateScript(kalman_code);

            // Initialize Kalman Filter
            jsContext.evaluateScript("var kf = new KalmanFilter();");

            filter_initialized = true;

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }



/*        beaconManager = BeaconManager.getInstanceForApplication(this);


        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind((BeaconConsumer) this);

        MonitorNotifier monitorNotifier = new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {

            }

            @Override
            public void didExitRegion(Region region) {

            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        };
        beaconManager.addMonitorNotifier(monitorNotifier);

*/

        // Find views
        imageView = findViewById(R.id.imageView);

        planBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floor_plan);
        placeFixedBeacons();

        drawUserLocation(new PointF(4,4));
    }



/*
    private double kalman_filter(double input){

        // Assert that the filter is initialized
        if (BuildConfig.DEBUG && !(filter_initialized)) { throw new AssertionError() ;}

        this.jsContext.evaluateScript("var result = kf.filter("+ Double.toString(input) +");");
        JSValue result = this.jsContext.property("result");
        return result.toNumber();
    }
*/

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




    private double kalman_filter(double input){

        // Assert that the filter is initialized
        if (BuildConfig.DEBUG && !(filter_initialized)) { throw new AssertionError() ;}

        this.jsContext.evaluateScript("var result = kf.filter("+ Double.toString(input) +");");
        JSValue result = this.jsContext.property("result");
        return result.toNumber();
    }



    @Override
    protected void beaconDataArrived(Beacon beacon) {

        if(macAddresses.contains(beacon.device.toString())){
            int index = macAddresses.indexOf(beacon.device.toString());

            currentRssiValues.set(index, beacon.rssi);

            if(previousRssiValues.get(index) == -9999.0){
                previousRssiValues.set(index, beacon.rssi);
            }
            else{
                currentRssiValues.set(index, previousRssiValues.get(index) - (1 * (previousRssiValues.get(index) - currentRssiValues.get(index))));
                Log.i("RSSI - Arma", currentRssiValues.get(index) + "");
            }

            //macCounts.set(index, macCounts.get(index) + 1 );

            kalmanDistances.set(index, kalman_filter(currentRssiValues.get(index)));
            distances.set(index, Math.pow(10d, ((double) txPower - kalmanDistances.get(index)) / (10 * 1.82) ));
            //distancesWithoutFilter.set(index, Math.pow(10d, ((double) txPower - currentRssiValues.get(index)) / (10 * 1.82) ));

            for (int i = 0 ; i < distances.size() ; i++){
              if(distances.get(index) < distances.get(i)){

                double tmpDistance = distances.get(i);
                String tmpMac = macAddresses.get(i);
                double tmpKalman = kalmanDistances.get(i);
                double tmpCurrentRssi = currentRssiValues.get(i);
                double tmpPrevRssi = previousRssiValues.get(i);

                macAddresses.set(i, macAddresses.get(index));
                macAddresses.set(index, tmpMac);

                previousRssiValues.set(i, previousRssiValues.get(index));
                previousRssiValues.set(index, tmpPrevRssi);

                currentRssiValues.set(i, currentRssiValues.get(index));
                currentRssiValues.set(index, tmpCurrentRssi);

                kalmanDistances.set(i, kalmanDistances.get(index));
                kalmanDistances.set(index, tmpKalman);

                distances.set(i, distances.get(index));
                distances.set(index, tmpDistance);

                index = i;

                break;

              }
            }

            //Log.i(TAG, "Device: + " + beacon.device + " RSSI: " + currentRssiValues.get(index) + " Distance: " + distances.get(index) +  " Without Filter: " + distancesWithoutFilter.get(index) + " " + " Current counter " + index + 1 + ": " + macCounts.get(index) + " Kalman Result: " + kalmanDistances.get(index) + " " );
            Log.i(TAG, "Index: " + index +  " Device: + " + beacon.device + " RSSI: " + currentRssiValues.get(index) + " Distance: " + distances.get(index) + " Kalman Result: " + kalmanDistances.get(index) + " " );

        }
/*
        if(beacon.macAddress.address.equals("00:13:AA:00:0A:3C")) {
            current_rssi1 = beacon.rssi;
            if(previous_rssi_beacon1 == -9999999){
                previous_rssi_beacon1 = beacon.rssi;
            }
            else{
                current_rssi1 = previous_rssi_beacon1 - (0.1 * (previous_rssi_beacon1- current_rssi1));
                Log.i("RSSI - Arma", current_rssi1 + "");
            }

            mac1++;

            distance1 = Math.pow(10d, ((double) txPower - current_rssi1) / (10 * 1.82));
            Log.i(TAG, "Device: + " + beacon.device + " RSSI: " + current_rssi1 + " Distance: " + distance1  + " Current counter1: " + mac1) ;

        }

        else if(beacon.macAddress.address.equals("00:13:AA:00:11:84")) {


            current_rssi2 = beacon.rssi;
            if(previous_rssi_beacon2 == -9999999){
                previous_rssi_beacon2 = beacon.rssi;
            }
            else{
                current_rssi2 = previous_rssi_beacon2 - (0.7 * (previous_rssi_beacon2 - current_rssi2));
                Log.i("RSSI - Arma", current_rssi2 + "");
            }

            mac2++;
            distance2 = Math.pow(10d, ((double) txPower - current_rssi2) / (10 * 1.82));
            Log.i(TAG, "Device: + " + beacon.device + " RSSI: " + current_rssi2 + " Distance: " + distance2 + " Current counter2: " + mac2) ;

        }
        else if(beacon.macAddress.address.equals("00:13:AA:00:09:C4")) {

            current_rssi3 = beacon.rssi;
            if(previous_rssi_beacon3 == -9999999){
                previous_rssi_beacon3 = beacon.rssi;
            }
            else{
                current_rssi3 = previous_rssi_beacon3 - (0.7 * (previous_rssi_beacon3 - current_rssi3));
                Log.i("RSSI - Arma", current_rssi3 + "");
            }

            distance3 = Math.pow(10d, ((double) txPower - current_rssi3) / (10 * 1.82));
            mac3++;
            Log.i(TAG, "Device: + " + beacon.device + " RSSI: " + current_rssi3 + " Distance: " + distance3 + " Current counter2: " + mac3) ;

        }
        else if(beacon.macAddress.address.equals("00:15:83:40:B2:8C")) {


            current_rssi4 = beacon.rssi;
            if(previous_rssi_beacon4 == -9999999){
                previous_rssi_beacon4 = beacon.rssi;
            }
            else{
                current_rssi4 = previous_rssi_beacon4 - (0.7 * (previous_rssi_beacon4 - current_rssi4));
                Log.i("RSSI - Arma", current_rssi4 + "");
            }

            distance4 = Math.pow(10d, ((double) txPower - current_rssi4) / (10 * 1.82));
            mac4++;
            Log.i(TAG, "Device: + " + beacon.device + " RSSI: " + current_rssi4 + " Distance: " + distance4 + " Current counter2: " + mac4) ;
        }

        //Log.d(TAG,"Address: "+ beacon.macAddress + " RSSI: " + beacon.rssi + " Distance: " + beacon.getDistance());

*/




        if(current_rssi1 != 99999 && current_rssi2 != 99999 && current_rssi3 != 99999 && current_rssi4 != 99999){

            float A = (float) (Math.pow(distance2, 2) - Math.pow(distance1, 2) - (Math.pow(p2.x, 2)) + (Math.pow(p1.x, 2)) - (Math.pow(p2.y, 2)) + (Math.pow(p1.y, 2)));
            float B = (float) (Math.pow(distance3, 2) - Math.pow(distance1, 2) - (Math.pow(p3.x, 2)) + (Math.pow(p1.x, 2)) - (Math.pow(p3.y, 2)) + (Math.pow(p1.y, 2)));
            float delta = (float) ((4.0d) * (p1.x - p2.x) * (p1.y - p3.y) - (p1.x - p3.x) * (p1.y - p2.y));
            float x0 = (float) ((1.0d / delta) * (2.0d * A * (p1.y - p3.y) - 2.0d * B * (p1.y - p2.y)));
            float y0 = (float) ((1.0d / delta) * (2.0d * B * (p1.x - p2.x) - 2.0d * A * (p1.x - p3.x)));

            Log.i("Ucgen distance", "(x,y): " + x0 + ", " + y0);

        }




    /*

            Double A = (d2 * d2) - (d1 * d1) - (x2 * x2) + (x1 * x1) - (y2 * y2) + (y1 * y1);
            Double B = (d3 * d3) - (d1 * d1) - (x3 * x3) + (x1 * x1) - (y3 * y3) + (y1 * y1);
            Double delta = (4.0d) * ((x1 - x2) * (y1 - y3) - (x1 - x3) * (y1 - y2));
            Double x0 = (1.0d / delta) * (2.0d * A * (y1 - y3) - 2.0d * B * (y1 - y2));
            Double y0 = (1.0d / delta) * (2.0d * B * (x1 - x2) - 2.0d * A * (x1 - x3));

     */



    }

    public String TriangleFunction(){


        for (int i = 0 ; i < 4; i++) {
            if(distances.get(i) == 10000) {
              return null;
            }
        }

            float A = (float) (Math.pow(distance2, 2) - Math.pow(distance1, 2) - (Math.pow(p2.x, 2)) + (Math.pow(p1.x, 2)) - (Math.pow(p2.y, 2)) + (Math.pow(p1.y, 2)));
            float B = (float) (Math.pow(distance3, 2) - Math.pow(distance1, 2) - (Math.pow(p3.x, 2)) + (Math.pow(p1.x, 2)) - (Math.pow(p3.y, 2)) + (Math.pow(p1.y, 2)));
            float delta = (float) ((4.0d) * (p1.x - p2.x) * (p1.y - p3.y) - (p1.x - p3.x) * (p1.y - p2.y));
            float x0 = (float) ((1.0d / delta) * (2.0d * A * (p1.y - p3.y) - 2.0d * B * (p1.y - p2.y)));
            float y0 = (float) ((1.0d / delta) * (2.0d * B * (p1.x - p2.x) - 2.0d * A * (p1.x - p3.x)));

            return x0 + "," + y0;

    }


    public void MacAddressesInit(){

        macAddresses.add("00:13:AA:00:2B:41");
        macAddresses.add("00:15:87:10:A1:A6");
        macAddresses.add("00:13:AA:00:07:06");
        macAddresses.add("00:15:87:20:A5:F3");
        macAddresses.add("00:15:87:20:AF:12");
        macAddresses.add("00:15:87:20:BA:54");
        macAddresses.add("00:13:AA:00:3A:08");
        macAddresses.add("00:15:87:20:F7:B0");
        macAddresses.add("00:13:AA:00:09:B5");
        macAddresses.add("00:13:AA:00:3C:FE");
        macAddresses.add("00:13:AA:00:09:C4");
        macAddresses.add("00:13:AA:00:2C:E8");
        macAddresses.add("00:13:AA:00:0A:3C");
        macAddresses.add("00:15:87:20:C3:0A");
        macAddresses.add("00:13:AA:00:26:FC");
        macAddresses.add("00:13:AA:00:11:9A");
        macAddresses.add("00:13:AA:00:2D:17");
        macAddresses.add("00:13:AA:00:16:7D");
        macAddresses.add("00:15:87:20:A9:A5");
        macAddresses.add("00:15:87:20:A2:9D");
        macAddresses.add("00:15:83:40:B2:8C");
        macAddresses.add("00:15:87:20:EE:BA");
        macAddresses.add("00:15:83:31:55:A9");
        macAddresses.add("00:13:AA:00:11:84");

    }

    protected void onDestroy() {
        super.onDestroy();
    }
/*
    public void onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
*/

}
