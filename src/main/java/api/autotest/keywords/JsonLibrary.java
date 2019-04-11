package api.autotest.keywords;

import static api.autotest.rest.RestClientUtils.REPLACE_WITH;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import api.autotest.util.CommonUtils;

@RobotKeywords
public class JsonLibrary {
    private final static Logger LOGGER = Logger.getLogger(JsonLibrary.class);

    private static Configuration configuration;

    static {
        JacksonJsonNodeJsonProvider jacksonJsonNodeJsonProvider = new JacksonJsonNodeJsonProvider();
        jacksonJsonNodeJsonProvider.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        configuration = Configuration.builder().jsonProvider(jacksonJsonNodeJsonProvider)
                .mappingProvider(new JacksonMappingProvider()).build();
    }

    @RobotKeyword
    public void validateJsonObject(String expected, String actual) {
        validateJsonObject(expected, actual, JSONCompareMode.LENIENT.toString());
    }

    @RobotKeyword
    public void validateJsonObject(String expected, String actual, String compareMode) {
        JSONCompareMode jsonCompareMode = JSONCompareMode.LENIENT;

        if (StringUtils.isNotBlank(compareMode)) {
            jsonCompareMode = JSONCompareMode.valueOf(compareMode);
        }

        JsonParser parser = new JsonParser();
        JsonElement expectedJson = parser.parse(expected);
        JsonElement actualJson = parser.parse(actual);

        if (expectedJson.equals(actualJson)) {
            LOGGER.info("Json objects are equal");
        } else {
            try {
                JSONAssert.assertEquals(expected, actual, jsonCompareMode);
                LOGGER.info("Json Object are equal");
            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @RobotKeyword
    public Object buildJsonTemplate(String jsonFilePath, String templateValue) {
        String[] templateValues = templateValue.split(";");
        Scanner scanner = null;
        try {
            Map<String, Object> templateValuesMap = CommonUtils.getTemplateValuesMap(templateValues, REPLACE_WITH);
            scanner = new Scanner(new File(jsonFilePath), "UTF-8");
            String text = scanner.useDelimiter("\\A").next();
            DocumentContext documentContext = JsonPath.using(configuration).parse(text);
            Iterator<Entry<String, Object>> itr = templateValuesMap.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<String, Object> entry = itr.next();
                if(entry.getValue() == null || entry.getValue().toString().equalsIgnoreCase("None")) {
                    documentContext.set(entry.getKey(), "-NULL-");
                }else if(entry.getValue().toString().equalsIgnoreCase("null")) {
                    documentContext.delete(entry.getKey());
                }else {
                    documentContext.set(entry.getKey(), entry.getValue());
                }
            }
            return documentContext.jsonString().replace("\"-NULL-\"", "null");
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}
