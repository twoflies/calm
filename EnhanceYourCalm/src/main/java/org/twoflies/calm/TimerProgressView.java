package org.twoflies.calm;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import junit.framework.Assert;

/**
 * Custom view to display the current progress and instructional message.
 */
public class TimerProgressView extends View {

    private static final float TEXT_SIZE = 50.0f;

    private Paint dashedPaint = null;
    private Paint solidPaint = null;
    private Paint markerPaint = null;
    private Paint imagePaint = null;
    private Paint messagePaint = null;
    private Bitmap buddhaBitmap = null;
    //
    private float xCenter = 0.0f;
    private float yCenter = 0.0f;
    private float radius = 0.0f;
    private RectF bounds = null;
    private float sweepAngle = 0.0f;
    private float xMarker = 0.0f;
    private float yMarker = 0.0f;
    private RectF imageBounds = null;
    private float xMessage = 0.0f;
    private float yMessage = 0.0f;
    //
    private float percentage = 0.0f;
    private String message = null;

    public TimerProgressView(Context context, AttributeSet attributes) {
        super(context, attributes);

        // initialize all paint members
        this.dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dashedPaint.setColor(Resources.getSystem().getColor(android.R.color.darker_gray));
        this.dashedPaint.setStyle(Paint.Style.STROKE);
        this.dashedPaint.setStrokeWidth(10.0f);
        this.dashedPaint.setPathEffect(new DashPathEffect(new float[] {20, 10}, 0));

        this.solidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.solidPaint.setColor(Resources.getSystem().getColor(android.R.color.holo_blue_light));
        this.solidPaint.setStyle(Paint.Style.STROKE);
        this.solidPaint.setStrokeWidth(12.0f);
        this.solidPaint.setStrokeCap(Paint.Cap.ROUND);

        this.markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.markerPaint.setColor(Resources.getSystem().getColor(android.R.color.holo_blue_dark));
        this.markerPaint.setStyle(Paint.Style.FILL);

        this.imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        this.messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.messagePaint.setColor(Resources.getSystem().getColor(android.R.color.holo_blue_light));
        this.messagePaint.setTextSize(TEXT_SIZE);
        this.messagePaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        this.calculateBounds(w, h);
        this.calculatePercentage();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(this.xCenter, this.yCenter, this.radius, this.dashedPaint);
        // progress arc
        canvas.drawArc(this.bounds, 270.0f, this.sweepAngle, false, this.solidPaint);
        // marker
        canvas.drawCircle(this.xMarker, this.yMarker, 10.0f, this.markerPaint);
        // buddha
        canvas.drawBitmap(this.buddhaBitmap, null, this.imageBounds, this.imagePaint);
        // message
        canvas.drawText(this.message, this.xMessage, this.yMessage, this.messagePaint);
    }

    /**
     * Updates the progress based on the given percentage, including the message to be displayed.
     * @param percentage
     * @param message
     */
    public void updateProgress(float percentage, String message) {
        Assert.assertTrue((percentage >= 0.0f) && (percentage <= 1.0f));
        Assert.assertNotNull(message);

        this.percentage = percentage;
        this.message = message;
        // only percentage based parameters need to be recalculated
        this.calculatePercentage();

        this.invalidate();
    }

    /**
     * Calculates the bounds and positions of all drawn items and loads the bitmap image.
     * @param w
     * @param h
     */
    private void calculateBounds(int w, int h) {
        float xPadding = (float)(this.getPaddingLeft() + this.getPaddingLeft());
        float yPadding = (float)(this.getPaddingTop() + this.getPaddingBottom());

        float width = w - xPadding;
        float height = h - yPadding;
        float diameter = Math.min(width, height);

        // calculate center and radius of circle
        this.xCenter = this.getPaddingLeft() + (width / 2.0f);
        this.yCenter = this.getPaddingTop() + (height / 2.0f) - this.messagePaint.getTextSize();
        this.radius = diameter / 2.0f;

        // calculate bounding box of circle
        float xPosition = this.xCenter - this.radius;
        float yPosition = this.yCenter - this.radius;
        this.bounds = new RectF(xPosition, yPosition,
                                xPosition + diameter, yPosition + diameter);

        // image to be display has a ratio of 3:4 width:height, so use some geometry to calculate
        // a bounding box for the image that fits inside the circle
        float xOffset = (3.0f / 5.0f) * this.radius;
        float yOffset = (4.0f / 5.0f) * this.radius;
        this.imageBounds = new RectF(this.xCenter - xOffset, this.yCenter - yOffset,
                                this.xCenter + xOffset, this.yCenter + yOffset);

        // get the size of the image to be loaded
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_buddha, options);

        // calculate a sub-sampling rate if the image to be loaded is bigger (substantially) than
        // the bounds into which it will be drawn
        float sampleSize = 1.0f;
        if ((options.outWidth > this.imageBounds.width()) || (options.outHeight > this.imageBounds.height())) {
            sampleSize = (options.outWidth > options.outHeight) ? (options.outHeight / this.imageBounds.height()) : (options.outWidth / this.imageBounds.width());
        }
        options.inSampleSize = (int)sampleSize;
        options.inJustDecodeBounds = false;
        this.buddhaBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_buddha, options);

        // calculate the center point of the message to be displayed
        this.xMessage = this.xCenter;
        this.yMessage = this.getPaddingTop() + height + (this.messagePaint.getTextSize() / 2.0f);
    }

    /**
     * Calculates the percentage related angles and positions.
     */
    private void calculatePercentage() {
        this.sweepAngle = this.percentage * 360.0f;

        // calculates the position of the marker on the circumference of the circle using awesome
        // trigonometry
        float radians = (float)(this.sweepAngle * (Math.PI / 180.0));
        this.xMarker = (float)(this.xCenter + (Math.sin(radians) * this.radius));
        this.yMarker = (float)(this.yCenter - (Math.cos(radians) * this.radius));
    }
}
