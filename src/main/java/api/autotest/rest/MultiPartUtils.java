package api.autotest.rest;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;

public class MultiPartUtils {
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final Configuration JSON_ORG_CONFIGURATION = Configuration.builder()
            .mappingProvider(new JsonOrgMappingProvider()).jsonProvider(new JsonOrgJsonProvider()).build();

    @SuppressWarnings("unchecked")
    public static Optional<MultiPart> getMultiPart(Map<String, String> headers, String postBody,
                                                   FormDataMultiPart formDataMultiPart) {
        if (headers.containsKey(HEADER_CONTENT_DISPOSITION)) {
            formDataMultiPart
                    .setContentDisposition(ContentDisposition.type(headers.get(HEADER_CONTENT_DISPOSITION)).build());
        }
        DocumentContext documentContext = JsonPath.using(JSON_ORG_CONFIGURATION).parse(postBody);
        Object dcObject = documentContext.json();
        if (dcObject instanceof org.json.JSONObject) {
            JSONObject jsonObject = (JSONObject) dcObject;
            Iterator<String> itr = jsonObject.keys();
            while (itr.hasNext()) {
                String key = itr.next();
                Object value;
                try {
                    value = jsonObject.get(key);
                    if (value instanceof String) {
                        String valueStr = String.valueOf(value);
                        if (valueStr != null && valueStr.trim().startsWith("file:")) {
                            valueStr = valueStr.replace("file:", "");
                            File file = new File(valueStr);
                            FileDataBodyPart filePart = new FileDataBodyPart(key, file);
                            return Optional.ofNullable(formDataMultiPart.bodyPart(filePart));
                        } else {
                            throw new RuntimeException("File path should start with 'file:'.");
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        return Optional.empty();
    }
}
