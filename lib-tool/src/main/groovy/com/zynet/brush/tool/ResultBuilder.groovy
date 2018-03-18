package com.zynet.brush.tool

import groovy.json.JsonBuilder

import java.sql.Array
import java.sql.Timestamp

/**
 * Created by hello on 2016/11/30.
 */
class ResultBuilder {
    int status

    void setDatas(datas) {
        def changeSQLArray = { map ->
            def _swap = [:]
            map.each { k, v ->
                if (v instanceof Array) {
                    def arr = v.getArray()
                    _swap[k] = buildType == 'xml' ? arr.join(',') : arr
                }
                if (v instanceof Timestamp) {
                    _swap[k] = v.format('yyyy-MM-dd HH:mm:ss')
                }
                if (v instanceof BigDecimal) {
                    _swap[k] = v.setScale(2, BigDecimal.ROUND_HALF_UP)
                }
            }
            if (_swap.size() > 0) {
                _swap.each { k, v ->
                    map[k] = v
                }
            }
        }

        if (datas instanceof Map) {
            changeSQLArray(datas)
        } else if (datas instanceof List) {
            datas.each {
                changeSQLArray(it)
            }
        }

        this._datas = datas
    }

    def getDatas() {
        return _datas
    }
    def _datas = []
    String content
    Boolean b

    String getBuildType() {
        return _buildType
    }

    void setBuildType(String buildType) {
        this._buildType = buildType
        if (datas) {
            datas = datas
        }
    }

    String _buildType = 'json'

    ResultBuilder() {
    }

    ResultBuilder(String buildType) {
        this.buildType = buildType == 'json' ? "json" : "xml"
    }

    ResultBuilder(int status, Map data, String content, boolean bl) {
        this.status = status
        this.content = content
        this.datas = [data]
        this.b = bl
    }

    ResultBuilder(int status, List datas, String content, boolean bl) {
        this.status = status
        this.content = content
        this.datas = datas
        this.b = bl
    }

    ResultBuilder(List datas, def page, def pagesize, int countNum, String buildType = 'json') {
        this.buildType = buildType
        this.status = 1
        this.content = "${Math.ceil(countNum / (pagesize ? pagesize.toInteger() : countNum)).toInteger()},${page ? page : 1},${countNum}"
        this.datas = datas
        this.b = true
    }

    String result(def data) {
        this.status = 1
        if (data instanceof String)
            this.content = data
        else if (data instanceof Boolean)
            this.b = data
        else if (data instanceof Map)
            this.datas = [data]
        else
            this.datas = data

        return this.toString()
    }

    String error(String msg) {
        this.status = 0
        this.content = msg

        return this.toString()
    }

    String toXML() {
        def xmlDocument = new groovy.xml.StreamingMarkupBuilder().bind {
            ResultModel {
                data() {
                    this.datas.each { map ->
                        row() {
                            map.each { key, value ->
//                                if (value instanceof Timestamp) {
//                                    value = value.format('yyyy-MM-dd HH:mm:ss')
//                                }
//                                if (value instanceof BigDecimal) {
//                                    value = value.setScale(2, BigDecimal.ROUND_HALF_UP)
//                                }
                                "$key"(value)
                            }
                        }
                    }
                }
                state(this.status)
                string(this.content)
                bool(this.b)
            }
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + xmlDocument
    }

    String toJSON() {
        return new JsonBuilder(
                [
                        state : status,
                        data  : datas,
                        string: content,
                        bool  : b
                ]
        ).toString()
    }


    @Override
    String toString() {
        return buildType == 'xml' ? this.toXML() : this.toJSON()
    }
}
