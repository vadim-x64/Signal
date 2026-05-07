package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class SpeedometerView extends View {
    private Paint arcPaint;
    private Paint backgroundArcPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint textPaint;
    private RectF arcBounds;

    private float currentSpeed = 0f;
    private float maxSpeed = 150f;

    private final float START_ANGLE = 135f;
    private final float SWEEP_ANGLE = 270f;

    private float centerX, centerY, radius;
    private ValueAnimator speedAnimator;

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(30f);
        backgroundArcPaint.setColor(Color.parseColor("#1AFFFFFF"));
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(30f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(Color.WHITE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#B3FFFFFF"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 100f;
        arcBounds.set(padding, padding, w - padding, h - padding);

        centerX = w / 2f;
        centerY = h / 2f;
        radius = arcBounds.width() / 2f;

        SweepGradient gradient = new SweepGradient(centerX, centerY,
                new int[]{Color.parseColor("#00C9FF"), Color.parseColor("#92FE9D"), Color.parseColor("#F6D365"), Color.parseColor("#FF512F")},
                new float[]{0f, 0.33f, 0.66f, 1f});
        arcPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, backgroundArcPaint);

        int numTicks = 30;
        for (int i = 0; i <= numTicks; i++) {
            float angle = START_ANGLE + (i * SWEEP_ANGLE / numTicks);
            double rad = Math.toRadians(angle);
            boolean isMajorTick = (i % 6 == 0);

            float tickLength = isMajorTick ? 25f : 12f;
            float startRadius = radius + 20f;

            float startX = (float) (centerX + startRadius * Math.cos(rad));
            float startY = (float) (centerY + startRadius * Math.sin(rad));
            float stopX = (float) (centerX + (startRadius + tickLength) * Math.cos(rad));
            float stopY = (float) (centerY + (startRadius + tickLength) * Math.sin(rad));

            tickPaint.setStrokeWidth(isMajorTick ? 6f : 3f);
            tickPaint.setColor(isMajorTick ? Color.WHITE : Color.parseColor("#66FFFFFF"));
            canvas.drawLine(startX, startY, stopX, stopY, tickPaint);

            if (isMajorTick) {
                int speedValue = (int) ((i / (float)numTicks) * maxSpeed);
                String text = String.valueOf(speedValue);

                float textRadius = startRadius + tickLength + 32f;
                float textX = (float) (centerX + textRadius * Math.cos(rad));
                float textY = (float) (centerY + textRadius * Math.sin(rad));

                Rect textBounds = new Rect();
                textPaint.getTextBounds(text, 0, text.length(), textBounds);
                textY += textBounds.height() / 2f;

                canvas.drawText(text, textX, textY, textPaint);
            }
        }

        float speedRatio = Math.max(0f, Math.min(currentSpeed / maxSpeed, 1f));
        float progressAngle = SWEEP_ANGLE * speedRatio;

        canvas.save();
        canvas.rotate(90, centerX, centerY);
        canvas.drawArc(arcBounds, START_ANGLE - 90, progressAngle, false, arcPaint);
        canvas.restore();

        float needleAngle = START_ANGLE + progressAngle;
        double needleRad = Math.toRadians(needleAngle);

        float innerNeedleRadius = radius - 40f;
        float outerNeedleRadius = radius + 10f;

        Path needlePath = new Path();
        float tipX = (float) (centerX + outerNeedleRadius * Math.cos(needleRad));
        float tipY = (float) (centerY + outerNeedleRadius * Math.sin(needleRad));

        float baseLeftX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad - 0.08));
        float baseLeftY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad - 0.08));
        float baseRightX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad + 0.08));
        float baseRightY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad + 0.08));

        needlePath.moveTo(tipX, tipY);
        needlePath.lineTo(baseLeftX, baseLeftY);
        needlePath.lineTo(baseRightX, baseRightY);
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);

        float baseX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad));
        float baseY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad));
        canvas.drawCircle(baseX, baseY, 8f, needlePaint);
    }

    public void setSpeed(float speed) {
        boolean scaleChanged = false;
        while (speed > maxSpeed * 0.9f) {
            maxSpeed *= 2f;
            scaleChanged = true;
        }

        if (scaleChanged) invalidate();

        if (speedAnimator != null && speedAnimator.isRunning()) {
            speedAnimator.cancel();
        }

        speedAnimator = ValueAnimator.ofFloat(currentSpeed, speed);
        speedAnimator.setDuration(250);
        speedAnimator.setInterpolator(new DecelerateInterpolator());
        speedAnimator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate();
        });
        speedAnimator.start();
    }

    public void smoothReset() {
        if (speedAnimator != null && speedAnimator.isRunning()) {
            speedAnimator.cancel();
        }

        speedAnimator = ValueAnimator.ofFloat(currentSpeed, 0f);
        speedAnimator.setDuration(800);
        speedAnimator.setInterpolator(new DecelerateInterpolator());
        speedAnimator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate();
        });
        speedAnimator.start();
    }

    public void reset() {
        if (speedAnimator != null) speedAnimator.cancel();
        maxSpeed = 150f;
        currentSpeed = 0f;
        invalidate();
    }
}