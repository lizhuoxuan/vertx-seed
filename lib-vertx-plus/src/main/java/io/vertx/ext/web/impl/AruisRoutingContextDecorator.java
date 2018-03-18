package io.vertx.ext.web.impl;

import com.zynet.brush.db.AruiSQL;
import io.vertx.ext.web.AruisRoutingContext;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by liurui on 2017/3/16.
 */
public class AruisRoutingContextDecorator extends RoutingContextDecorator implements AruisRoutingContext {

    //我这里用的Sql，是groovy封装的数据库访问类，在这里替换成自己业务需要使用的数据源即可
    private AruiSQL db;

    AruisRoutingContextDecorator(Route currentRoute, RoutingContext context, AruiSQL db) {
        super(currentRoute, context);
        this.db = db;
    }

    @Override
    public AruiSQL getDb() {
        return db;
    }
}
