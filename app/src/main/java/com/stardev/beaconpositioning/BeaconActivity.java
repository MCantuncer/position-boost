 package com.stardev.beaconpositioning;

 import android.Manifest;
 import android.app.Activity;
 import android.content.pm.PackageManager;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.ColorMatrix;
 import android.graphics.ColorMatrixColorFilter;
 import android.graphics.Matrix;
 import android.graphics.Paint;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.ActivityCompat;
 import android.widget.ArrayAdapter;
 import android.widget.ListView;
 import android.widget.Toast;
 import com.github.pwittchen.reactivebeacons.library.rx2.Beacon;
 import com.github.pwittchen.reactivebeacons.library.rx2.Proximity;
 import com.github.pwittchen.reactivebeacons.library.rx2.ReactiveBeacons;
 import io.reactivex.android.schedulers.AndroidSchedulers;
 import io.reactivex.annotations.NonNull;
 import io.reactivex.disposables.Disposable;
 import io.reactivex.functions.Consumer;
 import io.reactivex.schedulers.Schedulers;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
 import static android.Manifest.permission.ACCESS_FINE_LOCATION;
 import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

 public abstract class BeaconActivity extends Activity {
     private static final boolean IS_AT_LEAST_ANDROID_M =
             Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
     private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1000;
     private static final String ITEM_FORMAT = "MAC: %s, RSSI: %d\ndistance: %.2fm, proximity: %s\n%s";
     private ReactiveBeacons reactiveBeacons;
     private Disposable subscription;
     protected Map<String, Beacon> beacons;

     @Override protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         reactiveBeacons = new ReactiveBeacons(this);
         beacons = new HashMap<>();
     }

     @Override protected void onResume() {
         super.onResume();

         if (!canObserveBeacons()) {
             return;
         }

         startSubscription();
     }

     private void startSubscription() {
         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                 != PackageManager.PERMISSION_GRANTED
                 && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                 != PackageManager.PERMISSION_GRANTED) {
             requestCoarseLocationPermission();
             return;
         }

         subscription = reactiveBeacons.observe()
                 .subscribeOn(Schedulers.computation())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(new Consumer<Beacon>() {
                     @Override public void accept(@NonNull Beacon beacon) throws Exception {
                         beacons.put(beacon.device.getAddress(), beacon);
                         beaconDataArrived(beacon);
                     }
                 });
     }

     private boolean canObserveBeacons() {
         if (!reactiveBeacons.isBleSupported()) {
             Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
             return false;
         }

         if (!reactiveBeacons.isBluetoothEnabled()) {
             reactiveBeacons.requestBluetoothAccess(this);
             return false;
         } else if (!reactiveBeacons.isLocationEnabled(this)) {
             reactiveBeacons.requestLocationAccess(this);
             return false;
         } else if (!isFineOrCoarseLocationPermissionGranted() && IS_AT_LEAST_ANDROID_M) {
             requestCoarseLocationPermission();
             return false;
         }

         return true;
     }

     private String getBeaconItemString(Beacon beacon) {
         String mac = beacon.device.getAddress();
         int rssi = beacon.rssi;
         double distance = beacon.getDistance();
         Proximity proximity = beacon.getProximity();
         String name = beacon.device.getName();
         return String.format(ITEM_FORMAT, mac, rssi, distance, proximity, name);
     }

     @Override protected void onPause() {
         super.onPause();
         if (subscription != null && !subscription.isDisposed()) {
             subscription.dispose();
         }
     }

     @Override public void onRequestPermissionsResult(int requestCode,
                                                      @android.support.annotation.NonNull String[] permissions,
                                                      @android.support.annotation.NonNull int[] grantResults) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         final boolean isCoarseLocation = requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION;
         final boolean permissionGranted = grantResults[0] == PERMISSION_GRANTED;

         if (isCoarseLocation && permissionGranted && subscription == null) {
             startSubscription();
         }
     }

     private void requestCoarseLocationPermission() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             requestPermissions(new String[] { ACCESS_COARSE_LOCATION },
                     PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
         }
     }

     private boolean isFineOrCoarseLocationPermissionGranted() {
         boolean isAndroidMOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
         boolean isFineLocationPermissionGranted = isGranted(ACCESS_FINE_LOCATION);
         boolean isCoarseLocationPermissionGranted = isGranted(ACCESS_COARSE_LOCATION);

         return isAndroidMOrHigher && (isFineLocationPermissionGranted
                 || isCoarseLocationPermissionGranted);
     }

     private boolean isGranted(String permission) {
         return ActivityCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED;
     }

     protected abstract void beaconDataArrived(Beacon beacon);

     public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
         int width = bm.getWidth();
         int height = bm.getHeight();

         float scaleWidth = ((float) newWidth) / width;
         float scaleHeight = ((float) newHeight) / height;

         // CREATE A MATRIX FOR THE MANIPULATION
         Matrix matrix = new Matrix();

         // RESIZE THE BITMAP
         matrix.postScale(scaleWidth, scaleHeight);

         // INVERT THE BITMAP
         matrix.postScale(1,-1);

         // "RECREATE" THE NEW BITMAP
         Bitmap resizedBitmap = Bitmap.createBitmap(
                 bm, 0, 0, width, height, matrix, false);
         bm.recycle();

         return resizedBitmap;
     }

 }