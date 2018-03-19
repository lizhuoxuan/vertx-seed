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
        router.get(url).dbBlockingHandler({ rc ->
            def result = rc.db.rows("SELECT * from users limit 1")
            rc.response().end(result.toString())
        })
    }
}
