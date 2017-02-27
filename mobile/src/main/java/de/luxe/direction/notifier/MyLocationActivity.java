package de.luxe.direction.notifier;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MyLocationActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.SnapshotReadyCallback{

    private GoogleMap mMap;
    private String myLocation = "My Location";
    private LatLng myPos;
    private Bitmap direction;
    private String text = "Beispiel";
    private String title = "Beispiel";
    private Integer notiId;
    private PendingIntent pendingIntent;
    private String textIntent;
    Bitmap smaller = null;
    private SupportMapFragment mapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myPos = (LatLng) getIntent().getExtras().get("loc");
        if(Intent.FLAG_ACTIVITY_NEW_TASK == getIntent().getFlags()) {
            direction = (Bitmap) getIntent().getExtras().get("direction");
            text = (String) getIntent().getExtras().get("text");
            myLocation = text;
            title = (String) getIntent().getExtras().get("title");
            notiId = (Integer) getIntent().getExtras().get("notiId");
            pendingIntent = (PendingIntent) getIntent().getExtras().get("pendingIntent");
            textIntent = (String) getIntent().getExtras().get("textIntent");
        }
        setContentView(R.layout.activity_my_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

            mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 5;

        if(direction == null){
            smaller = BitmapFactory.decodeResource(getResources(), R.mipmap.direction2, options);
        }
        else{
            smaller = direction;
        }

        mMap.addMarker(new MarkerOptions()
                .position(myPos)
                .icon(BitmapDescriptorFactory.fromBitmap(smaller))
                .title(text));


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos,mMap.getMaxZoomLevel()-4));

        mMap.setOnMapLoadedCallback(this);

    }


    @Override
    public void onSnapshotReady(Bitmap bitmap) {
        // Set up the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setColor(Color.GREEN)
                .setShowWhen(true)
                .setContentInfo(text)
                .setSmallIcon(R.mipmap.direction2)
                .setLargeIcon(bitmap)
                .setContentTitle(title)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(text)
                        .bigLargeIcon(bitmap)
                        .setBigContentTitle(title));

        // adding exit intent
        //builder.addAction(android.R.drawable.ic_delete, textIntent, pendingIntent);
        // creating notification
        Notification notification = builder.build();
        // notify
        NotificationManagerCompat.from(this).notify(notiId, notification);
    }

    @Override
    public void onMapLoaded() {
        if(direction != null) {
            mMap.snapshot(this);
        }
    }

}
