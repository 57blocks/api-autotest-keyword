package api.autotest.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.log4testng.Logger;

public class CommonUtils {
    public final static String REPLACE_WITH = " REPLACE WITH ";
    private final static Logger LOGGER = Logger.getLogger(CommonUtils.class);

    public static Map<String, Object> getTemplateValuesMap(String[] templateValues, String delimeter) throws ParseException {
        Map<String, Object> formDataMap = new LinkedHashMap();
        JSONParser jsonParser = new JSONParser();
        if (templateValues != null && templateValues.length > 0) {
            for (String templateValue : templateValues) {
                templateValue = templateValue.trim();
                String[] keyValue = templateValue.split(REPLACE_WITH);
                String key = keyValue[0];
                Object value = keyValue[1];
                String valueType = "";
                String valueStr = value.toString();
                if (valueStr.equalsIgnoreCase("null") || valueStr.equalsIgnoreCase("None")) {
                    value = null;
                    valueType = "null";
                } else if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                    value = valueStr.substring(1, valueStr.length() - 1);
                    valueType = "String";
                } else if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
                    value = jsonParser.parse(valueStr);
                    valueType = "JSON Object";
                } else if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                    value = jsonParser.parse(valueStr);
                    valueType = "JSON Array";
                } else if (valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false")) {
                    value = Boolean.parseBoolean(valueStr);
                    valueType = "Boolean";
                } else if (valueStr.matches("^[+-]?\\d*$")) {
                    value = Long.parseLong(valueStr);
                    valueType = "Integer Number";
                } else if (valueStr.matches("^([+-]?\\d*\\.?\\d*)$")) {
                    value = Double.parseDouble(valueStr);
                    valueType = "Decimal Number";
                }
                formDataMap.put(key, value);
                LOGGER.info(String.format("key: %s ; type : %s ; value : %s", key, valueType, value));
            }
        }
        return formDataMap;
    }

    public static String removeNonAsciiChars(String str) {
        return str.replaceAll("[^\\p{ASCII}]", "");
    }

}