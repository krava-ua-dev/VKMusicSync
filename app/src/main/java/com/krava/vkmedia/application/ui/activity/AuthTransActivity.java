package com.krava.vkmedia.application.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.vk.sdk.VKSdk;

/**
 * Created by krava2008 on 27.05.16.
 */

public class AuthTransActivity extends Activity {

    private FrameLayout wrapper;
    private WebView webView;
    private WebChromeClient chromeClient;
    private String pass;
    private String login;
    private static boolean isError = false;

    private final String jsSetValuesForInputs = "javascript:(function() {" +
            "    setText('pass', '%s');" +
            "    setText('email', '%s');" +
            "    function setText(name, value){" +
            "        document.getElementsByName(name)[0].value = value;" +
            "    }" +
            "})()";
    private final String jsClickOnButton = "javascript:(function() {" +
            "    var elems = document.getElementsByClassName(\"button\");" +
            "    for(i=0; i < elems.length; i++){" +
            "      var element = elems[i];" +
            "      if(element.tagName.toLowerCase() == \"input\"){" +
            "          element.click();" +
            "          break;" +
            "      }" +
            "    }" +
            "})()";
    private final static String authUrl = "https://oauth.vk.com/authorize?" +
            "client_id=4935807&" +
            "display=page&" +
            "redirect_uri=https://oauth.vk.com/blank.html&" +
            "scope=friends,offline,audio,photos&response_type=token&v=5.52";
    private String lastUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isError = false;
        Bundle bundle = getIntent().getExtras();
        pass = bundle.getString("pass");
        login = bundle.getString("login");

        this.wrapper = new FrameLayout(this);
        this.webView = new WebView(this);
        this.wrapper.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(this.wrapper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        this.webView.setVisibility(View.INVISIBLE);
        this.wrapper.setVisibility(View.INVISIBLE);
        this.chromeClient = new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
        };
        clearCookies(this);
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        this.webView.addJavascriptInterface(new OnErrorListener(this), "checkError");
        this.webView.setWebChromeClient(this.chromeClient);
        this.webView.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(!isError) {
                    if(url.contains("state=&nohttps=1&display=page&__q_hash=")){
                        view.loadUrl(jsClickOnButton);
                    }else if (url.startsWith("https://oauth.vk.com/blank.html")) {
                        if (VKSdk.onCustomLogin(AuthTransActivity.this, url.split("#")[1])) {
                            onSuccess();
                        } else {
                            onError();
                        }
                    } else {
                        view.loadUrl("javascript:window.checkError.processHTML(document.getElementsByClassName('service_msg_warning').length > 0);");
                        doNext(view);
                    }
                }
            }
        });

        doNext(this.webView);
    }

    private void doNext(final WebView view){
        if(lastUrl.equals("")){
            lastUrl = authUrl;
            view.loadUrl(authUrl);
        }else if(lastUrl.equals(authUrl)){
            view.loadUrl(String.format(jsSetValuesForInputs, pass, login));
            view.loadUrl(jsClickOnButton);
        }
    }

    @SuppressWarnings("deprecation")
    private static void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr= CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager= CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private void onError(){
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void onConnectionFailed(){
        setResult(Activity.RESULT_FIRST_USER);
        finish();
    }

    public void onSuccess(){
        setResult(Activity.RESULT_OK);
        finish();
    }

    private class OnErrorListener {
        private Context ctx;

        OnErrorListener(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void processHTML(boolean isError) {
            if(isError && !AuthTransActivity.isError){
                AuthTransActivity.isError = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onError();
                    }
                });
            }
        }
    }
}
