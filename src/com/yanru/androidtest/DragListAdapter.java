package com.yanru.androidtest;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.yanru.androidtest.DragListView.DragListener;
import com.yanru.androidtest.PlanSlideView.SlideListener;

/***
 * 自定义适配器
 * 
 */
public class DragListAdapter extends BaseAdapter implements DragListener,
		SlideListener {
	private List<Object> mList;
	private LayoutInflater mInflater;

	private int screenWidth;

	public DragListAdapter(Context context, List<Object> arrayTitles) {
		mInflater = LayoutInflater.from(context);
		this.mList = arrayTitles;
		screenWidth = context.getResources().getDisplayMetrics().widthPixels;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		Object obj = getItem(position);
		if (obj instanceof String) {
			return 0;
		} else {
			return 1;
		}
	}

	private class ViewHolder {
		TextView title;
		PlanSlideView slideview;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Object obj = getItem(position);
		int type = getItemViewType(position);
		// 在这里尽可能每次都进行实例化新的，这样在拖拽ListView的时候不会出现错乱.
		// 具体原因不明，不过这样经过测试，目前没有发现错乱。虽说效率不高，但是做拖拽LisView足够了。
		ViewHolder viewHolder = new ViewHolder();
		if (type == 0) {
			convertView = mInflater.inflate(R.layout.undragable_item, parent,
					false);
			viewHolder.title = (TextView) convertView.findViewById(R.id.title);
		} else {
			convertView = mInflater.inflate(R.layout.dragable_item, parent,
					false);
			viewHolder.slideview = (PlanSlideView) convertView
					.findViewById(R.id.slideview);
		}

		if (type == 0) {
			viewHolder.title.setText(obj.toString());
		} else {
			viewHolder.slideview.setText(obj.toString());
			viewHolder.slideview.setTag(obj);
			viewHolder.slideview.setSlideListener(this);
		}
		return convertView;
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public void changePosition(int startPosition, int endPosition) {
		Object from_item = mList.get(startPosition);
		mList.remove(startPosition);
		mList.add(endPosition, from_item);
		notifyDataSetChanged();
	}

	@Override
	public void afterSlide(final View view, int operate) {
		if (view == null || view.getTag() == null) {
			return;
		}

		AnimatorSet animatorSet = new AnimatorSet();
		ObjectAnimator anim = null;
		if (operate == PlanSlideView.SLIDE_LEFT) {
			anim = ObjectAnimator.ofFloat(view, "translationX",
					ViewHelper.getX(view), -screenWidth).setDuration(200);
		} else {
			anim = ObjectAnimator.ofFloat(view, "translationX",
					ViewHelper.getX(view), screenWidth).setDuration(200);
		}
		ObjectAnimator anim_alpha = ObjectAnimator.ofFloat(view, "alpha", 1, 0)
				.setDuration(200);
		animatorSet.playTogether(anim, anim_alpha);
		animatorSet.start();
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				mList.remove(view.getTag());
				notifyDataSetChanged();
			}
		};
		new Handler().postDelayed(thread, 200);

	}

}