package com.xxl.job.admin.core.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * url调用工具类
 *
 * @author ydl
 */
public class UrlUtils {
    private static final Logger log = LoggerFactory.getLogger(UrlUtils.class);

    /**
     * url连接超时最大重试次数
     */
    private static final int TIMEOUT_RETYR_MAX_TIME = 2;

    /**
     * 读取url内容
     *
     * @param urlStr
     * @param timeOutSeconds 连接超时时间，单位秒
     * @return
     */
    public static String readUrlContent(String urlStr, int timeOutSeconds) throws IOException {
        try {
            return readInputStream(readUrlStream(urlStr, 0, timeOutSeconds));
        } catch (IOException e) {
            log.error("通过url地址获取文本内容失败 Exception：" + e);
            throw e;
        }
    }

    /**
     * 读取url内容流
     *
     * @param urlStr
     * @param retryTime      重试次数，超时时重试3次
     * @param timeOutSeconds 连接超时时间，单位秒
     * @return
     */
    public static InputStream readUrlStream(String urlStr, int retryTime, int timeOutSeconds) throws IOException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置超时间为3秒
            conn.setConnectTimeout(timeOutSeconds * 1000);
            //防止屏蔽程序抓取而返回403错误
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            //得到输入流
            return conn.getInputStream();
        } catch (SocketTimeoutException e) {
            if (retryTime > TIMEOUT_RETYR_MAX_TIME) {
                log.error("通过url地址获取文本内容失败，超时次数太多！", e);
                throw e;
            } else {
                log.info("请求超时,开始重试 {} {} {}", retryTime, urlStr, timeOutSeconds);
                return readUrlStream(urlStr, retryTime + 1, 5);
            }
        } catch (Exception e) {
            log.error("通过url地址获取文本内容失败 Exception：" + e);
            throw e;
        }
    }

    /**
     * 从输入流中获取字符串
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }


    /**
     * 指定get或post方式请求
     *
     * @author ydl
     */
    public static String executePost(String url, byte[] data) {
        if (null == url || "".equals(url = url.trim())) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader buf = null;
        OutputStream os = null;
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(20 * 1000);
            conn.setReadTimeout(20 * 1000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (null != data && data.length > 0) {
                os = conn.getOutputStream();
                os.write(data);
                os.flush();
            }
            buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while (null != (line = buf.readLine())) {
                sb.append(line).append(System.lineSeparator());
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            close(buf);
            close(os);
        }

        return sb.toString();
    }

    private static void close(Closeable c) {
        if (null == c) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            log.error("", e);
        }
        c = null;
    }
}
