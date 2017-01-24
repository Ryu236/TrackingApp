/*
 * Created by Ryutaro Kobayashi
 * Copyright (c) 2016. All rights reserved.
 *
 * Last modified 16/12/18 14:07
 */

package exp.a13t5027b.app.googlemap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.FindCallback;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBQuery;
import com.nifty.cloud.mb.core.NCMBUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MapsActivity1 extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        CompoundButton.OnCheckedChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    protected static final String TAG = "LocationData";
    protected static final String TAG2 = "LocationUpdates";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private GoogleMap mMap;
    protected GoogleApiClient mGoogleApiClient; // Provides the entry point to Google Play services.
    private static final int REQUEST_LOGIN = 0;
    protected String Username;
    protected Marker marker;

    /** Location Update Setting */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 4000;
    protected Location mCurrentLocation;
    protected LocationRequest mLocationRequest;
    public Boolean mRequestingLocationUpdates;

    /** Share Location */
    protected Date updateDate;
    protected Date lastDate = null;
    protected ArrayList<String> Userdata = new ArrayList<>();
    protected ArrayList<Float> Color = new ArrayList<>();
    protected String sharename;
    protected LatLng sharelocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps1);

        mRequestingLocationUpdates = false;

        /** NCMBの初期化 APIキーの設定 */
        NCMB.initialize(this,"2ec74409180bbf60ac01acbf23e2198ab84118da7415c7c587b02ec7d7b8cf5a","0c8707e959d5c3e91020cadc8d99bfb1801b5c0d1585dd0894365333fe82c56d");

        /** Login action */
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);

        // Obtain the MapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Switch _switch = (Switch) findViewById(R.id._switch);
        _switch.setOnCheckedChangeListener(this);

        //Create an instance of GoogleAPIClient
        buildGoogleApiClint();

        /** Serch all User */
        findUsers();

        Log.i("Userdata", "Userdata size is " + Userdata.size());

        /** Add Marker all User */
        AddMarkerlocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_logout) {
            NCMBUser.logoutInBackground(new DoneCallback() {
                @Override
                public void done(NCMBException e) {
                    if (e != null) {
                        //エラー時の処理
                        Log.e("Logout", "Logout missed. error :" + e.getMessage());
                    }
                }
            });
            Toast.makeText(getApplicationContext(), "Logout", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_LOGIN);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Builds a GoogleApiClient and Request Location Settings.*/
    protected synchronized void buildGoogleApiClint() {
        Log.i(TAG2, "Building GoogleApiClient");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                // Get Username
                Username = data.getStringExtra("Username");
                Toast.makeText(getApplicationContext(), "Welcome " + Username, Toast.LENGTH_SHORT).show();
                Log.i("Intent", "Intent success.");
            }else {
                // error Login activity (intent)
                Log.i("Intent", "LoginactivityとのresultCodeがうまくいってません。");
            }
        } else{
            Log.i("Intent","REQUEST_LOGIN is not match.");
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Move the camera at Shinshu University.
        LatLng shinshuU = new LatLng(36.6308777,138.189517);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(shinshuU));

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
    }

    /** Tracking Location Switch. */
    @Override
    public void onCheckedChanged(CompoundButton buttonview, boolean isChecked) {
        if (isChecked == true) {
            Log.i("Switch", "Switch is checked ON");
            Toast.makeText(getApplicationContext(), "Tracking Location ON", Toast.LENGTH_SHORT).show();
            if (!mRequestingLocationUpdates) {
                mRequestingLocationUpdates = true;
                startLocationUpdates();
            }
        } else {
            Log.i("Switch", "Switch is checked OFF");
            Toast.makeText(getApplicationContext(), "Tracking Location OFF", Toast.LENGTH_SHORT).show();
            if (mRequestingLocationUpdates) {
                mRequestingLocationUpdates = false;
                stopLocationUpdates();
            }
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this,"MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissons, @NonNull int[] grantResults) {
        if(requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if(PermissionUtils.isPermissionGranted(permissons, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        }else{
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if(mPermissionDenied) {
            //Permission was not granted, display error dialog.
            showMiddingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMiddingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG2, "Connected to GoogleApiClient");

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /** Request Location Settings */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /** Request Location Updates from the FusedLocationApi */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.i(TAG2, "StartLocationUpdates");
    }

    /** Removes Location Updates from the FusedLocationApi. */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Log.i(TAG2, "StopLocationUpdates");
    }

    /**  */
    private void AddMarkerlocation() {
        // クラスの選択
        NCMBQuery<NCMBObject> query = new NCMBQuery<>("LocationUpdates");
        query.addOrderByDescending("createDate");
        for (int i = 0, n = Userdata.size(); i < n; i++) {
            final int num = i;
            // Userの限定
            query.whereEqualTo("name", Userdata.get(num));
            // データストアの検索
            query.findInBackground(new FindCallback<NCMBObject>() {
                @Override
                public void done(List<NCMBObject> res, NCMBException e) {
                    if (e != null) {
                        //error
                        Log.e("addMarker", e.getMessage());
                    } else {
                        //success
                        for (int i = 0, n = res.size(); i < n; i++) {
                            NCMBObject obj = res.get(i);
                            String name = obj.getString("name"); // nameフィールドの取得
                            Location geo = obj.getGeolocation("geo");
                            Date date = obj.getCreateDate();

                            // マーカーの設置
                            if (i == 0) {
                                LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude()); // 緯度経度のオブジェクト
                                marker = mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .icon(BitmapDescriptorFactory.defaultMarker(Color.get(num)))
                                        .title(name));
                                marker.showInfoWindow();
                                Log.i("addMarker", "name: " + name);
                                Log.i("addMarker", pos.toString());
                                Log.i("addMarker", "createDate: " + date.toString());
                            }
                        }
                    }
                }
            });
        }
    }

    private void UpdateMarkerLocation() {

    }

    /** Callback that fires when the location changes. */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateDate = new Date();

        Toast.makeText(getApplicationContext(), "Location Updates", Toast.LENGTH_SHORT).show();
        NCMBLocationUpdates();
        //AddMarkerlocation();
    }

    /** Preserve Location Updates to NCMB datestore. */
    private void NCMBLocationUpdates() {
        Location geo = new Location("geo");
        geo.setLatitude(mCurrentLocation.getLatitude());
        geo.setLongitude(mCurrentLocation.getLongitude());

        NCMBObject obj = new NCMBObject("LocationUpdates");
        obj.put("name", Username);
        obj.put("geo", geo);
        obj.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {
                    // error
                    Log.e("NCMB", "Failed preserve Location. Error:" + e.getMessage());
                } else {
                    // success
                    Log.i("NCMB", "Success preserved Location");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        // We call connect() to attempt to re-establish the connection.
        Log.i(TAG2, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG2, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

//    /** Share Location Updates */
//    private void shareLocation() {
//
//    }

    /** search all User */
    private void findUsers() {
        NCMBQuery<NCMBUser> users = NCMBUser.getQuery();

        try {
            List<NCMBUser> list = users.find();
            for (int i = 0, n = list.size(); i < n; i++) {
                NCMBUser u = list.get(i);
                Userdata.add(i, u.getUserName());
                Color.add(i, (float) u.getLong("color"));
                Log.i("Users", "UserData: " + Userdata.get(i));
            }
            Log.i("Userdata", "Userdata size is " + Userdata.size());
        } catch (NCMBException e) {
            e.getStackTrace();
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

} /** End */