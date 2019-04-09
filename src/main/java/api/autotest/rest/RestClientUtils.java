package api.autotest.rest;

import static api.autotest.rest.RestClient.CONTENT_TYPE;
import static api.autotest.rest.RestClient.CT_APPLICATION_OCTETSTREAM;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import api.autotest.common.LoggerUtils;

public class RestClientUtils {
    public enum Action {
        GET, POST, PUT, DELETE;
    }

    private final static Logger LOGGER = Logger.getLogger(RestClientUtils.class);
    public final static String REPLACE_WITH = " REPLACE WITH ";

    private RestClient restClient;

    private OauthTokenCache oauthTokenCache;

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void setOauthTokenCache(OauthTokenCache oauthTokenCache) {
        this.oauthTokenCache = oauthTokenCache;
    }

    public void saveToCache(String key, Object value, long timeToLive) {
        oauthTokenCache.put(key, value, timeToLive, 0L);
    }

    public void saveToCache(String key, Object value, long timeToLive, long cacheAllowance) {
        oauthTokenCache.put(key, value, timeToLive, cacheAllowance);
    }

    public void saveToCache(Object value) {
        oauthTokenCache.put(value);
    }

    public Object getFromCache(String key) {
        return oauthTokenCache.get(key);
    }

    public Object getFromCache() {
        return oauthTokenCache.get();
    }

    public boolean removeFromCache(String key) {
        return oauthTokenCache.remove(key);
    }
    public RestServiceResponse invoke(String action, Map<String, String> requestHeaders, String url,
                                      String requestBody) {
        Optional<Response> res;
        switch (Action.valueOf(action)) {
            case GET:
                res = restClient.get(url, requestHeaders);
                break;
            case POST:
                res = restClient.post(url, requestHeaders, requestBody);
                break;
            case PUT:
                res = restClient.put(url, requestHeaders, requestBody);
                break;
            case DELETE:
                res = restClient.delete(url, requestHeaders);
                break;
            default:
                res = Optional.empty();
        }
        if(res.isPresent()) {
            LOGGER.info("Starting building response");
            RestServiceResponse response = getRestServiceResponseObject(res.get());
            if(response.getHeaders().get(CONTENT_TYPE) != null && CT_APPLICATION_OCTETSTREAM.equalsIgnoreCase(response.getHeaders().get(CONTENT_TYPE))) {
                this.getDownloadFile(url, response);
            }
            LOGGER.info("End building response");
            LoggerUtils.info(LOGGER, response);
            return response;
        }else {
            LOGGER.error("Response is empty.");
            throw new RuntimeException("API Response is null.");
        }
    }

    private void getDownloadFile(String url, RestServiceResponse response) {
        String downloadPath = this.getDownloadFilePath();
        String fileName = this.getDownloadFileName(url);
        Path path = Paths.get(downloadPath.toString(), fileName);
        try {
            Files.copy((InputStream)response.getResponse(), path);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        response.setResponse(path.toAbsolutePath().toString());
    }

    private String getDownloadFileName(String strUrl) {
        try {
            URL url = new URL(strUrl);
            String path = url.getPath();
            String fileName = path.substring(path.lastIndexOf("/") + 1, path.length());
            fileName = URLDecoder.decode(fileName, "UTF-8");
            return fileName;
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getDownloadFilePath() {
        Optional<Object> curDir = this.getRobotVariableValue("SUITE SOURCE");
        if(curDir.isPresent()) {
            return curDir.toString().substring(0, curDir.toString().lastIndexOf(File.separator));
        } else {
            LOGGER.error("Can't find Download File Path.");
            throw new RuntimeException("Can't find Download File Path.");
        }
    }

    private Optional<Object> getRobotVariableValue(String variableName) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            engine.eval("from robot.libraries.BuiltIn import BuiltIn");
            if (variableName.startsWith("${") && variableName.endsWith("}")) {
                engine.eval("v = BuiltIn().get_variables()['" + variableName + "']");
            } else {
                engine.eval("v = BuiltIn().get_variables()['${" + variableName + "}']");
            }
            return Optional.ofNullable(engine.get("v"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return Optional.empty();
    }

    private RestServiceResponse getRestServiceResponseObject(Response clientResponse) {
        RestServiceResponse response = new RestServiceResponse();
        response.setStatusCode(clientResponse.getStatusInfo().getStatusCode());
        response.setStatusMessage(clientResponse.getStatusInfo().getReasonPhrase());
        response.setHeaders(getResponseHeaders(clientResponse));
        Object responseBody = null;
        if (response.getHeaders().get(CONTENT_TYPE) != null && response.getHeaders().get(CONTENT_TYPE).equalsIgnoreCase(CT_APPLICATION_OCTETSTREAM)) {
            responseBody = clientResponse.readEntity(InputStream.class);
            response.setResponse(responseBody);
        } else {
            responseBody = clientResponse.readEntity(String.class);
            try {
                Object responseObject = new JSONParser().parse(responseBody.toString());
                if(responseObject instanceof JSONObject) {
                    JSONObject json = (JSONObject) responseObject;
                    response.setResponse(json);
                } else if ( responseObject instanceof JSONArray) {
                    JSONArray json = (JSONArray) responseObject;
                    response.setResponse(json);
                } else {
                    response.setResponse(responseBody);
                }
            } catch (ParseException e) {
                LOGGER.error(e);
                response.setResponse(responseBody);
            }
        }
        response.setCookies(clientResponse.getCookies());;
        return response;
    }

    public Map<String, String> getResponseHeaders(Response clientResponse) {
        Map<String, String> headers = new LinkedHashMap<>();
        MultivaluedMap<String, Object> responseHeaders = clientResponse.getHeaders();
        Set<Entry<String, List<Object>>> entrySet = responseHeaders.entrySet();
        Iterator<Entry<String, List<Object>>> itr = entrySet.iterator();
        while (itr.hasNext()) {
            Entry<String, List<Object>> entry = itr.next();
            headers.put(entry.getKey(), StringUtils.join(entry.getValue(), ";"));
        }
        return headers;
    }
}
