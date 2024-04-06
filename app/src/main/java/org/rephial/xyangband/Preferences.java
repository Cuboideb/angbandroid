package org.rephial.xyangband;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.lang.reflect.Array;

final public class Preferences {

	public static final int rows = 24;
	public static final int cols = 80;

	public static final int max_rows = 255;
	public static final int max_cols = 255;

	static final String NAME = "angband";

	static final String KEY_VIBRATE = "angband.vibrate";
	static final String KEY_FULLSCREEN = "angband.fullscreen";
	static final String KEY_ORIENTATION = "angband.orientation";

	static final String KEY_KEYBOARDOVERLAP = "angband.allowKeyboardOverlap";
	static final String KEY_KEYBOARDOPACITY = "angband.keyboardOpacity";
	static final String KEY_KEYBOARDPOSITION = "angband.keyboardPosition";
	static final String KEY_MIDDLEOPACITY = "angband.middleOpacity";
	static final String KEY_ENABLETOUCH = "angband.enabletouch";

	static final String KEY_TOUCHRIGHT = "angband.touchright";
	static final String KEY_TOUCHMULTIPLIER = "angband.touchmultiplier";
	static final String KEY_TOUCHDRAG = "angband.enable_touch_drag";
	static final String KEY_TOUCH_DRAG_OFFSET = "angband.touch_drag_offset";

	static final String KEY_RIBBONBUTTONMULT = "angband.ribbonbuttonmult";
	static final String KEY_SHOWAUTOLIST = "angband.showautolist";
	static final String KEY_RIBBON_ROWS = "angband.ribbon_rows";

	static final String KEY_USERKEYMAPS = "angband.userkeymaps";

	static final String KEY_NUM_SUBW = "angband.n_subwindows";
	static final String KEY_FONT_SUBW = "angband.font_size_subwindows";
	static final String KEY_ROWS_SUBW = "angband.rows_subwindows";
	static final String KEY_COLS_SUBW = "angband.cols_subwindows";
	static final String KEY_HORIZ_SUBW = "angband.horiz_subwindows";

	static final String KEY_FAB_MULT = "angband.fab_mult";
	static final String KEY_RANGE_REDUCTION = "angband.range_reduction";

	static final String KEY_TOP_BAR = "angband.top_bar";
	static final String KEY_MULT_TOP_BAR = "angband.mult_top_bar";

	static final String KEY_CENTERTAP = "angband.centerscreentap";
	static final String KEY_PORTRAITKB = "angband.portraitkb";
	static final String KEY_LANDSCAPEKB = "angband.landscapekb";
	static final String KEY_PORTRAITFONTSIZE = "angband.portraitfontsize";
	static final String KEY_LANDSCAPEFONTSIZE = "angband.landscapefontsize";

	static final String KEY_TERMWIDTH = "angband.termwidth";
	static final String KEY_TERMHEIGHT = "angband.termheight";

	static final String KEY_SIL_GX = "angband.sil_gx";

	static final String KEY_QUANTITY_POPUP = "angband.quantity_popup";

	static final String KEY_USE_ADV_KBD = "angband.use_adv_keyboard";
	static final String KEY_SHOW_ADV_KEYMAP = "angband.show_adv_keymaps";
	static final String KEY_USE_VERT_KBD = "angband.use_vert_keyboard";
	static final String KEY_KBD_HEIGHT = "angband.keyboard_height";
	static final String KEY_KBD_WIDTH = "angband.keyboard_width";

	static final String KEY_QWERTYNUMPAD = "angband.qwertynumpad";

	static final String KEY_USEICONS = "angband.useicons";

	static final String KEY_COREKEYMAPS = "angband.corekeymaps";
	static final String KEY_COMMANDMODE = "angband.commandmode";

	static final String KEY_GAMEPLUGIN = "angband.gameplugin";
	static final String KEY_GAMEPROFILE = "angband.gameprofile";
	static final String KEY_SKIPWELCOME = "angband.skipwelcome";

	static final String KEY_GRAPHICS = "angband.graphics";

	static final String KEY_PROFILES = "angband.profiles";
	static final String KEY_ACTIVEPROFILE = "angband.activeprofile";

	static final String KEY_INSTALLEDVERSION = "angband.installedversion";

	static final int KBD_CENTER = 0;
	static final int KBD_BOTTOM_LEFT = 1;
	static final int KBD_BOTTOM_RIGHT = 2;
	static final int KBD_TOP_LEFT = 3;
	static final int KBD_TOP_RIGHT = 4;

