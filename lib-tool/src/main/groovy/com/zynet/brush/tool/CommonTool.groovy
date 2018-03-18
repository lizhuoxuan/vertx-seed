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

    static String serverIp

    static def log = LoggerFactory.getLogger(this.class)

    static boolean isDebug = true

    static int loginTimeout = 60 * 2  //120分钟无操作，踢出用户
    static Vertx vertx

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

    static String md5ForLogin(String source) {
        MessageDigest md = MessageDigest.getInstance("MD5")
        byte[] bytes = md.digest(source.getBytes("utf-8"))
        StringBuilder sign = new StringBuilder()
        for (int i = 0; i < bytes.length; ++i) {
            String hex = Integer.toHexString(bytes[i] & 255)
            sign.append(hex.toUpperCase())
        }

        return sign.toString();
    }

    static String os() {
        Properties prop = System.getProperties()
        return prop.getProperty("os.name").toLowerCase()
    }

    static boolean isMac() {
        return os() == 'mac os x'
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

    static def addParam(String key, def value, Map map) {
        if (value) {
            map.put(key, value)
        }
    }

    static String getRandomChar(int digit = 5) {
        String randChar = "";
        digit.times {
            int index = (int) Math.round(Math.random() * 2);
            switch (index) {
                case 0://大写字符
                    randChar += String.valueOf((char) Math.round(Math.random() * 25 + 65));
                    break;
                case 1://小写字符
                    randChar += String.valueOf((char) Math.round(Math.random() * 25 + 97));
                    break;
                default://数字
                    randChar += String.valueOf(Math.round(Math.random() * 9));
                    break;
            }
        }
        return randChar;
    }

    static String getRandomChar2(String preSre, int digit = 5) {
        String randChar = ""
        digit.times {
            int index = (int) Math.round(Math.random() * 2)
            switch (index) {
                case 0://大写字符
                case 1:
                    randChar += String.valueOf((char) Math.round(Math.random() * 25 + 65))
                    break
                default://数字
                    randChar += String.valueOf(Math.round(Math.random() * 9))
                    break
            }
        }
        return preSre + "-" + randChar
    }

    static String buildID() {
        return UUID.randomUUID().toString()
    }

//    static def buildExcel(List rows) {
//        //创建HSSFWorkbook对象
//        HSSFWorkbook wb = new HSSFWorkbook();
//        //创建HSSFSheet对象
//        HSSFSheet sheet = wb.createSheet("sheet0");
//
//        //创建HSSFRow对象
//        rows.eachWithIndex { it, index ->
//            HSSFRow _firstrow
//            if (index == 0) {
//                _firstrow = sheet.createRow(index);
//            }
//            index++
//            HSSFRow _row = sheet.createRow(index);
//            def j = 0
//            it.each { k, v ->
//                if (_firstrow) {
//                    sheet.setColumnWidth(j, 256 * 25)
//                    _firstrow.createCell(j).setCellValue(k)
//                }
//                _row.createCell(j).setCellValue(v)
//                j++
//            }
//
//        }
//
//        //输出Excel文件
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        wb.write(bos);
//        def buf = Buffer.buffer()
//        bos.toByteArray().each {
//            buf.appendByte(it)
//        }
//
//        return buf
//    }

    static String getNextHourStr() {
        def hour = LocalTime.now().hour
        hour++
        if (hour < 10) {
            hour = "0" + hour
        }

        return "$hour:00:10"
    }

    static boolean isTrue(def str) {
        if (str == null)
            return false

        if (str == true || str == 1 || str == '1' || str.toUpperCase() == 'TRUE') {
            return true
        }
        return false
    }

    static String getRecommend(int count,def type){


        if(type =="威客"){
            def list = ('A'..'Z') - 'I' - 'O' -'Z'
            if (count <= 22976) {
                count=count+1
                def a = (count / 999).toInteger()
                def b = count % 999
                if (b == 0) {
                    a -= 1
                    b = 999
                }
                def c = list.get(a) + b
                return c
            } else if(count<229954 && count>=22977){
                if(count==22977){
                    def c = list.get(0) + 1000
                    return c
                }
                count=count+1
                count -= 22977
                def a = (count / 8999).toInteger()
                def b = count % 8999
                if (b == 0) {
                    a -= 1
                    b = 9999
                } else {
                    b += 999
                }
                def c = list.get(a) + b
                return c
            }else if(count>=229954 && count<2299931){
                if(count==229954){
                    def c = list.get(0) + 10000
                    return c
                }
                count=count+1
                count -=229954
                def a=(count/89999).toInteger()
                def b=count % 89999
                if(b == 0){
                    a -=1
                    b=99999
                }else{
                    b +=9999
                }
                def c=list.get(a)+b
                return c
            }else if(count>=2299931 && count<22999908){
                if(count==2299931){
                    def c = list.get(0) + 100000
                    return c
                }
                count=count+1
                count -=2299931
                def a=(count/899999).toInteger()
                def b=count % 899999
                if(b == 0){
                    a -=1
                    b=999999
                }else{
                    b +=99999
                }
                def c=list.get(a)+b
                return c
            }
        }else if(type =="商家"){
            return "Z" + count+1
        }
    }

    static String dist() {

        def addr = "/starcloud/dist"
        if (isDebug) {
//            addr = "D:\\workspace\\brush\\server-core\\src\\main\\groovy\\dist"
//            addr = "D:\\brush\\server-core\\src\\main\\groovy\\dist"

            //addr = "E:\\project\\brush\\brush\\server-core\\src\\main\\groovy\\dist"
            addr = "D:\\work\\starweb\\dist"
            if (isMac())
                addr = "/Users/hello/develop/workspace/brush/server-core/src/main/groovy/dist/"
        }
        return addr
    }
}
