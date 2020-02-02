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

package org.rephial.angband;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.content.pm.ActivityInfo;
import android.content.ComponentName;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Message;

public class GameActivity extends Activity {

	public static StateManager state = null;
	private AngbandDialog dialog = null;

	private LinearLayout screenLayout = null; 
	private TermView term = null;

	protected final int CONTEXTMENU_FITWIDTH_ITEM = 0;
	protected final int CONTEXTMENU_FITHEIGHT_ITEM = 1;
	protected final int CONTEXTMENU_VKEY_ITEM = 2;
	protected final int CONTEXTMENU_PREFERENCES_ITEM = 3;
	protected final int CONTEXTMENU_PROFILES_ITEM = 4;
	protected final int CONTEXTMENU_HELP_ITEM = 5;
	protected final int CONTEXTMENU_QUIT_ITEM = 6;

	protected Handler handler = null;

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
			getFilesDir(),
			getResources(), 
			getSharedPreferences(Preferences.NAME, MODE_PRIVATE),
			version
		);

		//Log.d("Angband", "onCreate");		

		if (state == null) {
			state = new StateManager();
		}

		if (dialog == null) dialog = new AngbandDialog(this,state);
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

	@Override
	public void finish() {
		//Log.d("Angband","finish");
		state.gameThread.send(GameThread.Request.StopGame);
		super.finish();
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
			screenLayout = new LinearLayout(this);

			term = new TermView(this);
			term.setLayoutParams(
					new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT,
						1.0f)
					);
			term.setFocusable(false);
			registerForContextMenu(term);
			state.link(term, handler);

			screenLayout.setOrientation(LinearLayout.VERTICAL);
			screenLayout.addView(term);

			Boolean kb = false;
			if(Preferences.isScreenPortraitOrientation())
				kb = Preferences.getPortraitKeyboard();
			else		
				kb = Preferences.getLandscapeKeyboard();

			if (kb) {
				AngbandKeyboard virtualKeyboard = new AngbandKeyboard(this);
				screenLayout.addView(virtualKeyboard.virtualKeyboardView);
			}

			setContentView(screenLayout);
			dialog.restoreDialog();

			term.invalidate();
		}
	}

	public void openContextMenu() {
		super.openContextMenu(term);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Quick Settings");
		menu.add(0, CONTEXTMENU_FITWIDTH_ITEM, 0, "Fit Width"); 
		menu.add(0, CONTEXTMENU_FITHEIGHT_ITEM, 0, "Fit Height"); 
		menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Toggle Keyboard"); 
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
			term.autoSizeFontByWidth(0);
			state.nativew.resize();
			return true; 
		case CONTEXTMENU_FITHEIGHT_ITEM:
			term.autoSizeFontByHeight(0);
			state.nativew.resize();
			return true; 
		case CONTEXTMENU_VKEY_ITEM:
			toggleKeyboard();
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

		term.onResume();
	}

	@Override
	protected void onPause() {
		//Log.d("Angband", "onPause");
		super.onPause();
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
}