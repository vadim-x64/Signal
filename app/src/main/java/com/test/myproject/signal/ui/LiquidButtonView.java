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

// Оновлена "Водяна кнопка" з 3D ефектом глянцю та опуклості
public class LiquidButtonView extends View {

    private Paint paint;
    private Paint textPaint;
    private Paint glossPaint;   // Для верхнього бліку
    private Paint borderPaint;  // Для скляної фаски
    private Path path;
    private RectF glossRect;

    private boolean isTesting = false;
    private float animationTime = 0f;
    private ValueAnimator liquidAnimator;
    private ValueAnimator colorAnimator; // ДОДАНО: Аніматор кольорів

    private String buttonText = "СТАРТ";
    // Трохи яскравіший стартовий колір для більшого контрасту об'єму
    private int startColor = Color.parseColor("#00C9FF");
    private int endColor = Color.parseColor("#0083B0");

    // Константні кольори для станів
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
        // Додаємо легку тінь тексту, щоб він "летів" над глянцем
        textPaint.setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"));

        glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glossPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f); // Товщина внутрішнього скла

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

        // ДОДАНО: Ініціалізація аніматора кольорів (без запуску)
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(400); // 400мс на плавний перехід кольору
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateVisuals(w, h);
    }

    // Метод для оновлення градієнтів при зміні розміру або стану (СТАРТ/СТОП)
    private void updateVisuals(int w, int h) {
        if (w == 0 || h == 0) return;

        // 1. Радіальний градієнт замість лінійного (створює об'єм / 3D ефект "опуклості").
        // Джерело світла зміщене вліво-вгору (w*0.3, h*0.3)
        float lightX = w * 0.3f;
        float lightY = h * 0.3f;
        float radius = Math.min(w, h) * 0.7f;
        RadialGradient radialGradient = new RadialGradient(
                lightX, lightY, radius,
                startColor, endColor, Shader.TileMode.CLAMP
        );
        paint.setShader(radialGradient);

        // Параметри для малювання бліку
        float centerX = w / 2f;
        float centerY = h / 2f;
        float baseRadius = Math.min(centerX, centerY) - 20f;

        // 2. Формуємо блік (глянець зверху)
        glossRect.set(
                centerX - baseRadius * 0.65f, // Ширина бліку
                centerY - baseRadius * 0.85f, // Верхній край
                centerX + baseRadius * 0.65f,
                centerY - baseRadius * 0.15f  // Нижній край бліку
        );
        LinearGradient glossGradient = new LinearGradient(
                0, glossRect.top, 0, glossRect.bottom,
                Color.parseColor("#99FFFFFF"), // 60% білий (світлий відблиск)
                Color.parseColor("#00FFFFFF"), // Прозорий
                Shader.TileMode.CLAMP
        );
        glossPaint.setShader(glossGradient);

        // 3. Внутрішнє світіння краю (фаска скла 3D)
        LinearGradient borderGradient = new LinearGradient(
                centerX - baseRadius, centerY - baseRadius,
                centerX + baseRadius, centerY + baseRadius,
                new int[]{Color.parseColor("#88FFFFFF"), Color.TRANSPARENT, Color.parseColor("#44000000")},
                new float[]{0f, 0.5f, 1f}, // Від білого світла зліва-згори до тіні справа-знизу
                Shader.TileMode.CLAMP
        );
        borderPaint.setShader(borderGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(centerX, centerY) - 20f;

        path.reset();

        int numPoints = 60;
        for (int i = 0; i <= numPoints; i++) {
            float angle = (float) (i * Math.PI * 2 / numPoints);
            float radius = baseRadius;

            // Якщо тестування активне, додаємо ефект "води"
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

        // 1. Малюємо основну форму (з опуклим градієнтом)
        canvas.drawPath(path, paint);

        // 2. Малюємо глянець і фаску ТІЛЬКИ коли кнопка у стані очікування (ідеально кругла)
        if (!isTesting) {
            canvas.drawPath(path, borderPaint);     // Внутрішня рамка (скляний край)
            canvas.drawOval(glossRect, glossPaint); // Верхній скляний відблиск
        }

        // 3. Малюємо текст по центру
        float textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(buttonText, centerX, textY, textPaint);
    }

    // Зміна стану кнопки з ПЛАВНОЮ анімацією кольорів
    public void setTestingState(boolean testing) {
        this.isTesting = testing;

        // Зупиняємо попередню анімацію кольору, якщо вона ще йде
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
        } else {
            buttonText = "СТАРТ";
            targetStartColor = IDLE_START_COLOR;
            targetEndColor = IDLE_END_COLOR;
            liquidAnimator.cancel();
            animationTime = 0f;
        }

        // ДОДАНО: Плавний перехід від поточних кольорів до цільових
        final int currentStartColor = startColor;
        final int currentEndColor = endColor;

        android.animation.ArgbEvaluator evaluator = new android.animation.ArgbEvaluator();

        colorAnimator.removeAllUpdateListeners();
        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            startColor = (int) evaluator.evaluate(fraction, currentStartColor, targetStartColor);
            endColor = (int) evaluator.evaluate(fraction, currentEndColor, targetEndColor);

            // Під час переходу кольорів оновлюємо градієнти
            updateVisuals(getWidth(), getHeight());
            invalidate();
        });

        colorAnimator.start();
    }
}