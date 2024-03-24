package org.rephial.xyangband;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Installer {

	/* installer state */
	public enum InstallState {
		Unknown
			,MediaNotReady
			,InProgress
			,Success
			,Failure;
		public static InstallState convert(int value)
		{
			return InstallState.class.getEnumConstants()[value];
		}
		public static boolean isError(InstallState s) {
			return (s == MediaNotReady || s == Failure);
		}
    };

	private static Thread installWorker = null;
	public InstallState state;
	public String message = "";
	public Activity activity = null;
	public boolean userResponded = false;

	public Installer(Activity _activity)
	{
		state = InstallState.Unknown;
		activity = _activity;
	}

	public synchronized void startInstall() {
		if (state == InstallState.Unknown) {
			state = InstallState.InProgress;
			
			installWorker = new Thread() {  
					public void run() {
						/* Old permission check
						// Check write permission
						if (ContextCompat.checkSelfPermission(activity,
								Manifest.permission.WRITE_EXTERNAL_STORAGE)
								!= PackageManager.PERMISSION_GRANTED) {

							ActivityCompat.requestPermissions(activity,
									new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									1);

							// Wait until the user responds
							while (!userResponded) {
								try { Thread.sleep(100); }
								catch (InterruptedException e) { }
							}

							// Check state to see if permissions were granted
							if (state != InstallState.Success) {
								message = "Error: Cannot create necessary library files without WRITE permissions.";
							}
						}
						// Permission already granted
						else {
							install();
						}
						*/

						// In 2023, just install in scoped storage
						install();
					}
				};
			installWorker.start();
		}
		else {
			return; // install is in error or in progress, cancel
		}
	}

	public boolean failed() {
		return state != InstallState.Success;
	}

	public String errorMessage() {
		String errorMsg = "Error: failed to write and verify files to external storage, cannot continue.";
		switch(state) {
		case MediaNotReady:
			errorMsg = "Error: external storage card not found, cannot continue.";
			break;
		case Failure:
			if (message.length() > 0) 
				errorMsg = message;
			break;
		}
		return errorMsg;
	}

	public boolean needsInstall() {
		// validate sdcard here
		String check = Environment.getExternalStorageState();
		//Log.v("Angband", "media check:" + check);
		if (check.compareTo(Environment.MEDIA_MOUNTED) != 0) {
			state = InstallState.MediaNotReady;
			return true;
		}

		/* old verification method
		if (Preferences.getInstalledVersion().compareTo(Preferences.getVersion()) == 0) {
			state = InstallState.Success;
			return false;
		}
		*/

		/* new verification method compares plugin files crc value */
		int[] plugins = Preferences.getInstalledPlugins();
		for(int i = 0; i < plugins.length; i++) {
			if (!doesCrcMatch(plugins[i])) return true;
		}

		state = InstallState.Success;
		return false;
	}

	public void install() {
		message = "";

		boolean success = true;
		int[] plugins = Preferences.getInstalledPlugins();
		for(int i = 0; i < plugins.length; i++) {

			// basic install of required files
			success = extractPluginResources(plugins[i]);
			if (!success) break;

			// upgrade if necessary
			success = upgradePlugin(plugins[i]);
			if (!success) break;
		}
		if (success) {
			// replaced with crc logic
			//Preferences.setInstalledVersion(Preferences.getVersion());
			state = InstallState.Success;
		}
		else {
			state = InstallState.Failure;
		}
	}

	private boolean doesCrcMatch(int plugin) {
		Log.d("Angband","doesCrcMatch "+plugin);
		boolean result = false;
		try {
			File f = new File(Preferences.getAngbandFilesDirectory(plugin));
			f.mkdirs();
			String filename = f.getAbsolutePath() + "/crc" + Plugins.getFilesDir(Plugins.Plugin.convert(plugin));
			File myfile = new File(filename);
			FileInputStream fis = new FileInputStream(myfile);
			String currentCrc = new Scanner(fis).useDelimiter("\\A").next().trim();
			//Log.d("Angband","doesCrcMatch.currentcrc="+currentCrc);
			//Log.d("Angband","doesCrcMatch.plugincrc="+Plugins.getPluginCrc(plugin));
			result = (Plugins.getPluginCrc(plugin).compareTo(currentCrc) == 0);
		} catch (Exception e) {
			Log.v("Angband", "doesCrcMatch.error reading crc: " + e);
		}
		return result;
	}

	private boolean extractPluginResources(int plugin) {
		//Log.d("Angband","extractPluginResources "+plugin);
		boolean result = true;
		try {
			File f = new File(Preferences.getAngbandFilesDirectory(plugin));
			f.mkdirs();
			String abs_path = f.getAbsolutePath();
			//Log.v("Angband", "installing to " + abs_path);

			// copy game files
			ZipInputStream zis = Plugins.getPluginZip(plugin);
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String ze_name = ze.getName();
				//Log.v("Angband", "extracting " + ze_name);

				String filename = abs_path + "/" + ze_name;
				File myfile = new File(filename);

				// Avoid path traversal vulnerability
				if (!myfile.getCanonicalPath().startsWith(abs_path)) {
					throw new SecurityException();
				}

				if (ze.isDirectory()) {
					myfile.mkdirs();
					continue;
				}

				byte contents[] = new byte[(int) ze.getSize()];

				FileOutputStream fos = new FileOutputStream(myfile);
				int remaining = (int) ze.getSize();

				int totalRead = 0;

				while (remaining > 0) {
					int readlen = zis.read(contents, 0, remaining);
					fos.write(contents, 0, readlen);
					totalRead += readlen;
					remaining -= readlen;
				}

				fos.close();

				// perform a basic length validation
				myfile = new File(filename);
				if (myfile.length() != totalRead) {
					//Log.v("Angband", "Installer.length mismatch: " + filename);
					message = "Error: failed to verify installed file on sdcard: "+filename;
					throw new IllegalStateException();
				}

				zis.closeEntry();
			}
			zis.close();

			// update crc file
			File myfile_crc = new File(abs_path + "/crc" + Plugins.getFilesDir(Plugins.Plugin.convert(plugin)));
			if (myfile_crc.exists()) myfile_crc.delete();
			Writer crc_out = new OutputStreamWriter(new FileOutputStream(myfile_crc));
			String crc_val = Plugins.getPluginCrc(plugin);
			crc_out.write(crc_val);
			crc_out.close();

		} catch (Exception e) {
			result = false;
			if (message.length() == 0)
				message = "Error: failed to install files to sdcard. "+e.getMessage();
			//Log.v("Angband", "error extracting files: " + e);
		}
		return result;
	}

	public void waitForInstall() {
		if (installWorker != null) {
			try {
				installWorker.join();
				installWorker = null;
			} catch (Exception e) {
				Log.d("Angband","installWorker "+e.toString());
			}
		}
	}

	private boolean upgradePlugin(int plugin) {
		try {
			String srcDir = Plugins.getUpgradePath(Plugins.Plugin.convert(plugin));
			if (srcDir.length() == 0) return true; // no upgrade to do
		
			String dstDir = Preferences.getAngbandFilesDirectory(plugin)+"/save";

			//Log.d("Angband","upgrade "+srcDir+" to "+dstDir);

			File fdstDir = new File(dstDir);
			File[] saveFiles = fdstDir.listFiles();
			if (saveFiles != null && saveFiles.length>0) return true; // save files found in dst (already upgraded)

			//Log.d("Angband","found no save files in dst");

			File fsrcDir = new File(srcDir);
			saveFiles = fsrcDir.listFiles();

			if (saveFiles == null || saveFiles.length==0) return true; // no save files found in src

			//Log.d("Angband","found "+saveFiles.length+" save files in src");

			// upgrade!
			for (int i=0; i<saveFiles.length; i++) {
				String src = saveFiles[i].getAbsolutePath();
				String dst = dstDir +"/"+saveFiles[i].getName();

				//Log.d("Angband","upgrade "+src+" to "+dst);
				InputStream in = new FileInputStream(src);
				OutputStream out = new FileOutputStream(dst);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0){
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
			return true;
		} catch (Exception e) {
			message = "Error: failed to copy save file(s) from prior version. "+e.getMessage();
			//Log.v("Angband", "error upgrading save files: " + e);
			return false;
		}
	}

	/*
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		//Log.d("Angband", "delete old file: "+dir.getAbsolutePath());
		return dir.delete();
	}
	*/

	public void userRespondedToPermissionRequest(boolean accepted) {
		if (accepted) {
			install();
		}
		else {
			state = InstallState.Failure;
		}
		userResponded = true;
	}
}
