package org.rephial.xyangband;

//import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.Log;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.MenuItem;
import android.view.View;
//import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.FrameLayout;
import android.widget.LinearLayout;
//import android.widget.PopupMenu;
//import android.widget.PopupWindow;
//import android.widget.ScrollView;
//import android.widget.TableLayout;
import android.view.ViewGroup.LayoutParams;
//import android.widget.TableRow;
//import android.widget.TextView;
//import android.widget.HorizontalScrollView;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import androidx.core.graphics.ColorUtils;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

class AdvKeyboard implements OnTouchListener
{
	public GameActivity context;
	public StateManager state;
	public LinearLayout mainView;
	public LinearLayout[] rows;
	public int btnWidth;
	public int btnHeight;
	public int page = 0;
	public int shiftMode = 0;
	public boolean keymapMode = false;
	public Paint back;
	public Paint fore;
	public Paint foreBold;
	public Point winSize;
	public int opacityMode = 0;
	public Handler handler;

	public boolean vertical = false;

	public AdvButton lastButton = null;
	public boolean skipInput = false;
	public static int LONG_PRESS = 1;

	public class KeyInfo
	{
		String action;
		boolean alwaysVisible;
	};

	public HashMap<String,KeyInfo> keys = new HashMap<>();

	public AdvKeyboard(GameActivity p_context)
	{
		context = p_context;
		state = context.state;

		back = new Paint();

		foreBold = new Paint();
		foreBold.setTextAlign(Paint.Align.LEFT);
		foreBold.setAntiAlias(true);
		foreBold.setTypeface(context.monoBoldFont);

		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		fore.setAntiAlias(true);
		fore.setTypeface(context.monoFont);

		mainView = new LinearLayout(context);
		mainView.setOrientation(LinearLayout.VERTICAL);
		mainView.setOnTouchListener(this);

		vertical = Preferences.getVerticalKeyboard();

		int nRows = 5;
		if (vertical) nRows = 10;

		rows = new LinearLayout[nRows];
		for (int i = 0; i < rows.length; i++) {
			LinearLayout ll = new LinearLayout(context);
			ll.setLayoutParams(new LayoutParams
				(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
			ll.setOrientation(LinearLayout.HORIZONTAL);
			mainView.addView(ll);
			rows[i] = ll;
		}

		winSize = context.getMySize();

		reloadKeymaps();

		createHandler();

		showCurrentPage();
	}

	public void createHandler()
	{
		handler = new Handler(Looper.myLooper()) {
        	public void handleMessage(Message msg)
        	{
        		if (msg.what == LONG_PRESS && lastButton != null) {
        			lastButton.longPress();
        			lastButton = null;
        			setFlashText(null);
        			// Discard input until new down or cancel events
        			skipInput = true;
        		}
        	}
        };
	}

	public void unpressAll()
	{
		for (AdvButton btn: collectButtons()) {
			btn.setPressed(false);
		}
	}

	public AdvButton getButtonAt(float x, float y)
	{
		int row = (int)(y / Math.max(btnHeight, 1));
		int col = (int)(x / Math.max(btnWidth, 1));

		if (row >= 0 && col >= 0 &&
			row < rows.length && col < rows[row].getChildCount()) {

			return (AdvButton)rows[row].getChildAt(col);
		}

		return null;
	}

	public boolean onTouch(View v, MotionEvent event)
	{
		unpressAll();

		AdvButton btn = getButtonAt(event.getX(), event.getY());

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			handler.removeMessages(LONG_PRESS);
			lastButton = btn;
			setFlashText(btn);
			skipInput = false;
			if (btn != null) {
				handler.sendEmptyMessageDelayed(LONG_PRESS, 1000);
				btn.setPressed(true);
			}
		}

		if (event.getAction() == MotionEvent.ACTION_UP && !skipInput) {
			handler.removeMessages(LONG_PRESS);
			// Long press clears lastButton
			setFlashText(null);
			if (btn != null && btn == lastButton) {
				btn.execute();
			}
			lastButton = null;
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE && !skipInput) {
			if (btn == null || btn != lastButton) {
				handler.removeMessages(LONG_PRESS);
			}
			lastButton = btn;
			setFlashText(btn);
			if (btn != null) {
				btn.setPressed(true);
			}
		}

		if (event.getAction() == MotionEvent.ACTION_CANCEL) {
			handler.removeMessages(LONG_PRESS);
			lastButton = null;
			skipInput = false;
			setFlashText(null);
		}

		return true;
	}

	public void toggleKeymapMode()
	{
		keymapMode = !keymapMode;
		for (AdvButton btn: collectButtons()) {
			btn.keymapMode = keymapMode;
			btn.invalidate();
		}
	}

	public void setOpacityMode(int value)
	{
		if (value != opacityMode) {
			opacityMode = value;

			for (AdvButton btn: collectButtons()) {
				btn.invalidate();
			}
		}
	}

	public void changeOpacityMode()
	{
		if (opacityMode == 2) {
			setOpacityMode(0);
			return;
		}
		setOpacityMode((opacityMode+1)%2);
	}

	public void showCurrentPage()
	{
		if (page == 0) {
			createPage0();
		}
		else {
			createPage1();
		}
		computeButtonSize();
		mainView.requestLayout();
	}

	public void changePage()
	{
		shiftMode = 0;

		page = (page+1)%2;
		showCurrentPage();
	}

	public ArrayList<AdvButton> collectButtons()
	{
		ArrayList<AdvButton> lst = new ArrayList<>();

		for (int i = 0; i < rows.length; i++) {
			for (int j = 0; j < rows[i].getChildCount(); j++) {
				lst.add((AdvButton)rows[i].getChildAt(j));
			}
		}

		return lst;
	}

	public void setFlashText(AdvButton btn)
	{
		if (context.term != null) {
			String str = "";
			if (btn != null) {
				str = btn.getLabel();
			}
			context.term.setFlashText(str);
		}
	}

	public void setShiftMode(int mode)
	{
		if (mode != shiftMode) {
			shiftMode = mode;
			for (AdvButton btn: collectButtons()) {
				btn.setShiftMode(mode);
			}
		}
	}

	public void changeShiftMode()
	{
		setShiftMode((shiftMode+1)%3);
	}

	public static float calcPercentage(int fixedPart,
		int varPart, int modPct)
	{
		int p = Preferences.getKeyboardWidth();
		p = fixedPart + ((varPart * modPct) / 100);
		if (p > 100) p = 100;
		return p / 100f;
	}

	public void computeButtonSize()
	{
		if (vertical) {

			int cols = 5;

			float pctW = calcPercentage(20, 20,
				Preferences.getKeyboardWidth());

			btnWidth = (int)(winSize.x * pctW) / cols;
			btnWidth = Math.max(btnWidth, 30);

			float pctH = calcPercentage(50, 50,
				Preferences.getKeyboardHeight());

			btnHeight = (int)(winSize.y * pctH) / rows.length;
			btnHeight = Math.max(btnHeight, 16);
		}
		else {

			int cols = 10;

			float pctW = calcPercentage(50, 50,
				Preferences.getKeyboardWidth());

			btnWidth = (int)(winSize.x * pctW) / cols;
			btnWidth = Math.max(btnWidth, 30);

			float pctH = calcPercentage(30, 20,
				Preferences.getKeyboardHeight());

			btnHeight = (int)(winSize.y * pctH) / rows.length;
			btnHeight = Math.max(btnHeight, 16);
		}

        int fs = (int)Math.min(btnHeight * 0.75f,
        	btnWidth * 0.45f);
        fs = Math.max(fs, 10);
		fore.setTextSize(fs);
		foreBold.setTextSize(fs);
	}

	public void clearRows()
	{
		handler.removeMessages(LONG_PRESS);
		lastButton = null;
		setFlashText(null);
		skipInput = false;

		for (int i = 0; i < rows.length; i++) {
			rows[i].removeAllViews();
		}
	}

	public AdvButton createButton(LinearLayout row, String label)
	{
		AdvButton btn = new AdvButton(context, this, label);
		row.addView(btn);
		btn.keymapMode = keymapMode;

		KeyInfo info = keys.get(label);
		if (info != null) {
			btn.keymap = info.action;
			btn.alwaysVisible = info.alwaysVisible;
		}

		return btn;
	}

	public void populate(ArrayList<String> items, String str)
	{
		List<String> lst = Arrays.asList(str.split(""));
		for (String s: lst) {
			if (s.length() > 0) items.add(s);
		}
	}

	public void createPage0()
	{
		clearRows();

		ArrayList<String> list = new ArrayList<>();

		if (vertical) {
			populate(list, "1234567890");
			populate(list, "abcdefghij");
			populate(list, "klmnopqrst");
			populate(list, "uvwxyz");
			populate(list, InputUtils.Shift +
					InputUtils.BackSpace + "'*");

			list.add(InputUtils.Visibility);
			list.add(InputUtils.Menu);
			list.add("sym");
			list.add("run");
			list.add(".");
			list.add(InputUtils.Escape);
			list.add(InputUtils.Enter);
			list.add("kmp");
			list.add(" ");
			list.add(" ");
		}
		else {
			populate(list, "1234567890");
			populate(list, "qwertyuiop");
			populate(list, "asdfghjkl*");
			populate(list, InputUtils.Shift + "zxcvbnm'" +
					InputUtils.BackSpace);

			list.add(InputUtils.Visibility);
			list.add(InputUtils.Menu);
			list.add("sym");
			list.add("kmp");
			list.add("run");
			list.add(".");
			list.add(" ");
			list.add(" ");
			list.add(InputUtils.Escape);
			list.add(InputUtils.Enter);
		}

		int i = 0;
		for (String str: list) {
			if (i >= 50) break;
			int r = i / (50/rows.length);
			createButton(rows[r], str);
			++i;
		}
	}

	public void createPage1()
	{
		clearRows();

		ArrayList<String> list = new ArrayList<>();

		populate(list, "~!#$&<>|,=");
		populate(list, "/\\[](){}`^");
		populate(list, "@+-_:;\"?*");
		list.add("tab");

		for (int i = 1; i <= 10; i++) {
			list.add("F" + i);
		}

		if (vertical) {
			list.add(InputUtils.Visibility);
			list.add(InputUtils.Menu);
			list.add("sym");
			list.add("run");
			list.add(".");
			list.add(InputUtils.Escape);
			list.add(InputUtils.Enter);
			list.add("kmp");
			list.add(" ");
			list.add(" ");
		}
		else {
			list.add(InputUtils.Visibility);
			list.add(InputUtils.Menu);
			list.add("sym");
			list.add("kmp");
			list.add("run");
			list.add(".");
			list.add(" ");
			list.add(" ");
			list.add(InputUtils.Escape);
			list.add(InputUtils.Enter);
		}

		int i = 0;
		for (String str: list) {
			if (i >= 50) break;
			int r = i / (50/rows.length);
			createButton(rows[r], str);
			++i;
		}
	}

	public void reloadKeymaps()
	{
		String txt = Preferences.getActiveProfile()
			.getAdvButtonKeymaps();

		keys.clear();

		//Log.d("Angband", "Decoding: " + txt);

		for (String pair: txt.split(":sep:")) {
			String[] parts = pair.split(":prop:");
			if (parts.length > 2) {
				try {
					String trigger = parts[0];
					KeyInfo info = new KeyInfo();
					info.action = parts[1];
					info.alwaysVisible = (parts[2].equals("yes"));
					keys.put(trigger, info);
				}
				catch (Exception e) {}
				continue;
			}
		}
	}

	public void updateKey(String trigger,
		String action, boolean alwaysVisible)
	{
		KeyInfo info = new KeyInfo();
		info.action = action;
		info.alwaysVisible = alwaysVisible;
		keys.put(trigger, info);
	}

	public void persistKeymaps()
	{
		String txt = "";
		String sep = "";
		for (String trigger: keys.keySet()) {
			KeyInfo info = keys.get(trigger);
			txt += sep;
			txt += trigger;
			txt += ":prop:";
			txt += info.action;
			txt += ":prop:";
			txt += (info.alwaysVisible ? "yes": "no");
			sep = ":sep:";
		}
		//Log.d("Angband", "Encoding: " + txt);
		Preferences.getActiveProfile().setAdvButtonKeymaps(txt);
		Preferences.saveProfiles();
	}
}
