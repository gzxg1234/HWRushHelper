package com.sanron.hwrushhelper;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author chenrong
 * @date 2019/11/22
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class RushUtil {

    public static final String GET_RUSH_JS = "(function() {\n" +
            "    var ks = [\"uid\", \"user\", \"name\", \"ts\", \"valid\", \"sign\", \"cid\", \"wi\", \"ticket\", \"hasphone\", \"hasmail\",\n" +
            "        \"logintype\", \"rush_info\"\n" +
            "    ];\n" +
            "    getCookie = function(m) {\n" +
            "        var g = null;\n" +
            "        if (document.cookie && document.cookie != \"\") {\n" +
            "            var j = document.cookie.split(\";\");\n" +
            "            for (var k = 0; k < j.length; k++) {\n" +
            "                var l = (j[k] || \"\").replace(/^(\\s|\\u00A0)+|(\\s|\\u00A0)+$/g, \"\");\n" +
            "                if (l.substring(0, m.length + 1) == (m + \"=\")) {\n" +
            "                    var h = function(c) {\n" +
            "                        c = c.replace(/\\+/g, \" \");\n" +
            "                        var a = '()<>@,;:\\\\\"/[]?={}';\n" +
            "                        for (var b = 0; b < a.length; b++) {\n" +
            "                            if (c.indexOf(a.charAt(b)) != -1) {\n" +
            "                                if (c.startWith('\"')) {\n" +
            "                                    c = c.substring(1)\n" +
            "                                }\n" +
            "                                if (c.endWith('\"')) {\n" +
            "                                    c = c.substring(0, c.length - 1)\n" +
            "                                }\n" +
            "                                break\n" +
            "                            }\n" +
            "                        }\n" +
            "                        return decodeURIComponent(c)\n" +
            "                    };\n" +
            "                    g = h(l.substring(m.length + 1));\n" +
            "                    break\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        return g\n" +
            "    };\n" +
            "\n" +
            "    getLoginPars = function() {\n" +
            "        var c = {};\n" +
            "        for (i = 0; i < ks.length; i += 1) {\n" +
            "            var d = getCookie(ks[i]);\n" +
            "            d = d == null ? \"\" : encodeURIComponent(d);\n" +
            "            if (d) {\n" +
            "                c[ks[i]] = d\n" +
            "            }\n" +
            "        }\n" +
            "        return c\n" +
            "    }\n" +
            "    var sbom = rush.sbom.getCurrSbom();\n" +
            "    var o = {};\n" +
            "    o.mainSku = sbom.id;\n" +
            "    o.targetUrl = sbom.gotoUrl;\n" +
            "    o.diyPackCode = rush.sbom.getPackageCode();\n" +
            "    o.diyPackSkus = rush.sbom.getSubProductSkus();\n" +
            "    o.backUrl = domainMain + window.location.pathname + \"#\" + sbom.id;\n" +
            "    var extendSbomCode = $(\"#extendSelect\").attr(\"skuid\");\n" +
            "    var accidentSbomCode = $(\"#accidentSelect\").attr(\"skuid\");\n" +
            "    var ucareSbomCode = $(\"#ucareSelect\").attr(\"skuid\");\n" +
            "    var extendCodes = [];\n" +
            "    if (extendSbomCode) {\n" +
            "        extendCodes.push(extendSbomCode)\n" +
            "    }\n" +
            "    if (accidentSbomCode) {\n" +
            "        extendCodes.push(accidentSbomCode)\n" +
            "    }\n" +
            "    if (ucareSbomCode) {\n" +
            "        extendCodes.push(ucareSbomCode)\n" +
            "    }\n" +
            "    o.accessoriesSkus = extendCodes.join(\",\");\n" +
            "    var rushUrl = \"\";\n" +
            "    if (o.targetUrl && o.mainSku && o.mainSku.length > 0) {\n" +
            "        rushUrl = o.targetUrl + \"?mainSku=\" + o.mainSku;\n" +
            "        if (o.accessoriesSkus && o.accessoriesSkus.length > 0) {\n" +
            "            rushUrl += \"&accessoriesSkus=\" + o.accessoriesSkus\n" +
            "        }\n" +
            "        if (o.backUrl && o.backUrl.length > 0) {\n" +
            "            rushUrl += \"&backUrl=\" + encodeURIComponent(o.backUrl) + \"\"\n" +
            "        }\n" +
            "        rushUrl += \"&_t=\" + (new Date).getTime();\n" +
            "    }\n" +
            "\n" +
            "    var result = {};\n" +
            "    result.rushUrl = rushUrl;\n" +
            "    result.createOrderParams = getLoginPars();\n" +
            "    result.createOrderParams.skuId = o.mainSku;\n" +
            "    result.createOrderParams.skuIds = o.mainSku;\n" +
            "    return result;\n" +
            "})()";


    public static OkHttpClient sOkHttpClient;

    static {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        sOkHttpClient = new OkHttpClient.Builder()
                .followSslRedirects(true)
                .followRedirects(true)
                .connectTimeout(10000, TimeUnit.SECONDS)
                .readTimeout(10000, TimeUnit.SECONDS)
                .writeTimeout(3000, TimeUnit.SECONDS)
//                .addInterceptor(httpLoggingInterceptor)
                .build();
    }


    public static final String CREATE_ORDER_URL = "https://ord01.vmall.com/order/pwm86t/createOrder.do";
    public static final String SUBMIT_ORDER_URL = "https://buy.vmall.com/submit_order.html";

    public static Runnable startRush(JSONObject data, ValueCallback<String> callback) {

        final Object lock = new Object();
        String actId = data.optString("activityId");
        String skuId = data.optString("skuId");
        String rushJsVer = data.optString("rushJsVer");
        data.remove("rushJsVer");

        FormBody.Builder formBuilder = new FormBody.Builder();
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            formBuilder.add(k, data.opt(k).toString());
        }
        formBuilder.add("t", String.valueOf(System.currentTimeMillis()));
        Request.Builder builder = new Request.Builder()
                .post(formBuilder.build())
                .url(CREATE_ORDER_URL);
        Request request = builder.build();

        AtomicBoolean success = new AtomicBoolean(false);
        final long interval = 1000;

        List<Call> calls = new ArrayList<>();
        AtomicBoolean cancel = new AtomicBoolean(false);
        new Thread() {
            @Override
            public void run() {
                int i = 0;
                while (!success.get() && !cancel.get()) {
                    final int curI = ++i;
                    Call call = sOkHttpClient.newCall(request);
                    calls.add(call);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful() && !call.isCanceled() && !success.get()) {
                                String respStr = response.body().string();
//                    ec.util.cookie.set("orderSign-" + ec.activityId + "-" + k.uid, k.orderSign, j);
//                    if (ec.flowType == 2) {
//                        ec.reserveTimes(k, b)
//                    } else {
//                        var f = ec.url.chooseComponent + "?nowTime=" + d + skus;
//                        if (ec.isRequestFromVmall) {
//                            f = f + "&backUrl=" + encodeURIComponent(ec.paramForVmall.backUrl) + (ec.paramForVmall.giftSkus ? "&optionalGiftIds=" + ec.paramForVmall.giftSkus : "") + (ec.paramForVmall.accessoriesSkus ? "&componentIds=" + ec.paramForVmall.accessoriesSkus : "") + (ec.paramForVmall.diyPackSkus ? "&diyPackSkus=" + ec.paramForVmall.diyPackSkus : "")
//                        }
//                        var h = ec.url.activity;
//                        window.location.href = f + "&rushbuy_js_version=" + ec.rushbuy_js_version + "&backto=" + encodeURIComponent(h)
//                    }
//                }
                                try {
                                    JSONObject resp = new JSONObject(respStr);

                                    boolean test = new Random().nextInt(10) < 9 && false;

                                    if ((test || resp.optBoolean("success", false))) {
                                        synchronized (lock) {
                                            if (!success.get()) {
                                                success.set(true);
                                            } else {
                                                return;
                                            }
                                        }
                                        Log.d("sanron", String.format("第%d次请求查到有货，准备去提交订单页面", curI));
                                        for (Call qqcall : calls) {
                                            qqcall.cancel();
                                        }
                                        //成功，有余件，进入下一个提交订单页面
                                        String key = "orderSign-" + actId + "-" + resp.optString("uid");
                                        String value = String.format("%s;expires=%s;path=/;domain=vmall.com",
                                                resp.optString("orderSign", ""),
                                                new Date().toGMTString());
                                        Log.d("sanron", "设置cookie=>" + key + "=" + value);
                                        CookieManager.getInstance().setCookie("vmall.com", key + "=" + value);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            CookieManager.getInstance().flush();
                                        }

                                        String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                                        String submitQuery = String.format("nowTime=%s&skuId=%s&skuIds=%s&activityId=%s&rushbuy_js_version=%s",
                                                nowTime, skuId, skuId, actId, rushJsVer);
                                        String submitUrl = SUBMIT_ORDER_URL + "?" + submitQuery;
                                        new Handler(Looper.getMainLooper()).post(() -> callback.onReceiveValue(submitUrl));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    });
                    SystemClock.sleep(interval);
                }
            }
        }.start();
        return () -> {
            cancel.set(true);
            for (Call qqcall : calls) {
                qqcall.cancel();
            }
        };
    }
}
