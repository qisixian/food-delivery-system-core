package com.sky.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer operationIdCustomiser() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (operation.getOperationId() != null
                            && operation.getTags() != null
                            && !operation.getTags().isEmpty()) {
                        // override the operationId
                        String oldId = operation.getOperationId();
                        String tag = operation.getTags().get(0);
                        String userPrefix = path.startsWith("/user") ? "user_" : "";
                        String normalizedTag = tag.replaceAll("-controller$", "");
                        normalizedTag = kebabToLowerCamelCase(normalizedTag);
                        operation.setOperationId(userPrefix + normalizedTag + "_" + oldId);
                    }
                });
            });
        };
    }

    /**
     * Reduce duplication caused by {@code Result<T>} by introducing a shared base schema and
     * rewriting concrete {@code Result*} schemas into {@code allOf: [ResultBase, {data: ...}]}.
     */
    @Bean
    public OpenApiCustomizer resultSchemaCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null || components.getSchemas().isEmpty()) {
                return;
            }

            Map<String, Schema> schemas = components.getSchemas();
            String resultBaseName = chooseResultBaseSchemaName(schemas);
            schemas.putIfAbsent(resultBaseName, buildResultBaseSchema());

            List<Map.Entry<String, Schema>> entries = new ArrayList<>(schemas.entrySet());
            for (Map.Entry<String, Schema> entry : entries) {
                String schemaName = entry.getKey();
                if (schemaName == null
                        || !schemaName.startsWith("Result")
                        || schemaName.equals(resultBaseName)) {
                    continue;
                }

                Schema<?> schema = entry.getValue();
                if (schema == null || schema.getAllOf() != null) {
                    continue;
                }

                Map<String, Schema> properties = schema.getProperties();
                if (properties == null
                        || !properties.containsKey("code")
                        || !properties.containsKey("msg")
                        || !properties.containsKey("data")) {
                    continue;
                }

                Map<String, Schema> overrideProperties = new LinkedHashMap<>();
                for (Map.Entry<String, Schema> propEntry : properties.entrySet()) {
                    String propName = propEntry.getKey();
                    if ("code".equals(propName) || "msg".equals(propName)) {
                        continue;
                    }
                    overrideProperties.put(propName, propEntry.getValue());
                }

                Schema<?> overrideSchema = new Schema<>();
                overrideSchema.setType("object");
                overrideSchema.setProperties(overrideProperties);
                if (schema.getRequired() != null) {
                    List<String> required = new ArrayList<>();
                    for (String r : schema.getRequired()) {
                        if (!"code".equals(r) && !"msg".equals(r)) {
                            required.add(r);
                        }
                    }
                    if (!required.isEmpty()) {
                        overrideSchema.setRequired(required);
                    }
                }

                Schema<?> resultBaseRef = new Schema<>();
                resultBaseRef.set$ref("#/components/schemas/" + resultBaseName);
                ComposedSchema composedSchema = new ComposedSchema();
                composedSchema.setAllOf(List.of(resultBaseRef, overrideSchema));
                composedSchema.setDescription(schema.getDescription());

                schemas.put(schemaName, composedSchema);
            }
        };
    }

    private static Schema<?> buildResultBaseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(new LinkedHashMap<>());
        IntegerSchema codeSchema = new IntegerSchema();
        codeSchema.setFormat("int32");
        schema.getProperties().put("code", codeSchema);
        schema.getProperties().put("msg", new StringSchema());
        return schema;
    }

    private static String chooseResultBaseSchemaName(Map<String, Schema> schemas) {
        if (!schemas.containsKey("Result")) {
            return "Result";
        }
        if (!schemas.containsKey("ResultBase")) {
            return "ResultBase";
        }

        int i = 2;
        while (schemas.containsKey("ResultBase" + i)) {
            i++;
        }
        return "ResultBase" + i;
    }

    private static String kebabToLowerCamelCase(String kebab) {
        String[] parts = kebab.split("-");
        if (parts.length == 0 || parts.length == 1) return kebab;

        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
