package com.zynet.brush.tool

import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Created by Administrator on 2016/6/28.
 */
class TimerTasks {
    def log = LoggerFactory.getLogger(this.class)

    static Vertx _vertx

    def _times = []

    long _delay

    long _initDelay

    long _setTimerId = -1

    long _setPeriodicId = -1

    static def timerIds = []

    TimerTasks(Vertx vertx = null, long delay = 24 * 60 * 60 * 1000) {
        if (vertx == null) {
            _vertx = Vertx.vertx()
        } else {
            _vertx = vertx
        }
        this._delay = delay
    }

    def setHandler(call) {

        if (_times.size() == 0) {
            _times.add("00:00:00")
        }

        _times.each { time ->

            _initDelay = getInitDelay(time)
            try {
                _setTimerId = _vertx.setTimer(_initDelay, {
                    //something to loop
                    _vertx.executeBlocking({ fu ->
                        call()
                        fu.complete()
                    }, false, {})
                    _setPeriodicId = _vertx.setPeriodic(_delay, {
                        //something to loop
                        _vertx.executeBlocking({ fu ->
                            call()
                            fu.complete()
                        }, false, {})
                    })
                    timerIds << _setTimerId
                    timerIds << _setPeriodicId
                    log.info("TimerID :      " + _setTimerId)
                    log.info("PeriodicID :    " + _setPeriodicId)
                })

            } catch (e) {
                e.printStackTrace()
                if (_setTimerId > -1) {
                    _vertx.cancelTimer(_setTimerId)
                }
                if (_setPeriodicId > -1) {
                    _vertx.cancelTimer(_setPeriodicId)
                }
            }

        }
        return timerIds
    }

    def setTimer(String... times) {
        String time = ""
        times.each { v ->
            def timeList = v.split(":")
            if (timeList.size() < 3) {
                v = "$v:00"
            }
            time = v
            _times << time
        }
        return this
    }

    static def stopTimer() {
        timerIds.each {
            if (it > -1) {
                _vertx.cancelTimer(it)
            }
        }
    }

    /**
     * 获取指定时间和当前时间相差的毫秒数
     * @param time "HH:mm:ss"
     * @return
     */
    static long getInitDelay(String time) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = dateFormat.parse(new Date().format('yyyy-MM-dd') + " " + time);
            long oneDay = 24 * 60 * 60 * 1000;
            long initDelay = curDate.getTime() - System.currentTimeMillis();
            initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
            return initDelay
        } catch (ParseException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
