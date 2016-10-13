package com.home.sms;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

/**
 * Created by Home on 10/11/2016.
 */

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref=findPreference(key);

        if(pref instanceof ListPreference){
            ListPreference listPref=(ListPreference)pref;
            pref.setSummary(listPref.getEntry());
            return;
        }
        if(SMS.FONT_SIZE.equals(key)){
            EditTextPreference editTextPreference=(EditTextPreference)pref;
            String txt=editTextPreference.getText();
            if((!TextUtils.isEmpty(txt) && Float.parseFloat(txt)>0)){
                editTextPreference.setSummary(txt);
            }
            else
            {
                editTextPreference.setSummary(SMS.DEFAULT_FONT_SIZE);
                editTextPreference.setText(SMS.DEFAULT_FONT_SIZE);
            }
        }

    }
}
