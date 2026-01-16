package com.atlantic.esocial.etc.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import com.google.common.base.CaseFormat;

import java.io.IOException;

public class CamelCaseSerializer extends StdKeySerializers.StringKeySerializer {

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
        String newKeyValue = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, (String) value);
        g.writeFieldName(newKeyValue);
    }

}