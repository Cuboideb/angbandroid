package org.rephial.xyangband;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageActivity extends Activity {

	protected ListView fileList;
	protected ViewGroup rootView;
	protected String cwd = "";
	protected String root = "";
	protected int selection = -1;
	protected ArrayList<String> names = new ArrayList();
	protected String prefix = "> ";

	protected static int OP_IMPORT = 101;
	protected static int OP_EXPORT = 102;

	protected static int BUFFER_SIZE = 4096;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		this.rootView = (ViewGroup) this.getLayoutInflater().inflate
				(R.layout.storage, null);

		setContentView(this.rootView);

		this.fileList = (ListView) this.findViewById(R.id.list_files);
		this.fileList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		this.root = this.cwd = Preferences.getAngbandBaseDirectory();

		Button b1 = this.rootView.findViewById(R.id.btn_export);
		b1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchExport();
			}
		});

		Button b2 = this.rootView.findViewById(R.id.btn_import);
		b2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchImport();
			}
		});

		Button b3 = this.rootView.findViewById(R.id.btn_rename);
		b3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchRename();
			}
		});

		initListView();
	}

	private void refreshItems() {
		this.selection = -1;

		File dir = new File(this.cwd);

		((TextView)this.rootView.findViewById(R.id.txt_desc)).setText(dir.getAbsolutePath());

		this.names.clear();
		if (!this.cwd.equals(this.root)) {
			this.names.add("..");
		}
		for (File f: dir.listFiles()) {
			String name = f.getName();
			if (f.isDirectory()) {
				name = this.prefix + name;
			}
			this.names.add(name);
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
			if (item.startsWith(this.prefix)) {
				item = item.substring(this.prefix.length());
			}
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

	private File getSelectedFile()
	{
		if (this.selection < 0) return null;

		String item = this.names.get(this.selection);

		if (item.equals("..")) return null;

		if (item.startsWith(this.prefix)) {
			item = item.substring(this.prefix.length());
		}

		return new File(this.cwd, item);
	}

	private File getDefaultFolder()
	{
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	}

	private void launchExport()
	{
		File source = this.getSelectedFile();

		if (source == null) return;

		if (!source.isFile()) return;

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(this.getDefaultFolder()));
		this.startActivityForResult(intent, this.OP_EXPORT);
	}

	private void launchImport()
	{
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(this.getDefaultFolder()));
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		this.startActivityForResult(intent, this.OP_IMPORT);
	}

	private void launchRename()
	{
		final File source = this.getSelectedFile();

		if (source == null) return;

		if (!source.isFile()) return;

		final StorageActivity storage = this;

		InputDialog.showInputDialog(this, "Rename", source.getName(),
				new InputDialog.InputDialogListener() {
					@Override
					public void onInputEntered(String input) {
						if (input.length() == 0 || input.indexOf('/') >= 0) return;

						if (input.equals(source.getName())) return;

						// Most have a letter or number
						Pattern pattern = Pattern.compile("[a-zA-Z0-9]");
						Matcher matcher = pattern.matcher(input);
						if (!matcher.matches()) return;

						File newFile = new File(storage.cwd, input);

						if (newFile.exists()) {
							GameActivity.infoAlert(storage, "A file already exists with name " + input);
							return;
						}

						if (!source.renameTo(newFile)) {
							GameActivity.infoAlert(storage, "Can't rename to " + input);
							return;
						}

						storage.refreshItems();
					}
				});
	}

	public static void copyStream(InputStream source, OutputStream target) throws IOException
	{
		final byte[] buffer = new byte[BUFFER_SIZE];
		int read;
		while ((read = source.read(buffer)) != -1)
		{
			target.write(buffer, 0, read);
		}
	}

	protected void performExport(Uri targetUri)
	{
		File source = this.getSelectedFile();

		if (source == null) return;

		if (!source.isFile()) return;

		DocumentFile targetFolder = DocumentFile.fromTreeUri(this, targetUri);

		DocumentFile exported = targetFolder.findFile(source.getName());

		if (exported == null) {
			try {
				exported = targetFolder.createFile(
						"application/octet-stream",
						source.getName());
			} catch (UnsupportedOperationException e) {
				GameActivity.infoAlert(this, e.getMessage());
				return;
			}
		}

		try {
			final OutputStream targetStream = this.getContentResolver().openOutputStream(exported.getUri());
			final InputStream sourceStream = new FileInputStream(source.getName());

			this.copyStream(sourceStream, targetStream);

			sourceStream.close();
			targetStream.close();
		}
		catch (IOException e)
		{
			GameActivity.infoAlert(this, e.getMessage());
			return;
		}

		GameActivity.infoAlert(this, "File exported");
	}

	protected void performImport(Uri sourceUri) {
		try {
			DocumentFile sourceDoc = DocumentFile.fromSingleUri(this, sourceUri);

			String name = sourceDoc.getName();

			File targetFile = new File(this.cwd, name);

			if (targetFile.exists()) {

				if (targetFile.isDirectory()) {
					GameActivity.infoAlert(this, "A directory already exists with this name " + name);
					return;
				}

				File oldFile = new File(this.cwd, name);

				int copy = 0;
				while (true) {
					if (++copy >= 200) {
						GameActivity.infoAlert(this, "Too many files");
						return;
					}

					File newFile = new File(this.cwd, name + "_" + copy);
					if (!newFile.exists()) {
						if (!oldFile.renameTo(newFile)) {
							GameActivity.infoAlert(this, "Can't rename " + oldFile.getName());
							return;
						}
						break;
					}
				}
			}

			InputStream sourceStream = this.getContentResolver().openInputStream(sourceUri);

			OutputStream targetStream = new FileOutputStream(targetFile.getAbsolutePath());

			this.copyStream(sourceStream, targetStream);

			sourceStream.close();
			targetStream.close();
		}
		catch (IOException e)
		{
			GameActivity.infoAlert(this, e.getMessage());
			return;
		}

		this.refreshItems();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

		if (requestCode == this.OP_EXPORT) {
			this.performExport(data.getData());
		}

		if (requestCode == this.OP_IMPORT) {
			this.performImport(data.getData());
		}
	}

	private void initListView() {

		refreshItems();

		final StorageActivity storage = this;
        fileList.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView parent,
            	View v, int position, long id)
            {
			storage.onClick(position);
            }
        });
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshItems();
	}
}
