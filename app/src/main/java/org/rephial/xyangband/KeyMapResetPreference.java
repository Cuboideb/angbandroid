package org.rephial.xyangband;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

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
