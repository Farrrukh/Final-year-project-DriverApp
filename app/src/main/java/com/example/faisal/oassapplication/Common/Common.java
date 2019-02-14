package com.example.faisal.oassapplication.Common;

import android.location.Location;

import com.example.faisal.oassapplication.Model.User;
import com.example.faisal.oassapplication.Remote.FCMClient;
import com.example.faisal.oassapplication.Remote.IFCMService;
import com.example.faisal.oassapplication.Remote.IGoogleAPI;
import com.example.faisal.oassapplication.Remote.RetrofitClient;

public class Common {
    public static final String token_tbl="Tokens";
    public static Location mLastLocation=null;

    public static User currentUser;
    public static final String driver_tbl="Drivers";
    public static final String user_driver_tbl="DriversInformation";
    public static final String user_rider_tbl="RidersInformation";
    public static final String pickup_request_tbl="PickupRequest";
    public static final String baseURL="https://maps.googleapis.com";
    public static final String fcmURL="https://fcm.googleapis.com";

    public static double base_fare=2.55;
    public static double time_rate=0.35;
    public  static  double distance_rate=1.75;
    public static final int PICK_IMAGE_REQUEST=9999;

    public static double formulaPrice(double km, Double min){
        return (base_fare+(distance_rate*km)+(time_rate*min));
    }
    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

}
