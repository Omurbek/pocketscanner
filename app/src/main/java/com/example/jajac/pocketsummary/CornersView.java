package com.example.jajac.pocketsummary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;


public class CornersView extends FrameLayout {

    private Context mContext;
    private Paint mPaint;
    private ImageView topLeft;
    private ImageView topRight;
    private ImageView bottomRight;
    private ImageView bottomLeft;

    private ImageView mClickedCorner;

    public CornersView(@NonNull Context context) {
        super(context);
        this.mContext = context;
        create();
    }

    public CornersView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        create();
    }

    public CornersView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        create();
    }

    private void create() {
        topLeft = getCornerView(0, 0);
        topRight = getCornerView(getWidth(), 0);
        bottomRight = getCornerView(getWidth(), getHeight());
        bottomLeft = getCornerView(0, getHeight());

        addView(topLeft);
        addView(topRight);
        addView(bottomRight);
        addView(bottomLeft);

        createPaint();
    }

    private void createPaint() {
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        mPaint.setStrokeWidth(5);
        mPaint.setAntiAlias(true);
    }

    private ImageView getCornerView(int x, int y) {
        ImageView imageView = new ImageView(mContext);
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.ic_circle_24dp);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener((view, motionEvent) -> onCornerTouch(view, motionEvent));
        return imageView;
    }

    public List<Point> getPoints() {
        List<Point> points = new ArrayList<>();
        float offset = topLeft.getWidth() / 2;
        points.add(new Point((int) (topLeft.getX() + offset), (int) (topLeft.getY() + offset)));
        points.add(new Point((int) (topRight.getX() + offset), (int) (topRight.getY() + offset)));
        points.add(new Point((int) (bottomRight.getX() + offset), (int) (bottomRight.getY() + offset)));
        points.add(new Point((int) (bottomLeft.getX() + offset), (int) (bottomLeft.getY() + offset)));
        return points;
    }

    public void setPoints(List<Point> points) {
        float offset = topLeft.getWidth() / 2;
        topLeft.setX(points.get(0).x - offset);
        topLeft.setY(points.get(0).y - offset);

        topRight.setX(points.get(1).x - offset);
        topRight.setY(points.get(1).y - offset);

        bottomRight.setX(points.get(2).x - offset);
        bottomRight.setY(points.get(2).y - offset);

        bottomLeft.setX(points.get(3).x - offset);
        bottomLeft.setY(points.get(3).y - offset);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        float offset = topLeft.getWidth() / 2;
        canvas.drawLine(topLeft.getX() + offset, topLeft.getY() + offset,
                topRight.getX() + offset, topRight.getY() + offset, mPaint);
        canvas.drawLine(topRight.getX() + offset, topRight.getY() + offset,
                bottomRight.getX() + offset, bottomRight.getY() + offset, mPaint);
        canvas.drawLine(bottomRight.getX() + offset, bottomRight.getY() + offset,
                bottomLeft.getX() + offset, bottomLeft.getY() + offset, mPaint);
        canvas.drawLine(bottomLeft.getX() + offset, bottomLeft.getY() + offset,
                topLeft.getX() + offset, topLeft.getY() + offset, mPaint);
    }

    private boolean onCornerTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (view == topLeft || view == topRight || view == bottomRight || view == bottomLeft) {
                    mClickedCorner = (ImageView) view;
                }
                break;
            case MotionEvent.ACTION_UP:
                mClickedCorner = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mClickedCorner != null) {
                    mClickedCorner.setX(mClickedCorner.getX() + motionEvent.getX());
                    mClickedCorner.setY(mClickedCorner.getY() + motionEvent.getY());
                }
                break;
        }
        this.invalidate();
        return true;
    }
}
