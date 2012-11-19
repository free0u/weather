package ru.free0u.weather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

public class Weather {

	final String ATTRIBUTE_NAME_TEMPERATURE = "temp";
	final String ATTRIBUTE_NAME_PRESSURE = "pressure";
	final String ATTRIBUTE_NAME_WIND = "wind";
	final String ATTRIBUTE_NAME_URL_ICON = "url_icon";
	
	
	
	final String ATTRIBUTE_NAME_IMAGE = "image";
	final String urlIcon = "http://www.worldweatheronline.com/images/wsymbols01_png_64/wsymbol_0033_cloudy_with_light_rain_night.png";
	
	final String urlData = "http://free.worldweatheronline.com/feed/weather.ashx?q=St+Petersburg&format=json&num_of_days=3&key=396acb677e180947121811";

	
	private String prettyTemp(String a, String b) {
		return a + "/" + b + " C";
	}
	
	private String prettyTemp(int a, int b) {
		return prettyTemp(Integer.toString(a), Integer.toString(b));
	}

	
	public ArrayList<Map<String, Object>> getForecastWeather(String data) {
		return getData();
	}
	
	public Map<String, String> getCurrentWeather(String data) {
		HashMap<String, String> m = new HashMap<String, String>();
		m.put(ATTRIBUTE_NAME_TEMPERATURE, "+20");
		m.put(ATTRIBUTE_NAME_PRESSURE, "743");
		m.put(ATTRIBUTE_NAME_WIND, "2");
		m.put(ATTRIBUTE_NAME_URL_ICON, urlIcon);
		return m;
	}
	
	public ArrayList<Map<String, Object>> getData() {
		ArrayList<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < 3; ++i) {
			HashMap<String, Object> m = new HashMap<String, Object>();
			m.put(ATTRIBUTE_NAME_TEMPERATURE, prettyTemp((i + 1) * 10 - 41, (i + 1) * 10 + 43));
			m.put(ATTRIBUTE_NAME_IMAGE, urlIcon);
			
			res.add(m);
		}
		return res;
	}
}
