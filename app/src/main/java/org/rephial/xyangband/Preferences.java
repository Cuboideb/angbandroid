package org.rephial.xyangband;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.io.File;
import java.lang.reflect.Array;

final public class Preferences {

	public static final int rows = 24;
	public static final int cols = 80;

	static final String NAME = "angband";

	static final String KEY_VIBRATE = "angband.vibrate";
	static final String KEY_FULLSCREEN = "angband.fullscreen";
	static final String KEY_ORIENTATION = "angband.orientation";

	static final String KEY_KEYBOARDOVERLAP = "angband.allowKeyboardOverlap";
	static final String KEY_KEYBOARDOPACITY = "angband.keyboardOpacity";
	static final String KEY_MIDDLEOPACITY = "angband.middleOpacity";
	static final String KEY_ENABLETOUCH = "angband.enabletouch";

	static final String KEY_TOUCHRIGHT = "angband.touchright";	
	static final String KEY_TOUCHMULTIPLIER = "angband.touchmultiplier";

	static final String KEY_RIBBONBUTTONMULT = "angband.ribbonbuttonmult";

	static final String KEY_USERKEYMAPS = "angband.userkeymaps";

	static final String KEY_CENTERTAP = "angband.centerscreentap";
	static final String KEY_PORTRAITKB = "angband.portraitkb";
	static final String KEY_LANDSCAPEKB = "angband.landscapekb";
	static final String KEY_PORTRAITFONTSIZE = "angband.portraitfontsize";
	static final String KEY_LANDSCAPEFONTSIZE = "angband.landscapefontsize";

	static final String KEY_TERMWIDTH = "angband.termwidth";
	static final String KEY_TERMHEIGHT = "angband.termheight";

	static final String KEY_QWERTYNUMPAD = "angband.qwertynumpad";

	static final String KEY_USEICONS = "angband.useicons";

	static final String KEY_COREKEYMAPS = "angband.corekeymaps";
	static final String KEY_RIBBONALPHA = "angband.ribbonalpha";
	static final String KEY_COMMANDMODE = "angband.commandmode";

	static final String KEY_GAMEPLUGIN = "angband.gameplugin";
	static final String KEY_GAMEPROFILE = "angband.gameprofile";
	static final String KEY_SKIPWELCOME = "angband.skipwelcome";

	static final String KEY_BIGTILES = "angband.bigtiles";
	static final String KEY_GRAPHICS = "angband.graphics";
	static final String KEY_PSEUDOASCII = "angband.pseudoascii";

	static final String KEY_PROFILES = "angband.profiles";
	static final String KEY_ACTIVEPROFILE = "angband.activeprofile";

	static final String KEY_INSTALLEDVERSION = "angband.installedversion";

	private static String activityFilesPath;

	private static SharedPreferences pref;
	private static int[] gamePlugins;
	private static String[] gamePluginNames;
	private static String[] gamePluginDescriptions;
	private static ProfileList profiles;
	private static String version;
	private static int fontSize = 17;
	private static Resources resources;

	private static KeyMapper keymapper;

	public static Context context;

	Preferences() {}

	public static void init(Context p_context, File filesDir,
			Resources res, SharedPreferences sharedPrefs, String pversion) {

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
		version = pversion;

		keymapper = new KeyMapper(pref);
	}

	public static boolean getQwertyNumPad()
	{
		return pref.getBoolean(Preferences.KEY_QWERTYNUMPAD, false);
	}

	public static String getVersion() {
		return version;
	}

	public static Resources getResources() {
		return resources;
	}

	public static String getString(String key) {
		return pref.getString(key, "");
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

	public static boolean getIconsEnabled() {
		return pref.getBoolean(Preferences.KEY_USEICONS, true);
	}

	public static boolean getKeyboardOverlap() {
		return pref.getBoolean(Preferences.KEY_KEYBOARDOVERLAP, true);
	}

	public static int getMiddleOpacity() {
		return pref.getInt(Preferences.KEY_MIDDLEOPACITY, 100);
	}

	public static int getTouchMultiplier() {
		return pref.getInt(Preferences.KEY_TOUCHMULTIPLIER, 40);
	}

	public static int getRibbonButtonMult() {
		return pref.getInt(Preferences.KEY_RIBBONBUTTONMULT, 50);
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

	public static int getTileWidth()
	{
		if (pref.getBoolean(Preferences.KEY_BIGTILES, true)) {

			// In text-mode, set a smaller tile
			if (Preferences.getGraphicsMode() == 0) {
				return 2;
			}

			return 3;
		}
		return 1;
	}
	public static int getTileHeight()
	{
		if (pref.getBoolean(Preferences.KEY_BIGTILES, true)) {
			return 2;
		}
		return 1;
	}
	public static int getGraphicsMode()
	{		
		String val = pref.getString(Preferences.KEY_GRAPHICS, "6");
		int num = Integer.parseInt(val);
		//return Math.min(num, 4);
		return num;
	}
	public static boolean getPseudoAscii()
	{		
		return pref.getBoolean(Preferences.KEY_PSEUDOASCII, false);
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

	public static void setRibbonAlpha(int k) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_RIBBONALPHA, k);
		ed.commit();
	}

	public static int getRibbonAlpha()
	{
		return pref.getInt(Preferences.KEY_RIBBONALPHA, 2);
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

	public static boolean getTouchRight() {
		return pref.getBoolean(Preferences.KEY_TOUCHRIGHT, true);
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
		//return Environment.getExternalStorageDirectory()
		//		+ "/"
		//		+ "Android/data/org.rephial.xyangband/files/lib";
		return context.getExternalFilesDir(null) + "/lib";
	}

	public static String getAngbandFilesDirectory(int pluginId) {
		return getAngbandBaseDirectory()
			+ Plugins.getFilesDir(Plugins.Plugin.convert(pluginId));
	}

	public static String getAngbandFilesDirectory() {
		String dir = Plugins.getFilesDir(getActivePlugin());
		return getAngbandBaseDirectory() + dir;
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

