package ru.free0u.weather;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnItemSelectedListener {
	Weather weather;
	ArrayList<Map<String, Object>> weatherListData;
	boolean updateButtonIsActive = true;
	LayoutInflater inflater;
	
	final String ATTRIBUTE_NAME_CITIES = "cities";
	String[] cities = null;
	private int curPosCity;
	
	// change UI - update weather forecast
	private void updateForecastWeather(ArrayList<Map<String, Object>> data) {
		LinearLayout lin = (LinearLayout)findViewById(R.id.linearLayoutWeatherList);
		lin.removeAllViewsInLayout();
		
		for (int i = 0; i < data.size(); ++i) {
			Map<String, Object> day = data.get(i);
			
			View v = inflater.inflate(R.layout.weather_item, null, false);
			
			TextView tv = (TextView)v.findViewById(R.id.textViewDate);
			tv.setText((String)day.get(weather.ATTRIBUTE_NAME_DATE));
			
			tv = (TextView)v.findViewById(R.id.textTemp);
			tv.setText((String)day.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
			
			ImageView iv = (ImageView)v.findViewById(R.id.imageIconWeather);
			iv.setImageBitmap(null);
			ImageViewSetter task = new ImageViewSetter(iv);
			task.execute((String)day.get(weather.ATTRIBUTE_NAME_URL_ICON));
			
			lin.addView(v);
			
		}
		
	}

	// change UI - update current weather
	private void updateCurrentWeather(Map<String, String> data) {
		// icon
		ImageView icon = (ImageView)findViewById(R.id.imageWeatherNow);
		icon.setImageBitmap(null);
		ImageViewSetter task = new ImageViewSetter(icon);
		task.execute(data.get(weather.ATTRIBUTE_NAME_URL_ICON));
		
		// temperature
		TextView tv = (TextView)findViewById(R.id.textViewTempNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_TEMPERATURE) + " " +
				getResources().getString(R.string.celsius));
		
		// wind
		tv = (TextView)findViewById(R.id.textViewWindNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_WIND));
		
		// pressure
		tv = (TextView)findViewById(R.id.TextViewPressureNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_PRESSURE));
		
		// update "last updated" field
		tv = (TextView)findViewById(R.id.textViewLastUpdated);
		String lastUpd = weather.getLastUpdated();
		tv.setText(lastUpd == null ? getResources().getString(R.string.neverupdated) : lastUpd);
		
		// city name
		tv = (TextView)findViewById(R.id.textViewCityName);
		if (curPosCity < cities.length) {
			tv.setText(cities[curPosCity]);
		} else
		{
			tv.setText("");
		}
	}
	
	
	private void updateWeather() {
		if (curPosCity < cities.length) {
			WeatherDownloader task = new WeatherDownloader();
			task.execute();
		}
	}
	
	
	private Set<String> readCitiesFromPreferences() {
		SharedPreferences pref = getSharedPreferences("cities", MODE_PRIVATE);
		Set<String> res = pref.getStringSet(ATTRIBUTE_NAME_CITIES, null);
		return res;
	}
	
	private void writeCitiesIntoPreferences(Set<String> set) {
		SharedPreferences pref = getSharedPreferences("cities", MODE_PRIVATE);
		Editor ed = pref.edit();
		ed.putStringSet(ATTRIBUTE_NAME_CITIES, set);
		ed.commit();
		
	}
	
	private void addCity(String s) {
		HashSet<String> set = (HashSet<String>) readCitiesFromPreferences();
		if (set == null) {
			set = new HashSet<String>(); 
		}
		set.add(s);
		writeCitiesIntoPreferences(set);
	}
	
	private String[] setOfStringToArray(Set<String> set) {
		if (set == null) {
			return new String[0];
		}
		String[] res = new String[set.size()];
		int cnt = 0;
		for (String s : set) {
			res[cnt++] = s;
		}
		return res;
	}
	
	private void setUpSpinner() {
		curPosCity = 0;
		
		Spinner spinner = (Spinner)findViewById(R.id.spinner1);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cities);
    	ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	spinner.setAdapter(ad);
    	spinner.setOnItemSelectedListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null) return;
		if (data.hasExtra("city")) {
			addCity(data.getStringExtra("city"));
		}
		cities = setOfStringToArray(readCitiesFromPreferences());
		setUpSpinner();
		updateWeather();
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
			long arg3) {
		Log.i("cities", "Pos: " + pos);
		if (curPosCity != pos) {
			curPosCity = pos;
			updateWeather();
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
   
        weather = new Weather();
        
        // add weather.xml view
        LinearLayout lin = (LinearLayout)findViewById(R.id.weatherLayout);
        inflater = getLayoutInflater();
        inflater.inflate(R.layout.weather, lin, true);
        
        // setup buttons
        ImageView updateButton = (ImageView)findViewById(R.id.imageViewUpdate);
        updateButton.setOnClickListener(this);
        ImageView addButton = (ImageView)findViewById(R.id.imageViewAdd);
        addButton.setOnClickListener(this);
    
        // setup spinner
        cities = setOfStringToArray(readCitiesFromPreferences());
        if (cities.length == 0) {
        	Intent intent = new Intent(this, CitiesActivity.class);
    		startActivityForResult(intent, 0);
        } else {
        	setUpSpinner();
            updateWeather();
        }
    }
    
    public void onClick(View v) {
    	switch(v.getId()) {
    	case R.id.imageViewUpdate:
    		if (updateButtonIsActive) {
        		updateButtonIsActive = false;
        		updateWeather();
    		}
    		break;
    	case R.id.imageViewAdd:
    		Intent intent = new Intent(this, CitiesActivity.class);
    		startActivityForResult(intent, 0);
    		break;
    	}
    	
	}
    
    class WeatherDownloader extends AsyncTask<Void, Void, String> {
    	String urlData;
    	
    	@Override
    	protected void onPreExecute() {
    		urlData = weather.getUrl(cities[curPosCity]);
    		
    		updateButtonIsActive = false;
    		
    		Toast t = Toast.makeText(getApplicationContext(), "Weather downloading...", Toast.LENGTH_SHORT);
    		t.show();
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
			updateButtonIsActive = true;
			
			// change UI:
			if (data != null) {
				updateForecastWeather(weather.getForecastWeather(data));
				updateCurrentWeather(weather.getCurrentWeather(data));
			}
			
			Toast t = Toast.makeText(getApplicationContext(), "Weather downloaded!", Toast.LENGTH_SHORT);
    		t.show();
		}
    }
    
    // usage: 
    // ctor ImageViewSetter(ImageView v)
    // execute(url)
    class ImageViewSetter extends AsyncTask<String, Void, Bitmap> {
    	ImageView v;
    	
    	public ImageViewSetter(ImageView v) {
    		this.v = v;
		}
    	
		@Override
		protected Bitmap doInBackground(String... arg0) {
			Bitmap bm = null;
			String url = arg0[0];
			try {
		        URLConnection conn = new URL(arg0[0]).openConnection();
		        conn.connect();
		        bm = BitmapFactory.decodeStream(conn.getInputStream());
		    } catch (Exception ignore) {
		    }
			return bm;
		}
    	
		@Override
		protected void onPostExecute(Bitmap bm) {
			v.setImageBitmap(bm);
		}
    }

	

	
}
