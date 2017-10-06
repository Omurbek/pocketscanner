package com.example.jajac.pocketsummary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
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
    private CornersView mCornersView;
    private ImageView topLeft;
    private ImageView topRight;
    private ImageView bottomRight;
    private ImageView bottomLeft;

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
        mCornersView = this;
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
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.colorAccent));
        mPaint.setStrokeWidth(10);
        mPaint.setAntiAlias(true);
    }

    private ImageView getCornerView(int x, int y) {
        ImageView imageView = new ImageView(mContext);
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.ic_adjust_48dp);
        imageView.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent));
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener(new CornerTouchListener());
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

    private class CornerTouchListener implements OnTouchListener {
        PointF pressedPt = new PointF();
        PointF imgStartPt = new PointF();

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressedPt.x = event.getX();
                    pressedPt.y = event.getY();
                    imgStartPt.x = view.getX();
                    imgStartPt.y = view.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    PointF current = new PointF(event.getX() - pressedPt.x, event.getY() - pressedPt.y);
                    view.setX((int) (imgStartPt.x + current.x));
                    view.setY((int) (imgStartPt.y + current.y));
                    imgStartPt.x = view.getX();
                    imgStartPt.y = view.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            mCornersView.invalidate();
            return true;
        }
    }
}
