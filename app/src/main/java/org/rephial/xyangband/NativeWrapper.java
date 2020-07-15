package org.rephial.xyangband;

import android.os.CountDownTimer;
import android.util.Log;

public class NativeWrapper {
	// Load native library
	static {
		System.loadLibrary(Plugins.LoaderLib);
	}

	private TermView term = null;
	private StateManager state = null;

	private final String display_lock = "lock";

	public LockWithTimer lockWithTimer = null;

	// Call native methods from library
	native void gameStart(String pluginPath, int argc, String[] argv);
	native int gameQueryInt(int argc, String[] argv);
	native byte[] gameQueryString(int argc, String[] argv);
	native int gameQueryResize(int width, int height);

	public NativeWrapper(StateManager s) {
		state = s;
		lockWithTimer = new LockWithTimer(500);
	}

	public void link(TermView t) {
		synchronized (display_lock) {
			term = t;
		}
	}

	public void unlink()
    {
        synchronized (display_lock) {
            term = null;
        }
    }

	public class LockWithTimer {
		public boolean locked = false;
		public int millis = 0;

		public LockWithTimer(int p_millis)
		{
			millis = p_millis;
		}

		public boolean reserveLock()
		{
			synchronized (this) {
				if (locked) return false;
				locked = true;
			}
			return true;
		}

		public void releaseLock()
		{
			synchronized (this) {
				locked = false;
			}
		}

		public void waitAndRelease()
		{
			int time = millis;
			new CountDownTimer(time, time) {
				@Override
				public void onTick(long millisUntilFinished) {
				}
				@Override
				public void onFinish() {
					releaseLock();
				}
			}.start();
		}
	}

	public String queryString(String what)
	{
		String[] argv = new String[]{what};

		byte[] data = gameQueryString(1, argv);

		if (data == null) {
			return null;
		}

		try {
			String str = new String(data, "UTF-8");
			return str;
		}
		catch (java.io.UnsupportedEncodingException ex) {
			Log.d("Angband", ex.getMessage());
			return null;
		}
	}

	int getch(final int v) {
		state.gameThread.setFullyInitialized();

		//Log.d("Angband", "Getch");

		int key = state.getKey(v);

		return key;
	}

	//this is called from native thread just before exiting
	public void onGameExit() {
		state.handler.sendEmptyMessage(AngbandDialog.Action.OnGameExit.ordinal());
	}

	public boolean onGameStart() {
		synchronized (display_lock) {
		    if (term == null) return false;
			return term.onGameStart();
		}
	}

	public void increaseFontSize() {
        if (term == null) return;

		if (!lockWithTimer.reserveLock()) return;

		synchronized (display_lock) {
			term.increaseFontSize();
			this.resize();
		}
		lockWithTimer.waitAndRelease();
	}

	public void decreaseFontSize() {
        if (term == null) return;

		if (!lockWithTimer.reserveLock()) return;

		synchronized (display_lock) {
			term.decreaseFontSize();
			this.resize();
		}

		lockWithTimer.waitAndRelease();
	}

	public void flushinp() {
		state.clearKeys();
	}

	public void fatal(String msg) {
		synchronized (display_lock) {
			state.fatalMessage = msg;
			state.fatalError = true;
			state.handler.sendMessage(state.handler.obtainMessage(AngbandDialog.Action.GameFatalAlert.ordinal(),0,0,msg));
		}
	}

	public void warn(String msg) {
		synchronized (display_lock) {
			state.warnMessage = msg;
			state.warnError = true;
			state.handler.sendMessage(state.handler.obtainMessage(AngbandDialog.Action.GameWarnAlert.ordinal(),0,0,msg));
		}
	}

	public void resize() {
		Log.d("Angband", "Native resize");
        if (term == null) return;

		synchronized (display_lock) {
			term.onGameStart(); // recalculate TermView canvas dimension			
			frosh(null);
		}
	}

	public void updateNow()
	{
		if (term == null) return;		

		frosh(null);		
	}

