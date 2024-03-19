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
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageActivity extends Activity {

	protected ListView fileList;
	protected ViewGroup rootView;
	protected String cwd = "";
	protected String root = "";
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

		this.fileList = this.findViewById(R.id.list_files);
		this.fileList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		this.root = this.cwd = Preferences.getAngbandBaseDirectory();

		Button btn = this.rootView.findViewById(R.id.btn_export);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchExport();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_import);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchImport();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_rename);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchRename();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_duplicate);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchDuplicate();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_duplicate_v2);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchDuplicate();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_exit);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		btn = this.rootView.findViewById(R.id.btn_exit_v2);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		if (GameActivity.getDpWidth(this) < 600) {
			this.rootView.findViewById(R.id.btn_exit).setVisibility(View.GONE);
			this.rootView.findViewById(R.id.btn_duplicate).setVisibility(View.GONE);
			this.rootView.findViewById(R.id.narrow_layout).setVisibility(View.VISIBLE);
		}

		initListView();
	}

	private TextView getWidgetSelection()
	{
		return this.rootView.findViewById(R.id.txt_selected);
	}

	private TextView getWidgetFolder()
	{
		return this.rootView.findViewById(R.id.txt_desc);
	}

	private void refreshItems() {
		this.getWidgetSelection().setText("");

		File dir = new File(this.cwd);

		int begin = this.root.length();
		String txt = this.cwd;
		if (begin <= txt.length()) txt = txt.substring(begin);

		this.getWidgetFolder().setText(txt);

		this.names = new ArrayList<>();

		if (!this.cwd.equals(this.root)) {
			this.names.add("..");
		}

		ArrayList<File> temp = new ArrayList<>();

		for (File f: dir.listFiles()) {
			temp.add(f);
		}

		Collections.sort(temp, new Comparator<File>() {
			@Override
			public int compare(File fa, File fb) {
				int da = (fa.isDirectory() ? 1: 0);
				int db = (fb.isDirectory() ? 1: 0);
				if (da == db) {
					String sa = fa.getName().toUpperCase();
					String sb = fb.getName().toUpperCase();
					return sa.compareTo(sb);
				}
				return (db - da);
			}
		});

		for (File f: temp) {
			String name = f.getName();
			//GameActivity.log(name);
			if (f.isDirectory()) {
				name = this.prefix + name;
			}
			this.names.add(name);
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, this.names);
		this.fileList.setAdapter(adapter);
	}

	private void onClick(int position) {
		String item = this.names.get(position);
		GameActivity.log("Navigate to " + item);

		this.getWidgetSelection().setText("");

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
				this.getWidgetSelection().setText(item);
			}
		}
	}

	private File getSelectedFile()
	{
		String item = this.getWidgetSelection().getText().toString().trim();

		if (item.length() == 0) return null;

		if (item.equals("..")) return null;

		if (item.startsWith(this.prefix)) {
			item = item.substring(this.prefix.length());
		}

		return new File(this.cwd, item);
	}

	/*
	private File getDefaultFolder()
	{
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	}
	*/

	/*
	private void launchExport()
	{
		File source = this.getSelectedFile();

		if (source == null || !source.isFile()) {
			GameActivity.infoAlert(this, "Select a file");
			return;
		}

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(this.getDefaultFolder()));
		this.startActivityForResult(intent, this.OP_EXPORT);
	}
	*/

	private void launchExport()
	{
		File source = this.getSelectedFile();

		if (source == null || !source.isFile()) {
			GameActivity.infoAlert(this, "Select a file");
			return;
		}

		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/octet-stream");
		intent.putExtra(Intent.EXTRA_TITLE, source.getName());
		startActivityForResult(intent, this.OP_EXPORT);
	}

	private void launchImport()
	{
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		//intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(this.getDefaultFolder()));
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		this.startActivityForResult(intent, this.OP_IMPORT);
	}

	private void launchRename()
	{
		final File source = this.getSelectedFile();

		if (source == null || !source.isFile()) {
			GameActivity.infoAlert(this, "Select a file");
			return;
		}

		final StorageActivity storage = this;

		InputDialog.showInputDialog(this, "Rename", source.getName(),
				new InputDialog.InputDialogListener() {
					@Override
					public void onInputEntered(String input) {
						if (input.length() == 0 || input.indexOf('/') >= 0) return;

						if (input.equals(source.getName())) return;

						// Most have a letter or number
						Pattern pattern = Pattern.compile(".*[a-zA-Z0-9].*");
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

	private void launchDuplicate()
	{
		final File source = this.getSelectedFile();

		if (source == null || !source.isFile()) {
			GameActivity.infoAlert(this, "Select a file");
			return;
		}

		String name = source.getName() + "_copy";
		File target = new File(this.cwd, name);

		if (target.isDirectory()) {
			GameActivity.infoAlert(this, "A directory already exists with this name " + name);
			return;
		}

		try {
			final OutputStream targetStream = new FileOutputStream(target);
			final InputStream sourceStream = new FileInputStream(source);

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

	public static void copyStream(InputStream source, OutputStream target) throws IOException
	{
		final byte[] buffer = new byte[BUFFER_SIZE];
		int read;
		while ((read = source.read(buffer)) != -1)
		{
			target.write(buffer, 0, read);
		}
	}

	/*
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
			final InputStream sourceStream = new FileInputStream(source);

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
	*/

	protected void performExport(Uri targetUri)
	{
		File source = this.getSelectedFile();

		if (source == null) return;

		if (!source.isFile()) return;

		try {
			final OutputStream targetStream = this.getContentResolver().openOutputStream(targetUri);
			final InputStream sourceStream = new FileInputStream(source);

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

					File newFile = new File(this.cwd, name + "_old" + copy);
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

		GameActivity.infoAlert(this, "File imported");

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
