package de.luxe.direction.notifier;

import android.app.PendingIntent;
import android.graphics.Bitmap;

/**
 * Created by deLuxe on 14.02.2017.
 */

public class DirectionNotification{

    private String title;
    private String text;
    private Bitmap icon;
    private PendingIntent intent;
    private String textIntent;

    public DirectionNotification(String title, String text, Bitmap icon, PendingIntent intent, String textIntent) {
        this.title = title;
        this.text = text;
        this.icon = icon;
        this.intent = intent;
        this.textIntent = textIntent;
    }
}
