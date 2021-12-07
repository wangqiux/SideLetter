package com.wqx.sideletterview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * 创建日期：2021/12/1 14:38
 *
 * @author wqx
 * 包名： com.wqx.slideletterview
 * 类说明：
 */
public class SlideLetterView extends View {

    //字母列表
    private List<String> mLetters;

    //字体大小
    private int mTextSize;

    //默认字体颜色
    private int mNormalTextColor;

    //选中字体颜色
    private int mSelectedTextColor;

    //选中字体大小
    private int mSelectedTextSize;

    //字母间距
    private int mLetterMargin;

    //字母画笔
    private Paint mLetterPaint;

    //选中字母画笔
    private Paint mSelectedLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 波浪画笔
    private Paint mWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 选中字体的坐标
    private float mPointX, mPointY;

    // 当前选中的位置
    private int mChoosePosition = 0;


    // 圆形中心点X
    private float mCircleCenterX;

    //提示文字的背景颜色
    private int mCircleTextBgColor;

    //提示文字大小
    private int mCircleTextSize;

    //曲线背景
    private int mWaveColor;

    //控件宽度
    private int mWidth;

    //控件高度
    private int mItemHeight;

    private int mOldPosition;

    private int mNewPosition;


    // 计算波浪贝塞尔曲线的角弧长值
    private static final double ANGLE = Math.PI * 45 / 180;
    private static final double ANGLE_R = Math.PI * 90 / 180;
    // 波浪路径
    private Path mWavePath = new Path();

    // 圆形路径
    private Path mCirclePath = new Path();

    // 手指滑动的Y点作为中心点
    private int mCenterY; //中心点Y

    // 贝塞尔曲线的分布半径
    private int mRadius;

    // 圆形半径
    private int mCircleRadius;
    // 用于过渡效果计算
    private ValueAnimator mRatioAnimator;

    // 用于绘制贝塞尔曲线的比率
    private float mRatio;

    //是否是滑动
    private boolean isSlide = false;

    private OnTouchLetterChangeListener mListener;

    public SlideLetterView(Context context) {
        super(context);
    }

