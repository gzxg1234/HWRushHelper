package com.sanron.hwrushhelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

/**
 * @author chenrong
 * @date 2019/11/22
 */
public class WebView1 extends BaseWebView {


    public ValueCallback<String> mCallback;

    @SuppressLint("JavascriptInterface")
    public WebView1(Context context) {
        super(context);

        getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

        setWebViewClient(new WebViewClient() {

            void ss(WebView view, int i) {
                view.evaluateJavascript("(function() {\n" +
                        "    if(typeof(ec)!=\"undefined\"){\n" +
                        "        if (ec && ec.activityId && ec.rushbuy_js_version) {\n" +
                        "            var x = {};\n" +
                        "            x.activityId = ec.activityId+'';\n" +
                        "            x.rushJsVer = ec.rushbuy_js_version;\n" +
                        "            return x;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    return 'no';\n" +
                        "})()", value -> {
                    if ("\"no\"".equals(value) && i > 0) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            ss(view, i - 1);
                        }, 100);
                        return;
                    }
                    if (mCallback != null) {
                        mCallback.onReceiveValue(value);
                    }
                });
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ss(view,10);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (mCallback != null) {
                    mCallback.onReceiveValue("error");
                }
            }
        });
    }


    public void getActId(String productRushUrl, ValueCallback<String> callback) {
        mCallback = callback;
        loadUrl(productRushUrl);
    }
}
