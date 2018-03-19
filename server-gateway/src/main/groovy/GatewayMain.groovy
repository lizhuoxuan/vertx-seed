import com.zynet.brush.db.DBFactory
import com.zynet.brush.tool.CommonTool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import redis.clients.jedis.Jedis

/**
 * Created by hello on 2016/11/30.
 */
class GatewayMain extends AbstractVerticle {

    def defaultPort = 80
    def defaultInPort = 19090

    def log = LoggerFactory.getLogger(this.class)
    def hosts = [:]

    Jedis jedis = null

    static void main(String[] args) {
        Vertx.vertx().deployVerticle(GatewayMain.newInstance())
    }

    @Override
    void start() throws Exception {
        jedis = DBFactory.redis()
        testHosts()
        List.metaClass.random = {
            int size = delegate?.size()
            if (size > 0) {
                if (size == 1)
                    return delegate[0]
                else
                    return delegate[new Random().nextInt(size)]
            } else {
                return null
            }
        }

        def port = System.getProperty('port') ? Integer.parseInt(System.getProperty('port')) : defaultPort
        def inport = System.getProperty('inport') ? Integer.parseInt(System.getProperty('inport')) : defaultInPort

        HttpServer server = vertx.createHttpServer()

        def router = Router.router(vertx)

        router.route("/favicon.ico").handler({ ctx ->
            ctx.response().sendFile("favicon.ico")
        })

        router.route("/api/*").handler(this.&apiHandler)

        def staticHandler = StaticHandler.create()
        staticHandler.setAllowRootFileSystemAccess(true)
        staticHandler.setWebRoot(addr())
        staticHandler.setCachingEnabled(true)
        staticHandler.setMaxAgeSeconds(86400 * 7) //7 days

        router.route("/*").handler(staticHandler)


        server.requestHandler(router.&accept).listen(port, { ar ->
            if (ar.succeeded()) {
                log.info("服务启动成功, @$port")
            }
        })

        startHost(inport)
    }

    @Override
    void stop() throws Exception {
        try {
            jedis.close()
        } catch (e) {

        }
        super.stop()
    }

    def getHostClient(key) {
        def list = hosts."$key"

        if (list == null || list.size() == 0) {
            throw new Exception('hosts zero...')
        }

        def host = list.random()
        return host.client
    }

    def testHosts() {
        vertx.setPeriodic(1 * 1000, {
            def localHost = vertx.sharedData().getLocalMap("hosts")

            def types = localHost.get('types')

            types.each { type ->
                def hosts = localHost.get(type)
                hosts.each { host ->
                    testHost(type, host.ip, host.port)
                }
            }
        })
    }

    def testHost(type, ip, port) {
        if (hosts."$type" == null)
            hosts."$type" = []

        def clientMap = hosts."$type".find { it.ip == ip && it.port == port }

        def client

        def suc = {
            def _map = hosts."$type".find { it.ip == ip && it.port == port }
            if (_map == null) {
                hosts."$type" << [
                        ip    : ip,
                        port  : port,
                        client: client
                ]

                log.info("type :$type,ip: $ip,port: $port connected")
            }
        }

        def err = {
            def _map = hosts."$type".find { it.ip == ip && it.port == port }
            if (_map) {
                hosts."$type".remove(_map)
            }

            def _localList = vertx.sharedData().getLocalMap("hosts").get(type)
            def _lmap = _localList.find { it.ip == ip && it.port == port }
            if (_lmap) {
                _localList.remove(_lmap)
                vertx.sharedData().getLocalMap("hosts").put(type, _localList)
            }

            log.info("type :$type,ip: $ip,port: $port disconnected")
        }

        if (clientMap) {
            client = clientMap.client
        } else {
            def option = [
                    defaultHost: ip,
                    defaultPort: port,
                    keepAlive  : true
            ]
            client = vertx.createHttpClient(option)
        }

        def time = vertx.setTimer(3 * 1000, {
            err()
        })

        client.getNow('/areyouok', { res ->
            res.bodyHandler({ body ->
                if (body.toString() == 'ok') {
                    suc()
                    vertx.cancelTimer(time)
                } else {
                    err()
                }
            })

            if (res.statusCode() != 200) {
                err()
            }

        })


    }

