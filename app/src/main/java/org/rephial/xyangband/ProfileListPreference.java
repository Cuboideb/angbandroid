package org.rephial.xyangband;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ProfileListPreference extends ListPreference 
{
	public ProfileListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfileListPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
		if (value != null) {
			Preferences.getActiveProfile().setPlugin(Integer.parseInt(value));
			Preferences.saveProfiles();
			return true;
		}
		else
			return false;
	}

	/* doesn't seem to help initial display problem
	@Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(getPersistedString("0"));
    }
	*/

	public String getDescription() {
		int pl = Preferences.getActiveProfile().getPlugin();
		return Preferences.getPluginDescription(pl);
	}

    @Override
    protected String getPersistedString(String defaultReturnValue) {
		int pl = Preferences.getActiveProfile().getPlugin();
		return String.valueOf(pl);
    }
}