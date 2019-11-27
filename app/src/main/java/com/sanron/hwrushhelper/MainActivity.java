package com.sanron.hwrushhelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
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
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    //获取抢购页面的跳转地址
    static WaitDialog waitDlg;

    JSONObject rushParams;

    JSONObject cacheAddress;
    JSONObject cacheInvoice;

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
            waitDlg.btnStop.setVisibility(View.VISIBLE);
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
        binding.btnGetTime.setOnClickListener(v -> {
            syncTime();
        });
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

                    RushUtil.getInvoice(obj.optString("cookie"), new ValueCallback<JSONObject>() {
                        @Override
                        public void onReceiveValue(JSONObject invoceData) {
                            if (invoceData == null) {
                                ToastOfJH.show("获取发票信息失败");
                                waitDlg.dismiss();
                                return;
                            }
                            RushUtil.getAddress(obj.optString("cookie"), address -> {
                                if (address == null) {
                                    ToastOfJH.show("获取地址失败");
                                    waitDlg.dismiss();
                                    return;
                                }

                                WebView1 s = new WebView1(MainActivity.this);
                                s.getActId(rushUrl, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        if (!waitDlg.btnStop.isShown()) {
                                            return;
                                        }
                                        if ("error".equals(value)) {
                                            waitDlg.dismiss();
                                            ToastOfJH.show("获取ActId失败，重试一下");
                                            return;
                                        }

                                        try {
                                            JSONObject r = new JSONObject(value);
                                            String actId = r.optString("activityId");
                                            params.put("activityId", actId);
                                            params.put("cookie", r.optString("cookie"));

                                            rushParams = params;
                                            cacheInvoice = invoceData;
                                            cacheAddress = address;

                                            RushUtil.log(String.format("获取actId完毕，actId=%s", actId));
                                            RushUtil.log("开始轮训是否有货步骤");

                                            AlertDialog dlg = new AlertDialog.Builder(MainActivity.this).setMessage("基础参数准备完毕，确认开始轮训是否有货\n（开抢前1-2秒点）")
                                                    .setPositiveButton("确定", (dialog, which) -> {
                                                        waitDlg.setMessage("轮训是否有货。。。。");
                                                        waitDlg.show();
                                                        if (cancelQuery != null) {
                                                            cancelQuery.run();
                                                        }
                                                        startRush(params, address, invoceData, binding.cbAutoRetry.isChecked() ? 0 : -1);
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
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });
        });

        initWebView();
    }


    private void startRush(JSONObject params, JSONObject address, JSONObject invoice, int count) {
        if (count >= 1) {
            RushUtil.log("第" + count + "次重试中。。。");
            waitDlg.setMessage("第" + count + "次重试中。。。太久没反应就关了");
        }
        if (cancelQuery != null) {
            cancelQuery.run();
        }
        cancelQuery = RushUtil.startRush(params, address, invoice, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String r) {
                if (!waitDlg.btnStop.isShown() || !waitDlg.isShowing()) {
                    return;
                }

                if (r != null) {
                    waitDlg.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("抢到啦！！！！！！！！！！！！！！！！！！！")
                            .show();
                } else {
                    if (count >= 0) {
                        startRush(params, address, invoice, count + 1);
                    } else {
                        waitDlg.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("毛抢到！！！！！！！！！！！！！！！！！！！")
                                .setNegativeButton("取消", null)
                                .setCancelable(false)
                                .setPositiveButton("重试", (dialog, which) -> {
                                    startRush(params, address, invoice, -1);
                                })
                                .show();
                    }
                }
            }
        });
    }

    private void initWebView() {
        WebSettings settings = binding.webview.getSettings();

        settings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

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
