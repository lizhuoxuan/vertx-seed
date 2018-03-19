package com.zynet.brush.tool

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.logging.LoggerFactory
//import org.apache.poi.hssf.usermodel.HSSFRow
//import org.apache.poi.hssf.usermodel.HSSFSheet
//import org.apache.poi.hssf.usermodel.HSSFWorkbook

import java.security.MessageDigest
import java.text.NumberFormat
import java.time.LocalTime

/**
 * Created by liurui on 2016/11/30.
 *
 */
class CommonTool {

    static boolean isDebug = true

    static int loginTimeout = 60 * 2  //120分钟无操作，踢出用户

    static String md5(String source) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(source.getBytes("utf-8"));
        return byte2hex(bytes);
    }

    static String signPWD(loginname, String password) {
        //指定sha1算法
        MessageDigest digest = MessageDigest.getInstance("SHA-1")
        //获取字节数组
        def bytes = digest.digest(password.getBytes("utf-8"))
        // Create Hex String
        StringBuffer hexString = new StringBuffer()
        // 字节数组转换为 十六进制 数
        for (int i = 0; i < bytes.length; i++) {
            String shaHex = Integer.toHexString(bytes[i] & 0xFF)
            if (shaHex.length() < 2) {
                hexString.append(0)
            }
            hexString.append(shaHex)
        }
        return hexString.toString()
    }

    private static String byte2hex(byte[] bytes) {
        //type
        StringBuilder sign = new StringBuilder()
        for (int i = 0; i < bytes.length; ++i) {
            String hex = Integer.toHexString(bytes[i] & 255)
            if (hex.length() == 1) {
                sign.append("0")
            }
            sign.append(hex.toUpperCase())
        }

        return sign.toString();
    }

    static isNull(def x) {
        if (x == null || (x instanceof String && x.trim() == ''))
            return true
        return false;
    }

    static isNotNull(def x) {
        return !isNull(x)
    }

    static transToPercent(def num, int pointBefore = 3, int pointBehind = 1, int fractionNum = 1) {
        if (num == null) {
            num = 0
        }
        NumberFormat percent = NumberFormat.getPercentInstance()
        percent.setMaximumIntegerDigits(pointBefore)
        percent.setMaximumFractionDigits(pointBehind)
        percent.setMinimumFractionDigits(fractionNum)
        return percent.format(num)
    }

    static String getProjectPath(Class aClass) {


        java.net.URL url = aClass.getProtectionDomain().getCodeSource().getLocation();

        String filePath = null;

        try {
            filePath = java.net.URLDecoder.decode(url.getPath(), "utf-8").replace("file:", "").replace("jar:", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (filePath.lastIndexOf('/') == filePath.length() - 1) {
            filePath = filePath.substring(0, filePath.length() - 1)
        }

        if (filePath.endsWith(".jar") || filePath.endsWith(".jar!")) {
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }

        java.io.File file = new java.io.File(filePath);

        filePath = file.getAbsolutePath();

        return filePath;

    }

    static String buildID() {
        return UUID.randomUUID().toString()
    }

    static String dist() {

        def addr = "/seed/dist"
        if (isDebug) {
//            addr = "D:\\workspace\\brush\\server-core\\src\\main\\groovy\\dist"
//            addr = "D:\\brush\\server-core\\src\\main\\groovy\\dist"

            //addr = "E:\\project\\brush\\brush\\server-core\\src\\main\\groovy\\dist"
            addr = "D:\\work\\starweb\\dist"
        }
        return addr
    }
}