	static final int INPUT_NONE = 0;
	static final int INPUT_ADV_KBD = 1;
	static final int INPUT_MINI_KBD = 2;
	static final int INPUT_RIBBON = 3;

	private static String activityFilesPath;

	private static SharedPreferences pref;
	private static int[] gamePlugins;
	private static String[] gamePluginNames;
	private static String[] gamePluginDescriptions;
	private static ProfileList profiles;
	private static String version;
	private static long versionCode;
	private static int fontSize = 17;
	private static Resources resources;

	public static String defNumSubWindows;
	public static String defFontSubWindows;
	public static String defColsSubWindows;
	public static String defRowsSubWindows;
	public static String dpadColor1;
	public static String dpadColor2;

	private static KeyMapper keymapper;

	public static Context context;

	public static ArrayList<String> changed = null;

	public static final int MICROCHASM_GX = 500;

	Preferences() {}

	public static void init(Context p_context, File filesDir,
			Resources res, SharedPreferences sharedPrefs,
			String pVersion, long pVersionCode) {

		context = p_context;

		activityFilesPath = filesDir.getAbsolutePath();
		pref = sharedPrefs;
		resources = res;

		String[] gamePluginsStr = resources.getStringArray(R.array.gamePlugins);
		gamePlugins = (int[])Array.newInstance(int.class, gamePluginsStr.length);
		for(int i = 0; i<gamePluginsStr.length; i++)
			gamePlugins[i] = Integer.parseInt(gamePluginsStr[i]);

		gamePluginNames = resources.getStringArray(R.array.gamePluginNames);
		gamePluginDescriptions = resources.getStringArray(R.array.gamePluginDescriptions);

		version = pVersion;
		versionCode = pVersionCode;

		keymapper = new KeyMapper(pref);

		defNumSubWindows = resources.getString(R.string.def_number_subwindows);
		defFontSubWindows = resources.getString(R.string.def_font_subwindows);
		defRowsSubWindows = resources.getString(R.string.def_rows_subwindows);
		defColsSubWindows = resources.getString(R.string.def_cols_subwindows);
		dpadColor1 = resources.getString(R.string.def_dpad_color1);
		dpadColor2 = resources.getString(R.string.def_dpad_color2);
	}

	public static HashMap<String,String> getAllPrefs()
	{
		HashMap<String,String> snap = new HashMap<>();
		Map<String,?> keys = pref.getAll();
		for(Map.Entry<String,?> entry : keys.entrySet()){
    		snap.put(entry.getKey(), entry.getValue().toString());
		}
		return snap;
	}

	public static void addChanged(String key)
	{
		if (changed == null) changed = new ArrayList<>();

		changed.add(key);
	}

	public static void clearChanged()
	{
		changed = null;
	}

	/*
	public static void saveSnapshot()
	{
		oldValues = getAllPrefs();
	}

	public HashMap<String,String> getChangesPrefs()
	{
		if (oldValues == null) return null;

		HashMap<String,String> changed = new HashMap<>();

		for (Map.Entry<String,String> entry: getAllPrefs().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!oldValues.containsKey(key)
				|| !oldValues.get(key).equals(value)) {
				changed.put(key, value);
			}
		}

		oldValues = null;

		return changed;
	}
	*/

	/*
	public static boolean getQwertyNumPad()
	{
		return pref.getBoolean(Preferences.KEY_QWERTYNUMPAD, false);
	}
	*/

	public static int getInputMode()
	{
		if (!getEnableSoftInput()) return INPUT_NONE;

		if (!isKeyboardVisible()) return INPUT_RIBBON;

		if (!getUseAdvKeyboard()) return INPUT_MINI_KBD;

		return INPUT_ADV_KBD;
	}

	public static boolean getDrawHealthBars()
	{
		return pref.getBoolean("angband.draw_health_bars", true);
	}

	public static String getVersion() {
		return version;
	}

	public static long getVersionCode() {
		return versionCode & 0x0FFFFL;
	}

	public static long getVersionCodeMajor() {
		return (versionCode >> 32) & 0x0FFFFL;
	}

	public static Resources getResources() {
		return resources;
	}

	public static String getString(String key) {
		return pref.getString(key, "");
	}

	public static String getDPadColor(int colorIdx) {
		String key = "angband.dpad_color" + colorIdx;
		String defValue = (colorIdx == 1) ? dpadColor1: dpadColor2;
		return pref.getString(key, defValue);
	}

