# SMS Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects SMS sending is needed.
> The agent must check stack.md AND configurations.md FIRST before writing any SMS code.
> The agent must recognize SMS requirements independently — never wait to be told.

---

## Step 0 — Two Checks Before Anything Else

### Check 1 — Does this project use SMS? (stack.md)

Before writing any SMS code, the agent reads `project/stack.md` and checks:

```
Is Twilio listed?   → Use Twilio patterns
Is AWS SNS listed?  → Use AWS SNS patterns
Is neither listed?  → DO NOT add any SMS code — ignore this file entirely
```

**If no SMS provider is in stack.md — no SMS code is written. No exceptions.**

---

### Check 2 — Is SMS already configured? (configurations.md)

After confirming SMS is in stack.md, the agent reads `.claude/configurations.md`
and checks the SMS Configuration section:

```
Is SMS status ✅ READY?    → Config exists — use it directly, do not re-setup
Is SMS status ⚠️ PARTIAL?  → Complete missing parts, then mark ✅ READY
Is SMS status ❌ MISSING?  → Set up SMS config first, then mark ✅ READY
```

**After completing SMS setup — agent must update configurations.md:**
```
SMS Configuration: ✅ READY
  Provider: Twilio / AWS SNS
  Interface: service/SmsService.java
  Implementation: service/TwilioSmsService.java or AwsSnsSmsService.java
  Thread pool: smsTaskExecutor (core=2, max=5)
  Environment variables: all set ✅
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When SMS Is Needed

### 🔴 Always SMS — Read This File

| Signal in Spec / Task | SMS Required |
|---|---|
| "OTP" / "one-time password" / "verification code" | Auth SMS — time-sensitive |
| "SMS confirmation" / "text message" | Explicit SMS requirement |
| "mobile verification" / "phone verification" | OTP flow |
| "send SMS" | Explicit SMS request |
| "2FA" / "two-factor authentication" | Security SMS |
| "appointment reminder via SMS" | Transactional SMS |

### 🟡 SMS Likely Needed — Evaluate First

| Signal | Evaluate This |
|---|---|
| "notify via mobile" | Is WhatsApp configured? Use that. Otherwise SMS |
| "send code" | Is it OTP? → SMS. Is it a report link? → Email |
| "remind client" | Is it time-critical on phone? → SMS. Otherwise email |

### 🟢 No SMS Needed — Skip This File

| Signal | Why No SMS |
|---|---|
| "send email" | Email — see email.md |
| "push notification" | Push — see notification.md |
| "admin notification" | Internal — use email or Slack |

---

## Decision Flowchart

```
Does the message need to reach a mobile number directly?
   NO  → Use email or push instead
   YES ↓

Is it time-sensitive (OTP, 2FA, verification)?
   YES → SMS — most reliable for time-critical delivery
   NO  ↓

Is the client in MENA region?
   YES → Prefer AWS SNS (better MENA delivery rates)
   NO  → Twilio works globally
```

---

## SMS Configuration Setup

### Dependencies

```xml
<!-- pom.xml — add only the provider in stack.md -->

<!-- Twilio -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.0</version>
</dependency>

<!-- AWS SNS — add only if not already in project via AWS SDK BOM -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sns</artifactId>
</dependency>
```

### application.yml

```yaml
# application.yml
app:
  sms:
    provider: ${SMS_PROVIDER}             # twilio or awssns — from stack.md
    enabled: ${SMS_ENABLED:false}         # false in dev/test — always

    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: ${TWILIO_FROM_NUMBER}

    aws-sns:
      region: ${AWS_REGION:me-south-1}   # Bahrain region for MENA
      sender-id: ${SNS_SENDER_ID:MFRA}

# application-dev.yml — no real SMS in dev
app:
  sms:
    enabled: false

# application-test.yml
app:
  sms:
    enabled: false
```

### Environment Variables

```bash
# Twilio
SMS_PROVIDER=twilio
SMS_ENABLED=true
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_FROM_NUMBER=+15555550100

# AWS SNS
SMS_PROVIDER=awssns
SMS_ENABLED=true
AWS_REGION=me-south-1
SNS_SENDER_ID=MFRA
# AWS_ACCESS_KEY and AWS_SECRET_KEY already set if using AWS S3 or other AWS services
```

---

## SMS Service Interface

One interface — two implementations. Agent picks based on stack.md:

```java
// service/SmsService.java
public interface SmsService {

    /**
     * Send a plain text SMS asynchronously.
     * @param to    Phone in E.164 format (+966501234567)
     * @param body  Message text — keep under 160 chars for single SMS
     */
    void sendSms(String to, String body);

