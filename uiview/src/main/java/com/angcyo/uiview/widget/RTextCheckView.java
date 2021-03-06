package com.angcyo.uiview.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

/**
 * 实现了 checked 状态的 TextView
 * Created by angcyo on 2017-01-01 10:46.
 */
public class RTextCheckView extends AppCompatTextView implements View.OnClickListener, RCheckGroup.ICheckView {

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };
    boolean mChecked = false;
    OnClickListener mOnClickListener;
    OnCheckedChangeListener mOnCheckedChangeListener;

    public RTextCheckView(Context context) {
        this(context, null);
    }

    public RTextCheckView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        super.setOnClickListener(this);
        setGravity(Gravity.CENTER);

        if (getTag() == null) {
            float density = getResources().getDisplayMetrics().density;
            int paddStart = (int) (density * 20);
            int paddTop = (int) (density * 10);
            setPadding(paddStart, paddTop, paddStart, paddTop);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked == checked) {
            return;
        }

        mChecked = checked;

        refreshDrawableState();

        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
        }
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
        if (mOnClickListener == null) {
            super.setOnClickListener(null);
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        mOnCheckedChangeListener = onCheckedChangeListener;
    }

    @Override
    public void onClick(View v) {
        setChecked(!isChecked());
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v);
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(RTextCheckView textCheckView, boolean isChecked);
    }
}