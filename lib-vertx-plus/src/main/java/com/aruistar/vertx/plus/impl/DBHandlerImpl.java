package com.aruistar.vertx.plus.impl;

import com.aruistar.vertx.plus.AruisRoutingContext;
import com.aruistar.vertx.plus.DBHandler;

/**
 * Created by liurui on 2017/3/17.
 */
public class DBHandlerImpl implements DBHandler {
    @Override
    public void handle(AruisRoutingContext event) {
        System.out.println("event = [" + event + "]");
    }
}
