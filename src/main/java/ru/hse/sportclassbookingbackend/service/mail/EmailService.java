package ru.hse.sportclassbookingbackend.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String VERIFY_EMAIL_TEMPLATE = "mail/verify-email";
    private static final String PASSWORD_RESET_TEMPLATE = "mail/password-reset";
    private static final String SHEET_CREATED_TEMPLATE = "mail/sheet-created";
    private static final String SHEET_CANCELLED_TEMPLATE = "mail/sheet-cancelled";
    private static final String LESSON_CHANGED_TEMPLATE = "mail/lesson-changed";
    private static final String LESSON_CANCELLED_TEMPLATE = "mail/lesson-cancelled";
    private static final String HEALTH_GROUP_CHANGED_TEMPLATE = "mail/health-group-changed";
    private static final String VERIFY_EMAIL_PATH = "/auth/verify-email";

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", new Locale("ru"));

    @Value("${mail.from}")
    private String from;

    @Value("${mail.verification.subject}")
    private String verificationSubject;

    @Value("${mail.reset.subject}")
    private String resetSubject;

    @Value("${mail.sheet-created.subject}")
    private String sheetCreatedSubject;

    @Value("${mail.sheet-cancelled.subject}")
    private String sheetCancelledSubject;

    @Value("${mail.lesson-changed.subject}")
    private String lessonChangedSubject;

    @Value("${mail.lesson-cancelled.subject}")
    private String lessonCancelledSubject;

    @Value("${mail.health-group-changed.subject}")
    private String healthGroupChangedSubject;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendVerificationEmail(String to, String firstName, UUID token) {
        String verificationLink = UriComponentsBuilder.fromUriString(appBaseUrl)
                .path(VERIFY_EMAIL_PATH)
                .queryParam("token", token)
                .build()
                .toUriString();

        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("verificationLink", verificationLink);

        String html = templateEngine.process(VERIFY_EMAIL_TEMPLATE, ctx);
        sendHtml(to, verificationSubject, html, "verification");
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String code) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("code", code);

        String html = templateEngine.process(PASSWORD_RESET_TEMPLATE, ctx);
        sendHtml(to, resetSubject, html, "password-reset");
    }

    @Async
    public void sendSheetCreatedEmail(String to, String firstName, LessonInfo lesson) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("title", lesson.title());
        ctx.setVariable("startTime", formatDateTime(lesson.startTime(), lesson.zoneId()));
        ctx.setVariable("endTime", formatTime(lesson.endTime(), lesson.zoneId()));
        ctx.setVariable("place", lesson.place());
        ctx.setVariable("campusName", lesson.campusName());
        ctx.setVariable("teacherFullName", lesson.teacherFullName());

        String html = templateEngine.process(SHEET_CREATED_TEMPLATE, ctx);
        sendHtml(to, sheetCreatedSubject, html, "sheet-created");
    }

    @Async
    public void sendSheetCancelledEmail(String to, String firstName, LessonInfo lesson, String cancelledByFullName) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("title", lesson.title());
        ctx.setVariable("startTime", formatDateTime(lesson.startTime(), lesson.zoneId()));
        ctx.setVariable("place", lesson.place());
        ctx.setVariable("cancelledBy", cancelledByFullName);

        String html = templateEngine.process(SHEET_CANCELLED_TEMPLATE, ctx);
        sendHtml(to, sheetCancelledSubject, html, "sheet-cancelled");
    }

    @Async
    public void sendLessonChangedEmail(String to, String firstName, LessonInfo before, LessonInfo after) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("oldTitle", before.title());
        ctx.setVariable("newTitle", after.title());
        ctx.setVariable("titleChanged", !java.util.Objects.equals(before.title(), after.title()));
        ctx.setVariable("oldStart", formatDateTime(before.startTime(), before.zoneId()));
        ctx.setVariable("newStart", formatDateTime(after.startTime(), after.zoneId()));
        ctx.setVariable("startChanged", !before.startTime().isEqual(after.startTime()));
        ctx.setVariable("oldPlace", before.place());
        ctx.setVariable("newPlace", after.place());
        ctx.setVariable("placeChanged", !java.util.Objects.equals(before.place(), after.place()));

        String html = templateEngine.process(LESSON_CHANGED_TEMPLATE, ctx);
        sendHtml(to, lessonChangedSubject, html, "lesson-changed");
    }

    @Async
    public void sendLessonCancelledEmail(String to, String firstName, LessonInfo lesson) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("title", lesson.title());
        ctx.setVariable("startTime", formatDateTime(lesson.startTime(), lesson.zoneId()));
        ctx.setVariable("place", lesson.place());

        String html = templateEngine.process(LESSON_CANCELLED_TEMPLATE, ctx);
        sendHtml(to, lessonCancelledSubject, html, "lesson-cancelled");
    }

    @Async
    public void sendHealthGroupChangedEmail(String to, String firstName,
                                            String oldGroupName, String newGroupName,
                                            java.util.List<String> allowedWorkoutTypes) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("oldGroup", oldGroupName);
        ctx.setVariable("newGroup", newGroupName);
        ctx.setVariable("allowedWorkoutTypes", allowedWorkoutTypes);

        String html = templateEngine.process(HEALTH_GROUP_CHANGED_TEMPLATE, ctx);
        sendHtml(to, healthGroupChangedSubject, html, "health-group-changed");
    }

    private String formatDateTime(OffsetDateTime dt, java.time.ZoneId zoneId) {
        return dt.atZoneSameInstant(zoneId).format(DATETIME_FORMAT);
    }

    private String formatTime(OffsetDateTime dt, java.time.ZoneId zoneId) {
        return dt.atZoneSameInstant(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void sendHtml(String to, String subject, String html, String kind) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("{} email sent to {}", kind, to);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send {} email to {}: {}", kind, to, ex.getMessage(), ex);
        }
    }
}
