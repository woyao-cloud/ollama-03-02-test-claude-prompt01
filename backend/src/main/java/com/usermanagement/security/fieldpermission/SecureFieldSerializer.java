package com.usermanagement.security.fieldpermission;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Secure Field Serializer
 * Custom JSON serializer that applies field-level permission filtering
 * during serialization
 *
 * @author Security Team
 * @since 1.0
 */
public class SecureFieldSerializer extends JsonSerializer<Object> {

    private final FieldPermission fieldPermission;
    private final JsonSerializer<Object> defaultSerializer;

    public SecureFieldSerializer(FieldPermission fieldPermission, JsonSerializer<Object> defaultSerializer) {
        this.fieldPermission = fieldPermission;
        this.defaultSerializer = defaultSerializer;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // Check if field should be hidden
        if (fieldPermission != null && fieldPermission.mask() == FieldPermission.MaskType.HIDE) {
            // Skip serialization entirely
            return;
        }

        // Use default serializer
        if (defaultSerializer != null) {
            defaultSerializer.serialize(value, gen, serializers);
        } else {
            gen.writeObject(value);
        }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value) {
        // Consider masked values as empty for serialization
        if (fieldPermission != null && fieldPermission.mask() == FieldPermission.MaskType.HIDE) {
            return true;
        }
        return super.isEmpty(provider, value);
    }
}
