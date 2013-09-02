package org.twoflies.calm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import junit.framework.Assert;

import java.util.Locale;

/**
 * Utility class for Dialogs.
 */
public class Dialogs {

    private static final long[] INTERVAL_VALUES = {5 * 60 * 1000, 15 * 60 * 1000, 20 * 60 * 1000, 30 * 60 * 1000, 45 * 60 * 1000, 60 * 60 * 1000};
    private static String[] INTERVAL_LABELS = null;  // {"5 minutes", "15 minutes", "20 minutes", "30 minutes", "45 minutes", "60 minutes"};

    /**
     * Initializes resources used by the dialogs.
     * @param context
     */
    public static void initialize(Context context) {
        Assert.assertNotNull(context);

        // Initialize labels
        INTERVAL_LABELS = new String[INTERVAL_VALUES.length];
        for (int index = 0; index < INTERVAL_VALUES.length; index++) {
            INTERVAL_LABELS[index] = String.format(Locale.getDefault(), context.getString(R.string.label_X_minutes), INTERVAL_VALUES[index] / (60 * 1000));
        }
    }

    /**
     * Shows a "query" dialog (yes/no) using the given title and positive, negative OnClickListeners.
     * @param context
     * @param title
     * @param positiveListener
     * @param negativeListener
     */
    public static void showQueryDialog(Context context, String title, final DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        Assert.assertNotNull(context);
        Assert.assertNotNull(title);
        // positiveListener and negativeListener can be null

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setPositiveButton(context.getString(R.string.button_yes), positiveListener);
        builder.setNegativeButton(context.getString(R.string.button_no), negativeListener);
        builder.create().show();
    }

    /**
     * Shows the interval selection dialog and calls the supplied OnIntervalSelectedListener when an
     * interval is chosen.  Dialogs.initialize must have been called first.
     * @param context
     * @param listener
     */
    public static void showIntervalSelectionDialog(Context context, final OnIntervalSelectedListener listener) {
        Assert.assertNotNull(context);
        // listener could technically be null

        Assert.assertNotNull("Dialogs.initialize must be called first", INTERVAL_LABELS);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.title_select_interval));
        builder.setItems(INTERVAL_LABELS, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (listener != null) listener.onIntervalSelected(INTERVAL_VALUES[i]);
            }
        });
        builder.create().show();
    }

    /**
     * Interface for interval selected listeners.
     */
    public interface OnIntervalSelectedListener {
        void onIntervalSelected(long interval);
    }
}
