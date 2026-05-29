package ru.hse.sportclassbookingbackend.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;
import ru.hse.sportclassbookingbackend.service.mail.EmailService;
import ru.hse.sportclassbookingbackend.service.mail.LessonInfo;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailEventListenerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LESSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SHEET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID STUDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CANCELLED_BY = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TOKEN = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock private EmailService emailService;
    @Mock private LessonRepository lessonRepository;
    @Mock private SheetRepository sheetRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MailEventListener listener;

    private LessonInfo lessonInfo;
    private Student student;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        lessonInfo = new LessonInfo(
                "Йога", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                "зал 3", "НН", "Иванов И. И.", ZoneId.of("Europe/Moscow")
        );

        student = new Student();
        student.setId(STUDENT_ID);
        student.setEmail("stud@hse.ru");
        student.setFirstName("Иван");

        Campus campus = new Campus();
        campus.setId(3);
        campus.setName("НН");
        campus.setTimezone("Europe/Moscow");

        Teacher teacher = new Teacher();
        teacher.setFirstName("Анна");
        teacher.setLastName("Смирнова");

        lesson = new Lesson();
        lesson.setId(LESSON_ID);
        lesson.setTitle("Йога");
        lesson.setStartTime(OffsetDateTime.now().plusDays(1));
        lesson.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        lesson.setPlace("зал 3");
        lesson.setCampus(campus);
        lesson.setTeacher(teacher);
    }

    @Test
    @DisplayName("UserRegisteredEvent → sendVerificationEmail-успехTest")
    void onUserRegisteredSendsVerificationTest() {
        listener.onUserRegistered(new UserRegisteredEvent(USER_ID, "u@hse.ru", "Ivan", TOKEN));

        verify(emailService).sendVerificationEmail("u@hse.ru", "Ivan", TOKEN);
    }

    @Test
    @DisplayName("SheetCreatedEvent → sendSheetCreatedEmail с данными студента-успехTest")
    void onSheetCreatedSendsConfirmationTest() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        listener.onSheetCreated(new SheetCreatedEvent(SHEET_ID, LESSON_ID, STUDENT_ID));

        verify(emailService).sendSheetCreatedEmail(eq("stud@hse.ru"), eq("Иван"), any(LessonInfo.class));
    }

    @Test
    @DisplayName("SheetCreatedEvent: если lesson/student исчезли — без письма-успехTest")
    void onSheetCreatedSkipsWhenLessonGoneTest() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        listener.onSheetCreated(new SheetCreatedEvent(SHEET_ID, LESSON_ID, STUDENT_ID));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("SheetCancelledByOtherEvent → sendSheetCancelledEmail с инициатором-успехTest")
    void onSheetCancelledByOtherTest() {
        User cancelledBy = new User();
        cancelledBy.setFirstName("Админ");
        cancelledBy.setLastName("Админов");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(userRepository.findById(CANCELLED_BY)).thenReturn(Optional.of(cancelledBy));

        listener.onSheetCancelledByOther(new SheetCancelledByOtherEvent(SHEET_ID, LESSON_ID, STUDENT_ID, CANCELLED_BY));

        verify(emailService).sendSheetCancelledEmail(eq("stud@hse.ru"), eq("Иван"),
                any(LessonInfo.class), eq("Админов Админ"));
    }

    @Test
    @DisplayName("LessonChangedEvent → рассылка всем записанным студентам-успехTest")
    void onLessonChangedNotifiesAllAttendeesTest() {
        Sheet sheet1 = new Sheet();
        sheet1.setStudent(student);

        Student s2 = new Student();
        s2.setEmail("s2@hse.ru");
        s2.setFirstName("Пётр");
        Sheet sheet2 = new Sheet();
        sheet2.setStudent(s2);

        when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet1, sheet2));

        LessonInfo after = new LessonInfo(
                "Йога-2", lessonInfo.startTime(), lessonInfo.endTime(),
                lessonInfo.place(), lessonInfo.campusName(),
                lessonInfo.teacherFullName(), lessonInfo.zoneId()
        );

        listener.onLessonChanged(new LessonChangedEvent(LESSON_ID, lessonInfo, after));

        verify(emailService).sendLessonChangedEmail(eq("stud@hse.ru"), eq("Иван"), eq(lessonInfo), eq(after));
        verify(emailService).sendLessonChangedEmail(eq("s2@hse.ru"), eq("Пётр"), eq(lessonInfo), eq(after));
    }

    @Test
    @DisplayName("LessonCancelledEvent → рассылка всем записанным студентам-успехTest")
    void onLessonCancelledNotifiesAllAttendeesTest() {
        Sheet sheet1 = new Sheet();
        sheet1.setStudent(student);

        when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet1));

        listener.onLessonCancelled(new LessonCancelledEvent(LESSON_ID, lessonInfo));

        verify(emailService, times(1)).sendLessonCancelledEmail(eq("stud@hse.ru"), eq("Иван"), eq(lessonInfo));
    }
}
