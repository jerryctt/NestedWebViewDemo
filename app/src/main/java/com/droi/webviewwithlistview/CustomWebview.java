
package com.droi.webviewwithlistview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.ScrollView;


public class CustomWebview extends WebView implements NestedScrollingChild2 {

    public static final String TAG = CustomWebview.class.getSimpleName();

    private float mLastMotionY;
    private int mNestedYOffset;

    private int mTouchSlop;
    private boolean mIsBeingDragged = false;

    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private NestedScrollingChildHelper mChildHelper;

    public CustomWebview(Context context) {
        super(context);
        init();
    }

    public CustomWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomWebview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mChildHelper = new NestedScrollingChildHelper(this);
        mVelocityTracker = VelocityTracker.obtain();
        setNestedScrollingEnabled(true);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
   }

    private View findParentScrollView() {
        ViewParent parent = getParent();
        while( parent != null ) {
            if ( parent instanceof ScrollView || parent instanceof NestedScrollView )
                break;

            parent = parent.getParent();
        }

        return (View) parent;
    }

    private int getParentScrollY() {
        View parent = findParentScrollView();
        if ( parent == null )
            return 0;

        if ( parent instanceof  ScrollView ) {
            return ((ScrollView) parent).getScrollY();
        } else if ( parent instanceof NestedScrollView ) {
            return ((NestedScrollView) parent).getScrollY();
        }

        return 0;
    }

    public int getScrollRange() {
        int maxContentY = computeVerticalScrollRange() - (getHeight()- getPaddingBottom() - getPaddingTop());
        return maxContentY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean result = false;

        MotionEvent event = MotionEvent.obtain(e);
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        event.offsetLocation(0, mNestedYOffset);

        int y = (int) event.getY();
//        Log.d( TAG, "The Y is " + y );
        int deltaY = (int) (mLastMotionY - y + 0.5f);

        switch( action ) {
            case MotionEvent.ACTION_DOWN:

                mLastMotionY = y;

                initOrResetVelocityTracker();
                getParentScrollView().abortFlingAnimation();

                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                mIsBeingDragged = false;
                result = super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
//                Log.d(  TAG, "total deltaY is " + deltaY );

                if ( Math.abs(deltaY) > mTouchSlop && !mIsBeingDragged ) {
                    mIsBeingDragged = true;
                    MotionEvent ev = MotionEvent.obtain(event);
                    ev.setAction( MotionEvent.ACTION_CANCEL );
                    super.onTouchEvent( ev );
                    ev.recycle();
                }

                if ( mIsBeingDragged == false ) {
                    result = super.onTouchEvent(event);
                    break;
                }

                result = true;
                int nPreOffset = ( deltaY < 0 )?Math.min( 0, Math.max( getTop()-getParentScrollY(), deltaY)):
                        (Math.max( 0, Math.min( getTop() - getParentScrollY(), deltaY )));

                if ( nPreOffset != 0 ) {
                    if (dispatchNestedPreScroll(0, nPreOffset, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1];
                        nPreOffset = mScrollConsumed[1];
                        event.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                    }
                }

                // Process webview
                int nChildOffset = ( deltaY < 0 )?Math.max( -getScrollY(), deltaY ):Math.min( getScrollRange() - getScrollY(), deltaY );
                if ( nChildOffset != 0 )
                    scrollTo( getScrollX(), getScrollY()+nChildOffset);
                deltaY -= nChildOffset;

                if ( deltaY != 0 ) {
                    dispatchNestedScroll(0, nChildOffset + nPreOffset, 0, deltaY, mScrollOffset);
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_UP:

                if ( mIsBeingDragged == false ) {
                    result = super.onTouchEvent(event);
                } else {
                    // Cancel this event
                    MotionEvent ev = MotionEvent.obtain(event);
                    ev.setAction( MotionEvent.ACTION_CANCEL );
                    result = super.onTouchEvent( ev );
                    ev.recycle();

                    // Make parent fling
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) mVelocityTracker.getYVelocity();
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        getParentScrollView().fling( -initialVelocity );
                    }
                    recycleVelocityTracker();
                }
                stopNestedScroll();
                break;
            case MotionEvent.ACTION_CANCEL:
                stopNestedScroll();
                result = super.onTouchEvent(event);
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }

        event.recycle();
        return result;
    }

    private CustomScrollView getParentScrollView() {
        ViewParent parent = getParent();
        while( parent != null ) {
            if ( parent instanceof  CustomScrollView)
                return (CustomScrollView) parent;

            parent = parent.getParent();
        }
        return null;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        Log.d( TAG, "hasNestedScrollingParent");
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll( axes, type );
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll( type );
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll( dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type );
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

}