package org.rephial.xyangband;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
//import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.util.Log;
//import android.util.TypedValue;
import android.view.Gravity;
//import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
//import android.view.View.OnClickListener;
import android.view.GestureDetector;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
//import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.MotionEvent;
//import android.widget.PopupMenu;
import android.widget.PopupWindow;
//import android.widget.ScrollView;
//import android.widget.TableLayout;
import android.view.ViewGroup.LayoutParams;
//import android.widget.TableRow;
//import android.widget.TextView;
//import android.widget.HorizontalScrollView;
//import android.os.Handler;

import androidx.core.graphics.ColorUtils;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

class MiniKbd extends View
{
	final public GameActivity context;
	public StateManager state;

	public int rows = 5;
	public int cols = 4;

	public int key_wid = 1;
	public int key_hgt = 1;

	public Key[][] keys;
	public Key helper;
	public String[] keyList;
	public int shiftMode = 0;

	public boolean locked = false;

	public Paint back;
	public Paint fore;
	public Paint foreBold;

	public long lastTime = 0;

	public class Key
	{
		public int x = 0;
		public int y = 0;
		public String text = "";
		public String pageText = "";
		public String label = "";
		public boolean selected = false;
		public boolean paging = false;
		public int page = 0;
	}

	public MiniKbd(GameActivity p_context)
	{
		super(p_context);

		context = p_context;
		state = context.state;

		back = new Paint();

		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		fore.setAntiAlias(true);
		fore.setTypeface(context.monoFont);

		foreBold = new Paint();
		foreBold.setTextAlign(Paint.Align.LEFT);
		foreBold.setAntiAlias(true);
		foreBold.setTypeface(context.monoBoldFont);

		createKeyList();

		//resetSize();

		keys = new Key[rows][cols];

		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				Key k = new Key();
				k.x = x;
				k.y = y;
				keys[y][x] = k;
			}
		}

		keys[0][0].text = "abc";
		keys[0][0].page = 1;

		keys[0][1].text = "mno";
		keys[0][1].page = 2;

		keys[0][2].text = "yz1";
		keys[0][2].page = 3;

		keys[1][0].text = ".*'";
		keys[1][0].page = 4;

		keys[1][1].text = ">={";
		keys[1][1].page = 5;

		keys[1][2].text = "@+-";
		keys[1][2].page = 6;

		keys[2][0].text = "F1";
		keys[2][0].page = 7;

		keys[4][0].text = InputUtils.Escape;
		keys[4][1].text = InputUtils.Enter;
		keys[4][2].text = InputUtils.Shift;
		keys[4][3].text = InputUtils.BackSpace;

		keys[2][3].text = "run";
		keys[3][2].text = "\\s";
		keys[3][3].text = "lck";

		helper = keys[0][3];

		resetKeys();
	}

	public void createKeyList()
	{
		String base = "abcdefghijklmnopqrstuvwxyz" +
			"0123456789.,*'?~!#$%&|<>^" +
			"/\\=()[]{}@+-_:;\"";
		keyList = new String[base.length()+5+12];
		int i = 0;
		for (String s: base.split("")) {
			if (s.length() > 0) keyList[i++] = s;
		}
		keyList[i++] = "tab";
		for (int j = 1; j <= 4; j++) {
			keyList[i++] = "";
		}
		for (int j = 1; j <= 12; j++) {
			keyList[i++] = "F" + j;
		}
	}

	public void resetSize()
	{
		int min = 20;
		int max = 60;

		Point winSize = GxUtils.getWinSize(context);

		int pctH = min + Preferences.getKeyboardHeight() * (max-min) / 100;
		key_hgt = (winSize.y * pctH / 100) / rows;
		key_wid = (int)(key_hgt * 1.5f);

		int fs = (int)Math.min(key_hgt * 0.65f, key_wid * 0.40f);
		fs = Math.max(fs, 10);
		fore.setTextSize(fs);

		int fs2 = (int)Math.min(key_hgt * 0.65f, key_wid * 0.5f);
		fs2 = Math.max(fs2, 10);
		foreBold.setTextSize(fs2);
	}

	public void resetKeys()
	{
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				String txt = keys[y][x].text;
				keys[y][x].pageText = txt;
				keys[y][x].label = txt;
				keys[y][x].paging = false;
			}
		}
		resetSelection();
		lastTime = 0;
		setShiftMode(shiftMode);
	}

	public void resetSelection()
	{
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				keys[y][x].selected = false;
			}
		}
		helper.label = "";
	}

	public void select(Key key)
	{
		resetSelection();
		key.selected = true;
		helper.label = key.label;
	}

	public void setShiftMode(int mode)
	{
		shiftMode = mode;

		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				Key key = keys[y][x];

				String txt = key.pageText;

				if (key.paging || (key.page > 0 && key.page <= 3)) {
					txt = computeShiftMode(txt);
				}

				key.label = txt;
			}
		}
	}

	public String computeShiftMode(String txt)
	{
		if (txt.length() == 0) return txt;

		char chr = txt.charAt(0);

		if (Character.isAlphabetic(chr)) {
			if (shiftMode == 0) {
				return txt;
			}
			if (shiftMode == 1) {
				return txt.toUpperCase();
			}
			if (shiftMode == 2) {
				return "^"+txt.toUpperCase();
			}
		}

		return txt;
	}

	public void configure(String action, boolean visible)
	{
	}

	public int calculateAlphaBg()
	{
		//int min = 10;
		int max = 200;

		if (state.opaqueWidgets) {
			return max;
		}

		// Normal
		return 80;
	}

	public int calculateAlphaFg()
	{
		int min = TermView.MIN_OPACITY;
		int max = 255;

		if (state.opaqueWidgets) {
			return max;
		}

		int p = Preferences.getKeyboardOpacity();
		p = min + p * (max-min) / 100;

		return Math.max(p, min);
	}

	public void setBgColor(Paint p, Key key)
	{
		if (key.selected) {
			p.setColor(AdvButton.TOGGLED_BG);
		}
		else {
			p.setColor(AdvButton.DEFAULT_BG);
		}
	}

	public void setFgColor(Paint p, Key key)
	{
		if (key == helper) {
			p.setColor(AdvButton.TOGGLED_BG);
		}
		else if (shiftMode > 0 && key.label.equals(InputUtils.Shift)) {
			p.setColor(AdvButton.TOGGLED_BG);
		}
		else if (key.label.equals("run") && state.getRunningMode()) {
			p.setColor(AdvButton.TOGGLED_BG);
		}
		else if (locked && key.label.equals("lck")) {
			p.setColor(AdvButton.TOGGLED_BG);
		}
		else {
			p.setColor(Color.WHITE);
		}
	}

	protected void drawKey(Canvas canvas, Key key)
	{
		int x = key.x * key_wid;
		int y = key.y * key_hgt;

		Rect bounds = new Rect(x, y, x + key_wid, y + key_hgt);

		setBgColor(back, key);
		back.setAlpha(calculateAlphaBg());
		canvas.drawRect(bounds, back);

		String label = key.label;
		if (label.length() == 0) return;

		Paint pfore = fore;

		if (label.length() > 3) label = label.substring(0, 3);
		if (label.length() == 1) pfore = foreBold;

		//back.setColor(0xAAAAAA);
		//back.setAlpha(calculateAlphaBg());
		//canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, back);

		int tw = key_wid;
		int th = key_hgt;

		float w2 = pfore.measureText(label);
		float padx = Math.max((tw - w2) / 2, 0);
		float h2 = pfore.descent() - pfore.ascent();
		float pady = Math.max((th - h2) / 2, 0)	+ pfore.descent();

		setFgColor(pfore, key);
		pfore.setShadowLayer(10f, 0, 0, Color.CYAN);
		pfore.setAlpha(calculateAlphaFg());

		canvas.drawText(label,
			bounds.left + padx,
			bounds.top + th - pady,
			pfore);
	}

	public Key keyFromPosition(float px, float py)
	{
		//Log.d("Angband", "px " + px + " - py " + py);

		if (px >= cols*key_wid || py < 0) {
			return null;
		}
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				float right = x * key_wid + key_wid;
				float bottom = y * key_hgt + key_hgt;
				if (px < right && py < bottom) return keys[y][x];
			}
		}
		return null;
	}

	protected void onDraw(Canvas canvas)
	{
		Rect bounds = new Rect();

		getDrawingRect(bounds);

		back.setColor(Color.BLACK);
		back.setAlpha(10);
		canvas.drawRect(bounds, back);

		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				drawKey(canvas, keys[y][x]);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec,
		int heightMeasureSpec)
	{
		resetSize();
		int w = key_wid * cols;
		int h = key_hgt * rows;
		setMeasuredDimension(w, h);
	}

	public void showPage(int page)
	{
		int from = page*12;
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 3; x++) {
				String txt = "";
				if (from < keyList.length) {
					txt = keyList[from];
				}
				++from;
				keys[y][x].pageText = txt;
				keys[y][x].label = computeShiftMode(txt);
				keys[y][x].paging = true;
			}
		}
	}

	public boolean execute(Key key)
	{
		String label = key.label;

		if (label.length() == 0) return true;

		if (label.equals(InputUtils.Shift)) {
			setShiftMode((shiftMode+1)%3);
			resetSelection();
			return false;
		}

		if (label.equals("lck")) {
			locked = !locked;
			if (!locked) setShiftMode(0);
			resetSelection();
			return false;
		}

		if (label.equals("run")) {
			state.setRunningMode(!state.getRunningMode());
			resetSelection();
			return false;
		}

		char ch = InputUtils.codeFromName(label);

		if (ch != 0) {
			state.addKey(ch);
		}
		else {
			InputUtils.processAction(state, label);
		}

		if (key.paging && key.pageText.length() == 1
			&& Character.isAlphabetic(key.pageText.charAt(0))
			&& !locked) {
			setShiftMode(0);
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		Key key = keyFromPosition(event.getX(), event.getY());

		if (key == null || (event.getAction() == MotionEvent.ACTION_CANCEL)) {
			resetKeys();
			invalidate();
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (key.page > 0 && !key.paging) {
				showPage(key.page-1);
				// Wait a short moment to process the up event
				// (to see the key list)
				lastTime = System.currentTimeMillis();
			}
			select(key);
			invalidate();
		}

		if (event.getAction() == MotionEvent.ACTION_UP) {

			// Discard the up event after showing a page?
			if (key.page > 0 &&
				System.currentTimeMillis() - lastTime < 250) {
				return true;
			}

			if (execute(key)) {
				if (locked) {
					resetSelection();
				}
				else {
					resetKeys();
				}
			}

			invalidate();
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (key.page > 0 && !key.paging) {
				showPage(key.page-1);
			}
			select(key);
			invalidate();
		}

		return true;
	}
}
