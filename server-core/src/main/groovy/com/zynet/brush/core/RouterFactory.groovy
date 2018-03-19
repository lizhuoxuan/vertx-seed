package com.zynet.brush.core

import io.vertx.core.Vertx
import io.vertx.ext.web.AruisRouter
import io.vertx.ext.web.client.WebClient

/**
 * Created by liurui on 2016/12/8.
 */
class RouterFactory {

    Vertx vertx
    AruisRouter router
    WebClient webClient

    RouterFactory(Vertx vertx, AruisRouter router, WebClient webClient) {
        this.vertx = vertx
        this.router = router
        this.webClient = webClient
    }

    RouterBasic build(Class aClass) {

        RouterBasic routerBasic = aClass.newInstance()
        routerBasic.router = router
        routerBasic.init()

        return routerBasic
    }
}
