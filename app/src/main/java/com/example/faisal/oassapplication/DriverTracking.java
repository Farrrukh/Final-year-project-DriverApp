package com.example.faisal.oassapplication;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.example.faisal.oassapplication.Common.Common;
import com.example.faisal.oassapplication.Helper.DirectionJSONparser;
import com.example.faisal.oassapplication.Model.FCMResponse;
import com.example.faisal.oassapplication.Model.Notification;
import com.example.faisal.oassapplication.Model.Sender;
import com.example.faisal.oassapplication.Model.Token;
import com.example.faisal.oassapplication.Remote.IFCMService;
import com.example.faisal.oassapplication.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback,
GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener
{

    private GoogleMap mMap;

    double riderLat,riderLng;
    String customerId;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    private Circle riderMarker;
    private Marker driverMarker;

    private Polyline direction;
    IGoogleAPI mService;
    IFCMService mFCMServuce;

    GeoFire geoFire;

    Button btnStartTrip;
    Location  pickupLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(getIntent()!=null)
        {
            riderLat=getIntent().getDoubleExtra("lat",-1.0);
            riderLng=getIntent().getDoubleExtra("lng",-1.0);
            customerId=getIntent().getStringExtra("customerId");

        }
        mService =Common.getGoogleAPI();
        mFCMServuce=Common.getFCMService();
        setUpLocation();

        btnStartTrip=(Button)findViewById(R.id.btnStartTrip);
        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnStartTrip.getText().equals("START TRIP")){
                    pickupLocation=Common.mLastLocation;
                    btnStartTrip.setText("DROP OFF HERE");
                }
                else   if(btnStartTrip.getText().equals("DROP OFF HERE")){
                    calculateCashFee(pickupLocation,Common.mLastLocation);
                }


                }
        });
    }

    private void calculateCashFee(final Location pickupLocation, Location mLastLocation) {

        String requestApi =null;
        try {
            requestApi="https://maps.googleapis.com/maps/api/directions/json?"+"mode=driving&"+"transit_routing_preference=less_driving&"+
                    "origin="+pickupLocation.getLatitude()+","+pickupLocation.getLongitude()+"&"+
                    "destination="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude()+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                 JSONObject jsonObject=new JSONObject(response.body().toString());
                                 JSONArray routes=jsonObject.getJSONArray("routes");

                                 JSONObject object=routes.getJSONObject(0);

                                 JSONArray legs=object.getJSONArray("legs");

                                 JSONObject legsObject=legs.getJSONObject(0);

                                 JSONObject distance=legsObject.getJSONObject("distance");
                                 String distance_text=distance.getString("text");
                                 Double distance_value=Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+",""));

                                JSONObject timeObject=legsObject.getJSONObject("duration");
                                String time_text=timeObject.getString("text");
                                Double time_value=Double.parseDouble(time_text.replaceAll("[^0-9\\\\.]+",""));

                                Intent intent=new Intent(DriverTracking.this,TripDetail.class);
                                intent.putExtra("start_address",legsObject.getString("start_address"));
                                intent.putExtra("end_address",legsObject.getString("end_address"));
                                intent.putExtra("time",String.valueOf(time_value));
                                intent.putExtra("distance",String.valueOf(distance_value));
                                intent.putExtra("total",Common.formulaPrice(distance_value,time_value));
                                intent.putExtra("location_start",String.format("%F,%f",pickupLocation.getLatitude(),pickupLocation.getLongitude()));
                                intent.putExtra("location_end",String.format("%F,%f",Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude()));

                                startActivity(intent);
                                finish();






                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void setUpLocation() {

                if (checkPlayServices()) {
                    buildGoogleApiClient();
                    createLocationRequest();
                        displayLocation();
                }
            }

    private void buildGoogleApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            else{
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }
    private void createLocationRequest() {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        riderMarker=mMap.addCircle(new CircleOptions()
        .center(new LatLng(riderLat,riderLng))
        .radius(50)
        .strokeColor(Color.BLUE)
        .fillColor(0x220000FF).strokeWidth(5.0f));

        geoFire=new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driver_tbl));
        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(riderLat,riderLng),0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendArrivedNotification(customerId);
                btnStartTrip.setEnabled(true);

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void sendArrivedNotification(String customerId) {
        Token token=new Token(customerId);

        Notification notification=new Notification("Arrived",String.format("The driver %s has arrived at your location",Common.currentUser.getName()));
        Sender sender =new Sender(token.getToken(),notification);

        mFCMServuce.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if(response.body().success!=1)
                {
                    Toast.makeText(DriverTracking.this,"Failed",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();
    }
    private void startLocationUpdate() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {
                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();

               if(driverMarker!=null)
                   driverMarker.remove();
               driverMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude))
               .title("You")
               .icon(BitmapDescriptorFactory.defaultMarker()));
               mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),17.0f));

            if(direction!=null)
                direction.remove();
            getDirection();
        }
        else {
            Log.d("ERROR", "Cannot get your location");
        }
    }

    private void getDirection() {
       LatLng currentPosition=new LatLng(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());

        String requestApi =null;
        try {
            requestApi="https://maps.googleapis.com/maps/api/directions/json?"+"mode=driving&"+"transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+riderLat+","+riderLng+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);
            Log.d("faisal",requestApi);
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                new parserTask().execute(response.body().toString());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
       Common.mLastLocation=location;
       displayLocation();
    }

    private class parserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {

        ProgressDialog mDialog=new ProgressDialog(DriverTracking.this);
       @Override
        protected void onPreExecute(){
           super.onPreExecute();
           mDialog.setMessage("Please waiting....");
           mDialog.show();
       }
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
           JSONObject jobject;
            List<List<HashMap<String, String>>> routes=null;
            try {
                jobject=new JSONObject(strings[0]);
                DirectionJSONparser parser=new DirectionJSONparser();
                routes=parser.parse(jobject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = new ArrayList<LatLng>();;
            PolylineOptions lineOptions = new PolylineOptions();;
            lineOptions.width(2);
            lineOptions.color(Color.RED);
            MarkerOptions markerOptions = new MarkerOptions();
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);

            }
            // Drawing polyline in the Google Map for the i-th route
            if(points.size()!=0)mMap.addPolyline(lineOptions);//to avoid crash
        }
    }
}
