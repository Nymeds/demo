package studdy.example.demo.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita o texto em bytes UTF-8 (não em caracteres). Existe por causa do BCrypt, que só aceita
 * 72 bytes: letras acentuadas ocupam 2 bytes e emojis 4. Nulo é válido (use @NotBlank junto).
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value() default PasswordLimits.BCRYPT_MAX_BYTES;

    String message() default "A senha é longa demais. Letras acentuadas e símbolos ocupam mais espaço; use menos caracteres.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
