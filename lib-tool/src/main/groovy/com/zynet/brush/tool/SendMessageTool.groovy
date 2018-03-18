package com.zynet.brush.tool

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient

/**
 * Created by Lzx on 2016/12/6.
 */
class SendMessageTool {

    static def sendM(List message, List toWho, Vertx vertx, def modelId = 87701) {
        def messages = ""
        message.each {
            messages += "\"${it}\","
        }
        messages = messages.substring(0, messages.size() - 1)
        def toWhos = toWho.join(',')
        def date = new Date()
        def modelID = modelId
        def appID = "8a48b5515493a1b70154c84edafd35e7"
        def authToken = "67fddaa323424547b0cefd975b8f004f"
        def accountSid = "aaf98f8954939ed50154c7969c153803"
        def bodyParam = """{"appId":"${appID}","to":"${toWhos}","templateId":"${modelID}","datas":[${messages}]}"""

        println(bodyParam)
        println(bodyParam.size())
        //使用MD5加密（账户Id + 账户授权令牌 + 时间戳）
        def headers = [
                "Host"          : "app.cloopen.com:8883",
                "Accept"        : "application/json;",
                "Content-Type"  : "application/json;charset=utf-8;",
                "Content-Length": "${bodyParam.size()};",
                "Authorization" : "${accountSid}:${date.format("yyyyMMddHHmmss")}".bytes.encodeBase64().toString()
        ]
        println(headers)
        def SigParameter = CommonTool.md5(accountSid + authToken + date.format("yyyyMMddHHmmss")).toUpperCase()
        def url = "/2013-12-26/Accounts/${accountSid}/SMS/TemplateSMS?sig=${SigParameter}"
        println(url)
        def options = [
//                defaultHost      : 'app.cloopen.com',
//                keepAlive        : true,
//                ssl              : true,
//                verifyHost       : false,
//                defaultPort      : 8883,
                tryUseCompression: true
        ]
        WebClient webClient = WebClient.create(vertx, options)
        def req = webClient.postAbs("https://app.cloopen.com${url}")
        headers.each { k, v ->
            req.putHeader(k, v)
        }
        req.sendBuffer(Buffer.buffer(bodyParam), { ar ->
            webClient.close()
            if (ar.succeeded()) {
                println("sms success")
            } else {
                println("sms fail")
            }
        })
    }

//    static def sendM(List message, List toWho, def modelId = 87701) {
//        def messages = ""
//        message.each {
//            messages += "\"${it}\","
//        }
//        messages = messages.substring(0, messages.size() - 1)
//        def toWhos = toWho.join(',')
//        def date = new Date()
//        def modelID = modelId
//        def appID = "8a48b5515493a1b70154c84edafd35e7"
//        def authToken = "67fddaa323424547b0cefd975b8f004f"
//        def accountSid = "aaf98f8954939ed50154c7969c153803"
//        def bodyParam = """{"appId":"${appID}","to":"${toWhos}","templateId":"${modelID}","datas":[${messages}]}"""
//
//        def SigParameter = CommonTool.md5(accountSid + authToken + date.format("yyyyMMddHHmmss")).toUpperCase()
//
//        def http = new HTTPBuilder('https://app.cloopen.com:8883')
//        def res
//        http.encoderRegistry = new EncoderRegistry(charset: 'utf-8')
//        http.request(Method.POST, ContentType.TEXT) {
//            //设置url相关信息
//            uri.path = "/2013-12-26/Accounts/${accountSid}/SMS/TemplateSMS"
//            uri.query = [sig:SigParameter]
//            body = bodyParam
//            requestContentType = ContentType.URLENC
//            //设置请求头信息
//            headers = [
//                    "Host"          : "app.cloopen.com:8883",
//                    "Accept"        : "application/json;",
//                    "Content-Type"  : "application/json;charset=utf-8;",
//                    "Authorization" : "${accountSid}:${date.format("yyyyMMddHHmmss")}".bytes.encodeBase64().toString()
//            ]
//            //设置成功响应的处理闭包
//            response.success = { resp, reader ->
//                res = new JsonSlurper().parseText(reader.text.toString())
//                //[statusCode:000000, templateSMS:[dateCreated:20170205093930, smsMessageSid:7da417aea81f4c238bfcf294114546a3]]
//                println(res)
//            }
//            //根据响应状态码分别指定处理闭包
//            response.'404' = { println 'not found' }
//            //未根据响应码指定的失败处理闭包
//            response.failure = { println "Unexpected failure: ${resp.statusLine}" }
//        }
//        return res
//    }
}
