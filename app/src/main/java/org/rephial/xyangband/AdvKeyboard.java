package org.rephial.xyangband;

//import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.RectF;
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

import java.lang.reflect.Array;
import java.util.Collection;
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
	public static int AUTO_HIDE = 2;

	public static int TIMER_LONG_PRESS = 1000;
	public static int TIMER_AUTO_HIDE = 5000;

	public float PADDING = 0.1f;
	public int MAX_PAGES = 2;

	public int numRows = 5;
	public int numCols = 10;

	public static class KeyInfo
	{
		String trigger;
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

		if (vertical) {
			numRows = 10;
			numCols = 5;
		}

		rows = new LinearLayout[numRows];
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

	public int getPad()
	{
		int pad = Math.min(btnWidth, btnHeight);
		pad = (int)(pad * PADDING);
		return Math.max(pad, 2);
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

				if (msg.what == AUTO_HIDE && opacityMode != 2) {
					setOpacityMode(1);
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

		int pad = getPad();

		RectF bounds = new RectF(col * btnWidth + pad,
			row * btnHeight + pad,
			(col+1) * btnWidth - pad,
			(row+1) * btnHeight - pad);

		if (row >= 0 && col >= 0 &&
			row < rows.length && col < rows[row].getChildCount()) {

			AdvButton btn = (AdvButton)rows[row].getChildAt(col);

			if (btn.isVisible() && !bounds.contains(x, y)) {
				return null;
			}

			return btn;
		}

		return null;
	}

	public boolean onTouch(View v, MotionEvent event)
	{
		unpressAll();

		AdvButton btn = getButtonAt(event.getX(), event.getY());

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			handler.removeMessages(LONG_PRESS);
			handler.removeMessages(AUTO_HIDE);
			lastButton = btn;
			setFlashText(btn);
			skipInput = false;
			if (btn != null) {
				handler.sendEmptyMessageDelayed(LONG_PRESS, TIMER_LONG_PRESS);
				btn.setPressed(true);
			}
		}

		if (event.getAction() == MotionEvent.ACTION_UP) {
			handler.removeMessages(AUTO_HIDE);
			if (Preferences.getAutoHideKeys()) {
				handler.sendEmptyMessageDelayed(AUTO_HIDE, TIMER_AUTO_HIDE);
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
			handler.removeMessages(AUTO_HIDE);
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
		else if (page == 1) {
			createPage1();
		}
		/*
		else {
			createPage2();
		}
		*/
		computeButtonSize();
		mainView.requestLayout();
	}

	public void changePage()
	{
		shiftMode = 0;

		page = (page+1)%MAX_PAGES;

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
		/*
		if (context.term != null) {
			String str = "";
			if (btn != null) {
				str = btn.getLabel();
			}
			context.term.setFlashText(str);
		}
		*/
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
		int p = fixedPart + ((varPart * modPct) / 100);
		if (p > 100) p = 100;
		return p / 100f;
	}

	public void computeButtonSize()
	{
		float pctW = calcPercentage(0, 100,
			Preferences.getKeyboardWidth());

		if (vertical) {

			btnWidth = (int)(winSize.x * pctW) / numCols;
			btnWidth = Math.max(btnWidth, 30);

			float pctH = calcPercentage(50, 50,
				Preferences.getKeyboardHeight());

			btnHeight = (int)(winSize.y * pctH) / rows.length;
			btnHeight = Math.max(btnHeight, 16);
		}
		else {

			btnWidth = (int)(winSize.x * pctW) / numCols;
			btnWidth = Math.max(btnWidth, 30);

			float pctH = calcPercentage(30, 20,
				Preferences.getKeyboardHeight());

			btnHeight = (int)(winSize.y * pctH) / rows.length;
			btnHeight = Math.max(btnHeight, 16);
		}

		int fs = (int)Math.min(btnHeight * 0.65f,
			btnWidth * 0.40f);
		fs = Math.max(fs, 10);
		fore.setTextSize(fs);

		int fs2 = (int)Math.min(btnHeight * 0.65f,
			btnWidth * 0.5f);
		fs2 = Math.max(fs2, 10);
		foreBold.setTextSize(fs2);
	}

	public void clearRows()
	{
		handler.removeMessages(LONG_PRESS);
		//handler.removeMessages(AUTO_HIDE);
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
		}
		else {
			populate(list, "1234567890");
			populate(list, "qwertyuiop");
			populate(list, "asdfghjkl*");
			populate(list, InputUtils.Shift + "zxcvbnm'" +
					InputUtils.BackSpace);
		}

		finishPage(list);
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

		finishPage(list);
	}

	public void finishPage(ArrayList<String> list)
	{
		if (vertical) {
			list.add(InputUtils.Escape);
			list.add(InputUtils.Enter);
			list.add(".");
			list.add(" ");
			list.add(" ");
			list.add(InputUtils.Visibility);
			list.add(InputUtils.Menu);
			list.add(page == 0 ? "+/-": "abc");
			list.add("kmp");
			list.add("run");
		}
		else {

			int[] grp = {0,1};

			// For smaller keyboard, invert groups
			if (Preferences.getKeyboardWidth() < 90) {
				grp[0] = 1;
				grp[1] = 0;
			}

			for (int i = 0; i < 2; i++) {
				if (grp[i] == 0) {
					list.add(InputUtils.Visibility);
					list.add(InputUtils.Menu);
					list.add(page == 0 ? "+/-": "abc");
					list.add("kmp");
					list.add("run");
					list.add(".");
					list.add(" ");
					list.add(" ");
				}				
				else {
					list.add(InputUtils.Escape);
					list.add(InputUtils.Enter);
				}
			}
		}

		int i = 0;
		int max = numCols * numRows;
		for (String str: list) {
			if (i >= max) break;
			int r = i / (max/rows.length);
			createButton(rows[r], str);
			++i;
		}
	}

	/*
	public void createPage0()
	{
		clearRows();

		ArrayList<String> list = new ArrayList<>();

		populate(list, "qwertyuiop");
		populate(list, "asdfghjkl*");
		populate(list, InputUtils.Shift + "zxcvbnm'" +
			InputUtils.BackSpace);

		finishPage(list);
	}

	public void createPage1()
	{
		clearRows();

		ArrayList<String> list = new ArrayList<>();

		populate(list, "1234567890");
		populate(list, "<>wdgil'*" +
			InputUtils.BackSpace);

		for (int i = 1; i <= 10; i++) {
			list.add("F" + i);
		}

		finishPage(list);
	}

	public void createPage2()
	{
		clearRows();

		ArrayList<String> list = new ArrayList<>();

		populate(list, "~!#$&<>|,=");
		populate(list, "/\\[](){}`^");
		populate(list, "@+-_:;\"?*");
		list.add("tab");

		finishPage(list);
	}
	*/

	public static ArrayList<KeyInfo> parseKeymaps(String txt)
	{
		ArrayList<KeyInfo> arr = new ArrayList<>();

		for (String pair: txt.split(":sep:")) {
			String[] parts = pair.split(":prop:");
			if (parts.length > 2) {
				KeyInfo info = new KeyInfo();
				info.trigger = parts[0];
				info.action = parts[1];
				info.alwaysVisible = (parts[2].equals("yes"));
				if (info.action.length() == 0) continue;
				arr.add(info);
			}
		}

		return arr;
	}

	public static String mergeKeymaps(String target, String source)
	{
		ArrayList<KeyInfo> arrTarget = parseKeymaps(target);

		for (KeyInfo info: parseKeymaps(source)) {
			boolean found = false;
			for (KeyInfo info2: arrTarget) {
				if (info2.trigger.equals(info.trigger)) {
					found = true;
					break;
				}
			}
			if (!found) arrTarget.add(info);
		}

		return encodeKeymaps(arrTarget);
	}

	public static String encodeKeymaps(Collection<KeyInfo> keys)
	{
		String txt = "";
		String sep = "";
		for (KeyInfo info: keys) {
			if (info.action.length() == 0) continue;
			txt += sep;
			txt += info.trigger;
			txt += ":prop:";
			txt += info.action;
			txt += ":prop:";
			txt += (info.alwaysVisible ? "yes": "no");
			sep = ":sep:";
		}
		return txt;
	}

	public void reloadKeymaps()
	{
		String txt = Preferences.getActiveProfile()
			.getAdvButtonKeymaps();

		keys.clear();

		//Log.d("Angband", "Decoding: " + txt);

		for (KeyInfo info: parseKeymaps(txt)) {
			try {
				keys.put(info.trigger, info);
			}
			catch (Exception e) {
				Log.d("Angband", "reloadKeymaps: " + e.getMessage());
			}
		}
	}

	public void updateKey(String trigger,
		String action, boolean alwaysVisible)
	{
		KeyInfo info = new KeyInfo();
		info.trigger = trigger;
		info.action = action;
		info.alwaysVisible = alwaysVisible;
		keys.put(trigger, info);
	}

	public void persistKeymaps()
	{
		String txt = encodeKeymaps(keys.values());
		//Log.d("Angband", "Encoding: " + txt);
		Preferences.getActiveProfile().setAdvButtonKeymaps(txt);
		Preferences.saveProfiles();
	}
}
