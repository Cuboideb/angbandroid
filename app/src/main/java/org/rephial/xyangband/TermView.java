/*
 * File: TermView.java
 * Purpose: Terminal-base view for Android application
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Base64;

public class TermView extends View implements OnGestureListener {

	Typeface tfStd;
	Typeface tfTiny;
	Bitmap bitmap;
	Canvas canvas;
	Paint fore;
	Paint fore_subw;
	Paint fore_topbar;
	Paint fore_fab;
	Paint back;
	Paint cursor;
	Paint dirZoneFill;
	Paint dirZoneStroke;
	Display fullDisplay;
	Display windowDisplay;

	Bitmap atlas = null;
	//BitmapRegionDecoder tiles = null;

	private int color1 = Color.parseColor("#4a4855");
	private int color2 = Color.parseColor("#807c93");

	private String[][] dPadColors = {
		{"Gray", "#807c93"},
		{"Darker Gray", "#4a4855"},
		{"Red", "red"},
		{"Blue", "#118ab2"},
		{"Yellow", "#ffff3f"},
		{"Violet", "#f20089"},
		{"Green", "#0ead69"}
	};
	private int color3 = Color.parseColor("#4d7ed6");
	private int color_drag = Color.parseColor("#B2FB49");
	private int alpha = 70;

	private int color1_stroke = Color.parseColor("#DDDDDD");
	private int color2_stroke = Color.parseColor("#4d7ed6");
	private int alpha_stroke = 200;

	Handler timerHandler;
	private FloatingView lastView = null;
	private int currentMode = 0;
	private Point lastLocation = null;
	private long lastRefresh = 0;
	private Point dragOffset = new Point(0, 0);

	public int idPool = 0;
	public ArrayList<FloatingView> views = new ArrayList<>();
	public static int SCREEN_VIEW = 1;

	private static int longRepeatDelay = 350;
	private static int shortRepeatDelay = 80;
	private static int dragDelay = 250;
	private static int longPressDelay = 700;
	private static int longPressDelay2 = 1000;
	private long savedTime = 0;

	public static int LONG_PRESS = 1;
	public static int DRAGGING = 2;
	public static int REPEAT_DIR = 3;
	public static int FLASH_TEXT = 4;

	public static int MIN_OPACITY = 10;

	private int lastEvent = -1000;
	private int curEvent = -1000;

	//	int row = 0;
	//  int col = 0;

	public int cols = 0;
	public int rows = 0;

	public int canvas_width = 0;
	public int canvas_height = 0;

	public int char_height = 0;
	public int char_width = 0;
	public int font_text_size = 0;

	public static char BIG_PAD = 0x1E00;

	public static int PLAYER_MASK = (0x20 << 10);
	public static int MONSTER_MASK = (0x10 << 10);

	public static int MIN_FONT = 6;
	public static int MAX_FONT = 64;

	public int tile_wid = 1;
	public int tile_hgt = 1;
	public int useGraphics = 0;

	public int tile_wid_pix = 0;
	public int tile_hgt_pix = 0;
	public Paint tile_fore;

	public boolean mouseSpecial = false;
	public RectF mouseToggle = null;
	public Bitmap iconMouse1 = null;
	public Bitmap iconMouse2 = null;

	public boolean topBar = true;
	public String flashText = "";

	public String pluginName = "";

	public static class Assert {
		public static void that(boolean condition, String message) {
			if (!condition) {
				throw new AssertionError(message);
			}
		}
	}

	public GraphicsMode currentGraf = null;
	public String currentAtlas = "";

	private Vibrator vibrator;
	private boolean vibrate;
	private Handler handler = null;
	private StateManager state = null;
	private GameActivity game_context;

	private GestureDetector gesture;

	public HashMap<String,Number> alphaTiles = new HashMap<>();

	public TermView(Context context) {
		super(context);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
		initTermView(context);
	}

	public TermView(Context context, AttributeSet attrs) {
		super(context, attrs);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
		initTermView(context);
	}

	protected void initTermView(Context context) {
		game_context = (GameActivity)context;

		// Add SCREEN_VIEW
		lastView = new ScreenView();
		views.add(lastView);

		// The display of the device
		WindowManager wm = (WindowManager)game_context.getSystemService(Context.WINDOW_SERVICE);
		fullDisplay = wm.getDefaultDisplay();

		// Just the activity window
		WindowManager wm2 = game_context.getWindowManager();
		windowDisplay = wm2.getDefaultDisplay();

		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) fore.setAntiAlias(true);
		setForeColor(Color.WHITE);

		tile_fore = new Paint();
		tile_fore.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) tile_fore.setAntiAlias(true);
		tile_fore.setColor(Color.WHITE);

		fore_subw = new Paint();
		fore_subw.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) fore_subw.setAntiAlias(true);
		fore_subw.setColor(Color.WHITE);

		fore_fab = new Paint();
		fore_fab.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) fore_fab.setAntiAlias(true);
		fore_fab.setColor(Color.WHITE);

		fore_topbar = new Paint();
		fore_topbar.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) fore_topbar.setAntiAlias(true);
		fore_topbar.setColor(Color.WHITE);

		back = new Paint();
		setBackColor(Color.BLACK);

		cursor = new Paint();
		cursor.setColor(Color.GREEN);
		cursor.setStyle(Paint.Style.STROKE);
		cursor.setStrokeWidth(0);

		dirZoneFill = new Paint();
		dirZoneFill.setColor(color1);
		dirZoneFill.setStyle(Paint.Style.FILL);
		dirZoneFill.setAlpha(alpha);

		dirZoneStroke = new Paint();
		dirZoneStroke.setColor(color1_stroke);
		dirZoneStroke.setStyle(Paint.Style.STROKE);
		dirZoneStroke.setAlpha(alpha);
		dirZoneStroke.setStrokeWidth(2);
		dirZoneStroke.setStrokeCap(Paint.Cap.ROUND);

		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		setFocusableInTouchMode(true);

		gesture = new GestureDetector(context, this);

		String[] offset = Preferences.getTouchDragOffset().split("x");
		if (offset.length == 2) {
			dragOffset.x = Integer.valueOf(offset[0]);
			dragOffset.y = Integer.valueOf(offset[1]);
		}

		this.cols = Preferences.getTermWidth();
		this.rows = Preferences.getTermHeight();

		tile_hgt = Preferences.getTileHeight();
		tile_wid = Preferences.getTileWidth();
		topBar = Preferences.getTopBar();

		iconMouse1 = BitmapFactory.decodeResource(game_context.getResources(),
				R.drawable.mouse1);
		iconMouse2 = BitmapFactory.decodeResource(game_context.getResources(),
				R.drawable.mouse2);

		pluginName = Preferences.getActivePluginName();

		createTimers();

		reloadGraphics();

		resetFloatingButtons();

		//Log.d("Angband", "Init TERM");

		//state.addKey(' ');
	}

	public void resetFloatingButtons()
	{
		Iterator<FloatingView> iter = views.iterator();

		while (iter.hasNext()) {
			FloatingView v = iter.next();

			if (v instanceof ButtonView) iter.remove();
		}

		if (deserializeButtons() > 0) return;
	}

	public void resetDragOffset()
	{
		Preferences.setTouchDragOffset(0, 0);
		dragOffset.x = 0;
		dragOffset.y = 0;
		updateAllOffsets();
		invalidate();
	}

	public boolean canDraw() {

		int tw = (char_width * tile_wid);
		int th = (char_height * tile_hgt);
		return (tw > 0 && th > 0);
	}

	public int ROW_MAP()
	{
		return 1;
	}

	public boolean topBarEnabled()
	{
		return Preferences.getTopBar() &&
			Preferences.getActivePlugin().canUseTopBar();
	}

	public int COL_MAP()
	{
		return topBarEnabled() ? 0: 13;
	}

	public void createTimers()
	{
		timerHandler = new Handler(Looper.myLooper()) {
			public void handleMessage(Message msg)
			{
				// Clear flash text
				if (msg.what == FLASH_TEXT) {
					Log.d("Angband", "Timeout!");
					flashText = "";
					invalidate();
					return;
				}

				long currTime = System.currentTimeMillis();
				long delta = 0;
				if (savedTime > 0 && currTime > savedTime) {
					delta = currTime - savedTime;
				}
				savedTime = currTime;

				if (msg.what == LONG_PRESS) {
					currentMode = LONG_PRESS;
					lastView.executeLongPress(null);
				}

				if (msg.what == DRAGGING) {
					currentMode = DRAGGING;
					invalidate();
				}

				if (msg.what == REPEAT_DIR) {

					// Keep repeating

					// Set bounds
					delta = Math.min(delta, longRepeatDelay);
					int effectiveDelay = (int)Math.max(delta, shortRepeatDelay);

					//Log.d("Angband", "Delay: " + effectiveDelay);

					// Paranoia
					if (lastView.repeatEnabled()) {
						timerHandler.sendEmptyMessageDelayed(REPEAT_DIR, effectiveDelay);
					}

					lastView.executeAction(null);
				}
			}
		};
	}

	public boolean reloadGraphics()
	{
		boolean refresh = false;
		int gidx = Preferences.getGraphicsMode();
		if (gidx > 0 && gidx == useGraphics && currentGraf == null
			&& !state.grafmodes.isEmpty()) {
			refresh = true;
		}
		setGraphicsMode(gidx);
		return refresh;
	}

	public void unloadTiles()
	{
		if (useGraphics != Preferences.getGraphicsMode() &&
			atlas != null) {
			atlas.recycle();
			atlas = null;
		}

		//tileCache.evictAll();
	}

	public void clearFull()
	{
		//Log.d("Angband", "Clear Full");
		state.virtscr.clear();
		state.stdscr.clear();
		this.clear();
	}

	public String serializeVisualState()
	{
		String txt = "graphics:" + useGraphics +
			":" + tile_hgt + ":" + tile_wid +
			":" + (topBar ? 1: 0);

		Log.d("Angband", "Visual State: " + txt);

		return txt;
	}

	public void sendVisuals(String oldVisuals)
	{
		if (!state.gameThread.gameRunning()) return;

		String newVisuals = serializeVisualState();

		if (!newVisuals.equals(oldVisuals)) {

			//Log.d("Angband", "Old visuals: " + oldVisuals);

			//Log.d("Angband", "Visuals to Core: " + newVisuals);

			state.addSpecialCommand(newVisuals);

			if (state.characterPlaying()) {

				clearFull();
				state.addKey(' ');
			}
		}
	}

	public void setGraphicsMode(int gidx)
	{
		if (state.grafmodes.isEmpty()) {
			useGraphics = gidx;
			Log.d("Angband", "No graphics modes");
			return;
		}

		GraphicsMode gm = state.findGraphicsMode(gidx);
		Bitmap image = null;
		String path = "";

		if (gm != null && !gm.havePages()) {

			path = gm.getFullPath(null);

			if (path.equals(currentAtlas) && atlas != null) {
				image = atlas;
			}
			else {
				image = BitmapFactory.decodeFile(path);
				if (image == null) {
					Log.d("Angband", "Can't load " + path);
				}
				else {
					image.prepareToDraw();
				}
			}
		}

		useGraphics = gidx;
		currentGraf = gm;
		atlas = image;
		currentAtlas = path;

		if (useGraphics > 0 && currentGraf == null) {
			Log.d("Angband","No currentGraf");
		}
	}

	public boolean bigTileActive()
	{
		return (tile_wid > 1) || (tile_hgt > 1);
	}

	public void configureVisuals(int rows, int cols, int graf)
	{
		//Log.d("Angband", "Configure visuals! REACT");
		tile_hgt = rows;
		tile_wid = cols;
		setGraphicsMode(graf);
	}

	Point mouseToGrid(int y, int x)
	{
		Point p = new Point();
		x += getScrollX();
		y += getScrollY();
		y -= getTopBarHeight();
		p.x = (char_width > 0) ? (x / char_width): 0;
		p.y = (char_height > 0 && y > 0) ? (y / char_height): 0;

		/*
		if (state.stdscr != null) {
			TermWindow.TermPoint u = state.stdscr.buffer[p.y][p.x];
			game_context.infoAlert("Cell: " +
					y + "x" + x + " - " +
					p.y + "x" + p.x + " - " +
					u.fgColor + " " + (int)u.ch + " " +
					u.bgColor + " " + (int)u.bgChar);
		}
		*/

		return p;
	}

	public void stopTimers()
	{
		timerHandler.removeMessages(REPEAT_DIR);
		timerHandler.removeMessages(LONG_PRESS);
		timerHandler.removeMessages(DRAGGING);

		currentMode = 0;
		lastLocation = null;
		lastRefresh = 0;

		lastView = getView(SCREEN_VIEW);
		savedTime = 0;

		invalidate();
	}

	public boolean largeCanvas()
	{
		return canvas_width > getWidth() || canvas_height > getHeight();
	}

	public DPadView getViewByDirection(int direction)
	{
		for (FloatingView v: views) {
			if (v instanceof DPadView) {
				DPadView temp = (DPadView)v;
				if (temp.direction == direction) {
					return temp;
				}
			}
		}
		return null;
	}

	protected void drawDPadFull(Canvas p_canvas)
	{
		float pctw[] = {0.2f, 0.6f, 0.2f};
		float pcth[] = {0.33f, 0.33f, 0.34f};
		int totalw = getWidth() - game_context.getKeyboardWidth();
		int totalh = getHeight() - game_context.getKeyboardHeight();

		for (int i = 0; i < 3; i++) {
			pctw[i] *= totalw;
			pcth[i] *= totalh;
		}

		int x0 = getScrollX() + game_context.getKeyboardWidth();
		int y0 = getScrollY();

		int y = y0;

		for (int py = 0; py < 3; py++) {

			int x = x0;

			for (int px = 0; px < 3; px++) {

				int direction = (2 - py) * 3 + px + '1';

				//Log.d("Angband", "direction: " + (char)direction);

				DPadView dw = getViewByDirection(direction);

				// Insert ?
				if (dw == null) {
					dw = new DPadView();
					dw.direction = direction;
					dw.margin = 0;
					views.add(dw);
				}

				// Update
				dw.x = x;
				dw.y = y;
				dw.w = pctw[px];
				dw.h = pcth[py];

				x += pctw[px];
			}

			y += pcth[py];
		}

		dirZoneStroke.setColor(color1_stroke);
		dirZoneStroke.setAlpha(alpha);

		p_canvas.drawLine(x0, y0 + pcth[0],
			x0 + totalw - 1, y0 + pcth[0],
			dirZoneStroke);

		p_canvas.drawLine(x0, y0 + pcth[0] +pcth[1],
			x0 + totalw - 1, y0 + pcth[0] + pcth[1],
			dirZoneStroke);

		p_canvas.drawLine(x0, y0 + totalh - 1,
			x0 + totalw - 1, y0 + totalh - 1,
			dirZoneStroke);

		p_canvas.drawLine(x0, y0, x0, y0 + totalh - 1,
			dirZoneStroke);

		p_canvas.drawLine(x0 + pctw[0], y0,
			x0 + pctw[0], y0 + totalh - 1,
			dirZoneStroke);

		p_canvas.drawLine(x0 + pctw[0] + pctw[1], y0,
			x0 + pctw[0] + pctw[1], y0 + totalh - 1,
			dirZoneStroke);

		if (getViewByDirection('5').isBeingDragged() && largeCanvas()) {
			dirZoneFill.setColor(color_drag);
			dirZoneFill.setAlpha(alpha);
			float cx = x0 + pctw[0];
			float cy = y0 + pcth[0];
			RectF re = new RectF(cx, cy, cx + pctw[1], cy + pcth[1]);
			p_canvas.drawRect(re, dirZoneFill);
		}
	}

	private int lookupColor(int colorIdx)
	{
		String name = Preferences.getDPadColor(colorIdx);

		//Log.d("Angband", "Color: " + name);

		for (int i = 0; i < dPadColors.length; i++) {
			if (dPadColors[i][0].equals(name)) {
				return Color.parseColor(dPadColors[i][1]);
			}
		}

		return Color.parseColor("red");
	}

	public void updateAllOffsets()
	{
		for (FloatingView v: views) {
			if (v instanceof DPadView) {
				DPadView dw = (DPadView)v;
				dw.updateOffset();
			}
		}
	}

	public void createDPadRight()
	{
		// Already created
		if (getViewByDirection('5') != null) return;

		// Get the height of the activity window
		Point winSize = new Point();
		windowDisplay.getSize(winSize);
		int w = (int)(winSize.x * 0.10f);
		int h = (int)(winSize.y * 0.10f);

		int totalw = getWidth();
		int totalh = getHeight() - getVerticalGap();

		int padx = (int)(totalw * 0.01f);
		int pady = (int)(totalh * 0.01f);

		int margin = Math.max(padx, pady);

		w = Math.min(w, h);
		h = w = ((100 + Preferences.getTouchMultiplier()) * w) / 100;

		for (int py = 1; py <= 3; py++) {
			for (int px = 1; px <= 3; px++) {

				int x = totalw - w * (3 - px + 1);

				int y = totalh - h * (3 - py + 1);

				int direction = (3 - py) * 3 + px + '0';

				DPadView dw = new DPadView();
				dw.ox = x;
				dw.oy = y;
				dw.w = w;
				dw.h = h;
				dw.direction = direction;
				dw.margin = margin;
				dw.updateOffset();

				views.add(dw);
			}
		}
	}

	protected void drawDPadRight(Canvas p_canvas)
	{
		createDPadRight();

		int _col1 = lookupColor(1);
		int _col2 = lookupColor(2);

		for (FloatingView v: views) {
			if (!(v instanceof DPadView)) continue;

			DPadView dw = (DPadView)v;

			float x = dw.x + dw.margin / 2 + getScrollX();
			float y = dw.y + dw.margin / 2 + getScrollY();
			float w = Math.max(dw.w - dw.margin, 2);
			float h = Math.max(dw.h - dw.margin, 2);

			RectF r = new RectF(x, y, x + w - 1, y + h - 1);

			dirZoneStroke.setColor(color1_stroke);
			dirZoneStroke.setAlpha(alpha_stroke);

			if (dw.isBeingDragged()) {
				dirZoneFill.setColor(color_drag);
			}
			else if ("24568".indexOf((char)dw.direction) != -1) {
				dirZoneFill.setColor(_col2);
			}
			else {
				dirZoneFill.setColor(_col1);
			}
			dirZoneFill.setAlpha(alpha);

			p_canvas.drawRoundRect(r, 10, 10, dirZoneFill);
			p_canvas.drawRoundRect(r, 10, 10, dirZoneStroke);
		}

		// Restore
		dirZoneFill.setColor(color1);
		dirZoneFill.setAlpha(alpha);
	}

	public static TSize getCharDimensions(Paint p)
	{
		int h = (int)Math.ceil(p.getFontSpacing());
		int w = (int)p.measureText("X", 0, 1);
		return new TSize(w, h);
	}

	public int calculateColumns()
	{
		int pct = Preferences.getColumnsSubWindows();
		if (pct == 0) {
			return 0;
		}
		int columns = pct * Preferences.cols / 100;
		return columns;
	}

	public int calculateRows()
	{
		int pct = Preferences.getRowsSubWindows();
		if (pct == 0) {
			return 0;
		}
		int rows = pct * Preferences.rows / 100;
		return rows;
	}

	public int countSubWindowRows(TermWindow w)
	{
		int columns = calculateColumns();
		if (columns == 0) {
			return 0;
		}
		int count = 0;
		int lastRow = -1;

		for(int r = 0; r < w.rows; r++) {
			for(int c = 0; c < w.cols; c++) {

				TermWindow.TermPoint p = w.buffer[r][c];

				// Ignore graphics
				if (p.isGraphicTile() || p.isBigPad()) continue;

				char ch = p.ch;

				if (c >= columns) continue;

				if (ch == ' ') continue;

				if (r != lastRow) {
					lastRow = r;
					++count;
				}
			}
		}

		return count;
	}

	public void drawSubWindow(Canvas p_canvas, TermWindow w,
		int startX, int startY, int maxWidth, int maxHeight, Paint paint,
		boolean reverse)
	{
		TSize s = getCharDimensions(paint);
		int th = s.height;
		int tw = s.width;

		int currentRow = -1;
		int lastRow = -1;

		for(int r = 0; r < w.rows; r++) {

			// Draw lines in reverse order?
			int r2 = reverse ? (w.rows-1-r): r;

			for(int c = 0; c < w.cols; c++) {

				TermWindow.TermPoint p = w.buffer[r2][c];

				// Ignore graphics
				if (p.isGraphicTile() || p.isBigPad()) continue;

				char ch = p.ch;

				if (ch == ' ') continue;

				int x = startX + c * tw;

				if (x + tw > maxWidth) continue;

				if (r != lastRow) {
					lastRow = r;
					++currentRow;
				}

				int y = startY + currentRow * th;

				if (y + th > maxHeight) return;

				String str = ch + "";

				int color = Color.WHITE;
				TermWindow.ColorPair pair = TermWindow.pairs.get(p.fgColor);
				if (pair != null) {
					color = pair.fColor;
				}

				paint.setColor(color);
				paint.setAlpha(255);

				int w2 = (int)paint.measureText(str, 0, 1);
				int pad = Math.max((tw - w2) / 2, 0);

				p_canvas.drawText(str,
					x + pad,
					y + th - paint.descent(),
					paint);
			}
		}
	}

	public void drawAllSubWindows(Canvas p_canvas)
	{
		if (!state.characterPlaying()
			|| !Preferences.getActivePlugin().enableSubWindows()
			|| Preferences.getNumberSubWindows() == 0) return;

		int columns = calculateColumns();
		if (columns == 0) return;

		TSize s = getCharDimensions(fore_subw);
		int th = s.height;
		int tw = s.width;

		int subWinWidth = columns*tw;
		int subWinHeight = getSubWindowsHeight();

		int startX = canvas_width - subWinWidth;
		int startY = 0;
		int maxWidth = canvas_width;
		int maxHeight = canvas_height;

		if (Preferences.lockSubWindowsOnScroll()) {
			startX = getScrollX() + getWidth() - subWinWidth;
			startY = getScrollY();
			maxWidth = startX + subWinWidth;
			maxHeight = startY + getHeight();
		}

		Rect re = new Rect(startX, startY, maxWidth, maxHeight);

		boolean vertical = !Preferences.getHorizontalSubWindows();

		if (!vertical) {
			if (subWinHeight <= 0) return;
			startX = 0;
			startY = maxHeight - subWinHeight;

			if (Preferences.lockSubWindowsOnScroll()) {
				startX = getScrollX();
				startY = getScrollY() + getHeight() - subWinHeight;
				maxWidth = startX + getWidth();
				maxHeight = startY + subWinHeight;
			}

			re = new Rect(startX, startY, maxWidth, maxHeight);
		}

		// Clear all
		fore_subw.setColor(Color.BLACK);
		p_canvas.drawRect(re, fore_subw);

		int msgWin = state.getMsgSubWindow();

		// One window is reserved
		for (int i = 1; i < state.MAX_WINDOWS-1; i++) {
			int idx = i;
			TermWindow w = state.getWin(idx);

			boolean reverse = (i == msgWin);

			if (w != null && state.windowIsVisible(w)) {

				int rows = countSubWindowRows(w);

				if (rows > 0) {
					if (vertical) {
						drawSubWindow(p_canvas, w, startX, startY,
							maxWidth, maxHeight, fore_subw, reverse);
						startY += ((rows+1)*th); // Plus one row
					}
					else {
						startY = re.top;
						drawSubWindow(p_canvas, w, startX, startY,
							startX+subWinWidth, maxHeight, fore_subw,
							reverse);
						startX += ((columns+1)*tw); // Plus one col
					}
				}
			}
		}

		dirZoneStroke.setColor(color1_stroke);
		dirZoneStroke.setAlpha(alpha);

		if (!useFullDPad()) {
			if (vertical) {
				p_canvas.drawLine(re.left, re.top, re.left, re.bottom-1,
					dirZoneStroke);
			}
			else {
				p_canvas.drawLine(re.left, re.top, re.right-1, re.top,
					dirZoneStroke);
			}
		}
	}

	public TSize calculateButtonSize()
	{
		Point winSize = new Point();
		windowDisplay.getSize(winSize);

		int w = (int)(getWidth() * 0.07f);
		// Get the height of the activity window
		int h = (int)(winSize.y * 0.07f);

		w = Math.min(w, h);
		h = w = ((100 + Preferences.getTouchMultiplier()) * w) / 100;
		return new TSize(w, h);
	}

	public void drawMouseToggle(Canvas p_canvas)
	{
		Bitmap icon = mouseSpecial ? iconMouse2: iconMouse1;

		if (icon == null || !Preferences.getShowMouseIcon() ||
			useFullDPad()) {

			mouseSpecial = false;
			mouseToggle = null;
			return;
		}

		Point winSize = new Point();
		windowDisplay.getSize(winSize);

		int x = (int)(getWidth() * 0.6f);
		int y = 15;
		//TSize size = calculateButtonSize();

		int size = (int)(Math.min(winSize.x, winSize.y) * 0.1f);
		size = Math.min(icon.getWidth(), size);

		//size = icon.getWidth();

		mouseToggle = new RectF(x, y, x+size, y+size);

		x += getScrollX();
		y += getScrollY();

		RectF r = new RectF(x, y, x+size, y+size);

		p_canvas.drawBitmap(icon, null, r, null);
	}

	public void drawTopBar(Canvas p_canvas)
	{
		int maxHeight = getTopBarHeight();

		if (maxHeight == 0) return;

		int x = 0;
		int y = 0;
		int w = canvas_width;
		int h = maxHeight;

		if (Preferences.lockSubWindowsOnScroll()) {
			x = getScrollX();
			y = getScrollY();
			w = x + getWidth();
			h = y + maxHeight;
		}

		Rect re = new Rect(x, y, w, h);

		// Clear all
		fore_topbar.setColor(Color.BLACK);
		p_canvas.drawRect(re, fore_topbar);

		TermWindow win = state.getWin(StateManager.TOP_BAR_WIN);

		if (win == null) return;

		drawSubWindow(p_canvas, win, x, y, w, h, fore_topbar, false);
	}

	public void setFlashText(String value)
	{
		if (!value.equals(flashText)) {
			flashText = value;
			invalidate();
		}
	}

	public void flashAndClear(String value, int timeout)
	{
		if (!value.equals(flashText)) {
			timerHandler.removeMessages(FLASH_TEXT);
			timerHandler.sendEmptyMessageDelayed(FLASH_TEXT, timeout);
			flashText = value;
			invalidate();
		}
	}

	public void drawFlashText(Canvas p_canvas)
	{
		Paint paint = fore_topbar;

		if (flashText.length() == 0 || paint == null) return;

		float old = paint.getTextSize();

		float new_size = Math.min(old * 4, MAX_FONT);
		new_size = Math.max(new_size, MIN_FONT);

		paint.setTextSize(new_size);
		paint.setColor(AdvButton.TOGGLED_BG);
		paint.setShadowLayer(10f, 0, 0, Color.CYAN);
		paint.setAlpha(200);

		TSize s = getCharDimensions(paint);
		int th = s.height;
		int tw = s.width;

		String str = flashText;

		int w2 = (int)paint.measureText(str, 0, str.length());

		float x = getScrollX() + (getWidth() - w2) / 2;
		float y = getScrollY() + (getHeight() * 0.1f)
			+ th - paint.descent();

		p_canvas.drawText(str, x, y, paint);

		paint.setTextSize(old);
		paint.setShadowLayer(0, 0, 0, 0);
		paint.setAlpha(255);
	}

	public void drawFloatingButtons(Canvas p_canvas)
	{
		Paint paint = fore_fab;

		if (paint == null) return;

		// Percentage of current font size
		int min = 150;
		int max = 500;
		int pct = min + Preferences.getFabMult() * (max-min) / 100;

		int fab_font_size = font_text_size;
		fab_font_size = pct * fab_font_size / 100;
		fab_font_size = Math.max(fab_font_size, MIN_FONT);
		//fab_font_size = Math.min(fab_font_size, MAX_FONT);

		min = MIN_OPACITY;
		max = 255;
		int _alphaFg = min + Preferences.getKeyboardOpacity() * (max-min) / 100;

		for (FloatingView v: views) {

			if (!(v instanceof ButtonView)) continue;

			ButtonView btn = (ButtonView)v;

			String label = btn.label;
			String icon = "";

			if (label.length() == 0) {
				icon = btn.icon;
				if (icon.length() == 0 && btn.action.equals("run")) {
					icon = ".";
				}
				if (icon.length() == 0 && btn.action.equals("opacity")) {
					icon = "l";
				}
			}

			if (icon.length() > 0) {
				label = icon;
				paint.setTypeface(game_context.iconFont);
				// Make it bigger
				paint.setTextSize((int)(fab_font_size * 1.2f));
			}
			else {
				paint.setTextSize(fab_font_size);

				if (label.length() == 0) {
					label = btn.action;
				}

				if (label.length() > 4) {
					label = label.substring(0, 4);
				}

				if (label.length() > 1) {
					paint.setTypeface(game_context.monoFont);
				}
				else {
					paint.setTypeface(game_context.monoBoldFont);
				}
			}

			TSize s = getCharDimensions(paint);
			int th = s.height;

			int w2 = (int)paint.measureText(label, 0, label.length());
			int h2 = (int)(paint.descent() - paint.ascent());

			int tw = Math.max(th, w2);

			float pad = th * 0.2f;

			tw += pad*2;
			th += pad*2;

			// Remember size
			btn.w = tw;
			btn.h = th;

			float x = btn.x+getScrollX();
			float y = btn.y+getScrollY();

			if (btn.isBeingDragged()) {
				dirZoneFill.setColor(color_drag);
			}
			else {
				dirZoneFill.setColor(AdvButton.DEFAULT_BG);
			}
			dirZoneFill.setAlpha(alpha);
			RectF r = new RectF(x, y, x+tw-1, y+th-1);
			p_canvas.drawRect(r, dirZoneFill);

			dirZoneStroke.setColor(color1_stroke);
			dirZoneStroke.setAlpha(alpha);
			p_canvas.drawRect(r, dirZoneStroke);

			int _color = Color.WHITE;
			if (btn.action.equals("run") && state.getRunningMode()) {
				_color = AdvButton.TOGGLED_BG;
			}
			paint.setColor(_color);
			paint.setShadowLayer(10f, 0, 0, Color.CYAN);
			paint.setAlpha(_alphaFg);

			int padx = Math.max((tw-w2)/2, 0);
			int pady = Math.max((th-h2)/2, 0);

			x += padx;
			y += (th - pady - paint.descent());

			p_canvas.drawText(label, x, y, paint);
		}

		paint.setShadowLayer(0, 0, 0, 0);
		paint.setAlpha(255);
	}

	protected void onDraw(Canvas p_canvas) {

		//Log.d("Angband", "Draw: " + System.currentTimeMillis());

		if (bitmap == null || state.stdscr == null) {
			p_canvas.drawColor(Color.BLACK);
			return;
		}

		int origin_y = getTopBarHeight();

		p_canvas.drawBitmap(bitmap, 0, origin_y, null);

		int tw = char_width;
		int th = char_height;

		int col = state.stdscr.col;
		int row = state.stdscr.row;

		int x = col * tw;
		int y = origin_y + row * th;

		if (this.bigTileActive() && state.stdscr.big_cursor) {
			tw = tile_wid_pix;
			th = tile_hgt_pix;
		}

		// due to font "scrunch", cursor is sometimes a bit too big
		int cl = Math.max(x,0);
		int cr = Math.min(x+tw,canvas_width);
		int ct = Math.max(y,0);
		int cb = Math.min(y+th,canvas_height);

		// Dont draw the cursor if we are using the timer
		if (state.stdscr.cursor_visible && savedTime == 0) {
			p_canvas.drawRect(cl, ct, cr, cb, cursor);
		}

		if (origin_y > 0 && state.characterPlaying()) {
			drawTopBar(p_canvas);
		}
		drawAllSubWindows(p_canvas);

		if (useSmallDPad()) {
			this.drawDPadRight(p_canvas);
		}
		else if (useFullDPad()) {
			this.drawDPadFull(p_canvas);
		}

		drawMouseToggle(p_canvas);

		drawFloatingButtons(p_canvas);

		drawFlashText(p_canvas);
	}

	public void computeCanvasSize() {
		canvas_width = Preferences.getTermWidth()*char_width
			+ getSubWindowsWidth();
		canvas_height = Preferences.getTermHeight()*char_height
			+ getSubWindowsHeight() + getTopBarHeight();
	}

	protected void setForeColor(int a) {
		fore.setColor(a);
	}
	protected void setBackColor(int a) {
		back.setColor(a);
	}

	public int getSubWindowsRows()
	{
		if (Preferences.getActivePlugin().enableSubWindows() &&
			Preferences.getHorizontalSubWindows() &&
			Preferences.getNumberSubWindows() > 0) {

			int nr = calculateRows();
			return Math.max(0, nr);
		}
		return 0;
	}

	public int getTopBarHeight()
	{
		if (Preferences.getActivePlugin().enableSubWindows()
			&& topBarEnabled()) {

			TSize size = getCharDimensions(fore_topbar);
			return (3 * size.height); // amount of rows for topbar
		}
		return 0;
	}

	public int getSubWindowsHeight()
	{
		int nr = getSubWindowsRows();
		if (nr > 0) {
			TSize size = getCharDimensions(fore_subw);
			return (nr * size.height);
		}
		return 0;
	}

	public int getSubWindowsColumns()
	{
		if (Preferences.getActivePlugin().enableSubWindows() &&
			!Preferences.getHorizontalSubWindows() &&
			Preferences.getNumberSubWindows() > 0) {

			int nc = calculateColumns();
			return Math.max(0, nc);
		}
		return 0;
	}

	public int getSubWindowsWidth()
	{
		int nc = getSubWindowsColumns();
		if (nc > 0) {
			TSize size = getCharDimensions(fore_subw);
			return nc * size.width;
		}
		return 0;
	}

	public void autoSizeFontByHeight(int maxWidth, int maxHeight) {

		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		/*
		if (game_context.horizontalInputMode()) {
			maxHeight -= game_context.getKeyboardHeight();
		}

		if (game_context.verticalInputMode()) {
			maxWidth -= game_context.getKeyboardWidth();
		}
		*/

		setFontFace();

		// HACK -- keep 480x320 fullscreen as-is
		if (!isHighRes()) {
			setFontSizeLegacy();
		}
		else {
			font_text_size = MIN_FONT;
			boolean success = false;
			do {
				font_text_size += 1;
				success = setFontSize(font_text_size, false);
			} while (success && (char_height * Preferences.rows) <=
				(maxHeight - getTopBarHeight() - getSubWindowsHeight()));

			font_text_size -= 1;
			setFontSize(font_text_size);
		}

		Log.d("Angband","autoSizeFontHeight "+font_text_size);
	}

	public void autoSizeFontByWidth(int maxWidth, int maxHeight) {

		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		/*
		if (game_context.horizontalInputMode()) {
			maxHeight -= game_context.getKeyboardHeight();
		}

		if (game_context.verticalInputMode()) {
			maxWidth -= game_context.getKeyboardWidth();
		}
		*/

		setFontFace();

		// HACK -- keep 480x320 fullscreen as-is
		if (!isHighRes()) {
			setFontSizeLegacy();
		}
		else {
			font_text_size = MIN_FONT;
			boolean success = false;
			do {
				font_text_size += 1;
				success = setFontSize(font_text_size, false);
			} while (success && (char_width * Preferences.cols) <=
				(maxWidth - getSubWindowsWidth()));

			font_text_size -= 1;
			setFontSize(font_text_size);
		}

		Log.d("Angband","autoSizeFontWidth "+font_text_size);
	}

	public int getHorizontalGap()
	{
		return (Preferences.getKeyboardOverlap() ?
				game_context.getKeyboardWidth(): 0);
	}

	public int getVerticalGap()
	{
		return (Preferences.getKeyboardOverlap() ?
				game_context.getKeyboardHeight(): 0);
	}

	public void adjustTermSize(int maxWidth, int maxHeight)
	{
		Log.d("Angband", "Adjust term size");

		state.nativew.clearTileCache();

		this.rows = Preferences.rows;
		this.cols = Preferences.cols;

		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		/*
		if (game_context.horizontalInputMode()) {
			maxHeight -= game_context.getKeyboardHeight();
		}

		if (game_context.verticalInputMode()) {
			maxWidth -= game_context.getKeyboardWidth();
		}
		*/

		maxHeight = maxHeight - getTopBarHeight() - getSubWindowsHeight();
		maxWidth = maxWidth - getSubWindowsWidth();

		while ((maxHeight > 0) &&
			((this.rows+1) * this.char_height < maxHeight)) {

			++this.rows;
		}

		while ((maxWidth > 0) &&
			((this.cols+1) * this.char_width < maxWidth)) {

			++this.cols;
		}

		// Check values
		this.rows = Math.max(this.rows, Preferences.rows);
		this.cols = Math.max(this.cols, Preferences.cols);

		this.rows = Math.min(this.rows, Preferences.max_rows);
		this.cols = Math.min(this.cols, Preferences.max_cols);

		//Log.d("Angband", "Resize. maxWidth "+maxWidth
		//		+" maxHeight "+maxHeight
		//		+" rows "+this.rows+" cols "+this.cols);

		// Do nothing
		if (this.rows == Preferences.getTermHeight()
			&& this.cols == Preferences.getTermWidth()) {
			Log.d("Angband", "Size unchanged");
			return;
		}

		// Remember for later
		Preferences.setSize(this.cols, this.rows);

		// Tell the core, or the termwindows
		state.nativew.resizeToCore(this.cols, this.rows);
	}

	public boolean isHighRes() {
		Point size = new Point();
		fullDisplay.getSize(size);
		int maxWidth = size.x;
		int maxHeight = size.y;

		//Log.d("Angband","isHighRes "+maxHeight+","+maxWidth +","+ (Math.max(maxWidth,maxHeight)>480));
		return Math.max(maxWidth,maxHeight)>480;
	}

	private void setFontSizeLegacy() {
		font_text_size = 13;
		char_height = 13;
		char_width = 6;
		setFontSize(font_text_size);
	}

	private void setFontFace() {
		if ( !isHighRes() ) {
			tfTiny = Typeface.createFromAsset(getResources().getAssets(), "6x13.ttf");
			fore.setTypeface(tfTiny);
		}
		else {
			tfStd = Typeface.createFromAsset(getResources().getAssets(), "VeraMoBd.ttf");
			//tfStd = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
			fore.setTypeface(tfStd);
		}
	}

	public void increaseFontSize() {
		setFontSize(font_text_size+1);
		adjustTermSize(0,0);
	}

	public void decreaseFontSize() {
		setFontSize(font_text_size-1);
		adjustTermSize(0,0);
	}

	private boolean setFontSize(int size) {
		return setFontSize(size,true);
	}

	private boolean setFontSize(int size, boolean persist) {

		setFontFace();

		size = Math.max(size, MIN_FONT);
		size = Math.min(size, MAX_FONT);

		font_text_size = size;

		fore.setTextSize(font_text_size);

		if (persist) {
			if(Preferences.isScreenPortraitOrientation())
				Preferences.setPortraitFontSize(font_text_size);
			else
				Preferences.setLandscapeFontSize(font_text_size);
		}

		char_height = (int)Math.ceil(fore.getFontSpacing());
		char_width = (int)fore.measureText("X", 0, 1);
		//Log.d("Angband","setSizeFont "+fore.measureText("X", 0, 1));

		int pct = Preferences.getFontSizeSubWindows();
		int subw_font_size = font_text_size / 2;
		subw_font_size += (pct * font_text_size / 100);
		subw_font_size = Math.max(subw_font_size, MIN_FONT);
		subw_font_size = Math.min(subw_font_size, MAX_FONT);
		fore_subw.setTextSize(subw_font_size);
		fore_subw.setTypeface(fore.getTypeface());

		pct = Preferences.getTopBarFontMultiplier();
		int topbar_font_size = font_text_size + (pct * font_text_size / 100);
		topbar_font_size = Math.max(topbar_font_size, MIN_FONT);
		topbar_font_size = Math.min(topbar_font_size, MAX_FONT);
		fore_topbar.setTextSize(topbar_font_size);
		fore_topbar.setTypeface(fore.getTypeface());

		float tile_font_mult = Preferences.getTileFontMult();
		int tile_font_size = (int)(font_text_size * tile_font_mult);
		tile_font_size = Math.max(tile_font_size, MIN_FONT);
		tile_font_size = Math.min(tile_font_size, MAX_FONT);
		tile_fore.setTextSize(tile_font_size);
		tile_fore.setTypeface(fore.getTypeface());

		tile_wid_pix = (int)(char_width * tile_wid);
		tile_hgt_pix = (int)(char_height * tile_hgt);

		//Log.d("Angband", "set_font_size - tile: " + tile_wid_pix + " " + tile_hgt_pix);

		return true;
	}

	@Override
	protected void onMeasure(int widthmeasurespec, int heightmeasurespec) {

		int height = MeasureSpec.getSize(heightmeasurespec);
		int width = MeasureSpec.getSize(widthmeasurespec);

		//Log.d("Angband", "System size " + width + "x" + height);

		boolean vertical = Preferences.isScreenPortraitOrientation();

		int fs = (vertical ?
			Preferences.getPortraitFontSize():
			Preferences.getLandscapeFontSize());

		// Testing
		// fs = 0;

		Log.d("Angband", "onMeasure. Font size: " + fs);

		if (fs == 0) {
			if (game_context.shouldAdjustByWidth()) {
				autoSizeFontByWidth(width, height);
			}
			else {
				autoSizeFontByHeight(width, height);
			}
			adjustTermSize(width, height);
		}
		else {
			setFontSize(fs, false);
		}

		fore.setTextAlign(Paint.Align.LEFT);

		//int minheight = getSuggestedMinimumHeight();
		//int minwidth = getSuggestedMinimumWidth();

		//computeCanvasSize();

		//Log.d("Angband","onMeasure - Canvas: "+canvas_width+","+canvas_height+
		//    " ; System: "+width+","+height);

		//width = Math.max(width, canvas_width);
		//height = Math.max(height, canvas_height);

		setMeasuredDimension(width, height);

		//Log.d("Angband","onMeasure "+canvas_width+","+canvas_height+";"+width+","+height);
	}

	public void updateDrag(float fx, float fy)
	{
		int px = (int)fx;
		int py = (int)fy;

		if (currentMode != DRAGGING) return;

		Point newLocation = new Point(px, py);

		if (lastLocation == null) {
			lastLocation = newLocation;
		}

		int dy = newLocation.y - lastLocation.y;
		int dx = newLocation.x - lastLocation.x;

		//Log.d("Angband", "Delta: " + dx + "@" + dy);

		int maxDist = 10;

		// Finetune the movement if we are already moving
		if (!timerHandler.hasMessages(LONG_PRESS)) maxDist = 0;

		// To disable long press, measure distance
		if ((dx * dx) + (dy * dy) > (maxDist * maxDist)) {

			// Save new location
			lastLocation = newLocation;

			// Disable Long Press Message
			timerHandler.removeMessages(LONG_PRESS);

			lastView.updatePosition(dy, dx);

			long now = System.currentTimeMillis();

			// Leave some gaps between draw operations
			if (now - lastRefresh >= 50) {
				lastRefresh = now;
				invalidate();
			}
		}
	}

	public boolean useFullDPad()
	{
		return Preferences.getEnableTouch() &&
			!Preferences.getTouchRight();
	}

	public boolean useSmallDPad()
	{
		return Preferences.getEnableTouch() &&
			Preferences.getTouchRight();
	}

	public boolean canMoveDPad()
	{
		return Preferences.getEnableTouch() &&
			Preferences.getTouchRight() &&
			Preferences.getTouchDragEnabled();
	}

	public void displayContextMenu()
	{
		handler.sendEmptyMessage(AngbandDialog.Action.OpenContextMenu.ordinal());
	}

	public FloatingView getView(int pid)
	{
		for (FloatingView v: views) {
			if (v.id == pid) {
				return v;
			}
		}
		return null;
	}

	public FloatingView getViewByXY(float py, float px)
	{
		FloatingView saved = null;

		for (FloatingView v: views) {
			if (v.selectable() && v.inside(py, px)) {
				if (saved == null || v.getPriority() > saved.getPriority()) {
					saved = v;
				}
			}
		}

		return (saved != null) ? saved: getView(SCREEN_VIEW);
	}

	public class FloatingView
	{
		public float x = 0;
		public float y = 0;
		public float w = 10;
		public float h = 10;
		public float margin = 0;
		public int id;

		public FloatingView()
		{
			id = ++idPool;
		}

		public boolean inside(float py, float px)
		{
			return py >= y && py < y+h && px >= x && px < x+w;
		}

		public boolean insideInner(float py, float px)
		{
			return py >= y+margin && py < y+h-margin &&
				px >= x+margin && px < x+w-margin;
		}

		public boolean isBeingDragged()
		{
			return currentMode == DRAGGING
				&& draggingEnabled() && lastView == this;
		}

		public boolean selectable()
		{
			return true;
		}

		public boolean draggingEnabled()
		{
			return true;
		}

		public boolean repeatEnabled()
		{
			return false;
		}

		public boolean longPressEnabled()
		{
			return true;
		}

		public void executeAction(MotionEvent me)
		{
			//
		}

		public void executeLongPress(MotionEvent me)
		{
			//
		}

		public void updatePosition(float dy, float dx)
		{
			x += dx;
			y += dy;
		}

		public void finishDragging()
		{
			//
		}

		public int getPriority()
		{
			return 0;
		}

		public int getLongPressDelay()
		{
			return longPressDelay;
		}
	}

	public class ScreenView extends FloatingView
	{
		@Override
		public boolean selectable()
		{
			return false;
		}

		@Override
		public void executeAction(MotionEvent me)
		{
			onSingleTapUp(me);
		}

		@Override
		public void executeLongPress(MotionEvent me)
		{
			onLongPress(me);
		}

		@Override
		public void updatePosition(float dy, float dx)
		{
			onScroll(null, null, dx, dy);
		}
	}

	public class DPadView extends FloatingView
	{
		int ox;
		int oy;
		int direction = 0;

		public void updateOffset()
		{
			x = ox + dragOffset.x;
			y = oy + dragOffset.y;
		}

		@Override
		public boolean draggingEnabled()
		{
			return direction == '5' && (useFullDPad() || canMoveDPad());
		}

		@Override
		public boolean repeatEnabled()
		{
			return direction != '5';
		}

		@Override
		public boolean longPressEnabled()
		{
			return direction == '5' && useFullDPad();
		}

		@Override
		public void executeAction(MotionEvent me)
		{
			if (me != null) {
				float x = me.getX();
				float y = me.getY();
				// In the margin ?
				if (!insideInner(y, x)) return;
			}
			state.addDirectionKey(direction);
		}

		@Override
		public void executeLongPress(MotionEvent me)
		{
			onLongPress(me);
		}

		@Override
		public void updatePosition(float dy, float dx)
		{
			if (useFullDPad()) {
				onScroll(null, null, dx, dy);
			}
			else {
				dragOffset.x += dx;
				dragOffset.y += dy;

				updateAllOffsets();
			}
		}

		@Override
		public void finishDragging()
		{
			Preferences.setTouchDragOffset(dragOffset.x, dragOffset.y);
		}

		@Override
		public int getPriority()
		{
			return 1;
		}
	}

	public void rearrangeFloatingButtons()
	{
		int h = 0;
		for (ButtonView b: getButtons()) {
			if (b.h > h) h = (int)b.h;
		}
		int pad = (int)(h * 0.2f);

		int x0 = getHorizontalGap() + pad;
		int y0 = getHeight() - getVerticalGap() - h - pad;

		int max = 5;
		int i = 0;

		int x = x0;
		int y = y0;

		for (ButtonView b: getButtons()) {

			if (i >= max) {
				y -= (h + pad);
				x = x0;
				i = 0;
			}

			int pady = (int)(h - b.h) / 2;

			b.x = x;
			b.y = y + pady;

			x += (b.w + pad);

			++i;
		}

		serializeButtons();
		resetFloatingButtons();
		invalidate();
	}

	public ArrayList<ButtonView> getButtons()
	{
		ArrayList<ButtonView> list = new ArrayList<>();

		for (FloatingView v: views) {
			if (v instanceof ButtonView) {
				list.add((ButtonView)v);
			}
		}
		return list;
	}

	public String serializeButtons()
	{
		JSONArray arr = new JSONArray();

		for (ButtonView b: getButtons()) {
			// Discard buttons with empty actions
			if (b.action.trim().length() == 0) continue;
			JSONObject obj = b.toJSON();
			if (obj != null) arr.put(obj);
		}

		String str;

		str = arr.toString();

		//Log.d("Angband", str);

		str = Base64.encodeToString(str.getBytes(), 0);

		//Log.d("Angband", str);

		Preferences.getActiveProfile().setFloatingButtons(str);
		Preferences.saveProfiles();

		return str;
	}

	public static JSONArray parseButtons(String str)
	{
		JSONArray arr = new JSONArray();

		if (str.length() == 0) return arr;

		byte[] data = Base64.decode(str, 0);

		try {
			str = new String(data, "UTF-8");
		}
		catch (java.io.UnsupportedEncodingException ex) {
			Log.d("Angband", ex.getMessage());
			return arr;
		}

		try {
			JSONArray temp = new JSONArray(str);
			arr = temp;
		}
		catch (JSONException ex) {
			Log.d("Angband", ex.getMessage());
			return arr;
		}

		return arr;
	}

	public static String mergeButtons(String target, String source)
	{
		JSONArray arrTarget = parseButtons(target);
		JSONArray arrSource = parseButtons(source);

		for (int i = 0; i < arrSource.length(); i++) {
			JSONObject obj = arrSource.optJSONObject(i);

			String action = obj.optString("action", "");
			if (action.length() == 0) continue;

			boolean found = false;
			for (int j = 0; j < arrTarget.length(); j++) {
				JSONObject obj2 = arrTarget.optJSONObject(j);
				if (obj2.optString("action", "").equals(action)) {
					found = true;
					break;
				}
			}
			if (!found) arrTarget.put(obj);
		}

		String str = arrTarget.toString();

		//Log.d("Angband", str);

		str = Base64.encodeToString(str.getBytes(), 0);

		return str;
	}

	public int deserializeButtons()
	{
		int n = 0;

		String str = Preferences.getActiveProfile().getFloatingButtons();

		JSONArray arr = parseButtons(str);

		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.optJSONObject(i);
			ButtonView b = new ButtonView();
			b.fromJSON(obj);
			views.add(b);
			++n;
		}

		return n;
	}

	public class ButtonView extends FloatingView
	{
		String action = "";
		String label = "";
		String icon = "";
		Boolean fixed = false;

		public void fromJSON(JSONObject obj)
		{
			x = obj.optInt("x", 20);
			y = obj.optInt("y", 20);
			action = obj.optString("action", "i");
			label = obj.optString("label", "");
			icon = obj.optString("icon", "");
			fixed = obj.optBoolean("fixed", false);
		}

		public JSONObject toJSON()
		{
			JSONObject obj = new JSONObject();
			try {
				obj.put("action", action);
				obj.put("icon", icon);
				obj.put("label", label);
				obj.put("x", x);
				obj.put("y", y);
				obj.put("fixed", fixed);
			}
			catch (JSONException ex) {
				Log.d("Angband", ex.getMessage());
				return null;
			}
			return obj;
		}

		@Override
		public boolean draggingEnabled()
		{
			return !fixed;
		}

		@Override
		public void executeAction(MotionEvent me)
		{
			if (action.equals("run")) {
				state.setRunningMode(!state.getRunningMode());
				invalidate();
				game_context.refreshInputWidgets();
				return;
			}

			if (action.equals("opacity")) {
				game_context.runOpacityPopup();
				return;
			}

			InputUtils.processAction(state, action);
		}

		@Override
		public void executeLongPress(MotionEvent me)
		{
			FabCrudPopup win = new FabCrudPopup(this);
			int gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
			win.showAtLocation(TermView.this, gravity, 0, 10);
		}

		@Override
		public void finishDragging()
		{
			serializeButtons();
		}

		@Override
		public int getPriority()
		{
			return 2;
		}

		@Override
		public void updatePosition(float dy, float dx)
		{
			float x2 = x+dx;
			float y2 = y+dy;

			RectF bounds = new RectF(0, 0, getWidth(), getHeight());

			RectF floating = new RectF(x2, y2, x2+w, y2+h);

			if (!bounds.contains(floating)) return;

			if (Preferences.getKeyboardOverlap()) {

				float kh = game_context.getKeyboardHeightAbsolute();
				float kw = game_context.getKeyboardWidthAbsolute();

				bounds.top = bounds.bottom - kh;
				bounds.right = kw;

				if (bounds.intersect(floating)) return;
			}

			x = x2;
			y = y2;
		}

		@Override
		public int getLongPressDelay()
		{
			return longPressDelay2;
		}
	}

	public void createFloatingButton()
	{
		int x = getHorizontalGap() + (getWidth() - getHorizontalGap()) / 2;
		int y = (getHeight() - getVerticalGap()) / 2;

		ButtonView b = new ButtonView();

		b.x = x;
		b.y = y;

		views.add(b);

		FabCrudPopup win = new FabCrudPopup(b);
		int gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		win.showAtLocation(this, gravity, 0, 10);
	}

	public class FabCrudPopup extends PopupWindow
		implements View.OnClickListener
	{
		public EditText actionTxt = null;
		public EditText labelTxt = null;

		public CheckBox ckFixed = null;

		public ButtonView target = null;

		public SeekBar sizeBar = null;

		public int max = 0;

		public ArrayList<Button> buttons = new ArrayList<>();

		public FabCrudPopup(ButtonView p_target)
		{
			super(game_context);

			target = p_target;

			String s = game_context.getResources().getString(R.string.def_keymap_len);
			max = Integer.parseInt(s);

			setFocusable(true);
			setWidth(LayoutParams.WRAP_CONTENT);
			setHeight(LayoutParams.WRAP_CONTENT);

			ViewGroup content = (ViewGroup)
				game_context.getLayoutInflater().inflate
				(R.layout.fab_crud, null);

			setContentView(content);

			actionTxt = content.findViewById(R.id.action);

			labelTxt = content.findViewById(R.id.label);

			labelTxt.setText(target.label);

			sizeBar = content.findViewById(R.id.size_mult);

			sizeBar.setProgress(Preferences.getFabMult());

			SeekBar.OnTouchListener listener = new SeekBar.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					int action = event.getAction();
					switch (action)
					{
					case MotionEvent.ACTION_DOWN:
						// Disallow ScrollView to intercept touch events.
						v.getParent().requestDisallowInterceptTouchEvent(true);
						break;
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						// Allow ScrollView to intercept touch events.
						v.getParent().requestDisallowInterceptTouchEvent(false);
						break;
					}

					// Handle Seekbar touch events.
					v.onTouchEvent(event);
					return true;
				}
			};

			sizeBar.setOnTouchListener(listener);

			assignText(target.action);

			ckFixed = content.findViewById(R.id.fixed);
			ckFixed.setChecked(target.fixed);

			Button btn = content.findViewById(R.id.run_button);
			btn.setTag("action:run");
			btn.setTypeface(game_context.iconFont);
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.opa_button);
			btn.setTag("action:opa");
			btn.setTypeface(game_context.iconFont);
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.esc_button);
			btn.setTag("action:esc");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.spc_button);
			btn.setTag("action:spc");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.tab_button);
			btn.setTag("action:tab");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.ret_button);
			btn.setTag("action:ret");
			btn.setOnClickListener(this);

			ImageButton btn2 = content.findViewById(R.id.delete_button);
			btn2.setTag("action:del");
			btn2.setOnClickListener(this);

			String icons = " imfvgdR+ewb,azuS[F|p]CBJqU*~LM=?>Q" +
				Character.toString('\u00D4') +
				Character.toString('\u00D2') +
				Character.toString('\u00D3') +
				Character.toString('\u006B') +
				Character.toString('\u00BB');

			TableRow[] rows = {
				content.findViewById(R.id.row1),
				content.findViewById(R.id.row2),
				content.findViewById(R.id.row3),
				content.findViewById(R.id.row4)
			};

			int i = 0;
			for (char c: icons.toCharArray()) {

				String label = Character.toString(c).trim();

				Button b = new Button(game_context);
				b.setText(label);
				b.setTextSize(font_text_size);
				b.setTypeface(game_context.iconFont);
				b.setTag("icon:"+i);
				b.setOnClickListener(this);

				int r = (i++ / 10) % rows.length;
				rows[r].addView(b);

				buttons.add(b);
			}

			updateButtons();
		}

		public void updateButtons()
		{
			for (Button b: buttons) {
				if (b.getText().toString().equals(target.icon)) {
					b.getBackground().setAlpha(100);
				}
				else {
					b.getBackground().setAlpha(255);
				}
			}
		}

		public void addText(String txt)
		{
			txt = actionTxt.getText()+txt;
			if (txt.length() <= max) assignText(txt);
		}

		public void assignText(String txt)
		{
			actionTxt.setText(txt);
			actionTxt.setSelection(actionTxt.getText().length());
		}

		@Override
		public void onClick(View v) {

			String tag = (String)v.getTag();

			if (tag.startsWith("icon:")) {
				target.icon = ((Button)v).getText().toString();
				updateButtons();
				return;
			}

			if (tag.equals("action:opa")) {
				assignText("opacity");
				return;
			}

			if (tag.equals("action:run")) {
				assignText("run");
				return;
			}

			if (tag.equals("action:ret")) {
				addText("\\n");
				return;
			}

			if (tag.equals("action:esc")) {
				addText("\\e");
				return;
			}

			if (tag.equals("action:spc")) {
				addText("\\s");
				return;
			}

			if (tag.equals("action:tab")) {
				addText("\\t");
				return;
			}

			if (tag.equals("action:del")) {
				assignText("");
				dismiss();
				return;
			}
		}

		@Override
		public void dismiss()
		{
			target.action = actionTxt.getText().toString().trim();

			target.label = labelTxt.getText().toString().trim();

			target.fixed = ckFixed.isChecked();

			AdvButton.closeSoftKeyboard(game_context, actionTxt);

			AdvButton.closeSoftKeyboard(game_context, labelTxt);

			Preferences.setFabMult(sizeBar.getProgress());

			serializeButtons();
			resetFloatingButtons();
			invalidate();

			super.dismiss();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {

		// Display quick settings (bottom left corner)
		if (me.getAction() == MotionEvent.ACTION_UP &&
			!Preferences.getEnableSoftInput() &&
			me.getX() < (getWidth() * 0.1f) &&
			me.getY() >= (getHeight() * 0.9f)) {

			stopTimers();
			displayContextMenu();
			return true;
		}

		//Log.d("Angband", "x: " + me.getX() + " scroll: " + this.getScrollX());

		float x = me.getX();
		float y = me.getY();

		FloatingView view = getViewByXY(y, x);

		lastEvent = curEvent;
		curEvent = me.getAction();

		// Restore state if:
		// 1. Reentrant event
		// 2. Different view (unless dragging)
		if (curEvent == MotionEvent.ACTION_DOWN
			|| (view != lastView && currentMode != DRAGGING)) {

			stopTimers();

			lastView = view;
		}

		if (curEvent == MotionEvent.ACTION_DOWN) {

			//Log.d("Angband", "DOWN! " + lastView.id);

			// Delay dragging
			if (lastView.draggingEnabled()) {
				timerHandler.sendEmptyMessageDelayed(DRAGGING, dragDelay);
			}
			// Delay long press
			if (lastView.longPressEnabled()) {
				timerHandler.sendEmptyMessageDelayed(LONG_PRESS, lastView.getLongPressDelay());
			}
			// Send the action now
			if (lastView.repeatEnabled()) {
				// Cancel these
				timerHandler.removeMessages(LONG_PRESS);
				timerHandler.removeMessages(DRAGGING);
				// --
				currentMode = REPEAT_DIR;
				lastView.executeAction(me);
				// And remember for repetition
				timerHandler.sendEmptyMessageDelayed(REPEAT_DIR, longRepeatDelay);
			}
		}

		// Drag the buttons
		if (curEvent == MotionEvent.ACTION_MOVE) {

			//Log.d("Angband", "MOVE!");

			updateDrag(x, y);
		}

		if (curEvent == MotionEvent.ACTION_CANCEL) {

			//Log.d("Angband", "CANCEL!");

			stopTimers();
		}

		if (curEvent == MotionEvent.ACTION_UP) {

			//Log.d("Angband", "UP!");

			// Disable all timers, now
			timerHandler.removeMessages(REPEAT_DIR);
			timerHandler.removeMessages(LONG_PRESS);
			timerHandler.removeMessages(DRAGGING);

			if (currentMode == 0)  {
				lastView.executeAction(me);
			}

			// Remember position if needed
			if (currentMode == DRAGGING &&
				lastEvent == MotionEvent.ACTION_MOVE) {
				lastView.finishDragging();
			}

			stopTimers();
		}

		return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		int newscrollx = this.getScrollX() + (int)distanceX;
		int newscrolly = this.getScrollY() + (int)distanceY;

		if(newscrollx < 0)
			newscrollx = 0;
		if(newscrolly < 0)
			newscrolly = 0;
		if(newscrollx >= canvas_width - getWidth())
			newscrollx = canvas_width - getWidth() + 1;
		if(newscrolly >= canvas_height - getHeight())
			newscrolly = canvas_height - getHeight() + 1;

		if (canvas_width <= getWidth()) newscrollx = 0; //this.getScrollX();
		if (canvas_height <= getHeight()) newscrolly = 0; //this.getScrollY();

		//Log.d("Angband", "Scroll to " + newscrollx + " " + newscrolly
		//    + " distance " + distanceX + " " + distanceY);

		scrollTo(newscrollx, newscrolly);

		return true;
	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return true;
	}

	public boolean inZoneOfDPad(float y, float x)
	{
		float padPct = 0.2f;

		if (useSmallDPad()) {

			DPadView first = getViewByDirection('7');
			DPadView last = getViewByDirection('3');

			if (last == null || first == null) return false;

			float side = first.h;
			float pad = side * padPct;

			RectF bigZone = new RectF(first.x - pad, first.y - pad,
				last.x + side + pad, last.y + side + pad);
			if (bigZone.contains(x, y)) {
				return true;
			}
		}

		return false;
	}

	public void onLongPress(MotionEvent e) {

		float x = 0;
		float y = 0;

		if (e != null) {
			x = e.getX();
			y = e.getY();
		}
		else if (lastLocation != null) {
			x = lastLocation.x;
			y = lastLocation.y;
		}

		// Have DPad ?
		if (Preferences.getEnableTouch()) {
			// Too close
			if (this.inZoneOfDPad(y, x)) {
				return;
			}
		}

		displayContextMenu();
	}
	public void onShowPress(MotionEvent e) {
	}

	public void popupMouseOptions(final int y, final int x,
								  int realY, int realX)
	{
		final PopupWindow win = new PopupWindow(game_context);
		win.setFocusable(true);
		win.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		win.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

		LinearLayout mainView = (LinearLayout)game_context
				.getLayoutInflater().inflate(R.layout.mouse_input, null);
		win.setContentView(mainView);

		LinearLayout row1 = mainView.findViewById(R.id.row1);
		LinearLayout row2 = mainView.findViewById(R.id.row2);

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				win.dismiss();
				int button = 1;
				int mods = 0;
				String label = ((Button)v).getText().toString();
				if (label.contains("2")) button = 2;
				if (label.contains("Ctrl")) mods = 1;
				if (label.contains("Shift")) mods = 2;
				if (label.contains("Alt")) mods = 4;
				state.addMousePress(y, x, button, mods);
			}
		};

		String[] labels = new String[]
				{"1","Shitf-1","Ctrl-1","Alt-1",
				"2","Shift-2","Ctrl-2","Alt-2"};
		int cut = 4;
		for (int i = 0; i < labels.length; i++) {
			Button btn = (Button)game_context.getLayoutInflater().inflate
					(R.layout.popupbutton, null);
			btn.setText(labels[i]);
			btn.setOnClickListener(listener);
			if (i < cut) {
				row1.addView(btn);
			}
			else {
				row2.addView(btn);
			}
		}

		int gravity = Gravity.LEFT | Gravity.TOP;
		win.showAtLocation(this, gravity, realX, realY);
	}

	public boolean ignoreMouse(int y, int x) {

		if (y < getTopBarHeight()) {
			//Log.d("Angband", "1");
			return true;
		}

		if (x >= getWidth() - getSubWindowsWidth()) {
			//Log.d("Angband", "2");
			return true;
		}

		if (y >= getHeight() - getSubWindowsHeight()) {
			//Log.d("Angband", "3");
			return true;
		}

		return false;
	}

	public void sendMousePress(int y, int x) {

		if (Preferences.getActivePlugin().noMouse() || ignoreMouse(y, x)) {
			state.addKey(InputUtils.KC_ESCAPE);
			return;
		}

		Point p = mouseToGrid(y, x);
		//Log.d("Angband", "y x " + p.y + " " + p.x);

		if (mouseSpecial) {
			popupMouseOptions(p.y, p.x, y, x);
		}
		else {
			state.addMousePress(p.y, p.x, 1, 0);
		}
	}

	public boolean onSingleTapUp(MotionEvent event) {

		// Do nothing ?
		if (event == null) return true;

		int x = (int)event.getX();
		int y = (int)event.getY();

		if (mouseToggle != null && mouseToggle.contains(x,y)) {
			mouseSpecial = !mouseSpecial;
			invalidate();
			return true;
		}

		// Too close to directionals
		if (this.inZoneOfDPad(y, x)) {
			// Do nothing
			//return true;
		}

		sendMousePress(y, x);
		return true;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		//Log.d("Angband", "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		handler.sendEmptyMessage(AngbandDialog.Action.StartGame.ordinal());
	}

	public boolean onGameStart() {

		computeCanvasSize();

		int bmp_w = Preferences.getTermWidth() * char_width;
		int bmp_h = Preferences.getTermHeight() * char_height;

		// sanity
		if (bmp_w == 0 || bmp_h == 0) {
			state.saveTheDungeon(null);
			return false;
		}

		// Try to load a stored bitmap of that size
		bitmap = state.restoreTheDungeon(bmp_w, bmp_h);

		if (bitmap != null) {
			//Log.d("Angband","Reuse Bitmap!!!");
			; // nothing
		}
		else {
			//Log.d("Angband","createBitmap "+canvas_width+","+canvas_height);
			bitmap = Bitmap.createBitmap(bmp_w, bmp_h, Bitmap.Config.RGB_565);
			//bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
			state.saveTheDungeon(bitmap);
		}

		canvas = new Canvas(bitmap);

		/*
		  canvas.setFilter(new PaintFlagsDrawFilter(
		  Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG,0
		  )); // this seems to have no effect, why?
		*/

		state.currentPlugin = Plugins.Plugin.convert(Preferences.getActiveProfile().getPlugin());

		Log.d("Angband", "onGameStart !!!");

		return true;
	}

	public void wipe(int r, int c)
	{
		int tw = char_width;
		int th = char_height;
		int x = c * tw;
		int y = r * th;

		setBackColor(Color.BLACK);

		canvas.drawRect(x, y, x + tw, y + th, back);
	}

	public void drawPoint(int r, int c, TermWindow.TermPoint p,
		int fcolor, int bcolor) {

		if (canvas == null) {
			// OnSizeChanged has not been called yet
			//Log.d("Angband","null canvas in drawPoint");
			return;
		}

		if (p.isBigPad()) return;

		char ch = p.ch;

		Paint fore_temp = fore;
		int tw = char_width;
		int th = char_height;
		int x = c * tw;
		int y = r * th;

		ch &= 0xFF;

		setBackColor(bcolor);

		Rect bg = new Rect(x, y, x+tw, y+th);

		canvas.drawRect(bg, back);

		if (ch != ' ') {

			String str = ch + "";

			fore_temp.setColor(fcolor);

			float w2 = fore_temp.measureText(str, 0, 1);
			float pad = Math.max((tw - w2) / 2, 0);
			float h2 = fore_temp.descent() - fore_temp.ascent();
			float pady = Math.max((th - h2) / 2, 0) + fore_temp.descent();

			canvas.save();
			canvas.clipRect(bg);

			canvas.drawText(str, x + pad, y + th - pady, fore_temp);

			canvas.restore();
		}
	}

	public void drawText(int row, int col, int a, int c, int ta, int tc)
	{
		int gxa = a;

		a &= 0x7F;
		c &= 0xFF;

		ta &= 0x7F;

		Paint fore_temp = fore;
		int tw = char_width;
		int th = char_height;
		int y = row * th;
		int x = col * tw;

		if (this.bigTileActive()) {

			fore_temp = tile_fore;
			tw = tile_wid_pix;
			th = tile_hgt_pix;
		}

		String str = Character.toString((char)c);

		Rect bg = new Rect(x, y, x+tw, y+th);

		// Background
		TermWindow.ColorPair pair = TermWindow.pairs.get(ta);
		setBackColor(TermWindow.safeFgColor(pair));
		canvas.drawRect(bg, back);

		// Foreground
		pair = TermWindow.pairs.get(a);

		if (c == ' ' || pair == null) return;

		fore_temp.setColor(pair.fColor);

		float w2 = fore_temp.measureText(str, 0, 1);
		float padx = Math.max((tw - w2) / 2, 0);
		float h2 = fore_temp.descent() - fore_temp.ascent();
		float pady = Math.max((th - h2) / 2, 0) + fore_temp.descent();

		canvas.save();
		canvas.clipRect(bg);

		canvas.drawText(str, x + padx, y + th - pady, fore_temp);

		if ((gxa & MONSTER_MASK) != 0) {
			Rect r = new Rect(x, y, x+tw, y+th);
			drawLifeColor(gxa, r);
		}

		canvas.restore();
	}

	public void markTile(TermWindow t, int r, int c, int th, int tw)
	{
		int x, y;

		for (y = r - th; y <= r; y++) {
			for (x = c; x < c + tw; x++) {

				if (y < t.begin_y || y >= t.rows ||
					x < t.begin_x || x >= t.cols) continue;

				t.buffer[y][x].bgColor = -255;
			}
		}
	}

	public ArrayList<TileGrid> collectTileGrids
		(GraphicsMode gm, TermWindow t)
	{
		ArrayList<TileGrid> grids = new ArrayList<>();
		TileGrid g;
		String key;

		for (int row = 0; row < t.rows; row++) {
			for (int col = 0; col < t.cols; col++) {
				TermWindow.TermPoint p = t.buffer[row][col];
				if (!p.isGraphicTile()) continue;
				if (p.bgColor < 0) continue;

				g = new TileGrid(gm, p.fgColor, p.ch, row, col, this);
				key = g.createKey();
				if (state.tileCache.get(key) == null) {
					grids.add(g);
				}
				else {
					//Log.d("Angband", "Cache hit (preload): " + key);
				}

				g = new TileGrid(gm, p.bgColor, p.bgChar, row, col, this);
				key = g.createKey();
				if (state.tileCache.get(key) == null) {
					grids.add(g);
				}
				else {
					//Log.d("Angband", "Cache hit (preload 2): " + key);
				}
			}
		}

		return grids;
	}

	public void preloadTiles(TermWindow t)
	{
		GraphicsMode gm = currentGraf;
		if (gm == null || !gm.havePages() || !canDraw()) return;

		int i;
		HashSet<Point> pages = new HashSet<>();
		ArrayList<TileGrid> grids = collectTileGrids(gm, t);

		for (TileGrid tile: grids) {
			pages.add(tile.page);
		}

		for (Point curPage: pages) {

			Bitmap map = null;
			Iterator<TileGrid> iter = grids.iterator();

			while (iter.hasNext()) {

				TileGrid tile = iter.next();

				if (!tile.page.equals(curPage)) continue;

				String key = tile.createKey();

				if (state.tileCache.get(key) != null) {
					//Log.d("Angband", "Cache hit (preload 3): " + key);
					continue;
				}

				if (map == null) {
					String mapPath = gm.getFullPath(curPage);
					map = BitmapFactory.decodeFile(mapPath);
					if (map == null) return;
					map.prepareToDraw();
					//Log.d("Angband", "Loading page: " + mapPath);
				}

				Bitmap result = tile.loadBitmap(map);

				if (result != null) {
					state.tileCache.put(key, result);
				}

				iter.remove();
			}

			if (map != null) {
				map.recycle();
			}
		}
	}

	public Bitmap getTile(TileGrid tile)
	{
		GraphicsMode gm = currentGraf;

		String key = tile.createKey();

		Bitmap result = state.tileCache.get(key);

		if (result != null) {
			return result;
		}

		if (gm.havePages()) {
			String path = gm.getFullPath(tile.page);
			atlas = BitmapFactory.decodeFile(path);
			if (atlas == null) {
				Log.d("Angband", "Can't load " + path);
				return null;
			}
			atlas.prepareToDraw();

			//Log.d("Angband", "Loading page: " + path);

			result = tile.loadBitmap(atlas);

			atlas.recycle();
			atlas = null;
		}
		else {
			if (atlas == null) {
				return null;
			}
			result = tile.loadBitmap(atlas);
		}

		if (result != null) {
			state.tileCache.put(key, result);
		}

		return result;
	}

	public int[] getLifeColorAndPct(int a)
	{
		int[] ret = {0,0}; // Color, Percentage

		// No-op
		if ((a & (PLAYER_MASK|MONSTER_MASK)) == 0) {
			return ret;
		}

		int pct = (a >> 10) & 0x0F;
		int color = 0;

		boolean silMode = (Preferences.getActivePlugin() == Plugins.Plugin.silq);

		// In sil we get the health level
		if (silMode) {
			switch(pct) {
				case 0: // Dead
					color = Color.RED;
					pct = 0;
					break;
				case 1: // Almost Dead
					color = Color.RED;
					pct = 1;
					break;
				case 2: // Badly Wounded, light red
					color = 0x0FF4040;
					pct = 2;
					break;
				case 3: // Wounded, orange
					color = 0x0FF8000;
					pct = 5;
					break;
				case 4: // Somewhat Wounded
					color = Color.YELLOW;
					pct = 7;
					break;
				default: return ret; // Nothing
			}
		}
		// Vanilla, FA
		else if ((a & PLAYER_MASK) != 0) {

			color = Color.RED;
			if (pct >= 3) color = 0x0FF4040; // Light red
			if (pct >= 5) color = 0x0FF8000; // Orange
			if (pct >= 7) color = Color.YELLOW;
			if (pct >= 9) return ret; // White, do nothing
		}
		// Vanilla, FA
		else if ((a & MONSTER_MASK) != 0) {

			color = Color.RED;
			if (pct >= 1) color = 0x0FF4040; // Light red
			if (pct >= 3) color = 0x0FF8000; // Orange
			if (pct >= 6) color = Color.YELLOW;
			if (pct >= 9) return ret; // White, do nothing
		}

		ret[0] = color;
		ret[1] = pct;

		return ret;
	}

	public void drawLifeColor(int a, Rect dst)
	{
		int[] data = getLifeColorAndPct(a);
		int color = data[0];
		int pct = data[1];

		if (color == 0) return;

		boolean silMode = (Preferences.getActivePlugin() == Plugins.Plugin.silq);

		boolean drawShadow = false;

		if ((a & PLAYER_MASK) != 0) {
			drawShadow = true;
		}

		// In Sil-Q, draw a shadow over monsters
		if (silMode && Preferences.getDrawHealthBars()) {
			drawShadow = true;
		}

		if (drawShadow) {
			back.setColor(color);
			back.setAlpha(70);
			canvas.drawRect(dst, back);
		}

		// Draw a health bar, for player and monster
		if (Preferences.getDrawHealthBars()) {
			// Get height
			int h = (int)(tile_hgt_pix * 0.08f);
			h = Math.max(h,3);

			// Adjust pct like Vanilla
			pct = Math.min(pct+1,10);
			int w = pct * (tile_wid_pix) / 10;
			w = Math.max(w,2);

			back.setColor(Color.BLACK);
			back.setAlpha(255);
			Rect r = new Rect(dst.left, dst.top, dst.right,
				dst.top + h);
			canvas.drawRect(r, back);

			back.setColor(color);
			back.setAlpha(255);
			Rect r2 = new Rect(dst.left, dst.top, dst.left + w,
				dst.top + h);
			canvas.drawRect(r2, back);
		}

		back.setColor(Color.BLACK);
		back.setAlpha(255);
	}

	public boolean bitmapHasAlpha(Bitmap bm)
	{
		int[] pixels = new int[bm.getWidth() * bm.getHeight()];
		bm.getPixels(pixels, 0, bm.getWidth(),
			0, 0, bm.getWidth(), bm.getHeight());
		for (int i = 0; i < pixels.length; i++) {
			int alpha = Color.alpha(pixels[i]);
			if (alpha < 255) return true;
		}
		return false;
	}

	public boolean tileHasAlpha(TileGrid tile)
	{
		Bitmap bm = getTile(tile);
		if (bm == null) return true;
		String key = tile.createKey();
		if (!alphaTiles.containsKey(key)) {
			if (alphaTiles.size() > 100) alphaTiles.clear();
			int value = 0;
			if (bitmapHasAlpha(bm)) value = 1;
			//Log.d("Angband", "Putting value " + value);
			alphaTiles.put(key, value);
		}
		return alphaTiles.get(key).intValue() == 1;
	}

	public class GxAscii
	{
		/* Graphics encoding */
		int ga;
		int gc;
		/* Ascii encoding */
		int a;
		int c;
		String type;
	}

	public ArrayList<GxAscii> gxa_list = new ArrayList<>();

	public void drawAsciiHelper(TileGrid tile, Rect src)
	{
		int pctHelper = Preferences.getAsciiHelperPct();

		if (pctHelper < 10) return;

		GxAscii info = null;

		int attr = tile.attr & 0xFF;

		for (GxAscii ele: gxa_list) {
			if (ele.ga == attr && ele.gc == tile.chr) {
				info = ele;
				break;
			}
		}

		// Not found
		if (info == null) {
			String key = "get_gx_ascii_" + attr
				+ "_" + tile.chr;

			info = new GxAscii();
			info.type = "unknown";
			info.ga = attr;
			info.gc = tile.chr;
			info.a = 0;
			info.c = 0;

			String txt = state.nativew.queryString(key);

			if (txt != null) {
				String[] parts = txt.split(";");
				if (parts.length == 3) {
					info.type = parts[0];
					info.a = Integer.parseInt(parts[1]);
					info.c = Integer.parseInt(parts[2]);
				}
			}

			gxa_list.add(info);
		}

		if (!info.type.equals("monster") || (info.a + info.c) == 0) {
			return;
		}

		int side = pctHelper * Math.min(src.bottom - src.top,
			src.right - src.left) / 100;

		if (side < 10) return;

		/*
		int top = src.top;
		if (tile.isLargeTile) top += ((src.bottom-src.top)/2);

		Rect dst = new Rect(src.right - side,
			top, src.right, top + side);
		*/

		Rect dst = new Rect(src.right - side,
			src.bottom - side, src.right, src.bottom);

		setBackColor(Color.BLACK);
		canvas.drawRect(dst, back);

		TermWindow.ColorPair pair = TermWindow.pairs.get(info.a);
		if (pair == null) return;

		int color = pair.fColor;

		Paint paint = fore;

		float old = paint.getTextSize();

		float fs = Math.max(side * 0.8f, MIN_FONT);

		paint.setTextSize(fs);
		paint.setColor(color);

		String str = Character.toString((char)info.c);

		int w2 = (int)paint.measureText(str, 0, str.length());

		float x = dst.left + (side - w2) / 2;
		float y = dst.bottom - paint.descent();

		canvas.drawText(str, x, y, paint);

		paint.setTextSize(old);
	}

	public void drawSilQEffects(TileGrid tile, boolean beforeTile)
	{
		// Object is "Glowing", draw the effect before the tile
		if (beforeTile) {
			if ((tile.attr & 0x40) != 0) {
				// Hack - Get tile
				TileGrid tileGlow = tile.copyChanged(0x8C, 0x8E);
				Bitmap glow = getTile(tileGlow);
				if (glow != null) {
					//Log.d("Angband", "Loaded GLOWING");
					Rect dst = tileGlow.locateDest();
					canvas.drawBitmap(glow, dst.left, dst.top, null);
				} else {
					Log.d("Angband", "Cant load GLOWING");
				}
			}
			// Done
			return;
		}

		// Monster is "Alert"
		if ((tile.chr & 0x40) != 0) {
			// Hack - Get tile
			TileGrid tileAlert = tile.copyChanged(0x8C, 0x8B);
			Bitmap alert = getTile(tileAlert);
			if (alert != null) {
				//Log.d("Angband", "Loaded ALERT");
				Rect dst = tileAlert.locateDest();
				canvas.drawBitmap(alert, dst.left, dst.top, null);
			}
			else {
				Log.d("Angband", "Cant load ALERT");
			}
		}

		if ((tile.attr & (PLAYER_MASK|MONSTER_MASK)) != 0) {
			drawLifeColor(tile.attr, tile.locateDest());
		}
	}

	public void drawVanillaEffects(TileGrid tile, Rect dst)
	{
		drawAsciiHelper(tile, dst);

		if ((tile.attr & (PLAYER_MASK|MONSTER_MASK)) != 0) {
			drawLifeColor(tile.attr, dst);
		}
	}

	public void drawTileAux(int row, int col, int a, int c,
		boolean fill)
	{
		if (currentGraf == null || !canDraw()) {
			//Log.d("Angband","Cant draw tiles");
			return;
		}

		if (!currentGraf.havePages() && atlas == null) {
			//Log.d("Angband","Dont have atlas");
			return;
		}

		TileGrid tile = new TileGrid(currentGraf, a, c, row, col, this);

		Rect dst = tile.locateDest();

		/*
		// Optimization: just clear grids with alpha
		if (fill && tileHasAlpha(tile)) {
			setBackColor(Color.BLACK);
			canvas.drawRect(dst, back);
		}
		*/

		if (fill) {
			setBackColor(TermWindow.safeFgColor(null));
			canvas.drawRect(dst, back);
		}

		Bitmap bm = getTile(tile);

		if (bm == null) return;

		// Background, just draw and return
		if (fill) {
			canvas.drawBitmap(bm, dst.left, dst.top, null);
			return;
		}

		boolean silMode = (useGraphics == Preferences.MICROCHASM_GX);

		// Glowing effect
		if (silMode) {
			drawSilQEffects(tile, true);
		}

		// Draw the tile
		canvas.drawBitmap(bm, dst.left, dst.top, null);

		// Alert effect, health
		if (silMode) {
			drawSilQEffects(tile, false);
		}
		// Vanilla and FAangband effects
		else {
			drawVanillaEffects(tile, dst);
		}
	}

	public void drawTile(int row, int col, int a, char c,
		int ta, char tc) {

		// Not initialized
		if (canvas == null) {
			//Log.d("Angband", "No canvas");
			return;
		}

		// Pad character
		if ((a & 0xFF) == 255) {
			return;
		}

		// Ascii ?
		if ((c & 0x80) == 0) {
			drawText(row, col, a, c, ta, tc);
			return;
		}

		drawTileAux(row, col, ta, tc, true);

		if (a == ta && c == tc) return;

		drawTileAux(row, col, a, c, false);
	}

	public void clear() {
		if (canvas != null) {
			canvas.drawPaint(back);
		}
	}

	public void noise() {
		if (vibrate) {
			//vibrator.vibrate(50);
		}
	}

	public void onResume() {
		Log.d("Angband", "Termview.onResume()");
		//vibrate = Preferences.getVibrate();
	}

	public void onPause() {
		//Log.d("Angband","Termview.onPause()");
		// this is the only guaranteed safe place to save state
		// according to SDK docs
		state.gameThread.send(GameThread.Request.SaveGame);

		flashText = "";

		stopTimers();
	}
}
