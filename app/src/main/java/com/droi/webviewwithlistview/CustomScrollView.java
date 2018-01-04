package com.droi.webviewwithlistview;

import android.content.Context;
import android.hardware.SensorManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.OverScroller;

/**
 * Created by jerryctt on 2017/12/28.
 */

public class CustomScrollView extends NestedScrollView {
    private final static String TAG = CustomScrollView.class.getSimpleName();
    private OverScroller mScroller;
    public CustomScrollView(Context context) {
        super(context);
        mScroller = new OverScroller( context );
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new OverScroller( context );
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new OverScroller( context );
    }


    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
//        Log.d( TAG, "onNestedPreScroll. dy is " + dy );
        scrollBy( dx, dy );
        if ( consumed != null ) {
            consumed[0] = dx;
            consumed[1] = dy;
        }
    }

    private View getWebview() {
        for ( int i=0; i<getChildCount(); i++ ) {
            View child = getChildAt(0);
            if ( child instanceof ViewGroup ) {
                for ( int j=0; j<((ViewGroup) child).getChildCount(); j++ ) {
                    View cchild = ((ViewGroup) child).getChildAt(j);
                    if ( cchild instanceof CustomWebview ) {
                        return cchild;
                    }
                }
            }
        }

        return null;
    }

    private int getWebviewRange() {
        CustomWebview wv = (CustomWebview) getWebview();
        if ( wv == null )
            return 0;

        return wv.getScrollRange();
    }

    private int getWebviewPosition() {
        View webView = getWebview();
        if ( webView != null )
            return webView.getTop();
        return 0;
    }

    private int getWebviewScrollY() {
        View webView = getWebview();
        if ( webView != null )
            return webView.getScrollY();
        return 0;
    }

    //==========================================
    // Fling Helper...
    public void abortFlingAnimation() {
        mScroller.abortAnimation();
    }

    public int getVirtualScrollRange() {
        // getChild(0).getHeight() - xxxxxxxx
        return getHeight() - getPaddingBottom() - getPaddingTop() + getWebviewRange();
    }

    public int getVirtualScrollY() {
        if ( getScrollY() == getWebviewPosition() ) {
            return getScrollY() + getWebviewScrollY();
        } else if ( getScrollY() > getWebviewPosition() ) {
            return getScrollY() + getWebviewRange();
        } else
            return getScrollY();
    }

    private int mPrevVirtualScrollY = 0;

    @Override
    public void computeScroll() {
        super.computeScroll();

        if ( mScroller.computeScrollOffset() ) {
            int currY = mScroller.getCurrY();
            if ( currY != mPrevVirtualScrollY ) {
                mPrevVirtualScrollY = currY;
                if (currY < getWebviewPosition()) {
                    scrollTo(0, currY);
                    getWebview().scrollTo(0, 0);
                } else if (currY > getWebviewPosition()) {
                    if (currY < (getWebviewPosition() + getWebviewRange())) {
                        scrollTo(0, getWebviewPosition());
                        getWebview().scrollTo(0, currY - getWebviewPosition());
                    } else {
                        scrollTo(0, currY - getWebviewRange());
                        getWebview().scrollTo(0, getWebviewRange());
                    }
                }
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }

    }

    @Override
    public void fling(int velocityY) {
        mScroller.abortAnimation();
        mPrevVirtualScrollY = getVirtualScrollY();
        mScroller.fling( 0, getVirtualScrollY(), 0, velocityY, 0, 0, 0, getVirtualScrollRange() );

        // Update UI
        ViewCompat.postInvalidateOnAnimation(this);
    }
}
