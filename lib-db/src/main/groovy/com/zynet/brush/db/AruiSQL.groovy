package com.zynet.brush.db

import com.zynet.brush.tool.CommonTool
import groovy.json.JsonBuilder
import groovy.sql.Sql
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
//import org.postgresql.util.PGobject

import javax.sql.DataSource
import java.sql.Array
import java.sql.Connection
import java.sql.Timestamp
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by liurui on 2017/3/18.
 */
class AruiSQL extends Sql {

    static _log = LoggerFactory.getLogger(this.class)

    static dbTables = [:]  //缓存数据库表信息

    int dbHash //连接池hashcode

    static cacheTablesMinute = 30

    Vertx vertx

    static {
//        if (CommonTool.isDebug) {
//        def logger = Logger.getLogger('com.zynet.brush.db.AruiSQL')
            LOG.level = Level.FINE
            LOG.addHandler(new ConsoleHandler(level: Level.FINE))
            LOG.addHandler(new FileHandler(level: Level.FINE))

//        }

    }

    AruiSQL(DataSource dataSource) {
        super(dataSource)
        dbHash = dataSource.hashCode()
    }

 
    //当在事务里调用，需要将事务里的connnection作为参数传入，否则报错
    def saveList(list, String table, String specialType = '', List keyList = null, batchSize = 100, Connection conn = null) {
        def sql
        def values
        def keys
        def type

        (sql, values, keys, type) = getSaveItemSql(list[0], table, specialType)

        if (keyList)
            keys = keyList
        this.withBatch(batchSize, sql, { st ->
            list.each { item ->
                if (item.id == null) item.id = CommonTool.buildID()
                def map = [:]
                item.each { k, v ->
                    map[k] = judgeType(k, v, conn)
                }
                if (type == 'insert')
                    st.addBatch(keys.collect { map[it] })
                if (type == 'update')
                    st.addBatch(map)
            }
        })


    }

    def getSaveItemSql(Map item, String table, String specialType = '') {
        String _id = item.id

        if (CommonTool.isNull(_id)) {
            item.id = CommonTool.buildID()
            _id = item.id

            specialType = 'insert'
        }



        if (specialType == '') {
            if (getItemByID(_id, table))
                specialType = 'update'
            else
                specialType = 'insert'
        }

        if (specialType == 'insert') {//insert
            String _sql = "insert into $table ( "
            def _param = []
            def keys = []
            item.each { it ->
                if (tables[table].contains(it.key)) {
                    keys << it.key
                    _sql += "$it.key,"
                    _param << it.value
                }
            }
            _sql = _sql.substring(0, _sql.length() - 1)
            _sql += " ) values ("
            _param.size().times {
                _sql += "?,"
            }
            _sql = _sql.substring(0, _sql.length() - 1)
            _sql += ")"
            return [_sql, _param, keys, specialType]
        } else if (specialType == 'update') {//update
            item.id = _id
            String _sql = "update $table set "
            def _param = []
            def keys = []
            item.each { it ->
                if (it.key != 'id' && tables[table].contains(it.key)) {
                    keys << it.key
                    _sql += "$it.key = :$it.key ,"
                    _param << it.value
                }
            }
            _sql = _sql.substring(0, _sql.length() - 1)
            _sql += " where id = :id"
            _param << _id
            keys << "id"
            return [_sql, item, keys, specialType]
        }
    }

    def saveItem(Map item, String table, String specialType = '') {
        try {
            String _id = item.id

            if (CommonTool.isNull(_id)) {
                item.id = CommonTool.buildID()
                _id = item.id

                specialType = 'insert'
            }

            if (specialType == '') {
                if (getItemByID(_id, table))
                    specialType = 'update'
                else
                    specialType = 'insert'
            }

            if (specialType == 'insert') {//insert
                String _sql = "insert into $table ( "
                def _param = []
                item.each { it ->
                    if (tables[table].contains(it.key)) {
                        _sql += "$it.key,"
                        it.value = judgeType(it.key, it.value)
                        _param << it.value
                    }
                }
                _sql = _sql.substring(0, _sql.length() - 1)
                _sql += " ) values ("
                _param.size().times {
                    _sql += "?,"
                }
                _sql = _sql.substring(0, _sql.length() - 1)
                _sql += ")"
                this.executeInsert(_sql, _param)
                return true
            } else if (specialType == 'update') {//update
                item.id = _id
                String _sql = "update $table set "
                def _param = []
                item.each { it ->
                    if (it.key != 'id' && tables[table].contains(it.key)) {
                        _sql += "$it.key = :$it.key ,"
                        it.value = judgeType(it.key, it.value)
                        _param << it.value
                    }
                }
                _sql = _sql.substring(0, _sql.length() - 1)
                _sql += " where id = :id"
                _param << _id
                this.executeUpdate(_sql, item)
                return true
            }
        } catch (e) {
            e.printStackTrace()
            return false
        }

    }