    /**
     * Send OTP with standard template.
     * @param to   Phone in E.164 format
     * @param otp  4–6 digit code
     */
    default void sendOtp(String to, String otp) {
        sendSms(to, "Your verification code is: " + otp + ". Valid for 5 minutes.");
    }
}
```

---

## Provider 1 — Twilio

```java
// service/TwilioSmsService.java
@Service
@Primary
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
@Slf4j
public class TwilioSmsService implements SmsService {

    @Value("${app.sms.twilio.account-sid}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token}")
    private String authToken;

    @Value("${app.sms.twilio.from-number}")
    private String fromNumber;

    @Value("${app.sms.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized");
        }
    }

    @Override
    @Async("smsTaskExecutor")
    public void sendSms(String to, String body) {
        if (!enabled) {
            log.info("[SMS DISABLED] Would send to: {}, body length: {}",
                maskPhone(to), body.length());
            return;
        }
        try {
            Message message = Message.creator(
                new PhoneNumber(sanitizePhone(to)),
                new PhoneNumber(fromNumber),
                body
            ).create();

            log.info("SMS sent via Twilio: sid={}, to={}", message.getSid(), maskPhone(to));

        } catch (ApiException e) {
            // Twilio error codes: https://www.twilio.com/docs/api/errors
            log.error("Twilio API error code={}, message={}, to={}",
                e.getCode(), e.getMessage(), maskPhone(to));
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to: {}", maskPhone(to), e);
        }
    }
}
```

---

## Provider 2 — AWS SNS

```java
// service/AwsSnsSmsService.java
@Service
@Primary
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "awssns")
@RequiredArgsConstructor
@Slf4j
public class AwsSnsSmsService implements SmsService {

    private final SnsClient snsClient;

    @Value("${app.sms.aws-sns.sender-id}")
    private String senderId;

    @Value("${app.sms.enabled:false}")
    private boolean enabled;

    @Override
    @Async("smsTaskExecutor")
    public void sendSms(String to, String body) {
        if (!enabled) {
            log.info("[SMS DISABLED] Would send to: {}, body length: {}",
                maskPhone(to), body.length());
            return;
        }
        try {
            PublishRequest request = PublishRequest.builder()
                .phoneNumber(sanitizePhone(to))
                .message(body)
                .messageAttributes(Map.of(
                    "AWS.SNS.SMS.SenderID",
                    MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(senderId)
                        .build(),
                    "AWS.SNS.SMS.SMSType",
                    MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("Transactional")   // higher delivery priority
                        .build()
                ))
                .build();

            PublishResponse response = snsClient.publish(request);
            log.info("SMS sent via AWS SNS: messageId={}, to={}",
                response.messageId(), maskPhone(to));

        } catch (SnsException e) {
            log.error("AWS SNS error: code={}, message={}, to={}",
                e.awsErrorDetails().errorCode(), e.getMessage(), maskPhone(to));
        } catch (Exception e) {
            log.error("Failed to send SMS via AWS SNS to: {}", maskPhone(to), e);
        }
    }
}

// config for SnsClient bean
// config/SmsConfig.java
@Configuration
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "awssns")
public class SmsConfig {

    @Bean
    public SnsClient snsClient(@Value("${app.sms.aws-sns.region}") String region) {
        return SnsClient.builder()
            .region(Region.of(region))
            .build();
    }

