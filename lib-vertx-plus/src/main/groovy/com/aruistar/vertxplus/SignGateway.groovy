package com.aruistar.vertxplus

import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router

/**
 * Created by liurui on 2016/12/6.
 */
class SignGateway {


    int myport
    String type
    static Vertx vertx

    def log = LoggerFactory.getLogger(this.class);

    SignGateway(def myport, String type, Vertx vertx, Router router) {
        this.myport = myport
        this.type = type
        this.vertx = vertx

        router.get('/areyouok').handler { routingContext ->
            routingContext.response().end("ok")
        }

    }

    def connect(String ip = Gateway.ip, int port = Gateway.signPort) {

        def client = vertx.createHttpClient([
                defaultHost: ip,
                defaultPort: port
        ])

        def _connect = {
            def request = client.get("/iamhere?port=$myport&type=$type", { res ->
                if (res.statusCode() == 200) {
//                    log.info("sign Gateway success.")
                } else {
                    log.info("sign Gateway error.")
                }
            })

            request.exceptionHandler({
            })

            request.end()
        }

        _connect()
        vertx.setPeriodic(1 * 1000, {
            _connect()
        })

    }

    static def getClient(String ip = Gateway.ip, int port = Gateway.port) {
        return vertx.createHttpClient([
                defaultHost: ip,
                defaultPort: port
        ])
    }
}
