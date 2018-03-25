package com.zynet.brush.core

/**
 * Created by Lzx on 2017/12/26.
 */
class RouterUser extends RouterBasic {
    @Override
    def init() {
        //查看购买帐号
        queryUsers("/api/core/users/query")
    }

    def queryUsers(String url) {
        router.get(url).dbBlockingHandler({ routingContext ->
            def result = routingContext.db.rows("SELECT * from users limit 1")
            routingContext.response().end(result.toString())
        })
    }
}
