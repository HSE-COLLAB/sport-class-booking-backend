package ru.hse.sportclassbookingbackend.service.mail;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String FROM = "Sport HSE <noreply@hse.ru>";
    private static final String SUBJECT = "Подтверждение email";
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String TO = "student@hse.ru";
    private static final String FIRST_NAME = "Иван";
    private static final UUID TOKEN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String RENDERED_HTML = "<html>email body</html>";

    @Mock private JavaMailSender mailSender;
    @Mock private SpringTemplateEngine templateEngine;

    @InjectMocks private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "from", FROM);
        ReflectionTestUtils.setField(emailService, "verificationSubject", SUBJECT);
        ReflectionTestUtils.setField(emailService, "appBaseUrl", BASE_URL);
    }

    @Test
    @DisplayName("Рендерит шаблон с firstName и ссылкой ${baseUrl}/auth/verify-email?token=... -успехTest")
    void rendersTemplateWithCorrectLinkTest() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/verify-email"), any(Context.class))).thenReturn(RENDERED_HTML);

        emailService.sendVerificationEmail(TO, FIRST_NAME, TOKEN);

        ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/verify-email"), ctxCaptor.capture());

        Context ctx = ctxCaptor.getValue();
        assertThat(ctx.getVariable("firstName")).isEqualTo(FIRST_NAME);
        assertThat(ctx.getVariable("verificationLink"))
                .isEqualTo(BASE_URL + "/auth/verify-email?token=" + TOKEN);
    }

    @Test
    @DisplayName("При падении SMTP не пробрасывает исключение наружу-успехTest")
    void swallowsMailSendExceptionTest() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/verify-email"), any(Context.class))).thenReturn(RENDERED_HTML);
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendVerificationEmail(TO, FIRST_NAME, TOKEN))
                .doesNotThrowAnyException();
    }

    // Локальный helper для mock'а MimeMessage без статического импорта Mockito.mock
    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
