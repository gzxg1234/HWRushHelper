package com.sanron.hwrushhelper;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.sanron.hwrushhelper.databinding.ActivityMainBinding;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    //获取抢购页面的跳转地址
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        initUrl();
    }

    private void initUrl() {
        binding.webview.loadUrl("https://www.vmall.com/product/10086374426533.html");
    }

    private void initView() {
        binding.etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    binding.webview.loadUrl(binding.etUrl.getText().toString());
                    return false;
                }
                return false;
            }
        });
        binding.btnRefresh.setOnClickListener(v -> {
            binding.webview.reload();
        });

        binding.btnReadyRush.setOnClickListener(v -> {
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("等待。。。太久没反应关了");
            pd.show();
            binding.webview.evaluateJavascript(RushUtil.GET_RUSH_JS, value -> {
                try {
                    JSONObject obj = new JSONObject(value);
                    JSONObject params = obj.optJSONObject("createOrderParams");
                    String rushUrl = Html.fromHtml(obj.optString("rushUrl")).toString();
                    Log.d("sanron", "排队页面url=" + rushUrl);

                    WebView1 s = new WebView1(MainActivity.this);
                    s.getActId(rushUrl, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if ("error".equals(value)) {
                                pd.dismiss();
                                ToastOfJH.show("获取ActId失败");
                                return;
                            }

                            try {
                                JSONObject r = new JSONObject(value);
                                String actId = r.optString("activityId");
                                String rushJsVer = r.optString("rushJsVer");
                                params.put("activityId", actId);
                                params.put("rushJsVer", rushJsVer);

                                Log.d("sanron", "获取actId和jsVer完毕，开始createOrder步骤");

                                RushUtil.startRush(params, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String submitUrl) {
                                        Log.d("sanron", "提交订单页面url:" + submitUrl);
                                        gotoSubmitOrder(submitUrl);
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });
        });

        initWebView();
    }

    private String rlistJson;

    private void gotoSubmitOrder(String submitUrl) {
        final AtomicBoolean x = new AtomicBoolean(false);
        binding.webview.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                if (request.getUrl().toString().contains("rlist.json")) {
//                    if (rlistJson != null) {
//                        InputStream is = new ByteArrayInputStream(rlistJson.getBytes());
//                        return new WebResourceResponse("application/json", "UTF-8", is);
//                    } else {
//                        Request reqBu = new Request.Builder()
//                                .url(request.getUrl().toString())
//                                .post(RequestBody.create(null, ""))
//                                .headers(Headers.of(request.getRequestHeaders()))
//                                .header("Cookie", CookieManager.getInstance().getCookie("vmall.com"))
//                                .build();
//                        try {
//                            Response resp = RushUtil.sOkHttpClient.newCall(reqBu).execute();
//                            if (resp.isSuccessful()) {
//                                String json = resp.body().string();
//                                rlistJson = json;
//                                return new WebResourceResponse("application/json", "utf-8", new ByteArrayInputStream(json.getBytes()));
//                            }
//                        } catch (Throwable e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        binding.webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 50 && !x.get()) {
                    x.set(true);
                    view.evaluateJavascript("(function() {\n" +
                            "    function x() {\n" +
                            "        if (typeof(flowType)!='undefined' && typeof(ec)!='undefined' && ec.order && ec.order.checkOrder && ec.order.checkOrder.doSubmit &&\n" +
                            "            $ && $(\"#_address\").text()) {\n" +
                            "            console.log('begin submit');\n" +
                            "            Math.random = function() {\n" +
                            "                return 0;\n" +
                            "            }\n" +
                            "            ec.order.checkOrder.doSubmit();\n" +
                            "        } else {\n" +
                            "            setTimeout(function() {\n" +
                            "                x();\n" +
                            "            }, 50);\n" +
                            "        }\n" +
                            "    }\n" +
                            "    x();\n" +
                            "})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                }
            }
        });
        binding.webview.loadUrl(submitUrl);
    }


    private void initWebView() {
        WebSettings settings = binding.webview.getSettings();
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getCacheDir().toString());
        settings.setLoadWithOverviewMode(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDatabasePath(getApplicationContext().getCacheDir().getAbsolutePath());
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(0);
        }

        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);


        settings.setSaveFormData(true);
        settings.setSavePassword(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setGeolocationEnabled(true);
        settings.setGeolocationDatabasePath(getFilesDir().toString());
        settings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        settings.setLoadsImagesAutomatically(true);

        settings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true);
        }

        binding.webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    view.loadUrl(url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
    }


    @Override
    public void onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