	public void wrefresh(int w) {
		//Log.d("Angband", "Native wrefresh");
		
		if (term == null) return;

		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) frosh(t);
		}
	}

	private void frosh(TermWindow w) {
		
		if (w != null) {			
			if (w != state.virtscr && w != state.stdscr) {
				//Log.d("Angband", "Frosh win > 0");

				// Just redraw
				if (state.windowIsVisible(w)) {
					term.postInvalidate();
				}

				return;
			}
			else {
				//Log.d("Angband", "Frosh with w in (stdscr, virtscr)");
			}
		}
		else {
			Log.d("Angband", "Frosh with NULL");
		}

		synchronized(display_lock) {
			/* for forcing a redraw due to an Android event, w should be null */

			TermWindow v = state.virtscr;

			if (w != null) v.overwrite(w);

			if (term == null) {
        	    		v.quiet();
	            		return;
        		}

			/* mark ugly points, i.e. those clobbered by anti-alias overflow */
			for(int c = 0; c<v.cols; c++) {
				for(int r = 0; r<v.rows; r++) {
					TermWindow.TermPoint p = v.buffer[r][c];
					if (p.isDirty || w == null) {

						if (r<v.rows-1) {
							TermWindow.TermPoint u = v.buffer[r+1][c];
							u.isUgly = !u.isDirty;
						}
						if (c<v.cols-1) {
							TermWindow.TermPoint u = v.buffer[r][c+1];
							u.isUgly = !u.isDirty;
						}
						if (c<v.cols-1 && r<v.rows-1) {
							TermWindow.TermPoint u = v.buffer[r+1][c+1];
							u.isUgly = !u.isDirty;
						}						
					}

					if (p.isDirty && (p.ch == TermView.BIG_PAD) &&
						term.bigTileActive()) {

						v.markBigTile(r, c, term.tile_wid, term.tile_hgt);
					}
				}
			}

			term.preloadTiles(v);
		
			for(int r = 0; r<v.rows; r++) {
				for(int c = 0; c<v.cols; c++) {
					TermWindow.TermPoint p = v.buffer[r][c];
					if (p.isDirty || p.isUgly || w == null) {
						if (p.isGraphicTile()) {
							drawTile(r, c, p);
						}			
						else {
							drawPoint(r, c, p, p.isDirty || w == null);
						}						
					}
				}
			}
			
			term.postInvalidate();
		
			if (w == null)
				term.onScroll(null,null,0,0);  // sanitize scroll position
		}
	}

	private void drawTile(int r, int c, TermWindow.TermPoint p)
	{
		if (p.bgColor < 0) return;

		term.drawTile(r, c, p.fgColor, p.ch, p.bgColor, p.bgChar);		

		p.isDirty = false;
		p.isUgly = false;
	}

	private void drawPoint(int r, int c, TermWindow.TermPoint p,
		boolean extendErase)
	{	
		if (p.bgColor < 0) return;	

		int fgColorIdx = p.fgColor;
		int bgColorIdx = p.bgColor;		

		/*
		 * This is a bit hacky.  Angband doesn't actually use colour pairs -
		 * it just sets the foreground colour.  So when we get a background
		 * colour, we get the 'colour pair' for the background colour and use
		 * its foreground.  XXX
		 */
		TermWindow.ColorPair fgCol = TermWindow.pairs.get(fgColorIdx);
		TermWindow.ColorPair bgCol = TermWindow.pairs.get(bgColorIdx);

		if (fgCol != null && bgCol != null) {
			term.drawPoint(r, c, p, fgCol.fColor, bgCol.fColor, extendErase);

			p.isDirty = false;
			p.isUgly = false;
		}
	}

	public void control_msg(final int what, final int n, final byte[] cp)
	{
		String str;

		try {
			str = new String(cp, "UTF-8");
		}
		catch (java.io.UnsupportedEncodingException ex) {
			Log.d("Angband", ex.getMessage());
			return;
		}

		//Log.d("Angband", "Control: " + what + " - " + str);

		synchronized (display_lock) {
			state.controlMsg(what, str);
		}
	}

	public void waddnstr(final int w, final int n, final byte[] cp) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null && state.windowIsVisible(t)) {
				t.cursor_visible = false;
				t.addnstr(n, cp);
			}
		}
	}

	public void waddtile(final int w, final int x, final int y,
		final int a, final int c, final int ta, final int tc)
	{
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null && state.windowIsVisible(t)) {
				t.cursor_visible = false;
				t.addTile(x, y, a, c, ta, tc);
				//t.addTilePad(x, y, term.tile_wid, term.tile_hgt);
			}
		}
	}

	public int mvwinch(final int w, final int r, final int c) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) 
				return t.mvinch(r, c);
			else
				return 0;
		}
	}

	public void init_pair(final int p, final int f, final int b) {
		synchronized (display_lock) {
			TermWindow.init_pair(p,f,b);
		}
	}

	public void init_color(final int c, final int rgb) {
		synchronized (display_lock) {
			TermWindow.init_color(c, rgb);
		}
	}

	public void scroll(final int w) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.scroll();
		}
	}

	public int wattrget(final int w, final int r, final int c) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) 
				return t.attrget(r, c);
			else 
				return 0;
		}
	}

	public void wattrset(final int w, final int a) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.attrset(a);
		}
	}

	public void wbgattrset(final int w, final int a) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.bgattrset(a);
		}
	}

	public void whline(final int w, final byte c, final int n) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.hline((char)c, n);
		}
	}

	public void wclear(final int w) {
		//Log.d("Angband", "Native wclear");
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);			
			if (t != null) t.clear();
		}
	}

	public void wclrtoeol(final int w) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.clrtoeol();
		}
	}

	public void wclrtobot(final int w) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.clrtobot();
		}
	}

	public void noise() {
		synchronized (display_lock) {
			if (term != null) term.noise();
		}
	}

	public void wmove(int w, final int y, final int x) {
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.move(y,x);
		}		
	}

	public void curs_set(final int v) {
		synchronized (display_lock) {
			if (v == 1) {
				state.stdscr.cursor_visible = true;
			} else if (v == 0) {
				state.stdscr.cursor_visible = false;
			}
		}
	}

	public void touchwin (final int w) {
		//Log.d("Angband", "Native touchwin");
		synchronized (display_lock) {
			TermWindow t = state.getWin(w);
			if (t != null) t.touch();
		}		
	}

	public int newwin (final int rows, final int cols, 
						final int begin_y, final int begin_x) {
		synchronized (display_lock) {
			int w = state.newWin(rows,cols,begin_y,begin_x);
			return w;
		}		
	}

	public void delwin (final int w) {
		synchronized (display_lock) {
			state.delWin(w);
		}		
	}

	public void initscr () {
		synchronized (display_lock) {
		}		
	}

	public void overwrite (final int wsrc, final int wdst) {
		synchronized (display_lock) {
			TermWindow td = state.getWin(wdst);
			TermWindow ts = state.getWin(wsrc);
			if (td != null && ts != null) td.overwrite(ts);
		}		
	}

	int getcury(final int w) {
		TermWindow t = state.getWin(w);
		if (t != null) 
			return t.getcury();
		else
			return 0;
	}

	int getcurx(final int w) {
		TermWindow t = state.getWin(w);
		if (t != null) 
			return t.getcurx();
		else
			return 0;
	}

    public int wctomb(byte[] pmb, byte character) {
	    byte[] ba = new byte[1];
	    ba[0] = character;
	    byte[] wc;
	    int wclen = 0;
	    //Log.d("Angband","wctomb("+pmb+","+character+")");
	    try {
		String str = new String(ba, "ISO-8859-1");
		wc = str.getBytes("UTF-8");
		for(int i=0; i<wc.length; i++) {
		    pmb[i] = wc[i];
		    wclen++;
		}
	    } catch(java.io.UnsupportedEncodingException e) {
			Log.d("Angband","wctomb: " + e);
	    }
	    return wclen;
    }

    public int mbstowcs(final byte[] wcstr, final byte[] mbstr, final int max) {
	    //Log.d("Angband","mbstowcs("+wcstr+","+mbstr+","+max+")");
	    try {
		String str = new String(mbstr, "UTF-8");
		//Log.d("Angband", "str = |" + str + "|");
		byte[] wc = str.getBytes("ISO-8859-1");
		//Log.d("Angband", "wc.length = " + wc.length);
		//Log.d("Angband", "wcstr.length = " + wcstr.length);
		int i;
		if(max == 0) {
		    // someone just wants to check the length
		    return wc.length;
		}
		for(i=0; i<wc.length && i<max; i++) {
		    //Log.d("Angband", "i = " + i);
		    wcstr[i] = wc[i];
		}
		return i;
	    } catch(java.io.UnsupportedEncodingException e) {
			Log.d("Angband","mbstowcs: " + e);
	    }
	    return -1;
    }

    public int wcstombs(final byte[] mbstr, final byte[] wcstr, final int max) {
	    //Log.d("Angband","mcstombs("+mbstr+","+wcstr+","+max+")");
	    try {
		String str = new String(wcstr, "ISO-8859-1");
		//Log.d("Angband", "str = |" + str + "|");
		byte[] mb = str.getBytes("UTF-8");
		//Log.d("Angband", "mb.length = " + mb.length);
		//Log.d("Angband", "mbstr.length = " + mbstr.length);
		int i;
		if(max == 0) {
		    // someone just wants to check the length
		    return mb.length;
		}
		for(i=0; i<mb.length && i<max; i++) {
		    //Log.d("Angband", "i = " + i);
		    mbstr[i] = mb[i];
		}
		return i;
	    } catch(java.io.UnsupportedEncodingException e) {
			Log.d("Angband","wcstombs: " + e);
	    }
	    return -1;
    }

    ScoreContainer curScore;

    public void score_submit(final byte[] score, final byte[] level) {
		if (curScore == null) curScore = new ScoreContainer();
		curScore.score = 0.0;
		curScore.level = 0;
		try {
			String strScore = new String(score, "UTF-8");
			String strLevel = new String(level, "UTF-8");
			//Log.d("Angband","score = \"" + strScore + "\"");

			curScore.score = Double.parseDouble(strScore.trim());
			curScore.level = Integer.parseInt(strLevel.trim());
		} catch(java.io.UnsupportedEncodingException e) {
			Log.d("Angband","score: " + e);
		}
		state.handler.sendMessage(state.handler.obtainMessage(AngbandDialog.Action.Score.ordinal(), 0, 0, curScore));
    }

    public void score_start() {
	    curScore = new ScoreContainer();
    }

    public void score_detail(final byte[] name, final byte[] value) {
		try {
			String strName = new String(name, "UTF-8");
			String strValue = new String(value, "UTF-8");
			// Log.d("Angband","score detail: \"" + strName + "\" = \"" +
			// 	  strValue + "\"");
			curScore.map.put(strName, strValue.trim());
		} catch(java.io.UnsupportedEncodingException e) {
			Log.d("Angband","score: " + e);
		}
    }
}
