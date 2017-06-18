package sergeylysyi.notes.note.RemoteNotes;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import sergeylysyi.notes.R;

public class ChooseSourceDialog {
    public static void show(Context context, final OnResult resultCallback) {
        final DialogInterface.OnClickListener decideLaterOption = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resultCallback.onCancel();
            }
        };
        new AlertDialog.Builder(context, 0)
                .setTitle(R.string.RemoteNotes_dialog_title)
                .setMessage(R.string.RemoteNotes_dialog_message)
                .setPositiveButton(R.string.RemoteNotes_dialog_local_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resultCallback.onResult(Result.USE_LOCAL);
                    }
                })
                .setNegativeButton(R.string.RemoteNotes_dialog_remote_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resultCallback.onResult(Result.USE_REMOTE);
                    }
                })
                .setNeutralButton(R.string.RemoteNotes_dialog_later_option, decideLaterOption)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        decideLaterOption.onClick(dialog, 0);
                    }
                })
        .show();
    }

    public enum Result {USE_LOCAL, USE_REMOTE}

    public interface OnResult {
        void onResult(Result result);

        void onCancel();
    }
}
