package org.rephial.xyangband;

import android.util.Log;

public class GameThread implements Runnable {

	public enum Request{
		StartGame
			,StopGame
			,SaveGame
			,OnGameExit;

		public static Request convert(int value)
		{
			return Request.class.getEnumConstants()[value];
		}
	};

	/* game thread state */
	private Thread thread = null;
	private boolean game_thread_running = false;
	private boolean game_fully_initialized = false;
	private boolean game_restart = false;
	private String running_plugin = null;
	private String running_profile = null;
	private boolean plugin_change = false;
	private NativeWrapper nativew = null;
	private StateManager state = null;

	public GameThread(StateManager s, NativeWrapper nw) {
		nativew = nw;
		state = s;
	}

	public boolean gameRunning()
	{
		return this.game_thread_running && this.game_fully_initialized;
	}

	public synchronized void send(Request rq) {
		switch (rq) {
		case StartGame:
			start();
			break;
		case StopGame:
			stop();
			break;
		case SaveGame:
			save();
			break;
		case OnGameExit:
			onGameExit();
			break;
		}
	}

	private void start() {
		plugin_change = false;

		// sanity checks: thread must not already be running
		// and we must have a valid canvas to draw upon.
		//			already_running = game_thread_running;
		//	already_initialized = game_fully_initialized;

		Log.d("Angband","GameThread.Start()");

		if (state.fatalError) {

			// don't bother restarting here, we are going down.
			Log.d("Angband","start.fatalError is set");
		}
		else if (game_thread_running) {

			/* this is an onResume event */
			if (game_fully_initialized &&
				running_plugin != null &&
				running_profile != null && (
				!running_plugin.equals(Preferences.getActivePluginName()) ||
					!running_profile.equals(Preferences.getActiveProfile().getName())
				)) {

				/* plugin or profile has been changed */

				Log.d("Angband","start.plugin changed");
				plugin_change = true;
				stop();
			}
			else {
				Log.d("Angband","startBand.redrawing");
				state.nativew.resize();
			}
		}
		else {

			//nativew.wclear(0);
			//nativew.updateNow();
			nativew.wipeAll();

			/* time to start angband */

			/* notify wrapper game is about to start */
			nativew.onGameStart();

			/* initialize keyboard buffer */
			state.resetKeyBuffer();

			game_thread_running = true;

			Log.d("Angband","startBand().starting loader thread");

			thread = new Thread(this);
			thread.start();
		}
	}

	private void stop() {
		// signal keybuffer to send quit command to angband
		// (this is when the user chooses quit or the app is pausing)

		Log.d("Angband","GameThread.Stop()");

		if (!game_thread_running) {
			//Log.d("Angband","stop().no game running");
			return;
		}
		if (thread == null)  {
			//Log.d("Angband","stop().no thread");
			return;
		}

		state.signalGameExit();

		Log.d("Angband","signalGameExit.waiting on thread.join()");

		try {
			thread.join();
		} catch (Exception e) {
			Log.d("Angband",e.toString());
		}

		Log.d("Angband","signalGameExit.after waiting for thread.join()");
	}

	private void save() {
		//Log.d("Angband","saveBand()");

		if (!game_thread_running) {
			Log.d("Angband","save().no game running");
			return;
		}
		if (thread == null) {
			Log.d("Angband","save().no thread");
			return;
		}

		state.signalSave();
	}

	private void onGameExit() {
		boolean local_restart = false;

		Log.d("Angband","GameThread.onGameExit()");
		game_thread_running = false;
		game_fully_initialized = false;

		// if game exited normally, restart!
		local_restart
			= game_restart
			= ((!state.getSignalGameExit() || plugin_change)
			   && !state.fatalError);

		if	(local_restart) {
			//nativew.wclear(0);
			//nativew.updateNow();
			nativew.wipeAll();
			state.handler.sendEmptyMessage(AngbandDialog.Action.StartGame.ordinal());
		}
	}

	public void setFullyInitialized() {

		if (!game_fully_initialized) {

			Log.d("Angband","Game is fully initialized");

			game_fully_initialized = true;

			//state.inGameHook();
		}
	}

	public void run() {
		if (game_restart) {
			game_restart = false;
			/* this hackery is no longer needed after
				serializing all access to GameThread
				through the sync'd send() method and
				use of handlers to initiate async actions.  */
			/*
			try {
				// if restarting, pause for effect (and to let the
				// other game thread unlock its mutex!)
				Thread.sleep(400);
			} catch (Exception ex) {}
			*/
		}

		running_plugin = Preferences.getActivePluginName();
		running_profile = Preferences.getActiveProfile().getName();

		Log.d("Angband","GameThread.run " + running_plugin);

		if (nativew.term != null) {
			//Log.d("Angband", "GX: " + nativew.term.useGraphics);
			//nativew.term.serializeVisualState();
		}

		Plugins.preparePlugin(Preferences.getActivePlugin());

		// Plain lib name, android set the folder?
		String pluginPath = "lib" + running_plugin + ".so";

		String width = "" + Preferences.getTermWidth();
		String height = "" + Preferences.getTermHeight();

		String visuals = "" + Preferences.getGraphicsMode()
			+ ":" + Preferences.getTileHeight()
			+ ":" + Preferences.getTileWidth()
			+ ":" + (Preferences.getTopBar() ? 1: 0);

		String pluginName = Preferences.getActivePluginName();

		String rangeReduction = "" + (Preferences.getRangeReduction() ? 1: 0);

		nativew.wipeAll();

		nativew.disableGraphics();

		String[] argv = new String[] {
			Preferences.getAngbandFilesDirectory(),
			Preferences.getActiveProfile().getSaveFile(),
			width,
			height,
			visuals,
			pluginName,
			rangeReduction
		};

		/* game is not running, so start it up */
		nativew.gameStart(
			pluginPath,
			argv.length,
			argv
		);
	}
}
