# WebSocket Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects real-time communication is needed.
> The agent must check stack.md AND configurations.md FIRST before writing any WebSocket code.
> The agent must recognize WebSocket requirements independently — never wait to be told.

---

## Step 0 — Two Checks Before Anything Else

### Check 1 — Does this project use WebSocket? (stack.md)

Before writing any WebSocket code, the agent reads `project/stack.md` and checks:

```
Is WebSocket listed?        → Use Spring WebSocket + STOMP patterns
Is Socket.IO listed?        → Use Socket.IO patterns
Is Server-Sent Events listed? → Use SSE patterns (simpler alternative)
Is none listed?             → DO NOT add any real-time code
                              Ignore this file entirely
```

**If no real-time technology is in stack.md — no WebSocket code is written. No exceptions.**

---

### Check 2 — Is WebSocket already configured? (configurations.md)

After confirming WebSocket is in stack.md, the agent reads `.claude/configurations.md`
and checks the WebSocket Configuration section:

```
Is WebSocket status ✅ READY?    → Config exists — use it directly, do not re-setup
Is WebSocket status ⚠️ PARTIAL?  → Complete missing parts, then mark ✅ READY
Is WebSocket status ❌ MISSING?  → Set up WebSocket config first, then mark ✅ READY
```

**Also check if Async config is ready — WebSocket requires @EnableAsync:**
```
Is Async status ✅ READY?  → Good — WebSocket can use the same async infrastructure
Is Async status ❌ MISSING? → Set up Async config first (instructions/async.md)
```

**After completing WebSocket setup — agent must update configurations.md:**
```
WebSocket Configuration: ✅ READY
  Type: Spring WebSocket + STOMP / SSE
  Config class: config/WebSocketConfig.java
  Endpoint: /ws
  Topics: /topic/notifications, /topic/job-status
  Auth: JWT validated in WebSocket handshake
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When WebSocket Is Needed

### 🔴 Always WebSocket — Read This File

| Signal in Spec / Task | WebSocket Required |
|---|---|
| "real-time" / "live updates" | Data must update without page refresh |
| "live status" / "live progress" | Job status bar updating in real time |
| "instant notification" | User sees notification without polling |
| "chat" / "messaging" | Two-way real-time communication |
| "collaborative" / "multiple users" | Multiple users see same live data |
| "dashboard live" / "live dashboard" | Metrics updating in real time |
| "WebSocket" explicitly mentioned | Explicit requirement |

### 🟡 WebSocket vs Polling — Evaluate First

| Signal | Choose WebSocket When | Choose Polling When |
|---|---|---|
| "job progress bar" | Updates happen frequently (every second) | Updates are infrequent (every 30s) |
| "notification bell" | High-traffic app with many users | Admin panel with few users |
| "export status" | Long job, frequent progress updates | Short job, just need done/failed |
| "unread count" | Many concurrent users | Few admin users |

> **Rule:** If fewer than 50 concurrent users — polling is simpler and good enough.
> WebSocket adds complexity — only use it when polling would create excessive load
> or when truly real-time UX is required.

### 🟢 No WebSocket Needed — Skip This File

| Signal | Why No WebSocket |
|---|---|
| "load data on page" | One-time fetch — use RTK Query |
| "refresh every 30 seconds" | Polling — simpler than WebSocket |
| "send form" | HTTP POST — no real-time needed |
| "export CSV" | Async job + polling — see async.md |

---

## Decision Flowchart

```
Does the feature require data to update without user action?
   NO  → HTTP + RTK Query is enough
   YES ↓

Does it require two-way communication (client sends AND receives)?
   YES → WebSocket (STOMP)
   NO  ↓

Is it one-way server-to-client only?
   YES → SSE (Server-Sent Events) — simpler
   NO  ↓

How many concurrent users?
   < 50  → Polling every 5–30 seconds is fine
   > 50  → WebSocket or SSE is more efficient
```

---

## Option A — Spring WebSocket + STOMP (Full Duplex)

Best for: chat, collaborative features, live dashboards with many users.

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### WebSocket Config

```java
// config/WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for topics clients subscribe to
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages sent from client to server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")   // configure properly in prod
            .withSockJS();                    // fallback for browsers without WebSocket
    }
}
```

### JWT Auth in WebSocket Handshake

```java
// config/WebSocketSecurityConfig.java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpDestMatchers("/app/**").authenticated()
            .simpSubscribeDestMatchers("/topic/**").authenticated()
            .simpSubscribeDestMatchers("/user/**").authenticated()
            .anyMessage().authenticated();
    }

    // Allow cross-site WebSocket connections
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}

