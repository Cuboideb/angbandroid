package org.rephial.xyangband;

import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

import org.rephial.xyangband.KeyMapper.KeyAction;

import java.util.LinkedList;
import java.util.Queue;

public class KeyBuffer {

	/* keyboard state */
	private Queue<Integer> keybuffer = new LinkedList<Integer>();
	private boolean wait = false;
	private int quit_key_seq = 0;
	private boolean signal_game_exit = false;
	private NativeWrapper nativew = null;
	private StateManager state = null;

	private boolean ctrl_mod = false;
	private boolean shift_mod = false;
	private boolean alt_mod = false;
	private boolean shift_down = false;
	private boolean alt_down = false;
	private boolean ctrl_down = false;
	private boolean ctrl_key_pressed = false;
	private boolean ctrl_key_overload = false;
	private boolean shift_key_pressed = false;
	private boolean alt_key_pressed = false;
	private boolean eat_shift = false;

	public KeyBuffer(StateManager state) {
		this.state = state;
		nativew = state.nativew;
		clear();
		if (Preferences.getSkipWelcome()) {
			add(32); //space
		}
		quit_key_seq = 0;
	}

	public void add(int key) {

	    // Clear fast keys
        //state.controlMsg(GameActivity.TERM_CONTROL_LIST_KEYS, "");

		//Log.d("Angband", "KebBuffer.add:"+key);

		synchronized (keybuffer) {
			ctrl_key_overload = false;

			if (key <= 127) {
				if (key >= 'a' && key <= 'z') {
					if (ctrl_mod) {
						key = key - 'a' + 1;
						ctrl_mod = ctrl_down; // if held down, mod is still active
					}
					else if (shift_mod) {
						if (!eat_shift) key = key - 'a'  + 'A';
						shift_mod = shift_down; // if held down, mod is still active
					}
				}
			}

			eat_shift = false;

			alt_key_pressed = alt_down;
			ctrl_key_pressed = ctrl_down;
			shift_key_pressed = shift_down;

			keybuffer.offer(key);
			wakeUp();
		}
	}

	public void performCenterTap()
	{
		KeyAction act = Preferences.getKeyMapper().getCenterScreenTapAction();

		//Log.d("Angband", "Action " + act.name());

		performActionKeyDown(act, 0, null);
		performActionKeyUp(act);
	}

	public void addDirection(int key) {
		boolean rogueLike = state.isRoguelikeKeyboard();
		boolean runningMode = state.getRunningMode();

		if (runningMode && state.cantRun()) runningMode = false;

		// Translate numbers when running
		if (rogueLike && runningMode) {
			switch(key) {
			case '1': key = 'b'; break;
			case '2': key = 'j'; break;
			case '3': key = 'n'; break;
			case '4': key = 'h'; break;
			//case '5': key = ' '; break; // now configurable below
			case '6': key = 'l'; break;
			case '7': key = 'y'; break;
			case '8': key = 'k'; break;
			case '9': key = 'u'; break;
			default: break;
			}
		}
		else {
			// Translate to better keys
			switch(key) {
				case '2':
					key = InputUtils.KC_ARROW_DOWN; break;
				case '4':
					key = InputUtils.KC_ARROW_LEFT; break;
				case '6':
					key = InputUtils.KC_ARROW_RIGHT; break;
				case '8':
					key = InputUtils.KC_ARROW_UP; break;
				default:
					break;
			}
		}

		// center tap
		if (key == '5') {
			performCenterTap();
		}
		else { // directional tap
			if (runningMode && !ctrl_mod) { // let ctrl influence directionals, even with running mode
				if (shift_mod) {  // shift temporarily overrides always run
					eat_shift = true;
				}
				else if (rogueLike) {
					key = Character.toUpperCase(key);
				}
				else {
					add(46); // '.' command
				}
			}

			add(key);
		}
	}

	public void clear() {
		synchronized (keybuffer) {
			keybuffer.clear();
		}
	}

	public int get(int v) {
		int key = 0;

		synchronized (keybuffer) {

			int check = getSpecialKey();
			if (check != -1) {
				key = check;
			}
			// Avoid losing keys before wait
			else if (keybuffer.peek() != null) {
				key = keybuffer.poll();
			}
			else if (v == 1) {
				try {
					//Log.d("Angband", "Wait keypress BEFORE");
					wait = true;
					keybuffer.wait();
					wait = false;
					//Log.d("Angband", "Wait keypress AFTER");
				} catch (Exception e) {
					wait = false;
					Log.d("Angband", "getch() wait exception: " + e);
				}

				// Return key after wait, if there is one
				if (keybuffer.peek() != null) {
					key = keybuffer.poll();
					//Log.w("Angband", "process key = " + key);
				}
			}
		}
		return key;
	}

	public void signalSave() {
		//Log.d("Angband", "signalSave");
		synchronized (keybuffer) {
			keybuffer.clear();
			keybuffer.offer(-1);
			wakeUp();
		}
	}

	public void addSpecialCommand(String command)
	{
		int mark = -200;
		synchronized (keybuffer) {
			//keybuffer.clear();
			keybuffer.offer(mark);
			for (char c: command.toCharArray()) {
				keybuffer.offer((int)c);
			}
			keybuffer.offer(mark);
			wakeUp();
		}
	}

	public void wakeUp() {
		synchronized (keybuffer) {
			if (wait) {
				keybuffer.notify();
			}
		}
	}

	public void signalGameExit() {
		signal_game_exit = true;
		wakeUp();
	}

	public boolean getSignalGameExit() {
		return signal_game_exit;
	}

