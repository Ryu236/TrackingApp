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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.FindCallback;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBQuery;
import com.nifty.cloud.mb.core.NCMBUser;

import java.util.List;


public class MapsActivity1 extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    protected static final String TAG = "LocationData";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps1);
        //経路検索
        // test0();

        /** NCMBの初期化 APIキーの設定 */
        NCMB.initialize(this,"2ec74409180bbf60ac01acbf23e2198ab84118da7415c7c587b02ec7d7b8cf5a","0c8707e959d5c3e91020cadc8d99bfb1801b5c0d1585dd0894365333fe82c56d");

        /** Login action */
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /** データストアからデータの取得 */
        // クラスの選択
        NCMBQuery<NCMBObject> query = new NCMBQuery<>("Location");

        // データストアの検索
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(List<NCMBObject> results, NCMBException e) {
                if (e != null) {
                    // error ログによる表示
                    Log.e("NCMB", "検索に失敗しました。エラー:" + e.getMessage());
                } else {
                    // success ログによる表示
                    Log.i("NCMB", "検索に成功しました。");

                    // for文による検索結果の処理(results)
                    for (int i = 0, n = results.size(); i < n; i++) {
                        NCMBObject o = results.get(i);
                        Log.i(TAG, o.getString("name")); // ログの表示
                        String name = o.getString("name"); // nameフィールドの取得
                        Location geo = o.getGeolocation("geo"); // geoフィールドの取得

                        // マーカーの設置
                        LatLng marker = new LatLng(geo.getLatitude(),geo.getLongitude()); // 緯度経度のオブジェクト
                        mMap.addMarker(new MarkerOptions()
                            .position(marker)
                            .title(name));
                    }
                }
            }
        });


        // Move the camera at Shinshu University.
        LatLng shinshuU = new LatLng(36.6308777,138.189517);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(shinshuU));

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
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

} /** End */