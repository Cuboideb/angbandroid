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
import android.util.LruCache;
import android.util.Size;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

public class TermView extends View implements OnGestureListener {

	Typeface tfStd;
	Typeface tfTiny;
	Bitmap bitmap;
	Canvas canvas;
	Paint fore;
    Paint fore_subw;
    Paint fore_topbar;
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
    private int color3 = Color.parseColor("#4d7ed6");
    private int color_drag = Color.parseColor("#B2FB49");
	private int alpha = 70;

    private int color1_stroke = Color.parseColor("#DDDDDD");
    private int color2_stroke = Color.parseColor("#4d7ed6");
    private int alpha_stroke = 200;

	Handler timerHandler;
	private int lastDirection = 0;

	private static int longRepeatDelay = 350;
	private static int shortRepeatDelay = 80;
    private static int dragDelay = 250;
    private static int longPressDelay = 700;
	private long savedTime = 0;
    private boolean dragEnabled = false;
    private Point lastLocation = null;
    private Point dragOffset = new Point(0, 0);

    public static int LONG_PRESS = 1;
    public static int DRAGGING = 2;
    public static int REPEAT_DIR = 3;
    public static int FLASH_TEXT = 4;

    private int lastEvent = -1000;
    private int curEvent = -1000;

	//	int row = 0;
	//  int col = 0;

	public int cols = 0;
	public int rows = 0;

	public int canvas_width = 0;
	public int canvas_height = 0;

	private int char_height = 0;
	private int char_width = 0;
	private int font_text_size = 0;

    public static char BIG_PAD = 0x1E00;

    public static int MIN_FONT = 6;
    public static int MAX_FONT = 64;

    public int tile_wid = 1;
    public int tile_hgt = 1;
    public int useGraphics = 0;
    public boolean pseudoAscii = false;

    public int tile_wid_pix = 0;
    public int tile_hgt_pix = 0;
    public Paint tile_fore;

    public boolean mouseSpecial = false;
    public RectF mouseToggle = null;
    public Bitmap iconMouse1 = null;
    public Bitmap iconMouse2 = null;

    public boolean topBar = true;
    public String flashText = "";

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
        //Point _pt;
        Point srcPoint;
        TSize srcSize;

        Point dstPoint;
        TSize dstSize;

        Point ptInPage;
        Point page;

        GraphicsMode gm;

        boolean isLargeTile = false;

        String key = "";

