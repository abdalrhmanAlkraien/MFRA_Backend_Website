# WhatsApp Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects WhatsApp messaging is needed.
> The agent must check stack.md AND configurations.md FIRST before writing any WhatsApp code.
> The agent must recognize WhatsApp requirements independently — never wait to be told.

---

## Step 0 — Two Checks Before Anything Else

### Check 1 — Does this project use WhatsApp? (stack.md)

Before writing any WhatsApp code, the agent reads `project/stack.md` and checks:

```
Is WhatsApp Business API listed?   → Use WhatsApp Cloud API patterns
Is Twilio WhatsApp listed?         → Use Twilio WhatsApp patterns
Is neither listed?                 → DO NOT add any WhatsApp code
                                     Ignore this file entirely
```

**If no WhatsApp provider is in stack.md — no WhatsApp code is written. No exceptions.**

---

### Check 2 — Is WhatsApp already configured? (configurations.md)

After confirming WhatsApp is in stack.md, the agent reads `.claude/configurations.md`
and checks the WhatsApp Configuration section:

```
Is WhatsApp status ✅ READY?    → Config exists — use it directly, do not re-setup
Is WhatsApp status ⚠️ PARTIAL?  → Complete missing parts, then mark ✅ READY
Is WhatsApp status ❌ MISSING?  → Set up WhatsApp config first, then mark ✅ READY
```

**After completing WhatsApp setup — agent must update configurations.md:**
```
WhatsApp Configuration: ✅ READY
  Provider: WhatsApp Cloud API / Twilio WhatsApp
  Config class: config/WhatsAppConfig.java
  Service class: service/WhatsAppService.java
  Thread pool: whatsappTaskExecutor (core=2, max=5)
  Environment variables: all set ✅
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When WhatsApp Is Needed

### 🔴 Always WhatsApp — Read This File

| Signal in Spec / Task | WhatsApp Required |
|---|---|
| "WhatsApp button" / "talk on WhatsApp" | Contact button linking to WhatsApp |
| "send WhatsApp" / "WhatsApp message" | Programmatic message sending |
| "WhatsApp notification" | User prefers WhatsApp over SMS |
| "MENA clients" + "mobile notification" | WhatsApp is the dominant channel in MENA |
| "consultation request" + "notify via WhatsApp" | Lead notification on WhatsApp |
| "WhatsApp template" / "approved template" | Business-initiated message |
| "WhatsApp OTP" / "code via WhatsApp" | Auth code delivery |

### 🟡 WhatsApp Likely Needed — Evaluate First

| Signal | Evaluate This |
|---|---|
| "mobile notification" + MENA market | WhatsApp preferred over SMS in Saudi, UAE, Jordan |
| "notify client" | Check if WhatsApp is in stack.md — use it if yes |
| "contact us" button | WhatsApp link vs form — check design |

### 🟢 No WhatsApp Needed — Skip This File

| Signal | Why No WhatsApp |
|---|---|
| "send email" | Email — see email.md |
| "SMS OTP" | SMS — see sms.md |
| "push notification" | Push — see notification.md |
| "admin internal alert" | Slack or email — not WhatsApp |

---

## Decision Flowchart

```
Is WhatsApp in stack.md?
   NO  → Stop. No WhatsApp code.
   YES ↓

Is this a static link (wa.me)?
   YES → Simple href link — no backend needed
   NO  ↓

Is this a programmatic message (send message from server)?
   YES → Use WhatsApp Cloud API or Twilio WhatsApp
   NO  ↓

Is it a user-initiated chat widget?
   YES → WhatsApp Chat Widget (frontend only)
