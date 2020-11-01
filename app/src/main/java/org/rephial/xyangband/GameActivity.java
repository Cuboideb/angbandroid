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
import android.graphics.Typeface;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.LinkedHashMap;

public class GameActivity extends Activity {

    public static Typeface monoFont = null;
	public static Typeface monoBoldFont = null;

	public static StateManager state = null;
	private AngbandDialog dialog = null;

	private RelativeLayout screenLayout = null;
	private TermView term = null;
	AngbandKeyboard virtualKeyboard = null;
	AngbandKeyboardView virtualKeyboardView = null;

	public AdvKeyboard advKeyboard = null;

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
	protected final int CONTEXTMENU_RIBBON_STYLE = 8;
	protected final int CONTEXTMENU_RAISE_RIBBON = 9;
	protected final int CONTEXTMENU_LOWER_RIBBON = 10;
	protected final int CONTEXTMENU_KEYMAPS = 11;
	protected final int CONTEXTMENU_TOGGLE_SUBW = 12;
	protected final int CONTEXTMENU_RUNNING = 13;
	protected final int CONTEXTMENU_RESET_DPAD = 14;
	protected final int CONTEXTMENU_RESET_LAYOUT = 15;

	public static final int TERM_CONTROL_LIST_KEYS = 1;
	public static final int TERM_CONTROL_CONTEXT = 2;
	public static final int TERM_CONTROL_VISUAL_STATE = 4;
	public static final int TERM_CONTROL_SHOW_CURSOR = 5;

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

	   	// Testing
		//Preferences.setSize(80,24);

		if (state == null) {
			state = new StateManager(this);
		}

		if (dialog == null) {
			dialog = new AngbandDialog(this,state);
		}

		if (keyHandler == null) {
			keyHandler = new Handler();
		}

		if (monoFont == null) {
           	monoFont = Typeface.createFromAsset(getAssets(),"VeraMono.ttf");
        }

		if (monoBoldFont == null) {
			monoBoldFont = Typeface.createFromAsset(getAssets(),"VeraMono-Bold.ttf");
		}