// config/WebSocketAuthInterceptor.java
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
            .getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String userId = jwtUtil.extractUserId(token);
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    accessor.setUser(auth);
                    log.debug("WebSocket authenticated: userId={}", userId);
                } catch (Exception e) {
                    log.warn("WebSocket auth failed: {}", e.getMessage());
                    throw new MessageDeliveryException("Invalid JWT token");
                }
            }
        }
        return message;
    }
}
```

### WebSocket Message Sender Service

```java
// service/WebSocketMessageService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    // ─── Broadcast to Topic (all subscribers) ─────────────────────

    public void broadcastToTopic(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, payload);
            log.debug("WebSocket broadcast: topic=/topic/{}", topic);
        } catch (Exception e) {
            log.error("Failed to broadcast to topic: /topic/{}", topic, e);
        }
    }

    // ─── Send to Specific User ─────────────────────────────────────

    public void sendToUser(String userId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/" + destination, payload);
            log.debug("WebSocket user message: userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to user: {}", userId, e);
        }
    }

    // ─── Common Use Cases ─────────────────────────────────────────

    // Notify all connected clients of a new consultation
    public void notifyNewConsultation(ConsultationResponse consultation) {
        broadcastToTopic("consultations", Map.of(
            "type", "NEW_CONSULTATION",
            "data", consultation,
            "timestamp", Instant.now()
        ));
    }

    // Send job progress to specific user
    public void sendJobProgress(String userId, String jobId, int progress, String status) {
        sendToUser(userId, "job-progress", Map.of(
            "jobId", jobId,
            "progress", progress,
            "status", status,
            "timestamp", Instant.now()
        ));
    }

    // Send notification count update to specific user
    public void sendUnreadCount(String userId, long count) {
        sendToUser(userId, "notification-count", Map.of(
            "count", count,
            "timestamp", Instant.now()
        ));
    }
}
```

### STOMP Controller (Receive from Client)

```java
// controller/WebSocketController.java
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketMessageService messageService;

    // Client sends ping — server responds with pong
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Map<String, Object> handlePing(Principal user) {
        return Map.of("type", "pong", "timestamp", Instant.now());
    }

    // Client subscribes to their own notifications
    @SubscribeMapping("/user/queue/notifications")
    public void onSubscribeNotifications(Principal user) {
        log.debug("User subscribed to notifications: {}", user.getName());
    }
}
```

---

## Option B — Server-Sent Events (One-Way, Simpler)

Best for: job progress, notification count updates, simple live feeds.
Use when: only server needs to push to client, no client-to-server messages.

```java
// controller/SseController.java
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseEmitterRegistry emitterRegistry;

    // Client subscribes for live updates
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails user) {
        SseEmitter emitter = new SseEmitter(0L);   // 0 = no timeout

        emitterRegistry.register(user.getUsername(), emitter);

        emitter.onCompletion(() -> emitterRegistry.remove(user.getUsername()));
        emitter.onTimeout(() -> emitterRegistry.remove(user.getUsername()));
        emitter.onError(e -> emitterRegistry.remove(user.getUsername()));

        // Send initial connected event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("userId", user.getUsername(), "timestamp", Instant.now())));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        log.debug("SSE client connected: userId={}", user.getUsername());
        return emitter;
    }
}

// service/SseEmitterRegistry.java
@Component
@Slf4j
public class SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.debug("SSE registered: userId={}, total={}", userId, emitters.size());
    }

    public void remove(String userId) {
        emitters.remove(userId);
        log.debug("SSE removed: userId={}, total={}", userId, emitters.size());
    }

    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (Exception e) {
            log.warn("SSE send failed, removing emitter: userId={}", userId);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }
    }

    public void broadcastToAll(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                emitters.remove(userId);
            }
        });
    }
}
```

---

## Frontend — WebSocket (STOMP)

```typescript
// lib/websocket.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { store } from '@/app/store';

let stompClient: Client | null = null;

export function connectWebSocket(onConnected: () => void) {
  const token = store.getState().auth.token;
  if (!token) return;

  stompClient = new Client({
    webSocketFactory: () => new SockJS(
      `${import.meta.env.VITE_API_BASE_URL}/ws`
    ),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('WebSocket connected');
      onConnected();
    },
    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },
    onStompError: (frame) => {
      console.error('WebSocket error:', frame.headers['message']);
    },
  });

  stompClient.activate();
}

export function disconnectWebSocket() {
  stompClient?.deactivate();
  stompClient = null;
}

export function subscribeToTopic(
  topic: string,
  callback: (message: any) => void
) {
  if (!stompClient?.connected) return null;

  return stompClient.subscribe(`/topic/${topic}`, (message) => {
    try {
      callback(JSON.parse(message.body));
    } catch (e) {
      console.error('Failed to parse WebSocket message:', e);
    }
  });
}

