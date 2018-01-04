package com.droi.webviewwithlistview;

import android.graphics.Point;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView( R.id.webView )
    CustomWebview webview;

    @BindView( R.id.outterLayout )
    CustomScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        ButterKnife.bind( this );

        // Set the height of webview to screen height
        scrollView.setNestedScrollingEnabled(true);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.setVerticalScrollBarEnabled(true);
        webview.loadUrl("https://developer.android.com/index.html");
//        webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                size.y));
        webview.setNestedScrollingEnabled(true);

    }
}
