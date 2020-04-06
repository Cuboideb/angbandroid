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
import android.graphics.Point;
import android.graphics.RectF;
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
	Paint dirZoneFill;
	Paint dirZoneStroke;
	Display fullDisplay;
	Display windowDisplay;

	public boolean drawRight = true;

	private int color1 = Color.parseColor("#4a4855");
	private int color2 = Color.parseColor("#807c93");
	private int alpha = 70;

	Handler timerHandler;
	Runnable timerRunnable;
	private int lastDirection = 0;
	private int longDelay = 350;
	private int shortDelay = 80;
	private long savedTime = 0;

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

	private ArrayList<RectF> zones = new ArrayList<>();

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
		dirZoneStroke.setColor(0xDDDDDD);
		dirZoneStroke.setStyle(Paint.Style.STROKE);
		dirZoneStroke.setAlpha(alpha);
		dirZoneStroke.setStrokeWidth(2);
		dirZoneStroke.setStrokeCap(Paint.Cap.ROUND);

		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		setFocusableInTouchMode(true);
		gesture = new GestureDetector(context, this);

		this.cols = Preferences.getTermWidth();
		this.rows = Preferences.getTermHeight();

		this.drawRight = Preferences.getDrawTouchRight();

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                //Log.d("Angband", "CLICK!");
                if (lastDirection == 0) {
                    stopTimer();
                }
                else {
                    timerHandler.removeCallbacks(this);

                    long currTime = System.currentTimeMillis();
                    long delta = 0;
                    if (savedTime > 0 && currTime > savedTime) {
                        delta = currTime - savedTime;
                    }
                    savedTime = currTime;
                    delta = Math.min(delta, longDelay);
                    int effectiveDelay = (int)Math.max(delta, shortDelay);
                    //Log.d("Angband", "Delay: " + effectiveDelay);
                    timerHandler.postDelayed(this, effectiveDelay);

                    state.addKey(lastDirection);
                }
            }
        };
	}

	public void toggleDrawRight()
	{
		this.drawRight = !this.drawRight;
		Preferences.setDrawTouchRight(this.drawRight);
		this.invalidate();
	}

	Point mouseToGrid(int y, int x)
	{
		Point p = new Point();
		p.x = (char_width > 0) ? (x / char_width): 0;
		p.y = (char_height > 0) ? (y / char_height): 0;
		return p;
	}

	public void stopTimer()
    {
        lastDirection = 0;
        savedTime = 0;
        timerHandler.removeCallbacks(timerRunnable);
    }

	protected void drawDirZonesFull(Canvas p_canvas)
	{
	    this.zones.clear();

		int totalw = getWidth();
		int totalh = getHeight() - game_context.getKeyboardHeight();
		int w = (int)(totalw * 0.25f);
		int h = (int)(totalh * 0.25f);

		int padx = (int)(totalw * 0.01f);
		int pady = (int)(totalh * 0.01f);

		if (h > w) h = w;
		else if (w > h) w = h;

		float pct[] = {0.0f, 0.5f, 1.0f};
        for (int py = 1; py <= 3; py++) {
		    for (int px = 1; px <= 3; px++) {

				int x = (int)(totalw * pct[px-1]) - w / 2;
				if (x < padx) x = padx;
				if (x + w >= totalw - padx) x = totalw - w - padx;

				x += this.getScrollX();

				int y = (int)(totalh * pct[py-1]) - h / 2;
				if (y < pady) y = pady;
				if (y + h >= totalh - pady) y = totalh - h - pady;

				y += this.getScrollY();

                RectF r = new RectF(x, y, x + w - 1, y + h - 1);

                // Remember for single tap
                this.zones.add(r);

				p_canvas.drawRoundRect(r, 10, 10, dirZoneFill);
				p_canvas.drawRoundRect(r, 10, 10, dirZoneStroke);
			}
		}
	}

	protected void drawDirZonesRight(Canvas p_canvas)
	{
	    this.zones.clear();

		int totalw = getWidth();
		int totalh = getHeight() - game_context.getKeyboardHeight();

		int w = (int)(totalw * 0.10f);
		// Get the height of the activity window
		Point winSize = new Point();
		windowDisplay.getSize(winSize);
		int h = (int)(winSize.y * 0.10f);

		int padx = (int)(totalw * 0.01f);
		int pady = (int)(totalh * 0.01f);

		if (padx < pady) padx = pady;
		if (pady < padx) pady = padx;

		//if (h > w) h = w;
		//else if (w > h) w = h;

		w = Math.min(w, h);
		h = w = ((100 + Preferences.getTouchMultiplier()) * w) / 100;

		for (int py = 1; py <= 3; py++) {
			for (int px = 1; px <= 3; px++) {

				int x = this.getScrollX();
				if (drawRight) {
					x += totalw - w * (3 - px + 1);
				}
				else {
					x += (px - 1) * w;
				}

                int y = this.getScrollY() +
						totalh - h * (3 - py + 1);

                RectF r = new RectF(x, y, x + w - 1, y + h - 1);

                // Remember for single tap
                this.zones.add(r);

                if (px == 2 || py == 2) {
                    dirZoneFill.setColor(color2);
                }
                else {
                    dirZoneFill.setColor(color1);
                }
                dirZoneFill.setAlpha(alpha);

                RectF rdraw = new RectF(r);
                rdraw.bottom -= pady;
                if (drawRight) {
					rdraw.right -= padx;
				}
                else {
                	rdraw.left += padx;
				}

				p_canvas.drawRoundRect(rdraw, 10, 10, dirZoneFill);
				p_canvas.drawRoundRect(rdraw, 10, 10, dirZoneStroke);
			}
		}

		// Restore
        dirZoneFill.setColor(color1);
        dirZoneFill.setAlpha(alpha);
	}

	protected void onDraw(Canvas canvas) {
	    this.zones.clear();

		if (bitmap != null) {
			canvas.drawBitmap(bitmap, 0, 0, null);

			int x = state.stdscr.col * (char_width);
			int y = (state.stdscr.row + 1) * char_height;

			// due to font "scrunch", cursor is sometimes a bit too big
			int cl = Math.max(x,0);
			int cr = Math.min(x+char_width,canvas_width-1);
			int ct = Math.max(y-char_height,0);
			int cb = Math.min(y,canvas_height-1);

			// Dont draw the cursor if we are using the timer
			if (state.stdscr.cursor_visible && savedTime == 0) {
				canvas.drawRect(cl, ct, cr, cb, cursor);
			}
		}

		if (Preferences.getEnableTouch()) {
			if (Preferences.getTouchRight()) {
				this.drawDirZonesRight(canvas);
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

		if (!Preferences.getKeyboardOverlap()) {
            maxHeight -= game_context.getKeyboardHeight();
        }

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

        if (!Preferences.getKeyboardOverlap()) {
            maxHeight -= game_context.getKeyboardHeight();
        }

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

        if (!Preferences.getKeyboardOverlap()) {
            maxHeight -= game_context.getKeyboardHeight();
        }

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
		float x = me.getX() + this.getScrollX();
		float y = me.getY() + this.getScrollY();
        int tempDirection = this.getDirFromZone(y, x);
        if (tempDirection != lastDirection) {
            this.stopTimer();
            lastDirection = tempDirection;
        }
        // Ignore center tap
        if (lastDirection == '5') {
        	lastDirection = 0;
		}
        if (me.getAction() == MotionEvent.ACTION_DOWN && lastDirection > 0) {
            //Log.d("Angband", "DOWN!");
            timerHandler.postDelayed(timerRunnable, longDelay);
            state.addDirectionKey(lastDirection);
            return true;
        }
	    if (me.getAction() == MotionEvent.ACTION_UP && lastDirection > 0) {
	        this.stopTimer();
	        //Log.d("Angband", "UP!");
	        return true;
        }
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

	public boolean inZoneOfDirectionals(float y, float x)
	{
		if (Preferences.getEnableTouch() &&
			Preferences.getTouchRight() &&
			(this.zones.size() > 0)) {

			if (drawRight) {
				RectF rect = this.zones.get(0);
				float side = rect.bottom - rect.top + 1;
				float pad = side * 0.5f;
				if (x >= rect.left - pad &&
						y >= rect.top - pad) {
					return true;
				}
			}
			else {
				RectF rect = this.zones.get(2);
				float side = rect.bottom - rect.top + 1;
				float pad = side * 0.5f;
				if (x <= rect.right + pad &&
						y >= rect.top - pad) {
					return true;
				}
			}
        }
		return false;
	}

	public void onLongPress(MotionEvent e) {
		// Running ?
	    if (lastDirection > 0) {
	        return;
        }

	    float y = e.getY() + this.getScrollY();
	    float x = e.getX() + this.getScrollX();

		// Too close to directionals
		if (this.inZoneOfDirectionals(y, x)) {
			return;
		}

	    // Directional button, do nothing
	    if (this.getDirFromZone(y, x) > 0) {
	        return;
        }

		handler.sendEmptyMessage(AngbandDialog.Action.OpenContextMenu.ordinal());
	}
	public void onShowPress(MotionEvent e) {
	}

	public int getDirFromZone(float y, float x) {
	    int r, c;
        // Find the rectangle
        int i = 0;
        while (i < this.zones.size() && !this.zones.get(i).contains(x,y)) {
            i = i + 1;
        }
        // Not found
        if (i >= this.zones.size()) {
            return 0;
        }
        // Get row and col
        r = i / 3;
        c = i % 3;
        int key = (2 - r) * 3 + c + '1';
        return key;
    }

    public void sendMousePress(int y, int x, int button) {
        Point p = mouseToGrid(y, x);
        //Log.d("Angband", "y x " + p.y + " " + p.x);
        state.addMousePress(p.y, p.x, button);
    }

	public boolean onSingleTapUp(MotionEvent event) {
		// Running ?
	    if (lastDirection > 0) {
	        return true;
        }

		int x = (int)event.getX() + this.getScrollX();
		int y = (int)event.getY() + this.getScrollY();
		int key = 0;

		if (Preferences.getEnableTouch() &&
				!this.zones.isEmpty()) {

			key = this.getDirFromZone(y, x);

			if (key != 0) {
				state.addDirectionKey(key);
				return true;
			}
		}

		// Too close to directionals
		if (this.inZoneOfDirectionals(y, x)) {
			// Do nothing
			return true;
		}

        sendMousePress(y, x, 1);
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

		this.stopTimer();
	}
}
