package com.yanru.androidtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class CopyOfDragListView extends ListView {

	private WindowManager windowManager;// windows窗口控制类
	private WindowManager.LayoutParams windowParams;// 用于控制拖拽项的显示的参数

	// private int scaledTouchSlop;// 判断滑动的一个距离,scroll的时候会用到(24)

	private ImageView dragImageView;// 被拖拽的项(item)，其实就是一个ImageView
	private int startPosition;// 手指拖动项原始在列表中的位置
	private int dragPosition;// 手指点击准备拖动的时候,当前拖动项在列表中的位置
	private int lastPosition;// 拖动项最后在列表中的位置

	private boolean isSameDragDirection = true;
	private int lastFlag = -1; // -1,0 == down,1== up
	// private int holdPosition;

	// private View dragItemView = null;// 拖动时隐藏的view

	private int dragPoint;// 在当前数据项中的位置
	private int dragOffset;// 当前视图和屏幕的距离(这里只使用了y方向上)

	private int upScrollBounce;// 拖动的时候，开始向上滚动的边界
	private int downScrollBounce;// 拖动的时候，开始向下滚动的边界

	private final static int step = 1;// ListView 滑动步伐.

	private int current_Step;// 当前步伐.

//	private boolean isLock;// 是否上锁.

	// private ItemInfo mDragItemInfo;
	// private boolean isMoving = false;
	// private boolean isDragItemMoving = false;

	private int mItemVerticalSpacing = 0;

	private boolean bHasGetSapcing = false;

	private static final int ANIMATION_DURATION = 200;

	private DragListener mDropViewListener;

	public void setDropViewListener(DragListener listener) {
		this.mDropViewListener = listener;
	}

	public interface DragListener {
		void changePosition(int startPosition, int endPosition);
	}

	public CopyOfDragListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
	}

	private void getSpacing() {
		bHasGetSapcing = true;

		upScrollBounce = getHeight() / 3;// 取得向上滚动的边际，大概为该控件的1/3
		downScrollBounce = getHeight() * 2 / 3;// 取得向下滚动的边际，大概为该控件的2/3

		int[] tempLocation0 = new int[2];
		int[] tempLocation1 = new int[2];

		View itemView0 = getChildAt(0);// 第一行
		View itemView1 = getChildAt(1);// 第二行

		if (itemView0 != null) {
			itemView0.getLocationOnScreen(tempLocation0);
		} else {
			return;
		}

		if (itemView1 != null) {
			itemView1.getLocationOnScreen(tempLocation1);
			mItemVerticalSpacing = Math
					.abs(tempLocation1[1] - tempLocation0[1]);
		} else {
			return;
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// 按下
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			int x = (int) ev.getX();// 获取相对与ListView的x坐标
			int y = (int) ev.getY();// 获取相应与ListView的y坐标
			lastPosition = startPosition = dragPosition = pointToPosition(x, y);
			// 无效不进行处理
			if (startPosition == AdapterView.INVALID_POSITION) {
				return super.onInterceptTouchEvent(ev);
			}
			if (false == bHasGetSapcing) {
				getSpacing();
			}

			// 获取当前位置的视图(可见状态)
			View dragger = getChildAt(startPosition - getFirstVisiblePosition());

			// 获取到的dragPoint其实就是在你点击指定item项中的高度.
			dragPoint = y - dragger.getTop();
			// 这个值是固定的:其实就是ListView这个控件与屏幕最顶部的距离（一般为标题栏+状态栏）.
			dragOffset = (int) (ev.getRawY() - y);

			// 可拖拽的图标
			View draggerIcon = dragger.findViewById(R.id.drag_img);
		
			// x > dragger.getLeft() - 20这句话为了更好的触摸（-20可以省略）
			if (draggerIcon != null && x > draggerIcon.getLeft())
			{
				dragger.destroyDrawingCache();
				dragger.setBackgroundColor(0x55555555);
				dragger.setDrawingCacheEnabled(true);// 开启cache.
				Bitmap bm = Bitmap.createBitmap(dragger.getDrawingCache(true));// 根据cache创建一个新的bitmap对象.
				dragger.setVisibility(View.INVISIBLE);
				startDrag(bm, y);// 初始化影像
			}

			return false;
		}

		return super.onInterceptTouchEvent(ev);
	}

	/**
	 * 触摸事件处理
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// item的view不为空，且获取的dragPosition有效
		if (dragImageView != null && dragPosition != INVALID_POSITION) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
				stopDrag();
				onDrop();
				break;
			case MotionEvent.ACTION_MOVE:
				int moveY = (int) ev.getY();
				onDrag(moveY);
				testAnimation(moveY);
				break;
			case MotionEvent.ACTION_DOWN:
				break;
			default:
				break;
			}
			return true;// 取消ListView滑动.
		}
		return super.onTouchEvent(ev);
	}

	private void testAnimation(int y) {
		int tempPosition = pointToPosition(0, y);
		if (tempPosition == INVALID_POSITION || tempPosition == lastPosition) {
			return;
		}
		dragPosition = tempPosition;

		int holdPosition;
		int MoveNum = tempPosition - lastPosition;
		int count = Math.abs(MoveNum);
		for (int i = 1; i <= count; i++) {
			int yAbsOffset;
			// 向下drag
			if (MoveNum > 0) {
				if (lastFlag == -1) {
					lastFlag = 0;
					isSameDragDirection = true;
				}
				if (lastFlag == 1) {
					// // turnUpPosition = tempPosition;
					lastFlag = 0;
					isSameDragDirection = !isSameDragDirection;
				}
				if (isSameDragDirection) {
					holdPosition = lastPosition + 1;
				} else {
					if (startPosition < tempPosition) {
						holdPosition = lastPosition + 1;
						isSameDragDirection = !isSameDragDirection;
					} else {
						holdPosition = lastPosition;
					}
				}
				yAbsOffset = -mItemVerticalSpacing;
				lastPosition++;
			}
			// 向上drag
			else {
				if (lastFlag == -1) {
					lastFlag = 1;
					isSameDragDirection = true;
				}
				if (lastFlag == 0) {
					// turnDownPosition = tempPosition;
					lastFlag = 1;
					isSameDragDirection = !isSameDragDirection;
				}
				if (isSameDragDirection) {
					holdPosition = lastPosition - 1;
				} else {
					if (startPosition > tempPosition) {
						holdPosition = lastPosition - 1;
						isSameDragDirection = !isSameDragDirection;
					} else {
						holdPosition = lastPosition;
					}
				}
				yAbsOffset = mItemVerticalSpacing;
				lastPosition--;
			}
			View moveView = getChildAt(holdPosition - getFirstVisiblePosition());
			Animation animation;
			if (isSameDragDirection) {
				animation = getFromSelfAnimation(yAbsOffset);
			} else {
				animation = getToSelfAnimation(-yAbsOffset);
			}
			moveView.startAnimation(animation);
		}
	}

	/**
	 * 准备拖动，初始化拖动项的图像
	 * 
	 * @param bm
	 * @param y
	 */
	private void startDrag(Bitmap bm, int y) {
		stopDrag();
		/***
		 * 初始化window.
		 */
		windowParams = new WindowManager.LayoutParams();
		windowParams.gravity = Gravity.TOP;
		windowParams.x = 0;
		windowParams.y = y - dragPoint + dragOffset;
		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE// 不需获取焦点
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE// 不需接受触摸事件
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON// 保持设备常开，并保持亮度不变。
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;// 窗口占满整个屏幕，忽略周围的装饰边框（例如状态栏）。此窗口需考虑到装饰边框的内容。

		windowParams.windowAnimations = 0;// 窗口所使用的动画设置

		// windowParams.alpha = 0.8f;
		windowParams.format = PixelFormat.TRANSLUCENT;// 默认为不透明，这里设成透明效果

		ImageView imageView = new ImageView(getContext());
		imageView.setImageBitmap(bm);

		windowManager.addView(imageView, windowParams);
		dragImageView = imageView;
	}

	/**
	 * 拖动执行，在Move方法中执行
	 * 
	 * @param y
	 */
	public void onDrag(int y) {
		int drag_top = y - dragPoint;// 拖拽view的top值不能＜0，否则则出界.
		if (dragImageView != null && drag_top >= 0) {
			windowParams.alpha = 1.0f;
			windowParams.y = y - dragPoint + dragOffset;
			windowManager.updateViewLayout(dragImageView, windowParams);// 时时移动.
		}
		// // 为了避免滑动到分割线的时候，返回-1的问题
		// int tempPosition = pointToPosition(0, y);
		// if (tempPosition != INVALID_POSITION) {
		// dragPosition = tempPosition;
		// }
		//
		// onChange(y);// 时时交换

		doScroller(y);// listview移动.

	}

	/***
	 * ListView的移动.
	 * 要明白移动原理：当我移动到下端的时候，ListView向上滑动，当我移动到上端的时候，ListView要向下滑动。正好和实际的相反.
	 * 
	 */
	private boolean isScroll = false;

	public void doScroller(int y) {
		// ListView需要下滑
		if (y < upScrollBounce) {
			current_Step = step + (upScrollBounce - y) / 10;// 时时步伐
		}// ListView需要上滑
		else if (y > downScrollBounce) {
			current_Step = -(step + (y - downScrollBounce)) / 10;// 时时步伐
		} else {
			isScroll = false;
			current_Step = 0;
		}

		// 获取你拖拽滑动到位置及显示item相应的view上（注：可显示部分）（position）
		View view = getChildAt(dragPosition - getFirstVisiblePosition());
		// 真正滚动的方法setSelectionFromTop()
		setSelectionFromTop(dragPosition, view.getTop() + current_Step);

	}

	/**
	 * 停止拖动，删除影像
	 */
	public void stopDrag() {
		// isMoving = false;
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}
		isSameDragDirection = true;
		lastFlag = -1; // -1,0 == down,1== up
	}

	/**
	 * 拖动放下的时候
	 * 
	 * @param y
	 */
	public void onDrop() {
		if (mDropViewListener != null) {
			mDropViewListener.changePosition(startPosition, dragPosition);
		}
	}

	public Animation getFromSelfAnimation(int y) {
		TranslateAnimation go = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, y);
		go.setInterpolator(new AccelerateDecelerateInterpolator());
		go.setFillAfter(true);
		go.setDuration(ANIMATION_DURATION);
		// go.setInterpolator(new AccelerateInterpolator());
		return go;
	}

	public Animation getToSelfAnimation(int y) {
		TranslateAnimation go = new TranslateAnimation(Animation.ABSOLUTE, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, y,
				Animation.RELATIVE_TO_SELF, 0);
		go.setInterpolator(new AccelerateDecelerateInterpolator());
		go.setFillAfter(true);
		go.setDuration(ANIMATION_DURATION);
		// go.setInterpolator(new AccelerateInterpolator());
		return go;
	}

}