        public TileGrid(GraphicsMode pGM, int pSrcRow, int pSrcCol,
            int pDstRow, int pDstCol)
        {
            gm = pGM;

            //_pt = new Point(pSrcCol, pSrcRow);

            srcPoint = new Point(pSrcCol & 0x7F, pSrcRow & 0x7F);

            srcSize = new TSize(gm.cell_width, gm.cell_height);

            dstPoint = new Point(pDstCol, pDstRow);

            dstSize = new TSize(tile_wid * char_width,
                tile_hgt * char_height);

            isLargeTile = false;

            if (gm.isLargeTile(srcPoint.y, srcPoint.x) && dstPoint.y > 2) {

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
            if (key == "") {
                key = "" + gm.idx + "-" + srcPoint.x + "@" + srcPoint.y +
                    "-" + dstSize.width + "x" + dstSize.height;
            }
            return key;
        }

        public Bitmap loadBitmap(Bitmap from)
        {
            Rect src = locateSource();

            Bitmap result = null;

            if (src.left < 0 || src.top < 0) return null;

            int sw = src.right - src.left;
            int sh = src.bottom - src.top;

            if (sw <= 0 || src.left + sw > from.getWidth()) return null;
            if (sh <= 0 || src.top + sh > from.getHeight()) return null;

            if (dstSize.width <= 0 || dstSize.height <= 0) return null;

            try {
                Bitmap region = Bitmap.createBitmap(from, src.left,
                        src.top, sw, sh);

                result = Bitmap.createScaledBitmap(region,
                        dstSize.width, dstSize.height, true);

                region.recycle();
            }
            catch (java.lang.IllegalArgumentException ex) {
                result = null;
            }

            return result;
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

	private ArrayList<RectF> zones = new ArrayList<>();

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
        pseudoAscii = Preferences.getPseudoAscii();
        topBar = Preferences.getTopBar();

        iconMouse1 = BitmapFactory.decodeResource(game_context.getResources(),
                R.drawable.mouse1);
        iconMouse2 = BitmapFactory.decodeResource(game_context.getResources(),
                R.drawable.mouse2);

        createTimers();

        reloadGraphics();

        //Log.d("Angband", "Init TERM");

        //state.addKey(' ');
	}

    public void resetDragOffset()
    {
        Preferences.setTouchDragOffset(0, 0);
        dragOffset.x = 0;
        dragOffset.y = 0;
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

    public int COL_MAP()
    {
        return Preferences.getTopBar() ? 0: 13;
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

                    onLongPress(null);
                }

                if (msg.what == DRAGGING) {

                    dragEnabled = true;

                    invalidate();
                }

                if (msg.what == REPEAT_DIR) {

                    // Keep repeating

                    // Set bounds
                    delta = Math.min(delta, longRepeatDelay);
                    int effectiveDelay = (int)Math.max(delta, shortRepeatDelay);

                    //Log.d("Angband", "Delay: " + effectiveDelay);

                    timerHandler.sendEmptyMessageDelayed(REPEAT_DIR, effectiveDelay);

                    // Send action
                    state.addDirectionKey(lastDirection);
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
            ":" + (pseudoAscii ? 1: 0) +
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

    public GraphicsMode findGraphicsMode(int gidx)
    {
        GraphicsMode gm = null;

        if (gidx > 0) {
            for (GraphicsMode gx: state.grafmodes) {
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
        if (state.grafmodes.isEmpty()) {
            useGraphics = gidx;
            return;
        }

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

        useGraphics = gidx;
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
        tile_hgt = rows;
        tile_wid = cols;
        setGraphicsMode(graf);
    }

    Point mouseToGrid(int y, int x)
    {
        Point p = new Point();
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
			//this.postInvalidate();
		}
        */

		return p;
	}

	public void stopTimers()
    {
        dragEnabled = false;
        //dragOffset.x = 0;
        //dragOffset.y = 0;
        lastLocation = null;

        lastDirection = 0;
        savedTime = 0;

        timerHandler.removeMessages(REPEAT_DIR);
        timerHandler.removeMessages(LONG_PRESS);
        timerHandler.removeMessages(DRAGGING);

        invalidate();
    }

    /*
	protected void drawDirZonesFull(Canvas p_canvas)
	{
	    this.zones.clear();

		int totalw = getWidth() - getHorizontalGap();
		int totalh = getHeight() - getVerticalGap();
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

				x += this.getScrollX() + getHorizontalGap();

				int y = (int)(totalh * pct[py-1]) - h / 2;
				if (y < pady) y = pady;
				if (y + h >= totalh - pady) y = totalh - h - pady;

				y += this.getScrollY();

                RectF r = new RectF(x, y, x + w - 1, y + h - 1);

                // Remember for single tap
                this.zones.add(r);

                dirZoneFill.setColor(color1);
                dirZoneFill.setAlpha(alpha);

                dirZoneStroke.setColor(color1_stroke);
                dirZoneStroke.setAlpha(alpha);

				p_canvas.drawRoundRect(r, 10, 10, dirZoneFill);
				p_canvas.drawRoundRect(r, 10, 10, dirZoneStroke);
			}
		}
	}
    */

    public boolean largeCanvas()
    {
        return canvas_width > getWidth() || canvas_height > getHeight();
    }

    protected void drawDirZonesFull(Canvas p_canvas)
    {
        this.zones.clear();

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

                RectF r = new RectF(x, y, x + pctw[px], y + pcth[py]);

                // Remember for single tap
                this.zones.add(r);

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

        if (dragEnabled && largeCanvas()) {
            dirZoneFill.setColor(color_drag);
            dirZoneFill.setAlpha(alpha);
            float cx = x0 + pctw[0];
            float cy = y0 + pcth[0];
            RectF re = new RectF(cx, cy, cx + pctw[1], cy + pcth[1]);
            p_canvas.drawRect(re, dirZoneFill);
        }
    }

	protected void drawDirZonesRight(Canvas p_canvas)
	{
	    this.zones.clear();

		// Get the height of the activity window
		Point winSize = new Point();
		windowDisplay.getSize(winSize);
        int w = (int)(winSize.x * 0.10f);
		int h = (int)(winSize.y * 0.10f);

        int totalw = getWidth();
        int totalh = getHeight() - getVerticalGap();

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

                dirZoneStroke.setColor(color1_stroke);
                dirZoneStroke.setAlpha(alpha_stroke);

                boolean centerButton = (lastDirection == '5');

                if (dragEnabled && centerButton && px == 2 && py == 2) {
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

    public TSize getCharDimensions(Paint p)
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

        int x = (int)(getWidth() * 0.6f) + getScrollX();
        int y = 15 + getScrollY();
        //TSize size = calculateButtonSize();

        int size = (int)(Math.min(winSize.x, winSize.y) * 0.1f);
        size = Math.min(icon.getWidth(), size);

        //size = icon.getWidth();

        mouseToggle = new RectF(x, y, x+size, y+size);

        p_canvas.drawBitmap(icon, null, mouseToggle, null);
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

	protected void onDraw(Canvas p_canvas) {
		if (this.zones != null) this.zones.clear();

		if (bitmap != null && state.stdscr != null) {

            int origin_y = getTopBarHeight();

			p_canvas.drawBitmap(bitmap, 0, origin_y, null);

			int tw = char_width;
			int th = char_height;

			int col = state.stdscr.col;
			int row = state.stdscr.row;

			int x = col * tw;
			int y = origin_y + row * th;

			if (this.bigTileActive()) {

                /*
                TermWindow.TermPoint p = null;

				if (state.virtscr != null) {
					 p = state.virtscr.buffer[row][col];
				}

			    if (p != null && (p.isGraphicTile() || p.isBigText())) {

                    tw = tile_wid_pix;
                    th = tile_hgt_pix;
                }
                */

                if (state.stdscr.big_cursor) {
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

            if (origin_y > 0 && state.characterPlaying()) {
                drawTopBar(p_canvas);
            }

            drawAllSubWindows(p_canvas);

			if (useSmallDPad()) {
				this.drawDirZonesRight(p_canvas);
			}
			else if (useFullDPad()) {
				this.drawDirZonesFull(p_canvas);
			}

			drawMouseToggle(p_canvas);

            drawFlashText(p_canvas);
		}
        else {
            p_canvas.drawColor(Color.BLACK);
        }
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
            && Preferences.getTopBar()) {

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

        if (lastLocation == null) {
            lastLocation = new Point(px, py);
            return;
        }

        if (!dragEnabled) return;

        Point newLocation = new Point(px, py);

        int dy = newLocation.y - lastLocation.y;
        int dx = newLocation.x - lastLocation.x;

        //Log.d("Angband", "Delta: " + dx + "@" + dy);

        int maxDist = 10;

        // Finetune the movement if we are already moving
        if (!timerHandler.hasMessages(LONG_PRESS)) maxDist = 2;

        // To disable long press, measure distance
        if ((dx * dx) + (dy * dy) > (maxDist * maxDist)) {

            // Save new location
            lastLocation = newLocation;

            // Disable Long Press Message
            timerHandler.removeMessages(LONG_PRESS);

            if (lastDirection == 0) {
                onScroll(null, null, dx, dy);
            }
            else if (lastDirection == '5') {
                dragOffset.x += dx;
                dragOffset.y += dy;
            }

            invalidate();
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

        float x = me.getX() + this.getScrollX();
        float y = me.getY() + this.getScrollY();

        int tempDirection = this.getDirFromZone(y, x);

        lastEvent = curEvent;
        curEvent = me.getAction();

        // Restore state if:
        // 1. Reentrant event
        // 2. Different dir (unless dragging)
        if (curEvent == MotionEvent.ACTION_DOWN
            || (!dragEnabled && tempDirection != lastDirection)) {

            stopTimers();

            lastDirection = tempDirection;

            lastLocation = new Point((int)x, (int)y);
        }

        if (curEvent == MotionEvent.ACTION_DOWN) {
            //Log.d("Angband", "DOWN!");

            // Start dragging
            if (lastDirection == 0)  {
                timerHandler.sendEmptyMessageDelayed(DRAGGING, dragDelay);
                timerHandler.sendEmptyMessageDelayed(LONG_PRESS, longPressDelay);

            }
            // Delay execution
            else if (lastDirection == '5' && canMoveDPad()) {
                timerHandler.sendEmptyMessageDelayed(DRAGGING, dragDelay);
            }
            // Send the action now
            else if (lastDirection != '5') {
                state.addDirectionKey(lastDirection);
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

            timerHandler.removeMessages(REPEAT_DIR);
            timerHandler.removeMessages(LONG_PRESS);
            timerHandler.removeMessages(DRAGGING);

            // It was just a single click
            if (!dragEnabled && lastDirection == 0) {
                // On full dpad, simulate the center key
                if (useFullDPad()) {
                    state.addDirectionKey('5');
                }
                // Just single click
                else {
                    onSingleTapUp(me);
                }
            }

            if (!dragEnabled && lastDirection == '5')  {
                state.addDirectionKey(lastDirection);
            }

            // Save offset for later
            if (lastEvent == MotionEvent.ACTION_MOVE
                && lastDirection == '5' && dragEnabled) {
                Preferences.setTouchDragOffset(dragOffset.x, dragOffset.y);
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

	public boolean inZoneOfDirectionals(float y, float x)
	{
		float padPct = 0.2f;

		if (useSmallDPad() && (this.zones.size() == 9)) {

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

	    //float y = e.getY() + this.getScrollY();
	    //float x = e.getX() + this.getScrollX();

        float x = 0;
        float y = 0;

        if (lastLocation != null) {
            x = lastLocation.x;
            y = lastLocation.y;
        }

        // Have DPad ?
        if (Preferences.getEnableTouch()) {
    		// Too close to directionals
    		if (this.inZoneOfDirectionals(y, x)) {
    			return;
    		}

    	    // Directional button, do nothing
    	    if (this.getDirFromZone(y, x) > 0) {
    	        return;
            }
        }

		displayContextMenu();
	}
	public void onShowPress(MotionEvent e) {
	}

	public int getDirFromZone(float y, float x) {

        if (!Preferences.getEnableTouch()) return 0;

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

        // Special case. Transform to "term" zone
        if (key == '5' && useFullDPad()) return 0;

        return key;
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

        x += getScrollX();

        if (x >= canvas_width - getSubWindowsWidth()) {
            return true;
        }

        y += getScrollY();

        if (y < getTopBarHeight()) {
            return true;
        }

        if (y >= canvas_height - getSubWindowsHeight()) {
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
		// Running ?
	    if (lastDirection > 0) {
	        return true;
        }

		int x = (int)event.getX() + this.getScrollX();
		int y = (int)event.getY() + this.getScrollY();

		if (mouseToggle != null && mouseToggle.contains(x,y)) {
		    mouseSpecial = !mouseSpecial;
		    invalidate();
		    return true;
        }

		int key = 0;

		if (Preferences.getEnableTouch() &&
				!this.zones.isEmpty()) {

			key = this.getDirFromZone(y, x);

			if (key != 0) {
				state.addDirectionKey(key);
				return true;
			}

            // Too close to directionals
            if (this.inZoneOfDirectionals(y, x)) {
                // Do nothing
                return true;
            }
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

        if (useGraphics != 0) {
	       extendedErase = false;
        }

		canvas.drawRect(x, y,
			x + tw + (extendedErase ? 1 : 0),
			y + th + (extendedErase ? 1 : 0),
			back);

		if (ch != ' ') {

			String str = ch + "";

			fore_temp.setColor(fcolor);

            float w2 = fore_temp.measureText(str, 0, 1);
            float pad = Math.max((tw - w2) / 2, 0);
            float h2 = fore_temp.descent() - fore_temp.ascent();
            float pady = Math.max((th - h2) / 2, 0) + fore_temp.descent();

			canvas.drawText(str, x + pad, y + th - pady, fore_temp);
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

        float w2 = fore_temp.measureText(str, 0, 1);
        float padx = Math.max((tw - w2) / 2, 0);
        float h2 = fore_temp.descent() - fore_temp.ascent();
        float pady = Math.max((th - h2) / 2, 0) + fore_temp.descent();

        canvas.drawText(str, x + padx, y + th - pady, fore_temp);
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

                g = new TileGrid(gm, p.fgColor, p.ch, row, col);
                key = g.createKey();
                if (state.tileCache.get(key) == null) {
                    grids.add(g);
                }
                else {
                    //Log.d("Angband", "Cache hit (preload): " + key);
                }

                g = new TileGrid(gm, p.bgColor, p.bgChar, row, col);
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
            if (atlas == null) return null;
            atlas.prepareToDraw();

            //Log.d("Angband", "Loading page: " + path);

            result = tile.loadBitmap(atlas);

            atlas.recycle();
            atlas = null;
        }
        else {
            if (atlas == null) return null;
            result = tile.loadBitmap(atlas);
        }

        if (result != null) {
            state.tileCache.put(key, result);
        }

        return result;
    }

    public void drawLifeColor(int a, Rect dst)
    {
        // Mask is 0x02000
        int pct = (a >> 8) & 0x0F;
        int color;

        if (pct <= 2) color = Color.RED;
        else if (pct <= 4) color = 0x0FF4040; // Light red
        else if (pct <= 6) color = 0x0FF8000; // Orange
        else if (pct <= 8) color = Color.YELLOW;
        else return; // White, do nothing

        back.setColor(color);
        back.setAlpha(90);

        canvas.drawRect(dst, back);

        back.setColor(Color.BLACK);
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

	public void drawTileAux(int row, int col, int a, int c,
        boolean fill)
	{
        if (currentGraf == null || !canDraw()) return;

        if (!currentGraf.havePages() && atlas == null) return;

        TileGrid tile = new TileGrid(currentGraf, a, c, row, col);

        if (tile.isLargeTile) {

            //game_context.infoAlert("Big tile! " + row + " " + col);

            markTile(state.virtscr, row, col, tile_hgt, tile_wid);
        }

        Rect dst = tile.locateDest();

        // Optimization: just clear grids with alpha
        if (fill && tileHasAlpha(tile)) {
            setBackColor(Color.BLACK);
            canvas.drawRect(dst, back);
        }

        Bitmap bm = getTile(tile);

        if (bm == null) return;

        canvas.drawBitmap(bm, dst.left, dst.top, null);

        // Special mark for life color
        if ((a & 0x02000) != 0) drawLifeColor(a, dst);
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
