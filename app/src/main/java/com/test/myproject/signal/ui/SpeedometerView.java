package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private RectF arcBounds;

    private float currentSpeed = 0f;
    private float maxSpeed = 150f; // Максимальна швидкість

    private final float START_ANGLE = 135f;
    private final float SWEEP_ANGLE = 270f;

    private float centerX, centerY, radius;

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(40f); // Трохи товстіше
        backgroundArcPaint.setColor(Color.parseColor("#1AFFFFFF"));
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(40f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setColor(Color.parseColor("#4DFFFFFF"));
        tickPaint.setStrokeWidth(4f);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(Color.WHITE);

        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 60f; // Збільшили відступ для стрілки та поділок
        arcBounds.set(padding, padding, w - padding, h - padding);

        centerX = w / 2f;
        centerY = h / 2f;
        radius = arcBounds.width() / 2f;

        // Чіткий градієнт: Зелений (до 50) -> Жовтий (до 100) -> Червоний (150+)
        SweepGradient gradient = new SweepGradient(centerX, centerY, new int[]{Color.parseColor("#00C9FF"), Color.parseColor("#92FE9D"), Color.parseColor("#F6D365"), Color.parseColor("#FF512F")}, new float[]{0f, 0.33f, 0.66f, 1f});
        arcPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Малюємо фонову дугу
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, backgroundArcPaint);

        // 2. Малюємо поділки (Ticks)
        int numTicks = 30; // Кількість поділок
        for (int i = 0; i <= numTicks; i++) {
            float angle = START_ANGLE + (i * SWEEP_ANGLE / numTicks);
            double rad = Math.toRadians(angle);

            // Довші поділки для головних значень
            float tickLength = (i % 6 == 0) ? 25f : 10f;

            float startX = (float) (centerX + (radius - 30f) * Math.cos(rad));
            float startY = (float) (centerY + (radius - 30f) * Math.sin(rad));
            float stopX = (float) (centerX + (radius - 30f + tickLength) * Math.cos(rad));
            float stopY = (float) (centerY + (radius - 30f + tickLength) * Math.sin(rad));

            tickPaint.setStrokeWidth((i % 6 == 0) ? 6f : 3f);
            tickPaint.setColor((i % 6 == 0) ? Color.WHITE : Color.parseColor("#66FFFFFF"));
            canvas.drawLine(startX, startY, stopX, stopY, tickPaint);
        }

        // 3. Малюємо кольорову дугу прогресу
        float speedRatio = Math.min(currentSpeed / maxSpeed, 1f);
        float progressAngle = SWEEP_ANGLE * speedRatio;

        canvas.save();
        canvas.rotate(90, centerX, centerY);
        canvas.drawArc(arcBounds, START_ANGLE - 90, progressAngle, false, arcPaint);
        canvas.restore();

        // 4. Малюємо стрілку (тільки біля дуги, щоб не закривати текст в центрі)
        float needleAngle = START_ANGLE + progressAngle;
        double needleRad = Math.toRadians(needleAngle);

        // Стрілка починається не з центру, а ближче до шкали
        float innerNeedleRadius = radius - 50f;
        float outerNeedleRadius = radius + 20f; // Трохи виступає за шкалу

        // Малюємо стрілку у вигляді трикутника для гарного вигляду
        Path needlePath = new Path();
        float tipX = (float) (centerX + outerNeedleRadius * Math.cos(needleRad));
        float tipY = (float) (centerY + outerNeedleRadius * Math.sin(needleRad));

        // Бокові точки основи стрілки
        float baseLeftX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad - 0.05));
        float baseLeftY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad - 0.05));
        float baseRightX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad + 0.05));
        float baseRightY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad + 0.05));

        needlePath.moveTo(tipX, tipY);
        needlePath.lineTo(baseLeftX, baseLeftY);
        needlePath.lineTo(baseRightX, baseRightY);
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);

        // Крапка на основі стрілки
        float baseX = (float) (centerX + innerNeedleRadius * Math.cos(needleRad));
        float baseY = (float) (centerY + innerNeedleRadius * Math.sin(needleRad));
        canvas.drawCircle(baseX, baseY, 8f, needlePaint);
    }

    public void setSpeed(float speed) {
        // Динамічно змінюємо максимальну шкалу, якщо швидкість дуже висока
        if (speed > maxSpeed * 0.9f) maxSpeed *= 2f;

        ValueAnimator animator = ValueAnimator.ofFloat(currentSpeed, speed);
        animator.setDuration(250);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void reset() {
        maxSpeed = 150f;
        setSpeed(0);
    }
}