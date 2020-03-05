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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

public class TermView extends View implements OnGestureListener {

	Typeface tfStd;
	Typeface tfTiny;
	Bitmap bitmap;
	Canvas canvas;
	Paint fore;
	Paint back;
	Paint cursor;
	Paint dirZone;

	//	int row = 0;
	//  int col = 0;

	public int cols = 0;
	public int rows = 0;

	public int canvas_width = 0;
	public int canvas_height = 0;

	private int char_height = 0;
	private int char_width = 0;
	private int font_text_size = 0;

	private Vibrator vibrator;
	private boolean vibrate;
	private Handler handler = null;
	private StateManager state = null;
	private GameActivity game_context;

	private GestureDetector gesture;

	private ArrayList<Rect> zones = new ArrayList<>();

	public TermView(Context context) {
		super(context);
		initTermView(context);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
	}

	public TermView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTermView(context);
		handler = ((GameActivity)context).getHandler();
		state = ((GameActivity)context).getStateManager();
	}

	protected void initTermView(Context context) {
		game_context = (GameActivity)context;
		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		if ( isHighRes() ) fore.setAntiAlias(true);
		setForeColor(Color.WHITE);

		back = new Paint();
		setBackColor(Color.BLACK);

		cursor = new Paint();
		cursor.setColor(Color.GREEN);
		cursor.setStyle(Paint.Style.STROKE);
		cursor.setStrokeWidth(0);

		dirZone = new Paint();
		dirZone.setColor(Color.GRAY);
		dirZone.setStyle(Paint.Style.FILL);
		dirZone.setAlpha(40);
		dirZone.setStrokeWidth(0);

		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		setFocusableInTouchMode(true);
		gesture = new GestureDetector(context, this);

		this.cols = Preferences.getTermWidth();
		this.rows = Preferences.getTermHeight();
	}

	protected void drawDirZonesFull(Canvas p_canvas)
	{
	    this.zones.clear();

		int totalw = getWidth();
		int totalh = getHeight() - game_context.getKeyboardOverlapHeight();
		int w = (int)(totalw * 0.20f);
		int h = (int)(totalh * 0.20f);

		int padx = (int)(totalw * 0.03f);
		int pady = (int)(totalh * 0.03f);

		if (h > w) h = w;
		else if (w > h) w = h;

		float pct[] = {0.0f, 0.5f, 1.0f};
		for (int px = 0; px < 3; px++) {
			for (int py = 0; py < 3; py++) {

				int x = (int)(totalw * pct[px]) - w / 2;
				if (x < padx) x = padx;
				if (x + w >= totalw - padx) x = totalw - w - padx;

				int y = (int)(totalh * pct[py]) - h / 2;
				if (y < pady) y = pady;
				if (y + h >= totalh - pady) y = totalh - h - pady;

				p_canvas.drawRect(x, y, x + w - 1, y + h - 1, dirZone);
			}
		}
	}

	protected void drawDirZonesTopRight(Canvas p_canvas)
	{
        this.zones.clear();

		int totalw = getWidth();
		int totalh = getHeight() - game_context.getKeyboardOverlapHeight();
		int w = (int)(totalw * 0.2f);
		int h = (int)(totalh * 0.2f);

		int padx = (int)(totalw * 0.02f);
		int pady = (int)(totalh * 0.02f);

		if (padx < pady) padx = pady;
		if (pady < padx) pady = padx;

		if (h > w) h = w;
		else if (w > h) w = h;

		for (int py = 1; py <= 3; py++) {
			for (int px = 1; px <= 3; px++) {

				int x = totalw - (w + padx) * (3 - px + 1);
				int y = pady + (h + pady) * (1 + py - 2);

                Rect r = new Rect(x, y, x + w - 1, y + h - 1);

                // Remember for single tap
                this.zones.add(r);

                p_canvas.drawRect(r, dirZone);
			}
		}
	}

	protected void onDraw(Canvas canvas) {
		if (bitmap != null) {
			canvas.drawBitmap(bitmap, 0, 0, null);

			int x = state.stdscr.col * (char_width);
			int y = (state.stdscr.row + 1) * char_height;

			// due to font "scrunch", cursor is sometimes a bit too big
			int cl = Math.max(x,0);
			int cr = Math.min(x+char_width,canvas_width-1);
			int ct = Math.max(y-char_height,0);
			int cb = Math.min(y,canvas_height-1);

			if (state.stdscr.cursor_visible) {
				canvas.drawRect(cl, ct, cr, cb, cursor);
			}
		}

		if (Preferences.getEnableTouch()) {
			if (Preferences.getTouchTopRight()) {
				this.drawDirZonesTopRight(canvas);
			}
			else {
				this.drawDirZonesFull(canvas);
			}
		}
	}

	public void computeCanvasSize() {
		canvas_width = Preferences.getTermWidth()*char_width;
	    canvas_height = Preferences.getTermHeight()*char_height;
	}

	protected void setForeColor(int a) {
		fore.setColor(a);
	}
	protected void setBackColor(int a) {
		back.setColor(a);
	}

	public void autoSizeFontByHeight(int maxWidth, int maxHeight) {
		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		setFontFace();

		// HACK -- keep 480x320 fullscreen as-is
		if (!isHighRes()) {
			setFontSizeLegacy();
		}
		else {
			font_text_size = 6;
			do {
				font_text_size += 1;
				setFontSize(font_text_size, false);
			} while (char_height * Preferences.rows <= maxHeight);

			font_text_size -= 1;
			setFontSize(font_text_size);
		}
		Log.d("Angband","autoSizeFontHeight "+font_text_size);
	}

	public void autoSizeFontByWidth(int maxWidth, int maxHeight) {
		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		setFontFace();

		// HACK -- keep 480x320 fullscreen as-is
		if (!isHighRes()) {
			setFontSizeLegacy();
		}
		else {
			font_text_size = 6;
			boolean success = false;
			do {
				font_text_size += 1;
				success = setFontSize(font_text_size, false);
			} while (success && char_width* Preferences.cols <= maxWidth);

			font_text_size -= 1;
			setFontSize(font_text_size);
		}
		Log.d("Angband","autoSizeFontWidth "+font_text_size);
	}

	public void adjustTermSize(int maxWidth, int maxHeight)
	{
        if (!this.state.gameThread.gameRunning()) {
            return;
        }

        this.rows = Preferences.rows;
        this.cols = Preferences.cols;

		if (maxWidth == 0) maxWidth = getMeasuredWidth();
		if (maxHeight == 0) maxHeight = getMeasuredHeight();

		while ((maxHeight > 0) && ((this.rows+1) * this.char_height < maxHeight)) {
			++this.rows;
		}

		while ((maxWidth > 0) && ((this.cols+1) * this.char_width < maxWidth)) {
			++this.cols;
		}

		// Check values
		this.rows = Math.max(this.rows, Preferences.rows);
		this.cols = Math.max(this.cols, Preferences.cols);

		Log.d("Angband", "Resize. maxWidth "+maxWidth
				+" maxHeight "+maxHeight
				+" rows "+this.rows+" cols "+this.cols);

		// Do nothing
		if (this.rows == Preferences.getTermHeight() && this.cols == Preferences.getTermWidth()) {
			return;
		}

        // Remember for later
        Preferences.setSize(this.cols, this.rows);

		if (this.state.stdscr != null) {
            this.state.stdscr.resize(this.cols, this.rows);
        }
        if (this.state.virtscr != null) {
            this.state.virtscr.resize(this.cols, this.rows);
        }

        Log.d("Angband", "Going to core");

		// Tell core we changed dimensions
		this.state.nativew.gameQueryResize(this.cols, this.rows);
		// Hack, add a keyboard event so the core can process the pending redraw event
        this.state.addKey(32); // Space (do nothing)
	}

	public boolean isHighRes() {
		Display display = ((WindowManager) game_context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int maxWidth = display.getWidth();
		int maxHeight = display.getHeight();

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
        this.state.nativew.resize();
	}

	public void decreaseFontSize() {
		setFontSize(font_text_size-1);
		adjustTermSize(0,0);
		this.state.nativew.resize();
	}

	private boolean setFontSize(int size) {
		return setFontSize(size,true);
	}
	private boolean setFontSize(int size, boolean persist) {

		setFontFace();

		if (size < 6) {size = 6; return false;}
		else if (size > 64) {size = 64; return false;}

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
		return true;
	}

	@Override
	protected void onMeasure(int widthmeasurespec, int heightmeasurespec) {
		int height = MeasureSpec.getSize(heightmeasurespec);
		int width = MeasureSpec.getSize(widthmeasurespec);

		int fs = 0;
		if(Preferences.isScreenPortraitOrientation())
			fs = Preferences.getPortraitFontSize();
		else
			fs = Preferences.getLandscapeFontSize();

		if (fs == 0) {
            autoSizeFontByWidth(width, height);
        }
		else {
			setFontSize(fs, false);
		}

		fore.setTextAlign(Paint.Align.LEFT);

		//int minheight = getSuggestedMinimumHeight();
		//int minwidth = getSuggestedMinimumWidth();

		setMeasuredDimension(width, height);

		//Log.d("Angband","onMeasure "+canvas_width+","+canvas_height+";"+width+","+height);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gesture.onTouchEvent(me);
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

		scrollTo(newscrollx, newscrolly);

		return true;
	}
	public boolean onDown(MotionEvent e) {
		return true;
	}
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return true;
	}
	public void onLongPress(MotionEvent e) {
		handler.sendEmptyMessage(AngbandDialog.Action.OpenContextMenu.ordinal());
	}
	public void onShowPress(MotionEvent e) {
	}
	public boolean onSingleTapUp(MotionEvent event) {
		if (!Preferences.getEnableTouch()) return false;

		int x = (int) event.getX();
		int y = (int) event.getY();

		int r = 0, c = 0;

		int w = getWidth();
		int h = (getHeight() - game_context.getKeyboardOverlapHeight());

		if (this.zones.isEmpty()) {
			// Use the whole screen
            c = (x * 3) / w;
            r = (y * 3) / h;
        }
		else {
			// Find the rectangle
		    int i = 0;
		    while (i < this.zones.size() && !this.zones.get(i).contains(x,y)) {
		    	i = i + 1;
			}
		    // Not found
		    if (i >= this.zones.size()) {
				return false;
			}
		    // Get row and col
		    r = i / 3;
		    c = i % 3;
        }

		int key = (2 - r) * 3 + c + '1';

		state.addDirectionKey(key);

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

		// sanity
		if (canvas_width == 0 || canvas_height == 0) return false;

		//Log.d("Angband","createBitmap "+canvas_width+","+canvas_height);
		bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.RGB_565);
		canvas = new Canvas(bitmap);
		/*
		  canvas.setDrawFilter(new PaintFlagsDrawFilter(
		  Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG,0
		  )); // this seems to have no effect, why?
		*/

		state.currentPlugin = Plugins.Plugin.convert(Preferences.getActiveProfile().getPlugin());

		Log.d("Angband", "onGameStart !!!");

   		return true;
	}

	public void drawPoint(int r, int c, char ch, int fcolor, int bcolor, boolean extendedErase) {
		float x = c * char_width;
		float y = r * char_height;

		if (canvas == null) {
			// OnSizeChanged has not been called yet
			//Log.d("Angband","null canvas in drawPoint");
			return;
		}

		setBackColor(bcolor);

		canvas.drawRect(
						x, 
						y, 
						x + char_width + (extendedErase ? 1 : 0), 
						y + char_height + (extendedErase ? 1 : 0), 
						back
						);					

		if (ch != ' ') {
			String str = ch + "";

			setForeColor(fcolor);

			canvas.drawText (
							 str,
							 x, 
							 y + char_height - fore.descent(), 
							 fore
							 );
		}
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
		//Log.d("Angband","Termview.onResume()");
		vibrate = Preferences.getVibrate();
	}

	public void onPause() {
		//Log.d("Angband","Termview.onPause()");
		// this is the only guaranteed safe place to save state 
		// according to SDK docs
		state.gameThread.send(GameThread.Request.SaveGame);
	}
}