```

---

## WhatsApp Use Cases

| Use Case | Implementation | Backend Needed? |
|---|---|---|
| "Contact us on WhatsApp" button | `wa.me` link | ❌ No — frontend only |
| "Chat widget" floating button | `wa.me` deep link | ❌ No — frontend only |
| Send consultation confirmation | Cloud API / Twilio | ✅ Yes |
| Send notification to admin phone | Cloud API / Twilio | ✅ Yes |
| Send OTP via WhatsApp | Cloud API template | ✅ Yes |
| Receive incoming messages | Webhook + Cloud API | ✅ Yes |

---

## Use Case A — Static WhatsApp Link (No Backend)

The simplest and most common case — just a link. No backend needed:

```typescript
// components/WhatsAppButton.tsx
interface WhatsAppButtonProps {
  phone: string;            // E.164 without + e.g. "966501234567"
  message?: string;         // Pre-filled message
  variant?: 'floating' | 'inline';
}

export function WhatsAppButton({
  phone,
  message = 'Hello, I am interested in your AWS services.',
  variant = 'floating',
}: WhatsAppButtonProps) {
  const encodedMessage = encodeURIComponent(message);
  const whatsappUrl = `https://wa.me/${phone}?text=${encodedMessage}`;

  if (variant === 'floating') {
    return (
      <a
        href={whatsappUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="fixed bottom-6 end-6 z-50 flex h-14 w-14 items-center justify-center
                   rounded-full bg-green-500 shadow-lg hover:bg-green-600
                   transition-colors duration-200"
        aria-label="Chat with us on WhatsApp"
        data-testid="whatsapp-floating-btn"
      >
        <WhatsAppIcon className="h-7 w-7 text-white" />
      </a>
    );
  }

  return (
    <a
      href={whatsappUrl}
      target="_blank"
      rel="noopener noreferrer"
      className="inline-flex items-center gap-2 rounded-lg bg-green-500 px-6 py-3
                 font-semibold text-white hover:bg-green-600 transition-colors"
      data-testid="whatsapp-inline-btn"
    >
      <WhatsAppIcon className="h-5 w-5" />
      Talk to Us on WhatsApp
    </a>
  );
}

// WhatsApp SVG icon
function WhatsAppIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15
               -.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075
               -.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059
               -.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52
               .149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52
               -.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51
               -.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372
               -.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074
               .149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625
               .712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413
               .248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421
               7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648
               -.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884
               2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994
               c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0
               0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588
               5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005
               c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0
               00-3.48-8.413z" />
    </svg>
  );
}
```

**Get WhatsApp number from settings API:**
```typescript
// Read from site settings — admin controls the number
const { data: settings } = useGetPublicSettingsQuery();
const whatsappNumber = settings?.contact_whatsapp; // e.g. "966501234567"

<WhatsAppButton phone={whatsappNumber} variant="floating" />
```

---

## Use Case B — Programmatic Messages (Cloud API)

Used when `stack.md` lists `whatsapp-cloud-api` or `whatsapp-business`.

### Dependencies

```xml
<!-- pom.xml — no SDK needed, pure REST -->
<!-- Uses RestTemplate or WebClient already in Spring Boot -->
```

### Configuration

```yaml
# application.yml
app:
  whatsapp:
    provider: ${WHATSAPP_PROVIDER:cloud-api}
    enabled: ${WHATSAPP_ENABLED:false}       # false in dev/test
    cloud-api:
      token: ${WHATSAPP_TOKEN}
      phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID}
      api-url: https://graph.facebook.com/v18.0
      api-version: v18.0

# application-dev.yml
app:
  whatsapp:
    enabled: false   # never send real WhatsApp in dev

# application-test.yml
app:
  whatsapp:
    enabled: false
```

### Environment Variables

```bash
WHATSAPP_PROVIDER=cloud-api
WHATSAPP_ENABLED=true
WHATSAPP_TOKEN=EAAxxxxxxxxxxxxxxxx       # Meta Business token
WHATSAPP_PHONE_NUMBER_ID=1234567890     # From Meta Business Manager
```

### Config Class

```java
// config/WhatsAppConfig.java
@Configuration
public class WhatsAppConfig {

    @Bean(name = "whatsappTaskExecutor")
    public Executor whatsappTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("whatsapp-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate whatsappRestTemplate() {
        return new RestTemplate();
    }
}
```

### WhatsApp Service — Cloud API

```java
// service/WhatsAppService.java
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.whatsapp.enabled", havingValue = "true")
public class WhatsAppService {

