package com.example.elradardemoises.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.elradardemoises.R;
import com.example.elradardemoises.models.LLuvia;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeatherBackgroundHelper {

    private Context context;
    private ViewGroup containerView;
    private List<ImageView> particleViews;
    private AnimatorSet currentAnimatorSet;
    private Random random;

    public WeatherBackgroundHelper(Context context, ViewGroup containerView) {
        this.context = context;
        this.containerView = containerView;
        this.particleViews = new ArrayList<>();
        this.random = new Random();
    }


    public void applyWeatherBackground(LLuvia lluvia) {
        clearAnimations();

        applyGradientBackground(lluvia);

        if (lluvia.shouldShowParticles()) {
            createParticleAnimation(lluvia);
        }
    }

    private void applyGradientBackground(LLuvia lluvia) {
        int backgroundDrawableId = lluvia.getBackgroundAnimado(context);

        if (backgroundDrawableId != android.R.drawable.screen_background_dark) {
            try {
                containerView.setBackgroundResource(backgroundDrawableId);

                if (containerView.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) containerView.getBackground();
                    animationDrawable.start();
                }
            } catch (Exception e) {
                applyManualGradient(lluvia);
            }
        } else {
            applyManualGradient(lluvia);
        }
    }

    private void applyManualGradient(LLuvia lluvia) {
        int[] colors = lluvia.getBackgroundGradientColors();

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                colors
        );
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        containerView.setBackground(gradientDrawable);

        ValueAnimator alphaAnimator = ValueAnimator.ofInt(100, 255);
        alphaAnimator.setDuration(4000);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimator.addUpdateListener(animation -> {
            int alpha = (int) animation.getAnimatedValue();
            gradientDrawable.setAlpha(alpha);
        });
        alphaAnimator.start();
    }

    private void createParticleAnimation(LLuvia lluvia) {
        String particleType = lluvia.getParticleType();

        switch (particleType) {
            case "heavy_rain_drops":
                createRainAnimation(lluvia, 30, 1000);
                break;
            case "rain_drops":
                createRainAnimation(lluvia, 20, 1500);
                break;
            case "light_rain_drops":
                createRainAnimation(lluvia, 10, 2000);
                break;
            case "lightning_rain":
                createStormAnimation(lluvia);
                break;
            case "sun_rays":
                createSunRaysAnimation(lluvia);
                break;
            case "clouds":
                createCloudAnimation(lluvia);
                break;
        }
    }

    private void createRainAnimation(LLuvia lluvia, int particleCount, long baseDuration) {
        containerView.post(() -> {
            for (int i = 0; i < particleCount; i++) {
                ImageView rainDrop = new ImageView(context);
                rainDrop.setImageResource(R.drawable.rain_drop);
                rainDrop.setScaleX(0.5f + random.nextFloat() * 0.5f);
                rainDrop.setScaleY(0.5f + random.nextFloat() * 0.5f);

                int startX = random.nextInt(containerView.getWidth());
                int startY = -100;

                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(20, 40);
                rainDrop.setLayoutParams(params);
                rainDrop.setX(startX);
                rainDrop.setY(startY);

                containerView.addView(rainDrop);
                particleViews.add(rainDrop);

                ValueAnimator fallAnimator = ValueAnimator.ofFloat(startY, containerView.getHeight() + 100);
                fallAnimator.setDuration(baseDuration + random.nextInt(1000));
                fallAnimator.setRepeatCount(ValueAnimator.INFINITE);
                fallAnimator.setInterpolator(new LinearInterpolator());
                fallAnimator.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    rainDrop.setY(value);
                });

                fallAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        rainDrop.setTranslationX(0);
                        int newX = random.nextInt(containerView.getWidth());
                        rainDrop.setX(newX);
                    }

                    @Override public void onAnimationStart(Animator animation) { }
                    @Override public void onAnimationEnd(Animator animation) { }
                    @Override public void onAnimationCancel(Animator animation) { }
                });



                fallAnimator.setStartDelay(random.nextInt(2000));

                fallAnimator.start();
            }
        });
    }
    private void createStormAnimation(LLuvia lluvia) {
        createRainAnimation(lluvia, 40, 800);

        createLightningEffect();
    }

    private void createLightningEffect() {
        View lightningOverlay = new View(context);
        lightningOverlay.setBackgroundColor(0x80FFFFFF);
        lightningOverlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        lightningOverlay.setAlpha(0f);

        containerView.addView(lightningOverlay);

        Runnable lightningFlash = new Runnable() {
            @Override
            public void run() {
                ObjectAnimator flashAnimator = ObjectAnimator.ofFloat(lightningOverlay, "alpha", 0f, 1f, 0f);
                flashAnimator.setDuration(150);
                flashAnimator.start();

                containerView.postDelayed(this, 3000 + random.nextInt(7000));
            }
        };

        containerView.postDelayed(lightningFlash, 2000);
    }

    private void createSunRaysAnimation(LLuvia lluvia) {
        ImageView sunRays = new ImageView(context);
        sunRays.setImageResource(R.drawable.sun_rays);
        //posición
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(350, 550);
        params.leftMargin = 10;
        params.topMargin = 110;
        sunRays.setLayoutParams(params);
        sunRays.setScaleType(ImageView.ScaleType.CENTER);
        sunRays.setAlpha(0.8f);

        containerView.addView(sunRays);
        particleViews.add(sunRays);

        // Rotación
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(sunRays, "rotation", 0f, 360f);
        rotationAnimator.setDuration(20000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.start();

        //opacidad
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(sunRays, "alpha", 5f, 5f);
        alphaAnimator.setDuration(3000);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimator.start();
    }

    private void createCloudAnimation(LLuvia lluvia) {
        for (int i = 0; i < 3; i++) {
            ImageView cloud = new ImageView(context);
            cloud.setImageResource(R.drawable.cloud);
            cloud.setLayoutParams(new ViewGroup.LayoutParams(150, 80));
            cloud.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cloud.setAlpha(0.6f);

            int startX = random.nextInt(containerView.getWidth());
            int startY = random.nextInt(containerView.getHeight() / 3);
            cloud.setX(startX);
            cloud.setY(startY);

            containerView.addView(cloud);
            particleViews.add(cloud);

            ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(cloud, "translationX",
                    -50, containerView.getWidth() + 50);
            moveAnimator.setDuration(15000 + random.nextInt(10000));
            moveAnimator.setRepeatCount(ValueAnimator.INFINITE);
            moveAnimator.setRepeatMode(ValueAnimator.RESTART);
            moveAnimator.setInterpolator(new LinearInterpolator());
            moveAnimator.setStartDelay(random.nextInt(5000));
            moveAnimator.start();
        }
    }

    public void clearAnimations() {
        if (currentAnimatorSet != null) {
            currentAnimatorSet.cancel();
        }

        for (ImageView particle : particleViews) {
            containerView.removeView(particle);
        }
        particleViews.clear();
    }

    public void pauseAnimations() {
        if (currentAnimatorSet != null) {
            currentAnimatorSet.pause();
        }
    }
    public void resumeAnimations() {
        if (currentAnimatorSet != null) {
            currentAnimatorSet.resume();
        }
    }
}