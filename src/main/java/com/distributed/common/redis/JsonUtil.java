package com.distributed.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

/**
 * @author caoyuyi
 */
@Slf4j
public class JsonUtil {
    /**
     * 定义jackson对象
     */
    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将对象转换成json字符串。
     * <p>Title: pojoToJson</p>
     * <p>Description: </p>
     *
     * @param data
     * @return
     */
    public static String objectToJson(Object data) {
        try {
            String string = MAPPER.writeValueAsString(data);
            return string;
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将json结果集转化为对象
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            T t = MAPPER.readValue(jsonData, beanType);
            return t;
        } catch (Exception e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将json数据转换成pojo对象list
     * <p>Title: jsonToList</p>
     * <p>Description: </p>
     *
     * @param jsonData
     * @param beanType
     * @return
     */
    public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
        JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            List<T> list = MAPPER.readValue(jsonData, javaType);
            return list;
        } catch (Exception e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 当JSON里只含有Bean的部分属性时，更新一个已存在Bean，只覆盖该部分的属性
     */
    public static void update(String jsonString, Object object) {
        try {
            MAPPER.readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException e) {
            log.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        } catch (IOException e) {
            log.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        }
    }

}