	public int getSpecialKey()
	{
		int key = -1;

		if (signal_game_exit) {

			int[] keys = {
				state.getKeyEsc(), // Esc
				0,
				state.getKeyQuitAndSave(), // Ctrl-X (Quit)
				0,
				state.getKeyEsc(), // Esc
				0,
				state.getKeyKill() // Kill
			};

			key = keys[quit_key_seq];
			quit_key_seq = (quit_key_seq+1) % keys.length;
		}
		return key;
	}

	private KeyMap getKeyMapFromKeyCode(int keyCode, KeyEvent event)
	{
		int meta=0, event_modifiers=0;

		// Disable alt_mod management for external keyboards. 2020-11-25
		/*
		if (keyCode == KeyEvent.KEYCODE_ALT_LEFT
			|| keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
			return null;
		}
		*/

		if(alt_mod) {
			meta |= KeyEvent.META_ALT_ON;
			meta |= KeyEvent.META_ALT_LEFT_ON;
			if (event.getAction() == KeyEvent.ACTION_UP)
				alt_mod = alt_down; // if held down, mod is still active
		}
		int ch = 0;
		boolean char_mod = false;
		if (event != null) {
			//ch = event.getUnicodeChar(meta);
			if (android.os.Build.VERSION.SDK_INT >= 13) {
			    event_modifiers = event.getModifiers() & ~(KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON|KeyEvent.META_CTRL_RIGHT_ON);
			}
			ch = event.getUnicodeChar(event_modifiers|meta);
			char_mod = (ch > 32 && ch < 127);
		}
		int key_code = char_mod ? ch : keyCode;

		String keyAssign = KeyMap.stringValue(key_code, alt_mod, char_mod);
		KeyMap map = Preferences.getKeyMapper().findKeyMapByAssign(keyAssign);
		//Log.d("Angband", "keyAssign="+keyAssign + " found: " + (map != null ? map.getPrefValue(): ""));
		return map;
	}

	private boolean performActionKeyDown(KeyAction act, int character, KeyEvent event) {
		boolean res = true;

		//Log.d("Angband", "performActionKeyDown: " + act.name() + " " + character);

		if (act == KeyAction.CtrlKey) {
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			ctrl_mod = !ctrl_mod;
			ctrl_key_pressed = !ctrl_mod; // double tap, turn off mod
			ctrl_down = true;
			if (ctrl_key_overload) {
				// ctrl double tap, translate into appropriate action
				act = Preferences.getKeyMapper().getCtrlDoubleTapAction();
			}
		}

   		switch(act){
		case CharacterKey:
			add(character);
			break;
		case BackKey:
			add(state.getKeyEsc());
			break;
		case EscKey:
			add(state.getKeyEsc());
			break;
		case BackspaceKey:
			add(state.getKeyBackspace());
			break;
		case DeleteKey:
			add(state.getKeyDelete());
			break;
		case Five:
			add('5');
			break;
		case Space:
			add(' ');
			break;
		case Period:
			add('.');
			break;
		case Comma:
			add(',');
			break;
		case CenterPlayer:
			add('@');
			break;
		case EnterKey:
			add(state.getKeyEnter());
			break;
		case ArrowDownKey:
			add(state.getKeyDown());
			break;
		case ArrowUpKey:
			add(state.getKeyUp());
			break;
		case ArrowLeftKey:
			add(state.getKeyLeft());
			break;
		case ArrowRightKey:
			add(state.getKeyRight());
			break;
		case AltKey:
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			alt_mod = !alt_mod;
			alt_key_pressed = !alt_mod; // double tap, turn off mod
			alt_down = true;
			break;
		case ShiftKey:
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			shift_mod = !shift_mod;
			shift_key_pressed = !shift_mod; // double tap, turn off mod
			shift_down = true;
			break;
		case ZoomIn:
			nativew.increaseFontSize();
			break;
		case ZoomOut:
			nativew.decreaseFontSize();
			break;
		case CtrlKey:
			//handled above
			break;
		case VirtualKeyboard:
			// handled on keyup
			break;
		default:
			res = false; // let the OS handle the key
			break;
		}
		return res;
	}

	private boolean performActionKeyUp(KeyAction act) {
		boolean res = true; // handled the key

		//Log.d("Angband", "performActionKeyUp: " + act.name());

		switch(act){
		case AltKey:
			alt_down = false;
			alt_mod = !alt_key_pressed; // turn off mod only if used at least once
			break;
		case CtrlKey:
			ctrl_down = false;
			ctrl_mod = !ctrl_key_pressed; // turn off mod only if used at least once
			ctrl_key_overload = ctrl_mod;
			break;
		case ShiftKey:
			shift_down = false;
			shift_mod = !shift_key_pressed; // turn off mod only if used at least once
			break;
		case VirtualKeyboard:
			state.handler.sendEmptyMessage(AngbandDialog.Action.ToggleKeyboard.ordinal());
			break;

		// these are handled on keydown
		case ZoomIn:
		case ZoomOut:
		case None:
		case CharacterKey:
			break;

		default:
			res = false;  // let the OS handle the key
			break;
		}
		return res;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Log.d("Angband", "onKeyDown("+keyCode+","+event+")");

		KeyMap map = getKeyMapFromKeyCode(keyCode, event);
		if (map == null)
			return false;
		else
			return performActionKeyDown(map.getKeyAction(), map.getCharacter(), event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		//Log.d("Angband", "onKeyUp("+keyCode+","+event+")");

		KeyMap map =  getKeyMapFromKeyCode(keyCode, event);
		if (map == null)
			return false;
		else
			return performActionKeyUp(map.getKeyAction());
	}
}