export function subscribeToUserQueue(
  destination: string,
  callback: (message: any) => void
) {
  if (!stompClient?.connected) return null;

  return stompClient.subscribe(`/user/queue/${destination}`, (message) => {
    try {
      callback(JSON.parse(message.body));
    } catch (e) {
      console.error('Failed to parse user message:', e);
    }
  });
}
```

```typescript
// hooks/useWebSocket.ts
export function useJobProgress(jobId: string | null) {
  const [progress, setProgress] = useState<JobProgress | null>(null);

  useEffect(() => {
    if (!jobId) return;

    connectWebSocket(() => {
      const sub = subscribeToUserQueue('job-progress', (data) => {
        if (data.jobId === jobId) {
          setProgress(data);

          // Stop listening when complete
          if (data.status === 'COMPLETED' || data.status === 'FAILED') {
            sub?.unsubscribe();
          }
        }
      });
    });

    return () => {
      disconnectWebSocket();
    };
  }, [jobId]);

  return progress;
}
```

---

## Frontend — SSE (Simpler)

```typescript
// hooks/useSse.ts
export function useLiveNotifications() {
  const token = useSelector(selectToken);
  const dispatch = useDispatch();

  useEffect(() => {
    if (!token) return;

    const eventSource = new EventSource(
      `${import.meta.env.VITE_API_BASE_URL}/api/sse/subscribe`,
      { withCredentials: true }
    );

    eventSource.addEventListener('notification-count', (e) => {
      const data = JSON.parse(e.data);
      dispatch(setUnreadCount(data.count));
    });

    eventSource.addEventListener('job-progress', (e) => {
      const data = JSON.parse(e.data);
      dispatch(updateJobProgress(data));
    });

    eventSource.onerror = () => {
      console.warn('SSE connection lost — will reconnect');
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [token]);
}
```

---

## Testing — WebSocket

### Backend Integration Test

```java
@Test
@DisplayName("WebSocket — authenticated client connects successfully")
void websocket_authenticatedClient_connectsSuccessfully() throws Exception {
    WebSocketStompClient client = new WebSocketStompClient(
        new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
    );

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + adminToken());

    StompSession session = client.connectAsync(
        "ws://localhost:" + port + "/ws",
        new WebSocketHttpHeaders(),
        connectHeaders,
        new StompSessionHandlerAdapter() {}
    ).get(5, TimeUnit.SECONDS);

    assertThat(session.isConnected()).isTrue();
    session.disconnect();
}

@Test
@DisplayName("SSE /api/sse/subscribe — authenticated — returns event stream")
void sseSubscribe_authenticated_returnsEventStream() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/sse/subscribe")
            .header("Authorization", adminToken())
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type",
            containsString("text/event-stream")));
}
```

### Frontend Playwright Test

```javascript
// Scenario: Job progress updates via WebSocket
// Mock WebSocket for Playwright
await page.addInitScript(() => {
  window.__wsMessages = [];
  const OriginalWS = window.WebSocket;
  window.WebSocket = class extends OriginalWS {
    constructor(url) {
      super(url);
      this.addEventListener('message', (e) => {
        window.__wsMessages.push(JSON.parse(e.data));
      });
    }
  };
});

await playwright_navigate({ url: 'http://localhost:5173/admin/dashboard' });
await loginAsAdmin(page);

// Trigger export job
await playwright_click({ selector: '[data-testid="export-btn"]' });

// Wait for progress bar to appear
await page.waitForSelector('[data-testid="job-progress-bar"]', { timeout: 5000 });
const progressBar = await page.locator('[data-testid="job-progress-bar"]').isVisible();
console.log('Progress bar shown:', progressBar ? '✅' : '❌');

await playwright_screenshot({ name: 'test-X.Y-websocket-progress' });
```

---

## ✅ Always Do This

1. Check `stack.md` — only add WebSocket/SSE if listed
2. Check `configurations.md` — confirm WebSocket config status before setup
3. Check Async config is `✅ READY` before setting up WebSocket
4. Validate JWT in WebSocket handshake — never allow unauthenticated connections
5. Always catch exceptions when sending — connection may be closed
6. Use `ConcurrentHashMap` for emitter registry — WebSocket is multi-threaded
7. Clean up emitters on `onCompletion`, `onTimeout`, `onError`
8. Use SSE for simple one-way push — avoid WebSocket complexity when not needed
9. Always use `wss://` in production — never plain `ws://`
10. Update `configurations.md` after completing WebSocket setup

---

## ❌ Never Do This

1. Write WebSocket code if not in `stack.md`
2. Allow unauthenticated WebSocket connections — always validate JWT
3. Store emitters in a non-thread-safe collection
4. Skip cleanup of disconnected emitters — causes memory leaks
5. Use plain `ws://` in production — always `wss://`
6. Add WebSocket when polling is sufficient (< 50 concurrent users)
7. Block the WebSocket thread — all heavy processing must be async
8. Skip updating `configurations.md` after setup