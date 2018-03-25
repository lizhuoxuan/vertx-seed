package com.zynet.brush.core

import com.zynet.brush.db.DBFactory
import com.zynet.brush.tool.ResultBuilder

/**
 * Created by hello on 2017/1/11.
 */
class RouterAcross extends RouterBasic {

    @Override
    init() {
        //查看开通的城市
        queryUsers("/core/across/cities/query")

    }

    def queryUsers(String url) {
        router.get(url).handler({ routingContext ->
            def db = DBFactory.other()
            def cities = db.rows("SELECT v_servername from server;")
            db.close()
            routingContext.response().end(new ResultBuilder().result([cities: cities]))
        })
    }

}
