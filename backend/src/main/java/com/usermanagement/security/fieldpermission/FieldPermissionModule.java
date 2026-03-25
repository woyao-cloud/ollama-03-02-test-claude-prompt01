package com.usermanagement.security.fieldpermission;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Field Permission Jackson Module
 * Custom Jackson module that applies field-level permission filtering
 * during JSON serialization
 *
 * @author Security Team
 * @since 1.0
 */
@Component
public class FieldPermissionModule extends SimpleModule {

    private final FieldPermissionService fieldPermissionService;

    public FieldPermissionModule(FieldPermissionService fieldPermissionService) {
        super("FieldPermissionModule");
        this.fieldPermissionService = fieldPermissionService;

        // Register custom serializer modifier
        setSerializerModifier(new FieldPermissionSerializerModifier());
    }

    /**
     * Custom serializer modifier that handles @FieldPermission annotations
     */
    private class FieldPermissionSerializerModifier extends BeanSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {

            for (BeanPropertyWriter writer : beanProperties) {
                FieldPermission annotation = getFieldPermissionAnnotation(beanDesc, writer);
                if (annotation != null) {
                    // Wrap the original writer with permission-aware logic
                    writer.assignSerializer(new PermissionAwareSerializer(writer, annotation));
                }
            }

            return beanProperties;
        }

        private FieldPermission getFieldPermissionAnnotation(BeanDescription beanDesc, BeanPropertyWriter writer) {
            // Try to get from field
            var field = writer.getMember();
            if (field != null) {
                return field.getAnnotation(FieldPermission.class);
            }
            return null;
        }
    }

    /**
     * Permission-aware serializer that checks access before serializing
     */
    private class PermissionAwareSerializer extends JsonSerializer<Object> {

        private final BeanPropertyWriter writer;
        private final FieldPermission annotation;

        public PermissionAwareSerializer(BeanPropertyWriter writer, FieldPermission annotation) {
            this.writer = writer;
            this.annotation = annotation;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            // Check if field should be serialized
            if (shouldSerialize(value)) {
                try {
                    writer.serializeAsField(value, gen, serializers);
                } catch (Exception e) {
                    throw new IOException("Failed to serialize field", e);
                }
            } else {
                // Skip or mask based on configuration
                if (annotation.mask() == FieldPermission.MaskType.HIDE) {
                    // Don't write anything
                    return;
                }

                // Write masked value
                Object maskedValue = createMaskedValue(value);
                if (maskedValue != null) {
                    gen.writeObjectField(writer.getName(), maskedValue);
                }
            }
        }

        private boolean shouldSerialize(Object value) {
            // Check FieldFilterContext first
            if (FieldFilterContext.exists()) {
                FieldFilterContext ctx = FieldFilterContext.current();
                String resource = annotation.resource();
                String field = annotation.field().isEmpty() ? writer.getName() : annotation.field();

                if (!ctx.isFieldAllowed(resource, field)) {
                    return false;
                }
            }

            // Check field permission service
            String permissionCode = buildPermissionCode();
            return fieldPermissionService.canReadField(annotation.resource(),
                    annotation.field().isEmpty() ? writer.getName() : annotation.field());
        }

        private Object createMaskedValue(Object value) {
            switch (annotation.mask()) {
                case NULL:
                    return null;
                case EMPTY:
                    return "";
                case ASTERISK:
                    return "****";
                case CUSTOM:
                    return annotation.maskPattern();
                default:
                    return null;
            }
        }

        private String buildPermissionCode() {
            if (!annotation.value().isEmpty()) {
                return annotation.value();
            }
            String resource = annotation.resource();
            String field = annotation.field().isEmpty() ? writer.getName() : annotation.field();
            return resource.toUpperCase() + "_FIELD_" + field.toUpperCase() + "_READ";
        }
    }
}
