# Notification Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects push notifications are needed.
> The agent must check stack.md AND configurations.md FIRST before writing any notification code.
> The agent must recognize notification requirements independently — never wait to be told.

---

## Step 0 — Two Checks Before Anything Else

### Check 1 — Does this project use push notifications? (stack.md)

Before writing any notification code, the agent reads `project/stack.md` and checks:

```
Is Firebase (FCM) listed?   → Use Firebase push notification patterns
Is Web Push listed?         → Use Web Push API (VAPID) patterns
Is neither listed?          → DO NOT add any push notification code
                              Ignore this file entirely
```

**If no push provider is in stack.md — no notification code is written. No exceptions.**

---

### Check 2 — Is notification already configured? (configurations.md)

After confirming push is in stack.md, the agent reads `.claude/configurations.md`
and checks the Notification Configuration section:

```
Is Notification status ✅ READY?    → Config exists — use it directly, do not re-setup
Is Notification status ⚠️ PARTIAL?  → Complete missing parts, then mark ✅ READY
Is Notification status ❌ MISSING?  → Set up notification config first, then mark ✅ READY
```

**After completing notification setup — agent must update configurations.md:**
```
Notification Configuration: ✅ READY
  Type: Push (browser/mobile)
  Provider: Firebase FCM / Web Push VAPID
  Config class: config/PushNotificationConfig.java
  Service class: service/PushNotificationService.java
  Thread pool: pushTaskExecutor (core=2, max=5)
  Environment variables: all set ✅
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When Push Notifications Are Needed

### 🔴 Always Push — Read This File

| Signal in Spec / Task | Push Required |
|---|---|
| "push notification" / "browser notification" | Explicit push requirement |
| "notify user on mobile" / "mobile alert" | Mobile push via FCM |
| "real-time alert to browser" | Web push notification |
| "subscribe to notifications" | User opt-in push flow |
| "notify even when app is closed" | Background push — FCM only |
| "send to device" / "device token" | Push via FCM token |
| "permission for notifications" | Browser push permission flow |

### 🟡 Push Likely Needed — Evaluate First

| Signal | Evaluate This |
|---|---|
| "notify user" | Is user on mobile/browser? Push. Otherwise email (email.md) |
| "real-time alert" | Is it in-browser? Push. Is it async summary? Email |
| "remind" | Is it time-sensitive on device? Push. Otherwise email |

### 🟢 No Push Needed — Skip This File

| Signal | Why No Push |
|---|---|
| "send email" | Email — see email.md |
| "admin panel alert" | In-app only — no push needed |
| "SMS" | SMS — see sms.md |
| "log event" | Audit log — not a notification |

---

## Decision Flowchart

```
Does the notification need to reach the user outside the app?
   NO  → Email or in-app — not push
   YES ↓

Is it browser-based (web app)?
   YES → Web Push VAPID or FCM Web
   NO  ↓

Is it mobile (iOS / Android)?
   YES → FCM (Firebase Cloud Messaging)
   NO  → Email is the right channel
```

---

## Push Providers

| Provider | stack.md Signal | When to Use |
|---|---|---|
| **Firebase FCM** | `firebase` or `fcm` | Mobile apps (Android/iOS) + Web |
| **Web Push VAPID** | `webpush` or `vapid` | Browser-only web apps |

---

## Provider 1 — Firebase Cloud Messaging (FCM)

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

### Configuration

```yaml
# application.yml
app:
  push:
    provider: ${PUSH_PROVIDER:fcm}
    enabled: ${PUSH_ENABLED:false}          # false in dev/test by default
    fcm:
      credentials-file: ${FCM_CREDENTIALS_FILE}
      project-id: ${FCM_PROJECT_ID}

# application-dev.yml — never send real push in dev
app:
  push:
    enabled: false

# application-test.yml
app:
  push:
    enabled: false
```

### Config Class

```java
// config/PushNotificationConfig.java
@Configuration
public class PushNotificationConfig {

    @Value("${app.push.fcm.credentials-file:}")
    private String credentialsFile;

    @Value("${app.push.enabled:false}")
    private boolean enabled;

