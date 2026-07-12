package com.medcare.hda.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.medcare.hda.common.crypto.DesensitizeSerializer;
import com.medcare.hda.common.crypto.DesensitizeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 出参脱敏：字段序列化为 JSON 时自动打码。
 * 用法：在实体/VO 字段上 @Desensitize(DesensitizeType.PHONE)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {
    DesensitizeType value() default DesensitizeType.GENERIC;
}