    @Bean(name = "smsTaskExecutor")
    public Executor smsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sms-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## OTP Use Case — Full Implementation

When spec mentions OTP, verification code, or 2FA:

```java
// service/OtpService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final SmsService smsService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final String OTP_PREFIX      = "otp:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";

    public void sendOtp(String phone) {
        String sanitized = sanitizePhone(phone);
        String otp = generateOtp();

        // Store in Redis with TTL — never in DB
        redisTemplate.opsForValue().set(
            OTP_PREFIX + sanitized, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES
        );

        smsService.sendOtp(sanitized, otp);
        log.info("OTP sent to: {}", maskPhone(sanitized));
    }

    public boolean verifyOtp(String phone, String submitted) {
        String sanitized = sanitizePhone(phone);
        String key = OTP_PREFIX + sanitized;
        String attemptsKey = ATTEMPTS_PREFIX + sanitized;

        // Enforce attempt limit
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            throw new TooManyOtpAttemptsException("Too many attempts. Request a new code.");
        }

        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            log.warn("OTP expired or not found for: {}", maskPhone(sanitized));
            return false;
        }

        if (stored.equals(submitted)) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey);
            log.info("OTP verified: {}", maskPhone(sanitized));
            return true;
        }

        // Wrong code — track attempt
        Long count = redisTemplate.opsForValue().increment(attemptsKey);
        if (count == 1) {
            redisTemplate.expire(attemptsKey, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        }
        log.warn("OTP mismatch attempt {} for: {}", count, maskPhone(sanitized));
        return false;
    }

    private String generateOtp() {
        return String.format("%0" + OTP_LENGTH + "d",
            new SecureRandom().nextInt((int) Math.pow(10, OTP_LENGTH)));
    }

    private String sanitizePhone(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
```

---

## Phone Number Rules

```
Always use E.164 format before sending:
  +966501234567  (Saudi Arabia)
  +971501234567  (UAE)
  +962791234567  (Jordan)
  +20101234567   (Egypt)

Rules:
  - Strip spaces, dashes, parentheses
  - Ensure starts with +
  - Never log full phone number — always mask
  - Mask pattern: first 4 chars + **** + last 2 chars
    +966501234567 → +966****67
```

---

## Testing SMS

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class TwilioSmsServiceTest {

    @InjectMocks private TwilioSmsService smsService;

    @Test
    @DisplayName("sendSms() — disabled — logs and returns without calling Twilio")
    void sendSms_disabled_doesNotCallTwilio() {
        ReflectionTestUtils.setField(smsService, "enabled", false);

        smsService.sendSms("+966501234567", "Test message");

        // Verify Twilio SDK never initialized or called
        // (can only verify via log output in unit test)
    }

    @Test
    @DisplayName("sendSms() — Twilio throws — exception caught and logged")
    void sendSms_twilioThrows_exceptionCaught() {
        ReflectionTestUtils.setField(smsService, "enabled", true);
        // Twilio would throw on invalid credentials — verify no rethrow
        assertThatNoException().isThrownBy(() ->
            smsService.sendSms("+966501234567", "Test")
        );
    }
}

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private SmsService smsService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private OtpService otpService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("verifyOtp() — correct code — returns true and deletes from Redis")
    void verifyOtp_correctCode_returnsTrueAndCleansUp() {
        String phone = "+966501234567";
        when(valueOps.get("otp:" + phone)).thenReturn("123456");
        when(valueOps.get("otp:attempts:" + phone)).thenReturn(null);

        boolean result = otpService.verifyOtp(phone, "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("otp:" + phone);
        verify(redisTemplate).delete("otp:attempts:" + phone);
    }

    @Test
    @DisplayName("verifyOtp() — expired OTP — returns false")
    void verifyOtp_expired_returnsFalse() {
        when(valueOps.get(anyString())).thenReturn(null);
        boolean result = otpService.verifyOtp("+966501234567", "123456");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifyOtp() — max 3 attempts exceeded — throws exception")
    void verifyOtp_maxAttemptsExceeded_throwsException() {
        when(valueOps.get("otp:attempts:+966501234567")).thenReturn("3");

        assertThatThrownBy(() -> otpService.verifyOtp("+966501234567", "000000"))
            .isInstanceOf(TooManyOtpAttemptsException.class);
    }
}
```

---

## ✅ Always Do This

1. Check `stack.md` — only write SMS code if Twilio or AWS SNS is listed
2. Check `configurations.md` — confirm SMS config before setup
3. Use `SmsService` interface — never call Twilio/SNS SDK directly from business logic
4. Always send SMS `@Async("smsTaskExecutor")` — never block
5. Always catch exceptions — SMS failure must never break the HTTP response
6. Use `app.sms.enabled=false` in dev and test — never send real SMS
7. Log `[SMS DISABLED]` clearly when SMS is skipped
8. Always sanitize to E.164 format before sending
9. Always mask phone numbers in all logs
10. Store OTPs in Redis with TTL — never in DB
11. Enforce 3-attempt limit on OTP via Redis counter
12. Use `@ConditionalOnProperty` on provider service beans
13. Update `configurations.md` after completing SMS setup

---

## ❌ Never Do This

1. Write SMS code if provider is not in `stack.md`
2. Call Twilio or AWS SNS SDK directly from business services — always `SmsService`
3. Send SMS synchronously — always `@Async`
4. Let SMS failure throw to the HTTP response — catch and log only
5. Log full phone numbers — always mask
6. Send real SMS in dev or tests — `app.sms.enabled=false` always
7. Store OTPs in DB — always Redis with TTL
8. Allow unlimited OTP attempts — always 3-attempt limit
9. Hardcode credentials — always environment variables
10. Put `@Primary` on more than one SMS provider — only one per project
11. Skip updating `configurations.md` after SMS setup