    def getItemByID(id, table) {
        return this.firstRow("SELECT * from $table WHERE id = ?".toString(), [id])
    }

    def getItemByKey(key, value, tablename) {
        return this.firstRow("SELECT * from $tablename WHERE $key = ?".toString(), [value])
    }

    def getItems(tablename) {
        return this.rows("SELECT * FROM $tablename".toString())
    }

    boolean isExist(key, value, key2, value2, key3, value3, tablename) {
        if (CommonTool.isNotNull(this.firstRow("SELECT * from $tablename WHERE $key = ? and $key2 = ? and $key3 = ?".toString(), [value, value2, value3]))) {
            return true
        } else {
            return false
        }
    }

    boolean deleteItem(id, tablename) {
        try {
            this.execute("DELETE FROM $tablename WHERE id = ?".toString(), [id])
        } catch (e) {
            e.printStackTrace()
            return false
        }
        return true
    }

    def dynQuery(String sql, Map param, String postfix = "", page = null, pageSize = null) {
        def _where, _params
        (_where, _params) = buildWhere(param)
        String pageStr = ""
        if (page && pageSize) {
            pageStr = " limit ${pageSize} offset ${(page.toString().toInteger() - 1) * pageSize.toString().toInteger()}"
        }
        return this.rows("${sql}  where  ${_where}  ${postfix} ${pageStr}".toString(), _params)
    }

    def judgeType(def key, def value, def conn = null) {
        if (value == null) {
            return value
        }
        if (key.toString().startsWith('v_')) {
            return value.toString().replaceAll("'" , "\\\\'")
        }
        if ((!(value instanceof Timestamp)) && key.toString().startsWith('t_') || key.toString().startsWith('d_')) {
            if (value.toString().size() > 10) {
                return Timestamp.valueOf(value.toString())
            } else {
                return Timestamp.valueOf(value.toString() + " 00:00:00")
            }
        }
        if ((!(value instanceof Boolean)) && key.toString().startsWith('b_')) {
            return CommonTool.isTrue(value.toString())
        }
        if ((!(value instanceof BigDecimal)) && key.toString().startsWith('n_')) {
            return value.toString().toBigDecimal()
        }
        if ((!(value instanceof Integer)) && key.toString().startsWith('i_') || key == 'id_at_t_taskstatus') {
            if (value == '') value = 0
            return value.toString().toInteger()
        }
        if ((!(value instanceof Array)) && key.toString().startsWith('a_')) {
            if (value instanceof String) {
                value = value.split(',')
            }
            if (value instanceof ArrayList) {
                value = value as String[]
            }
            if (conn) {
                value = conn.createArrayOf("VARCHAR", value)
            } else {
                this.withTransaction { connn ->
                    value = connn.createArrayOf("VARCHAR", value)
                }
            }
            return value
        }


//        if ((!(value instanceof PGobject)) && key.toString().startsWith('j_')) {
//
//            PGobject jsonObject = new PGobject();
//            jsonObject.setType("json");
//            if (value instanceof String)
//                jsonObject.setValue(value)
//            else if (value instanceof Map)
//                jsonObject.setValue(new JsonBuilder(value).toString())
//            else
//                throw new Exception("json error.")
//
//            return jsonObject
//        }

        return value
    }

    def getTables() {
        if (dbTables.containsKey(dbHash)) {
            return dbTables[dbHash]
        } else {
            LOG.info("init database tables and columns")
            def tables = [:]


            this.eachRow("""
SELECT
  table_schema,
  table_name,
  array_agg(column_name::VARCHAR) AS colarray
FROM information_schema.columns cols
  INNER JOIN pg_stat_user_tables tabs ON tabs.schemaname = cols.table_schema AND tabs.relname = cols.table_name
GROUP BY table_schema, table_name
""", {
                tables[it.table_name] = it.colarray.getArray()

            })

            dbTables[dbHash] = tables

            if (vertx) {
                vertx.setTimer(cacheTablesMinute * 60 * 1000, {
                    dbTables.remove(dbHash)
                })
            }

            return tables
        }
    }

    static buildWhere(Map conditions) {
        String _where = ''
        def _params = []
        conditions.eachWithIndex { k, v, i ->
            if (v != null) {
                _where += " $k = ? "
                if (i < conditions.size()) {
                    _where += 'and '
                }
                _params << v
            }
        }
        if (_where.length() > 0) {
            _where = _where.substring(0, _where.length() - 4)
        } else {
            _where = ' 1=1 '
        }
        return [_where, _params]
    }

    def log(who, what, task = '') {
        saveItem([
                id_at_c_user: who?.toString(),
                v_summary   : what?.toString(),
                id_at_t_task: task?.toString()
        ], 'l_log', 'insert')
    }
}
