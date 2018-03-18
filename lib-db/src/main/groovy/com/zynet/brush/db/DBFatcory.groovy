package com.zynet.brush.db

import com.zynet.brush.tool.CommonTool
import io.vertx.core.Vertx
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by liurui on 2017/3/14.
 */
class DBFactory {

    private static RS _redis

    static brush(Vertx vertx = null) {
        def sql = new AruiSQL(StarDataSource.instance)
        sql.vertx = vertx
        return sql
    }

    static other(Vertx vertx = null) {
        def sql = new AruiSQL(StarDataSourceOther.instance)
        sql.vertx = vertx
        return sql
    }

    static Jedis redis() {
        String rsHost = '127.0.0.1'
        int rsPort = 16379
        String rsPassword = 'brush008'

        if (CommonTool.isDebug) {
            rsHost = "127.0.0.1"
            rsPort = 6379
            rsPassword = null
            if (CommonTool.isMac()) {
                rsHost = "127.0.0.1"
            }
        }

        if (_redis == null)
            _redis = new RS(rsHost, rsPort, rsPassword)
        return _redis.db
    }
}


class RS {

    static rss = [:]

    String rsHost
    int rsPort
    String rsPassword
    int rsHash

    RS(String rsHost, int rsPort, String rsPassword) {
        this.rsHost = rsHost
        this.rsPort = rsPort
        this.rsPassword = rsPassword

        rsHash = (rsHost + rsPort + rsPassword).hashCode()
    }

    Jedis getDb() {
        if (rss.containsKey(rsHash))
            return rss[rsHash].getResource()
        else {
            JedisPool jpl = new JedisPool(new JedisPoolConfig(), rsHost, rsPort, 8000, rsPassword)
            rss[rsHash] = jpl
            return jpl.getResource()
        }
    }
}