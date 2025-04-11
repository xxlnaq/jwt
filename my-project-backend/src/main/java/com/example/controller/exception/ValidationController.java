package com.example.controller.exception;

import com.example.entity.RestBean;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.generate.ValueCodeGenerationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ValidationController {
    @ExceptionHandler(ValueCodeGenerationException.class)
    public RestBean<Void> validateException( ValidationException exception ) {
        log.warn("Resolve[{}:{}]",exception.getClass().getName(), exception.getMessage());
          return RestBean.fail(400,"请求参数有误");
    }

}