    def proxy(request, response, newclient, headerPlus, uri) {
        HttpClientRequest request2 = newclient.request(request.method(), uri, { response2 ->
            response.headers().setAll(response2.headers())

            response.statusCode = response2.statusCode()
            response.chunked = true

            response2.handler({ data ->
                response.write(data);
            })

            response2.endHandler({
                response.end()
            })

        })

        request2.chunked = true

        request2.headers().setAll(request.headers())
        headerPlus?.each { k, v ->
            request2.headers().add(k, v)
        }

        request.handler({ data ->
            request2.write(data)
        })

        request.endHandler({
            request2.end()
        })
    }

    def startHost(int inport) {


        def server = vertx.createHttpServer()

        def router = Router.router(vertx)

        router.route("/iamhere").handler({ routingContext ->

            def request = routingContext.request()

            def type = request.getParam("type")
            def ip = request.remoteAddress().host()
            def port = request.getParam("port")?.toInteger()

            def hosts = vertx.sharedData().getLocalMap("hosts")

            def hostList = hosts.get(type)
            if (hostList == null) {
                hostList = []
            }

            def typeNames = hosts.get('types')
            if (typeNames == null)
                typeNames = []

            if (typeNames.find { it == type } == null) {
                typeNames << type
                hosts.put('types', typeNames)
            }

            if (hostList.findAll {
                it.ip == ip && it.port == port
            }.size() == 0) {
                hostList << [
                        ip  : ip,
                        port: port
                ]

                hosts.put(type, hostList)
            }

            def response = routingContext.response()
            response.end('welcome')
        })

        server.requestHandler(router.&accept).listen(inport, { ar ->
            if (ar.succeeded()) {
                log.info("内连服务启动成功, @$inport")
            }
        })
    }

    def addr() {
        if (CommonTool.isDebug) {
            return "d:/work/seed/dist"
        }
        return "/seed/dist"
    }

    def apiHandler(RoutingContext ctx) {
        HttpServerRequest request = ctx.request()
        HttpServerResponse response = request.response()
        def err = {
            response.statusCode = 504
            response.end()
        }

        def uri = request.uri()
        def uriArr = uri.split('/')

        if (uriArr.size() > 2
                && vertx.sharedData().getLocalMap("hosts").get(uriArr[2])?.size() > 0) {
            def client = getHostClient(uriArr[2])

            def headerPlus = [:]
            if (CommonTool.isDebug) {
                log.info("call :$uri")
            }

            String ip = request.remoteAddress().host()

            if (uri.indexOf('/api/core/across') == 0 || uri.indexOf('/api/auth/login') == 0 ) {

            } else {//验证session里面的token
                def headers = request.headers()
                def token = headers.get('Authorization')
                if (!token) {
                    token = headers.get('Authorization2')
                }

                if (token == null && headers.contains("Cookie") && headers.get("Cookie").indexOf("zynet.session") > -1) {
                    token = headers.get("Cookie").split("zynet.session=")[1].split(";")[0]
                }

                if (token) {
                    def user
                    if (jedis.exists(token)) {
                        user = jedis.get(token)
                    }

                    if (user) {
                        if (request.query()?.indexOf("msid=") > 0) { //替换所有msid参数
                            uri = uri.replace("msid=", "superstar=")
                        }

                        if (request.query() == null)
                            uri += '?'

                        uri += "&msid=${user}"
                    } else {
                        err()
                        return
                    }
                } else {
                    err()
                    return
                }
            }

            proxy(request, response, client, headerPlus, uri)

        } else {//请求的url路径不符合要求
            err()
        }
    }
}
