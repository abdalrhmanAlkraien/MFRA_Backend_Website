# Email Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects email sending is needed.
> The agent must check configurations.md FIRST — if email config is not ready, set it up before writing any email code.

---

## Step 0 — Check configurations.md Before Anything Else

Before writing any email code, the agent reads `.claude/configurations.md` and checks:

```
Is email configuration listed as ✅ READY?   → Proceed with email patterns below
Is email configuration listed as ❌ MISSING? → Set up email config first, then mark ✅ READY
Is email not listed at all?                  → Set up email config, add to configurations.md
```

**Never write email code without confirming email config is ready in configurations.md.**

---

## Agent Decision Rule — When Email Is Needed

The agent reads this file automatically when it detects ANY of these signals
in the spec or task:

### 🔴 Always Send Email — Read This File

| Signal in Spec / Task | Email Required |
|---|---|
| "send confirmation" / "confirmation email" | User confirms their action |
| "notify admin" / "notify team" / "alert team" | Internal notification |
| "free consultation" / "contact form" | Lead submission notification |
| "password reset" / "forgot password" | Auth flow email |
| "welcome email" / "onboarding" | New user/client onboarding |
| "invoice" / "receipt" / "order confirmation" | Transactional email |
| "weekly digest" / "monthly report" | Scheduled email |
| "status changed" / "request updated" | Status change notification |
| "export ready" / "job completed" | Async job completion notification |
| "account activated" / "email verified" | Verification email |

### 🟡 Email Likely Needed — Evaluate First

| Signal | Evaluate This |
|---|---|
| "notify" without specifying channel | Check if email is the right channel |
| "send update" | Check if real-time notification or email |
| "alert" | Check severity — critical = email, low = in-app only |

### 🟢 No Email Needed — Skip This File

| Signal | Why No Email |
|---|---|
| "save to DB" / "create record" | Pure data operation |
| "update status" (admin only) | Internal tool action |
| "display notification" | In-app notification only |
| "log event" | Audit logging, not communication |

---

## Decision Flowchart

```
Does the action communicate something to a human outside the system?
   NO  → No email needed
   YES ↓

Is the recipient a client / external user?
   YES → Transactional email (confirmation, verification, receipt)
   NO  ↓

Is the recipient an internal team member?
   YES → Internal notification email (new lead, status change, alert)
   NO  ↓

Is it time-based (digest, report)?
   YES → Scheduled email — see async.md for scheduling
```

---

## Email Configuration Setup

### Step 1 — Add Dependencies (pom.xml)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Optional: Thymeleaf for HTML email templates -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

---

### Step 2 — application.yml Configuration

```yaml
# application.yml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
        debug: false                    # set true only in dev for troubleshooting

# application-dev.yml — use Mailhog locally (no real emails sent)
spring:
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

# application-test.yml — disable email in tests
spring:
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""

app:
  email:
    from: ${MAIL_FROM:noreply@mfra.com}
    from-name: ${MAIL_FROM_NAME:MFRA Team}
    admin: ${ADMIN_EMAIL:admin@mfra.com}
    reply-to: ${MAIL_REPLY_TO:hello@mfra.com}
```

---

### Step 3 — Environment Variables

Add these to `.env` and document in `configurations.md`:

```bash
# .env (never commit this file)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password       # Gmail App Password, not account password
MAIL_FROM=noreply@mfra.com
MAIL_FROM_NAME=MFRA Team
ADMIN_EMAIL=admin@mfra.com
MAIL_REPLY_TO=hello@mfra.com
```

---

### Step 4 — EmailConfig Bean

```java
// config/EmailConfig.java
@Configuration
public class EmailConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

> **Note:** If `async.md` has already set up `emailTaskExecutor` — do not duplicate it.
> Check `configurations.md` to see if async config is already prepared.

---

### Step 5 — Mark Configuration Ready

After completing setup, update `.claude/configurations.md`:

```
Email Configuration: ✅ READY
  Provider: Gmail SMTP / SendGrid / AWS SES
  From: noreply@mfra.com
  Templates: Plain text + HTML (Thymeleaf)
  Thread pool: emailTaskExecutor (core=2, max=5)