    @Qualifier("whatsappRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${app.whatsapp.cloud-api.token}")
    private String token;

    @Value("${app.whatsapp.cloud-api.phone-number-id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.cloud-api.api-url}")
    private String apiUrl;

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    // ─── Send Plain Text Message ──────────────────────────────────

    @Async("whatsappTaskExecutor")
    public void sendTextMessage(String to, String message) {
        if (!enabled) {
            log.info("[WHATSAPP DISABLED] Would send to: {}, message length: {}",
                maskPhone(to), message.length());
            return;
        }
        try {
            String url = apiUrl + "/" + phoneNumberId + "/messages";

            Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", sanitizePhone(to),
                "type", "text",
                "text", Map.of(
                    "preview_url", false,
                    "body", message
                )
            );

            HttpHeaders headers = buildHeaders();
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp text sent to: {}", maskPhone(to));
            } else {
                log.warn("WhatsApp API non-2xx: status={}, to={}",
                    response.getStatusCode(), maskPhone(to));
            }

        } catch (HttpClientErrorException e) {
            log.error("WhatsApp API client error: status={}, body={}, to={}",
                e.getStatusCode(), e.getResponseBodyAsString(), maskPhone(to));
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to: {}", maskPhone(to), e);
        }
    }

    // ─── Send Template Message (Business-Initiated) ───────────────

    @Async("whatsappTaskExecutor")
    public void sendTemplateMessage(String to, String templateName,
                                    String languageCode, List<String> bodyParams) {
        if (!enabled) {
            log.info("[WHATSAPP DISABLED] Would send template: {} to: {}",
                templateName, maskPhone(to));
            return;
        }
        try {
            String url = apiUrl + "/" + phoneNumberId + "/messages";

            List<Map<String, Object>> parameters = bodyParams.stream()
                .map(param -> Map.<String, Object>of("type", "text", "text", param))
                .collect(Collectors.toList());

            Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", sanitizePhone(to),
                "type", "template",
                "template", Map.of(
                    "name", templateName,
                    "language", Map.of("code", languageCode),
                    "components", List.of(
                        Map.of("type", "body", "parameters", parameters)
                    )
                )
            );

            HttpHeaders headers = buildHeaders();
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            log.info("WhatsApp template sent: template={}, to={}",
                templateName, maskPhone(to));

        } catch (Exception e) {
            log.error("Failed to send WhatsApp template: template={}, to={}",
                templateName, maskPhone(to), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String sanitizePhone(String phone) {
        // WhatsApp requires phone without + prefix for Cloud API
        return phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
```

---

## Use Case C — Programmatic Messages (Twilio WhatsApp)

Used when `stack.md` lists `twilio-whatsapp` and Twilio is already the SMS provider.

```java
// service/TwilioWhatsAppService.java
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "twilio")
public class TwilioWhatsAppService {

    @Value("${app.sms.twilio.account-sid}")     // reuse Twilio credentials
    private String accountSid;

    @Value("${app.sms.twilio.auth-token}")
    private String authToken;

    @Value("${app.whatsapp.twilio.from}")        // whatsapp:+14155238886
    private String fromWhatsApp;

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled) Twilio.init(accountSid, authToken);
    }

    @Async("whatsappTaskExecutor")
    public void sendMessage(String to, String message) {
        if (!enabled) {
            log.info("[WHATSAPP DISABLED] Would send to: {}", maskPhone(to));
            return;
        }
        try {
            Message msg = Message.creator(
                new PhoneNumber("whatsapp:" + sanitizePhone(to)),
                new PhoneNumber(fromWhatsApp),
                message
            ).create();
            log.info("WhatsApp sent via Twilio: sid={}, to={}", msg.getSid(), maskPhone(to));
        } catch (Exception e) {
            log.error("Failed to send Twilio WhatsApp to: {}", maskPhone(to), e);
        }
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

## Incoming Webhook (Receive Messages)

When spec requires receiving WhatsApp replies from users:

```java
// controller/WhatsAppWebhookController.java
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    @Value("${app.whatsapp.cloud-api.verify-token}")
    private String verifyToken;

    // ─── Webhook Verification (GET) ───────────────────────────────

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.challenge") String challenge,
        @RequestParam("hub.verify_token") String token
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // ─── Receive Incoming Messages (POST) ─────────────────────────

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            // Parse incoming message
            log.info("WhatsApp webhook received: {}", payload);
            // Process asynchronously — always return 200 immediately
            processWebhookAsync(payload);
        } catch (Exception e) {
            log.error("WhatsApp webhook processing error", e);
        }
        return ResponseEntity.ok().build();   // always 200 — Meta retries on non-200
    }

    @Async("whatsappTaskExecutor")
    public void processWebhookAsync(Map<String, Object> payload) {
        // Extract message and route to appropriate handler
        log.info("Processing WhatsApp webhook payload asynchronously");
    }
}
```

---

## Testing — WhatsApp

### Backend Unit Test

```java
@Test
@DisplayName("sendTextMessage() — disabled — logs and skips API call")
void sendTextMessage_disabled_skipsApiCall() {
    ReflectionTestUtils.setField(whatsAppService, "enabled", false);

    whatsAppService.sendTextMessage("+966501234567", "Test message");

    verifyNoInteractions(restTemplate);
}

@Test
@DisplayName("sendTextMessage() — API throws — exception caught and logged")
void sendTextMessage_apiThrows_exceptionCaught() {
    ReflectionTestUtils.setField(whatsAppService, "enabled", true);
    when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
        .thenThrow(new RestClientException("Network error"));

    assertThatNoException().isThrownBy(() ->
        whatsAppService.sendTextMessage("+966501234567", "Test")
    );
}

@Test
@DisplayName("GET /api/webhooks/whatsapp — valid token — returns challenge")
void verifyWebhook_validToken_returnsChallenge() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/webhooks/whatsapp")
            .param("hub.mode", "subscribe")
            .param("hub.challenge", "test-challenge-123")
            .param("hub.verify_token", "my-verify-token"))
        .andExpect(status().isOk())
        .andExpect(content().string("test-challenge-123"));
}
```

### Frontend Playwright Test

```javascript
// Scenario: WhatsApp floating button is visible and links correctly
await playwright_navigate({ url: 'http://localhost:5173' });
await page.waitForLoadState('networkidle');

const btn = page.locator('[data-testid="whatsapp-floating-btn"]');
const isVisible = await btn.isVisible();
console.log('WhatsApp floating button visible:', isVisible ? '✅' : '❌');

const href = await btn.getAttribute('href');
console.log('Links to wa.me:', href?.includes('wa.me') ? '✅' : `❌ got: ${href}`);
console.log('Has pre-filled message:', href?.includes('text=') ? '✅' : '⚠️');

await playwright_screenshot({ name: 'test-X.Y-whatsapp-button' });
```

---

## ✅ Always Do This

1. Check `stack.md` — only add WhatsApp code if provider is listed
2. Check `configurations.md` — confirm WhatsApp config status before setup
3. Always send messages `@Async("whatsappTaskExecutor")` — never block
4. Always catch exceptions — WhatsApp failure must never break the HTTP response
5. Use `app.whatsapp.enabled=false` in dev and test
6. Log `[WHATSAPP DISABLED]` clearly when skipped
7. Always mask phone numbers in logs — never log full number
8. Always return `200 OK` from webhook immediately — process async
9. Use `@ConditionalOnProperty` on WhatsApp service bean
10. Update `configurations.md` after completing WhatsApp setup

---

## ❌ Never Do This

1. Write WhatsApp code if provider is not in `stack.md`
2. Send WhatsApp messages synchronously — always `@Async`
3. Let WhatsApp failure break the HTTP response — catch and log only
4. Send real WhatsApp messages in dev or test — `app.whatsapp.enabled=false`
5. Log full phone numbers — always mask
6. Return non-200 from webhook endpoint — Meta will keep retrying
7. Hardcode WhatsApp credentials — always environment variables
8. Skip updating `configurations.md` after setup