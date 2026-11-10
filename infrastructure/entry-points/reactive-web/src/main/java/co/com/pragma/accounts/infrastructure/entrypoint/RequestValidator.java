package co.com.pragma.accounts.infrastructure.entrypoint;

import co.com.pragma.accounts.model.exception.BusinessException;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Valida los DTO de entrada con Jakarta Bean Validation (SEG-O-005). En WebFlux
 * funcional la validacion no es automatica: se invoca explicitamente en el
 * handler tras deserializar el body. Las violaciones se traducen a un error de
 * negocio de validacion (400) con el detalle por campo en metadata.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T target) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (violations.isEmpty()) {
            return target;
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        violations.forEach(v -> fields.put(v.getPropertyPath().toString(), v.getMessage()));
        throw new BusinessException(
            ErrorCatalog.VALIDATION_ERROR.code(),
            ErrorCatalog.VALIDATION_ERROR.defaultMessage(),
            fields);
    }
}
