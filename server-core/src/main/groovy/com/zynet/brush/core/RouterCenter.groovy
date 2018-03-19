package com.zynet.brush.core

import com.zynet.brush.tool.CommonTool
import io.vertx.core.Vertx
import io.vertx.ext.web.AruisRouter

import java.util.concurrent.CopyOnWriteArrayList
import java.util.jar.JarFile

/**
 * Created by Administrator on 2016/6/30.
 */
class RouterCenter {

    static CopyOnWriteArrayList fileNames = []

    /**
     *               通过这个方法路由
     * @param router
     * @param url
     * @param vertx
     * @return
     */
    static def centerRouter(AruisRouter router) {

        RouterFactory routerFactory = new RouterFactory(router)

        //动态加载router
        def clazz = RouterBasic.class

        if (CommonTool.isDebug) {
            getClazzs()
            fileNames.unique()
            fileNames.each { fileName ->
                def clazzTarget = Class.forName(fileName)
                if (clazz.isAssignableFrom(clazzTarget)) {
                    routerFactory.build(clazzTarget)
                }
            }
        } else {
            def url = RouterBasic.class.getProtectionDomain().getCodeSource().getLocation()

            String filePath = null

            try {
                filePath = URLDecoder.decode(url.getPath(), "utf-8")
            } catch (Exception e) {
                e.printStackTrace()
            }
            //获取RouterBasic类的包
            def packagePath = RouterBasic.class.getPackage().name
            packagePath = packagePath.split('\\.')

            //jar文件路径
            JarFile jarFile = new JarFile(filePath)
            jarFile.entries().each {
                //筛选RouterBasic类目录下的所有继承RouterBasic类的类
                if ((it.name.contains(packagePath.join('\\')) || it.name.contains(packagePath.join('/'))) && !it.name.contains('$') && it.name.endsWith('class')) {
                    def fileName = (it.name - '.class').replaceAll("\\\\|/", '.')
                    def clazzTarget = Class.forName(fileName)
                    if (clazz.isAssignableFrom(clazzTarget)) {
                        routerFactory.build(clazzTarget)
                    }
                }
            }
        }
    }

    static def getClazzs() {
        def path = CommonTool.getProjectPath(RouterBasic.class)
        def packagePath = RouterBasic.class.getPackage().name
        packagePath = packagePath.replace(".", "/")
        path += "/"+packagePath
        def clazzs = new File(path)

        clazzs.eachFile { file ->
            if (!file.name.contains('$')) {
                getClassName(file)

            }
        }
    }

    static def getClassName(def file) {
        if (!file.name.contains('$')) {
            if (file.isDirectory()) {
                file.eachFile { file2 ->
                    getClassName(file2)
                }
            } else {
                fileNames << file.path.substring(file.path.indexOf('com'), file.path.lastIndexOf('.')).replaceAll("\\\\|/", '.')
            }
        }
    }
}
