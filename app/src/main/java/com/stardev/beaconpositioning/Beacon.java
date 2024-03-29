/*
 * Copyright (C) 2015 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stardev.beaconpositioning;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.Arrays;

public class Beacon {

  public int prev_rssi;
  public final BluetoothDevice device;
  public final double rssi; // Received Signal Strength Indication
  public final byte[] scanRecord;
  public final int txPower; // The Transmit Power Level characteristics in dBm
  public final MacAddress macAddress;
  private boolean isInitialized = false;

  private double estimatedRSSI;//calculated rssi
  private double errorCovarianceRSSI;//calculated covariance
  private double processNoise = 0.125;
  private double measurementNoise = 0.8;

  public Beacon(BluetoothDevice device, double rssi, byte[] scanRecord) {
    this.device = device;
    this.rssi = rssi;
    this.scanRecord = scanRecord;
    this.txPower = -59; // default value for Estimote and Kontakt.io beacons
    this.macAddress = new MacAddress(device.getAddress()); // contains validated MAC address
  }

  public static Beacon create(BluetoothDevice device, int rssi, byte[] scanRecord) {
    return new Beacon(device, rssi, scanRecord);
  }

  @SuppressLint("NewApi")
  public static Beacon create(ScanResult result) {
    return create(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
  }

  public double applyFilter() {
    double priorRSSI;
    double kalmanGain;
    double priorErrorCovarianceRSSI;
    if (!isInitialized) {
      priorRSSI = rssi;
      priorErrorCovarianceRSSI = 1;
      isInitialized = true;
    } else {
      priorRSSI = estimatedRSSI;
      priorErrorCovarianceRSSI = errorCovarianceRSSI + processNoise;
    }

    kalmanGain = priorErrorCovarianceRSSI / (priorErrorCovarianceRSSI + measurementNoise);
    estimatedRSSI = priorRSSI + (kalmanGain * (rssi - priorRSSI));
    errorCovarianceRSSI = (1 - kalmanGain) * priorErrorCovarianceRSSI;

    return estimatedRSSI;
  }

  /**
   * Gets distance from BLE beacon to mobile device in meters
   *
   * @return distance in meters as double
   */
  public double getDistance() {
    return getDistance(rssi, txPower);
  }

  public Proximity getProximity() {
    double distance = getDistance();
    Proximity immediate = Proximity.IMMEDIATE;
    Proximity near = Proximity.NEAR;

    if (distance < immediate.maxDistance) {
      return immediate;
    }

    if (distance >= near.minDistance && distance <= near.maxDistance) {
      return near;
    }

    return Proximity.FAR;
  }

  private double getDistance(double rssi, int txPower) {
    double filter_res = applyFilter();
    isInitialized = false;
    Log.i("RSSI - Kalman", filter_res + "");
    return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
  }

  @Override public String toString() {
    return "Beacon{device=" + device + ", rssi=" + rssi + '}';
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Beacon beacon = (Beacon) o;

    if (rssi != beacon.rssi) {
      return false;
    }

    if (!device.equals(beacon.device)) {
      return false;
    }

    return Arrays.equals(scanRecord, beacon.scanRecord);
  }

  @Override public int hashCode() {
    int result = device.hashCode();
    result = 31 * result + (int)rssi;
    result = 31 * result + (scanRecord != null ? Arrays.hashCode(scanRecord) : 0);
    return result;
  }
}
