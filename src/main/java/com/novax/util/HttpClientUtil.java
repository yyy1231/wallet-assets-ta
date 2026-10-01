package com.novax.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.net.SocketTimeoutException;
import java.util.*;

public class HttpClientUtil {

    static Logger logger = Logger.getLogger(HttpClientUtil.class);

    private static final PoolingHttpClientConnectionManager cm;
    private static final CloseableHttpClient httpClient;
    private static final String HTTPSTATUS = "HttpStatus";
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(20000)
            .setConnectionRequestTimeout(20000)
            .setSocketTimeout(30000)
            .build();

    static {
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
    }

    private static void applyHeaders(HttpRequestBase request, HashMap<String, String> headers) {
        if (headers == null) return;
        AllureParam.set("headers", JSON.toJSONString(headers));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
    }

    public static JSONObject sendGet(String url, HashMap<String, String> params, HashMap<String, String> headers)
            throws Exception {
        JSONObject responseJson = null;
        // 拼接url?key=value
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String value = entry.getValue();
                if (!value.isEmpty()) {
                    pairs.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs), "UTF-8");
        }
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", JSON.toJSONString(params));
        // 创建httpGet请求，传入请求url
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(REQUEST_CONFIG);
        // HttpGet添加请求头信息
        applyHeaders(httpGet, headers);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "UTF-8");
                responseJson = JSON.parseObject(responseEntityString);
                responseJson.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            // 释放请求，关闭连接
            EntityUtils.consume(responseEntity);

        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return responseJson;
    }

    public static JSONObject sendGet(String url, HashMap<String, String> params) throws Exception {
        return sendGet(url, params, null);
    }

    public static JSONObject sendGet(String url) throws Exception {
        return sendGet(url, null, null);
    }

    public static JSONObject sendPostByJson(String url, Object object, HashMap<String, String> headers) throws Exception {
        JSONObject responseJson = null;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(REQUEST_CONFIG);
        // 设置请求主体
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPost.setEntity(requestEntity);

        // HttpPost添加请求头信息
        applyHeaders(httpPost, headers);
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                responseJson = JSON.parseObject(responseEntityString);
                responseJson.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return responseJson;
    }

    public static int sendPostByJsonReturnHttpStatus(String url, Object object, HashMap<String, String> headers) throws Exception {
        int httpStatus = 0;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(REQUEST_CONFIG);
        // 设置请求主体
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPost.setEntity(requestEntity);

        // HttpPost添加请求头信息
        applyHeaders(httpPost, headers);
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            httpStatus = response.getStatusLine().getStatusCode();
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return httpStatus;
    }

    public static JSONObject sendPostByJson(String url, Object object) throws Exception {
        return sendPostByJson(url, object, null);
    }

    public static JSONObject sendPostByForm(String url, Map<String, String> formData, HashMap<String, String> headers) throws Exception {
        JSONObject jsonObject = null;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(REQUEST_CONFIG);
        // 设置请求主体
        UrlEncodedFormEntity entity = null;
        if (formData.size() > 0) {
            ArrayList<BasicNameValuePair> list = new ArrayList<>();
            formData.forEach((key, value) -> list.add(new BasicNameValuePair(key, value)));
            entity = new UrlEncodedFormEntity(list, "utf-8");
        }
        Objects.requireNonNull(entity).setContentType("application/x-www-form-urlencoded");
        httpPost.setEntity(entity);
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", JSON.toJSONString(formData));
        applyHeaders(httpPost, headers);
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public static JSONObject sendPostByForm(String url, Map<String, String> formData) throws Exception {
        return sendPostByForm(url, formData, null);
    }


    public static JSONObject sendPut(String url, String entityString, HashMap<String, String> headers) throws Exception {
        JSONObject jsonObject = null;
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(REQUEST_CONFIG);
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", entityString);
        httpPut.setEntity(new StringEntity(entityString, "utf-8"));
        applyHeaders(httpPut, headers);
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public static JSONObject sendPut(String url, Object object, HashMap<String, String> headers) throws Exception {
        JSONObject jsonObject = null;
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(REQUEST_CONFIG);
        // 设置请求主体
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAddress", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPut.setEntity(requestEntity);

        // HttpPut添加请求头信息
        applyHeaders(httpPut, headers);
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public static int sendDelete(String url) throws Exception {
        int statusCode = 0;
        HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.setConfig(REQUEST_CONFIG);
        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            statusCode = response.getStatusLine().getStatusCode();
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时:" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return statusCode;
    }
}
