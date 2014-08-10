package org.twoflies.calm;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Main and only activity.
 */
public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener, Timer.OnTimerListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {

    // Keys for Preferences and instance state.
    private static final String INTERVAL_PREFERENCE_KEY = "org.twoflies.calm.INTERVAL_PREFERENCE";
    private static final String STATE_TIMER_RUNNING_KEY = "timerRunning";

    private TimerProgressView progressView = null;
    private TextView timerView = null;
    //
    private Timer timer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        this.progressView = (TimerProgressView)this.findViewById(R.id.animationView);
        this.progressView.setOnClickListener(this);

        this.timerView = (TextView)this.findViewById(R.id.timerView);
        this.timerView.setOnClickListener(this);
        this.timerView.setOnLongClickListener(this);

        // Disable 'Up', since using a single Activity
        this.getActionBar().setDisplayHomeAsUpEnabled(false);

        // Set the "Alarm" stream as the current volume control stream
        this.setVolumeControlStream(AudioManager.STREAM_ALARM);

        // Create timer
        this.timer = new Timer(Timer.DEFAULT_INTERVAL);
        this.timer.addOnTimerListener(this);
        // and initialize with stored preference
        long interval = this.getPreferences(MODE_PRIVATE).getLong(INTERVAL_PREFERENCE_KEY, Timer.DEFAULT_INTERVAL);
        this.initializeTimer(interval);

        // initialize dialog resources
        Dialogs.initialize(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // store running state of timer
        outState.putBoolean(STATE_TIMER_RUNNING_KEY, this.timer.isRunning());
        this.timer.stop();
        this.timer.saveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        this.timer.restoreInstanceState(savedInstanceState);
        boolean timerRunning = savedInstanceState.getBoolean(STATE_TIMER_RUNNING_KEY, false);
        // restore running state of timer
        if (timerRunning) {
            this.timer.start();
            // Progress and Timer views will update via Tick
        }
        else {
            // manually update views
            this.updateProgressView(this.timer.getRemainingInterval());
            this.updateTimerView(this.timer.getRemainingInterval());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop the timer so that it is in a consistent state on resume
        this.stopTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset:
                this.queryResetTimer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* View.OnClickListener */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.animationView:
                this.switchTimerState();
                break;
            case R.id.timerView:
                if (this.timer.isRunning()) {
                    this.switchTimerState();
                }
                else {
                    this.showIntervalSelectionDialog();
                }
                break;
        }
    }

    /* View.OnLongClickListener */

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.timerView:
                // TODO: This is debug code
                if (!this.timer.isRunning()) {
                    this.updateInterval(5 * 1000);  // Set to 5 seconds
                    return true;
                }
        }
        return false;
    }

    /* Timer.OnTimerListener */

    @Override
    public void onTimerTick(long remainingInterval) {
        // update views
        this.updateProgressView(remainingInterval);
        this.updateTimerView(remainingInterval);
    }

    @Override
    public void onTimerElapsed() {
        this.playAlarm();
        this.updateProgressView(this.timer.getRemainingInterval());  // should be 0
        // no need to update Timer view
    }

    /* AudioManager.OnAudioFocusChangeListener */

    @Override
    public void onAudioFocusChange(int i) {
        // no-op
    }

    /* MediaPlayer.OnCompletionListener */

    @Override
    public void onCompletion(MediaPlayer player) {
        player.release();
    }

    /**
     * Initializes the timer with the given interval.
     * @param interval The interval to use.
     */
    private void initializeTimer(long interval) {
        assert interval > 0L;

        this.timer.stop();  // just in case
        this.timer.setInterval(interval);
        // update views
        this.updateProgressView(this.timer.getInterval());
        this.updateTimerView(this.timer.getInterval());
    }

    /**
     * Updates the TimerProgressView by calculating the percentage complete from the given remaining
     * interval, and the display message from the timer state.
     * @param remainingInterval
     */
    private void updateProgressView(long remainingInterval) {
        assert remainingInterval >= 0L;

        // Calculate percentage
        float percentage = (this.timer.getInterval() - remainingInterval) / (float)this.timer.getInterval();

        // and message based on timer state
        String message;
        if (this.timer.isRunning()) message = this.getString(R.string.message_press_to_stop);
        else if (this.timer.isElapsed()) message = this.getString(R.string.message_press_to_reset);
        else message = this.getString(R.string.message_press_to_start);

        this.progressView.updateProgress(percentage, message);
    }

    /**
     * Updates the Timer(Text)View using the given remaining interval.
     * @param remainingInterval
     */
    private void updateTimerView(long remainingInterval) {
        assert remainingInterval >= 0L;

        long minutes = remainingInterval / (60 * 1000);
        long seconds = (remainingInterval % (60 * 1000)) / 1000;

        this.timerView.setText(String.format(Locale.getDefault(), getString(R.string.time_format), minutes, seconds));
    }

    /**
     * Starts, stops or resets the timer based on the current state.
     */
    private void switchTimerState() {
        if (this.timer.isRunning()) {
            this.stopTimer();
        }
        else if (this.timer.isElapsed()) {
            this.resetTimer();
        }
        else {
            this.startTimer();
        }
    }

    /**
     * (Re-)Starts the timer.
     */
    private void startTimer() {
        this.timer.start();
        // Progress and Timer views will update via Tick

        // Make sure screen stays on
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Stops the timer.
     */
    private void stopTimer() {
        this.timer.stop();
        // update views
        this.updateProgressView(this.timer.getRemainingInterval());
        // no need to update Timer view

        // Allow screen to shut off
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Resets the timer (back to the timer's interval).
     */
    private void resetTimer() {
        this.timer.reset();
        // update views
        this.updateProgressView(this.timer.getInterval());
        this.updateTimerView(this.timer.getInterval());
    }

    /**
     * Queries whether to reset the timer, if it is running; resets it otherwise.
     */
    private void queryResetTimer() {
        if (this.timer.isRunning()) {
            Dialogs.showQueryDialog(this, getString(R.string.title_abandon_timer), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopTimer();
                    resetTimer();
                }
            }, null);
        }
        else {
            this.resetTimer();
        }
    }

    /**
     * Plays the "alarm".
     */
    private void playAlarm() {
        AudioManager audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        // Request "transient" audio focus
        if (audioManager.requestAudioFocus(this, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            MediaPlayer player = MediaPlayer.create(this, R.raw.bowl);
            player.setOnCompletionListener(this);
            player.start();
            // will be released in OnCompletion

            audioManager.abandonAudioFocus(this);
        }
        else  // at least "toast" ;-)
        {
            Toast.makeText(this, getString(R.string.toast_alarm_expired), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Shows the interval selection dialog, and sets the timer interval based on its result.
     */
    private void showIntervalSelectionDialog() {
        Dialogs.showIntervalSelectionDialog(this, new Dialogs.OnIntervalSelectedListener() {
            @Override
            public void onIntervalSelected(long interval) {
                updateInterval(interval);
            }
        });
    }

    /**
     * Updates the timer interval to the given interval and stores it as the current preference.
     * @param interval
     */
    private void updateInterval(long interval) {
        assert interval > 0L;

        SharedPreferences.Editor editor = this.getPreferences(MODE_PRIVATE).edit();
        editor.putLong(INTERVAL_PREFERENCE_KEY, interval);
        editor.commit();

        this.initializeTimer(interval);
    }
}
