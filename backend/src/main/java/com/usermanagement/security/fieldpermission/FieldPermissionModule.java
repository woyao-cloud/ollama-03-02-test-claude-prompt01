package com.usermanagement.security.fieldpermission;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

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
            // Check if field should be serialized. Note: BeanPropertyWriter writes the
            // field name before calling this serializer, so this method must only
            // write the field value (not the name).
            if (shouldSerialize(value)) {
                if (value == null) {
                    gen.writeNull();
                } else {
                    serializers.defaultSerializeValue(value, gen);
                }
            } else {
                // Mask or hide the value. We must still write a valid JSON value
                // because the field name has already been emitted by the property
                // writer.
                switch (annotation.mask()) {
                    case HIDE:
                        gen.writeNull();
                        break;
                    case NULL:
                        gen.writeNull();
                        break;
                    case EMPTY:
                        gen.writeString("");
                        break;
                    case ASTERISK:
                        gen.writeString("****");
                        break;
                    case CUSTOM:
                        gen.writeString(annotation.maskPattern());
                        break;
                    default:
                        gen.writeNull();
                        break;
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
