package org.rephial.xyangband;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class StorageActivity extends Activity {

	protected ListView fileList;
	protected String cwd = "";
	protected String root = "";
	protected int selection = -1;
	protected ArrayList<String> names = new ArrayList();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.storage);

		this.fileList = (ListView) this.findViewById(R.id.list_files);
		this.fileList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		this.root = this.cwd = Preferences.getAngbandBaseDirectory();

		initListView();
	}

	private void refreshItems() {
		this.selection = -1;
		File dir = new File(this.cwd);
		this.names.clear();
		if (!this.cwd.equals(this.root)) {
			this.names.add("..");
		}
		for (File f: dir.listFiles()) {
			this.names.add(f.getName());
		}
		this.fileList.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, this.names));
	}

	private void onClick(int position) {
		String item = this.names.get(position);
		GameActivity.log("Navigate to " + item);

		if (this.selection >= 0) {
			View v = this.fileList.getChildAt(this.selection);
			((TextView)v).setTextColor(Color.BLACK);
			this.selection = -1;
		}

		if (item.equals("..")) {
			File f = new File(this.cwd).getParentFile();
			if (f != null) {
				this.cwd = f.getAbsolutePath();
				this.refreshItems();
			}
		}
		else {
			File f = new File(this.cwd, item);
			if (f != null && f.isDirectory()) {
				this.cwd = f.getAbsolutePath();
				this.refreshItems();
			}
			if (f != null && f.isFile()) {
				this.selection = position;
				View v = this.fileList.getChildAt(this.selection);
				((TextView)v).setTextColor(Color.BLUE);
			}
		}
	}

	private void initListView() {

		refreshItems();

		/*
		proList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
													ContextMenuInfo menuInfo) {
					menu.setHeaderTitle("Profile");
					menu.add(0, CONTEXTMENU_EDITITEM, 0, "Edit");
					menu.add(0, CONTEXTMENU_DELETEITEM, 0, "Delete");
				}
			});
		*/

		final StorageActivity self = this;
        fileList.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView parent,
            	View v, int position, long id)
            {
				self.onClick(position);
            }
        });
	}

	/*
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(getApplication());
		inflater.inflate(R.menu.profile, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getNumericShortcut()) {
		case '1':
			intent = new Intent(this, ProfileAddActivity.class);
			startActivity(intent);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
		Profile pro = Preferences.getProfiles().get(menuInfo.position);

		switch (aItem.getItemId()) {
		case CONTEXTMENU_DELETEITEM:
			if (profiles.size()>1) {
				profiles.remove(pro);
				Preferences.saveProfiles();
				refreshProListItems();
			}
			else
				Preferences.alert(this, "Not Allowed", "You cannot delete the last profile");
			return true; 
		case CONTEXTMENU_EDITITEM:
			Intent intent = new Intent(this, ProfileAddActivity.class);
			intent.putExtra("profile", pro.id);
			startActivity(intent);
			return true; 
		}
		return false;
	}
	*/

	@Override
	protected void onResume() {
		super.onResume();
		refreshItems();
	}
}
