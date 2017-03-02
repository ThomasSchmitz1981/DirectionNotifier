/**
 *
 * @author deLUXe
 * @date 2017-02-07
 */
package de.luxe.direction.notifier;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

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
        donate.setBackgroundColor(Color.rgb(204,255,255));
        donate.setMovementMethod(LinkMovementMethod.getInstance());

    }

}
