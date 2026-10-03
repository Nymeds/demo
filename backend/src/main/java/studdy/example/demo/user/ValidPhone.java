package studdy.example.demo.user;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Celular brasileiro com DDD, ou vazio. Fica no próprio campo para o erro voltar como "phone"
 * (um @AssertTrue isPhoneValid() voltava como "phoneValid" e a tela não mostrava embaixo do campo).
 */
@Documented
@Constraint(validatedBy = ValidPhone.Validator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {

    String message() default "Informe um celular válido com DDD: (DD) 9XXXX-XXXX, começando com 9 após o DDD.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPhone, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return PhoneNumbers.isValidOrBlank(value == null ? null : value.trim());
        }
    }
}
