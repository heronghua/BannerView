package com.foxconn.bannerview;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;

public class BannerView1 extends FrameLayout {

	private static final String TAG = "BannerView";
	private static final boolean LOGD = false;

	private static final int DEFAULT_INTERVAL = 3000;

	private int mFlipInterval = DEFAULT_INTERVAL;
	private boolean mAutoStart = false;

	private boolean mRunning = false;
	private boolean mStarted = false;
	private boolean mVisible = false;
	private boolean mUserPresent = true;

	//
	private List<String> urls;
	private LinearLayout dotsContainer;
	private ViewAnimator va;

	private Paint mPaint = new Paint();

	private final Bitmap grayDotBmp = Bitmap.createBitmap(40, 40, Config.ARGB_8888);

	private final Bitmap redDotBmp = Bitmap.createBitmap(40, 40, Config.ARGB_8888);

	public BannerView1(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		Canvas c = new Canvas(grayDotBmp);
		mPaint.setColor(Color.DKGRAY);
		c.drawOval(new RectF(0f, 0f, 40f, 40f), mPaint);

		Canvas c1 = new Canvas(redDotBmp);
		mPaint.setColor(Color.RED);
		c1.drawOval(new RectF(0f, 0f, 40f, 40f), mPaint);

	}

	public BannerView1(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BannerView);
		mFlipInterval = a.getInt(R.styleable.BannerView_flipInterval, DEFAULT_INTERVAL);
		mAutoStart = a.getBoolean(R.styleable.BannerView_autoStart, false);
		a.recycle();

		init(context);
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				mUserPresent = false;
				updateRunning();
			} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
				mUserPresent = true;
				updateRunning(false);
			}
		}
	};

	public void setImageUrls(List<String> urls) {
		this.urls = urls;

		this.dotsContainer = new LinearLayout(getContext());
		this.dotsContainer.setOrientation(LinearLayout.HORIZONTAL);
		for (int i = 0; i < urls.size(); i++) {
			ImageView view = new ImageView(getContext());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(40, 40);
			lp.leftMargin = 10;
			lp.rightMargin = 10;
			view.setLayoutParams(lp);
			this.dotsContainer.addView(view);

		}

		this.va = new ViewAnimator(getContext());
		for (int i = 0; i < urls.size(); i++) {
			SmartImageView s = new SmartImageView(getContext());
			s.setRatio(2);
			if (i % 2 == 0) {
				s.setImageResource(R.drawable.f);
			} else {
				s.setImageResource(R.drawable.g);
			}
			this.va.addView(s);
		}
		this.addView(va);
		this.addView(dotsContainer);

	}

	private void resetDotSrc(int pos) {
		for (int i = 0; i < dotsContainer.getChildCount(); i++) {
			ImageView child = (ImageView) dotsContainer.getChildAt(i);
			if (i == pos) {
				child.setImageBitmap(redDotBmp);
			} else {
				child.setImageBitmap(grayDotBmp);
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		// Listen for broadcasts related to user-presence
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);

		// OK, this is gross but needed. This class is supported by the
		// remote views machanism and as a part of that the remote views
		// can be inflated by a context for another user without the app
		// having interact users permission - just for loading resources.
		// For exmaple, when adding widgets from a user profile to the
		// home screen. Therefore, we register the receiver as the current
		// user not the one the context is for.
		// getContext().registerReceiverAsUser(mReceiver,
		// android.os.Process.myUserHandle(),
		// filter, null, mHandler);

		getContext().registerReceiver(mReceiver, filter, null, mHandler);
		if (mAutoStart) {
			// Automatically start when requested
			startFlipping();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mVisible = false;

		getContext().unregisterReceiver(mReceiver);
		updateRunning();
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		mVisible = visibility == VISIBLE;
		updateRunning(false);
	}

	/**
	 * How long to wait before flipping to the next view
	 *
	 * @param milliseconds
	 *            time in milliseconds
	 */
	public void setFlipInterval(int milliseconds) {
		mFlipInterval = milliseconds;
	}

	/**
	 * Start a timer to cycle through child views
	 */
	public void startFlipping() {
		mStarted = true;
		updateRunning();
	}

	/**
	 * No more flips
	 */
	public void stopFlipping() {
		mStarted = false;
		updateRunning();
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return ViewFlipper.class.getName();
	}

	/**
	 * Internal method to start or stop dispatching flip {@link Message} based
	 * on {@link #mRunning} and {@link #mVisible} state.
	 */
	private void updateRunning() {
		updateRunning(true);
	}

	/**
	 * Internal method to start or stop dispatching flip {@link Message} based
	 * on {@link #mRunning} and {@link #mVisible} state.
	 *
	 * @param flipNow
	 *            Determines whether or not to execute the animation now, in
	 *            addition to queuing future flips. If omitted, defaults to
	 *            true.
	 */
	private void updateRunning(boolean flipNow) {
		boolean running = mVisible && mStarted && mUserPresent;
		if (running != mRunning) {
			if (running) {
				// showOnly(mWhichChild, flipNow);
				Message msg = mHandler.obtainMessage(FLIP_MSG);
				mHandler.sendMessageDelayed(msg, mFlipInterval);
			} else {
				mHandler.removeMessages(FLIP_MSG);
			}
			mRunning = running;
		}
		if (LOGD) {
			Log.d(TAG, "updateRunning() mVisible=" + mVisible + ", mStarted=" + mStarted + ", mUserPresent="
					+ mUserPresent + ", mRunning=" + mRunning);
		}
	}

	/**
	 * Returns true if the child views are flipping.
	 */
	public boolean isFlipping() {
		return mStarted;
	}

	/**
	 * Set if this view automatically calls {@link #startFlipping()} when it
	 * becomes attached to a window.
	 */
	public void setAutoStart(boolean autoStart) {
		mAutoStart = autoStart;
	}

	/**
	 * Returns true if this view automatically calls {@link #startFlipping()}
	 * when it becomes attached to a window.
	 */
	public boolean isAutoStart() {
		return mAutoStart;
	}

	private final int FLIP_MSG = 1;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == FLIP_MSG) {
				if (mRunning) {
					va.showNext();
					resetDotSrc(va.getDisplayedChild());
					msg = obtainMessage(FLIP_MSG);
					sendMessageDelayed(msg, mFlipInterval);
				}
			}
		}
	};

}
