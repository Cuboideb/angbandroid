package org.rephial.angband;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.preference.DialogPreference;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.KeyEvent;

public class KeyMapResetPreference
	extends DialogPreference implements DialogInterface.OnClickListener {

	Context context;

	public KeyMapResetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		setDialogTitle("Really reset all keys?");
		setPositiveButtonText("OK");
		setNegativeButtonText("Cancel");
	}

	public void onClick(DialogInterface dialog, int which) {
		if (which == -1) { //OK
			Preferences.getKeyMapper().init(true);
		}
		else {
		}
	} 
}
