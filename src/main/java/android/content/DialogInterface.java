package android.content;

public interface DialogInterface {
    void dismiss();

    void cancel();

    interface OnClickListener {
        void onClick(DialogInterface dialog, int which);
    }
}
