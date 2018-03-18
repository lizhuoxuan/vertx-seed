package com.aruistar.vertx.plus;

import com.aruistar.vertx.plus.impl.DBHandlerImpl;
import io.vertx.core.Handler;

/**
 * Created by liurui on 2017/3/17.
 */
public interface DBHandler extends Handler<AruisRoutingContext> {

    static DBHandlerImpl create() {
        return new DBHandlerImpl();
    }
}
