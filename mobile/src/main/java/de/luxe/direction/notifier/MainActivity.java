/**
 *
 * @author deLUXe
 * @date 2017-02-07
 */
package de.luxe.direction.notifier;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * direction notifier, main activity to show some info
 *
 * @author deLUXe
 */
public class MainActivity extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView donate = (TextView) findViewById(R.id.infoTxtDonate);
        donate.setMovementMethod(LinkMovementMethod.getInstance());
        Button btnMyLocation = (Button) findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LatLng myLocation = getMyLocation();
                if(myLocation == null){
                    return;
                }
                Intent start = new Intent(MainActivity.this, MyLocationActivity.class);
                start.putExtra("loc", myLocation);
                startActivity(start);
            }
        });
    }

    public LatLng getMyLocation() {
        Geocoder geocoder;
        String bestProvider;
        List<Address> user = null;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        bestProvider = lm.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Please give permission for your location", Toast.LENGTH_LONG).show();
            return null;
        }
        Location location = lm.getLastKnownLocation(bestProvider);

        if (location == null) {
            Toast.makeText(this, "Location Not found", Toast.LENGTH_LONG).show();
        } else {
            geocoder = new Geocoder(this);
            try {
                user = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                return new LatLng((double) user.get(0).getLatitude(), (double) user.get(0).getLongitude());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
