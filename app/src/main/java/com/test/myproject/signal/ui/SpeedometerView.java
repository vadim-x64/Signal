package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class SpeedometerView extends View {
    private Paint arcPaint;
    private Paint needlePaint;
    private Paint backgroundArcPaint;
    private RectF arcBounds;

    private float currentSpeed = 0f;
    private float maxSpeed = 150f; // Максимальна швидкість на шкалі (Мбіт/с)

    private final float START_ANGLE = 135f;
    private final float SWEEP_ANGLE = 270f;

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(30f);
        backgroundArcPaint.setColor(Color.parseColor("#33FFFFFF"));
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(30f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(Color.WHITE);

        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40f;
        arcBounds.set(padding, padding, w - padding, h - padding);

        // Динамічний градієнт: від зеленого через жовтий до червоного
        SweepGradient gradient = new SweepGradient(
                w / 2f, h / 2f,
                new int[]{Color.parseColor("#92FE9D"), Color.parseColor("#F6D365"), Color.parseColor("#FF512F"), Color.parseColor("#92FE9D")},
                new float[]{0f, 0.4f, 0.8f, 1f}
        );
        arcPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Малюємо фонову дугу
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, backgroundArcPaint);

        // Рахуємо кут прогресу
        float speedRatio = Math.min(currentSpeed / maxSpeed, 1f);
        float progressAngle = SWEEP_ANGLE * speedRatio;

        // Малюємо дугу прогресу
        canvas.save();
        canvas.rotate(90, getWidth() / 2f, getHeight() / 2f); // Повертаємо градієнт для правильної позиції
        canvas.drawArc(arcBounds, START_ANGLE - 90, progressAngle, false, arcPaint);
        canvas.restore();

        // Малюємо стрілку
        float needleAngle = START_ANGLE + progressAngle;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = arcBounds.width() / 2f - 20f;

        float needleEndX = (float) (centerX + radius * Math.cos(Math.toRadians(needleAngle)));
        float needleEndY = (float) (centerY + radius * Math.sin(Math.toRadians(needleAngle)));

        canvas.drawCircle(centerX, centerY, 15f, needlePaint); // Центр стрілки
        needlePaint.setStrokeWidth(8f);
        canvas.drawLine(centerX, centerY, needleEndX, needleEndY, needlePaint); // Сама стрілка
    }

    // Метод для плавної анімації швидкості
    public void setSpeed(float speed) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentSpeed, speed);
        animator.setDuration(300); // 300мс на оновлення для плавності
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate(); // Перемальовуємо
        });
        animator.start();
    }

    public void reset() {
        setSpeed(0);
    }
}