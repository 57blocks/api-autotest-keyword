package api.autotest.db;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import api.autotest.common.LoggerUtils;


public class ValidationUtils {
    private final static Logger LOGGER = Logger.getLogger(ValidationUtils.class);

    public final static String KEY_DBNAME = "DB_NAME";
    public final static String KEY_COLLUMN_NAME = "COLLUMN_NAME";
    public final static String KEY_QUERY_NAME = "QUERY_NAME";
    public final static String KEY_QUERY = "QUERY";
    public final static String KEY_EXPECTED_VALUE = "EXPECTED_VALUE";
    public final static String LENGTH_EQUALS = " LENGTH EQUALS ";
    public final static String VALUE_EQUALS = " VALUE EQUALS ";
    public final static String VALUE_IN_RANGE = " VALUE IN RANGE ";
    public final static String ARRAY_CONTAINS = " ARRAY CONTAINS ";

    public SortedMap<String, String> getByPreffix(NavigableMap<String, String> map, String preffix) {
        return map.subMap(preffix,  true, preffix, true);
    }

    public SortedMap<String, String> getByPreffix(NavigableMap<String, String> map, String preffix, Character toKey) {
        return map.subMap(preffix, preffix + toKey);
    }

    public Map<String, String> getValidationAttributes(String sqlResource, TreeMap<String, String> sqlQueries, String key) {
        Map<String, String> attributes = new HashMap<String, String>();
        List<Object> queryParameters = new LinkedList<Object>();
        if(key.indexOf("(") > -1) {
            String queryParametersStr = key.substring(key.indexOf("(") + 1, key.length() - 1);
            key = key.substring(0, key.indexOf("("));
            String[] queryParametersStrArray = queryParametersStr.split("(?<!\\\\)" + Pattern.quote(","));
            for(int i = 0 ; i < queryParametersStrArray.length; i++) {
                String queryParameter = queryParametersStrArray[i].trim();
                System.out.println("queryParameter : " + queryParameter);
                if(queryParameter.startsWith("\"")) {
                    System.out.println("string : " + queryParameter);
                    queryParameters.add(new String(queryParameter));
                } else if (queryParameter.contains(".")) {
                    System.out.println("dobule : " + queryParameter);
                    queryParameters.add(Double.parseDouble(queryParameter));
                } else {
                    System.out.println("long : " + queryParameter);
                    queryParameters.add(Long.parseLong(queryParameter));
                }
            }
            LoggerUtils.info(LOGGER, queryParameters);
        }
        String[] keyArray = key.split(Pattern.quote("."));
        if(keyArray.length == 1) {
            attributes.put(KEY_EXPECTED_VALUE, key);
        } else if(keyArray.length == 2) {
            attributes.put(KEY_DBNAME, keyArray[0]);
            SortedMap<String, String> queryMap = getByPreffix(sqlQueries, keyArray[0] + "." + keyArray[1]);
            if(queryMap.size() == 0) {
                throw new RuntimeException("Query '" + key + "' not found in the resource " + sqlResource);
            }else if(queryMap.size() > 1) {
                throw new RuntimeException("Too many matching queries found for " + key);
            }
            String query = queryMap.get(queryMap.firstKey());
            LoggerUtils.info(LOGGER, "\nExpected Query : " + query);
            attributes.put(KEY_QUERY_NAME, keyArray[1]);
            attributes.put(KEY_QUERY, formatQuery(query, queryParameters));
        } else if(keyArray.length == 3) {
            System.out.println("4.... ");
            attributes.put(KEY_DBNAME, keyArray[0]);
            attributes.put(KEY_COLLUMN_NAME, keyArray[2]);
            SortedMap<String, String> queryMap = getByPreffix(sqlQueries, keyArray[0] + '.' + keyArray[1]);
            if(queryMap.size() == 0) {
                queryMap = getByPreffix(sqlQueries, keyArray[0] + "." + keyArray[1] + "." + keyArray[2]);
            }
            if(queryMap.size() == 0) {
                queryMap = getByPreffix(sqlQueries, keyArray[0] + "." + keyArray[1] + "." + keyArray[2], Character.MAX_VALUE);
            }
            if(queryMap.size() == 0) {
                queryMap = getByPreffix(sqlQueries, keyArray[0] + "." + keyArray[1], Character.MAX_VALUE);
            }
            if(queryMap.size() == 0) {
                throw new RuntimeException("Query '" + key + "' not found in the resource : " + sqlResource);
            } else if (queryMap.size() > 1) {
                throw new RuntimeException("Too many matching queries found for " + key);
            }
            String query = queryMap.get(queryMap.firstKey());
            LOGGER.info("\nExtracted Query : " + query);
            attributes.put(KEY_QUERY_NAME, keyArray[1]);
            attributes.put(KEY_QUERY, formatQuery(query, queryParameters));
        }
        return attributes;
    }

    public String formatQuery(String query, List<Object> queryParameters) {
        if(CollectionUtils.isEmpty(queryParameters)) {
            return query;
        }
        for(Object obj : queryParameters) {
            query = query.replaceFirst("\\?", obj.toString());
        }
        return query;
    }
}
