package com.example.zk_final_433;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class MyDrawingArea extends View {
    private Path path = new Path();
    private Bitmap bmp;
    private boolean isDrawing = false;
    private OnDrawingCompleteListener listener;

    public interface OnDrawingCompleteListener {
        void onDrawingComplete(Bitmap drawing);
    }
    public void setOnDrawingCompleteListener(OnDrawingCompleteListener listener) {
        this.listener = listener;
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyDrawingArea(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        canvas.drawPath(path, paint);

        if (!isDrawing) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(50f);
            paint.setColor(Color.GRAY);
            canvas.drawText("Drawing Area", getWidth() / 3, getHeight() / 2, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            isDrawing = true;
            path.moveTo(x, y);
            invalidate();
        } else if (action == MotionEvent.ACTION_MOVE) {
            path.lineTo(x, y);
            invalidate();
        }
        if (action == MotionEvent.ACTION_UP) {
            if (listener != null) {
                listener.onDrawingComplete(getBitmap());
            }
        }
        return true;
    }

    public void clearDraw() {
        path.reset();
        isDrawing = false;
        invalidate();
    }

    public Bitmap getBitmap() {
        bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        c.drawColor(Color.WHITE);

        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5f);
        c.drawPath(path, p);

        return bmp;
    }
}
