package com.home.sms;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * Created by Home on 10/11/2016.
 */

public class SMS extends Application {
    public static SharedPreferences sp;
    public static final Uri INBOX_URI=Uri.parse("content://sms/inbox");
    public static final String CLIPBOARD = "clipboard";
    public static final String FULL_SMS = "full_sms";
    public static final String DEFAULT_SORT = "default_sort";
    //	public static final String DATE_FORMAT = "date_format";
    public static final String TIME_FORMAT = "time_format";
    public static final String FONT_SIZE = "font_size";
    public static final String CONFIRM_PREF = "confirm_pref";
    public static final String USE_DEFAULT = "use_default";

    //	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_FONT_SIZE = "18";

    public static final String DRAFT_MSG = "draft_msg";

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this,R.xml.settings,false);
        sp=PreferenceManager.getDefaultSharedPreferences(this);
    }
    public static String getClipboardData() {
        return sp.getString(CLIPBOARD, "");
    }

    public static void setClipboardData(String data) {
        sp.edit().putString(CLIPBOARD, data).commit();
    }

    public static boolean showFullText() {
        return sp.getBoolean(FULL_SMS, true);
    }

    public static int getDefaultSort() {
        return Integer.parseInt(sp.getString(DEFAULT_SORT, "3"));
    }

/*	public static String getDateFormat() {
		return sp.getString(DATE_FORMAT, DEFAULT_DATE_FORMAT);
	}*/

    public static boolean is24Hours() {
        return sp.getBoolean(TIME_FORMAT, false);
    }

    public static float getFontSize() {
        return Float.parseFloat(sp.getString(FONT_SIZE, DEFAULT_FONT_SIZE));
    }

    public static boolean showConfirmDialog() {
        return sp.getBoolean(CONFIRM_PREF, true);
    }

    public static boolean useDefaultApp() {
        return sp.getBoolean(USE_DEFAULT, true);
    }

}
