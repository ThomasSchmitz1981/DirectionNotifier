/**
 * direction notifier
 *
 * @author deLUXe
 * @date 2017-02-07
 */
package de.luxe.direction.notifier;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import cz.msebera.android.httpclient.Header;

import static de.luxe.direction.notifier.BuildConfig.API_KEY;


/**
 * AccessibilityService receive event AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
 * of google maps navigation direction notifications (which are not triggered to be listened by a
 * NotificationListenerService) and creates a new notification that can be pushed
 * by the ticwear comp app.
 * See res/xml/accessibilityservice.xml for more info.
 *
 * @author deLUXe
 */
public class DirectionNotifierService extends AccessibilityService {
    static final String TAG = "DirNotifierService";
    private Bitmap bitmap;
    private Bitmap staticMapImage;
    private PendingIntent pendingIntent;
    private String textIntent;
    private LatLng lastLocation;
    private Map<String, Integer> notis = new HashMap<String, Integer>();
    private static AsyncHttpClient client;
    private NotificationManagerCompat managerCompat;


    /**
     * Callback for {@link AccessibilityEvent}s.
     *
     * @param event The new event. This event is owned by the caller and cannot be used after
     *              this method returns. Services wishing to use the event after this method returns should
     *              make a copy.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        Integer notiId = new Random().nextInt();
        if (!notis.isEmpty()) {
            managerCompat.cancel(notis.get("0"));
            notis.remove("0");
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        if(staticMapImage != null){
            staticMapImage.recycle();
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.v(TAG, "Received event");
            Parcelable parcelableData = event.getParcelableData();
            if (parcelableData == null) {
                return;
            }
            if (parcelableData instanceof Notification) {
                Notification direction = (Notification) parcelableData;
                // the notification which is interesting consists of a bigContentView (maps navigation notification),
                // if null, no creation of a notification is done (may be maps traffic notification)
                RemoteViews views = direction.bigContentView;
                if(views == null){
                    return;
                }
                // parsing the RemoteViews by reflection,
                // this is not an elegant way to get the infos from the notification, but find no other way
                StringBuilder textConcated = parseContent(views);
                String dirTitle = "New Direction";

                LatLng myLocation = getMyLocation();
                if(myLocation != null) {
                    staticMapImage = getGoogleStaticMapImageAndSend(myLocation, textConcated.toString(),dirTitle,notiId);
                }
                if(staticMapImage != null){
                    return;
                }
                staticMapImage = bitmap;
                sendNotification(textConcated.toString(), dirTitle, notiId);

            }
        }

    }

    private void sendNotification(String textConcated, String dirTitle, Integer notiId){
        // Set up the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setColor(Color.GREEN)
                .setShowWhen(true)
                .setContentInfo(textConcated.replace('#',' '))
                .setSmallIcon(R.mipmap.direction2)
                .setLargeIcon(bitmap)
                .setContentTitle(dirTitle)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentText(textConcated.replace('#',' '))
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(staticMapImage)
                        .setSummaryText(textConcated.replace('#',' '))
                        .bigLargeIcon(staticMapImage)
                        .setBigContentTitle(dirTitle));

        // adding exit intent
        builder.addAction(android.R.drawable.ic_delete, textIntent, pendingIntent);
        // creating notification
        Notification notification = builder.build();
        // managing notification
        notis.put("0", notiId);
        // notify
        managerCompat.notify(notiId, notification);
    }


    private Bitmap getGoogleStaticMapImageAndSend(final LatLng myLocation, final String textConcated, final String dirTitle, final Integer notiId){
        String url = "https://maps.googleapis.com/maps/api/staticmap";
        RequestParams params = new RequestParams();
        provideParams(myLocation, params);
        Log.v(TAG, "URL: " + AsyncHttpClient.getUrlWithQueryString(true, url, params));
        RequestHandle handler = client.get(AsyncHttpClient.getUrlWithQueryString(true, url, params),
                new AsyncHttpResponseHandler() {
                    /**
                     * Fired when a request returns successfully, override to handle in your own code
                     *
                     * @param statusCode   the status code of the response
                     * @param headers      return headers, if any
                     * @param responseBody the body of the HTTP response from the server
                     */
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if(statusCode == 200) {
                            ByteArrayInputStream is = new ByteArrayInputStream(responseBody);
                            staticMapImage = BitmapFactory.decodeStream(is);
                            if(staticMapImage != null){
                                staticMapImage = joinImages(textConcated);
                                lastLocation = myLocation;
                                sendNotification(textConcated, dirTitle, notiId);
                            }
                        }
                    }

                    /**
                     * Fired when a request fails to complete, override to handle in your own code
                     *
                     * @param statusCode   return HTTP status code
                     * @param headers      return headers, if any
                     * @param responseBody the response body, if any
                     * @param error        the underlying cause of the failure
                     */
                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        staticMapImage = null;
                        Toast.makeText(DirectionNotifierService.this,
                                "Error getting static map image: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
        });

        return staticMapImage;
    }

    private Bitmap joinImages(String textConcated){
        Bitmap bmOverlay = Bitmap.createBitmap(staticMapImage.getWidth(), staticMapImage.getHeight(), staticMapImage.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(staticMapImage, 0, 0, null);
        float height = staticMapImage.getHeight()/2.5F;
        float heightDir = bitmap.getHeight();
        Paint paintImg = new Paint();
        paintImg.setColor(Color.GRAY);
        paintImg.setColorFilter(new PorterDuffColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.MULTIPLY));
        canvas.drawBitmap(bitmap, height, 10, paintImg);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        StringTokenizer tokenizer = new StringTokenizer(textConcated, "#");
        int i = 15;
        while(tokenizer.hasMoreTokens()) {
            canvas.drawText(tokenizer.nextToken(), 15+ i, height + heightDir + i, paint);
            i = i + 20;
        }
        return bmOverlay;
    }

    private void provideParams(LatLng myLocation, RequestParams params){
        params.add("center", new StringBuilder().append(myLocation.latitude).append(",").append(myLocation.longitude).toString());
        params.add("zoom", "17");
        params.add("size", "250x250");
        params.add("scale", "2");
        params.add("maptype", "roadmap");
        params.add("markers", new StringBuilder().append("color:red|").append("size:mid|")
                .append(myLocation.latitude).append(",").append(myLocation.longitude).toString());
        if(lastLocation != null){
            params.add("markers", new StringBuilder().append("color:white|").append("label:L|").append("size:mid|")
                    .append(lastLocation.latitude).append(",").append(lastLocation.longitude).toString());
        }
        params.add("key", API_KEY);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected()...");
        client = new AsyncHttpClient();
        managerCompat = NotificationManagerCompat.from(this);
    }

    /**
     * Callback for interrupting the accessibility feedback.
     */
    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt()...");
        if(bitmap != null){
            bitmap.recycle();
        }
        if(staticMapImage != null){
            staticMapImage.recycle();
        }
        if(client != null){
            client.cancelAllRequests(false);
        }
        notis.clear();
    }

    /**
     * not a elegant way to get the info from the notification,
     * but find no other way. Thats why the functionality can be afflected
     * by a newer version of maps
     *
     * @param views
     * @return infos
     */
    private StringBuilder parseContent(Object views) {
        StringBuilder textConcated = new StringBuilder();
        if (views != null) {
            Class secretClass = views.getClass();
            List<String> text = new ArrayList<String>();
            try {
                Field outerFields[] = secretClass.getDeclaredFields();
                Log.v(TAG, "bigContentView: " + secretClass);
                for (int i = 0; i < outerFields.length; i++) {
                    if (!outerFields[i].getName().equals("mActions")){
                        continue;
                    }
                    outerFields[i].setAccessible(true);
                    ArrayList<Object> actions = (ArrayList<Object>) outerFields[i].get(views);
                    for (Object action : actions) {
                        Field innerFields[] = action.getClass().getDeclaredFields();
                        Log.v(TAG, "action.getClass: " + action.getClass());
                        Object value = null;
                        Integer type = null;
                        Integer viewId = null;
                        for (Field field : innerFields) {
                            field.setAccessible(true);
                            if (field.getName().equals("value")) {
                                value = field.get(action);
                            } else if (field.getName().equals("type")) {
                                type = field.getInt(action);
                            } else if (field.getName().equals("viewId")) {
                                viewId = field.getInt(action);
                            } else if (field.getName().equals("bitmap")) {
                                bitmap = (Bitmap) field.get(action);
                            } else if (field.getName().equals("pendingIntent")) {
                                pendingIntent = (PendingIntent) field.get(action);
                            }
                            Log.v(TAG, "field.getName(): " + field.getName());
                        }
                        if (type == null) {
                            continue;
                        }
                        if (type == 9 || type == 10) {
                            if (value != null) {
                                Log.v(TAG, "ValueId: " + viewId + " = " + value.toString());
                                text.add(value.toString());
                                if (text.size() > 1) {
                                    textConcated.append(value.toString()).append("#");
                                } else {
                                    textIntent = value.toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return textConcated;
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
