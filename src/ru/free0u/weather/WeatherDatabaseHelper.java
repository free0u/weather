package ru.free0u.weather;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class WeatherDatabaseHelper {
	ContentValues cvCity, cvForecast;
	Context context;
	WeatherHelper weather;
	
	public WeatherDatabaseHelper(Context context) {
		this.context = context;
		
		
		cvCity= new ContentValues();
		cvForecast = new ContentValues();
		weather = new WeatherHelper();
	}
	
	public int getUpdateTime(String city) {
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_CITY_URI, city);
		Cursor c = context.getContentResolver().query(uri, null, null, null, null);
		int res;
		if (c.moveToFirst()) {
			res = c.getInt(c.getColumnIndex("last_upd"));
		} else
		{
			res =  -1;
		}
		return res;
	}
	
	void updateCurrentWeather(String city, Map<String, String> data) {
		
		int id = haveCity(city);
		if (id == -1) {
			addCity(city);
			id = haveCity(city);
		}
		
		cvCity.put("last_upd", WeatherHelper.getUnixTime());
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_CITY_URI, city);
		context.getContentResolver().update(uri, cvCity, null, null);
		
		cvForecast.put("city_id", id);
		cvForecast.put("date", "0");
		cvForecast.put("temp", data.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
		cvForecast.put("pressure", data.get(weather.ATTRIBUTE_NAME_PRESSURE));
		cvForecast.put("wind", data.get(weather.ATTRIBUTE_NAME_WIND));
		cvForecast.put("url_icon", data.get(weather.ATTRIBUTE_NAME_URL_ICON));
		cvForecast.put("updated_icon", 0);
		
		context.getContentResolver().delete(WeatherContentProvider.CONTENT_WEATHER_URI, 
				"city_id = ? and date = ?", new String[] {Integer.toString(id), "0"});
		
		context.getContentResolver().insert(WeatherContentProvider.CONTENT_WEATHER_URI, cvForecast);
	}
	
	Map<String, Object> getCurrentWeather(String city) {
		Map<String, Object> res = new HashMap<String, Object>();
		int id = haveCity(city);
		if (id != -1) {
			Cursor c = context.getContentResolver().query(WeatherContentProvider.CONTENT_WEATHER_URI, 
					null, "city_id = ? and date = ?", new String[] {Integer.toString(id), "0"}, null);
			if (!c.moveToFirst()) {
				res = null;
			} else {
				res.put(weather.ATTRIBUTE_NAME_TEMPERATURE, c.getString(c.getColumnIndex("temp")));
				res.put(weather.ATTRIBUTE_NAME_PRESSURE, c.getString(c.getColumnIndex("pressure")));
				res.put(weather.ATTRIBUTE_NAME_WIND, c.getString(c.getColumnIndex("wind")));
				res.put(weather.ATTRIBUTE_NAME_URL_ICON, c.getString(c.getColumnIndex("url_icon")));
				res.put("updated_icon", c.getString(c.getColumnIndex("updated_icon")));
				res.put("icon", c.getBlob(c.getColumnIndex("icon")));
			}
		} else {
			return null;
		}
		return res;
	}
	
	void updateForecastWeather(String city, ArrayList<Map<String, Object>> data) {
		int id = haveCity(city);
		if (id == -1) {
			addCity(city);
			id = haveCity(city);
		}
		cvCity.put("last_upd", WeatherHelper.getUnixTime());
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_CITY_URI, city);
		context.getContentResolver().update(uri, cvCity, null, null);
		
		
		context.getContentResolver().delete(WeatherContentProvider.CONTENT_WEATHER_URI, 
				"city_id = ? and date != ?", new String[] {Integer.toString(id), "0"});
		
		for (int i = 0; i < data.size(); ++i) {
			Map<String, Object> day = data.get(i);
			
			cvForecast.put("city_id", id);
			cvForecast.put("date", (String)day.get(weather.ATTRIBUTE_NAME_DATE));
			cvForecast.put("temp", (String)day.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
			cvForecast.put("pressure", "0"); // stub
			cvForecast.put("wind", "0"); // stub
			cvForecast.put("url_icon", (String)day.get(weather.ATTRIBUTE_NAME_URL_ICON));
			cvForecast.put("updated_icon", 0);
			context.getContentResolver().insert(WeatherContentProvider.CONTENT_WEATHER_URI, cvForecast);
		}
	}
	
	ArrayList<Map<String, Object>> getForecastWeather(String city) {
		ArrayList<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		
		int id = haveCity(city);
		
		if (id != -1) {
			Cursor c = context.getContentResolver().query(WeatherContentProvider.CONTENT_WEATHER_URI, 
					null, "city_id = ? and date != ?", new String[] {Integer.toString(id), "0"}, null);
			if (c == null) {
				res = null;
			} else {
				if (c.moveToFirst()) {
					do {
						HashMap<String, Object> m = new HashMap<String, Object>();
						m.put(weather.ATTRIBUTE_NAME_DATE, c.getString(c.getColumnIndex("date")));
						m.put(weather.ATTRIBUTE_NAME_TEMPERATURE, c.getString(c.getColumnIndex("temp")));
						m.put(weather.ATTRIBUTE_NAME_URL_ICON, c.getString(c.getColumnIndex("url_icon")));
						m.put("updated_icon", c.getString(c.getColumnIndex("updated_icon")));
						m.put("icon", c.getBlob(c.getColumnIndex("icon")));
						res.add(m);
					} while (c.moveToNext());
				}
			}
		} else {
			res =  null;
		}
		return res;
	}
	
	
	void updateAll() {
		//Cursor c = db.query("cities", null, null, null, null, null, null);
		Cursor c = context.getContentResolver().query(WeatherContentProvider.CONTENT_CITY_URI, null, null, null, null);
		if (c == null) return;
		if (c.moveToFirst()) {
			do {
				String city = c.getString(c.getColumnIndex("title"));
				WeatherUpdater task = new WeatherUpdater(city);
				task.execute();
			} while (c.moveToNext());
		}
	}
	
	void setIcon(String url, byte[] pic) {
		cvForecast.clear();
		cvForecast.put("icon", pic);
		cvForecast.put("updated_icon", "1");
		context.getContentResolver().update(WeatherContentProvider.CONTENT_WEATHER_URI, cvForecast, 
				"url_icon = ? and updated_icon = ?", new String[] {url, "0"});
	}
	
	// return id
	int haveCity(String city) {
		int res;
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_CITY_URI, city);
		Cursor c = context.getContentResolver().query(uri, null, null, null, null);
		if (c.moveToFirst()) {
			res = c.getInt(c.getColumnIndex("id"));
		} else
		{
			res = -1;
		}
		return res;
		
	}
	
	void addCity(String city) {
		if (haveCity(city) == -1) {
			cvCity.put("title", city);
			cvCity.put("last_upd", -1);
			context.getContentResolver().insert(WeatherContentProvider.CONTENT_CITY_URI, cvCity);
		}
	}
	
	ArrayList<String> getCities() {
		ArrayList<String> res = new ArrayList<String>();
		Cursor c = context.getContentResolver().query(WeatherContentProvider.CONTENT_CITY_URI, null, null, null, null);
		if (c == null) {
			res = null;
		} else {
			if (c.moveToFirst()) {
				do {
					String city = c.getString(c.getColumnIndex("title"));
					res.add(city);
				} while (c.moveToNext());
			}
		}
		return res;
	}
	
	void clearTables() {
		context.getContentResolver().delete(WeatherContentProvider.CONTENT_CITY_URI, null, null);
		context.getContentResolver().delete(WeatherContentProvider.CONTENT_WEATHER_URI, null, null);
	}
	
	private void logCursor(Cursor c) {
	    if (c != null) {
	      if (c.moveToFirst()) {
	        String str;
	        do {
	          str = "";
	          for (String cn : c.getColumnNames()) {
	            String data = null;
	        	  if (cn.equals("icon")) {
	        		  byte[] a = c.getBlob(c.getColumnIndex(cn));
	        		  data = "{";
	        		  int cnt = 0;
	        		  if (a != null) {
	        			  for (int i = 0; i < a.length; ++i) {
		        			  data += (a[i] + ", ");
		        			  cnt += a[i];
	        			  }
	        		  }
	        		  data += "}";
	        		  
	        		  data = "{ " + cnt + " }";
	        	  } else
	        	  {
	        		  data = c.getString(c.getColumnIndex(cn));
	        	  }
	        	  
	            	
	        	  str = str.concat(cn + " = " + data + "; ");
	          }
	        } while (c.moveToNext());
	      } else {
	      }
	    } else {
	    }
	}
	

	
	class WeatherUpdater extends AsyncTask<Void, Void, String> {
    	String urlData;
    	String city;
    	
    	public WeatherUpdater(String city) {
    		this.city = city;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		urlData = weather.getUrl(city);
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
			// update db
			if (data != null) {
				Log.i("db", "wd.onPostExecute(). updateWeather()");
				ArrayList<Map<String, Object>> data1 = weather.getForecastWeather(data);
				updateForecastWeather(city, data1);
				
				Map<String, String> data2 = weather.getCurrentWeather(data);
				updateCurrentWeather(city, data2);
			}
		}
    }
}