		if (keyRunnable == null) {
			keyRunnable = new Runnable() {

				@Override
				public void run() {
					//Log.d("Angband", "Clear fast keys!");
					clearKeyTimer();
					setFastKeysAux("");
				}
			};
		}
	}

	public boolean shouldAdjustByWidth()
	{
		boolean byWidth = true;

		if (!Preferences.getActivePlugin().enableSubWindows() ||
			Preferences.getNumberSubWindows() == 0 ||
			Preferences.getHorizontalSubWindows()) {
			byWidth = !landscapeNow();
		}

		return byWidth;
	}

	public void processPrefChanges()
	{
		boolean adjust = false;

		for (String key: Preferences.changed) {
			Log.d("Angband", "Changed: " + key);

			if (key.equals(Preferences.KEY_NUM_SUBW) ||
				key.equals(Preferences.KEY_FONT_SUBW) ||
				key.equals(Preferences.KEY_COLS_SUBW) ||
				key.equals(Preferences.KEY_ROWS_SUBW) ||
				key.equals(Preferences.KEY_HORIZ_SUBW) ||
				key.equals(Preferences.KEY_TOP_BAR) ||
				key.equals(Preferences.KEY_MULT_TOP_BAR)) {
				adjust = true;
			}
		}

		if (adjust && state.characterPlaying()) {
			adjustSize(shouldAdjustByWidth());
		}
	}

	@Override
	public void onStart() {
		Log.d("Angband", "START");

		super.onStart();

		final AngbandDialog ad = dialog;
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				ad.HandleMessage(msg);
			}
		};

		StatPublisher.start(this);

		if (Preferences.changed != null) {

			processPrefChanges();

			Preferences.clearChanged();
		}

		rebuildViews();
	}

	@Override
	public void onDestroy()
	{
		Log.d("Angband", "DESTROY");

		super.onDestroy();
	}

	@Override
	public void onStop() {
		Log.d("Angband", "STOP");

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
		if (what == TERM_CONTROL_CONTEXT) {

			//Log.d("Angband", "Refresh: " + msg);

			// Save keymaps for later
			String pattern = "keymaps:";
			int pos = msg.indexOf(pattern);
			if (pos > -1) {
				coreKeymaps = msg.substring(pos + pattern.length());
				Preferences.setCoreKeymaps(coreKeymaps);
			}

			if (msg.indexOf("in_dungeon:1") > -1) {

				if (bottomRibbon != null) {
					bottomRibbon.restoreCommandMode();
				}

				//state.nativew.reloadGraphics();
			}
			else {
				if (bottomRibbon != null) {
					bottomRibbon.setCommandMode(false);
				}
			}
		}
		if (what == TERM_CONTROL_VISUAL_STATE && term != null) {

			Pattern pattern = Pattern.compile("visual:(\\d+):(\\d+)" +
				":(\\d+)");
			Matcher matcher = pattern.matcher(msg);
			if (matcher.matches()) {
				int rows = Integer.parseInt(matcher.group(1));
				int cols = Integer.parseInt(matcher.group(2));
				int graf = Integer.parseInt(matcher.group(3));

				term.configureVisuals(rows, cols, graf);
			}
		}
		if (what == TERM_CONTROL_SHOW_CURSOR) {
			//Log.d("Angband", "Big cursor: " + msg);
			state.setBigCursor(msg.equals("big"));
			state.setCursorVisible(true);
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

	public void rebuildButtonRibbon()
	{
		if (ribbonZone != null) {

			ribbonZone.removeAllViews();

			topRibbon = new ButtonRibbon(this, state,
				true, false);
			ribbonZone.addView(topRibbon.rootView);

			bottomRibbon = new ButtonRibbon(this, state,
				false, false);
			ribbonZone.addView(bottomRibbon.rootView);
			bottomRibbon.addSibling(topRibbon);

			for (int i = 0; i < Preferences.getExtraRibbonRows(); i++) {
				ButtonRibbon another = new ButtonRibbon(this, state,
					false, true);
				ribbonZone.addView(another.rootView);
				bottomRibbon.addSibling(another);
				bottomRibbon.addClone(another);
			}

			bottomRibbon.notifyClones();
		}
	}

	protected void rebuildViews() {

		synchronized (state.progress_lock) {
			Log.d("Angband","Rebuild Views");
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

			String oldVisuals = "";

			if (term != null) {

				term.unloadTiles();

				oldVisuals = term.serializeVisualState();
			}

			clearKeyTimer();

			setFastKeysAux("");

			if (screenLayout != null) screenLayout.removeAllViews();
			screenLayout = null;

			Boolean makeOldKbd = Preferences.isKeyboardVisible();

			virtualKeyboard = null;
            virtualKeyboardView = null;

			ribbonZone = null;
			bottomRibbon = null;
			topRibbon = null;

			advKeyboard = null;

			boolean makeAdvKeyboard = false;
			boolean vertical = false;

			if (makeOldKbd && Preferences.getUseAdvKeyboard()) {
				makeAdvKeyboard = true;
				makeOldKbd = false;
				vertical = Preferences.getVerticalKeyboard();
			}

			if (Preferences.getKeyboardOverlap()) {
				if (vertical) {
					screenLayout = (RelativeLayout)getLayoutInflater()
							.inflate(R.layout.term_horiz_overlap, null);
				}
				else {
					screenLayout = (RelativeLayout)getLayoutInflater()
							.inflate(R.layout.term_vert_overlap, null);
				}
			}
			else {
				if (vertical) {
					screenLayout = (RelativeLayout)getLayoutInflater()
							.inflate(R.layout.term_horiz_no_overlap, null);
				}
				else {
					screenLayout = (RelativeLayout)getLayoutInflater()
							.inflate(R.layout.term_vert_no_overlap, null);
				}
			}

			FrameLayout frameTerm = screenLayout.findViewById(R.id.frameTerm);
			FrameLayout frameInput = screenLayout.findViewById(R.id.frameInput);

			term = new TermView(this);
			term.setFocusable(false);
			registerForContextMenu(term);
			state.link(term, handler);

			term.setLayoutParams
					(new FrameLayout.LayoutParams(
							FrameLayout.LayoutParams.MATCH_PARENT,
							FrameLayout.LayoutParams.MATCH_PARENT));
			frameTerm.addView(term);

			if (makeAdvKeyboard) {
				advKeyboard = new AdvKeyboard(this);
				if (vertical) {
					advKeyboard.mainView.setLayoutParams
							(new FrameLayout.LayoutParams(
									FrameLayout.LayoutParams.WRAP_CONTENT,
									FrameLayout.LayoutParams.MATCH_PARENT));
				}
				else {
					advKeyboard.mainView.setLayoutParams
							(new FrameLayout.LayoutParams(
									FrameLayout.LayoutParams.MATCH_PARENT,
									FrameLayout.LayoutParams.WRAP_CONTENT));
				}
				frameInput.addView(advKeyboard.mainView);
			}
			else if (makeOldKbd) {
				virtualKeyboard = new AngbandKeyboard(this);
				virtualKeyboardView = virtualKeyboard.virtualKeyboardView;
				virtualKeyboardView.setLayoutParams
						(new FrameLayout.LayoutParams(
								FrameLayout.LayoutParams.MATCH_PARENT,
								FrameLayout.LayoutParams.WRAP_CONTENT));
				frameInput.addView(virtualKeyboardView);
			}
			else {
				ribbonZone = new LinearLayout(this);
				ribbonZone.setOrientation(LinearLayout.VERTICAL);
				ribbonZone.setLayoutParams
						(new FrameLayout.LayoutParams(
								FrameLayout.LayoutParams.MATCH_PARENT,
								FrameLayout.LayoutParams.WRAP_CONTENT));
				frameInput.addView(ribbonZone);
				rebuildButtonRibbon();
			}

			setContentView(screenLayout);
			dialog.restoreDialog();

			term.sendVisuals(oldVisuals);
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

	public boolean adjustSize(boolean byWidth)
	{
		if (term == null) return false;

		if (byWidth) {
			term.autoSizeFontByWidth(0, 0);
		}
		else {
			term.autoSizeFontByHeight(0, 0);
		}

		term.adjustTermSize(0, 0);

		state.nativew.resize();

		return true;
	}

	public void resetAdvKeyboardHeight()
	{
		// Estimate keyboard height
		Point size = getMySize();
		float pct = 0f;
		if (size.x > 0 && size.y > 0) {
			pct = (size.x / 10.0f) * (5.0f / size.y) * 100.0f;
		}
		pct = Math.max(20f, pct);
		Preferences.setKeyboardHeight((int)pct);
	}

	public void resetGameLayout()
	{
		int n;

		if (Preferences.getActivePlugin().enableSubWindows()) {
			n = Integer.valueOf(getString(R.string.def_number_subwindows));
			Preferences.setNumberSubWindows(n);
		}
		else {
			Preferences.setNumberSubWindows(0);
		}
		Preferences.setKeyboardOverlap(true);

		n = Integer.valueOf(getString(R.string.def_rows_subwindows));
		Preferences.setRowsSubWindows(n);

		Preferences.setUseAdvKeyboard(true);
		Preferences.setVerticalKeyboard(false);
		Preferences.setKeyboardWidth(100);
		Preferences.setKeyboardHeight(40);
		//Preferences.setMiddleOpacity(100);

		Preferences.setEnableTouch(true);
		Preferences.setTouchRight(true);

		Preferences.setCommandMode(false);
		Preferences.setRibbonAlpha(2);

		term.resetDragOffset();

		boolean horizontal = landscapeNow();
		boolean toggle = false;

		if (horizontal) {
			n = Integer.valueOf(getString(R.string.def_font_subwindows));
			Preferences.setFontSizeSubWindows(n);

			Preferences.setColumnsSubWindows(25);
			toggle = (ribbonZone == null);
		}
		else {
			n = Integer.valueOf(getString(R.string.def_font_subwindows));
			Preferences.setFontSizeSubWindows(n);

			n = Integer.valueOf(getString(R.string.def_cols_subwindows));
			Preferences.setColumnsSubWindows(n);

			toggle = (ribbonZone != null);

			resetAdvKeyboardHeight();
		}

		Preferences.setHorizontalSubWindows(!horizontal);
		Preferences.setTopBar(!horizontal);

		if (term != null) adjustSize(shouldAdjustByWidth());

		if (toggle) toggleKeyboard();
		else rebuildViews();

		//state.addKey(' ');
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {

		menu.setHeaderTitle("Quick Settings");
		menu.add(0, CONTEXTMENU_FITWIDTH_ITEM, 0, "Fit Width");
		menu.add(0, CONTEXTMENU_FITHEIGHT_ITEM, 0, "Fit Height");

		if (state.characterPlaying()) {
			menu.add(0, CONTEXTMENU_RESET_LAYOUT, 0, "Reset Layout " +
				(landscapeNow() ? "(Landscape)": "(Portrait)"));
		}

		if (topRibbon != null) {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Full Keyboard");
			//menu.add(0, CONTEXTMENU_TOGGLE_SUBW, 0, "Toggle Sub-Windows");
			menu.add(0, CONTEXTMENU_RIBBON_STYLE, 0, "Change Ribbon Style");
			menu.add(0, CONTEXTMENU_KEYMAPS, 0, "Manage Keymaps");
			menu.add(0, CONTEXTMENU_LOWER_RIBBON, 0, "Lower Ribbon Opacity");
			menu.add(0, CONTEXTMENU_RAISE_RIBBON, 0, "Raise Ribbon Opacity");
		}
		else {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Button Ribbon");
			//menu.add(0, CONTEXTMENU_TOGGLE_SUBW, 0, "Toggle Sub-Windows");
			//menu.add(0, CONTEXTMENU_KEYMAPS, 0, "Manage Keymaps");
		}
		if (state != null) {
			menu.add(0, CONTEXTMENU_RUNNING, 0, "Toggle Running " +
				(state.getRunningMode() ? "OFF": "ON"));
		}
		menu.add(0, CONTEXTMENU_RESET_DPAD, 0, "Reset D-Pad Position");
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
            if (term != null && state.nativew.lockWithTimer.reserveLock()) {
			    adjustSize(true);
                state.nativew.lockWithTimer.waitAndRelease();
			}
			return true;
		case CONTEXTMENU_FITHEIGHT_ITEM:
            if (term != null && state.nativew.lockWithTimer.reserveLock()) {
				adjustSize(false);
                state.nativew.lockWithTimer.waitAndRelease();
			}
			return true;
		case CONTEXTMENU_RUNNING:
			state.setRunningMode(!state.getRunningMode());
			return true;
	    case CONTEXTMENU_KEYMAPS:
	    	KeymapEditor editor = new KeymapEditor(this, screenLayout);
	    	editor.show();
	    	return true;
	    case CONTEXTMENU_RESET_DPAD:
	    	if (term != null) term.resetDragOffset();
	    	return true;
	    case CONTEXTMENU_RESET_LAYOUT:
	    	this.resetGameLayout();
	    	return true;
	    case CONTEXTMENU_TOGGLE_SUBW:
	    	state.showSubWindows = !state.showSubWindows;
	    	if (term != null) term.invalidate();
	    	return true;
		case CONTEXTMENU_VKEY_ITEM:
			toggleKeyboard();
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

		// Reset position of the touch directionals
		Preferences.setTouchDragOffset(0,0);

		rebuildViews();
	}

	@Override
	protected void onResume() {
		Log.d("Angband", "RESUME");

		super.onResume();

		setScreen();

		setFastKeysAux("");

		term.onResume();
	}

	@Override
	protected void onPause() {
		Log.d("Angband", "PAUSE");

		super.onPause();

		clearKeyTimer();

		setFastKeysAux("");

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

		if (advKeyboard != null && !advKeyboard.vertical) {
			h += advKeyboard.mainView.getHeight();
		}

    	if (Preferences.isKeyboardVisible() &&
			virtualKeyboardView != null) {
			h += virtualKeyboardView.getHeight();
		}

    	if (ribbonZone != null) {
    		h += ribbonZone.getHeight();
		}

    	return h;
	}

	public int getKeyboardWidth() {
		int w = 0;

		if (advKeyboard != null && advKeyboard.vertical) {
			w += advKeyboard.mainView.getWidth();
		}

    	return w;
	}
}