/*
 * File: PreferencesActivity.java
 * Purpose: Preferences activity for Android application
 *
 * Copyright (c) 2009 David Barr, Sergey Belinsky
 *
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of either:
 *
 * a) the GNU General Public License as published by the Free Software
 *    Foundation, version 2, or
 *
 * b) the "Angband licence":
 *    This software may be copied and distributed for educational, research,
 *    and not for profit purposes provided that this copyright and statement
 *    are included in all such copies.  Other copyrights may also apply.
 */

package org.rephial.xyangband;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.WindowManager;

public class PreferencesActivity
	extends PreferenceActivity implements OnSharedPreferenceChangeListener,
		OnPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(Preferences.NAME);

		addPreferencesFromResource(R.xml.preferences);

		// Enable or disable keyboard transparency based on overlap setting
		findPreference(Preferences.KEY_KEYBOAR_DOVERLAP).setOnPreferenceChangeListener(this);
		//findPreference(Preferences.KEY_KEYBOARDOPACITY).setEnabled(Preferences.getKeyboardOverlap());
		//findPreference(Preferences.KEY_MIDDLEOPACITY).setEnabled(Preferences.getKeyboardOverlap());
	}

	@Override
	protected void onResume() {
		super.onResume();

		setSummaryAll(getPreferenceScreen());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		SharedPreferences pref = getSharedPreferences(Preferences.NAME,MODE_PRIVATE);

		if (pref.getBoolean(Preferences.KEY_FULLSCREEN, true)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	@Override protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	private void setSummaryAll(PreferenceScreen pScreen) {
		for (int i = 0; i < pScreen.getPreferenceCount(); i++) {
			Preference pref = pScreen.getPreference(i);
			setSummaryPref(pref);
		}
	}

	public void setSummaryPref(Preference pref) {
		if (pref == null) return;

		String key = pref.getKey();
		if (key == null) key = "";

		if (pref instanceof KeyMapPreference) {
			KeyMapPreference kbPref = (KeyMapPreference) pref;
			String desc = kbPref.getDescription();
			pref.setSummary(desc);
		}
		else if (pref instanceof PreferenceCategory) {
			PreferenceCategory prefCat = (PreferenceCategory)pref;
			int count = prefCat.getPreferenceCount();
			for (int i=0; i < count; i++) {
				setSummaryPref(prefCat.getPreference(i));
			}
		}
		else if (pref instanceof ProfileListPreference) {
			ProfileListPreference plPref = (ProfileListPreference) pref;
			String desc = plPref.getDescription();
			pref.setSummary(desc);
		}
		else if (pref instanceof ProfileCheckBoxPreference) {
			ProfileCheckBoxPreference pcPref = (ProfileCheckBoxPreference) pref;
			if (key.compareTo(Preferences.KEY_SKIP_WELCOME) == 0) {
				pcPref.setChecked(Preferences.getActiveProfile().getSkipWelcome());
			}
		}
		else if (pref instanceof PreferenceScreen) {
			setSummaryAll((PreferenceScreen) pref);
		}
		else if (key.compareTo(Preferences.KEY_GAME_PROFILE) == 0) {
			pref.setSummary(Preferences.getActiveProfile().getName());
		}
		else if (key.compareTo(Preferences.KEY_CENTERTAP) == 0) {
			ListPreference p = (ListPreference) pref;
			pref.setSummary(p.getEntry());
		}
	}

	public void	onSharedPreferenceChanged(SharedPreferences
										  sharedPreferences, String key) {
		if (key.compareTo(Preferences.KEY_ACTIVE_PROFILE)==0
			|| key.compareTo(Preferences.KEY_PROFILES)==0) {
			setSummaryAll(getPreferenceScreen());
		}
		else {
			Preference pref = findPreference(key);
			setSummaryPref(pref);

			Preferences.addChanged(key);
		}
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}
}
