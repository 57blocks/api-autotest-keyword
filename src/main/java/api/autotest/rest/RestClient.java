package api.autotest.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import api.autotest.common.LoggerUtils;

public class RestClient {
    private static final Logger LOGGER = Logger.getLogger(RestClient.class);
    public static final String CONTENT_TYPE = "Content-Type";
    private static final String CT_JSON = "application/json";
    private static final String CT_URLENCODED = "application/x-www-form-urlencoded";
    private static final String CT_MULTIPART_FORMDATA = "multipart/form-data";
    public static final String CT_APPLICATION_OCTETSTREAM = "application/octet-stream";

    public Optional<Response> get(String url, Map<String, String> headers) {
        LoggerUtils.info(LOGGER, "Invoking Get URL : " + url);
        try {
            url = URIUtil.encodeQuery(url);
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url);
            Builder builder = target.request();
            this.parseHeader(builder, headers);
            return Optional.ofNullable(builder.get());
        } catch (URIException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public Optional<Response> post(String url, Map<String, String> headers, String postBody) {
        LoggerUtils.info(LOGGER, "Invoking Post URL : " + url);
        FormDataMultiPart formDataMultiPart = null;
        try {
            url = URIUtil.encodeQuery(url);
            Client client = ClientBuilder.newClient();
            client.register(MultiPartFeature.class);
            WebTarget target = client.target(url);
            Builder builder = target.request();
            this.parseHeader(builder, headers);
            Entity<?> payload = null;
            if (MapUtils.isNotEmpty(headers) && headers.containsKey(CONTENT_TYPE)
                    && headers.get(CONTENT_TYPE).equalsIgnoreCase(CT_URLENCODED)) {
                String[] keyValueArray = postBody.split("&");
                List<String[]> argsArray = Arrays.stream(keyValueArray).map(a -> a.split("="))
                        .collect(Collectors.toList());
                Map<String, String> map = new HashMap<>();
                for (String[] arg : argsArray) {
                    map.put(arg[0], arg[1]);
                }
                MultivaluedMap<String, String> formData = new MultivaluedHashMap<>(map);
                payload = Entity.form(formData);
            } else if (MapUtils.isNotEmpty(headers) && headers.containsKey(CONTENT_TYPE)
                    && headers.get(CONTENT_TYPE).equalsIgnoreCase(CT_MULTIPART_FORMDATA)) {
                formDataMultiPart = new FormDataMultiPart();
                Optional<MultiPart> multiPart = MultiPartUtils.getMultiPart(headers, postBody, formDataMultiPart);
                if (multiPart.isPresent()) {
                    MultiPart multiPartDetail = multiPart.get();
                    payload = Entity.entity(multiPartDetail, multiPartDetail.getMediaType());
                }
            } else if (MapUtils.isNotEmpty(headers) && headers.containsKey(CONTENT_TYPE)
                    && headers.get(CONTENT_TYPE).equalsIgnoreCase(CT_JSON)) {
                payload = Entity.json(postBody);
            } else {
                payload = Entity.text(postBody);
            }
            LOGGER.info("postBody : " + postBody);
            return Optional.ofNullable(builder.post(payload));
        } catch (URIException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        } finally {
            if (formDataMultiPart != null) {
                try {
                    formDataMultiPart.close();
                } catch (IOException e) {
                    LOGGER.error("Close FormDataMultiPart Resource failure.");
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public Optional<Response> put(String url, Map<String, String> headers, String putBody) {
        LoggerUtils.info(LOGGER, "Invoking Put URL : " + url);
        try {
            url = URIUtil.encodeQuery(url);
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url);
            Builder builder = target.request();
            this.parseHeader(builder, headers);
            Entity<?> payload = null;
            if (MapUtils.isNotEmpty(headers) && headers.containsKey(CONTENT_TYPE)
                    && headers.get(CONTENT_TYPE).equalsIgnoreCase(CT_JSON)) {
                payload = Entity.json(putBody);
            } else {
                payload = Entity.text(putBody);
            }
            return Optional.ofNullable(builder.put(payload));
        } catch (URIException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public Optional<Response> delete(String url, Map<String, String> headers) {
        LoggerUtils.info(LOGGER, "Invoking Delete URL : " + url);
        try {
            url = URIUtil.encodeQuery(url);
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url);
            Builder builder = target.request();
            this.parseHeader(builder, headers);
            return Optional.ofNullable(builder.delete());
        } catch (URIException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void parseHeader(Builder builder, Map<String, String> headers) {
        if (MapUtils.isNotEmpty(headers)) {
            Set<Entry<String, String>> entrySet = headers.entrySet();
            Iterator<Entry<String, String>> itr = entrySet.iterator();
            while (itr.hasNext()) {
                Entry<String, String> entry = itr.next();
                LOGGER.info(entry.toString());
                builder = builder.header(entry.getKey(), entry.getValue());
            }
        }
    }
}