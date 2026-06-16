package com.sky.interceptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

public class DynamicDebugLogTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String s, Object[] objects, Throwable throwable) {
//        String debug = MDC.get("debug_log");
//        // 开启调试
//        if ("true".equals(debug)) {
//            return FilterReply.NEUTRAL; // 继续处理日志事件
//        }
//        // 只拒绝 DEBUG 和 TRACE
//        if (level == Level.DEBUG || level == Level.TRACE) {
//            return FilterReply.DENY;
//        }
        return FilterReply.NEUTRAL;
    }
}
