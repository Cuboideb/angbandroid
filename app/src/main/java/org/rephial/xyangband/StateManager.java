package org.rephial.xyangband;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.graphics.Bitmap;
import android.util.LruCache;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

public class StateManager {
	/* screen state */
	public Map<Integer, TermWindow> termwins = null;
	public TermWindow virtscr = null;
	public TermWindow stdscr = null;
	public int termWinNext = 0;

	/* alert dialog state */
	public boolean fatalError = false;
	public boolean warnError = false;
	public String fatalMessage = "";
	public String warnMessage = "";
	public Plugins.Plugin currentPlugin;

	public Bitmap savedDungeon = null;

	/* progress dialog state */
	public static String progress_lock = "lock";

	/* keybuffer */
	private KeyBuffer keyBuffer = null;

	/* native angband library interface */
	public NativeWrapper nativew = null;

	public GameActivity context = null;

	/* game thread */
	public GameThread gameThread = null;

	/* game thread */
	public Handler handler = null;

	/* running mode */
	private boolean runningMode = false;

	public LruCache<String, Bitmap> tileCache = null;
    public static int CACHE_MAX = 5;

    public static int MAX_WINDOWS = 5;
    public static int TOP_BAR_WIN = 4;

    public boolean showSubWindows = true;

    public ArrayList<GraphicsMode> grafmodes = null;

    public class CoreCommand
    {
    	int[] keys = new int[2];
    	String desc;

    	public int getKey()
    	{
    		return isRoguelikeKeyboard() ? keys[1]: keys[0];
    	}
    }

    public ArrayList<CoreCommand> coreCommands = null;

	StateManager(GameActivity p_context) {

		Log.d("Angband", "Creating State");

		endWin();

		this.context = p_context;

		nativew = new NativeWrapper(this);
		gameThread = new GameThread(this, nativew);

		keyBuffer = new KeyBuffer(this);

		grafmodes = new ArrayList<>();

		coreCommands = new ArrayList<>();

		createBitmapCache();
	}

	public void createBitmapCache()
	{
		int cacheSize = CACHE_MAX * 1024 * 1024;
		tileCache = new LruCache<String, Bitmap>(cacheSize) {
		    @Override
		    protected int sizeOf(String key, Bitmap value) {
		        return value.getByteCount();
		    }
		    @Override
		    protected void entryRemoved(boolean evicted, String key,
		                                Bitmap oldValue,
		                                Bitmap newValue) {
		        oldValue.recycle();
		    }
		};
	}

	public void inGameHook()
	{
		loadGraphics();

		buildCommandList();
	}

	public void loadGraphics()
    {
    	if (grafmodes.size() > 0) return;

        String buf = nativew.queryString("get_tilesets");

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
            gm.pageSize = (gm.idx != 5 && gm.idx != 6) ? 0: 8;

            grafmodes.add(gm);

            //game_context.infoAlert(gm.fullPath);

            //if (gm.idx == useGraphics) currentGraf = gm;
        }

