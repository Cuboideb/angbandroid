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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.LinkedHashMap;

public class GameActivity extends Activity {

	public static Typeface monoFont = null;
	public static Typeface monoBoldFont = null;
	public static Typeface iconFont = null;

	public static StateManager state = null;
	private AngbandDialog dialog = null;

	private RelativeLayout screenLayout = null;
	public TermView term = null;

	public AdvKeyboard advKeyboard = null;
	public MiniKbd miniKeyboard = null;

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
	protected final int CONTEXTMENU_OPACITY = 9;
	protected final int CONTEXTMENU_KEYMAPS = 11;
	protected final int CONTEXTMENU_TOGGLE_SUBW = 12;
	protected final int CONTEXTMENU_RUNNING = 13;
	protected final int CONTEXTMENU_RESET_DPAD = 14;
	protected final int CONTEXTMENU_RESET_LAYOUT = 15;
	protected final int CONTEXTMENU_ADD_FAB = 16;
	protected final int CONTEXTMENU_FIX_FAB = 17;
	protected final int CONTEXTMENU_IMPORT_KEYS = 18;

	public static final int TERM_CONTROL_LIST_KEYS = 1;
	public static final int TERM_CONTROL_CONTEXT = 2;
	public static final int TERM_CONTROL_VISUAL_STATE = 4;
	public static final int TERM_CONTROL_SHOW_CURSOR = 5;
	public static final int TERM_CONTROL_DEBUG = 6;
	public static final int TERM_CONTROL_QUANTITY = 7;

	protected Handler handler = null;

	protected Handler keyHandler = null;
	protected Runnable keyRunnable = null;
	protected String lastKeys = "";
	protected boolean keyHandlerIsRunning = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String version = "unknown";
		long versionCode = 0;

