package com.test.myproject.signal.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.test.myproject.signal.R;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION = 3000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.imgSplashLogo);
        View vRipple = findViewById(R.id.vRipple);
        TextView tvTitle = findViewById(R.id.tvSplashTitle);
        TextView tvSubtitle = findViewById(R.id.tvSplashSubtitle);

        imgLogo.setScaleX(0.3f);
        imgLogo.setScaleY(0.3f);
        imgLogo.setAlpha(0f);
        imgLogo.setRotation(0f);

        vRipple.setScaleX(1f);
        vRipple.setScaleY(1f);
        vRipple.setAlpha(0f);

        tvTitle.setTranslationY(40f);
        tvTitle.setAlpha(0f);

        tvSubtitle.setTranslationY(20f);
        tvSubtitle.setAlpha(0f);

        ObjectAnimator logoRotation = ObjectAnimator.ofFloat(imgLogo, View.ROTATION, 0f, 360f);
        logoRotation.setDuration(900);
        logoRotation.setInterpolator(new DecelerateInterpolator(1.8f));

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(imgLogo, View.SCALE_X, 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(imgLogo, View.SCALE_Y, 0.3f, 1f);
        logoScaleX.setDuration(900);
        logoScaleY.setDuration(900);
        logoScaleX.setInterpolator(new OvershootInterpolator(1.5f));
        logoScaleY.setInterpolator(new OvershootInterpolator(1.5f));

        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(imgLogo, View.ALPHA, 0f, 1f);
        logoAlpha.setDuration(400);

        AnimatorSet logoEntrance = new AnimatorSet();
        logoEntrance.playTogether(logoRotation, logoScaleX, logoScaleY, logoAlpha);
        logoEntrance.setStartDelay(0);

        ObjectAnimator rippleScaleX = ObjectAnimator.ofFloat(vRipple, View.SCALE_X, 1f, 2.8f);
        ObjectAnimator rippleScaleY = ObjectAnimator.ofFloat(vRipple, View.SCALE_Y, 1f, 2.8f);
        ObjectAnimator rippleAlphaIn = ObjectAnimator.ofFloat(vRipple, View.ALPHA, 0f, 0.7f, 0f);

        rippleScaleX.setDuration(700);
        rippleScaleY.setDuration(700);
        rippleAlphaIn.setDuration(700);
        rippleScaleX.setInterpolator(new DecelerateInterpolator(2f));
        rippleScaleY.setInterpolator(new DecelerateInterpolator(2f));
        rippleAlphaIn.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet rippleAnim = new AnimatorSet();
        rippleAnim.playTogether(rippleScaleX, rippleScaleY, rippleAlphaIn);
        rippleAnim.setStartDelay(850);

        ObjectAnimator logoPulseX = ObjectAnimator.ofFloat(imgLogo, View.SCALE_X, 1f, 1.12f, 1f);
        ObjectAnimator logoPulseY = ObjectAnimator.ofFloat(imgLogo, View.SCALE_Y, 1f, 1.12f, 1f);
        logoPulseX.setDuration(350);
        logoPulseY.setDuration(350);
        logoPulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        logoPulseY.setInterpolator(new AccelerateDecelerateInterpolator());
        logoPulseX.setStartDelay(900);
        logoPulseY.setStartDelay(900);

        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(tvTitle, View.ALPHA, 0f, 1f);
        ObjectAnimator titleSlide = ObjectAnimator.ofFloat(tvTitle, View.TRANSLATION_Y, 40f, 0f);
        titleAlpha.setDuration(700);
        titleSlide.setDuration(700);
        titleAlpha.setInterpolator(new DecelerateInterpolator(1.5f));
        titleSlide.setInterpolator(new DecelerateInterpolator(1.5f));
        titleAlpha.setStartDelay(400);
        titleSlide.setStartDelay(400);

        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(tvSubtitle, View.ALPHA, 0f, 1f);
        ObjectAnimator subtitleSlide = ObjectAnimator.ofFloat(tvSubtitle, View.TRANSLATION_Y, 20f, 0f);
        subtitleAlpha.setDuration(500);
        subtitleSlide.setDuration(500);
        subtitleAlpha.setInterpolator(new DecelerateInterpolator());
        subtitleSlide.setInterpolator(new DecelerateInterpolator());
        subtitleAlpha.setStartDelay(1100);
        subtitleSlide.setStartDelay(1100);

        AnimatorSet masterSet = new AnimatorSet();
        masterSet.playTogether(
                logoEntrance,
                rippleAnim,
                logoPulseX,
                logoPulseY,
                titleAlpha,
                titleSlide,
                subtitleAlpha,
                subtitleSlide
        );

        masterSet.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}