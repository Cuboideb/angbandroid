package org.rephial.xyangband;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.LayoutInflater;

public class AngbandKeyboard implements OnKeyboardActionListener
{
	AngbandKeyboardView virtualKeyboardView;
	Keyboard kbLayoutQwerty;
	Keyboard kbLayoutSymbols;
	Keyboard kbLayoutSymbolsShift;
	StateManager state = null;
	GameActivity game_context = null;

	AngbandKeyboard(Context ctx)
	{
		game_context = (GameActivity)ctx;

		state = game_context.getStateManager();

        if (Preferences.getQwertyNumPad()) {
            kbLayoutQwerty = new Keyboard(ctx, R.xml.keyboard_qwerty_portrait);
        }
        else {
            kbLayoutQwerty = new Keyboard(ctx, R.xml.keyboard_qwerty_classic);
        }
		kbLayoutSymbols      = new Keyboard(ctx, R.xml.keyboard_sym_portrait);
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

		// Show the keyboard, consume the keypress
		if (virtualKeyboardView.getHideAllKeys() ||
				virtualKeyboardView.getHideSomeKeys()) {
			String label = virtualKeyboardView.getLabelFromKeyCode(primaryCode);
			if (virtualKeyboardView.keyShouldHide(label)) {
				virtualKeyboardView.setHideAllKeys(false);
				virtualKeyboardView.setHideSomeKeys(false);
                return;
			}
		}

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

			case -7000: {
				//virtualKeyboardView.showMenu();
				virtualKeyboardView.toggleHideSomeKeys();
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
		    // If player is running, translate keys
		    if (state.getRunningMode()) {

		        String numbers = "12346789";
		        String letters = "bjnhlyku";

		        // Roguelike, only letters
		        if (state.isRoguelikeKeyboard()) {

		            if (letters.indexOf(c) > -1) {
                        state.addDirectionKey(c);
                        return;
                    }
                }
		        // Classic, only numbers
		        else if (numbers.indexOf(c) > -1) {
                    state.addDirectionKey(c);
                    return;
                }
            }

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
		//_context.toggleKeyboard();
	}
	public void swipeLeft()
	{
		/*
		int value = Preferences.getKeyboardOpacity();
		value = (int)(value * 0.7f);
		Preferences.setKeyboardOpacity(value);
		 */
	}
	public void swipeRight()
	{
		/*
		int value = Preferences.getKeyboardOpacity();
		value = (int)(value * 1.3f);
		Preferences.setKeyboardOpacity(value);
		*/
	}
	public void swipeUp()
	{
	}
}