    @Bean
    @ConditionalOnProperty(name = "app.push.enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream(credentialsFile);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean(name = "pushTaskExecutor")
    public Executor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### FCM Push Service

```java
// service/PushNotificationService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenService deviceTokenService;

    @Value("${app.push.enabled:false}")
    private boolean enabled;

    // ─── Send to Single Device ─────────────────────────────────────

    @Async("pushTaskExecutor")
    public void sendToDevice(String deviceToken, String title,
                             String body, String actionUrl) {
        if (!enabled) {
            log.info("[PUSH DISABLED] Would send to: {}, title: {}", maskToken(deviceToken), title);
            return;
        }
        try {
            Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("actionUrl", actionUrl != null ? actionUrl : "")
                .putData("timestamp", Instant.now().toString())
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push sent: messageId={}, token={}", response, maskToken(deviceToken));

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Token expired — remove from DB
                log.warn("FCM token unregistered, removing: {}", maskToken(deviceToken));
                deviceTokenService.deactivateToken(deviceToken);
            } else {
                log.error("FCM error sending to token: {}", maskToken(deviceToken), e);
            }
        } catch (Exception e) {
            log.error("Unexpected error sending push to: {}", maskToken(deviceToken), e);
        }
    }

    // ─── Send to Multiple Devices (Multicast) ─────────────────────

    @Async("pushTaskExecutor")
    public void sendToMultipleDevices(List<String> deviceTokens, String title,
                                      String body, String actionUrl) {
        if (!enabled) {
            log.info("[PUSH DISABLED] Would send to {} devices, title: {}", deviceTokens.size(), title);
            return;
        }
        if (deviceTokens.isEmpty()) return;

        try {
            MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("actionUrl", actionUrl != null ? actionUrl : "")
                .build();

            BatchResponse response = FirebaseMessaging.getInstance()
                .sendEachForMulticast(message);

            log.info("Multicast push: success={}, failure={}, total={}",
                response.getSuccessCount(), response.getFailureCount(), deviceTokens.size());

            // Clean up invalid tokens
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    FirebaseMessagingException ex = responses.get(i).getException();
                    if (ex != null && ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                        deviceTokenService.deactivateToken(deviceTokens.get(i));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed multicast push to {} devices", deviceTokens.size(), e);
        }
    }

    // ─── Send to User (all their devices) ─────────────────────────

    @Async("pushTaskExecutor")
    public void sendToUser(UUID userId, String title, String body, String actionUrl) {
        List<String> tokens = deviceTokenService.getActiveTokensForUser(userId);
        if (tokens.isEmpty()) {
            log.debug("No active push tokens for user: {}", userId);
            return;
        }
        sendToMultipleDevices(tokens, title, body, actionUrl);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
```

---

## Device Token Management

```sql
-- Flyway migration: VX__create_device_tokens.sql
CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(20) NOT NULL,         -- WEB, ANDROID, IOS
    is_active   BOOLEAN DEFAULT TRUE,
    last_used   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_device_tokens_user   ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_active ON device_tokens(user_id, is_active)
    WHERE is_active = TRUE AND deleted_at IS NULL;
```

```java
// service/DeviceTokenService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository tokenRepository;

    @Transactional
    public void registerToken(UUID userId, String token, String platform) {
        // Upsert — token may move between users after re-login
        DeviceTokenEntity entity = tokenRepository
            .findByToken(token)
            .orElse(new DeviceTokenEntity());

        entity.setUserId(userId);
        entity.setToken(token);
        entity.setPlatform(platform);
        entity.setIsActive(true);
        entity.setLastUsed(Instant.now());
        tokenRepository.save(entity);

        log.info("Device token registered: userId={}, platform={}", userId, platform);
    }

    @Transactional
    public void deactivateToken(String token) {
        tokenRepository.findByToken(token).ifPresent(t -> {
            t.setIsActive(false);
            t.setDeletedAt(Instant.now());
            tokenRepository.save(t);
            log.info("Device token deactivated: {}", token.substring(0, 6) + "...");
        });
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokensForUser(UUID userId) {
        return tokenRepository
            .findAllByUserIdAndIsActiveTrueAndDeletedAtIsNull(userId)
            .stream()
            .map(DeviceTokenEntity::getToken)
            .collect(Collectors.toList());
    }
}

// controller/DeviceTokenController.java
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> register(
        @Valid @RequestBody DeviceTokenRequest request,
        @AuthenticationPrincipal UserDetails user
    ) {
        deviceTokenService.registerToken(
            UUID.fromString(user.getUsername()),
            request.getToken(),
            request.getPlatform()
        );
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/deregister")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deregister(
        @Valid @RequestBody DeviceTokenRequest request
    ) {
        deviceTokenService.deactivateToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

---

## Provider 2 — Web Push VAPID

Use when `stack.md` lists `webpush` or `vapid` (browser-only projects).

```xml
<!-- pom.xml -->
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
</dependency>
```

```yaml
# application.yml
app:
  push:
    provider: vapid
    enabled: ${PUSH_ENABLED:false}
    vapid:
      public-key: ${VAPID_PUBLIC_KEY}
      private-key: ${VAPID_PRIVATE_KEY}
      subject: ${VAPID_SUBJECT:mailto:admin@mfra.com}
```

```java
// service/VapidPushService.java
@Service
@ConditionalOnProperty(name = "app.push.provider", havingValue = "vapid")
@Slf4j
public class VapidPushService {

    @Value("${app.push.vapid.public-key}")
    private String publicKey;

    @Value("${app.push.vapid.private-key}")
    private String privateKey;

    @Value("${app.push.vapid.subject}")
    private String subject;

    @Value("${app.push.enabled:false}")
    private boolean enabled;

    private PushService pushService;

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (enabled) {
            pushService = new PushService(publicKey, privateKey, subject);
        }
    }

    @Async("pushTaskExecutor")
    public void sendPush(String endpoint, String p256dh, String auth,
                         String title, String body, String actionUrl) {
        if (!enabled) {
            log.info("[PUSH DISABLED] Would send VAPID push, title: {}", title);
            return;
        }
        try {
            Subscription subscription = new Subscription(endpoint,
                new Subscription.Keys(p256dh, auth));

            String payload = String.format(
                "{\"title\":\"%s\",\"body\":\"%s\",\"actionUrl\":\"%s\"}",
                title, body, actionUrl != null ? actionUrl : ""
            );

            HttpResponse response = pushService.send(new Notification(subscription, payload));
            log.info("VAPID push sent: status={}", response.getStatusLine().getStatusCode());

        } catch (Exception e) {
            log.error("Failed to send VAPID push notification", e);
        }
    }
}
```

---

## Frontend — Push Notification Setup

```typescript
// hooks/usePushNotifications.ts
import { getMessaging, getToken, onMessage } from 'firebase/messaging';
import { useRegisterDeviceTokenMutation } from '@/features/push/api';

export function usePushNotifications() {
  const [registerToken] = useRegisterDeviceTokenMutation();

  const requestPermission = async (): Promise<boolean> => {
    try {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') return false;

      const messaging = getMessaging();
      const token = await getToken(messaging, {
        vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
      });

      if (token) {
        await registerToken({ token, platform: 'WEB' }).unwrap();
        return true;
      }
      return false;
    } catch (error) {
      console.error('Push permission failed:', error);
      return false;
    }
  };

  const listenForeground = (callback: (payload: any) => void) => {
    const messaging = getMessaging();
    return onMessage(messaging, callback);
  };

  return { requestPermission, listenForeground };
}

// components/PushPermissionBanner.tsx
export function PushPermissionBanner() {
  const { requestPermission } = usePushNotifications();
  const [dismissed, setDismissed] = useState(false);

  if (dismissed || Notification.permission !== 'default') return null;

  return (
    <div
      className="fixed bottom-4 end-4 z-50 max-w-sm rounded-xl border border-gray-100
                 bg-white p-4 shadow-lg"
      data-testid="push-permission-banner"
    >
      <p className="text-sm font-medium text-gray-900">Enable Notifications</p>
      <p className="mt-1 text-sm text-gray-500">
        Get notified about important updates
      </p>
      <div className="mt-3 flex gap-2">
        <button
          onClick={requestPermission}
          className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-medium
                     text-white hover:bg-orange-600"
          data-testid="enable-push-btn"
        >
          Enable
        </button>
        <button
          onClick={() => setDismissed(true)}
          className="rounded-lg px-4 py-2 text-sm font-medium text-gray-500
                     hover:bg-gray-100"
          data-testid="dismiss-push-btn"
        >
          Not now
        </button>
      </div>
    </div>
  );
}
```

---

## Environment Variables

```bash
# FCM
PUSH_PROVIDER=fcm
PUSH_ENABLED=true
FCM_CREDENTIALS_FILE=/secrets/firebase-service-account.json
FCM_PROJECT_ID=your-project-id

# Frontend
VITE_FIREBASE_API_KEY=xxx
VITE_FIREBASE_PROJECT_ID=your-project-id
VITE_FIREBASE_MESSAGING_SENDER_ID=xxx
VITE_FIREBASE_APP_ID=xxx
VITE_FIREBASE_VAPID_KEY=xxx

# VAPID
PUSH_PROVIDER=vapid
PUSH_ENABLED=true
VAPID_PUBLIC_KEY=xxx
VAPID_PRIVATE_KEY=xxx
VAPID_SUBJECT=mailto:admin@mfra.com
```

---

## Testing — Push Notifications

### Backend

```java
@Test
@DisplayName("sendToDevice() — push disabled — logs and skips Firebase call")
void sendToDevice_disabled_skipsFirebase() {
    ReflectionTestUtils.setField(pushService, "enabled", false);
    pushService.sendToDevice("fake-token", "Title", "Body", "/url");
    verifyNoInteractions(firebaseMessagingMock);
}

@Test
@DisplayName("sendToDevice() — exception thrown — caught and logged, no rethrow")
void sendToDevice_exceptionThrown_caughtSilently() {
    ReflectionTestUtils.setField(pushService, "enabled", true);
    assertThatNoException().isThrownBy(() ->
        pushService.sendToDevice("bad-token", "Title", "Body", null)
    );
}

@Test
@DisplayName("POST /api/push/register — 200 — token saved to DB")
void registerToken_authenticated_returns200AndSavesToken() throws Exception {
    mockMvc.perform(adminPost("/api/push/register",
            new DeviceTokenRequest("fcm-token-xyz", "WEB")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    assertThat(deviceTokenRepository.findByToken("fcm-token-xyz")).isPresent();
}
```

### Frontend Playwright

```javascript
// Mock browser Notification API
await page.addInitScript(() => {
  Object.defineProperty(window, 'Notification', {
    value: { permission: 'default', requestPermission: async () => 'granted' },
    writable: true,
  });
});

await playwright_navigate({ url: 'http://localhost:5173' });
await page.waitForLoadState('networkidle');

const banner = page.locator('[data-testid="push-permission-banner"]');
console.log('Permission banner shown:', await banner.isVisible() ? '✅' : '❌');
await playwright_screenshot({ name: 'test-X.Y-push-banner' });

await playwright_click({ selector: '[data-testid="enable-push-btn"]' });
await playwright_screenshot({ name: 'test-X.Y-push-enabled' });
```

---

## ✅ Always Do This

1. Check `stack.md` — only write push code if Firebase or WebPush is listed
2. Check `configurations.md` — confirm push config status before setup
3. Always send push `@Async("pushTaskExecutor")` — never block the caller
4. Always catch exceptions — push failure must never break the HTTP response
5. Use `app.push.enabled=false` in dev and test — never send real pushes
6. Log `[PUSH DISABLED]` clearly when push is skipped
7. Handle `UNREGISTERED` FCM error — always deactivate the stale token in DB
8. Use `@ConditionalOnProperty` on provider beans — clean disable
9. Mask FCM tokens in all logs — never log full token
10. Update `configurations.md` after completing push setup

---

## ❌ Never Do This

1. Write push code if provider is not in `stack.md`
2. Send push synchronously — always `@Async`
3. Let push failure throw to the HTTP response — catch and log only
4. Send real pushes in dev or tests — `app.push.enabled=false` always
5. Log full FCM tokens — always mask
6. Skip `UNREGISTERED` error handling — stale tokens waste quota
7. Hardcode Firebase credentials — always use env vars + service account file
8. Skip updating `configurations.md` after push setup