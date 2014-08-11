package com.yanru.androidtest;

import android.content.Context;
import android.renderscript.ScriptIntrinsicLUT;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class PlanSlideView extends RelativeLayout implements OnTouchListener {
	public static final int SLIDE_LEFT = 1;
	public static final int SLIDE_RIGHT = 2;

	private int screenWidth;
	private float downX; // 按下时位置
	private float downY;

	private View mLayout;
	private TextView mTextView;
	private ImageView mDragView;

	private SlideListener mSlideListener;

	public interface SlideListener {
		void afterSlide(View view, int operate);
	}

	public void setSlideListener(SlideListener slideListener) {
		this.mSlideListener = slideListener;
	}

	public PlanSlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	private void initView(Context context) {
		mLayout = LayoutInflater.from(context).inflate(
				R.layout.dragable_slide_item, this, true);
		mTextView = (TextView) mLayout.findViewById(R.id.drag_text);
		mDragView = (ImageView) mLayout.findViewById(R.id.drag_img);
		mTextView.setOnTouchListener(this);
		mTextView.setLongClickable(true);
		screenWidth = context.getResources().getDisplayMetrics().widthPixels;
	}

	public void setText(String text) {
		mTextView.setText(text);
	}

	public ImageView getDragView() {
		return mDragView;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_MOVE:
			sliding(event);
			break;
		case MotionEvent.ACTION_DOWN:
			downX = event.getRawX();
			downY = event.getRawY();
			break;
		default:
			slideDone();
			break;
		}
		return true;
	}
	
	private void sliding(MotionEvent event) {
		float moveX = event.getRawX() - downX;
		float moveY = event.getRawY() - downY;
		ViewHelper.setX(mLayout, moveX);
	}

	private void slideDone() {
		float layout_x = ViewHelper.getX(mLayout);
		int flag = 0;
		if (layout_x > screenWidth / 2) {
			flag = SLIDE_RIGHT;
		} else if (layout_x < -screenWidth / 2) {
			flag = SLIDE_LEFT;
		}

		if (flag == 0) {
			setAnim(layout_x);
		} else {
			if (mSlideListener != null) {
				mSlideListener.afterSlide(this, flag);
			} else {
				setAnim(layout_x);
			}
		}
	}

	private void setAnim(float layout_x) {
		AnimatorSet animatorSet = new AnimatorSet();
		ObjectAnimator anim = ObjectAnimator.ofFloat(mLayout, "translationX",
				layout_x, 0).setDuration(200);
		animatorSet.play(anim);
		animatorSet.start();
	}

}
