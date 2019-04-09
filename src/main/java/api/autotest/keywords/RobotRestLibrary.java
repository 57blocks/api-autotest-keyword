package api.autotest.keywords;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywordOverload;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import api.autotest.rest.RestClientUtils;

@RobotKeywords
public class RobotRestLibrary {
    private final static Logger LOGGER = Logger.getLogger(RobotRestLibrary.class);
    private static ApplicationContext context;
    public static Map<String, List<Map<String, String>>> tableMap = new HashMap<String, List<Map<String, String>>>();

    private RestClientUtils restClientUtils;

    public RobotRestLibrary() {
        context = new ClassPathXmlApplicationContext("spring/bean-mappings.xml");
        restClientUtils = (RestClientUtils) context.getBean("restClientUtils");
        LOGGER.info("Init RobotRestLibrary class");
    }

    @RobotKeyword
    public Object encodeBase64(String value) {
        return new String(Base64.encodeBase64(value.getBytes()));
    }

    /**
     * Save a specific oauth token
     * @param value
     */
    @RobotKeyword
    public void saveToCache(Object value) {
        restClientUtils.saveToCache(value);
    }

    /**
     * Save oauth token to a special ${key} in one hour valid.
     * @param key
     * @param value
     */
    @RobotKeywordOverload
    public void saveToCache(String key, Object value) {
        restClientUtils.saveToCache(key, value,  60 * 60);
    }

    /**
     * Save oauth token to a special ${key} in ${timeToLive} valid.
     * @param key
     * @param value
     * @param timeToLive
     */
    @RobotKeywordOverload
    public void saveToCache(String key, Object value, long timeToLive) {
        restClientUtils.saveToCache(key, value, timeToLive);
    }

    /**
     * Save oauth token to a special ${key} in ${timetoLive} - ${cacheAllowance} valid
     * @param key
     * @param value
     * @param timeToLive
     * @param cacheAllowance
     */
    @RobotKeywordOverload
    public void saveToCache(String key, Object value, long timeToLive, long cacheAllowance) {
        restClientUtils.saveToCache(key, value, timeToLive, cacheAllowance);
    }

    /**
     * Get oauth token by given ${key} if token is valid
     * @param key
     * @return
     */
    @RobotKeyword
    public Object getFromCache(String key) {
        return restClientUtils.getFromCache(key);
    }

    /**
     * Get oauth token
     * @return
     */
    @RobotKeywordOverload
    public Object getFromCache() {
        return restClientUtils.getFromCache();
    }

    /**
     * Remove oauth token by given ${key}
     * @param key
     * @return
     */
    @RobotKeyword
    public boolean removeFromCache(String key) {
        return restClientUtils.removeFromCache(key);
    }

    @RobotKeyword
    public Object invokeService(String url) {
        return invokeService("GET", Collections.<String, String>emptyMap(), url);
    }

    @RobotKeywordOverload
    public Object invokeService(String action, String url) {
        return invokeService(action, Collections.<String, String>emptyMap(), url);
    }

    @RobotKeywordOverload
    public Object invokeService(String action, Map<String, String> requestHeaders, String url) {
        return invokeService(action, requestHeaders, url, null);
    }

    @RobotKeywordOverload
    public Object invokeService(String action, Map<String, String> requestHeaders, String url, String requestBody) {
        return restClientUtils.invoke(action, requestHeaders, url, requestBody);
    }
}
