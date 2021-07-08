/*
 * File: AngbandActivity.java
 * Purpose: Splash & installer
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
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;

import org.rephial.xyangband.R;

public class AngbandActivity extends Activity
	implements OnRequestPermissionsResultCallback {

	protected boolean active = true;
	protected int splashTime = 500;
	protected ProgressDialog progressDialog = null;
	protected Handler handler = null;
	protected Installer installer = null;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		String version = "unknown";
		long versionCode = 0;

		try {
			ComponentName comp = new ComponentName(this, AngbandActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			version = pinfo.versionName;
			versionCode = pinfo.getLongVersionCode();
		} catch (Exception e) {}

		Preferences.init (
			this,
			getFilesDir(),
			getResources(),
			getSharedPreferences(Preferences.NAME, MODE_PRIVATE),
			version,
			versionCode
		);

		// Simulate fresh install
		/*
		Preferences.setTermWidth(80);
		Preferences.setTermHeight(24);
		Preferences.setPortraitFontSize(0);
		Preferences.setLandscapeFontSize(0);
		Preferences.setTopBar(true);
		Preferences.setNumberSubWindows(2);
		Preferences.setHorizontalSubWindows(true);
		*/

		final Activity splash = this;
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case 0:
					//Log.d("Angband", "handler show progress");
					progressDialog = ProgressDialog.show(splash, "Angband", "Installing files...", true);
					break;
				case 1:
					//Log.d("Angband", "handler dismiss progress");
					/*
					if (progressDialog != null) progressDialog.dismiss();
					progressDialog = null;
					*/
					dismissDialog();
					break;
				case 2:
					//Log.d("Angband", "handler fatal error");
					new AlertDialog.Builder(splash)
						.setTitle("Angband")
						.setMessage((String)msg.obj)
						.setNegativeButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								splash.finish();
							}
						}).show();
					break;
				}
			}
		};

		checkInstall();
	}

	public void dismissDialog()
	{
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		progressDialog = null;
	}

	@Override
	public void onDestroy() {
		dismissDialog();
		super.onDestroy();
	}

	@Override
	public void onStop() {
		super.onStop();
		StatPublisher.stop(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		StatPublisher.start(this);
	}

	public synchronized void checkInstall() {
		final Activity splash = this;
		Thread splashTread = new Thread() {
			@Override
			public void run() {
				Log.d("Angband", "splashThread.run");
				installer = new Installer(splash);
				try {
					int waited = 0;
					Log.d("Angband", "splashThread.installer.needsInstall");
					if (installer.needsInstall()) {
						handler.sendEmptyMessage(0); //show progress
						Log.d("Angband", "splashThread.startinstall");
						installer.startInstall();
						Log.d("Angband", "splashThread.wait");
						installer.waitForInstall();
						Log.d("Angband", "finished waiting");
						handler.sendEmptyMessage(1); //dismiss progress

						splashTime = 200;
					}

					active = true;
					while(active && (waited < splashTime)) {
						sleep(100);
						if(active) {
							waited += 100;
						}
					}
				} catch(InterruptedException e) {
				} finally {
					if (installer.failed()) {
						handler.sendMessage(Message.obtain(handler,2,installer.errorMessage()));
						return;
					}
					else {
						finish();
						Intent intent = new Intent(splash, GameActivity.class);
						startActivity(intent);
					}
				}
			}
		};
		splashTread.start();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			active = false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// Granted pemission - proceed with installation
		boolean accepted = (requestCode == 1 && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED);

		installer.userRespondedToPermissionRequest(accepted);
	}
}
