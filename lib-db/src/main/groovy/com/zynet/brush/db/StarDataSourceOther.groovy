package com.zynet.brush.db

import com.alibaba.druid.pool.DruidDataSource
import com.zynet.brush.tool.CommonTool
import io.vertx.core.logging.LoggerFactory

/**
 * Created by liurui on 2017/3/18.
 */
@Singleton(strict = false)
class StarDataSourceOther extends DruidDataSource {

    static log = LoggerFactory.getLogger(this.class)

    String dbName = 'other'
    String dbServer = '47.104.5.42:3306'
    String dbUser = "zydl"
    String dbPassword = "zydlpwd741"
    String dbType = "mysql"

    private StarDataSourceOther() {
        if (CommonTool.isDebug) {
            dbServer = '192.168.1.240:3306'
            dbUser = 'root'
            dbPassword = 'root'
//            dbServer = "47.104.5.42:3306"
//            dbUser = "zydl"
//            dbPassword = "zydlpwd741"
        }

        log.info("$dbName datasource init.")
        def dds = this

        dds.url = "jdbc:mysql://$dbServer/$dbName?useSSL=false&useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"
        dds.driverClassName = "com.mysql.cj.jdbc.Driver"
        dds.initialSize = 1
        dds.maxActive = 30
        dds.minIdle = 1
        dds.poolPreparedStatements = true
        dds.username = dbUser
        dds.password = dbPassword
        dds.validationQuery = "SELECT 1"
        dds.testWhileIdle = true
        dds.testOnBorrow = false
        dds.testOnReturn = false
        //配置获取连接等待超时的时间
        dds.maxWait = 60000
        //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        dds.timeBetweenEvictionRunsMillis = 60000
        //配置一个连接在池中最小生存的时间，单位是毫秒
        dds.minEvictableIdleTimeMillis = 300000
        //配置监控统计拦截的filters
//        dds.filters = "stat"
//        //对于建立连接过长的连接强制关闭
//        dds.removeAbandoned = true
//        //如果连接建立时间超过了30分钟，则强制将其关闭
//        dds.removeAbandonedTimeout = 1800
//        //将当前关闭动作记录到日志
//        dds.logAbandoned = true

        if (CommonTool.isDebug) {
//            dds.initialSize = 1
//            dds.maxActive = 3
//            dds.minIdle = 1
        }


    }
}
