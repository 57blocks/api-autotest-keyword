package api.autotest.rest;

import java.util.Map;

import javax.ws.rs.core.NewCookie;

import org.codehaus.jackson.map.ObjectMapper;

public class RestServiceResponse {
    public int statusCode;
    public String statusMessage;
    public Map<String, String> headers;
    public Map<String, NewCookie> cookies;
    public Object response;

    public Map<String, NewCookie> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, NewCookie> cookies) {
        this.cookies = cookies;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    @Override
    public String toString() {
        ObjectMapper mapperObj = new ObjectMapper();
        try {
            return mapperObj.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.toString();
    }
}
