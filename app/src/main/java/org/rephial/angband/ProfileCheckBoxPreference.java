package org.rephial.angband;

import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class ProfileCheckBoxPreference extends CheckBoxPreference 
{
	public ProfileCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfileCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
		if (getKey().compareTo(Preferences.KEY_SKIPWELCOME)==0) {
			Preferences.getActiveProfile().setSkipWelcome(value);
			Preferences.saveProfiles();

			return true;
		}
		return false;
	}

	@Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {		
        setChecked(getPersistedBoolean(false));
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
		boolean val = defaultReturnValue;
		if (getKey().compareTo(Preferences.KEY_SKIPWELCOME)==0) {
			val = Preferences.getActiveProfile().getSkipWelcome();
		}
		return val;
    }
}