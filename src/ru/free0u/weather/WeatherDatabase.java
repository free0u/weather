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
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class WeatherDatabase {
	final String LOG_TAG = "db";
	DBHelper dbHelper;
	SQLiteDatabase db;
	ContentValues cvCity, cvForecast;
	Context context;
	WeatherHelper weather;
	
	public WeatherDatabase(Context context) {
		this.context = context;
		
		dbHelper = new DBHelper(context);
		db = dbHelper.getWritableDatabase();
		cvCity= new ContentValues();
		cvForecast = new ContentValues();
		weather = new WeatherHelper();
	}
	
	void test() {
		Log.i(LOG_TAG, "void test()");

		
		//clearTables();
		// ===
		Cursor c;
//		getCurrentWeather("spb");
//		c = db.query("weather", null, null, null, null, null, null);
//		c = db.query("weather", null, "city_id = ? and date = ?", new String[] {Integer.toString(haveCity("spb")), "0"}, 
//				null, null, null);
		//logCursor(c);
		//updateCurrentWeather("msk", weather.getCurrentWeatherStub());
		
		//c = db.query("cities", null, null, null, null, null, null);
		//logCursor(c);
		
		//c = db.query("cities inner join weather on cities.id = weather.city_id", null, null, null, null, null, null);
		c = db.query("cities", null, null, null, null, null, null);
		//logCursor(c);
		
		// ==
		
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public int getUpdateTime(String city) {
		Cursor c = db.query("cities", null, "title = ?", new String[] {city}, null, null, null);
		if (c.moveToFirst()) {
			return c.getInt(c.getColumnIndex("last_upd"));
		} else
		{
			return -1;
		}
	}
	
	void updateCurrentWeather(String city, Map<String, String> data) {
		int id = haveCity(city);
		if (id == -1) {
			addCity(city);
			id = haveCity(city);
		}
		
		cvCity.put("title", city);
		cvCity.put("last_upd", WeatherHelper.getUnixTime());
		db.update("cities", cvCity, "id = ?", new String[] {Integer.toString(id)});
		
		cvForecast.put("city_id", id);
		cvForecast.put("date", "0");
		cvForecast.put("temp", data.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
		cvForecast.put("pressure", data.get(weather.ATTRIBUTE_NAME_PRESSURE));
		cvForecast.put("wind", data.get(weather.ATTRIBUTE_NAME_WIND));
		cvForecast.put("url_icon", data.get(weather.ATTRIBUTE_NAME_URL_ICON));
		
		db.delete("weather", "city_id = ? and date = ?", new String[] {Integer.toString(id), "0"});
		db.insert("weather", null, cvForecast);
	}
	
	Map<String, String> getCurrentWeather(String city) {
		Map<String, String> res = new HashMap<String, String>();
		int id = haveCity(city);
		if (id != -1) {
			Cursor c = db.query("weather", null, "city_id = ? and date = ?", new String[] {Integer.toString(id), "0"}, 
					null, null, null);
			if (!c.moveToFirst()) {
				return null;
			}
			res.put(weather.ATTRIBUTE_NAME_TEMPERATURE, c.getString(c.getColumnIndex("temp")));
			res.put(weather.ATTRIBUTE_NAME_PRESSURE, c.getString(c.getColumnIndex("pressure")));
			res.put(weather.ATTRIBUTE_NAME_WIND, c.getString(c.getColumnIndex("wind")));
			res.put(weather.ATTRIBUTE_NAME_URL_ICON, c.getString(c.getColumnIndex("url_icon")));
			return res;
		} else {
			return null;
		}
	}
	
	void updateForecastWeather(String city, ArrayList<Map<String, Object>> data) {
		int id = haveCity(city);
		if (id == -1) {
			addCity(city);
			id = haveCity(city);
		}
		
		cvCity.put("title", city);
		cvCity.put("last_upd", WeatherHelper.getUnixTime());
		
		db.update("cities", cvCity, "id = ?", new String[] {Integer.toString(id)});
		
		db.delete("weather", "city_id = ? and date != ?", new String[] {Integer.toString(id), "0"});
		for (int i = 0; i < data.size(); ++i) {
			Map<String, Object> day = data.get(i);
			
			cvForecast.put("city_id", id);
			cvForecast.put("date", (String)day.get(weather.ATTRIBUTE_NAME_DATE));
			cvForecast.put("temp", (String)day.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
			cvForecast.put("pressure", "0"); // stub
			cvForecast.put("wind", "0"); // stub
			cvForecast.put("url_icon", (String)day.get(weather.ATTRIBUTE_NAME_URL_ICON));
			
			db.insert("weather", null, cvForecast);
		}
	}
	
	ArrayList<Map<String, Object>> getForecastWeather(String city) {
		ArrayList<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		
		int id = haveCity(city);
		if (id != -1) {
			Cursor c = db.query("weather", null, "city_id = ? and date != ?", new String[] {Integer.toString(id), "0"}, 
					null, null, null);
			
			if (c == null) return null;
			if (c.moveToFirst()) {
				do {
					HashMap<String, Object> m = new HashMap<String, Object>();
					m.put(weather.ATTRIBUTE_NAME_DATE, c.getString(c.getColumnIndex("date")));
					m.put(weather.ATTRIBUTE_NAME_TEMPERATURE, c.getString(c.getColumnIndex("temp")));
					m.put(weather.ATTRIBUTE_NAME_URL_ICON, c.getString(c.getColumnIndex("temp")));
					res.add(m);
				} while (c.moveToNext());
			}
			return res;
		} else {
			return null;
		}
	}
	
	
	void updateAll() {
		Cursor c = db.query("cities", null, null, null, null, null, null);
		
		if (c == null) return;
		if (c.moveToFirst()) {
			do {
				String city = c.getString(c.getColumnIndex("title"));
				WeatherUpdater task = new WeatherUpdater(city);
				task.execute();
			} while (c.moveToNext());
		}
	}
	
	
	// retund id
	int haveCity(String city) {
		Cursor c = db.query("cities", null, "title = ?", new String[] {city}, null, null, null);
		if (c.moveToFirst()) {
			return c.getInt(c.getColumnIndex("id"));
		} else
		{
			return -1;
		}
	}
	
	void addCity(String city) {
		if (haveCity(city) == -1) {
			Log.i(LOG_TAG, "dont have city " + city);
			cvCity.put("title", city);
			cvCity.put("last_upd", -1);
			db.insert("cities", null, cvCity);
		} else {
			Log.i(LOG_TAG, "have city " + city);
		}
	}
	
	ArrayList<String> getCities() {
		ArrayList<String> res = new ArrayList<String>();
		Cursor c = db.query("cities", null, null, null, null, null, null);
		if (c == null) return res;
		if (c.moveToFirst()) {
			do {
				String city = c.getString(c.getColumnIndex("title"));
				res.add(city);
			} while (c.moveToNext());
		}
		return res;
	}
	
	void clearTables() {
		Log.i(LOG_TAG, "clearTables");
		db.delete("weather", null, null);
		db.delete("cities", null, null);
	}
	
	private void logCursor(Cursor c) {
	    if (c != null) {
	      if (c.moveToFirst()) {
	        String str;
	        do {
	          str = "";
	          for (String cn : c.getColumnNames()) {
	            str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
	          }
	          Log.i(LOG_TAG, str);
	        } while (c.moveToNext());
	      } else {
	    	  Log.i(LOG_TAG, "!c.moveToFirst()");
	      }
	    } else {
	    	Log.i(LOG_TAG, "Cursor is null");
	    }
	
	}
	
	class DBHelper extends SQLiteOpenHelper {
		public DBHelper(Context context) {
			super(context, "weather_db", null, 5);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(LOG_TAG, "onCreate()");
			String q;
			
			q = "CREATE TABLE cities (id integer primary key autoincrement, title text, last_upd integer);";
			db.execSQL(q);
			q = "CREATE TABLE weather (" +
					"id integer primary key autoincrement, " + 
					"city_id integer, " + 
					"date text, " +
					"temp text, " +
					"pressure text, " + 
					"wind text, " +
					"url_icon text);";
			db.execSQL(q);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(LOG_TAG, "onUpgrade() " + oldVersion + " " + newVersion);
			if (newVersion == 5) {
				String q = "CREATE TABLE weather (" +
						"id integer primary key autoincrement, " + 
						"city_id integer, " + 
						"date text, " +
						"temp text, " +
						"pressure text, " + 
						"wind text, " +
						"url_icon text);";
				db.execSQL(q);
			}
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
				ArrayList<Map<String, Object>> data1 = weather.getForecastWeather(data);
				updateForecastWeather(city, data1);
				
				Map<String, String> data2 = weather.getCurrentWeather(data);
				updateCurrentWeather(city, data2);
				
				// TODO delete toast
				Toast t = Toast.makeText(context, "Updated all", Toast.LENGTH_SHORT);
	    		t.show();
			}
		}
    }
}
