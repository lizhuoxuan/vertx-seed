import com.aruistar.vertxplus.SignGateway
import com.zynet.brush.db.DBFactory
import com.zynet.brush.tool.CommonTool
import com.zynet.brush.tool.ResultBuilder
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.AruisRouter
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import redis.clients.jedis.Jedis

/**
 * Created by liurui on 16/5/28.
 */
class AuthMain extends AbstractVerticle {

    def defaultPort = 6060

    static void main(String[] args) {
        Vertx.vertx().deployVerticle(AuthMain.newInstance())
    }

    @Override
    void start() throws Exception {

        def port = System.getProperty('port') ? Integer.parseInt(System.getProperty('port')) : defaultPort


        def log = LoggerFactory.getLogger(this.class);

        def options = [
                compressionSupported: true
        ]

        def server = vertx.createHttpServer(options)

        def router = AruisRouter.router(vertx)

        new SignGateway(port, 'auth', vertx, router).connect()

        router.route().handler(BodyHandler.create())

        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.OPTIONS).allowedHeader("content-type"))

        router.get('/api/auth/logout').handler({ routingContext ->
            def response = routingContext.response()
            def request = routingContext.request()

            def responseType = request.getParam("responseType") == 'json' ? 'json' : "xml"; response.putHeader("content-type", "application/$responseType");
            def userid = request.getParam("msid")
            if (userid) {
                Jedis jedis = DBFactory.redis()

                def oldToken = jedis.get(userid)
                if (oldToken) {
                    jedis.del(oldToken)
                    jedis.del(userid)
                }

                jedis.close()
            }
            response.end(new ResultBuilder(responseType).result(true))
        })

        // For test
        router.get("/api/auth/login").dbBlockingHandler({ routingContext ->
            def response = routingContext.response()
            def username = routingContext.request().getParam("username")
            def password = routingContext.request().getParam("password")
            def token_create = CommonTool.buildID()
            Jedis redis = DBFactory.redis()
//            def user = routingContext.db.firstRow("SELECT * FROM manager WHERE v_username=? AND v_pwd=? ", [username, password])
            def user = [id: "1"]
            if (user) {
                def oldToken = redis.get(user.id.toString())
                if (oldToken) {
                    redis.del(oldToken)

                    //用以暂时关闭用户唯一在线功能,注释掉则准许一个用户在线一次
////                        token_create = oldToken
                }
                response.putHeader('Set-Cookie', "zynet.session=$token_create; Path=/; HTTPOnly")

                redis.set(token_create, user.id.toString())
                redis.expire(token_create, CommonTool.loginTimeout * 60)
                redis.set(user.id.toString(), token_create)
                redis.expire(user.id.toString(), 60 * 60 * 12)
                response.end(new ResultBuilder().result([user: user]))
            } else {
                response.end(new ResultBuilder().error("用户名或者密码错误！"))
            }

        })

        router.post("/api/auth/login").dbBlockingHandler({ routingContext ->
            if (routingContext?.bodyAsJson != null && routingContext?.bodyAsJson != "") {
                def body = routingContext.bodyAsJson
                def response = routingContext.response()
                def token_create = routingContext.request().getHeader('token')
                def userName = body?.userName
                def password = body.password
                def imgCode = body.imgCode
                def type = body.type
                def db = routingContext.db
                def ip = routingContext.request().remoteAddress().host()
                if (CommonTool.isNull(userName) || CommonTool.isNull(password) || CommonTool.isNull(imgCode) ||
                        CommonTool.isNull(type)) {
                    response.end(new ResultBuilder().error("登录信息不全！"))
                    return
                }
                Jedis redis = DBFactory.redis()
                try {
                    if (!redis.exists(ip + "code" + type)) {
                        response.end(new ResultBuilder().error("验证码已过期！"))
                        return
                    } else if (!redis.get(ip + "code" + type).equalsIgnoreCase(imgCode)) {
                        redis.del(ip + "code" + type)
                        response.end(new ResultBuilder().error("验证码输入错误！"))
                        return
                    }
                    password = CommonTool.signPWD("", password)
                    def user = db.firstRow("SELECT * FROM manager WHERE v_username=? AND v_pwd=? ", [userName, password])
                    if (user) {
                        def oldToken = redis.get(user.n_id.toString())
                        if (oldToken) {
                            redis.del(oldToken)

                            //用以暂时关闭用户唯一在线功能,注释掉则准许一个用户在线一次
////                        token_create = oldToken
                        }
                        response.putHeader('Set-Cookie', "zynet.session=$token_create; Path=/; HTTPOnly")

                        redis.set(token_create, user.n_id.toString())
                        redis.expire(token_create, CommonTool.loginTimeout * 60)
                        redis.set(user.n_id.toString(), token_create)
                        redis.expire(user.n_id.toString(), 60 * 60 * 12)
                        response.end(new ResultBuilder().result([user: user]))
                    } else {
                        redis.del(ip + "code" + type)
                        response.end(new ResultBuilder().error("用户名或者密码错误！"))
                    }

                } catch (Exception e) {
                    log.info(e.message)
                    redis.del(ip + "code" + type)
                    response.end(new ResultBuilder().error("错误"))
                } finally {
                    redis.close()
                }
            } else {
                routingContext.response().end(new ResultBuilder("登录信息不全！").toJSON())
            }
        })

        server.requestHandler(router.&accept).listen(defaultPort, { ar ->
            if (ar.succeeded()) {
                log.info('服务启动成功。')
            }
        })

    }

}
