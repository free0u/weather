package ru.free0u.weather;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	Weather weather;
	ArrayList<Map<String, Object>> weatherListData;
	MySimpleAdapter ad;	
	boolean updateButtonIsActive = true;
	
	private void setUpWeatherList() {
		weatherListData = new ArrayList<Map<String,Object>>();
		String[] from = {weather.ATTRIBUTE_NAME_IMAGE, weather.ATTRIBUTE_NAME_TEMPERATURE};
		int[] to = {R.id.imageIcon, R.id.textTemp};
		ad = new MySimpleAdapter(this, weatherListData, R.layout.weather_item, from, to);
		
		ListView lv = (ListView)findViewById(R.id.listViewWeather);
        lv.setAdapter(ad);
	}
	
	// change UI - update weather forecast
	private void updateForecastWeather(ArrayList<Map<String, Object>> data) {
		weatherListData.clear();
		weatherListData.addAll(data);
		ad.notifyDataSetChanged();
	}

	// change UI - update current weather
	private void updateCurrentWeather(Map<String, String> data) {
		// icon
		ImageView icon = (ImageView)findViewById(R.id.imageWeatherNow);
		ImageViewSetter task = new ImageViewSetter(icon);
		task.execute(data.get(weather.ATTRIBUTE_NAME_URL_ICON));
		
		// temperature
		TextView tv = (TextView)findViewById(R.id.textViewTempNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
		
		// wind
		tv = (TextView)findViewById(R.id.textViewWindNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_WIND));
		
		// pressure
		tv = (TextView)findViewById(R.id.TextViewPressureNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_PRESSURE));
	}
	
	
	private void updateWeather() {
		WeatherDownloader task = new WeatherDownloader();
		task.execute();
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        weather = new Weather();
        
        // add weather.xml view
        LinearLayout lin = (LinearLayout)findViewById(R.id.weatherLayout);
        LayoutInflater inf = getLayoutInflater();
        inf.inflate(R.layout.weather, lin, true);

        // add weather forecast list
        setUpWeatherList();
        
        // setup buttons
        ImageView updateButton = (ImageView)findViewById(R.id.imageViewUpdate);
        updateButton.setOnClickListener(this);
        ImageView addButton = (ImageView)findViewById(R.id.imageViewAdd);
        addButton.setOnClickListener(this);
    
        updateWeather();
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
    		Log.i("test", "add button");
    		break;
    	}
    	
	}
    
    class WeatherDownloader extends AsyncTask<Void, Void, String> {
    	String url;
    	
    	@Override
    	protected void onPreExecute() {
    		url = weather.urlData;
    		updateButtonIsActive = false;
    		
    		Toast t = Toast.makeText(getApplicationContext(), "Weather downloading...", Toast.LENGTH_SHORT);
    		t.show();
    	}
    	
		@Override
		protected String doInBackground(Void... arg0) {
			// TODO get json
			return null;
		}
    	
		@Override
		protected void onPostExecute(String data) {
			updateButtonIsActive = true;
			
			// change UI:
			updateForecastWeather(weather.getForecastWeather(data));
			updateCurrentWeather(weather.getCurrentWeather(data));
			
			Toast t = Toast.makeText(getApplicationContext(), "Weather downloaded!", Toast.LENGTH_SHORT);
    		t.show();
		}
    }
    
    // SimpleAdapter with AsyncTask downloading image into ImageView
    class MySimpleAdapter extends SimpleAdapter {

		public MySimpleAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}
    	
		@Override 
		public void setViewImage(ImageView v, String text) {
			ImageViewSetter task = new ImageViewSetter(v);
	        task.execute(text);
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
