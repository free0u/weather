package ru.free0u.weather;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class CitiesActivity extends Activity implements OnClickListener {
	ListView listView;
	String[] cities;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cities);
        
        Button bt = (Button)findViewById(R.id.button1);
        bt.setOnClickListener(this);
        
        Button bt2 = (Button)findViewById(R.id.button2);
        bt2.setOnClickListener(this);
        
        listView = (ListView)findViewById(R.id.listViewCities);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

	private void setUpList(String[] arr) {
		//String[] cities2 = {"Moscow", "St Petersburg"};
		//arr = cities2;
		cities = arr;
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arr);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				TextView tv = (TextView)view;
				Intent intent = new Intent();
				intent.putExtra("city", tv.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1: // find
			EditText et = (EditText)findViewById(R.id.editTextFind);
			String s = et.getText().toString();
			CityDownloader task = new CityDownloader(s);
			task.execute();
			break;
		case R.id.button2: // clear
			SharedPreferences pref = getSharedPreferences("cities", MODE_PRIVATE);
			Editor ed = pref.edit();
			ed.remove("cities");
			ed.commit();
			setResult(RESULT_OK, new Intent());
			finish();
			break;
		}
	}
	
	class CityDownloader extends AsyncTask<Void, Void, String> {
    	String urlData;
    	Weather weather;
    	
    	public CityDownloader(String city) {
    		weather = new Weather();
    		urlData = weather.getCityUrl(city);
    	}
    	
    	protected String convertStreamToString(InputStream is) {
    	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    	    return s.hasNext() ? s.next() : "";
    	}
    	
		@Override
		protected String doInBackground(Void... arg0) {
			String res = "";
			HttpURLConnection conn = null;
			try {
				URL url = new URL(urlData);
				conn = (HttpURLConnection)url.openConnection();
				InputStream in = new BufferedInputStream(conn.getInputStream());
				res = convertStreamToString(in);
			}
			catch (Exception ignore) {
			}
			finally {
				conn.disconnect();
			}
			return res;
		}
    	
		@Override
		protected void onPostExecute(String data) {
			setUpList(weather.parseCities(data));
		}
    }
	
}