	/* no longer needed, replaced with crc logic
	public static String getInstalledVersion() {
		return pref.getString(Preferences.KEY_INSTALLEDVERSION, "");
	}
	public static void setInstalledVersion(String value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_INSTALLEDVERSION, value);
		ed.commit();
	}
	*/

	public static boolean getFullScreen() {
		return pref.getBoolean(Preferences.KEY_FULLSCREEN, true);
	}
	public static void setFullScreen(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_FULLSCREEN, value);
		ed.commit();
	}
	public static boolean isScreenPortraitOrientation() {
		Configuration config = resources.getConfiguration();
		return (config.orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	public static int getOrientation() {
		return Integer.parseInt(pref.getString(Preferences.KEY_ORIENTATION, "0"));
	}

	public static float getReducedButtonWeight() {
		try {
			int n = pref.getInt("angband.reduce_buttons", 0);
			float w = (float)n / 100.0f;
			return w < 0.2f ? 0.0f: w;
		}
		catch (Exception ex) {
			Log.d("Angband", ex.getMessage());
		}
		return 0.5f;
	}

	public static boolean getIconsEnabled() {
		return pref.getBoolean(Preferences.KEY_USEICONS, true);
	}

	public static boolean getQuantityPopupEnabled() {
		return pref.getBoolean(Preferences.KEY_QUANTITY_POPUP, true)
				&& Preferences.getEnableSoftInput()
				&& !Preferences.isKeyboardVisible();
	}

	public static int getKeyboardPosition()
	{
		String s = pref.getString(Preferences.KEY_KEYBOARDPOSITION, ""+KBD_CENTER);
		//GameActivity.log("Position: " + s);
		return Integer.parseInt(s);
	}

	public static void setKeyboardPosition(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_KEYBOARDPOSITION, Integer.toString(value));
		ed.commit();
	}

	public static int getInputWidgetPosition()
	{
		int inputMode = getInputMode();

		if (inputMode == INPUT_RIBBON) return KBD_CENTER;

		if (inputMode == INPUT_ADV_KBD && getKeyboardWidth() >= 80) return KBD_CENTER;

		int position = getKeyboardPosition();

		// Small widgets
		if (position == KBD_CENTER) {
			if (inputMode == INPUT_MINI_KBD || getVerticalKeyboard()) {
				return KBD_BOTTOM_LEFT;
			}
		}

		return position;
	}

	public static boolean getKeyboardOverlap() {
		return pref.getBoolean(Preferences.KEY_KEYBOARDOVERLAP, true);
	}

	public static void setKeyboardOverlap(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_KEYBOARDOVERLAP, value);
		ed.commit();
	}

	public static boolean getShowAutoList() {
		return pref.getBoolean(Preferences.KEY_SHOWAUTOLIST, false);
	}

	public static int getMiddleOpacity() {
		return pref.getInt(Preferences.KEY_MIDDLEOPACITY, 100);
	}

	public static boolean getUseAdvKeyboard()
	{
		return pref.getBoolean(Preferences.KEY_USE_ADV_KBD, true);
	}

	public static boolean getShowAdvKeymaps()
	{
		return pref.getBoolean(Preferences.KEY_SHOW_ADV_KEYMAP, true);
	}

	public static String getFastKeysPopupPosition()
	{
		if (!getEnableSoftInput() || isKeyboardVisible()) {
			return "Hidden";
		}
		return pref.getString("angband.fk_popup_pos", "Hidden");
	}

	public static void setUseAdvKeyboard(boolean value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_USE_ADV_KBD, value);
		ed.commit();
	}

	public static boolean getVerticalKeyboard()
	{
		return (getInputMode() == INPUT_ADV_KBD) && (getKeyboardWidth() <= 45);
	}

	/*
	public static void setVerticalKeyboard(boolean value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_USE_VERT_KBD, value);
		ed.commit();
	}
	*/

	public static int getKeyboardHeight() {
		return pref.getInt(Preferences.KEY_KBD_HEIGHT, 0);
	}

	public static void setKeyboardHeight(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_KBD_HEIGHT, value);
		ed.commit();
	}

	public static int getKeyboardWidth() {
		return pref.getInt(Preferences.KEY_KBD_WIDTH, 100);
	}

	public static void setKeyboardWidth(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_KBD_WIDTH, value);
		ed.commit();
	}

	public static int getFabMult() {
		return pref.getInt(Preferences.KEY_FAB_MULT, 0);
	}

	public static void setFabMult(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_FAB_MULT, value);
		ed.commit();
	}

	public static int getTouchMultiplier() {
		return pref.getInt(Preferences.KEY_TOUCHMULTIPLIER, 0);
	}

	public static void setTouchMultiplier(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_TOUCHMULTIPLIER, value);
		ed.commit();
	}

	public static boolean getTouchDragEnabled() {
		return pref.getBoolean(Preferences.KEY_TOUCHDRAG, true);
	}

	public static boolean getAutoHideKeys() {
		return pref.getBoolean("angband.auto_hide_adv_kbd", false);
	}

	public static void setTouchDragOffset(int px, int py)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_TOUCH_DRAG_OFFSET,
			"" + px + "x" + py);
		ed.commit();
	}

	public static String getTouchDragOffset() {
		return pref.getString(Preferences.KEY_TOUCH_DRAG_OFFSET, "0x0");
	}

	public static int getRibbonButtonMult() {
		return pref.getInt(Preferences.KEY_RIBBONBUTTONMULT, 50);
	}

	public static boolean getDebugKeycodes()
	{
		return pref.getBoolean("angband.debug_keycodes", false);
	}

	public static boolean getEnableSoftInput()
	{
		return pref.getBoolean("angband.enable_soft_input", true);
	}

	public static void setEnableSoftInput(boolean value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean("angband.enable_soft_input", value);
		ed.commit();
	}

	public static boolean useSilQGraphics()
	{
		return (Preferences.getActivePlugin() == Plugins.Plugin.silq)
			&& pref.getBoolean(Preferences.KEY_SIL_GX, false);
	}

	public static void setMiddleOpacity(int value)
	{
		if (value < 0) {
			value = 0;
		}
		if (value > 100) {
			value = 100;
		}
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_MIDDLEOPACITY, value);
		ed.commit();
	}

	public static boolean lockSubWindowsOnScroll()
	{
		return true;
	}

	public static String getTileMultiplier()
	{
		if (Preferences.useSilQGraphics()) return "2x1";

		if (Preferences.getActivePlugin().only1x1()) {
			return "1x1";
		}

		return pref.getString("angband.tile_multiplier", "4x2");
	}

	public static int getTileWidth()
	{
		String[] parts = getTileMultiplier().split("x");
		return parts.length == 2 ? Integer.valueOf(parts[0]) : 1;
	}

	public static int getTileHeight()
	{
		String[] parts = getTileMultiplier().split("x");
		return parts.length == 2 ? Integer.valueOf(parts[1]) : 1;
	}

	public static float getTileFontMult()
	{
		String mult = getTileMultiplier();
		if (mult.equals("2x2") || mult.equals("3x2") || mult.equals("4x2")) {
			return 2;
		}
		if (mult.equals("3x3")) {
			return 2.5f;
		}
		if (mult.equals("6x3")) {
			return 3;
		}
		if (mult.equals("4x4")) {
			return 3.5f;
		}
		return 1;
	}

	public static int getGraphicsMode()
	{
		if (Preferences.useSilQGraphics()) return MICROCHASM_GX;

		String val = pref.getString(Preferences.KEY_GRAPHICS, "6");
		int num = Integer.parseInt(val);
		if (getActivePlugin().onlyText()) {
			num = 0;
		}
		return num;
	}

	public static int getExtraRibbonRows()
	{
		//String s = pref.getString(Preferences.KEY_RIBBON_ROWS, "0");
		//return Integer.valueOf(s);
		return 0;
	}

	public static String getUserKeymaps()
	{
		return pref.getString(Preferences.KEY_USERKEYMAPS, "");
	}

	public static void setUserKeymaps(String value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_USERKEYMAPS, value);
		ed.commit();
	}

	public static int getKeyboardOpacity() {
		return pref.getInt(Preferences.KEY_KEYBOARDOPACITY, 50);
	}

	public static void setKeyboardOpacity(int value)
	{
		if (value < 0) {
			value = 0;
		}
		if (value > 100) {
			value = 100;
		}
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_KEYBOARDOPACITY, value);
		ed.commit();
	}

	public static int getDefaultFontSize() {
		return fontSize;
	}
	public static int setDefaultFontSize(int value) {
		return fontSize = value;
	}

	public static boolean getVibrate() {
		return pref.getBoolean(Preferences.KEY_VIBRATE, false);
	}

	public static boolean getPortraitKeyboard() {
		return pref.getBoolean(Preferences.KEY_PORTRAITKB, true);
	}
	public static void setPortraitKeyboard(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_PORTRAITKB, value);
		ed.commit();
	}
	public static boolean getLandscapeKeyboard() {
		return pref.getBoolean(Preferences.KEY_LANDSCAPEKB, false);
	}
	public static void setLandscapeKeyboard(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_LANDSCAPEKB, value);
		ed.commit();
	}

	public static int getNumberSubWindows()
	{
		String str = pref.getString(KEY_NUM_SUBW, defNumSubWindows);
		return Integer.parseInt(str);
	}

	public static void setNumberSubWindows(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(KEY_NUM_SUBW, ""+value);
		ed.commit();
	}

	public static int getFontSizeSubWindows()
	{
		return pref.getInt(KEY_FONT_SUBW, Integer.parseInt(defFontSubWindows));
	}

	public static void setFontSizeSubWindows(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(KEY_FONT_SUBW, value);
		ed.commit();
	}

	public static int getColumnsSubWindows()
	{
		return pref.getInt(KEY_COLS_SUBW, Integer.parseInt(defColsSubWindows));
	}

	public static void setColumnsSubWindows(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(KEY_COLS_SUBW, value);
		ed.commit();
	}

	public static int getRowsSubWindows()
	{
		return pref.getInt(KEY_ROWS_SUBW, Integer.parseInt(defRowsSubWindows));
	}

	public static void setRowsSubWindows(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(KEY_ROWS_SUBW, value);
		ed.commit();
	}

	public static boolean getHorizontalSubWindows()
	{
		return pref.getBoolean(KEY_HORIZ_SUBW, true);
	}

	public static void setHorizontalSubWindows(boolean value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(KEY_HORIZ_SUBW, value);
		ed.commit();
	}

	public static int getTermWidth()
	{
		return pref.getInt(Preferences.KEY_TERMWIDTH, 80);
	}
	public static void setTermWidth(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_TERMWIDTH, value);
		ed.commit();
	}
	public static int getTermHeight()
	{
		return pref.getInt(Preferences.KEY_TERMHEIGHT, 24);
	}
	public static void setTermHeight(int value)
	{
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_TERMHEIGHT, value);
		ed.commit();
	}
	public static void setSize(int width, int height)
	{
		if (width == 0) width = Preferences.cols;
		if (height == 0) height = Preferences.rows;
		setTermWidth(width);
		setTermHeight(height);
	}

	public static void setCoreKeymaps(String k) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_COREKEYMAPS, k);
		ed.commit();
	}

	public static String getCoreKeymaps()
	{
		return pref.getString(Preferences.KEY_COREKEYMAPS, "");
	}

	public static void setCommandMode(boolean k) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_COMMANDMODE, k);
		ed.commit();
	}

	public static boolean getCommandMode()
	{
		return pref.getBoolean(Preferences.KEY_COMMANDMODE, false);
	}

	public static int getPortraitFontSize() {
		return pref.getInt(Preferences.KEY_PORTRAITFONTSIZE, 0);
	}
	public static void setPortraitFontSize(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_PORTRAITFONTSIZE, value);
		ed.commit();
	}
	public static int getLandscapeFontSize() {
		return pref.getInt(Preferences.KEY_LANDSCAPEFONTSIZE, 0);
	}
	public static void setLandscapeFontSize(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_LANDSCAPEFONTSIZE, value);
		ed.commit();
	}

	public static boolean getEnableTouch() {
		return pref.getBoolean(Preferences.KEY_ENABLETOUCH, true);
	}

	public static void setEnableTouch(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_ENABLETOUCH, value);
		ed.commit();
	}

	public static boolean getTouchRight() {
		return pref.getBoolean(Preferences.KEY_TOUCHRIGHT, true);
	}

	public static void setTouchRight(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_TOUCHRIGHT, value);
		ed.commit();
	}

	public static boolean getTopBar() {
		return pref.getBoolean(KEY_TOP_BAR, true);
	}

	public static boolean getRangeReduction()
	{
		return pref.getBoolean(KEY_RANGE_REDUCTION, false);
	}

	public static void setTopBar(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(KEY_TOP_BAR, value);
		ed.commit();
	}

	public static int getTopBarFontMultiplier() {
		return pref.getInt(KEY_MULT_TOP_BAR, 0);
	}

	public static int getAsciiHelperPct() {
		return pref.getInt("angband.ascii_helper", 0);
	}

	public static boolean getShowMouseIcon() {
		return pref.getBoolean("angband.show_mouse_icon", false);
	}

	public static boolean isKeyboardVisible()
	{
		if(Preferences.isScreenPortraitOrientation()) {
			return Preferences.getPortraitKeyboard();
		}
		else {
			return Preferences.getLandscapeKeyboard();
		}
	}

	public static boolean getSkipWelcome() {
		return getActiveProfile().getSkipWelcome();
	}

	public static int[] getInstalledPlugins() {
		return gamePlugins;
	}

	public static String getPluginName(int pluginId) {
		if (pluginId > -1 && pluginId < gamePluginNames.length)
			return gamePluginNames[pluginId];
		else
			return gamePluginNames[0];
	}

	public static String getPluginDescription(int pluginId) {
		if (pluginId > -1 && pluginId < gamePluginDescriptions.length)
			return gamePluginDescriptions[pluginId];
		else
			return gamePluginDescriptions[0];
	}

	public static String getAngbandBaseDirectory() {
		return context.getExternalFilesDir(null).getAbsolutePath();
	}

	public static String getAngbandFilesDirectory(int pluginId) {
		String dir = Plugins.getFilesDir(Plugins.Plugin.convert(pluginId));
		return getAngbandBaseDirectory() + "/lib" + dir;
	}

	public static String getAngbandFilesDirectory() {
		String dir = Plugins.getFilesDir(getActivePlugin());
		return getAngbandBaseDirectory() + "/lib" + dir;
	}

	public static String getActivityFilesDirectory() {
		return activityFilesPath;
	}

	public static String getActivePluginName() {
		return getPluginName(getActivePlugin().getId());
	}

	public static Plugins.Plugin getActivePlugin() {
		int activePlugin;
		int prefPlugin = getActiveProfile().getPlugin();
		activePlugin = gamePlugins[0];
		for(int i = 0; i < gamePlugins.length; i++) {
			if (prefPlugin == gamePlugins[i])
				activePlugin = gamePlugins[i];
		}

		return Plugins.Plugin.convert(activePlugin);
	}

	public static ProfileList getProfiles() {
		if (profiles == null) {
			//Log.d("Angband", "loading profiles");
			String s = pref.getString(Preferences.KEY_PROFILES, "");
			if (s.length() == 0) {
				profiles = ProfileList.deserialize(Plugins.DEFAULT_PROFILE);
				saveProfiles();

				// for some reason ProfileListPreference needs a persisted value to display
				// the very first time.
				// ...there is probably an override to get around this in ListPreference.
				SharedPreferences.Editor ed = pref.edit();
				ed.putString(Preferences.KEY_GAMEPLUGIN, String.valueOf(getActiveProfile().getPlugin()));
				ed.commit();
			}
			else {
				profiles = ProfileList.deserialize(s);
			}
		}
		return profiles;
	}

	public static void saveProfiles() {
		// low-level save
		// assumes validation has already occurred in activity

		//Log.d("Angband", "saving profiles");

		// generate Ids if necessary
		ProfileList pl = getProfiles();
		for(int ix = 0; ix < pl.size(); ix++) {
			if (pl.get(ix).id == 0) {
				pl.get(ix).id = pl.getNextId();
			}
		}

		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_PROFILES, profiles.serialize());
		ed.commit();
	}

	public static Profile getActiveProfile() {
		ProfileList pl = getProfiles();
		int id = pref.getInt(Preferences.KEY_ACTIVEPROFILE, 0);
		Profile p = pl.findById(id);
		if (p == null) {
			p = pl.get(0);
			setActiveProfile(p);
		}
		return p;
	}

	public static void setActiveProfile(Profile p) {
		ProfileList pl = getProfiles();
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_ACTIVEPROFILE, p.id);
		ed.commit();
	}

	public static boolean saveFileExists(String filename) {
		for(int i = 0; i < getInstalledPlugins().length; i++) {
			File f = new File(
				getAngbandFilesDirectory(getInstalledPlugins()[i])
				+ "/save/"
				+ filename
			);
			if (f.exists()) return true;
		}
		return false;
	}

	public static String generateSaveFilename() {
		ProfileList pl = getProfiles();
		String saveFile = null;
		for(int i = 2; i < 100; i++) {
			saveFile = "PLAYER"+i;
			if (pl.findBySaveFile(saveFile,0) == null
				&& !saveFileExists(saveFile))
				break;
		}
		return saveFile;
	}

	public static int alert(Context ctx, String title, String msg) {
		new AlertDialog.Builder(ctx)
			.setTitle(title)
			.setMessage(msg)
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {}
			}
		).show();
		return 0;
	}

	public static KeyMapper getKeyMapper() {
		return keymapper;
	}
}

