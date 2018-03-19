import com.aruistar.vertxplus.SignGateway
import com.zynet.brush.core.RouterCenter
import com.zynet.brush.tool.CommonTool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.AruisRouter
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler

/**
 * Created by  on 16/12/01.
 */
class CoreMain extends AbstractVerticle {

    def defaultPort = 7070
    static def log = LoggerFactory.getLogger(this.class)

    static void main(String[] args) {
        Vertx.vertx().deployVerticle(CoreMain.newInstance())
    }

    @Override
    void start() throws Exception {
        int port = System.getProperty('port') ? Integer.parseInt(System.getProperty('port')) : defaultPort

        //返回值压缩
        def options = [
                compressionSupported: true
        ]

        def server = vertx.createHttpServer(options)

        def router = AruisRouter.router(vertx)

        new SignGateway(port, 'core', vertx, router).connect()

        String addr = CommonTool.dist()

        println(addr)

        router.route().handler(BodyHandler.create())

        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.OPTIONS).allowedHeader('Content-Type'))

        //测试用，不用登陆，添加msid参数
//        if (CommonTool.isDebug) {
//            router.route("/core/*").handler({ ctx ->
//                ctx.request().params().putAt("msid", "55")
//                ctx.next()
//            })
//        }

        //从这里路由到其他业务
        RouterCenter.centerRouter(router)

        def staticHandler = StaticHandler.create()
        staticHandler.setAllowRootFileSystemAccess(true)
        staticHandler.setWebRoot(addr)
        staticHandler.setCachingEnabled(true)
        staticHandler.setMaxAgeSeconds(86400 * 1)

        router.route().handler(staticHandler)

        server.requestHandler(router.&accept).listen(port, { ar ->
            if (ar.succeeded()) {
                log.info('服务启动成功。')
            } else {
                log.info(ar.cause().message)
            }
        })

        if (!CommonTool.isDebug) {
            vertx.setPeriodic(60000 * 5, {
                //指定路径
                def file = new File("/seed/core/out.log")
                if (file.length() > 31457280) {
                    file.write("")
                }
            })
        }
    }
}