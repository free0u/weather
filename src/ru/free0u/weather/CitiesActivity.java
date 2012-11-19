package ru.free0u.weather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class CitiesActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cities);
        
        Button bt = (Button)findViewById(R.id.button1);
        bt.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.putExtra("city", "Moscow");
				setResult(RESULT_OK, intent);
				finish();
			}
		});
        
        
        Button bt2 = (Button)findViewById(R.id.button2);
        bt2.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				SharedPreferences pref = getSharedPreferences("cities", MODE_PRIVATE);
				Editor ed = pref.edit();
				ed.remove("cities");
				ed.commit();
				
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		});
    }
}
