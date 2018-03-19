package com.zynet.brush.core

import io.vertx.ext.web.AruisRouter

/**
 * Created by liurui on 2016/12/8.
 */
class RouterFactory {

    AruisRouter router

    RouterFactory(AruisRouter router) {
        this.router = router
    }

    RouterBasic build(Class aClass) {

        RouterBasic routerBasic = aClass.newInstance()
        routerBasic.router = router
        routerBasic.init()

        return routerBasic
    }
}