		try {
			ComponentName comp = new ComponentName(this, AngbandActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			version = pinfo.versionName;
			versionCode = pinfo.versionCode;
		} catch (Exception e) {}

		Preferences.init (
			this,
			getFilesDir(),
			getResources(),
			getSharedPreferences(Preferences.NAME, MODE_PRIVATE),
			version,
			versionCode
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

		if (iconFont == null) {
			iconFont = Typeface.createFromAsset(getAssets(),"ui-cmd.ttf");
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

		boolean resetDim = false;

		for (String key: Preferences.changed) {
			Log.d("Angband", "Changed: " + key);

			if (key.equals(Preferences.KEY_NUM_SUBW) ||
				key.equals(Preferences.KEY_FONT_SUBW) ||
				key.equals(Preferences.KEY_COLS_SUBW) ||
				key.equals(Preferences.KEY_ROWS_SUBW) ||
				key.equals(Preferences.KEY_HORIZ_SUBW) ||
				key.equals(Preferences.KEY_TOP_BAR) ||
				key.equals(Preferences.KEY_SIL_GX) ||
				key.equals(Preferences.KEY_MULT_TOP_BAR)) {
				adjust = true;
			}

			if (key.equals(Preferences.KEY_GAMEPLUGIN)) {
				resetDim = true;
				state.nativew.disableGraphics();
			}
		}

		if (adjust && state.characterPlaying()) {
			adjustSize(shouldAdjustByWidth());
		}

		// Reset font size
		/*
		if (resetDim) {
			if (landscapeNow())
				Preferences.setPortraitFontSize(0);
			else
				Preferences.setLandscapeFontSize(0);
		}
		*/
	}

	public void runOpacityPopup()
	{
		if (term != null) {
			OpacityPopup win = new OpacityPopup(this);
			int gravity = Gravity.TOP | Gravity.RIGHT;
			win.showAtLocation(term, gravity, 0, 0);
		}
	}

	public void runQuantityPopup(String message, int maxValue, int initialValue)
	{
		if (term != null) {
			QuantityPopup win = new QuantityPopup(this, message, maxValue, initialValue);
			int gravity = Gravity.CENTER;
			win.showAtLocation(term, gravity, 0, 0);
		}
	}

	@Override
	public void onStart() {
		Log.d("Angband", "Activity START");

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
		Log.d("Angband", "Activity DESTROY");

		super.onDestroy();
	}

	@Override
	public void onStop() {
		Log.d("Angband", "Activity STOP");

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
		Log.d("Angband","finish");
		state.gameThread.send(GameThread.Request.StopGame);
		super.finish();
	}

	public void controlMsg(int what, String msg)
	{
		if (what == TERM_CONTROL_LIST_KEYS) {
			setFastKeys(msg);
		}
		if (what == TERM_CONTROL_CONTEXT) {
			// Nothing
		}
		if (what == TERM_CONTROL_VISUAL_STATE) {
			// Nothing
		}
		if (what == TERM_CONTROL_DEBUG) {
			Log.d("Angband", "Native: " + msg);
		}
		if (what == TERM_CONTROL_SHOW_CURSOR) {
			// Nothing
		}
		if (what == TERM_CONTROL_QUANTITY && term != null) {
			Pattern pattern = Pattern.compile("(\\d+):(\\d+):(.+)");
			Matcher matcher = pattern.matcher(msg);
			if (matcher.matches()) {
				int maxValue = Integer.parseInt(matcher.group(1));
				int initialValue = Integer.parseInt(matcher.group(2));
				String msg2 = matcher.group(3);
				runQuantityPopup(msg2, maxValue, initialValue);
			}
		}
	}

	public void setFastKeysAux(String keys)
	{
		if (ribbonZone != null) {
			topRibbon.setFastKeys(keys);
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

			/*
			for (int i = 0; i < Preferences.getExtraRibbonRows(); i++) {
				ButtonRibbon another = new ButtonRibbon(this, state,
					false, true);
				ribbonZone.addView(another.rootView);
				bottomRibbon.addSibling(another);
				bottomRibbon.addClone(another);
			}

			bottomRibbon.notifyClones();
			*/
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
			String oldPlugin = "";

			if (term != null) {

				term.unloadTiles();

				oldPlugin = term.pluginName;

				oldVisuals = term.serializeVisualState();
			}

			clearKeyTimer();

			setFastKeysAux("");

			if (screenLayout != null) screenLayout.removeAllViews();
			screenLayout = null;

			boolean makeMiniKbd = false;
			boolean makeAdvKeyboard = false;
			boolean makeRibbon = false;

			if (Preferences.getEnableSoftInput()) {
				if (Preferences.isKeyboardVisible()) {

					makeAdvKeyboard = Preferences.getUseAdvKeyboard();
					makeMiniKbd = !makeAdvKeyboard;
				}
				else {
					makeRibbon = true;
				}
			}

			ribbonZone = null;
			bottomRibbon = null;
			topRibbon = null;

			advKeyboard = null;
			miniKeyboard = null;

			boolean vertical = false;

			if (Preferences.getKeyboardOverlap()) {
				if (makeAdvKeyboard || makeMiniKbd) {
					vertical = true;

					screenLayout = (RelativeLayout)getLayoutInflater()
						.inflate(R.layout.term_horiz_overlap, null);
				}
				else {
					screenLayout = (RelativeLayout)getLayoutInflater()
						.inflate(R.layout.term_vert_overlap, null);
				}
			}
			else {
				if ((makeAdvKeyboard && Preferences.getVerticalKeyboard()) || makeMiniKbd) {
					vertical = true;

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

			FrameLayout.LayoutParams lparams = null;
			if (vertical) {
				lparams =
					new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT,
						FrameLayout.LayoutParams.MATCH_PARENT);
			}
			else {
				lparams =
					new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.WRAP_CONTENT);
			}

			if (makeAdvKeyboard) {
				if (Preferences.getKeyboardHeight() == 0) {
					resetAdvKeyboardHeight();
				}

				advKeyboard = new AdvKeyboard(this);
				advKeyboard.mainView.setLayoutParams(lparams);
				frameInput.addView(advKeyboard.mainView);
			}
			else if (makeMiniKbd) {
				miniKeyboard = new MiniKbd(this);
				miniKeyboard.setLayoutParams(lparams);
				frameInput.addView(miniKeyboard);
			}
			else if (makeRibbon) {
				ribbonZone = new LinearLayout(this);
				ribbonZone.setOrientation(LinearLayout.VERTICAL);
				ribbonZone.setLayoutParams(lparams);
				frameInput.addView(ribbonZone);
				rebuildButtonRibbon();
			}

			setContentView(screenLayout);
			dialog.restoreDialog();

			// Be careful with plugin changes
			if (term.pluginName.equals(oldPlugin)) {
				term.sendVisuals(oldVisuals);
			}
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
		pct = Math.min(100f, pct);
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

		Preferences.setEnableSoftInput(true);

		Preferences.setUseAdvKeyboard(true);
		//Preferences.setVerticalKeyboard(false);
		Preferences.setKeyboardWidth(100);
		Preferences.setKeyboardHeight(100);
		//Preferences.setMiddleOpacity(100);

		Preferences.setEnableTouch(true);
		Preferences.setTouchRight(true);

		//Preferences.setCommandMode(false);
		state.opaqueWidgets = false;

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
		Preferences.setTopBar(true);

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

		menu.add(0, CONTEXTMENU_ADD_FAB, 0, "Add Floating Button");

		if (topRibbon != null) {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Full Keyboard");
			menu.add(0, CONTEXTMENU_RIBBON_STYLE, 0, "Change Ribbon Style");
			menu.add(0, CONTEXTMENU_KEYMAPS, 0, "Manage Keymaps");
		}
		else if (Preferences.getEnableSoftInput()) {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Show Button Ribbon");
		}
		else {
			menu.add(0, CONTEXTMENU_VKEY_ITEM, 0, "Enable Software Input");
		}

		if (state != null) {
			menu.add(0, CONTEXTMENU_RUNNING, 0, "Toggle Running " +
				(state.getRunningMode() ? "OFF": "ON"));
		}

		menu.add(0, CONTEXTMENU_OPACITY, 0, "Change Opacity");

		if (term != null && term.getButtons().size() > 0) {
			menu.add(0, CONTEXTMENU_FIX_FAB, 0, "Rearrange Floating Buttons");
		}

		menu.add(0, CONTEXTMENU_RESET_DPAD, 0, "Reset D-Pad Position");
		menu.add(0, CONTEXTMENU_PREFERENCES_ITEM, 0, "Preferences");
		menu.add(0, CONTEXTMENU_PROFILES_ITEM, 0, "Profiles");

		if (Preferences.getProfiles().size() > 1) {
			menu.add(0, CONTEXTMENU_IMPORT_KEYS, 0, "Copy Keymaps and Floating Buttons");
		}

		menu.add(0, CONTEXTMENU_HELP_ITEM, 0, "Help");
		menu.add(0, CONTEXTMENU_QUIT_ITEM, 0, "Quit");
	}

	public void showKeymapEditor()
	{
		KeymapEditor editor = new KeymapEditor(this, screenLayout);
		editor.show();
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
			showKeymapEditor();
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
		case CONTEXTMENU_ADD_FAB:
			if (term != null) term.createFloatingButton();
			return true;
		case CONTEXTMENU_FIX_FAB:
			if (term != null) term.rearrangeFloatingButtons();
			return true;
		case CONTEXTMENU_IMPORT_KEYS:
			importKeys();
			return true;
		case CONTEXTMENU_OPACITY:
			runOpacityPopup();
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

	public void importKeysAux(String nameSource, String nameTarget)
	{
		Profile source = Preferences.getProfiles().findByName(nameSource, -1);

		Profile target = Preferences.getActiveProfile();

		target.importFromProfile(source);

		rebuildViews();
	}

	public void importKeys()
	{
		ArrayList<String> arr = new ArrayList<>();

		final Profile current = Preferences.getActiveProfile();

		if (current == null) return;

		for (Profile prof: Preferences.getProfiles()) {
			if (prof.getName().equals(current.getName())) continue;
			arr.add(prof.getName());
		}

		if (arr.size() == 0) return;

		final String[] list = new String[arr.size()];

		arr.toArray(list);

		listAlert("Select the source Profile", list,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					importKeysAux(list[which], current.getName());
				}
			}
		);
	}

	public void listAlert(String title, String[] list,
		DialogInterface.OnClickListener okHandler) {
		new AlertDialog.Builder(this)
				.setItems(list, okHandler)
				.setTitle(title)
				.show();
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

	public static void invalidateRecursive(ViewGroup layout) {
    	int count = layout.getChildCount();
    	View child;
    	for (int i = 0; i < count; i++) {
        	child = layout.getChildAt(i);
        	if(child instanceof ViewGroup)
            	invalidateRecursive((ViewGroup) child);
        	else
            	child.invalidate();
    	}
	}

	public void refreshInputWidgets()
	{
		if (term != null) term.invalidate();

		if (advKeyboard != null) {
			advKeyboard.setOpacityMode(state.opaqueWidgets ? 2: 0);
			invalidateRecursive(advKeyboard.mainView);
		}

		if (bottomRibbon != null) {
			setFixedRibbonOpacity(state.opaqueWidgets ? 3: 2);
			invalidateRecursive(bottomRibbon.rootView);
		}

		if (topRibbon != null) {
			invalidateRecursive(topRibbon.rootView);
		}

		if (miniKeyboard != null) {
			miniKeyboard.invalidate();
		}
	}

	public void infoAlert(String msg) {
		new AlertDialog.Builder(this)
			//.setTitle("Angband")
			.setMessage(msg)
			.setCancelable(true)
			.show();
	}

	public void setFixedRibbonOpacity(int value)
	{
		if (bottomRibbon != null) {
			if (value > 3) value = 1;
			if (value < 1) value = 3;
			bottomRibbon.alphaLevel = value;
			bottomRibbon.updateAlpha();
		}
	}

	public void toggleKeyboard() {
		// Show soft input if hidden
		if (!Preferences.getEnableSoftInput())
			Preferences.setEnableSoftInput(true);
		else if (Preferences.isScreenPortraitOrientation())
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

		//Log.d("Angband", "KeyDown: " + keyCode);
		//return true;

		if (term != null && Preferences.getDebugKeycodes()) {
			term.flashAndClear(Integer.toString(keyCode), 700);
		}

		if (!state.onKeyDown(keyCode,event)) {
			return super.onKeyDown(keyCode,event);
		}
		else {
			return true;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		//Log.d("Angband", "KeyUp: " + keyCode);
		//return true;

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

		if (miniKeyboard != null) {
			w += miniKeyboard.getWidth();
		}

		return w;
	}

	public int getKeyboardHeightAbsolute() {
		int h = 0;

		if (advKeyboard != null) {
			h += advKeyboard.mainView.getHeight();
		}

		if (ribbonZone != null) {
			h += ribbonZone.getHeight();
		}

		if (miniKeyboard != null) {
			h += miniKeyboard.getHeight();
		}

		return h;
	}

	public int getKeyboardWidthAbsolute() {
		int w = 0;

		if (advKeyboard != null) {
			w += advKeyboard.mainView.getWidth();
		}

		if (ribbonZone != null) {
			w += ribbonZone.getWidth();
		}

		if (miniKeyboard != null) {
			w += miniKeyboard.getWidth();
		}

		return w;
	}
}