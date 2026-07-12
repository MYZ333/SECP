package com.medcare.hda.common.crypto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.medcare.hda.annotation.Desensitize;

import java.io.IOException;

/** 脱敏序列化器：读取字段上的 @Desensitize 类型，输出打码后的值 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type = DesensitizeType.GENERIC;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(mask(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Desensitize ann = property.getAnnotation(Desensitize.class);
            if (ann != null) {
                DesensitizeSerializer s = new DesensitizeSerializer();
                s.type = ann.value();
                return s;
            }
        }
        return this;
    }

    private String mask(String v, DesensitizeType t) {
        if (v == null || v.isEmpty()) {
            return v;
        }
        return switch (t) {
            case PHONE -> v.length() >= 11
                    ? v.substring(0, 3) + "****" + v.substring(v.length() - 4)
                    : maskGeneric(v);
            case ID_CARD -> v.length() >= 8
                    ? v.substring(0, 3) + "***********" + v.substring(v.length() - 4)
                    : maskGeneric(v);
            case NAME -> v.length() <= 1 ? v : v.charAt(0) + "*".repeat(v.length() - 1);
            case GENERIC -> maskGeneric(v);
        };
    }

    private String maskGeneric(String v) {
        if (v.length() <= 2) {
            return v.charAt(0) + "*";
        }
        return v.charAt(0) + "*".repeat(v.length() - 2) + v.charAt(v.length() - 1);
    }
}
