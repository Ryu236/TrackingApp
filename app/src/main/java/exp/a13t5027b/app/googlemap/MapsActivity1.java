package exp.a13t5027b.app.googlemap;

import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.Manifest;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import android.location.Location;

//import com.google.android.gms.drive.Permission;
//import com.google.android.gms.drive.internal.StringListResponse;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;

import com.google.android.gms.maps.model.MarkerOptions;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBQuery;
import com.nifty.cloud.mb.core.FindCallback;

import java.util.List;

//経路検索
//import android.content.Intent;
//import android.net.Uri;

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

        //経路検索
        // test0();

        // mBaaSの初期化 APIキーの設定
        NCMB.initialize(this.getApplicationContext(),"2ec74409180bbf60ac01acbf23e2198ab84118da7415c7c587b02ec7d7b8cf5a","0c8707e959d5c3e91020cadc8d99bfb1801b5c0d1585dd0894365333fe82c56d");

        setContentView(R.layout.activity_maps1);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
//        mMap.setMyLocationEnabled(true);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // データストアからデータの取得
        // クラスの選択
        NCMBQuery<NCMBObject> query = new NCMBQuery<>("Location");

        // データストアの検索
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(List<NCMBObject> results, NCMBException e) {
                if (e != null) {
                    // error
                    Log.e("NCMB", "検索に失敗しました。エラー:" + e.getMessage());
                } else {
                    // success
                    Log.i("NCMB", "検索に成功しました。");

                    // for文による検索結果の処理(results)
                    for (int i = 0, n = results.size(); i < n; i++) {
                        NCMBObject o = results.get(i);
                        Log.i("NCMB", o.getString("name"));
                        String name = o.getString("name");
                        Location geo = o.getGeolocation("geo");

                        // マーカーの設置
                        LatLng marker = new LatLng(geo.getLatitude(),geo.getLongitude()); // 緯度経度のオブジェクト
                        mMap.addMarker(new MarkerOptions()
                            .position(marker)
                            .title(name));
                    }
                }
            }
        });
        /**
        * // Move the camera at Shinshu University.
        * LatLng shinshuU = new LatLng(36.6308777,138.189517);
        * //mMap.addMarker(new MarkerOptions()
        *  //    .position(shinshuU)
        * //    .title("信州大学工学部"));
         */
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



    //地名を入れて経路を検索
//    private void test0(){
//        String start = "信州大学工学部";
//        String destination = "前橋ナップス";
//
//        //車:d
//        String dir = "d";
//
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_VIEW);
//        intent.setClassName("com.google.android.apps.maps","com.google.android.maps.MapsActivity");
//        intent.setData(Uri.parse("http://maps.google.com/maps?saddr="+start+"&daddr="+destination+"&dirflg="+dir));
//        startActivity(intent);
//    }

} /** End */