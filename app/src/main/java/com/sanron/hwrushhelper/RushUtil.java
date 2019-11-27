package com.sanron.hwrushhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.ValueCallback;

import com.tencent.smtt.sdk.CookieManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author chenrong
 * @date 2019/11/22
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class RushUtil {

    public static SharedPreferences sp = App.getInstance().getSharedPreferences("se", Context.MODE_PRIVATE);


    public static Handler sHandler = new Handler(Looper.getMainLooper());

    public static void log(String msg) {
        if(MainActivity.waitDlg!=null){
            sHandler.post(() -> {
                MainActivity.waitDlg.appendLog(msg);
            });
        }
        Log.d("sunron", msg);
    }

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
            "    result.cookie = document.cookie;\n" +
            "    result.createOrderParams = getLoginPars();\n" +
            "    result.createOrderParams.skuId = o.mainSku;\n" +
            "    result.createOrderParams.skuIds = o.mainSku;\n" +
            "    return result;\n" +
            "})()";


    public static OkHttpClient sOkHttpClient;
    public static OkHttpClient rushClient;

    static {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        sOkHttpClient = new OkHttpClient.Builder()
                .followSslRedirects(true)
                .followRedirects(true)
                .build();
        rushClient = sOkHttpClient.newBuilder()
//                .addInterceptor(httpLoggingInterceptor)
                .callTimeout(1000, TimeUnit.MILLISECONDS).build();
    }


    public static final String CREATE_ORDER_URL = "https://ord01.vmall.com/order/pwm86t/createOrder.do";
    public static final String SUBMIT_ORDER_URL = "https://buy.vmall.com/submit_order.html";

    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public static Runnable startRush(JSONObject data, ValueCallback<String> callback) {
        try {
            data = new JSONObject(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String actId = data.optString("activityId");
        String skuId = data.optString("skuId");
        String rushJsVer = data.optString("rushJsVer");
        String cookies = data.optString("cookie");
        String uid = data.optString("uid");
        data.remove("cookie");
        data.remove("rushJsVer");

        FormBody.Builder formBuilder = new FormBody.Builder();
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            formBuilder.add(k, data.opt(k).toString());
        }
        Date nowDate = new Date();
        formBuilder.add("t", String.valueOf(nowDate.getTime()));
        formBuilder.add("nowTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(nowDate));
        Request.Builder builder = new Request.Builder()
                .post(formBuilder.build())
                .header("Cookie", cookies)
                .url(CREATE_ORDER_URL);
        Request request = builder.build();

        AtomicBoolean cancel = new AtomicBoolean(false);
        long interval = getQueryInterval();

        executor.execute(() -> {
            int i = 0;
            while (!cancel.get()) {
                final int curI = ++i;
                Call call = rushClient.newCall(request);
                RushUtil.log( String.format("第%d次查询是否有货", curI));
                try {
                    Response response = call.execute();
                    if (response.isSuccessful() && response.body() != null && !call.isCanceled() && !cancel.get()) {
                        ResponseBody body = response.body();
                        MediaType mediaType = body.contentType();
                        if (mediaType != null && "text".equals(mediaType.type())) {
                            //返回了html可能是请求太频繁被限制了,休息久一点
                            RushUtil.log("请求太频繁，休息2s");
                            SystemClock.sleep(1500);
                            continue;
                        }
                        String respStr = body.string();
                        try {
                            JSONObject resp = new JSONObject(respStr);
                            boolean test = new Random().nextInt(10) < 5 && false;

                            RushUtil.log(String.format("第%d次查询结果%s", curI, resp.toString()));
                            if ((test || resp.optBoolean("success", false))) {
                                if (test) {
                                    resp.put("orderSign", "testOrderSign");
                                    resp.put("uid", uid);
                                }

                                RushUtil.log(String.format("第%d次请求查到有货，准备去提交订单页面", curI));
                                //成功，有余件，进入下一个提交订单页面

                                //设置Cookies
                                String key = "orderSign-" + actId + "-" + resp.optString("uid");
                                String cookie = String.format(key + "=%s;path=/;domain=vmall.com",
                                        resp.optString("orderSign", ""));
                                RushUtil.log("设置cookie=>" + cookie);
                                CookieManager.getInstance().setCookie("vmall.com", cookie);
                                CookieManager.getInstance().flush();
                                String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                                String submitQuery = String.format("nowTime=%s&skuId=%s&skuIds=%s&activityId=%s&rushbuy_js_version=%s",
                                        nowTime, skuId, skuId, actId, rushJsVer);
                                String submitUrl = SUBMIT_ORDER_URL + "?" + submitQuery;
                                sHandler.post(() -> callback.onReceiveValue(submitUrl));
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SystemClock.sleep(interval);
            }
        });

        return () -> cancel.set(true);
    }


    public static void getHwTime(ValueCallback<Date> callback) {
        String url = "https://www.vmall.com/system/getSysDate.json";
        Request.Builder builder = new Request.Builder()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .url(url);
        Request request = builder.build();
//
        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String date = response.header("Date");
                    sHandler.post(() -> {
                        Date x = new Date(date);
                        callback.onReceiveValue(x);
                    });
                }
            }
        });
    }

    public static void setQueryInterval(long t) {
        sp.edit()
                .putLong("qi", t)
                .apply();
    }

    public static long getQueryInterval() {
        return sp.getLong("qi", 100);
    }
}
