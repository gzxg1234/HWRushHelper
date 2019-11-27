package com.sanron.hwrushhelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.databinding.DataBindingUtil;

import com.sanron.hwrushhelper.databinding.ActivityMainBinding;
import com.sanron.hwrushhelper.databinding.DlgLogBinding;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    //获取抢购页面的跳转地址
    static WaitDialog waitDlg;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    public static class WaitDialog extends Dialog {

        TextView tvMsg;
        TextView tvLog;
        Button btnCancel, btnStop;

        public WaitDialog(@NonNull Context context) {
            super(context);
            setContentView(R.layout.dlg_wait);

            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            getWindow().setAttributes(layoutParams);

            tvLog = findViewById(R.id.tv_log);
            tvMsg = findViewById(R.id.tv_msg);
            btnCancel = findViewById(R.id.btn_cancel);
            btnStop = findViewById(R.id.btn_stop);
            btnCancel.setOnClickListener(v -> {
                dismiss();
            });
            tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        }


        public void setMessage(String msg) {
            tvMsg.setText(msg);
        }

        public void reset() {
            tvLog.setText("");
            tvMsg.setText("");
            waitDlg.btnStop.setText("停止抢单");
        }

        public void appendLog(String log) {
            if (tvLog.length() > 0) {
                tvLog.append("\n\n");
            }
            tvLog.append(log);
            int offset = tvLog.getLineCount() * tvLog.getLineHeight();
            if (offset > tvLog.getHeight()) {
                tvLog.scrollTo(0, offset - tvLog.getHeight());
            }
        }
    }


    private void initView() {
        waitDlg = new WaitDialog(MainActivity.this);
        waitDlg.setCanceledOnTouchOutside(false);
        binding.btnMate30.setOnClickListener(v -> {
            binding.webview.clearHistory();
            binding.webview.loadUrl("https://www.vmall.com/product/10086374426533.html");
        });
        binding.btnMatex.setOnClickListener(v -> {
            binding.webview.clearHistory();
            binding.webview.loadUrl("https://www.vmall.com/product/10086831441169.html");
        });
        binding.btnSetting.setOnClickListener(v -> {
            new SettingDialog(MainActivity.this).show();
        });
//        binding.btnGetAddress.setOnClickListener(v -> {
//            binding.webview.evaluateJavascript("document.cookie", cookie -> {
//                RushUtil.getAddress(cookie, value1 -> {
//                    if (value1 == null) {
//                        ToastOfJH.show("缓存地址失败");
//                        return;
//                    }
//                    try {
//                        JSONArray shoppingConfigList = new JSONObject(value1).optJSONArray("shoppingConfigList");
//                        JSONObject j;
//                        for (int i = 0; i < shoppingConfigList.length(); i++) {
//                            if ("1".equals(shoppingConfigList.optJSONObject(i).optString("defaultFlag"))) {
//                                j = shoppingConfigList.optJSONObject(i);
//                                new AlertDialog.Builder(MainActivity.this)
//                                        .setTitle("成功，默认地址数据如下")
//                                        .setMessage(j.toString())
//                                        .setPositiveButton("确定", (dialog, which) -> {
//                                            dialog.dismiss();
//                                        })
//                                        .show();
//                                break;
//                            }
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                });
//            });
//        });
        binding.btnGetTime.setOnClickListener(v -> {
            syncTime();
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

        waitDlg.setOnDismissListener(dialog -> {
            if (cancelQuery != null) {
                cancelQuery.run();
            }
            needRetry = false;
        });
        waitDlg.btnStop.setOnClickListener(v -> {
            if (cancelQuery != null) {
                cancelQuery.run();
            }
            needRetry = false;
            waitDlg.setMessage("已经停止");
            waitDlg.btnStop.setVisibility(View.GONE);
        });
        binding.btnReadyRush.setOnClickListener(v -> {
            waitDlg.reset();
            needRetry = binding.cbAutoRetry.isChecked();
            retryCount = 0;
            waitDlg.setMessage("获取基础参数。。。太久没反应关了");
            waitDlg.show();
            binding.webview.evaluateJavascript(RushUtil.GET_RUSH_JS, value -> {
                try {
                    JSONObject obj = new JSONObject(value);
                    JSONObject params = obj.optJSONObject("createOrderParams");
                    String rushUrl = Html.fromHtml(obj.optString("rushUrl")).toString();
                    RushUtil.log("排队页面url=" + rushUrl);


                    RushUtil.getAddress(obj.optString("cookie"), value1 -> {
                        if (value1 == null) {
                            ToastOfJH.show("获取地址失败");
                            waitDlg.dismiss();
                            return;
                        }

                        RushUtil.log("缓存地址列表ok");

                        WebView1 s = new WebView1(MainActivity.this);
                        s.getActId(rushUrl, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if (!waitDlg.btnStop.isShown()) {
                                    return;
                                }
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
                                    params.put("cookie", r.optString("cookie"));

                                    rushParams = params;

                                    RushUtil.log(String.format("获取actId和jsVer完毕，actId=%s,jsVer=%s", actId, rushJsVer));
                                    RushUtil.log("开始轮训是否有货步骤");

                                    AlertDialog dlg = new AlertDialog.Builder(MainActivity.this).setMessage("基础参数准备完毕，确认开始轮训是否有货\n（开抢前1-2秒点）")
                                            .setPositiveButton("确定", (dialog, which) -> {
                                                waitDlg.setMessage("轮训是否有货。。。。");
                                                waitDlg.show();
                                                if (cancelQuery != null) {
                                                    cancelQuery.run();
                                                }
                                                cancelQuery = RushUtil.startRush(params, new ValueCallback<String>() {
                                                    @Override
                                                    public void onReceiveValue(String submitUrl) {
                                                        waitDlg.setMessage("尝试提交订单。。。。");
                                                        RushUtil.log("准备提交,提交页面url:" + submitUrl);
                                                        gotoSubmitOrder(submitUrl);
                                                    }
                                                });
                                            })
                                            .setOnCancelListener(dialog -> {
                                                waitDlg.dismiss();
                                            })
                                            .setNegativeButton("取消", (dialog, which) -> {
                                                dialog.cancel();
                                            })
                                            .create();
                                    dlg.setCanceledOnTouchOutside(false);
                                    dlg.show();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
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
                            "    var count = 0;\n" +
                            "\n" +
                            "    function x() {\n" +
                            "        if (++count >= 1000) {\n" +
                            "            app.log(\"提交订单页面获取参数超时\");\n" +
                            "            native.rushResult(false);\n" +
                            "            return;\n" +
                            "        }\n" +
                            "        if (typeof flowType != 'undefined' && typeof ec != 'undefined' && ec.order && ec.order.checkOrder\n " +
                            "            && ec.order.checkOrder.doSubmit && $\n" +
                            "            && $(\"#_address\").text()) {\n" +
                            "            var originAjax = $.ajax;\n" +
                            "            $.ajax = function(e, t) {\n" +
                            "                if (\"object\" == typeof e) {\n" +
                            "                    if (e.url.indexOf('/order/create.json') > 0) {\n" +
                            "                        app.log('发出提交订单请求,时间为'+Date.now());\n" +
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
                            "                            app.log(\"提交订单返回结果\" + JSON.stringify(result));\n" +
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
                            "            app.log('请求提交');\n" +
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
                    if (!waitDlg.btnStop.isShown()) {
                        return;
                    }

                    if (success) {
                        waitDlg.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("抢到啦！！！！！！！！！！！！！！！！！！！")
                                .show();
                    } else {
                        if (needRetry) {
                            retryCount++;
                            RushUtil.log("第" + retryCount + "次重试中。。。");
                            waitDlg.setMessage("第" + retryCount + "次重试中。。。太久没反应就关了");
                            waitDlg.setCanceledOnTouchOutside(false);
                            if (rushParams != null) {
                                waitDlg.show();
                                if (cancelQuery != null) {
                                    cancelQuery.run();
                                }
                                cancelQuery = RushUtil.startRush(rushParams, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String submitUrl) {
                                        RushUtil.log("提交订单页面url:" + submitUrl);
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
                                                    RushUtil.log("提交订单页面url:" + submitUrl);
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
                if (request.getUrl().getLastPathSegment() != null
                        && (request.getUrl().getLastPathSegment().endsWith(".jpg")
                        || request.getUrl().getLastPathSegment().endsWith(".gif")
                        || request.getUrl().getLastPathSegment().endsWith(".jpeg")
                        || request.getUrl().getLastPathSegment().endsWith(".png"))) {
                    return new WebResourceResponse(null, null, null);
                } else if (request.getUrl().getLastPathSegment() != null
                        && request.getUrl().getLastPathSegment().contains("rlist.json")
                        && RushUtil.addressCache != null) {
                    try {
                        WebResourceResponse wrr = new WebResourceResponse("application/json", "UTF-8", new BufferedInputStream(new ByteArrayInputStream(RushUtil.addressCache.getBytes("utf-8"))));
                        wrr.setStatusCodeAndReasonPhrase(200, "");
                        Map<String,String> map = new HashMap<>();
                        map.put("Access-Control-Allow-Credentials","true");
                        map.put("Access-Control-Allow-Headers","x-requested-with,CsrfToken");
                        map.put("Access-Control-Allow-Methods","POST,GET");
                        map.put("Access-Control-Allow-Origin","https://buy.vmall.com");
                        map.put("Access-Control-Max-Age","3600");
                        map.put("Cache-Control","no-store");
                        wrr.setResponseHeaders(map);
                        return wrr;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
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
