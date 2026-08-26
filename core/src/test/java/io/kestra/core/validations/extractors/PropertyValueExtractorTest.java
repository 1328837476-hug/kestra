package io.kestra.core.validations.extractors;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.kestra.core.models.property.Property;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class PropertyValueExtractorTest {

    @Inject
    private Validator validator;

    @Test
    public void should_extract_and_validate_integer_value() {
        DynamicPropertyDto dto = new DynamicPropertyDto(Property.ofValue(20), Property.ofValue("Test"));
        Set<ConstraintViolation<DynamicPropertyDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());

        dto = new DynamicPropertyDto(Property.ofValue(5), Property.ofValue("Test"));
        violations = validator.validate(dto);
        assertThat(violations.size()).isEqualTo(1);
        ConstraintViolation<DynamicPropertyDto> violation = violations.stream().findFirst().get();
        assertThat(violation.getMessage()).isEqualTo("must be greater than or equal to 10");
    }

    // Gating experiment for kestra-ee#10347: proves @Valid cascades through a Property's list
    // elements only when placed on the type argument (Property<List<@Valid X>>), not the field.
    @Test
    public void should_cascade_valid_into_property_list_elements() {
        DynamicPropertyDto blank = new DynamicPropertyDto(
            Property.ofValue(20), Property.ofValue("Test"), Property.ofValue(List.of(new NestedDto(" ")))
        );
        Set<ConstraintViolation<DynamicPropertyDto>> violations = validator.validate(blank);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().findFirst().get().getPropertyPath().toString()).contains("list").contains("id");

        DynamicPropertyDto ok = new DynamicPropertyDto(
            Property.ofValue(20), Property.ofValue("Test"), Property.ofValue(List.of(new NestedDto("ok")))
        );
        assertTrue(validator.validate(ok).isEmpty());

        DynamicPropertyDto expression = new DynamicPropertyDto(
            Property.ofValue(20), Property.ofValue("Test"), Property.ofExpression("{{ x }}")
        );
        assertTrue(validator.validate(expression).isEmpty());
    }

}
