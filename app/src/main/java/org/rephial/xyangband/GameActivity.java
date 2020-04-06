/*
 * File: GameActivity.java
 * Purpose: Generic ui functions in Android application
 *
 * Copyright (c) 2010 David Barr, Sergey Belinsky
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class GameActivity extends Activity {

	public static StateManager state = null;
	private AngbandDialog dialog = null;

	private RelativeLayout screenLayout = null;
	private TermView term = null;
	AngbandKeyboard virtualKeyboard = null;
	AngbandKeyboardView virtualKeyboardView = null;

    private LinearLayout ribbonZone = null;
    private ButtonRibbon bottomRibbon = null;
	private ButtonRibbon topRibbon = null;

	public String coreKeymaps = "";

	protected final int CONTEXTMENU_FITWIDTH_ITEM = 0;
	protected final int CONTEXTMENU_FITHEIGHT_ITEM = 1;
	protected final int CONTEXTMENU_VKEY_ITEM = 2;
	protected final int CONTEXTMENU_PREFERENCES_ITEM = 3;
	protected final int CONTEXTMENU_PROFILES_ITEM = 4;
	protected final int CONTEXTMENU_HELP_ITEM = 5;
	protected final int CONTEXTMENU_QUIT_ITEM = 6;
	protected final int CONTEXTMENU_TOGGLE_TOUCHDIR = 7;
	protected final int CONTEXTMENU_RIBBON_STYLE = 8;
	protected final int CONTEXTMENU_RAISE_RIBBON = 9;
	protected final int CONTEXTMENU_LOWER_RIBBON = 10;

	public static final int TERM_CONTROL_LIST_KEYS = 1;
	public static final int TERM_CONTROL_NOT_PLAYING = 2;
	public static final int TERM_CONTROL_PLAYING_NOW = 3;

	protected Handler handler = null;

	protected Handler keyHandler = null;
	protected Runnable keyRunnable = null;
	protected String lastKeys = "";
	protected boolean keyHandlerIsRunning = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String version = "unknown";
		try {
			ComponentName comp = new ComponentName(this, AngbandActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			version = pinfo.versionName;
		} catch (Exception e) {}

	    Preferences.init (
	    	this,
			getFilesDir(),
			getResources(),
			getSharedPreferences(Preferences.NAME, MODE_PRIVATE),
			version
		);

		//Log.d("Angband", "onCreate");

		if (state == null) {
			state = new StateManager(this);
		}

		if (dialog == null) {
			dialog = new AngbandDialog(this,state);
		}

		if (keyHandler == null) {
			keyHandler = new Handler();
		}

		if (keyRunnable == null) {
			keyRunnable = new Runnable() {

				@Override
				public void run() {
					Log.d("Angband", "Clear fast keys!");
					clearKeyTimer();
					setFastKeysAux("");
				}
			};
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		final AngbandDialog ad = dialog;
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				ad.HandleMessage(msg);
			}
		};

		StatPublisher.start(this);

		rebuildViews();
	}

	@Override
	public void onStop() {
		super.onStop();

		StatPublisher.stop(this);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(getApplication());
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getNumericShortcut()) {
		case '1':
			intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			break;
		case '2':
			intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			break;
		case '3':
			dialog.ShowScoreEntry();
			break;
		case '4':
			dialog.ShowScoreLeaderboards();
			break;
		case '5':
			finish();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public Point getMySize()
	{
		WindowManager wm2 = this.getWindowManager();
		Display display = wm2.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size;
	}

	@Override
	public void finish() {
		//Log.d("Angband","finish");
		state.gameThread.send(GameThread.Request.StopGame);
		super.finish();
	}

	public void controlMsg(int what, String msg)
	{
		if (what == TERM_CONTROL_LIST_KEYS) {
			setFastKeys(msg);
		}
		if (what == TERM_CONTROL_NOT_PLAYING &&
			bottomRibbon != null) {
			bottomRibbon.setCommandMode(false);
		}
		if (what == TERM_CONTROL_PLAYING_NOW) {

			//Log.d("Angband", "Refresh: " + msg);

			// Save keymaps for later
			if (msg.startsWith("keymaps:")) {
				coreKeymaps = msg.substring("keymaps:".length());
				Preferences.setCoreKeymaps(coreKeymaps);
			}

			rebuildViews();
		}
	}

	public void setFastKeysAux(String keys)
	{
		if (ribbonZone != null) {
			topRibbon.setKeys(keys, ButtonRibbon.CmdLocation.Dynamic);
			bottomRibbon.setShift(false);
		}
	}

	public void clearKeyTimer()
	{
		if (keyHandler != null && keyRunnable != null) {
			keyHandler.removeCallbacks(keyRunnable);
			keyHandlerIsRunning = false;
		}
	}

	public void setFastKeys(String keys)
	{
		if (keys.equals("[[:clear:]]")) {
			keys = "";
		}

		// If we are already deleting the fast keys, do nothing
		if (keys.length() == 0 && keyHandlerIsRunning) {
			return;
		}

		clearKeyTimer();

		if (ribbonZone != null && !keys.equals(lastKeys)) {
			// Delay the deletion of the fast keys
			if (keys.length() == 0) {
				keyHandler.postDelayed(keyRunnable, 200);
				keyHandlerIsRunning = true;
			}
			else {
				setFastKeysAux(keys);
			}
		}
		lastKeys = keys;
	}

	public Point getWinSize()
    {
        Point aux = new Point();
        WindowManager wm2 = this.getWindowManager();
        wm2.getDefaultDisplay().getSize(aux);
        return aux;
    }

    public void toggleDrawRight()
	{
		term.toggleDrawRight();
	}

	protected void rebuildViews() {
		synchronized (state.progress_lock) {
			//Log.d("Angband","rebuildViews");
			//dialog.dismissProgress();

			int orient = Preferences.getOrientation();
			switch (orient) {
			case 0: // sensor
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				break;
			case 1: // portrait
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			case 2: // landscape
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			}

			if (screenLayout != null) screenLayout.removeAllViews();
			screenLayout = new RelativeLayout(this);

			Boolean kb = Preferences.isKeyboardVisible();

			virtualKeyboard = null;
            virtualKeyboardView = null;

			ribbonZone = null;
			bottomRibbon = null;
			topRibbon = null;

            if (kb) {
                virtualKeyboard = new AngbandKeyboard(this);
                virtualKeyboardView = virtualKeyboard.virtualKeyboardView;
                virtualKeyboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }

            term = new TermView(this);
            term.setFocusable(false);
            registerForContextMenu(term);
            state.link(term, handler);

			term.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			screenLayout.addView(term);

			RelativeLayout.LayoutParams rLParams =
					new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
			rLParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);

			if (kb) {
				screenLayout.addView(virtualKeyboardView, rLParams);
			}
			else {
				ribbonZone = new LinearLayout(this);
				ribbonZone.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT));
				ribbonZone.setOrientation(LinearLayout.VERTICAL);

				topRibbon = new ButtonRibbon(this, state, true);
				ribbonZone.addView(topRibbon.rootView);

				bottomRibbon = new ButtonRibbon(this, state, false);
				ribbonZone.addView(bottomRibbon.rootView);
				bottomRibbon.addSibling(topRibbon);

				screenLayout.addView(ribbonZone, rLParams);
			}

			setContentView(screenLayout);
			dialog.restoreDialog();

			term.invalidate();
		}
	}

	public void openContextMenu() {
		super.openContextMenu(term);
	}

	public boolean landscapeNow()
	{
		return getResources().getConfiguration().orientation
				== Configuration.ORIENTATION_LANDSCAPE;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Quick Settings");
		menu.add(0, CONTEXTMENU_FITWIDTH_ITEM, 0, "Fit Width");
		menu.add(0, CONTEXTMENU_FITHEIGHT_ITEM, 0, "Fit Height");
		if (Preferences.getEnableTouch() && Preferences.getTouchRight()) {
			menu.add(0, CONTEXTMENU_TOGGLE_TOUCHDIR, 0, "Swap Touch Side");
		}
		if (topRibbon != null) {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Full Keyboard");
			menu.add(0, CONTEXTMENU_RIBBON_STYLE, 0, "Change Ribbon Style");
			menu.add(0, CONTEXTMENU_LOWER_RIBBON, 0, "Lower Ribbon Opacity");
			menu.add(0, CONTEXTMENU_RAISE_RIBBON, 0, "Raise Ribbon Opacity");
		}
		else {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Button Ribbon");
		}
		menu.add(0, CONTEXTMENU_PREFERENCES_ITEM, 0, "Preferences");
		menu.add(0, CONTEXTMENU_PROFILES_ITEM, 0, "Profiles");
		menu.add(0, CONTEXTMENU_HELP_ITEM, 0, "Help");
		menu.add(0, CONTEXTMENU_QUIT_ITEM, 0, "Quit");
	}

	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		Intent intent;
		switch (aItem.getItemId()) {
		case CONTEXTMENU_FITWIDTH_ITEM:
			term.autoSizeFontByWidth(0, 0);
			term.adjustTermSize(0,0);
			state.nativew.resize();
			return true;
		case CONTEXTMENU_FITHEIGHT_ITEM:
			term.autoSizeFontByHeight(0, 0);
			term.adjustTermSize(0,0);
			state.nativew.resize();
			return true;
		case CONTEXTMENU_VKEY_ITEM:
			toggleKeyboard();
			return true;
		case CONTEXTMENU_TOGGLE_TOUCHDIR:
			toggleDrawRight();
			return true;
		case CONTEXTMENU_RIBBON_STYLE:
			if (bottomRibbon != null) bottomRibbon.toggleCommandMode();
			return true;
		case CONTEXTMENU_LOWER_RIBBON:
			if (bottomRibbon != null) bottomRibbon.changeOpacity(-1);
			return true;
		case CONTEXTMENU_RAISE_RIBBON:
			if (bottomRibbon != null) bottomRibbon.changeOpacity(+1);
			return true;
		case CONTEXTMENU_PREFERENCES_ITEM:
			intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			return true;
		case CONTEXTMENU_PROFILES_ITEM:
			intent = new Intent(this, ProfilesActivity.class);
			startActivity(intent);
			return true;
		case CONTEXTMENU_HELP_ITEM:
			intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		case CONTEXTMENU_QUIT_ITEM:
			finish();
			return true;
		}
		return false;
	}

	public void questionAlert(String msg,
							  DialogInterface.OnClickListener okHandler) {
		new AlertDialog.Builder(this)
				//.setTitle("Angband")
				.setMessage(msg)
				.setCancelable(true)
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton("Yes", okHandler)
				.show();
	}

	public void infoAlert(String msg) {
		new AlertDialog.Builder(this)
				//.setTitle("Angband")
				.setMessage(msg)
				.setCancelable(true)
				.show();
	}

	public void toggleKeyboard() {
		if(Preferences.isScreenPortraitOrientation())
			Preferences.setPortraitKeyboard(!Preferences.getPortraitKeyboard());
		else
			Preferences.setLandscapeKeyboard(!Preferences.getLandscapeKeyboard());
		rebuildViews();
	}

	@Override
	protected void onResume() {
		//Log.d("Angband", "onResume");
		super.onResume();

		setScreen();

		setFastKeysAux("");

		term.onResume();
	}

	@Override
	protected void onPause() {
		//Log.d("Angband", "onPause");
		super.onPause();

		clearKeyTimer();

		term.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!state.onKeyDown(keyCode,event)) {
			return super.onKeyDown(keyCode,event);
		}
		else {
			return true;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!state.onKeyUp(keyCode,event)) {
			return super.onKeyUp(keyCode,event);
		}
		else {
			return true;
		}
	}

	public void setScreen() {
		if (Preferences.getFullScreen()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	public Handler getHandler() {
		return handler;
	}

	public StateManager getStateManager() {
		return state;
	}

    public int getKeyboardHeight() {
		int h = 0;

        if (Preferences.isKeyboardVisible()) {
			h += virtualKeyboardView.getHeight();
		}

        if (ribbonZone != null) {
        	h += ribbonZone.getHeight();
		}

        return h;
    }
}