package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

// Це наша нова інтерактивна "Водяна кнопка"
public class LiquidButtonView extends View {

    private Paint paint;
    private Paint textPaint;
    private Path path;

    private boolean isTesting = false;
    private float animationTime = 0f;
    private ValueAnimator liquidAnimator;

    private String buttonText = "СТАРТ";
    private int startColor = Color.parseColor("#00B4DB");
    private int endColor = Color.parseColor("#0083B0");

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
        textPaint.setTextSize(60f); // Розмір шрифту

        path = new Path();

        // Аніматор для постійної зміни форми "краплі"
        liquidAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        liquidAnimator.setDuration(3000); // 3 секунди на повний цикл коливань
        liquidAnimator.setRepeatCount(ValueAnimator.INFINITE);
        liquidAnimator.setInterpolator(new LinearInterpolator());
        liquidAnimator.addUpdateListener(animation -> {
            animationTime = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        LinearGradient gradient = new LinearGradient(0, 0, w, h, startColor, endColor, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(centerX, centerY) - 20f; // Відступ для амплітуди хвиль

        path.reset();

        // Кількість контрольних точок для форми кола/краплі
        int numPoints = 60;
        for (int i = 0; i <= numPoints; i++) {
            float angle = (float) (i * Math.PI * 2 / numPoints);

            float radius = baseRadius;

            // Якщо тестування активне, додаємо ефект "води/бульбашки"
            if (isTesting) {
                // ВИПРАВЛЕНО: Використовуємо цілі множники для animationTime (1f та 2f),
                // щоб після завершення циклу (2 * PI) хвиля ідеально збігалася з початком, без "ривків"
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

        // Малюємо форму кнопки
        canvas.drawPath(path, paint);

        // Малюємо текст по центру
        // Корегуємо Y координату, щоб текст був чітко по центру по вертикалі
        float textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(buttonText, centerX, textY, textPaint);
    }

    // Зміна стану кнопки (СТАРТ/СТОП та анімація води)
    public void setTestingState(boolean testing) {
        this.isTesting = testing;
        if (testing) {
            buttonText = "СТОП";
            startColor = Color.parseColor("#FF512F");
            endColor = Color.parseColor("#DD2476"); // Червонуватий градієнт для стоп
            liquidAnimator.start();
        } else {
            buttonText = "СТАРТ";
            startColor = Color.parseColor("#00B4DB");
            endColor = Color.parseColor("#0083B0"); // Синій градієнт
            liquidAnimator.cancel();
            animationTime = 0f;
        }

        // Оновлюємо градієнт
        LinearGradient gradient = new LinearGradient(0, 0, getWidth(), getHeight(), startColor, endColor, Shader.TileMode.CLAMP);
        paint.setShader(gradient);

        invalidate();
    }
}