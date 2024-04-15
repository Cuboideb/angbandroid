package org.rephial.xyangband;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

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
		if (getKey().compareTo(Preferences.KEY_SKIP_WELCOME)==0) {
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
		if (getKey().compareTo(Preferences.KEY_SKIP_WELCOME)==0) {
			val = Preferences.getActiveProfile().getSkipWelcome();
		}
		return val;
    }
}