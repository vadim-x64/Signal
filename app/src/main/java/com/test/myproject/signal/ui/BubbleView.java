package com.test.myproject.signal.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BubbleView extends View {
    private Paint paint;
    private List<Bubble> bubbles;
    private boolean isRunning = false;
    private Random random;

    public BubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#00C9FF"));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        bubbles = new ArrayList<>();
        random = new Random();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bubbles.clear();

        for (int i = 0; i < 15; i++) {
            bubbles.add(new Bubble(w, h));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isRunning) return;

        for (Bubble b : bubbles) {
            paint.setAlpha(b.alpha);
            canvas.drawCircle(b.x, b.y, b.radius, paint);
            b.update();
        }

        invalidate();
    }

    public void startAnimation() {
        isRunning = true;
        invalidate();
    }

    public void stopAnimation() {
        isRunning = false;

        for (Bubble b : bubbles) {
            b.reset();
        }

        invalidate();
    }

    private class Bubble {
        float x, y, radius, speed;
        int alpha;
        int viewWidth, viewHeight;

        Bubble(int w, int h) {
            viewWidth = w;
            viewHeight = h;
            reset();
        }

        void reset() {
            radius = random.nextInt(12) + 6;
            x = random.nextInt(viewWidth);
            y = viewHeight + radius;
            speed = random.nextFloat() * 3 + 2;
            alpha = random.nextInt(155) + 100;
        }

        void update() {
            y -= speed;
            x += (random.nextFloat() - 0.5f) * 2;
            alpha -= 3;

            if (y + radius < 0 || alpha <= 0) {
                reset();
            }
        }
    }
}