package ru.free0u.weather;

import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        LinearLayout lin = (LinearLayout)findViewById(R.id.weatherLayout);
        LayoutInflater inf = getLayoutInflater();
        

        View v2 = inf.inflate(R.layout.weather, lin, false);
        lin.addView(v2);
        
        for (int i = 0; i < 3; ++i) {
        	View v1 = inf.inflate(R.layout.weather_item, lin, false);
            lin.addView(v1);
            
        }
    }
}