    public SlideLetterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SlideLetterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SlideLetterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        mLetters = Arrays.asList(context.getResources().getStringArray(R.array.slideLetters));
        int textSize = context.getResources().getDimensionPixelSize(R.dimen.sp_11);
        int margin = context.getResources().getDimensionPixelSize(R.dimen.dp_2);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.slideLetterView);
            mNormalTextColor = a.getColor(R.styleable.slideLetterView_normalTextColor, Color.parseColor("#333333"));
            mSelectedTextColor = a.getColor(R.styleable.slideLetterView_selectedTextColor, Color.parseColor("#2C8DF8"));
            mTextSize = a.getDimensionPixelSize(R.styleable.slideLetterView_normalTextSize, textSize);
            mSelectedTextSize = a.getDimensionPixelSize(R.styleable.slideLetterView_selectedTextSize, textSize);
            mLetterMargin = a.getDimensionPixelSize(R.styleable.slideLetterView_letterMargin, margin);
            mCircleTextBgColor = a.getColor(R.styleable.slideLetterView_chooseTextColor, Color.parseColor("#ffffff"));
            mCircleTextSize = a.getDimensionPixelSize(R.styleable.slideLetterView_hintTextSize, textSize);
            mWaveColor = a.getColor(R.styleable.slideLetterView_circleBackground, Color.parseColor("#2C8DF8"));
            mRadius = a.getDimensionPixelSize(R.styleable.slideLetterView_radius,
                    context.getResources().getDimensionPixelSize(R.dimen.dp_15));
            mCircleRadius = a.getDimensionPixelSize(R.styleable.slideLetterView_circleRadius,
                    context.getResources().getDimensionPixelSize(R.dimen.dp_15));
            a.recycle();
        }
        mLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectedLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWavePaint = new Paint();
        mWavePaint.setAntiAlias(true);
        mWavePaint.setStyle(Paint.Style.FILL);
        mWavePaint.setColor(mWaveColor);
        mSelectedLetterPaint.setAntiAlias(true);
        mSelectedLetterPaint.setColor(mCircleTextBgColor);
        mSelectedLetterPaint.setStyle(Paint.Style.FILL);
        mSelectedLetterPaint.setTextSize(mCircleTextSize);
        mSelectedLetterPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mItemHeight = mTextSize + mLetterMargin;
        mPointX = mWidth - mTextSize;
        setMeasuredDimension(mWidth, mItemHeight * mLetters.size() + 20);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLetter(canvas);
        drawWavePath(canvas);
        drawCirclePath(canvas);
        drawSelectedLetter(canvas);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float y = event.getY();
        final float x = event.getX();
        mOldPosition = mChoosePosition;
        mNewPosition = (int) (y / (mItemHeight * mLetters.size()) * mLetters.size());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("position", mNewPosition + "");
                //限定触摸范围
                if (x < mWidth - 1.5 * mRadius) {
                    return false;
                }
                if (mNewPosition > mLetters.size()-1) {
                    return false;
                }
                mCenterY = (int) y;
                startAnimator(1.0f);

                isSlide = false;
                mChoosePosition = mNewPosition;
                if (mListener != null) {
                    mListener.onLetterChange(mLetters.get(mChoosePosition));
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                isSlide = true;
                mCenterY = (int) y;
                if (mOldPosition != mNewPosition) {
                    if (mNewPosition >= 0 && mNewPosition < mLetters.size()) {
                        mChoosePosition = mNewPosition;
                        if (mListener != null) {
                            mListener.onLetterChange(mLetters.get(mNewPosition));
                        }
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isSlide = false;
                startAnimator(0f);
//                mChoosePosition = -1;
                break;
            default:
                break;
        }
        return true;
    }


    /**
     * 绘制字母
     *
     * @param canvas
     */
    private void drawLetter(Canvas canvas) {
        for (int i = 0; i < mLetters.size(); i++) {
            mLetterPaint.reset();
            mLetterPaint.setAntiAlias(true);
            mLetterPaint.setTextAlign(Paint.Align.CENTER);
            mLetterPaint.setTextSize(mTextSize);
            mLetterPaint.setColor(mNormalTextColor);

            Paint.FontMetrics fontMetrics = mLetterPaint.getFontMetrics();
            float baseline = Math.abs(-fontMetrics.bottom - fontMetrics.top);

            float pointY = mItemHeight * i + baseline + 20;
            if (i == mChoosePosition) {
                mPointY = pointY;
            } else {
                canvas.drawText(mLetters.get(i).substring(0, 1), mPointX, pointY, mLetterPaint);
            }
        }
    }

    /**
     * 绘制选中的字母
     *
     * @param canvas
     */
    private void drawSelectedLetter(Canvas canvas) {
        if (mChoosePosition != -1) {
            // 绘制右侧选中字符
            mSelectedLetterPaint.reset();
            mSelectedLetterPaint.setTextSize(mSelectedTextSize);
            if (isSlide) {
                mSelectedLetterPaint.setColor(mCircleTextBgColor);
            } else {
                mSelectedLetterPaint.setColor(mSelectedTextColor);
            }
            mSelectedLetterPaint.setTextAlign(Paint.Align.CENTER);
            mSelectedLetterPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(mLetters.get(mChoosePosition).substring(0, 1), mPointX, mPointY, mSelectedLetterPaint);
        }
        // 绘制提示字符
        if (mRatio >= 0.9f) {
            String target = mLetters.get(mChoosePosition).substring(0, 1);
            Paint.FontMetrics fontMetrics = mSelectedLetterPaint.getFontMetrics();
            float baseline = Math.abs(-fontMetrics.bottom - fontMetrics.top);
            float x = mCircleCenterX;
            float y = mCenterY + baseline / 2;
            canvas.drawText(target, x, y, mSelectedLetterPaint);
        }
    }

    /**
     * 绘制波浪
     *
     * @param canvas
     */
    private void drawWavePath(Canvas canvas) {
        mWavePath.reset();
        // 移动到起始点
        mWavePath.moveTo(mWidth, mCenterY - 3 * mRadius);
        //计算上部控制点的Y轴位置
        int controlTopY = mCenterY - 2 * mRadius;

        //计算上部结束点的坐标
        int endTopX = (int) (mWidth - mRadius * Math.cos(ANGLE) * mRatio);
        int endTopY = (int) (controlTopY + mRadius * Math.sin(ANGLE));
        mWavePath.quadTo(mWidth, controlTopY, endTopX, endTopY);

        //计算中心控制点的坐标
        int controlCenterX = (int) (mWidth - 1.8f * mRadius * Math.sin(ANGLE_R) * mRatio);
        int controlCenterY = mCenterY;
        //计算下部结束点的坐标
        int controlBottomY = mCenterY + 2 * mRadius;
        int endBottomX = endTopX;
        int endBottomY = (int) (controlBottomY - mRadius * Math.cos(ANGLE));
        mWavePath.quadTo(controlCenterX, controlCenterY, endBottomX, endBottomY);

        mWavePath.quadTo(mWidth, controlBottomY, mWidth, controlBottomY + mRadius);

        mWavePath.close();
        canvas.drawPath(mWavePath, mWavePaint);
    }

    private void startAnimator(float value) {
        if (mRatioAnimator == null) {
            mRatioAnimator = new ValueAnimator();
        }
        mRatioAnimator.cancel();
        mRatioAnimator.setFloatValues(value);
        mRatioAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator value) {

                mRatio = (float) value.getAnimatedValue();
                //球弹到位的时候，并且点击的位置变了，即点击的时候显示当前选择位置
                if (mRatio == 1f && mOldPosition != mNewPosition) {
                    if (mNewPosition >= 0 && mNewPosition < mLetters.size()) {
                        mChoosePosition = mNewPosition;
                        if (mListener != null) {
                            mListener.onLetterChange(mLetters.get(mNewPosition));
                        }
                    }
                }
                invalidate();
            }
        });
        mRatioAnimator.start();
    }

    /**
     * 绘制左边提示的圆
     *
     * @param canvas
     */
    private void drawCirclePath(Canvas canvas) {
        //x轴的移动路径
        mCircleCenterX = (mWidth + mCircleRadius) - (2.0f * mRadius + 2.0f * mCircleRadius) * mRatio;

        mCirclePath.reset();
        mCirclePath.addCircle(mCircleCenterX, mCenterY, mCircleRadius, Path.Direction.CW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCirclePath.op(mWavePath, Path.Op.DIFFERENCE);
        }

        mCirclePath.close();
        canvas.drawPath(mCirclePath, mWavePaint);

    }

    /**
     * 设置当前选中
     *
     * @param pos
     */
    public void setCurrentPos(int pos) {
        if (pos >= mLetters.size()) {
            return;
        }
        mChoosePosition = pos;
        invalidate();
    }

    public List<String> getLetters() {
        return mLetters;
    }

    public void setLetters(List<String> mLetters) {
        this.mLetters = mLetters;
    }

    public void setOnTouchLetterChangeListener(OnTouchLetterChangeListener listener) {
        this.mListener = listener;
    }

    public interface OnTouchLetterChangeListener {
        void onLetterChange(String letter);
    }
}
