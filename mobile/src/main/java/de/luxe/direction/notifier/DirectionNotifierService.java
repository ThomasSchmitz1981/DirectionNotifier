/**
 * direction notifier
 *
 * @author deLUXe
 * @date 2017-02-07
 */
package de.luxe.direction.notifier;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private PendingIntent pendingIntent;
    private String textIntent;
    private Map<String, Integer> notis = new HashMap<String, Integer>();



    /**
     * Callback for {@link AccessibilityEvent}s.
     *
     * @param event The new event. This event is owned by the caller and cannot be used after
     *              this method returns. Services wishing to use the event after this method returns should
     *              make a copy.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.v(TAG, "Received event");
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
            Integer notiId = new Random().nextInt();
            if (!notis.isEmpty()) {
                managerCompat.cancel(notis.get("0"));
                notis.remove("0");
            }
            if (bitmap != null) {
                bitmap.recycle();
            }
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
                DirectionNotification model = new DirectionNotification(dirTitle, textConcated.toString(),
                        bitmap, pendingIntent, textIntent);
                // Set up the notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setColor(Color.GREEN)
                        .setShowWhen(true)
                        .setContentInfo(textConcated.toString())
                        .setSmallIcon(R.mipmap.direction2)
                        .setLargeIcon(bitmap)
                        .setContentTitle(dirTitle)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentText(textConcated.toString())
                        .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .setSummaryText(textConcated.toString())
                                .bigLargeIcon(bitmap)
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
        }

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
        if(bitmap != null){
            bitmap.recycle();
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
                                    textConcated.append(value.toString()).append(" ");
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




}
