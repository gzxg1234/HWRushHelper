package com.sanron.hwrushhelper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.sanron.hwrushhelper.databinding.ActivityMainBinding;
import com.sanron.hwrushhelper.databinding.DlgLogBinding;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.databinding.DataBindingUtil;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    //获取抢购页面的跳转地址
    ProgressDialog waitDlg;

    JSONObject rushParams;

    Runnable cancelQuery;

    int retryCount = 0;
    boolean retrying = false;
    boolean needRetry = false;
    long timeCha = 0;
    Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        initUrl();
        syncTime();
    }

    private void initUrl() {
        binding.webview.loadUrl("https://www.vmall.com/product/10086374426533.html");
    }

    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private void syncTime() {
        RushUtil.getHwTime(value -> {
            if (value != null) {
                timeCha = value.getTime() - System.currentTimeMillis();
                binding.tvTime.setText(sdf.format(value));
                if (mTimer != null) {
                    mTimer.cancel();
                }
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        binding.tvTime.post(() -> {
                            binding.tvTime.setText(sdf.format(System.currentTimeMillis() + timeCha));
                        });
                    }
                }, 0, 10);
            }
        });
    }

    private class SettingDialog extends AppCompatDialog {

        public SettingDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            DlgLogBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dlg_log, null, false);
            setContentView(binding.getRoot());
            binding.etQueueInterval.setText(String.valueOf(RushUtil.getQueryInterval()));
            binding.btnQueueIntervalOk.setOnClickListener(v -> {
                try {
                    long x = Long.parseLong(binding.etQueueInterval.getText().toString());
                    RushUtil.setQueryInterval(x);
                } catch (Throwable e) {

                }
            });

        }
    }


    private void initView() {
        waitDlg = new ProgressDialog(MainActivity.this);
        binding.btnMate30.setOnClickListener(v -> {
            binding.webview.clearHistory();
            binding.webview.loadUrl("https://www.vmall.com/product/10086374426533.html");
        });
        binding.btnSetting.setOnClickListener(v -> {
            new SettingDialog(MainActivity.this).show();
        });
        binding.btnGetTime.setOnClickListener(v -> {
            syncTime();
        });
        binding.btnMatex.setOnClickListener(v -> {
            binding.webview.clearHistory();
            binding.webview.loadUrl("https://www.vmall.com/product/10086831441169.html");
        });
//        binding.etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_GO) {
//                    binding.webview.loadUrl(binding.etUrl.getText().toString());
//                    return false;
//                }
//                return false;
//            }
//        });
        binding.btnRefresh.setOnClickListener(v -> {
            binding.webview.reload();
        });

        waitDlg.setOnCancelListener(dialog -> {
            if (cancelQuery != null) {
                cancelQuery.run();
            }
            needRetry = false;
        });
        binding.btnReadyRush.setOnClickListener(v -> {
            needRetry = binding.cbAutoRetry.isChecked();
            retryCount = 0;
            waitDlg.setMessage("获取基础参数。。。太久没反应关了");
            waitDlg.show();
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
                                waitDlg.dismiss();
                                ToastOfJH.show("基础参数获取失败，重试一下");
                                return;
                            }

                            try {
                                JSONObject r = new JSONObject(value);
                                String actId = r.optString("activityId");
                                String rushJsVer = r.optString("rushJsVer");
                                params.put("activityId", actId);
                                params.put("rushJsVer", rushJsVer);

                                rushParams = params;

                                Log.d("sanron", "获取actId和jsVer完毕，开始createOrder步骤");

                                if (cancelQuery != null) {
                                    cancelQuery.run();
                                }
                                cancelQuery = RushUtil.startRush(params, new ValueCallback<String>() {
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
        binding.webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 50 && !x.get()) {
                    x.set(true);
                    view.evaluateJavascript("(function() {\n" +
                            "    function x() {\n" +
                            "        if (typeof flowType != 'undefined' && typeof ec != 'undefined' && ec.order && ec.order.checkOrder && ec.order\n" +
                            "            .checkOrder.doSubmit &&\n" +
                            "            $ && $(\"#_address\").text()) {\n" +
                            "            var originAjax = $.ajax;\n" +
                            "            $.ajax = function(e, t) {\n" +
                            "                if (\"object\" == typeof e) {\n" +
                            "                    app.log('e = ' + JSON.stringify(e));\n" +
                            "                    if (e.url.indexOf('/order/create.json') > 0) {\n" +
                            "                        app.log('拦截create.json请求');\n" +
                            "                        var oS = e.success;\n" +
                            "                        var oE = e.error;\n" +
                            "                        e.error = function() {\n" +
                            "                            app.log(\"提交订单失败\");\n" +
                            "                            native.rushResult(false);\n" +
                            "                            oE();\n" +
                            "                        }\n" +
                            "                        e.success = function(result) {\n" +
                            "                            if (\"object\" != typeof e) {\n" +
                            "                                app.log(\"提交订单失败\");\n" +
                            "                                native.rushResult(false);\n" +
                            "                                return;\n" +
                            "                            }\n" +
                            "                            // oS(result);\n" +
                            "                            app.log(\"create.json接口结果\" + JSON.stringify(result));\n" +
                            "                            if (result.success) {\n" +
                            "                                app.log(\"提交订单成功啦啦啦啦\");\n" +
                            "                                native.rushResult(true);\n" +
                            "                            } else {\n" +
                            "                                app.log(\"提交订单失败\");\n" +
                            "                                native.rushResult(false);\n" +
                            "                            }\n" +
                            "                        }\n" +
                            "                    }\n" +
                            "                }\n" +
                            "                originAjax(e, t);\n" +
                            "            }\n" +
                            "\n" +
                            "            app.log('begin submit');\n" +
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
                            "})()", value -> {

                    });
                }
            }
        });
        binding.webview.loadUrl(submitUrl);
    }


    private void initWebView() {
        WebSettings settings = binding.webview.getSettings();

        settings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

        binding.webview.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void rushResult(boolean success) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (success) {
                        waitDlg.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("抢到啦！！！！！！！！！！！！！！！！！！！")
                                .show();
                    } else {
                        if (needRetry) {
                            retryCount++;
                            waitDlg.setMessage("第" + retryCount + "重试中。。。太久没反应就关了");
                            waitDlg.setCanceledOnTouchOutside(false);
                            if (rushParams != null) {
                                waitDlg.show();
                                if (cancelQuery != null) {
                                    cancelQuery.run();
                                }
                                cancelQuery = RushUtil.startRush(rushParams, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String submitUrl) {
                                        Log.d("sanron", "提交订单页面url:" + submitUrl);
                                        gotoSubmitOrder(submitUrl);
                                    }
                                });
                            }
                        } else {
                            retryCount = 0;
                            waitDlg.dismiss();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("毛抢到！！！！！！！！！！！！！！！！！！！")
                                    .setNegativeButton("取消", null)
                                    .setCancelable(false)
                                    .setPositiveButton("重试", (dialog, which) -> {
                                        if (rushParams != null) {
                                            waitDlg.show();
                                            if (cancelQuery != null) {
                                                cancelQuery.run();
                                            }
                                            cancelQuery = RushUtil.startRush(rushParams, new ValueCallback<String>() {
                                                @Override
                                                public void onReceiveValue(String submitUrl) {
                                                    Log.d("sanron", "提交订单页面url:" + submitUrl);
                                                    gotoSubmitOrder(submitUrl);
                                                }
                                            });
                                        }
                                    })
                                    .show();
                        }
                    }
                });

            }
        }, "native");
        binding.webview.setWebViewClient(new WebViewClient() {

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl().getLastPathSegment().endsWith(".jpg")
                        || request.getUrl().getLastPathSegment().endsWith(".gif")
                        || request.getUrl().getLastPathSegment().endsWith(".jpeg")
                        || request.getUrl().getLastPathSegment().endsWith(".png")) {
                    return new WebResourceResponse(null, null, null);
                }
                return super.shouldInterceptRequest(view, request);
            }

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
//            super.onBackPressed();
        }
    }
}
