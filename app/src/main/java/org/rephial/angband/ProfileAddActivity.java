package org.rephial.angband;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.util.Log;

public class ProfileAddActivity extends Activity {

	private Profile profile; 
	private String origSaveFile = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profileadd);

		Bundle extras = getIntent().getExtras(); 
		if(extras !=null) {			
			int id = extras.getInt("profile",-1);
			if (id > -1) {
				profile = Preferences.getProfiles().findById(id);
				origSaveFile = profile.saveFile;
			}
		}
		if (profile == null) {
			profile = new Profile();
		}
		

        populateWidgets();

        Button btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
           public void onClick(View arg0) {
        	   persistWidgetData();
           }
        });

        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
 	           setResult(RESULT_CANCELED);
 	           finish();
            }
         });
    }

    private void populateWidgets(){
    	EditText editName = (EditText) findViewById(R.id.editName);
    	editName.setText(profile.name);
    	EditText editSaveFile = (EditText) findViewById(R.id.editSaveFile);
		if (profile.saveFile.length()>0)
			editSaveFile.setText(profile.saveFile);
		else
			editSaveFile.setText(Preferences.generateSaveFilename());
    }

    private void persistWidgetData(){
    	EditText editName = (EditText) findViewById(R.id.editName);
		String newName = editName.getText().toString();
    	EditText editSaveFile = (EditText) findViewById(R.id.editSaveFile);
		String newSaveFile = editSaveFile.getText().toString();

		String msg = Preferences.getProfiles().validateChange(profile.id, 
															  newName,
															  newSaveFile);
		if (msg != null) {
			Preferences.alert(this, "Not Allowed", msg);		
			return;
		}

		if (origSaveFile.compareTo(newSaveFile)!=0
			&& Preferences.saveFileExists(newSaveFile)) {
			new AlertDialog.Builder(this) 
				.setTitle("Are you sure?") 
				.setMessage("The save file already exists on the card") 
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						doSave();
					}
				})
				.show();
		}
		else {
			doSave();
		}
    }

	private void doSave() {
    	EditText editName = (EditText) findViewById(R.id.editName);
    	EditText editSaveFile = (EditText) findViewById(R.id.editSaveFile);

		profile.name = editName.getText().toString();
		profile.saveFile = editSaveFile.getText().toString();

		if (profile.id == 0) {
			Preferences.getProfiles().add(profile);
		}

		// save changes
		Preferences.saveProfiles();

		// set this edited/added profile active
		Preferences.setActiveProfile(profile);

		setResult(RESULT_OK);
		finish();
	}

}