```

---

## Email Service Implementation

### Base Email Service

```java
// service/EmailService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;    // Thymeleaf — null if not using templates

    @Value("${app.email.from}")
    private String fromAddress;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.admin}")
    private String adminEmail;

    @Value("${app.email.reply-to}")
    private String replyTo;

    // ─── Plain Text Email ──────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendPlainText(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setReplyTo(replyTo);

            mailSender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email: to={}, subject={}", to, subject, e);
            // Never rethrow from @Async void — log and move on
        }
    }

    // ─── HTML Email (Thymeleaf template) ──────────────────────────

    @Async("emailTaskExecutor")
    public void sendHtml(String to, String subject, String templateName,
                         Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setReplyTo(replyTo);

            // Render Thymeleaf template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);    // true = isHtml

            mailSender.send(message);
            log.info("HTML email sent: to={}, subject={}, template={}", to, subject, templateName);

        } catch (Exception e) {
            log.error("Failed to send HTML email: to={}, subject={}", to, subject, e);
        }
    }

    // ─── Internal Admin Notification ──────────────────────────────

    @Async("emailTaskExecutor")
    public void notifyAdmin(String subject, String body) {
        sendPlainText(adminEmail, "[MFRA] " + subject, body);
    }
}
```

---

## Email Templates — Per Use Case

### Use Case 1 — Consultation Request Confirmation

Triggered by: `POST /api/public/consultation`

```java
// service/ConsultationEmailService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationEmailService {

    private final EmailService emailService;

    // To client — confirms we received their request
    @Async("emailTaskExecutor")
    public void sendClientConfirmation(ConsultationEntity consultation) {
        String subject = "MFRA — We received your consultation request";
        String body = String.format("""
            Dear %s,

            Thank you for reaching out to MFRA (Migration For Real Architecture).

            We have received your consultation request from %s.

            What happens next:
            1. Our AWS engineers will review your submission
            2. We will contact you within 24 hours to schedule a discovery call
            3. We will propose a tailored solution for your needs

            Your request details:
            - Services: %s
            - Timeline: %s
            - Infrastructure: %s

            If you have any urgent questions, reply to this email or reach us on WhatsApp.

            Best regards,
            The MFRA Team
            https://mfra.com
            """,
            consultation.getFullName(),
            consultation.getCompanyName(),
            String.join(", ", consultation.getServicesInterested()),
            consultation.getProjectTimeline(),
            consultation.getCurrentInfrastructure()
        );
        emailService.sendPlainText(consultation.getWorkEmail(), subject, body);
    }

    // To admin — new lead notification
    @Async("emailTaskExecutor")
    public void sendAdminNotification(ConsultationEntity consultation) {
        String subject = "New Consultation Request — " + consultation.getCompanyName();
        String body = String.format("""
            New consultation request received.

            ─── Client Details ───────────────────
            Name:        %s
            Title:       %s
            Company:     %s
            Email:       %s
            Phone:       %s
            Country:     %s
            Company Size: %s

            ─── Project Details ──────────────────
            Current Infrastructure: %s
            Services Interested:    %s
            Project Timeline:       %s

            ─── Challenge ────────────────────────
            %s

            ─── Action Required ──────────────────
            Review and contact within 24 hours.
            Admin panel: https://mfra.com/admin/consultations
            """,
            consultation.getFullName(),
            consultation.getJobTitle(),
            consultation.getCompanyName(),
            consultation.getWorkEmail(),
            consultation.getPhone(),
            consultation.getCountry(),
            consultation.getCompanySize(),
            consultation.getCurrentInfrastructure(),
            String.join(", ", consultation.getServicesInterested()),
            consultation.getProjectTimeline(),
            consultation.getChallengeDescription()
        );
        emailService.notifyAdmin(subject, body);
    }
}
```

---

### Use Case 2 — Contact Form Notification

Triggered by: `POST /api/public/contact`

```java
// service/ContactEmailService.java
@Service
@RequiredArgsConstructor
public class ContactEmailService {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    public void notifyAdminOfNewMessage(ContactMessageEntity message) {
        String subject = "New Contact Message — " + message.getSubject();
        String body = String.format("""
            New contact message received.

            From:    %s
            Email:   %s
            Subject: %s

            Message:
            ─────────────────────────────
            %s
            ─────────────────────────────

            Reply directly to this email to respond to %s.
            Admin panel: https://mfra.com/admin/messages
            """,
            message.getFullName(),
            message.getEmail(),
            message.getSubject(),
            message.getMessage(),
            message.getFullName()
        );
        emailService.notifyAdmin(subject, body);
    }
}
```

---

### Use Case 3 — Async Job Completion

Triggered when: export job, report generation, or bulk operation completes.

```java
// service/JobEmailService.java
@Service
@RequiredArgsConstructor
public class JobEmailService {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    public void notifyJobCompleted(String recipientEmail, String jobType,
                                   String downloadUrl) {
        String subject = "Your " + jobType + " is ready";
        String body = String.format("""
            Your %s has been completed successfully.

            Download your file here:
            %s

            This link will be available for 24 hours.

            If you did not request this export, please contact us immediately.

            Best regards,
            The MFRA Team
            """,
            jobType,
            downloadUrl
        );
        emailService.sendPlainText(recipientEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void notifyJobFailed(String recipientEmail, String jobType, String reason) {
        String subject = "Your " + jobType + " failed";
        String body = String.format("""
            Unfortunately, your %s could not be completed.

            Reason: %s

            Please try again or contact our support team if the issue persists.

            Best regards,
            The MFRA Team
            """,
            jobType,
            reason
        );
        emailService.sendPlainText(recipientEmail, subject, body);
    }
}
```

---

## HTML Email Templates (Thymeleaf)

Create templates under `src/main/resources/templates/email/`:

```html
<!-- templates/email/consultation-confirmation.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Consultation Request Received</title>
  <style>
    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
    .header { background-color: #0f172a; color: white; padding: 24px; border-radius: 8px 8px 0 0; }
    .header h1 { margin: 0; font-size: 24px; }
    .accent { color: #f97316; }
    .content { background: #ffffff; padding: 24px; border: 1px solid #e5e7eb; }
    .detail-row { padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
    .label { font-weight: bold; color: #6b7280; font-size: 13px; }
    .steps { background: #f9fafb; padding: 16px; border-radius: 6px; margin: 16px 0; }
    .step { display: flex; gap: 12px; margin: 8px 0; }
    .step-num { background: #f97316; color: white; border-radius: 50%;
                width: 24px; height: 24px; display: flex;
                align-items: center; justify-content: center; font-weight: bold; }
    .footer { background: #f9fafb; padding: 16px; border-radius: 0 0 8px 8px;
              text-align: center; font-size: 13px; color: #9ca3af; }
    .btn { display: inline-block; background: #f97316; color: white;
           padding: 12px 24px; border-radius: 6px; text-decoration: none;
           font-weight: bold; margin: 16px 0; }
  </style>
</head>
<body>
<div class="container">
  <div class="header">
    <h1>MFRA <span class="accent">—</span> Migration For Real Architecture</h1>
  </div>
  <div class="content">
    <h2>We received your consultation request, <span th:text="${name}">Ahmed</span>!</h2>
    <p>Thank you for reaching out to MFRA. Our AWS engineers will review your submission
       and contact you within <strong>24 hours</strong>.</p>

    <div class="steps">
      <div class="step"><div class="step-num">1</div>
        <div>Our engineers review your infrastructure details</div></div>
      <div class="step"><div class="step-num">2</div>
        <div>We schedule a 30-minute discovery call</div></div>
      <div class="step"><div class="step-num">3</div>
        <div>We propose a tailored AWS solution</div></div>
    </div>

    <h3>Your Request Summary</h3>
    <div class="detail-row">
      <div class="label">Company</div>
      <div th:text="${company}">TechCorp</div>
    </div>
    <div class="detail-row">
      <div class="label">Services Interested</div>
      <div th:text="${services}">Cloud Migration, AWS Infrastructure</div>
    </div>
    <div class="detail-row">
      <div class="label">Project Timeline</div>
      <div th:text="${timeline}">1-3 months</div>
    </div>
    <div class="detail-row">
      <div class="label">Current Infrastructure</div>
      <div th:text="${infrastructure}">On-Premise</div>
    </div>

    <p style="margin-top: 24px;">
      Have an urgent question? Reply directly to this email or
      <a href="https://wa.me/your-number" class="btn">WhatsApp Us</a>
    </p>
  </div>
  <div class="footer">
    <p>© 2025 MFRA — Migration For Real Architecture</p>
    <p>This email was sent because you submitted a consultation request on mfra.com</p>
  </div>
</div>
</body>
</html>
```

---

## Local Development — Mailhog Setup

Use Mailhog to catch all emails locally — no real emails sent during development:

```yaml
# docker-compose.yml — add Mailhog service
services:
  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"   # SMTP — Spring Boot sends to this
      - "8025:8025"   # Web UI — view emails at http://localhost:8025
    restart: unless-stopped
```

```yaml
# application-dev.yml
spring:
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

Open `http://localhost:8025` to see all caught emails during development.

---

## Testing Email

### Unit Test — Email Service Called (Mock JavaMailSender)

```java
// test: EmailServiceTest.java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;

    @InjectMocks private EmailService emailService;

    @Test
    @DisplayName("sendPlainText() — valid params — sends email via mailSender")
    void sendPlainText_validParams_callsMailSender() {
        emailService.sendPlainText(
            "client@example.com",
            "Test Subject",
            "Test body content"
        );

        verify(mailSender).send(argThat((SimpleMailMessage msg) ->
            msg.getTo()[0].equals("client@example.com") &&
            msg.getSubject().equals("Test Subject") &&
            msg.getText().contains("Test body content")
        ));
    }

    @Test
    @DisplayName("sendPlainText() — mailSender throws — exception is caught and logged")
    void sendPlainText_mailSenderThrows_exceptionCaught() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Must NOT throw — exception must be swallowed and logged
        assertThatNoException().isThrownBy(() ->
            emailService.sendPlainText("to@test.com", "Subject", "Body")
        );
    }
}
```

### Integration Test — Email Triggered by Controller

```java
@Test
@DisplayName("POST /api/public/consultation — 201 — triggers confirmation and admin emails")
void submitConsultation_validRequest_triggersEmails() throws Exception {
    ConsultationCreateRequest request = factory.validConsultationRequest();

    mockMvc.perform(publicPost("/api/public/consultation", request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true));

    // Verify async email methods called — they run in background thread
    verify(consultationEmailService, timeout(3000).times(1))
        .sendClientConfirmation(any(ConsultationEntity.class));

    verify(consultationEmailService, timeout(3000).times(1))
        .sendAdminNotification(any(ConsultationEntity.class));
}

@Test
@DisplayName("POST /api/public/consultation — email failure — 201 still returned")
void submitConsultation_emailFails_stillReturns201() throws Exception {
    // Email failure must NOT affect the HTTP response
    doThrow(new RuntimeException("SMTP down"))
        .when(emailService).sendPlainText(anyString(), anyString(), anyString());

    ConsultationCreateRequest request = factory.validConsultationRequest();

    // Must still return 201 — email is async, failure is non-fatal
    mockMvc.perform(publicPost("/api/public/consultation", request))
        .andExpect(status().isCreated());
}
```

---

## ✅ Always Do This

1. Check `configurations.md` first — confirm email config is ready before writing code
2. Always send email asynchronously — `@Async("emailTaskExecutor")` on every email method
3. Always catch exceptions inside `@Async` email methods — never let them propagate
4. Always use environment variables for SMTP credentials — never hardcode
5. Always use Mailhog in local dev — never send real emails during development
6. Always include `app.email.from`, `app.email.admin`, and `app.email.reply-to` in config
7. Always use plain text as fallback — HTML template is optional enhancement
8. Always log `log.info` on success and `log.error` on failure
9. Always update `configurations.md` after setting up email config
10. Always test that email failure does NOT break the HTTP response

---

## ❌ Never Do This

1. Write email code without checking `configurations.md` first
2. Send email synchronously — always `@Async`
3. Throw exceptions from `@Async` email methods — catch and log only
4. Hardcode SMTP credentials — always environment variables
5. Send real emails in tests — use mock `JavaMailSender`
6. Send real emails in development — use Mailhog
7. Skip the Mailhog setup — every project needs local email catching
8. Include sensitive data (passwords, tokens) in email body
9. Send emails from the controller layer — always through a dedicated email service
10. Forget to update `configurations.md` after completing email setup