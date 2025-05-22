package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class GlowingGradientTextView extends AppCompatTextView {

    private Paint paint;
    private LinearGradient gradient;
    private Matrix matrix;
    private float translateX = 0;
    private int viewWidth = 0;

    // ✅ Add these missing fields:
    private int streak = 0;
    private float speedFactor = 20f;

    public GlowingGradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = getPaint();
        matrix = new Matrix();
        post(animateGradient);
    }

    private final Runnable animateGradient = new Runnable() {
        @Override
        public void run() {
            translateX += viewWidth / speedFactor;

            if (translateX > viewWidth * 3) {
                translateX = -viewWidth;
            }

            if (gradient != null) {
                matrix.setTranslate(translateX, 0);
                gradient.setLocalMatrix(matrix);
                invalidate();
            }

            postDelayed(this, 50); // ~20 FPS
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w > 0) {
            viewWidth = w;
            if (gradient == null && this.streak > 0) {
                setStreak(this.streak);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setStreak(int streak) {
        this.streak = streak;

        // Fallback to 1 if 0 or negative
        int safeStreak = Math.max(streak, 1);

        int[] allColors = new int[]{
                Color.parseColor("#AFFF33"), // Lime
                Color.YELLOW,
                Color.GREEN,
                Color.BLUE,
                Color.WHITE,
                Color.parseColor("#4B0082"), // Indigo
                Color.parseColor("#32CD32"), // LimeGreen (OKX)
                Color.BLACK,
                Color.parseColor("#FFD700"), // Gold
                Color.RED,
                Color.MAGENTA,
                Color.CYAN
        };

        int maxColors = allColors.length;

        // Clamp streak to not exceed available colors
        int useColors = Math.min(safeStreak, maxColors);

        int[] colors = new int[useColors];
        System.arraycopy(allColors, 0, colors, 0, useColors);

        // Adjust speed — higher streak = faster
        speedFactor = Math.max(30f - (streak * 2), 6f); // Minimum speedFactor = 6

        if (viewWidth == 0) return;

        int gradientWidth = viewWidth * 3;
        gradient = new LinearGradient(0, 0, gradientWidth, 0, colors, null, Shader.TileMode.MIRROR);
        paint.setShader(gradient);
        paint.setAntiAlias(true);
        paint.setDither(true);
    }
}