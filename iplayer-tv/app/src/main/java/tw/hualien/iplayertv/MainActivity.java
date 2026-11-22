package tw.hualien.iplayertv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Android TV launcher for the official BBC iPlayer website.
 *
 * Remote control:
 *   D-pad        move the on-screen pointer (page scrolls at the edges)
 *   OK / Enter   click under the pointer
 *   Back         leave fullscreen / go back / exit
 *   Menu         switch between pointer mode and normal key navigation
 *   Play/Pause   toggle the video that is playing
 */
public class MainActivity extends Activity {

    private static final String HOME_URL = "https://www.bbc.co.uk/iplayer";

    // A desktop Chrome user agent so the BBC serves the full website
    // (and its full-HD quality ladder) instead of redirecting to the app store.
    private static final String DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    // Only BBC domains are opened inside the app.
    private static final String[] ALLOWED_HOSTS = {
            "bbc.co.uk", "bbc.com", "bbci.co.uk"
    };

    private WebView webView;
    private CursorView cursor;
    private FrameLayout root;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean cursorMode = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(Color.BLACK);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(DESKTOP_UA);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true); // needed for BBC sign-in

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return blockIfNotBbc(req.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return blockIfNotBbc(Uri.parse(url));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Allow protected (DRM) playback for BBC pages only.
                if (isBbc(request.getOrigin())) {
                    for (String r : request.getResources()) {
                        if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) {
                            request.grant(new String[]{r});
                            return;
                        }
                    }
                }
                request.deny();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { callback.onCustomViewHidden(); return; }
                customView = view;
                customViewCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                cursor.bringToFront();
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() { exitFullscreen(); }
        });

        cursor = new CursorView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(cursor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        hideSystemUi();

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl(HOME_URL);
    }

    // ---------- Domain filter ----------

    private static boolean isBbc(Uri uri) {
        if (uri == null || uri.getHost() == null) return false;
        if (!"https".equals(uri.getScheme())) return false;
        String host = uri.getHost().toLowerCase();
        for (String h : ALLOWED_HOSTS) {
            if (host.equals(h) || host.endsWith("." + h)) return true;
        }
        return false;
    }

    private boolean blockIfNotBbc(Uri uri) {
        if (isBbc(uri)) return false;
        Toast.makeText(this, "Only BBC pages open in this app", Toast.LENGTH_SHORT).show();
        return true;
    }

    // ---------- Remote control ----------

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;

        switch (code) {
            case KeyEvent.KEYCODE_BACK:
                if (down) return true;
                if (customView != null) exitFullscreen();
                else if (webView.canGoBack()) webView.goBack();
                else finish();
                return true;

            case KeyEvent.KEYCODE_MENU:
                if (!down) {
                    cursorMode = !cursorMode;
                    cursor.setVisibility(cursorMode ? View.VISIBLE : View.GONE);
                    Toast.makeText(this, cursorMode ? "Pointer mode" : "Key navigation mode",
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (down) {
                    webView.evaluateJavascript(
                            "(function(){var v=document.querySelector('video');"
                                    + "if(v){v.paused?v.play():v.pause();}})()", null);
                }
                return true;
        }

        if (!cursorMode) return super.dispatchKeyEvent(e);

        // Speed up when the key is held down.
        int step = Math.min(8 + e.getRepeatCount() * 4, 60);
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:    if (down) moveCursor(0, -step); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:  if (down) moveCursor(0, step);  return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:  if (down) moveCursor(-step, 0); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: if (down) moveCursor(step, 0);  return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (!down && e.getRepeatCount() == 0) tap(cursor.x, cursor.y);
                return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private void moveCursor(int dx, int dy) {
        int w = root.getWidth(), h = root.getHeight();
        float nx = cursor.x + dx, ny = cursor.y + dy;
        int edge = 30;
        // Scroll the page when the pointer pushes against an edge.
        if (customView == null) {
            if (ny < edge && dy < 0) webView.scrollBy(0, dy * 3);
            if (ny > h - edge && dy > 0) webView.scrollBy(0, dy * 3);
            if (nx < edge && dx < 0) webView.scrollBy(dx * 3, 0);
            if (nx > w - edge && dx > 0) webView.scrollBy(dx * 3, 0);
        }
        cursor.moveTo(Math.max(0, Math.min(w - 1, nx)), Math.max(0, Math.min(h - 1, ny)));
    }

    private void tap(float x, float y) {
        View target = customView != null ? customView : webView;
        long t = SystemClock.uptimeMillis();
        MotionEvent downEv = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent upEv = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0);
        downEv.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        upEv.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        target.dispatchTouchEvent(downEv);
        target.dispatchTouchEvent(upEv);
        downEv.recycle();
        upEv.recycle();
    }

    // ---------- Fullscreen / lifecycle ----------

    private void exitFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customViewCallback = null;
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        webView.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        CookieManager.getInstance().flush(); // keep the BBC sign-in
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        root.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }

    // ---------- On-screen pointer ----------

    private static class CursorView extends View {
        float x = -1, y = -1;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

        CursorView(Context c) {
            super(c);
            fill.setColor(Color.argb(170, 255, 255, 255));
            ring.setColor(Color.argb(220, 0, 0, 0));
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(4f);
            setFocusable(false);
        }

        void moveTo(float nx, float ny) { x = nx; y = ny; invalidate(); }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            if (x < 0) { x = w / 2f; y = h / 2f; }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(x, y, 14f, fill);
            canvas.drawCircle(x, y, 14f, ring);
        }
    }
}
