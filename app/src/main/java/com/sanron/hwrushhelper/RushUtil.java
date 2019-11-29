package com.sanron.hwrushhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
        if (MainActivity.waitDlg != null) {
            sHandler.post(() -> {
                MainActivity.waitDlg.appendLog(msg);
            });
        }
        Log.d("sunron", msg);
    }

    public static void log2(String msg) {
        if (MainActivity.waitDlg != null) {
            sHandler.post(() -> {
                MainActivity.waitDlg.appendLog2(msg);
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


    public static class LocalDns implements Dns {
        private String[] NEED_CACHE = {"buy.vmall.com"};

        private Map<String, List<InetAddress>> cacheIps = new HashMap<>();

        public void clearCache() {
            cacheIps.clear();
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            for (String ch : NEED_CACHE) {
                if (ch.equals(hostname)) {
                    List<InetAddress> inetAddresses = cacheIps.get(hostname);
                    if (inetAddresses == null) {
                        inetAddresses = Dns.SYSTEM.lookup(hostname);
                        if (inetAddresses != null) {
                            cacheIps.put(hostname, inetAddresses);
                        }
                    }
                    return inetAddresses;
                }
            }
            return Dns.SYSTEM.lookup(hostname);
        }
    }

    private static LocalDns LOCAL_DNS = new LocalDns();

    static {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(null, null, null);
        sOkHttpClient = new OkHttpClient.Builder()
                .followSslRedirects(true)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier((hostname, session) -> true)
                .dns(LOCAL_DNS)
                .followRedirects(true)
                .build();
        rushClient = sOkHttpClient.newBuilder()
//                .addInterceptor(httpLoggingInterceptor)
                .callTimeout(1000, TimeUnit.MILLISECONDS).build();
    }

    public static String orderSignKey = "";
    public static String orderSignValue = "";

    public static final String CREATE_ORDER_URL = "https://ord01.vmall.com/order/pwm86t/createOrder.do";

    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public static Map<String, String> transCookie(String cookie) {
        Map<String, String> map = new HashMap<>();
        String[] dd = cookie.split(";");
        for (String itemStr : dd) {
            String[] item = itemStr.split("=");
            if (item.length == 2) {
                if (item[0].startsWith(" ")) {
                    item[0] = item[0].substring(1);
                }
                map.put(item[0], item[1]);
            }
        }
        return map;
    }

    public static Runnable startRush(JSONObject data, JSONObject address, JSONObject invoInfo, ValueCallback<String> callback) {
        try {
            data = new JSONObject(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String actId = data.optString("activityId");
        String skuId = data.optString("skuId");
        String cookies = data.optString("cookie");
        String uid = data.optString("uid");
        data.remove("cookie");

        Map<String, String> cookieMap = transCookie(cookies);

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


        final FormBody.Builder submitData = new FormBody.Builder();
        submitData.add("duid", cookieMap.get("uid"));
        submitData.add("uid", cookieMap.get("uid"));
        submitData.add("skuIds", skuId);
        submitData.add("quantity", String.valueOf(1));
        submitData.add("diyPackCodeArr", "");
        submitData.add("diyPackSkus", "");
        submitData.add("activityId", actId);
        submitData.add("streetId", address.optString("street"));
        submitData.add("street", address.optString("streetName"));
        submitData.add("districtId", address.optString("district"));
        submitData.add("district", address.optString("districtName"));
        submitData.add("cityId", address.optString("city"));
        submitData.add("city", address.optString("cityName"));
        submitData.add("provinceId", address.optString("province"));
        submitData.add("province", address.optString("provinceName"));
        submitData.add("consignee", address.optString("consignee"));
        submitData.add("address", address.optString("address"));
        submitData.add("mobile", address.optString("mobile"));
        submitData.add("phone", "");
        submitData.add("zipCode", "");
        submitData.add("custName", cookieMap.get("user"));
        submitData.add("titleType", invoInfo.optString("invoiceType"));
        submitData.add("invoiceTitle", invoInfo.optString("invoiceContext"));
        submitData.add("taxpayerIdentityNum", invoInfo.optString("taxpayerIdentityNum"));
        submitData.add("orderSource", "1");
        submitData.add("activityUid", uid);
        submitData.add("nickName", cookieMap.get("name"));

        RushUtil.log2("开始查询是否有货。。。。");
        executor.execute(() -> {
            int i = 0;
            long reQtime = 0;
            while (!cancel.get()) {
                if (reQtime == 0) {
                    reQtime = System.currentTimeMillis();
                }
                final int curI = ++i;
                Call call = rushClient.newCall(request);
                RushUtil.log2(String.format("第%d次查询是否有货", curI));
                try {
                    Response response = call.execute();
                    if (response.isSuccessful() && response.body() != null && !call.isCanceled() && !cancel.get()) {
                        ResponseBody body = response.body();
                        MediaType mediaType = body.contentType();
                        if (mediaType != null && "text".equals(mediaType.type())) {
                            //返回了html可能是请求太频繁被限制了,休息久一点
                            RushUtil.log2("请求太频繁，休息2s，持续了" + (System.currentTimeMillis() - reQtime) + "ms");
                            reQtime = 0;
                            SystemClock.sleep(1500);
                            continue;
                        }
                        String respStr = body.string();
                        try {
                            JSONObject resp = new JSONObject(respStr);
                            boolean test = new Random().nextInt(10) < 5 && false;

                            RushUtil.log2(String.format("第%d次查询结果%s", curI, resp.toString()));
                            if ((test || resp.optBoolean("success", false))) {

                                RushUtil.log(String.format("第%d次请求查到有货，时间为=%s", curI, System.currentTimeMillis()));

                                if (TextUtils.isEmpty(resp.optString("uid"))) {
                                    resp.put("uid", cookieMap.get("uid"));
                                }
                                orderSignKey = "orderSign-" + actId + "-" + resp.optString("uid");
                                orderSignValue = resp.optString("orderSign");

                                RushUtil.log(String.format("orderSign数据--->%s=%s", orderSignKey, orderSignValue));

                                String newCookie = cookies + "; " + orderSignKey + "=" + orderSignValue;
                                submitData.add("orderSign", orderSignValue);

                                boolean success = submit(newCookie, submitData);
                                if (cancel.get()) {
                                    return;
                                }
                                if (success) {
                                    sHandler.post(() -> {
                                        callback.onReceiveValue("true");
                                    });
                                } else {
                                    sHandler.post(() -> {
                                        callback.onReceiveValue(null);
                                    });
                                }
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


    public static boolean submit(String cookie, FormBody.Builder formBuilder) {
        RushUtil.log("====开始提交订单====");
        final FormBody formBody = formBuilder.build();
        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < formBody.size(); i++) {
                sb.append(formBody.name(i)).append("=").append(formBody.value(i)).append(", ");
            }
            RushUtil.log("提交订单参数:" + sb.toString());
        });

        Request.Builder builder = new Request.Builder()
                .header("Cookie", cookie)
                .header("Origin", "https://buy.vmall.com")
                .post(formBody)
                .url("https://buy.vmall.com/order/create.json");
        Call call = sOkHttpClient.newCall(builder.build());
        Response response = null;
        try {
            response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    if (obj.optBoolean("success", false)) {
                        log("订单提交成功！！！！");
                        return true;
                    } else {
                        log("提交订单失败，原因：" + json);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                LOCAL_DNS.clearCache();
            }
            e.printStackTrace();
        }
        return false;
    }


    public static void getEuid(Context context, ValueCallback<String> callback) {
        RushUtil.executor.execute(() -> {
            try {
                //预缓存dns
                try {
                    LOCAL_DNS.lookup("buy.vmall.com");
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                File appWebview = context.getDir("webview", Context.MODE_PRIVATE);
                File cookieFile = new File(appWebview, "Cookies");
                if (!cookieFile.exists()) {
                    cookieFile = new File(appWebview, "/Default/Cookies");
                }
                if (cookieFile.exists()) {
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(cookieFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                    try (Cursor cur = db.query("cookies", new String[]{"value"}, "name=? and host_key=?",
                            new String[]{"euid", ".vmall.com"}, null, null, null)
                    ) {
                        if (cur != null && cur.moveToFirst()) {
                            String euid = cur.getString(cur.getColumnIndex("value"));
                            RushUtil.log("提取到euid=" + euid);
                            sHandler.post(() -> {
                                callback.onReceiveValue(euid);
                            });
                            return;
                        }
                    }
                }
            } catch (Throwable e) {

            }
            sHandler.post(() -> {
                callback.onReceiveValue(null);
            });
            return;
        });
    }

    public static void getInvoice(String cookie, ValueCallback<JSONObject> callback) {
        Map<String, String> cookieMap = transCookie(cookie);
        Request.Builder builder = new Request.Builder()
                .header("Access-Control-Request-Headers", "csrftoken")
                .header("Access-Control-Request-Method", "GET")
                .header("Referer", "https://buy.vmall.com/submit_order.html?nowTime=2019-11-27%2013:47:30&skuId=10086239677333&skuIds=10086239677333&activityId=860120191122950&backUrl=https%3A%2F%2Fwww.vmall.com%2Fproduct%2F10086831441169.html%2310086239677333&rushbuy_js_version=0536f1be-fa18-4932-88de-78bf4b72c9f8&backto=https%3A%2F%2Fwww.vmall.com%2Fproduct%2F10086831441169.html%2310086239677333")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36")
                .method("OPTIONS", null)
                .url("https://openapi.vmall.com/uc/invoice/queryInvoiceList.json?userId=" + cookieMap.get("uid"));
        Request request = builder.build();
        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sHandler.post(() -> {
                    callback.onReceiveValue(null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    List<String> headers = response.headers("Set-Cookie");
                    StringBuilder newCookie = new StringBuilder(cookie);
                    newCookie.append("; ");
                    for (String c : headers) {
                        newCookie.append(c.replace("path=/", ""));
                    }
                    newCookie.replace(newCookie.length() - 2, newCookie.length(), "");

                    Request.Builder builder2 = new Request.Builder()
                            .header("Accept", "application/json, text/javascript, */*; q=0.01")
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .header("Accept-Language", "zh-CN,zh;q=0.9")
                            .header("Cookie", newCookie.toString())
                            .header("CsrfToken", cookieMap.get("CSRF-TOKEN"))
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "same-site")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36")
                            .get()
                            .url("https://openapi.vmall.com/uc/invoice/queryInvoiceList.json?userId=" + cookieMap.get("uid"));
                    Request request2 = builder2.build();
                    sOkHttpClient.newCall(request2).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            sHandler.post(() -> {
                                callback.onReceiveValue(null);
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            JSONObject result = new JSONObject();
                            if (response.isSuccessful() && response.body() != null && !call.isCanceled()) {
                                String json = response.body().string();
                                try {
                                    JSONObject respData = new JSONObject(json);
                                    if ("200000".equals(respData.optString("resultCode")) && respData.opt("data") != null) {
                                        int invoiceSubType = 1;
                                        String subTypeTitle = "个人";
                                        JSONObject data = respData.optJSONObject("data");
                                        JSONArray arr = data.optJSONArray("invoiceInfoVOList");
                                        JSONObject electronicInvoice = new JSONObject();
                                        if (arr != null && arr.length() > 0) {
                                            for (int i = 0; i < arr.length(); i++) {
                                                JSONObject item = arr.optJSONObject(i);
                                                if ("1".equals(item.optString("invoiceType"))) {
                                                    electronicInvoice = new JSONObject();
                                                    electronicInvoice.put("company", "");
                                                    electronicInvoice.put("taxpayerId", "");
                                                    electronicInvoice.put("personalTitle", "");
                                                } else {
                                                    if ("2".equals(item.optString("invoiceType"))) {
                                                        if (TextUtils.isEmpty(item.optString("personalTitle"))) {
                                                            item.put("personalTitle", "个人");
                                                        }
                                                        electronicInvoice = new JSONObject();
                                                        electronicInvoice.put("company", item.optString("company"));
                                                        electronicInvoice.put("taxpayerId", item.optString("taxpayerId"));
                                                        electronicInvoice.put("personalTitle", item.optString("personalTitle"));
                                                        break;
                                                    }
                                                }
                                            }
                                            if ("1".equals(data.optString("lastInvoiceTitleType"))) {
                                                if (!TextUtils.isEmpty(electronicInvoice.optString("personalTitle"))) {
                                                    subTypeTitle = electronicInvoice.optString("personalTitle");
                                                }
                                                invoiceSubType = 1;
                                            } else {
                                                if (!TextUtils.isEmpty(electronicInvoice.optString("company"))) {
                                                    subTypeTitle = electronicInvoice.optString("company");
                                                }
                                                invoiceSubType = 2;
                                            }
                                        }


                                        result.put("invoiceType", 50);
                                        result.put("invoiceContext", subTypeTitle);
                                        if (invoiceSubType == 2) {
                                            result.put("taxpayerIdentityNum", electronicInvoice.optString("taxpayerId"));
                                        }

                                        sHandler.post(() -> {
                                            callback.onReceiveValue(result);
                                        });
                                        return;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }

                            try {
                                result.put("invoiceType", 50);
                                result.put("invoiceContext", "个人");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            sHandler.post(() -> {
                                callback.onReceiveValue(result);
                            });
                        }
                    });

                }
            }
        });
    }

    public static void getAddress(String cookie, ValueCallback<JSONObject> callback) {
        Request.Builder builder = new Request.Builder()
                .header("Cookie", cookie)
                .post(RequestBody.create(null, ""))
                .url("https://addr.vmall.com/address/rlist.json");
        Request request = builder.build();
        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sHandler.post(() -> {
                    callback.onReceiveValue(null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null && !call.isCanceled()) {
                    String json = response.body().string();
                    try {
                        JSONObject obj = new JSONObject(json);
                        if ("32000".equals(obj.optString("code"))
                                && obj.optJSONArray("shoppingConfigList") != null
                                && obj.optJSONArray("shoppingConfigList").length() > 0) {
                            JSONArray jsonArray = obj.optJSONArray("shoppingConfigList");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                final JSONObject dd = jsonArray.getJSONObject(i);
                                if ("1".equals(dd.optString("defaultFlag"))) {
                                    sHandler.post(() -> {
                                        callback.onReceiveValue(dd);
                                    });
                                    return;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                sHandler.post(() -> {
                    callback.onReceiveValue(null);
                });
            }
        });
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