        Log.d("Angband","Loaded tiles: " + grafmodes.size());
    }

    public void buildCommandList()
    {
    	if (coreCommands.size() > 0) return;

        String result = nativew.queryString("cmd_list");

        if (result == null) return;

        String[] list = result.split(",");

        for (String item: list) {

        	String[] parts = item.split(":");

        	if (parts.length != 2) continue;

            String desc = nativew.queryString("cmd_desc_" + parts[0]);

            if (desc == null) desc = "";

            CoreCommand cmd = new CoreCommand();
            cmd.keys[0] = Integer.valueOf(parts[0]); // Original
            cmd.keys[1] = Integer.valueOf(parts[1]); // Roguelike
            cmd.desc = desc;

            coreCommands.add(cmd);
        }
    }

    public CoreCommand findCommand(int key)
    {
    	int mode = 0;
    	if (isRoguelikeKeyboard()) mode = 1;
    	return findCommand(key, mode);
    }

    public CoreCommand findCommand(int key, int mode)
    {
    	for (CoreCommand cmd: coreCommands) {
    		if (cmd.keys[mode] == key) {
    			return cmd;
    		}
    	}
    	return null;
    }

	public void addSpecialCommand(String cmd)
	{
		if (keyBuffer != null) keyBuffer.addSpecialCommand(cmd);
	}

	public boolean isRoguelikeKeyboard()
	{
		if (!gameThread.gameRunning()) return false;
		boolean rogueLike = (nativew.gameQueryInt(1,new String[]{"rl"})==1);
		return rogueLike;
	}

	public boolean cantRun()
	{
		if (!gameThread.gameRunning()) return false;
		return (nativew.gameQueryInt(1, new String[]{"cant_run"})==1);
	}

	public boolean characterPlaying()
	{
		if (!gameThread.gameRunning()) return false;
		return (nativew.gameQueryInt(1,new String[]{"playing"})==1);
	}

	public boolean inTheDungeon()
	{
		if (!gameThread.gameRunning()) return false;
		return (nativew.gameQueryInt(1,new String[]{"in_the_dungeon"})==1);
	}

	public void link(TermView t, Handler h) {
		handler = h;
		nativew.link(t);
	}

	public void endWin() {
		termWinNext = -1;
		termwins = new HashMap<Integer, TermWindow>();

		// Initialize virtual screen (virtscr) and curses stdscr
		int h = newWin(0,0,0,0);
		virtscr = getWin(h);
		h = newWin(0,0,0,0);
		stdscr = getWin(h);

		// Create more windows
		for (int i = 1; i < MAX_WINDOWS; i++) {
			newWin(Preferences.rows,Preferences.cols,0,0);
		}
	}

	public TermWindow getWin(int handle) {
		TermWindow w = termwins.get(handle);
		return w;
	}

	public boolean windowIsVisible(TermWindow w)
	{
		int pref = Preferences.getNumberSubWindows();

		for (int i = -1; i <= MAX_WINDOWS; i++) {
			TermWindow w2 = getWin(i);
			if (w2 != null && w2 == w) {
				if (i <= 0) {
					return true;
				}

				if (!Preferences.getActivePlugin().enableSubWindows()) {
					return false;
				}

				if (i == TOP_BAR_WIN && Preferences.getTopBar()) {
					return true;
				}

				if (i <= pref) {
					return true;
				}
			}
		}
		return false;
	}

	public void delWin(int handle) {
		termwins.remove(handle);
	}
	public int newWin(int nlines, int ncols, int begin_y, int begin_x) {
		int h = termWinNext;
		termwins.put(h,new TermWindow(nlines,ncols,begin_y,begin_x));
		termWinNext++;
		return h;
	}

	public void setCursorVisible(boolean visible) {
		if (stdscr != null) {
			stdscr.cursor_visible = visible;
		}
	}

	public void setBigCursor(boolean big) {
		if (stdscr != null) {
			stdscr.big_cursor = big;
		}
	}

	public void saveTheDungeon(Bitmap bm)
	{
		if (savedDungeon != null) savedDungeon.recycle();
		savedDungeon = bm;
	}

	public Bitmap restoreTheDungeon(int w, int h)
	{
		if (savedDungeon != null &&
			(savedDungeon.getWidth() != w || savedDungeon.getHeight() != h)) {
			return null;
		}
		return savedDungeon;
	}

	public String getFatalError() {
		return "Angband quit with the following error: "+fatalMessage;
	}

	public String getWarnError() {
		return "Angband sent the following warning: "+warnMessage;
	}

	public int getKeyUp() {
		return Plugins.getKeyUp(currentPlugin);
	}
	public int getKeyDown() {
	    return Plugins.getKeyDown(currentPlugin);
	}
	public int getKeyLeft() {
		return Plugins.getKeyLeft(currentPlugin);
	}
	public int getKeyRight() {
		return Plugins.getKeyRight(currentPlugin);
	}
	public int getKeyEnter() {
		return Plugins.getKeyEnter(currentPlugin);
	}
	public int getKeyEsc() {
		return Plugins.getKeyEsc(currentPlugin);
	}
	public int getKeyQuitAndSave() {
		return Plugins.getKeyQuitAndSave(currentPlugin);
	}
	public int getKeyTab() {
		return Plugins.getKeyTab(currentPlugin);
	}
	public int getKeyBackspace() {
		return Plugins.getKeyBackspace(currentPlugin);
	}
	public int getKeyDelete() {
		return Plugins.getKeyDelete(currentPlugin);
	}

	public void clearKeys() {
		if (this.keyBuffer != null)
			this.keyBuffer.clear();
	}
	public void resetKeyBuffer() {
		this.keyBuffer = new KeyBuffer(this);
	}

	public void controlMsg(int type, String body)
	{
		Message msg = Message.obtain();
		msg.what = AngbandDialog.Action.ControlMsg.ordinal();
		msg.arg1 = type;
		msg.obj = body;
		handler.sendMessage(msg);
	}

	public void addKey(int k) {
		if (this.keyBuffer == null) {
			return;
		} else if (k == 0x9C) {
			this.keyBuffer.add(getKeyEnter());
		} else if (k == 0x9F) {
			this.keyBuffer.add(getKeyBackspace());
		} else if (k == 0xE000) {
			this.keyBuffer.add(getKeyEsc());
		} else {
			this.keyBuffer.add(k);
		}
	}

    public static int set_bit(int n) {
        return (1 << (n));
    }

    public static int make_mask(int n) {
        int i, mask = 0;
        for (i = 0; i < n; i++) {
            mask |= set_bit(i);
        }
        return mask;
    }

	public void addMousePress(int y, int x, int button, int mods)
    {
        if (this.keyBuffer == null) {
            return;
        }

        if (y < 0 || y >= 1024) return;
        if (x < 0 || x >= 1024) return;
        if (button < 0 || button >= 3) return;

        // Mouse bit
        int ch = set_bit(30);
        // Encode mods.
        ch |= ((mods & make_mask(3)) << 22); // 3 bits [24-22]
        // Encode button
        ch |= ((button & make_mask(2)) << 20); // 2 bits [21-20]
        // Encode coordinates
        ch |= ((x & make_mask(10)) << 10); // 10 bits [19-10]
        ch |= (y & make_mask(10)); // 10 bits [9-0]

        //Log.d("Angband", "mouse " + ch);

        this.keyBuffer.add(ch);
    }

	public int getKey(int v) {
		if (this.keyBuffer != null)
			return keyBuffer.get(v);
		else
			return 0;
	}
	public void addDirectionKey(int k) {
		if (this.keyBuffer != null)
			this.keyBuffer.addDirection(k);
	}
	public void signalGameExit() {
		if (this.keyBuffer != null)
			this.keyBuffer.signalGameExit();
	}
	public boolean getSignalGameExit() {
		if (this.keyBuffer != null)
			return this.keyBuffer.getSignalGameExit();
		else
			return false;
	}
	public void signalSave() {
		if (this.keyBuffer != null)
			this.keyBuffer.signalSave();
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (this.keyBuffer != null)
			return this.keyBuffer.onKeyDown(keyCode, event);
		else
			return false;
	}
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (this.keyBuffer != null)
			return this.keyBuffer.onKeyUp(keyCode, event);
		else
			return false;
	}

	public void setRunningMode(boolean on) {
		runningMode = on;
	}

	public boolean getRunningMode() {
		return runningMode;
	}
}
