package api.autotest.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import api.autotest.common.LoggerUtils;

public class OauthTokenCache {
    private final static Logger LOGGER = Logger.getLogger(OauthTokenCache.class);
    private static Object cacheObject = null;
    private static Map<String, Object[]> cache = new HashMap<String, Object[]>();

    public void put(String key, Object val, Long timeToLive, Long cacheAllowance) {
        LoggerUtils.info(LOGGER, String.format("Saving value for '%s' in cache for %d seconds.", key, timeToLive));
        timeToLive = System.currentTimeMillis() + timeToLive * 1000;
        cacheAllowance = cacheAllowance * 1000;
        if (key == null) {
            throw new RuntimeException("Key can't be null.");
        }
        cache.put(key, new Object[] { timeToLive, val, cacheAllowance });
        LoggerUtils.info(LOGGER, String.format("Save %s = %s for %d milli seconds.", key, cache.get(key), timeToLive));
    }

    public void put(Object val) {
        LoggerUtils.info(LOGGER, "Saving Object in cache");
        cacheObject = val;
    }

    public Object get(String key) {
        if(cache.containsKey(key)) {
            Long expires = (Long) cache.get(key)[0];
            Long cacheAllowance = (Long) cache.get(key)[2];
            LoggerUtils.info(LOGGER, "expires:                  : " + expires);
            LoggerUtils.info(LOGGER, "cacheAllowance            : " + cacheAllowance);
            LoggerUtils.info(LOGGER, "CurrentTimeMillis()       : " + System.currentTimeMillis());
            if((expires - cacheAllowance) - System.currentTimeMillis() > 0) {
                LoggerUtils.info(LOGGER, String.format("Returning value for %s from cache", key));
                return cache.get(key)[1];
            }
            else {
                LoggerUtils.info(LOGGER, String.format("Removing key %s from cache", key));
                remove(key);
            }
        }
        LoggerUtils.info(LOGGER, String.format("Returning value for %s as null from cache", key));
        return null;
    }

    public Object get() {
        LoggerUtils.info(LOGGER, "Get cache from CacheObject");
        return cacheObject;
    }

    public boolean remove(String key) {
        return removeAndGet(key) != null;
    }

    public Object removeAndGet(String key) {
        return cache.remove(key);
    }

}
