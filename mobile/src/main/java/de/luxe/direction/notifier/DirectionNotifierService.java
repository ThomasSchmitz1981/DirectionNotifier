package de.luxe.direction.notifier;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DirectionNotifierService extends AccessibilityService {

    static final String TAG = "DirNotifierService";

    private AccessibilityServiceInfo info = new AccessibilityServiceInfo();
    private Bitmap bitmap;
    private int noti_id = 1;


    /**
     * Callback for {@link AccessibilityEvent}s.
     *
     * @param event The new event. This event is owned by the caller and cannot be used after
     *              this method returns. Services wishing to use the event after this method returns should
     *              make a copy.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (noti_id != 1) {
            manager.cancel(noti_id - 1);
        }
        Notification direction;
        String textConcated = "";
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.v(TAG, "Recieved event");
            Parcelable parcelableData = event.getParcelableData();
            if (parcelableData == null) {
                return;
            }

            if (parcelableData instanceof Notification) {
                direction = (Notification) parcelableData;
                RemoteViews views = direction.bigContentView;
                textConcated = parseContent(views);
            }
            String dirTitle = "New Direction";
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    .setColor(Color.GREEN)
                    .setShowWhen(true)
                    .setContentInfo(textConcated)
                    .setSmallIcon(R.mipmap.direction2)
                    .setLargeIcon(bitmap)
                    .setContentTitle(dirTitle)
                    .setContentText(textConcated);

            Notification notification = builder.build();
            notification.visibility = Notification.VISIBILITY_PUBLIC;
            manager.notify(noti_id, notification);
            noti_id++;

        }

    }

    public String bitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    private Bitmap darkenBitMap(Bitmap bm) {

        Canvas canvas = new Canvas(bm);
        Paint p = new Paint(Color.RED);
        //ColorFilter filter = new LightingColorFilter(0xFFFFFFFF , 0x00222222); // lighten
        ColorFilter filter = new LightingColorFilter(0xFF7F7F7F, 0x0000FF00);    // darken
        p.setColorFilter(filter);
        canvas.drawBitmap(bm, new Matrix(), p);

        return bm;
    }

    private String parseContent(Object views) {
        Class secretClass = views.getClass();
        List<String> text = new ArrayList<String>();
        String textConcated = "";
        try {

            Field outerFields[] = secretClass.getDeclaredFields();
            Log.v(TAG, "bigContentView: " + secretClass);
            for (int i = 0; i < outerFields.length; i++) {
                if (!outerFields[i].getName().equals("mActions")) continue;

                outerFields[i].setAccessible(true);

                ArrayList<Object> actions = (ArrayList<Object>) outerFields[i]
                        .get(views);

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
                        }
                        Log.v(TAG, "field.getName(): " + field.getName());
                    }
                    if (type != null) {
                        if (type == 9 || type == 10) {
                            if (value != null) {
                                Log.v(TAG, "ValueId: " + viewId + " = " + value.toString());
                                text.add(value.toString());
                                if (text.size() > 1) {
                                    textConcated = textConcated + value.toString() + " ";
                                }
                            }
                        }
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return textConcated;
    }


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected()...");

    }

    /**
     * Callback for interrupting the accessibility feedback.
     */
    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt()...");

    }

}
