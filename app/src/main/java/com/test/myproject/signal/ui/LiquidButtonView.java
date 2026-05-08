package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class LiquidButtonView extends View {
    private Paint paint;
    private Paint textPaint;
    private Paint glossPaint;
    private Paint borderPaint;
    private Paint ringPaint;
    private Path path;
    private RectF glossRect;

    private boolean isTesting = false;
    private float animationTime = 0f;
    private ValueAnimator liquidAnimator;
    private ValueAnimator colorAnimator;
    private ValueAnimator pulseAnimator;

    private float pulseRadius = 0f;
    private int pulseAlpha = 0;
    private float pulseRadius2 = 0f;
    private int pulseAlpha2 = 0;

    private String buttonText = "СТАРТ";
    private int startColor = Color.parseColor("#00C9FF");
    private int endColor = Color.parseColor("#0083B0");
    private final int IDLE_START_COLOR = Color.parseColor("#00C9FF");
    private final int IDLE_END_COLOR = Color.parseColor("#0083B0");
    private final int TEST_START_COLOR = Color.parseColor("#FF512F");
    private final int TEST_END_COLOR = Color.parseColor("#DD2476");

    public LiquidButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(60f);
        textPaint.setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"));

        glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glossPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f);
        ringPaint.setColor(Color.parseColor("#00C9FF"));

        path = new Path();
        glossRect = new RectF();

        liquidAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        liquidAnimator.setDuration(3000);
        liquidAnimator.setRepeatCount(ValueAnimator.INFINITE);
        liquidAnimator.setInterpolator(new LinearInterpolator());
        liquidAnimator.addUpdateListener(animation -> {
            animationTime = (float) animation.getAnimatedValue();
            invalidate();
        });

        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(400);

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            float maxRadius = Math.min(getWidth(), getHeight()) / 2f;
            float baseRadius = maxRadius * 0.7f;

            pulseRadius = baseRadius + (maxRadius - baseRadius) * fraction;
            pulseAlpha = (int) (255 * (1f - fraction));

            float fraction2 = (fraction + 0.5f) % 1.0f;
            pulseRadius2 = baseRadius + (maxRadius - baseRadius) * fraction2;
            pulseAlpha2 = (int) (255 * (1f - fraction2));

            invalidate();
        });

        pulseAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateVisuals(w, h);
    }

    private void updateVisuals(int w, int h) {
        if (w == 0 || h == 0) return;

        float lightX = w * 0.3f;
        float lightY = h * 0.3f;
        float radius = Math.min(w, h) * 0.7f;

        RadialGradient radialGradient = new RadialGradient(
                lightX, lightY, radius,
                startColor, endColor, Shader.TileMode.CLAMP
        );
        paint.setShader(radialGradient);

        float centerX = w / 2f;
        float centerY = h / 2f;
        float baseRadius = Math.min(centerX, centerY) * 0.7f;

        glossRect.set(
                centerX - baseRadius * 0.65f,
                centerY - baseRadius * 0.85f,
                centerX + baseRadius * 0.65f,
                centerY - baseRadius * 0.15f
        );

        LinearGradient glossGradient = new LinearGradient(
                0, glossRect.top, 0, glossRect.bottom,
                Color.parseColor("#99FFFFFF"),
                Color.parseColor("#00FFFFFF"),
                Shader.TileMode.CLAMP
        );
        glossPaint.setShader(glossGradient);

        LinearGradient borderGradient = new LinearGradient(
                centerX - baseRadius, centerY - baseRadius,
                centerX + baseRadius, centerY + baseRadius,
                new int[]{Color.parseColor("#88FFFFFF"), Color.TRANSPARENT, Color.parseColor("#44000000")},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        borderPaint.setShader(borderGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(centerX, centerY) * 0.7f;

        if (!isTesting) {
            ringPaint.setAlpha(pulseAlpha);
            canvas.drawCircle(centerX, centerY, pulseRadius, ringPaint);

            ringPaint.setAlpha(pulseAlpha2);
            canvas.drawCircle(centerX, centerY, pulseRadius2, ringPaint);
        }

        path.reset();
        int numPoints = 60;

        for (int i = 0; i <= numPoints; i++) {
            float angle = (float) (i * Math.PI * 2 / numPoints);
            float radius = baseRadius;

            if (isTesting) {
                float noise = (float) (Math.sin(angle * 3 + animationTime) * 8f
                        + Math.cos(angle * 4 - animationTime * 2f) * 5f);
                radius += noise;
            }

            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();

        canvas.drawPath(path, paint);

        if (!isTesting) {
            canvas.drawPath(path, borderPaint);
            canvas.drawOval(glossRect, glossPaint);
        }

        float textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(buttonText, centerX, textY, textPaint);
    }

    public void setTestingState(boolean testing) {
        this.isTesting = testing;

        if (colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }

        final int targetStartColor;
        final int targetEndColor;

        if (testing) {
            buttonText = "СТОП";
            targetStartColor = TEST_START_COLOR;
            targetEndColor = TEST_END_COLOR;
            liquidAnimator.start();
            pulseAnimator.cancel();
        } else {
            buttonText = "СТАРТ";
            targetStartColor = IDLE_START_COLOR;
            targetEndColor = IDLE_END_COLOR;
            liquidAnimator.cancel();
            animationTime = 0f;
            pulseAnimator.start();
        }

        final int currentStartColor = startColor;
        final int currentEndColor = endColor;

        android.animation.ArgbEvaluator evaluator = new android.animation.ArgbEvaluator();
        colorAnimator.removeAllUpdateListeners();

        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            startColor = (int) evaluator.evaluate(fraction, currentStartColor, targetStartColor);
            endColor = (int) evaluator.evaluate(fraction, currentEndColor, targetEndColor);
            updateVisuals(getWidth(), getHeight());
            invalidate();
        });

        colorAnimator.start();
    }
}