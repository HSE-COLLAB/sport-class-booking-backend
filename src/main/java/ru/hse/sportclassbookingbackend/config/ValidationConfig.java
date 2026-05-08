package ru.hse.sportclassbookingbackend.config;

import jakarta.validation.MessageInterpolator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Locale;

@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setMessageInterpolator(new EnglishMessageInterpolator(
                new ParameterMessageInterpolator()
        ));
        return validatorFactoryBean;
    }

    private static final class EnglishMessageInterpolator implements MessageInterpolator {
        private final MessageInterpolator delegate;

        private EnglishMessageInterpolator(MessageInterpolator delegate) {
            this.delegate = delegate;
        }

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return delegate.interpolate(messageTemplate, context, Locale.ENGLISH);
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return delegate.interpolate(messageTemplate, context, Locale.ENGLISH);
        }
    }
}
