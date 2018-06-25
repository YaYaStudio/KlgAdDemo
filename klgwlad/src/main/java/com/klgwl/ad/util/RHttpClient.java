package com.klgwl.ad.util;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：请求网络的基础类
 * 创建人员：Robi
 * 创建时间：2018/06/22 16:13
 * 修改人员：Robi
 * 修改时间：2018/06/22 16:13
 * 修改备注：
 * Version: 1.0.0
 */
public class RHttpClient {
    public static final int RES_CODE_SUCCESS = 200;
    private static final String TAG = "RHttpClient";
    private static final Integer TIMEOUT = 10 * 1000; // 允许反射修此常量，如果是基本类型，那么编译时会被优化直接替换，运行时无法在修改。
    private static final int BUFFER_SIZE = 1024;
    private static final String CHARSET = "UTF-8";
    private static final String HTTP_GET = "GET";
    private static final String HTTP_POST = "POST";

    public static RResult<String> get(final String urlStr, String... args) {
        return get(urlStr, map(args));
    }

    public static RResult<String> get(final String urlStr, final Map<String, String> headers) {
        L.d(TAG, "http get url=" + urlStr);
        RResult<String> result = new RResult<>();

        HttpURLConnection urlConnection = null;
        try {
            // conn
            urlConnection = buildGet(urlStr, headers);

            // request
            int resCode = result.code = urlConnection.getResponseCode(); // 开始连接并发送数据

            // response
            if (resCode == RES_CODE_SUCCESS) {
                result.obj = buildString(urlConnection.getInputStream());
                L.d(TAG, "http get success, result=" + result.obj + ", url=" + urlStr);
            } else {
                result.error = buildString(urlConnection.getErrorStream());
                L.e(TAG, "http get failed, code=" + resCode + ", url=" + urlStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.e = e;
            L.e(TAG, "http get error, e=" + e.getMessage() + ", url=" + urlStr);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result;
    }

    public static RResult<String> post(final String urlStr, String body) {
        return post(urlStr, null, body);
    }

    public static <T> RResult<String> post(final String urlStr, final Map<String, String> headers, T body) {
        L.d(TAG, "http post url=" + urlStr);
        RResult<String> result = new RResult<>();

        HttpURLConnection urlConnection = null;
        try {
            // conn
            urlConnection = buildPost(urlStr, headers, body); // os.flush 开始建立连接

            // request
            int resCode = result.code = urlConnection.getResponseCode(); // 开始发送数据

            // response
            if (resCode == RES_CODE_SUCCESS) {
                result.obj = buildString(urlConnection.getInputStream());
                L.d(TAG, "http post success, result=" + result + ", url=" + urlStr);
            } else {
                result.error = buildString(urlConnection.getErrorStream());
                L.e(TAG, "http post failed, code=" + resCode + ", url=" + urlStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.e = e;
            L.e(TAG, "http post error, e=" + e.getMessage() + ", url=" + urlStr);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }

    private static HttpURLConnection buildGet(String urlStr, final Map<String, String> headers) throws IOException {
        URL url = new URL(urlStr); // URLEncoder.encode(param, "UTF-8")

        // conn
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        configUrlConnection(urlConnection);
        urlConnection.setRequestMethod(HTTP_GET);

        // headers
        buildHeaders(urlConnection, headers);

        return urlConnection;
    }

    private static <T> HttpURLConnection buildPost(String urlStr, final Map<String, String> headers, T body) throws IOException {
        URL url = new URL(urlStr);

        // conn
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        configUrlConnection(urlConnection);
        urlConnection.setRequestMethod(HTTP_POST);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        // headers
        buildHeaders(urlConnection, headers);

        // json body
        buildJsonHeaders(urlConnection, body);

        // body
        OutputStream os = urlConnection.getOutputStream();
        DataOutputStream out = new DataOutputStream(os);
        IOException exception = null;
        try {
            if (body instanceof String) {
                out.write(((String) body).getBytes(CHARSET));
            } else if (body instanceof byte[]) {
                out.write((byte[]) body);
            } else if (body instanceof JSONObject) {
                out.write(body.toString().getBytes(CHARSET));
            } else if (body instanceof org.json.JSONObject) {
                out.write(body.toString().getBytes(CHARSET));
            }
            os.flush(); // 开始与对方建立三次握手。
        } catch (IOException e) {
            exception = e;
        } finally {
            out.close();
            os.close();
        }

        if (exception != null) {
            throw exception;
        }

        return urlConnection;
    }

    private static void configUrlConnection(HttpURLConnection urlConnection) {
        urlConnection.setReadTimeout(TIMEOUT);
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setUseCaches(false);
    }

    private static void buildHeaders(HttpURLConnection urlConnection, final Map<String, String> headers) {
        // common
        urlConnection.setRequestProperty("charset", CHARSET);

        // custom
        if (headers != null) {
            for (String key : headers.keySet()) {
                urlConnection.setRequestProperty(key, headers.get(key));
            }
        }
    }

    private static <T> void buildJsonHeaders(HttpURLConnection urlConnection, T body) {
        if (body instanceof String) {
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        } else if (body instanceof JSONObject ||
                body instanceof org.json.JSONObject) {
            urlConnection.setRequestProperty("Content-Type", "application/json");
        }
    }

    private static String buildString(final InputStream is) throws IOException {
        if (is == null) {
            return null;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            int len;
            byte buffer[] = new byte[BUFFER_SIZE];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            is.close();
            os.close();
        }

        return new String(os.toByteArray(), CHARSET);
    }

    /**
     * 组装参数
     */
    public static Map<String, String> map(String... args) {
        final Map<String, String> map = new HashMap<>();
        foreach(new OnPutValue() {
            @Override
            public void onValue(String key, String value) {
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }
            }

            @Override
            public void onRemove(String key) {
                map.remove(key);
            }
        }, args);
        return map;
    }

    private static void foreach(OnPutValue onPutValue, String... args) {
        if (onPutValue == null || args == null) {
            return;
        }
        for (String str : args) {
            String[] split = str.split(":");
            if (split.length >= 2) {
                String first = split[0];
                if (TextUtils.isEmpty(split[1])) {
                    onPutValue.onRemove(split[0]);
                } else {
                    onPutValue.onValue(first, str.substring(first.length() + 1));
                }
            } else if (split.length == 1) {
                onPutValue.onRemove(split[0]);
            }
        }
    }

    interface OnPutValue {
        void onValue(String key, String value);

        void onRemove(String key);
    }


}
