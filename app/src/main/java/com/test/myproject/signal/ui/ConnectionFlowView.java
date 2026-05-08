package com.test.myproject.signal.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ConnectionFlowView extends View {
    private Paint iconPaint;
    private Paint linePaint;
    private Paint arrowPaint;
    private Paint textPaint;

    public enum State { IDLE, HANDSHAKE, DOWNLOADING, UPLOADING }
    private State currentState = State.IDLE;

    private ValueAnimator flowAnimator;
    private float flowOffset = 0f;

    public ConnectionFlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.parseColor("#00C9FF"));
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#44FFFFFF"));
        linePaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.parseColor("#92FE9D"));
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        flowAnimator = ValueAnimator.ofFloat(0f, 1f);
        flowAnimator.setDuration(800);
        flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flowAnimator.setInterpolator(new LinearInterpolator());
        flowAnimator.addUpdateListener(animation -> {
            flowOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setState(State state) {
        this.currentState = state;
        if (state == State.IDLE) {
            flowAnimator.cancel();
            iconPaint.setColor(Color.parseColor("#66FFFFFF"));
        } else {
            if (!flowAnimator.isRunning()) flowAnimator.start();
            iconPaint.setColor(Color.parseColor("#00C9FF"));

            if (state == State.DOWNLOADING) {
                arrowPaint.setColor(Color.parseColor("#92FE9D"));
                flowAnimator.setDuration(400);
            } else if (state == State.UPLOADING) {
                arrowPaint.setColor(Color.parseColor("#00C9FF"));
                flowAnimator.setDuration(400);
            } else if (state == State.HANDSHAKE) {
                arrowPaint.setColor(Color.parseColor("#F6D365"));
                flowAnimator.setDuration(1200);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float iconWidth = 22f * density;
        float iconHeight = 36f * density;
        float serverWidth = 28f * density;
        float centerY = h / 2f - 12f * density;

        iconPaint.setStrokeWidth(2f * density);
        linePaint.setStrokeWidth(1.5f * density);
        arrowPaint.setStrokeWidth(2.5f * density);
        textPaint.setTextSize(11f * density);

        float phoneTextWidth = textPaint.measureText("Мій пристрій");
        float serverTextWidth = textPaint.measureText("Сервер");
        float phoneCenterX = Math.max(iconWidth / 2f, phoneTextWidth / 2f) + 16f * density;
        float serverCenterX = w - (Math.max(serverWidth / 2f, serverTextWidth / 2f) + 16f * density);

        RectF phoneRect = new RectF(phoneCenterX - iconWidth/2, centerY - iconHeight/2, phoneCenterX + iconWidth/2, centerY + iconHeight/2);
        canvas.drawRoundRect(phoneRect, 4f * density, 4f * density, iconPaint);
        canvas.drawLine(phoneCenterX - iconWidth*0.2f, centerY - iconHeight/2 + 5f * density, phoneCenterX + iconWidth*0.2f, centerY - iconHeight/2 + 5f * density, iconPaint);

        RectF serverRect = new RectF(serverCenterX - serverWidth/2, centerY - iconHeight/2, serverCenterX + serverWidth/2, centerY + iconHeight/2);
        canvas.drawRoundRect(serverRect, 4f * density, 4f * density, iconPaint);
        canvas.drawLine(serverCenterX - serverWidth/2, centerY, serverCenterX + serverWidth/2, centerY, iconPaint);
        canvas.drawCircle(serverCenterX + serverWidth*0.25f, centerY - 6f * density, 1.5f * density, iconPaint);
        canvas.drawCircle(serverCenterX + serverWidth*0.25f, centerY + 6f * density, 1.5f * density, iconPaint);
        canvas.drawText("Мій пристрій", phoneCenterX, centerY + iconHeight/2 + 18f * density, textPaint);
        canvas.drawText("Сервер", serverCenterX, centerY + iconHeight/2 + 18f * density, textPaint);

        float startX = phoneCenterX + iconWidth/2 + 12f * density;
        float endX = serverCenterX - serverWidth/2 - 12f * density;
        canvas.drawLine(startX, centerY, endX, centerY, linePaint);

        if (currentState != State.IDLE) {
            float lineLength = endX - startX;
            float arrowSpacing = 20f * density;
            int numArrows = (int) (lineLength / arrowSpacing) + 1;

            canvas.save();
            canvas.clipRect(startX, 0, endX, h);

            for (int i = -1; i <= numArrows; i++) {
                float x = 0;
                Path arrowPath = new Path();
                float arrowSize = 6f * density;

                if (currentState == State.DOWNLOADING) {
                    x = endX - (i * arrowSpacing) - (flowOffset * arrowSpacing);
                    arrowPath.moveTo(x + arrowSize, centerY - arrowSize);
                    arrowPath.lineTo(x, centerY);
                    arrowPath.lineTo(x + arrowSize, centerY + arrowSize);
                } else if (currentState == State.UPLOADING) {
                    x = startX + (i * arrowSpacing) + (flowOffset * arrowSpacing);
                    arrowPath.moveTo(x - arrowSize, centerY - arrowSize);
                    arrowPath.lineTo(x, centerY);
                    arrowPath.lineTo(x - arrowSize, centerY + arrowSize);
                } else if (currentState == State.HANDSHAKE) {
                    float centerPulse = startX + lineLength/2;
                    float offset = (float) Math.sin(flowOffset * Math.PI * 2) * (lineLength/4);
                    canvas.drawCircle(centerPulse + offset, centerY, 3f * density, arrowPaint);
                }

                if (currentState != State.HANDSHAKE) {
                    canvas.drawPath(arrowPath, arrowPaint);
                }
            }
            canvas.restore();
        }
    }
}