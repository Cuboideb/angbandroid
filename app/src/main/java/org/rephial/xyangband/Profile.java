package org.rephial.xyangband;

import android.util.Base64;

public class Profile {

	protected int id = 0;
	protected String name = "";
    protected String saveFile = "";
	protected int flags = 0;
    protected int plugin = 0;
    protected String keymaps = "";
    protected String advBtnKeymaps = "";   
	protected static String dl = "~";
	protected static String dlEscaped = "¿@?";	

	public Profile(int id, String name, String saveFile, int flags, int plugin) {
		this.id = id;
		this.name = name;
		this.saveFile = saveFile;
		this.flags = flags;
		this.plugin = plugin;
	}

	public Profile() {}

	public String toString() {
		return name;
	}

	public int getId() {
		return id;
	}
	public void setId(int value) {
		id = value;
	}

	public String getName() {
		return name;
	}
	public void setName(String value) {
		name = value;
	}

	public String getSaveFile() {
		return saveFile;
	}
	public void setSaveFile(String value) {
		saveFile = value;
	}

	public boolean getSkipWelcome() {
		return (flags & 0x0000002)!=0;
	}
	public void setSkipWelcome(boolean value) {
		if (value)
			flags |= 0x00000002;
		else
			flags &= ~0x00000002;
	}

	public int getPlugin() {
		return plugin;
	}
	public void setPlugin(int value) {
		plugin = value;
	}
	public String getKeymaps()
	{
		return keymaps;
	}
	public void setAdvButtonKeymaps(String value)
	{
		advBtnKeymaps = value;
	}
	public String getAdvButtonKeymaps()
	{
		return advBtnKeymaps;
	}
	public void setKeymaps(String value)
	{
		keymaps = value;
	}	
	public static String escape(String str)
	{
		return str.replace(dl, dlEscaped);
	}
	public static String unescape(String str)
	{
		return str.replace(dlEscaped, dl);
	}
	public static String encode64(String str)
	{
		return Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
	}	
	public static String decode64(String str)
	{	
		return new String(Base64.decode(str, Base64.DEFAULT));
	}
	public String serialize() {		
		return id+dl+name+dl+saveFile+dl+flags+dl+plugin+dl+
			escape(keymaps)+dl+escape(advBtnKeymaps);
	}
	public static Profile deserialize(String value) {
		String[] tk = value.split(dl);
		Profile p = new Profile();
		if (tk.length>0) try {p.id = Integer.parseInt(tk[0]);} catch (Exception ex) {}
		if (tk.length>1) try {p.name = tk[1];} catch (Exception ex) {}
		if (tk.length>2) try {p.saveFile = tk[2];} catch (Exception ex) {}
		if (tk.length>3) try {p.flags = Integer.parseInt(tk[3]);} catch (Exception ex) {}
		if (tk.length>4) try {p.plugin = Integer.parseInt(tk[4]);} catch (Exception ex) {}
		if (tk.length>5) try {
			p.keymaps = unescape(tk[5]);
		} catch(Exception ex) {};
		if (tk.length>6) try {
			p.advBtnKeymaps = unescape(tk[6]);
		} catch(Exception ex) {};
		return p;
	}
}
