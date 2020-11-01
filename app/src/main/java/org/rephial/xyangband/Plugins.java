package org.rephial.xyangband;

import org.rephial.xyangband.R;

import java.io.InputStream;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

final public class Plugins {
	public enum Plugin {
		angband(0),
		frogcomposband(1),
		npp041(2),
		faangband(3);

		private int id;

		private Plugin(int id) {
			this.id = id;
		}
		public int getId() {
			return id;
		}
		public static Plugin convert(int value) {
			return Plugin.class.getEnumConstants()[value];
		}
		public boolean onlyText()
		{
			return (id > 0);
		}
		public boolean only1x1()
		{
			return (id > 0) && (id != 3);
		}
		public boolean noMouse()
		{
			return (id > 0) && (id != 3);
		}
		public boolean enableSubWindows()
		{
			return (id == 0);
		}
	}

	static final String DEFAULT_PROFILE = "0~Default~PLAYER~0~0";
	public static String LoaderLib ="loader-angband";

	public static String getFilesDir(Plugin p) {
//		switch (p) {
//		case Sangband: return "sang";
//		case Steamband: return "steam";
//		default: return p.toString().toLowerCase();
//		}
		return p.toString().toLowerCase();
	}

	/* These should all match with ui-event.h */
	public static int getKeyDown(Plugin p)        { return 0x80; }
	public static int getKeyUp(Plugin p)          { return 0x83; }
	public static int getKeyLeft(Plugin p)        { return 0x81; }
	public static int getKeyRight(Plugin p)       { return 0x82; }

	public static int getKeyEnter(Plugin p)       { return 0x9c; }
	public static int getKeyTab(Plugin p)         { return 0x9d; }
	public static int getKeyDelete(Plugin p)      { return 0x9e; }
	public static int getKeyBackspace(Plugin p)   { return 0x9f; }
	public static int getKeyEsc(Plugin p)         { return 0xE000; }
	public static int getKeyQuitAndSave(Plugin p) { return 0x18; }

	public static ZipInputStream getPluginZip(int plugin) {
		InputStream is = null;

		if (plugin == Plugin.angband.getId())
			is = Preferences.getResources().openRawResource(R.raw.zipangband);

		if (plugin == Plugin.faangband.getId())
			is = Preferences.getResources().openRawResource(R.raw.zipfaangband);

		if (plugin == Plugin.frogcomposband.getId())
			is = Preferences.getResources().openRawResource(R.raw.zipfrogcomposband);

		if (plugin == Plugin.npp041.getId())
			is = Preferences.getResources().openRawResource(R.raw.zipnpp041);

		return new ZipInputStream(is);
	}
	public static String getPluginCrc(int plugin) {
		InputStream is = null;
		if (plugin == Plugin.angband.getId()) {
			is = Preferences.getResources().openRawResource(R.raw.crcangband);
		}
		else if (plugin == Plugin.faangband.getId()) {
			is = Preferences.getResources().openRawResource(R.raw.crcfaangband);
		}
		else if (plugin == Plugin.frogcomposband.getId()) {
			is = Preferences.getResources().openRawResource(R.raw.crcfrogcomposband);
		}
		else if (plugin == Plugin.npp041.getId()) {
			is = Preferences.getResources().openRawResource(R.raw.crcnpp041);
		}
		else {
			return "";
		}
		return new Scanner(is).useDelimiter("\\A").next().trim();
	}

	public static String getUpgradePath(Plugin p) {
		switch (p) {
		default: return "";
		}
	}
}
