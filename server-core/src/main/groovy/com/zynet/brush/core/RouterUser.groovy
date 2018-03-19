package com.zynet.brush.core

import com.zynet.brush.tool.ResultBuilder

/**
 * Created by Lzx on 2017/12/26.
 */
class RouterUser extends RouterBasic {
    @Override
    def init() {
        //查看购买帐号
        router.get("/api/core/users/query").dbBlockingHandler(this.&queryUsers)
    }

    def queryUsers(def ctx) {
        def msid = ctx.request().getParam("msid")
        def users = ctx.db.rows("SELECT * from users", [msid])
        ctx.response().end(new ResultBuilder().result([user: users]))
    }
}
