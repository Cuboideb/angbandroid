package org.rephial.angband;

import android.content.Context;
import android.app.Activity;
import android.view.LayoutInflater;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.util.Log;

import org.rephial.angband.AngbandKeyboardView;

public class AngbandKeyboard implements OnKeyboardActionListener
{
	AngbandKeyboardView virtualKeyboardView;
	Keyboard kbLayoutQwerty;
	Keyboard kbLayoutSymbols;
	Keyboard kbLayoutSymbolsShift;
	StateManager state = null;

	AngbandKeyboard(Context ctx)
	{
		state = ((GameActivity)ctx).getStateManager();

		kbLayoutQwerty       = new Keyboard(ctx, R.xml.keyboard_qwerty);
		kbLayoutSymbols      = new Keyboard(ctx, R.xml.keyboard_sym);
		kbLayoutSymbolsShift = new Keyboard(ctx, R.xml.keyboard_symshift);

		LayoutInflater inflater = LayoutInflater.from(ctx);
		virtualKeyboardView = (AngbandKeyboardView)inflater.inflate(R.layout.input, null);
		virtualKeyboardView.setOnKeyboardActionListener(this);

		switchKeyboard(kbLayoutQwerty);

		boolean shouldRun = state.getRunningMode();
		virtualKeyboardView.setRunning(shouldRun);
	}

	private void switchKeyboard(Keyboard newKb)
	{
		Keyboard current = virtualKeyboardView.getKeyboard();

		if (newKb == current) {
			return;
		} else {
			if (current == kbLayoutSymbols) {
				virtualKeyboardView.setSymbolic(false);
			} else if (current == kbLayoutSymbolsShift) {
				virtualKeyboardView.setControl(false);
			} else if (current == kbLayoutQwerty) {
				virtualKeyboardView.setShifted(false);
			}

			virtualKeyboardView.setKeyboard(newKb);

			if (newKb == kbLayoutSymbols) {
				virtualKeyboardView.setSymbolic(true);
			} else if (newKb == kbLayoutSymbolsShift) {
				virtualKeyboardView.setControl(true);
			}
		}
	}

	public void onKey(int primaryCode, int[] keyCodes)
	{
		char c = 0;

		switch (primaryCode) {
			case Keyboard.KEYCODE_DELETE:
				c = 0x9F;
				break;

			case Keyboard.KEYCODE_SHIFT: {
				Keyboard currentKeyboard = virtualKeyboardView.getKeyboard();
				if (currentKeyboard == kbLayoutQwerty) {
					virtualKeyboardView.setShifted(!virtualKeyboardView.isShifted());
				}

				break;
			}

			case Keyboard.KEYCODE_ALT: {
				Keyboard keyboard = virtualKeyboardView.getKeyboard();
				if (keyboard == kbLayoutSymbolsShift) {
					keyboard = kbLayoutQwerty;
				} else {
					keyboard = kbLayoutSymbolsShift;
				}

				switchKeyboard(keyboard);
				break;
			}

			case Keyboard.KEYCODE_MODE_CHANGE: {
				Keyboard keyboard = virtualKeyboardView.getKeyboard();
				if (keyboard == kbLayoutSymbols) {
					keyboard = kbLayoutQwerty;
				} else {
					keyboard = kbLayoutSymbols;
				}

				switchKeyboard(keyboard);
				break;
			}

			// Running mode; find a place to store this constant XXX
			// should maybe be a property of StateManager, also XXX
			case -4: {
				boolean shouldRun = !state.getRunningMode();
				state.setRunningMode(shouldRun);
				virtualKeyboardView.setRunning(shouldRun);
				break;
			}

			default: {
				c = (char)primaryCode;
				if (virtualKeyboardView.getKeyboard() == kbLayoutQwerty &&
						virtualKeyboardView.isShifted()) {
					c = Character.toUpperCase(c);
					virtualKeyboardView.setShifted(false);
				}

				if (virtualKeyboardView.getKeyboard() == kbLayoutSymbolsShift) {
					switchKeyboard(kbLayoutQwerty);
				}

				break;
			}
		}

		if (c != 0) {
			state.addKey(c);
		}
	}

	public void onPress(int primaryCode)
	{
	}
	public void onRelease(int primaryCode)
	{
	}
	public void onText(CharSequence text)
	{
	}
	public void swipeDown()
	{
	}
	public void swipeLeft()
	{
	}
	public void swipeRight()
	{
	}
	public void swipeUp()
	{
	}
}
