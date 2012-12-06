package ru.free0u.weather;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public class WeatherContentProvider extends ContentProvider {
	private static String AUTHORITY = "ru.free0u.weather.provider";
	public static Uri CONTENT_URI = Uri.parse(ContentResolver.SCHEME_CONTENT
			+ "://" + AUTHORITY);

	// tables
	private static String CITIES_TABLE = "cities";
	private static String WEATHER_TABLE = "weather";

	// content uri
	public static Uri CONTENT_CITY_URI = Uri.withAppendedPath(CONTENT_URI,
			CITIES_TABLE);
	public static Uri CONTENT_WEATHER_URI = Uri.withAppendedPath(CONTENT_URI,
			WEATHER_TABLE);

	private static final int CODE_CITIES = 1;
	private static final int CODE_CITY = 2;
	private static final int CODE_WEATHER = 3;
	
	private static UriMatcher MATCHER = null;
	static {
		MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		MATCHER.addURI(AUTHORITY, CITIES_TABLE, CODE_CITIES);
		MATCHER.addURI(AUTHORITY, CITIES_TABLE + "/*", CODE_CITY);
		MATCHER.addURI(AUTHORITY, WEATHER_TABLE, CODE_WEATHER);
	}

	private SQLiteOpenHelper helper;

	@Override
	public boolean onCreate() {
		helper = new DBHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArg) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		String table = null;
		Uri notifyUri = null;
		switch (MATCHER.match(uri)) {
		case CODE_CITIES:
			table = CITIES_TABLE;
			notifyUri = CONTENT_CITY_URI;
			break;
		case CODE_WEATHER:
			table = WEATHER_TABLE;
			notifyUri = CONTENT_WEATHER_URI;
			break;
		default:
			throw new IllegalArgumentException("Wrong uri: " + uri.toString());
		}
		int res = db.delete(table, selection, selectionArg);
		getContext().getContentResolver().notifyChange(notifyUri, null);
		return res;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues cv) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		String table = null;
		Uri notifyUri = null;
		switch (MATCHER.match(uri)) {
		case CODE_CITIES:
			table = CITIES_TABLE;
			notifyUri = CONTENT_CITY_URI;
			break;
		case CODE_WEATHER:
			table = WEATHER_TABLE;
			notifyUri = CONTENT_WEATHER_URI;
			break;
		default:
			throw new IllegalArgumentException("Wrong uri: " + uri.toString());
		}
		long rowID = db.insert(table, null, cv);
		getContext().getContentResolver().notifyChange(notifyUri, null);
		return Uri.withAppendedPath(notifyUri, Long.toString(rowID));
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String table = null;
		Uri notifyUri = null;
		switch (MATCHER.match(uri)) {
		case CODE_CITIES:
			table = CITIES_TABLE;
			notifyUri = CONTENT_CITY_URI;
			break;
		case CODE_CITY:
			table = CITIES_TABLE;
			notifyUri = CONTENT_CITY_URI;
			selection = "title = ?";
			selectionArgs = new String[] {uri.getLastPathSegment()};
			break;
		case CODE_WEATHER:
			table = WEATHER_TABLE;
			notifyUri = CONTENT_WEATHER_URI;
			break;
		default:
			throw new IllegalArgumentException("Wrong uri: " + uri.toString());
		}
		Cursor c = db.query(table, projection, selection, selectionArgs, null, null, null);
		c.setNotificationUri(getContext().getContentResolver(), notifyUri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues cv, String selection, String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String table = null;
		Uri notifyUri = null;
		switch (MATCHER.match(uri)) {
		case CODE_CITY:
			table = CITIES_TABLE;
			notifyUri = CONTENT_CITY_URI;
			selection = "title = ?";
			selectionArgs = new String[] {uri.getLastPathSegment()};
			break;
		case CODE_WEATHER:
			notifyUri = CONTENT_WEATHER_URI;
			table = WEATHER_TABLE;
			break;
		default:
			throw new IllegalArgumentException("Wrong uri: " + uri.toString());
		}
		int res = db.update(table, cv, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(notifyUri, null);
		return res;
	}

	class DBHelper extends SQLiteOpenHelper {
		public DBHelper(Context context) {
			super(context, "weather_db", null, 9);
		}

		private static final String CREATE_TABLE_CITIES = "CREATE TABLE cities (id integer primary key autoincrement, "
				+ "title text, " + "last_upd integer);";
		private static final String DROP_TABLE_CITIES = "drop table cities if exist";

		private static final String CREATE_TABLE_WEATHER = "CREATE TABLE weather ("
				+ "id integer primary key autoincrement, "
				+ "city_id integer, "
				+ "date text, "
				+ "temp text, "
				+ "pressure text, "
				+ "wind text, "
				+ "url_icon text, "
				+ "updated_icon text default \"0\", " + "icon blob);";
		private static final String DROP_TABLE_WEATHER = "drop table weather if exist";

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_CITIES);
			db.execSQL(CREATE_TABLE_WEATHER);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(DROP_TABLE_CITIES);
			db.execSQL(DROP_TABLE_WEATHER);
			onCreate(db);
		}
	}
}
