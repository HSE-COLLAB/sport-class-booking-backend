package ru.hse.sportclassbookingbackend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;
import ru.hse.sportclassbookingbackend.service.mail.EmailService;
import ru.hse.sportclassbookingbackend.service.mail.LessonInfo;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final EmailService emailService;
    private final LessonRepository lessonRepository;
    private final SheetRepository sheetRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final HealthGroupRepository healthGroupRepository;
    private final WorkoutTypeRepository workoutTypeRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Handling UserRegisteredEvent for user {}", event.userId());
        emailService.sendVerificationEmail(event.email(), event.firstName(), event.verificationToken());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSheetCreated(SheetCreatedEvent event) {
        log.debug("Handling SheetCreatedEvent for sheet {}", event.sheetId());
        Lesson lesson = lessonRepository.findById(event.lessonId()).orElse(null);
        Student student = studentRepository.findById(event.studentId()).orElse(null);
        if (lesson == null || student == null) {
            log.warn("SheetCreatedEvent: lesson or student gone, skipping email");
            return;
        }
        emailService.sendSheetCreatedEmail(student.getEmail(), student.getFirstName(), toLessonInfo(lesson));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSheetCancelledByOther(SheetCancelledByOtherEvent event) {
        log.debug("Handling SheetCancelledByOtherEvent for sheet {}", event.sheetId());
        Lesson lesson = lessonRepository.findById(event.lessonId()).orElse(null);
        Student student = studentRepository.findById(event.studentId()).orElse(null);
        User cancelledBy = userRepository.findById(event.cancelledById()).orElse(null);
        if (lesson == null || student == null) {
            log.warn("SheetCancelledByOtherEvent: lesson or student gone, skipping email");
            return;
        }
        String cancelledByName = cancelledBy != null
                ? fullName(cancelledBy)
                : null;
        emailService.sendSheetCancelledEmail(student.getEmail(), student.getFirstName(),
                toLessonInfo(lesson), cancelledByName);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onLessonChanged(LessonChangedEvent event) {
        log.debug("Handling LessonChangedEvent for lesson {}", event.lessonId());
        List<Sheet> sheets = sheetRepository.findAllByLessonIdWithStudent(event.lessonId());
        for (Sheet sheet : sheets) {
            Student s = sheet.getStudent();
            emailService.sendLessonChangedEmail(s.getEmail(), s.getFirstName(), event.before(), event.after());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onHealthGroupChanged(HealthGroupChangedEvent event) {
        log.debug("Handling HealthGroupChangedEvent for student {}", event.studentId());
        var studentOpt = studentRepository.findById(event.studentId());
        if (studentOpt.isEmpty()) {
            log.warn("HealthGroupChangedEvent: student gone, skipping email");
            return;
        }
        var student = studentOpt.get();

        String oldName = event.oldGroupId() != null
                ? healthGroupRepository.findById(event.oldGroupId()).map(HealthGroup::getDescription).orElse("—")
                : "—";
        String newName = healthGroupRepository.findById(event.newGroupId())
                .map(HealthGroup::getDescription).orElse("—");

        List<String> allowedWorkoutTitles = workoutTypeRepository.findAllByIsActiveTrue().stream()
                .filter(wt -> wt.getAllowedHealthGroups().stream()
                        .anyMatch(hg -> hg.getId().equals(event.newGroupId())))
                .map(WorkoutType::getTitle)
                .sorted()
                .toList();

        emailService.sendHealthGroupChangedEmail(
                student.getEmail(), student.getFirstName(),
                oldName, newName, allowedWorkoutTitles
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onLessonCancelled(LessonCancelledEvent event) {
        log.debug("Handling LessonCancelledEvent for lesson {}", event.lessonId());
        List<Sheet> sheets = sheetRepository.findAllByLessonIdWithStudent(event.lessonId());
        for (Sheet sheet : sheets) {
            Student s = sheet.getStudent();
            emailService.sendLessonCancelledEmail(s.getEmail(), s.getFirstName(), event.lesson());
        }
    }

    private LessonInfo toLessonInfo(Lesson lesson) {
        return new LessonInfo(
                lesson.getTitle(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                lesson.getPlace(),
                lesson.getCampus().getName(),
                fullName(lesson.getTeacher()),
                ZoneId.of(lesson.getCampus().getTimezone())
        );
    }

    private String fullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName()).append(' ');
        if (user.getFirstName() != null) sb.append(user.getFirstName()).append(' ');
        if (user.getMiddleName() != null) sb.append(user.getMiddleName());
        return sb.toString().trim();
    }
}
