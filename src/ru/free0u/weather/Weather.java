package ru.free0u.weather;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.*;


public class Weather {

	final String ATTRIBUTE_NAME_DATE = "date";
	final String ATTRIBUTE_NAME_TEMPERATURE = "temp";
	final String ATTRIBUTE_NAME_PRESSURE = "pressure";
	final String ATTRIBUTE_NAME_WIND = "wind";
	final String ATTRIBUTE_NAME_URL_ICON = "url_icon";
	
	long lastUpdated = 0;
	
	private void updateTime() {
		lastUpdated = System.currentTimeMillis();
	}
	
	private String prettyTemp(String a) {
		int t = Integer.parseInt(a);
		if (t > 0) {
			a = "+" + a;
		}
		return a;
	}
	
	private String prettyTemp(String a, String b) {
		return prettyTemp(a) + " / " + prettyTemp(b);
	}
	
	private String pressureConvert(String s) {
		int p = Integer.parseInt(s);
		double p2 = p * 0.750;
		
		int res = (int)p2;
		return String.valueOf(res);
	}
	
	public String getUrl(String city) {
		String url = "http://free.worldweatheronline.com/feed/weather.ashx?format=json&num_of_days=3&key=396acb677e180947121811&q=";
		try {
			city = URLEncoder.encode(city, "UTF-8");
		} catch (Exception ignore) {
		}
		url += city;
		return url;
	}
	
	public String getCityUrl(String city) {
		String url = "http://www.worldweatheronline.com/feed/search.ashx?key=396acb677e180947121811&num_of_results=3&format=json&query=";
		try {
			city = URLEncoder.encode(city, "UTF-8");
		} catch (Exception ignore) {
		}
		url += city;
		return url;
	}
	
	public String getLastUpdated() {
		if (lastUpdated == 0) {
			return null;
		}
		Date a = new Date(lastUpdated);
		
		SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		return ft.format(a);
	}
	
	// parse json in data
	public Map<String, String> getCurrentWeather(String jString) {
		updateTime();
		
		HashMap<String, String> m = new HashMap<String, String>();
		
		try {
			JSONObject ob = new JSONObject(jString);
			JSONObject data = ob.getJSONObject("data");
			
			JSONObject currentCondition = data.getJSONArray("current_condition").getJSONObject(0);
			
			// temperature
			String temperature = currentCondition.getString("temp_C");
			int intTemp = Integer.parseInt(temperature);
			if (intTemp > 0) temperature = "+" + temperature;
			//presssure
			String pressure = pressureConvert(currentCondition.getString("pressure"));
			//icon
			String url = currentCondition.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
			
			m.put(ATTRIBUTE_NAME_TEMPERATURE, temperature);
			m.put(ATTRIBUTE_NAME_PRESSURE, pressure);
			m.put(ATTRIBUTE_NAME_WIND, currentCondition.getString("windspeedKmph"));
			m.put(ATTRIBUTE_NAME_URL_ICON, url);
			
		} catch (JSONException e) {
		}
		return m;
	}
		
	// parse json in data
	public ArrayList<Map<String, Object>> getForecastWeather(String jString) {
		//updateTime();
		
		ArrayList<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		
		try {
			JSONObject ob = new JSONObject(jString);
			JSONArray days = ob.getJSONObject("data").getJSONArray("weather");
			
			for (int i = 0; i < days.length(); ++i) {
				JSONObject day = days.getJSONObject(i);
				
				HashMap<String, Object> m = new HashMap<String, Object>();
				
				// temperature
				String temp = prettyTemp(day.getString("tempMaxC"), day.getString("tempMinC"));
				// icon
				String url = day.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
						
				m.put(ATTRIBUTE_NAME_DATE, day.getString("date"));
				m.put(ATTRIBUTE_NAME_TEMPERATURE, temp);
				m.put(ATTRIBUTE_NAME_URL_ICON, url);
				
				res.add(m);	
			}
			
		} catch (JSONException e) {
		}
		
		
		return res;
	}

	public String[] parseCities(String jString) {
		String[] res = new String[0];
		
		
		try {
			JSONObject ob = new JSONObject(jString);
			
			JSONArray cities = ob.getJSONObject("search_api").getJSONArray("result");
			
			res = new String[cities.length()];
			for (int i = 0; i < cities.length(); ++i) {
				JSONObject city = cities.getJSONObject(i);
				
				String areaName = city.getJSONArray("areaName").getJSONObject(0).getString("value");
				String country = city.getJSONArray("country").getJSONObject(0).getString("value");
				String region = city.getJSONArray("region").getJSONObject(0).getString("value");

				res[i] = areaName + ", " + country + " (" + region + ")";
			}
		} catch (JSONException e) {
		}
		
		return res;
	}
}
