package org.twoflies.calm;

import android.os.Bundle;
import android.os.Handler;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates timer functionality, including registering and notifying timer listeners.
 */
public class Timer {

    public static final long DEFAULT_INTERVAL = 15 * 60 * 1000;  // 15 minutes

    // Keys for instance state
    private static final String STATE_INTERVAL_KEY = "interval";
    private static final String STATE_ADJUSTED_INTERVAL_KEY = "adjustedInterval";
    private static final String STATE_REMAINING_INTERVAL_KEY = "remainingInterval";

    // Delay for timer ticks
    private static final int TIMER_DELAY_MS = 200;

    private long interval = 0L;
    //
    private long adjustedInterval = 0L;
    private long remainingInterval = 0L;
    private long startTime = 0L;
    private Handler handler = new Handler();
    // Runnable for timer ticks
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (timerTick()) {
                handler.postDelayed(this, TIMER_DELAY_MS);
            }
        }
    };
    private List<OnTimerListener> listeners = new ArrayList<OnTimerListener>();

    /**
     * Creates a timer with the given interval
     * @param interval
     */
    public Timer(long interval) {
        Assert.assertTrue(interval > 0L);

        this.setInterval(interval);
    }

    /* OnTimerListener */

    /**
     * Adds the given OnTimerListener.
     * @param listener
     */
    public void addOnTimerListener(OnTimerListener listener) {
        Assert.assertNotNull(listener);

        if (!this.listeners.contains(listener)) this.listeners.add(listener);
    }

    /**
     * Removes the given OnTimerListener.
     * @param listener
     */
    public void removeOnTimerListener(OnTimerListener listener) {
        Assert.assertNotNull(listener);

        this.listeners.remove(listener);
    }

    /**
     * Invokes the OnTimerTick method of all registered OnTimerListeners.
     * @param remainingInterval
     */
    protected void invokeOnTimerTick(long remainingInterval) {
        assert remainingInterval >= 0L;

        for (OnTimerListener listener : this.listeners) {
            listener.onTimerTick(remainingInterval);
        }
    }

    /**
     * Invokes the OnTimerElapsed method of all registered OnTimerListeners.
     */
    protected void fireOnTimerElapsed() {
        for (OnTimerListener listener : this.listeners) {
            listener.onTimerElapsed();
        }
    }

    /* Public Accessors */

    /**
     * Returns this timer's interval.
     */
    public long getInterval() {
        return this.interval;
    }

    /**
     * Sets this timer's interval.
     * @param interval an interval > 0
     */
    public void setInterval(long interval) {
        Assert.assertTrue(interval > 0L);

        this.interval = interval;
        this.adjustedInterval = interval;
        this.remainingInterval = interval;
    }

    /**
     * Returns this timer's remaining interval.
     */
    public long getRemainingInterval() {
        return this.remainingInterval;
    }

    /**
     * Returns whether this timer is currently running.
     */
    public boolean isRunning() {
        return (this.startTime > 0L);
    }

    /**
     * Returns whether this timer has currently elapsed.
     */
    public boolean isElapsed() {
        return (this.remainingInterval == 0L);
    }

    /**
     * Starts this timer if it is not already running.
     */
    public void start() {
        if (this.isRunning()) return;

        this.startTime = System.currentTimeMillis();
        this.handler.postDelayed(this.runnable, TIMER_DELAY_MS);
    }

    /**
     * Stops this timer if it is running.
     */
    public void stop() {
        if (!this.isRunning()) return;

        this.handler.removeCallbacks(this.runnable);
        // store the remaining interval
        this.adjustedInterval = this.remainingInterval;
        this.startTime = 0L;
    }

    /**
     * Resets this timer to the original interval if it is not running.
     */
    public void reset() {
        if (this.isRunning()) return;

        this.handler.removeCallbacks(this.runnable);
        this.remainingInterval = this.adjustedInterval = this.interval;
        this.startTime = 0L;
    }

    /**
     * Saves the instance state of this timer to the given bundle.
     * @param outState
     */
    public void saveInstanceState(Bundle outState) {
        Assert.assertNotNull(outState);

        outState.putLong(STATE_INTERVAL_KEY, this.interval);
        outState.putLong(STATE_ADJUSTED_INTERVAL_KEY, this.adjustedInterval);
        outState.putLong(STATE_REMAINING_INTERVAL_KEY, this.remainingInterval);
    }

    /**
     * Restores the instance state of this timer from the given bundle.
     * @param instanceState
     */
    public void restoreInstanceState(Bundle instanceState) {
        Assert.assertNotNull(instanceState);

        this.interval = instanceState.getLong(STATE_INTERVAL_KEY, DEFAULT_INTERVAL);
        this.adjustedInterval = instanceState.getLong(STATE_ADJUSTED_INTERVAL_KEY, this.interval);
        this.remainingInterval = instanceState.getLong(STATE_REMAINING_INTERVAL_KEY, this.interval);
    }

    /**
     * "Callback" for a timer tick.
     * @return true to register for the next click, false to stop.
     */
    private boolean timerTick() {
        this.remainingInterval = Math.max(this.adjustedInterval - (System.currentTimeMillis() - this.startTime), 0L);

        this.invokeOnTimerTick(this.remainingInterval);

        // if the timer has elapsed, stop it and trigger OnTimerElapsed
        if (this.isElapsed()) {
            this.stop();
            this.fireOnTimerElapsed();
        }

        return !this.isElapsed();
    }

    /**
     * Interface for timer listeners.
     */
    public interface OnTimerListener {
        void onTimerTick(long remainingInterval);
        void onTimerElapsed();
    }
}
