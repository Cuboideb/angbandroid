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
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.util.Size;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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

    Bitmap atlas = null;    
    //BitmapRegionDecoder tiles = null;

	private int color1 = Color.parseColor("#4a4855");
	private int color2 = Color.parseColor("#807c93");
    private int color_drag = Color.parseColor("#B2FB49");
	private int alpha = 70;

	Handler timerHandler;
	Runnable timerRunnable;
	private int lastDirection = 0;
	private int longDelay = 350;
	private int shortDelay = 80;
    private int dragDelay = 200;
	private long savedTime = 0;
    private boolean dragEnabled = false;
    private Point lastLocation = null;
    private Point dragOffset = new Point(0, 0);

	//	int row = 0;
	//  int col = 0;

	public int cols = 0;
	public int rows = 0;

	public int canvas_width = 0;
	public int canvas_height = 0;

	private int char_height = 0;
	private int char_width = 0;
	private int font_text_size = 0;

	public static int ROW_MAP = 1;
	public static int COL_MAP = 13;
    public static char BIG_PAD = 0x1E00;

    public int tile_wid = 1;
    public int tile_hgt = 1;
    public int useGraphics = 0;
    public boolean pseudoAscii = false;    

	public float tile_multiplier = 1f;
    public int tile_wid_pix = 0;
    public int tile_hgt_pix = 0;
    public int tile_font_size = 0;
    public Paint tile_fore;

    public static class Assert {
        public static void that(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }

    public class TSize {

        public int width;
        public int height;

        public TSize(int w, int h) {
            width = w;
            height = h;
        }
    }

    public class TileGrid
    {
        Point srcPoint;
        TSize srcSize;

        Point dstPoint;
        TSize dstSize;

        Point ptInPage;
        Point page;

        GraphicsMode gm;

        boolean isLargeTile = false;

        public TileGrid(GraphicsMode pGM, int pSrcRow, int pSrcCol,
            int pDstRow, int pDstCol)
        {
            gm = pGM;

            srcPoint = new Point(pSrcCol & 0x7F, pSrcRow & 0x7F);

            srcSize = new TSize(gm.cell_width, gm.cell_height);

            dstPoint = new Point(pDstCol, pDstRow);

            dstSize = new TSize(tile_wid * char_width,
                tile_hgt * char_height);

            isLargeTile = false;

            if (gm.isLargeTile(srcPoint.y) && dstPoint.y > 2) {

                isLargeTile = true;

                srcPoint.y -= 1;
                srcSize.height *= 2;

                dstPoint.y -= tile_hgt;
                dstSize.height *= 2;
            }

            page = new Point(0, 0);
            ptInPage = new Point(srcPoint);            

            if (gm.havePages()) {
                ptInPage.x %= gm.pageSize;
                ptInPage.y %= gm.pageSize;                

                page.x = srcPoint.x / gm.pageSize;
                page.y = srcPoint.y / gm.pageSize;
            }
        }
        
        public Rect locateSource()
        {
            int x = ptInPage.x * gm.cell_width;
            int y = ptInPage.y * gm.cell_height;
            return new Rect(x, y, x + srcSize.width,
                y + srcSize.height);
        }

        public Rect locateDest()
        {
            int x = dstPoint.x * char_width;
            int y = dstPoint.y * char_height;
            return new Rect(x, y, x + dstSize.width,
                y + dstSize.height);
        }

        public String createKey()
        {
            return "" + gm.idx + "-" + srcPoint.x + "@" + srcPoint.y + 
                "-" + dstSize.width + "x" + dstSize.height;
        }

        public Bitmap loadBitmap(Bitmap from)
        {
            Rect src = locateSource();

            Bitmap region = Bitmap.createBitmap(from, src.left,
                src.top, src.right - src.left,
                src.bottom - src.top);

            Bitmap result = Bitmap.createScaledBitmap(region,
                dstSize.width, dstSize.height, true);

            region.recycle();

            return result;
        }
    }

    public class GraphicsMode {
        int idx;
        String name;
        String directory;
        String file;        
        int cell_width;
        int cell_height;
        int alphaBlend;
        int overdrawRow;
        int overdrawMax;
        int pageSize;

        public GraphicsMode() {    
        }

        public boolean canDraw() {

            int tw = (char_width * tile_wid);
            int th = (char_height * tile_hgt);
            return (tw > 0 && th > 0);
        }

        public boolean isLargeTile(int row) {
            return overdrawRow != 0
                && row >= overdrawRow
                && row <= overdrawMax;
        }

        public boolean havePages() {

            return pageSize > 0;
        }

        public String getFullPath(Point page)
        {
            String pathToPicture = Preferences.getAngbandFilesDirectory() +
                "/tiles/" + directory + "/" + file;

            if (page != null) {
                pathToPicture = pathToPicture.replace(".png",
                    "-" + page.x + "-" + page.y + ".png");
            }

            return pathToPicture;
        }
    };
    public ArrayList<GraphicsMode> grafmodes = null;
    public GraphicsMode currentGraf = null;
    public String currentAtlas = "";

	private Vibrator vibrator;
	private boolean vibrate;
	private Handler handler = null;
	private StateManager state = null;
	private GameActivity game_context;

	private GestureDetector gesture;

	private ArrayList<RectF> zones = new ArrayList<>();

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

        tile_hgt = Preferences.getTileHeight();
        tile_wid = Preferences.getTileWidth();
        tile_multiplier = tile_hgt;
        //int tempUseGraphics = Preferences.getGraphicsMode();
        pseudoAscii = Preferences.getPseudoAscii();

        createTimers();

        grafmodes = new ArrayList<>();

        if (state.gameThread.gameRunning()) {

            loadGraphics();            
        }

        setGraphicsMode(Preferences.getGraphicsMode());

        //Log.d("Angband", "Init TERM");

        //state.addKey(' ');
	}

    public void createTimers()
    {
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

                    // Special case
                    if (lastDirection == '5') {                        
                        dragEnabled = true;
                        invalidate();
                        return;
                    }

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

    public void loadGraphics()
    {

        String buf = state.nativew.queryString("get_tilesets");

        if (buf == null) return;
        
        for (String reg: buf.split("#")) {

            String parts[] = reg.split(":");

            if (parts.length != 9) continue;

            GraphicsMode gm = new GraphicsMode();

            gm.idx = Integer.parseInt(parts[0]);
            gm.name = parts[1];
            gm.directory = parts[2];
            gm.file = parts[3];
            gm.cell_height = Integer.parseInt(parts[4]);
            gm.cell_width = Integer.parseInt(parts[5]);
            gm.alphaBlend = Integer.parseInt(parts[6]);
            gm.overdrawRow = Integer.parseInt(parts[7]);
            gm.overdrawMax = Integer.parseInt(parts[8]);
            // Shockbolt is paged
            gm.pageSize = (gm.idx != 5) ? 0: 16;

            grafmodes.add(gm);
            
            //game_context.infoAlert(gm.fullPath);
            
            //if (gm.idx == useGraphics) currentGraf = gm;
        }
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
            ":" + (pseudoAscii ? 1: 0);

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

    public GraphicsMode findGraphicsMode(int gidx)
    {
        GraphicsMode gm = null;        

        if (gidx > 0) {
            for (GraphicsMode gx: grafmodes) {
                if (gx.idx == gidx) {
                    gm = gx;
                    break;
                }
            }
        }

        return gm;
    }
   
    public void setGraphicsMode(int gidx)
    {   
        useGraphics = gidx;

        currentGraf = null;        

        if (grafmodes.isEmpty()) return;

        GraphicsMode gm = findGraphicsMode(gidx);
        Bitmap image = null;
        String path = "";

        if (gm != null && !gm.havePages()) {

            path = gm.getFullPath(null);

            if (path.equals(currentAtlas) && atlas != null) {
                image = atlas;
            }
            else {
                image = BitmapFactory.decodeFile(path);
                image.prepareToDraw();
            }            
        }        
        
        currentGraf = gm;        
        atlas = image;
        currentAtlas = path;
    }

    public boolean bigTileActive()
    {
        return (tile_wid > 1) || (tile_hgt > 1);
    }

	public void configureVisuals(int rows, int cols, int graf)
    {
        //Log.d("Angband", "Configure visuals! REACT");
        tile_multiplier = rows;
        tile_hgt = rows;
        tile_wid = cols;
        setGraphicsMode(graf);        
    }

	Point mouseToGrid(int y, int x)
	{
		Point p = new Point();
		p.x = (char_width > 0) ? (x / char_width): 0;
		p.y = (char_height > 0) ? (y / char_height): 0;

        /*
		if (state.virtscr != null) {
			TermWindow.TermPoint u = state.virtscr.buffer[p.y][p.x];
			game_context.infoAlert("Cell: " +
					u.fgColor + " " + (int)u.ch + " " +
					u.bgColor + " " + (int)u.bgChar);
			this.postInvalidate();
		}
        */

		return p;
	}

	public void stopTimer()
    {
        dragEnabled = false;
        //dragOffset.x = 0;
        //dragOffset.y = 0;
        lastLocation = null;

        lastDirection = 0;
        savedTime = 0;
        
        timerHandler.removeCallbacks(timerRunnable);

        invalidate();
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

				int x = this.getScrollX() + dragOffset.x;
				x += totalw - w * (3 - px + 1);				

                int y = this.getScrollY() + dragOffset.y;
                y += totalh - h * (3 - py + 1);

                RectF r = new RectF(x, y, x + w - 1, y + h - 1);

                // Remember for single tap
                this.zones.add(r);

                if (dragEnabled && px == 2 && py == 2) {
                    dirZoneFill.setColor(color_drag);
                }
                else if (px == 2 || py == 2) {
                    dirZoneFill.setColor(color2);
                }
                else {
                    dirZoneFill.setColor(color1);
                }
                dirZoneFill.setAlpha(alpha);

                RectF rdraw = new RectF(r);
                rdraw.bottom -= pady;
                rdraw.right -= padx;				

				p_canvas.drawRoundRect(rdraw, 10, 10, dirZoneFill);
				p_canvas.drawRoundRect(rdraw, 10, 10, dirZoneStroke);
			}
		}

		// Restore
        dirZoneFill.setColor(color1);
        dirZoneFill.setAlpha(alpha);
	}

	protected void onDraw(Canvas p_canvas) {
		if (this.zones != null) this.zones.clear();

		if (bitmap != null && state.stdscr != null) {

			p_canvas.drawBitmap(bitmap, 0, 0, null);            

			int tw = char_width;
			int th = char_height;

			int col = state.stdscr.col;
			int row = state.stdscr.row;

			int x = col * tw;
			int y = row * th;

			if (this.bigTileActive()) {

                TermWindow.TermPoint p = null;

				if (state.virtscr != null) {
					 p = state.virtscr.buffer[row][col];
				}
                
			    if (p != null && (p.isGraphicTile() || p.isBigText())) {

                    tw = tile_wid_pix;
                    th = tile_hgt_pix;
                }
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

			if (Preferences.getEnableTouch()) {
				if (Preferences.getTouchRight()) {
					this.drawDirZonesRight(p_canvas);
				}
				else {
					this.drawDirZonesFull(p_canvas);
				}
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
		//Log.d("Angband","autoSizeFontHeight "+font_text_size);
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
		//Log.d("Angband","autoSizeFontWidth "+font_text_size);
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

		//Log.d("Angband", "Resize. maxWidth "+maxWidth
		//		+" maxHeight "+maxHeight
		//		+" rows "+this.rows+" cols "+this.cols);

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

        //Log.d("Angband", "RESIZE TO CORE");

        String cmd = "resize:" + this.cols + ":" + this.rows;
        state.addSpecialCommand(cmd);
        state.addKey(' ');

        /*
        if (state.characterPlaying()) {

            clearFull();
            state.addKey(' ');
        }
        */
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
        
        // Always double size
        tile_font_size = (int)(font_text_size * 2);
		tile_fore.setTypeface(fore.getTypeface());
		tile_fore.setTextSize(tile_font_size);
        
        tile_wid_pix = (int)(char_width * tile_wid);
        tile_hgt_pix = (int)(char_height * tile_hgt);

        //Log.d("Angband", "set_font_size - tile: " + tile_wid_pix + " " + tile_hgt_pix);

		return true;
	}

	@Override
	protected void onMeasure(int widthmeasurespec, int heightmeasurespec) {

        //Log.d("Angband", "onMeasure!");

		int height = MeasureSpec.getSize(heightmeasurespec);
		int width = MeasureSpec.getSize(widthmeasurespec);

        //Log.d("Angband", "System size " + width + "x" + height);

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

        if (lastLocation == null) {
            lastLocation = new Point(px, py);
            return;
        }

        Point newLocation = new Point(px, py);

        if (newLocation.equals(lastLocation)) return;

        int dy = newLocation.y - lastLocation.y;
        int dx = newLocation.x - lastLocation.x;

        dragOffset.x += dx;
        dragOffset.y += dy;

        //Log.d("Angband", "Delta: " + dx + "@" + dy);

        lastLocation = newLocation;

        if (!dragEnabled) return;

        invalidate();
    }

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		float x = me.getX() + this.getScrollX();
		float y = me.getY() + this.getScrollY();
        int tempDirection = this.getDirFromZone(y, x);

        // If we are dragging, continue
        if ((tempDirection != lastDirection && !dragEnabled) ||
                // Except if it's a reentrant event
                me.getAction() == MotionEvent.ACTION_DOWN) {
            this.stopTimer();
            lastDirection = tempDirection;
        }        

        // Disable special behavior sometimes
        if (lastDirection == '5' && !(Preferences.getEnableTouch() &&
            Preferences.getTouchRight())) {
            lastDirection = 0;
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN && lastDirection > 0) {
            //Log.d("Angband", "DOWN!");

            // Send the action now
            if (lastDirection != '5') {
                timerHandler.postDelayed(timerRunnable, longDelay);
                state.addDirectionKey(lastDirection);
            }
            else {
                timerHandler.postDelayed(timerRunnable, dragDelay);
            }
            return true;
        }

        // Drag the buttons
        if (me.getAction() == MotionEvent.ACTION_MOVE && lastDirection == '5') {

            updateDrag(x, y);

            return true;
        }
        if (me.getAction() == MotionEvent.ACTION_CANCEL && lastDirection > 0) {

            this.stopTimer();
            return true;
        }
	    if (me.getAction() == MotionEvent.ACTION_UP && lastDirection > 0) {

            // It was just a single click
            if (lastDirection == '5' && !dragEnabled) {

                //Log.d("Angband", "Adding delayed '5'");

                state.addDirectionKey('5');
            }

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
		float padPct = 0.2f;

		if (Preferences.getEnableTouch() &&
			Preferences.getTouchRight() &&
			(this.zones.size() == 9)) {
           
            RectF first = this.zones.get(0);
            RectF last = this.zones.get(8);
            float side = first.bottom - first.top;
            float pad = side * padPct;           

            RectF bigZone = new RectF(first.left - pad, first.top - pad,
                last.right + pad, last.bottom + pad);
            if (bigZone.contains(x, y)) {
                return true;
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
		if (canvas_width == 0 || canvas_height == 0) {
            state.saveTheDungeon(null);
            return false;        
        }
        
        bitmap = state.restoreTheDungeon(canvas_width, canvas_height);        
        if (bitmap != null) {
            //Log.d("Angband","Reuse Bitmap!!!");
            ; // nothing            
        }
        else {                        
            //Log.d("Angband","createBitmap "+canvas_width+","+canvas_height);
            bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.RGB_565);
            //bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);    //    
            state.saveTheDungeon(bitmap);
        }
		
		canvas = new Canvas(bitmap);

		/*
		  canvas.setDrawFilter(new PaintFlagsDrawFilter(
		  Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG,0
		  )); // this seems to have no effect, why?
		*/

		state.currentPlugin = Plugins.Plugin.convert(Preferences.getActiveProfile().getPlugin());

		//Log.d("Angband", "onGameStart !!!");

   		return true;
	}


	public void drawPoint(int r, int c, TermWindow.TermPoint p,
        int fcolor, int bcolor, boolean extendedErase) {

		if (canvas == null) {
			// OnSizeChanged has not been called yet
			//Log.d("Angband","null canvas in drawPoint");
			return;
		}

        char ch = p.ch;
        
        if (ch == BIG_PAD) return;

		Paint fore_temp = fore;
		int tw = char_width;
		int th = char_height;
        int x = c * tw;
        int y = r * th;		

		if (p.isBigText() && this.bigTileActive()) {

            fore_temp = tile_fore;
            tw = tile_wid_pix;
            th = tile_hgt_pix;
        }

        ch &= 0xFF;

		setBackColor(bcolor);

		//extendedErase = false;

		canvas.drawRect(x, y,
			x + tw + (extendedErase ? 1 : 0),
			y + th + (extendedErase ? 1 : 0),
			back);

		if (ch != ' ') {

			String str = ch + "";

			fore_temp.setColor(fcolor);

            int w2 = (int)fore_temp.measureText(str, 0, 1);
            int pad = Math.max((tw - w2) / 2, 0);

			canvas.drawText(str, x + pad,
				y + th - fore_temp.descent(),
				fore_temp);
		}
	}

    public void drawText(int row, int col, int a, int c)
    {
        a &= 0x7F;
        c &= 0xFF;

        if (c == ' ') return;

        TermWindow.ColorPair pair = TermWindow.pairs.get(a);
        if (pair == null) return;

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
        setBackColor(Color.BLACK);
        canvas.drawRect(bg, back);        

        fore_temp.setColor(pair.fColor);

        int w2 = (int)fore_temp.measureText(str, 0, 1);
        int pad = Math.max((tw - w2) / 2, 0);

        canvas.drawText(str, x + pad,
            y + th - fore_temp.descent(),
            fore_temp);        
    }

    public void markTile(TermWindow t, int row, int col)
    {
        t.buffer[row][col].bgColor = -255;
    }

    public ArrayList<TileGrid> collectTileGrids(TermWindow t)
    {
        ArrayList<TileGrid> grids = new ArrayList<>();
        TileGrid g;
        String key;

        for (int row = 0; row < t.rows; row++) {
            for (int col = 0; col < t.cols; col++) {
                TermWindow.TermPoint p = t.buffer[row][col];
                if (!p.isGraphicTile()) continue;
                if (p.bgColor < 0) continue;

                g = new TileGrid(currentGraf, p.fgColor, p.ch, row, col);                
                key = g.createKey();                
                if (state.tileCache.get(key) == null) {
                    grids.add(g);
                }
                else {
                    //Log.d("Angband", "Cache hit (preload): " + key);
                }

                g = new TileGrid(currentGraf, p.bgColor, p.bgChar, row, col);                
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
        if (gm == null || !gm.canDraw() || !gm.havePages()) return;
        
        int i;
        HashSet<Point> pages = new HashSet<>();
        ArrayList<TileGrid> grids = collectTileGrids(t);

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
                    map.prepareToDraw();
                    //Log.d("Angband", "Loading page: " + mapPath);
                }
               
                Bitmap result = tile.loadBitmap(map);

                state.tileCache.put(key, result);

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
            atlas.prepareToDraw();

            //Log.d("Angband", "Loading page: " + path);
            
            result = tile.loadBitmap(atlas);

            atlas.recycle();
            atlas = null;
        }
        else {        
            result = tile.loadBitmap(atlas);
        }        

        state.tileCache.put(key, result);

        return result;        
    }

	public void drawTileAux(int row, int col, int a, int c,
        boolean fill)
	{
        if (currentGraf == null || !currentGraf.canDraw()) return;

        if (!currentGraf.havePages() && atlas == null) return;

        TileGrid tile = new TileGrid(currentGraf, a, c, row, col);
		
        if (tile.isLargeTile) {

            //game_context.infoAlert("Big tile! " + row + " " + col);

            markTile(state.virtscr, row - tile_hgt, col);
            markTile(state.virtscr, row, col);
        }

        Rect dst = tile.locateDest();

        if (fill) {
            canvas.drawRect(dst, back);
        }
        
        Bitmap bm = getTile(tile);

        canvas.drawBitmap(bm, dst.left, dst.top, null);
	}

    public void drawTile(int row, int col, int a, char c,
        int ta, char tc) {

        // Not initialized
        if (canvas == null) {            
            return;
        }

        // Pad character
        if (a == 255) {
            return;
        }

        // Ascii ?
        if ((c & 0x80) == 0) {
            drawText(row, col, a, c);
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
