package com.sanron.hwrushhelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

/**
 * @author chenrong
 * @date 2019/11/22
 */
public class WebView1 extends BaseWebView {

    public static WebView1 sWebView1;


    public static final String JS = " (function() {\n" +
            "      var c = 0;\n" +
            "      function xc() {\n" +
            "        app.log('尝试获取基础参数'+(++c));\n" +
            "        if (c>= 100) {\n" +
            "          native.result('error');\n" +
            "          return;\n" +
            "        }\n" +
            "        if (typeof ec != \"undefined\" &&\n" +
            "          ec.util &&\n" +
            "          ec.util.cookie &&\n" +
            "          ec.util.cookie.set) {\n" +
            "          var oset = ec.util.cookie.set;\n" +
            "          ec.util.cookie.set = function(a, b, c) {\n" +
            "            oset(a, b, c);\n" +
            "            if (a.indexOf('queueSign') >= 0) {\n" +
            "              var x = {};\n" +
            "              x.activityId = ec.activityId + '';\n" +
            "              x.cookie = document.cookie;\n" +
            "              var nn = JSON.stringify(x);\n" +
            "              native.result(nn);\n" +
            "            }\n" +
            "          };\n" +
            "          native.setOk();\n" +
            "          app.log('hook cookie set成功');\n" +
            "        } else {\n" +
            "          setTimeout(function() {\n" +
            "            xc();\n" +
            "          }, 50);\n" +
            "        }\n" +
            "      }\n" +
            "      xc();\n" +
            "    })();";


    public ValueCallback<String> mCallback;

    private boolean setOk;

    @SuppressLint("JavascriptInterface")
    public WebView1(Context context) {
        super(BaseWebView.getFixedContext(context));
        if(sWebView1!=null){
            sWebView1.onPause();
            sWebView1.destroy();
        }
        sWebView1 = this;

        getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
        addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void result(String value) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mCallback != null) {
                        mCallback.onReceiveValue(value);
                    }
                });
            }

            @JavascriptInterface
            public void setOk() {
                setOk = true;
            }
        }, "native");
        setWebChromeClient(new WebChromeClient() {
            boolean x = false;

            @Override
            public void onProgressChanged(WebView webView, int i) {
                super.onProgressChanged(webView, i);
                if (i > 20 && !x) {
                    x = true;
                    webView.evaluateJavascript(JS, null);
                }
            }
        });
        setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                if (webResourceRequest.getUrl().getLastPathSegment() != null) {
                    if (webResourceRequest.getUrl().getLastPathSegment().contains("isqueue.json")) {
                        int count = 0;
                        while (!setOk && count++ <100) {
                            SystemClock.sleep(100);
                        }
                        return super.shouldInterceptRequest(webView, webResourceRequest);
                    } else if (webResourceRequest.getUrl().getLastPathSegment().contains("createOrder")) {
                        WebResourceResponse resourceResponse = new WebResourceResponse();
                        resourceResponse.setStatusCodeAndReasonPhrase(400, "444");
                        return resourceResponse;
                    }
                }
                return super.shouldInterceptRequest(webView, webResourceRequest);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mCallback != null) {
                        mCallback.onReceiveValue("error");
                    }
                });
            }
        });
    }


    public void getActId(String productRushUrl, ValueCallback<String> callback) {
        mCallback = callback;
        loadUrl(productRushUrl);
    }
}
