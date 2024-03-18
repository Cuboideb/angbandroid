package org.rephial.xyangband;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
public class InputDialog {

    public interface InputDialogListener {
        void onInputEntered(String input);
    }

    public static void showInputDialog(Context context, String title, String initialValue,
                                       final InputDialogListener listener) {
        final EditText editText = new EditText(context);
        editText.setText(initialValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(editText);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = editText.getText().toString().trim();
                if (listener != null) {
                    listener.onInputEntered(inputText);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
