package org.rephial.xyangband;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Message;
import android.widget.Toast;

public class AngbandDialog {
	private GameActivity activity;
	private StateManager state;
	private ProgressDialog progressDialog = null;
	private ScorePublisher score = null;

	public enum Action {
			OpenContextMenu
			,GameFatalAlert
			,GameWarnAlert
			,StartGame
			,OnGameExit
			,Toast
			,ToggleKeyboard
			,Score
			,ControlMsg;

		public static Action convert(int value)
		{
			return Action.class.getEnumConstants()[value];
		}
    };

	AngbandDialog(GameActivity a, StateManager s) {
		activity = a;
		state = s;
		score = new ScorePublisher(activity);
	}

	public void HandleMessage(Message msg) {
		//Log.d("Angband","handleMessage: "+msg.what);		

		switch (Action.convert(msg.what)) {
		case ControlMsg:
			activity.controlMsg(msg.arg1, (String)msg.obj);
			break;
		case OpenContextMenu: // display context menu
			activity.openContextMenu();
			break;
		case ToggleKeyboard: 
			activity.toggleKeyboard();
			break;
		case GameFatalAlert: // fatal error from angband (native side)
			fatalAlert(state.getFatalError());
			break;
		case GameWarnAlert: // warning from angband (native side)
			warnAlert(state.getWarnError());
			break;
		case StartGame: // start angband
			state.gameThread.send(GameThread.Request.StartGame);
			break;
		case OnGameExit: // angband is exiting
			state.gameThread.send(GameThread.Request.OnGameExit);
			break;
		case Toast: 
			Toast.makeText(activity, (String)msg.obj, Toast.LENGTH_SHORT).show();			
			break;
		case Score:
			score.Publish((ScoreContainer)msg.obj);
			break;
		}
	}

	public void restoreDialog() {
		if (state.fatalError) 
			fatalAlert(state.getFatalError());
		else if (state.warnError) 
			warnAlert(state.getWarnError());
	}

	public int fatalAlert(String msg) {
		new AlertDialog.Builder(activity) 
			.setTitle("Angband")
			.setMessage(msg) 
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						state.fatalMessage=""; 
						state.fatalError=false; 
						activity.finish();
					}
			}
		).show();
		return 0;
	}

	public int warnAlert(String msg) {
		new AlertDialog.Builder(activity) 
			.setTitle("Angband") 
			.setMessage(msg) 
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						state.warnMessage=""; 
						state.warnError = false;
					}
			}
		).show();
		return 0;
	}

	public void ShowScoreEntry()
	{
		score.ShowEntry();
	}

	public void ShowScoreLeaderboards()
	{
		score.ShowLeaderboards();
	}
}