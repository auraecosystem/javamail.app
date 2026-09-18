---
implementation: Jakarta Mail API with Eclipse Angus Mail
protocol: implementation Jakarta Mail 2.1 moved its implementation out of the API repository into Eclipse Angus, whose stated purpose is to implement Jakarta Mail 2.1+ 
platform: independent mail applications. 
---


# javamail.app v1

Repository identity:

```javamail.app
├── Java/Jakarta Mail application
├── SMTP / SMTPS
├── IMAP / IMAPS
├── POP3 / POP3S
├── MIME
├── TLS
├── OAuth2-ready authentication
├── OpenPGP-ready message security
├── mailbox synchronization
├── message/search API
└── Web/API application layer
```
The core principle is:
```
Application
    ↓
Mail Service
    ↓
Mail Domain
    ↓
Mail Provider Adapter
    ↓
Jakarta Mail API
    ↓
Eclipse Angus Mail
    ↓
SMTP / IMAP / POP3
    ↓
Remote Mail Server
```
This is preferable to putting jakarta.mail.Session, SMTP configuration, MIME parsing, authentication, and application logic throughout the codebase.

1. Complete repository

```javamail.app/
│
├── README.md
├── LICENSE
├── NOTICE
├── CONTRIBUTING.md
├── SECURITY.md
├── CODE_OF_CONDUCT.md
├── CHANGELOG.md
├── VERSION
│
├── .gitignore
├── .gitattributes
├── .editorconfig
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│
├── Dockerfile
├── compose.yaml
│
├── Makefile
│
├── .github/
│   ├── dependabot.yml
│   ├── CODEOWNERS
│   │
│   └── workflows/
│       ├── ci.yml
│       ├── test.yml
│       ├── security.yml
│       ├── dependency-review.yml
│       ├── release.yml
│       └── docker.yml
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── CONFIGURATION.md
│   ├── SECURITY.md
│   ├── MAIL_PROTOCOLS.md
│   ├── AUTHENTICATION.md
│   ├── DEPLOYMENT.md
│   ├── DEVELOPMENT.md
│   ├── OPENPGP.md
│   └── ADR/
│       ├── 0001-architecture.md
│       ├── 0002-mail-provider.md
│       ├── 0003-credential-storage.md
│       └── 0004-message-storage.md
│
├── src/
│   │
│   ├── main/
│   │   ├── java/
│   │   │   └── app/
│   │   │       └── javamail/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback.xml
│   │       └── META-INF/
│   │           └── javamail.providers
│   │
│   └── test/
│       ├── java/
│       │   └── app/
│       │       └── javamail/
│       │
│       └── resources/
│           ├── application-test.properties
│           └── fixtures/
│
└── scripts/
    ├── dev.sh
    ├── test.sh
    ├── lint.sh
    ├── security-scan.sh
    └── release.sh
```
2. Java package architecture

I would use:

app.javamail

with the following packages:

```app.javamail
│
├── Application.java
│
├── config/
│   ├── AppConfig.java
│   ├── MailConfig.java
│   ├── SecurityConfig.java
│   └── StorageConfig.java
│
├── domain/
│   ├── account/
│   │   ├── MailAccount.java
│   │   ├── MailIdentity.java
│   │   └── AccountStatus.java
│   │
│   ├── message/
│   │   ├── MailMessage.java
│   │   ├── MessageId.java
│   │   ├── MessageAddress.java
│   │   ├── MessageAttachment.java
│   │   ├── MessageBody.java
│   │   └── MessageFlags.java
│   │
│   ├── mailbox/
│   │   ├── Mailbox.java
│   │   ├── MailboxId.java
│   │   └── MailboxType.java
│   │
│   └── thread/
│       ├── MailThread.java
│       └── ThreadId.java
│
├── mail/
│   ├── MailClient.java
│   ├── MailSessionFactory.java
│   │
│   ├── smtp/
│   │   ├── SmtpClient.java
│   │   └── SmtpTransport.java
│   │
│   ├── imap/
│   │   ├── ImapClient.java
│   │   ├── ImapFolder.java
│   │   └── ImapSynchronizer.java
│   │
│   ├── pop3/
│   │   └── Pop3Client.java
│   │
│   └── mime/
│       ├── MimeParser.java
│       ├── MimeBuilder.java
│       ├── AttachmentParser.java
│       └── HtmlSanitizer.java
│
├── auth/
│   ├── AuthenticationProvider.java
│   ├── PasswordAuthenticator.java
│   ├── OAuth2Authenticator.java
│   └── CredentialStore.java
│
├── security/
│   ├── TlsPolicy.java
│   ├── CertificatePolicy.java
│   ├── MessageSecurity.java
│   ├── OpenPgpService.java
│   └── SecurityAudit.java
│
├── storage/
│   ├── MessageRepository.java
│   ├── MailboxRepository.java
│   ├── AccountRepository.java
│   └── AttachmentRepository.java
│
├── sync/
│   ├── SyncEngine.java
│   ├── SyncState.java
│   ├── SyncCursor.java
│   └── SyncEvent.java
│
├── search/
│   ├── MailSearchService.java
│   ├── SearchQuery.java
│   └── SearchResult.java
│
├── api/
│   ├── AccountController.java
│   ├── MessageController.java
│   ├── MailboxController.java
│   ├── SearchController.java
│   └── HealthController.java
│
└── observability/
    ├── Metrics.java
    ├── MailLogger.java
    └── HealthService.java
```
That separation is important. domain should know nothing about Jakarta Mail.

3. Core domain model

MailAccount:
```java
public final class MailAccount {
    private final String id;
    private final String email;
    private final MailIdentity identity;
    private final MailServerConfig incoming;
    private final MailServerConfig outgoing;
    private final AccountStatus status;
    // constructors/getters
}
```
MailIdentity:
```Java
public record MailIdentity(
    String displayName,
    String email
) {}
```
MailServerConfig:
```
public record MailServerConfig(
    String host,
    int port,
    String protocol,
    SecurityMode security,
    AuthenticationMode authentication
) {}
```
Security:
```wrx
public enum SecurityMode {
    NONE,
    STARTTLS,
    TLS
}

Authentication:

public enum AuthenticationMode {
    PASSWORD,
    OAUTH2
}
```
The application should never put passwords directly into MailAccount.

4. Message model

The internal representation should be independent of jakarta.mail.Message.
```java
public final class MailMessage {
    private MessageId id;
    private String subject;
    private List<MessageAddress> from;
    private List<MessageAddress> to;
    private List<MessageAddress> cc;
    private List<MessageAddress> bcc;
    private String textBody;
    private String htmlBody;
    private List<MessageAttachment> attachments;
    private MessageFlags flags;
    private Instant sentAt;
    private Instant receivedAt;
    private String messageId;
    private String inReplyTo;
    private List<String> references;
}
```
This gives you a stable application-level message model even if the underlying mail implementation changes.

5. SMTP

The SMTP subsystem should support:
```
smtp
smtps
STARTTLS
TLS
authentication
OAuth2-ready authentication
connection timeout
read timeout
write timeout
message size limits
```
The application flow becomes:
```
POST /messages
      ↓
MessageController
      ↓
MailService
      ↓
MimeBuilder
      ↓
SmtpClient
      ↓
Jakarta Mail
      ↓
Angus SMTPTransport
      ↓
SMTP server
```
Angus currently provides the underlying Jakarta Mail implementation and exposes SMTP/IMAP/POP3 providers through the mail stack. 

6. IMAP

IMAP should be the primary synchronization protocol.

```IMAP
│
├── account authentication
├── mailbox discovery
├── message headers
├── message body retrieval
├── flags
├── read/unread
├── starred
├── deleted
├── move
├── copy
├── search
├── UID tracking
└── synchronization
```
The synchronizer should maintain:
```sync
public record SyncState(
    String accountId,
    String mailboxId,
    long uidValidity,
    long lastSeenUid,
    Instant lastSync
) {}
```
Do not use message sequence numbers as permanent IDs. IMAP UIDs and UIDVALIDITY should drive synchronization state.

One current implementation consideration is important: Angus has active work/issues around IMAP connection management and extensions such as NOTIFY, so javamail.app should encapsulate IMAP behavior behind ImapClient rather than coupling application code to Angus internals. 

7. POP3

POP3 should be supported primarily for compatibility:
```
POP3
POP3S
TLS
authentication
UIDL
download
delete-after-download
```
But POP3 should not become the primary synchronization model.
```
IMAP  → primary
POP3  → compatibility
SMTP  → outbound
```
8. MIME engine

This is one of the most important parts of the application.

```MimeParser
     │
     ├── text/plain
     ├── text/html
     ├── multipart/mixed
     ├── multipart/alternative
     ├── multipart/related
     ├── attachments
     ├── inline images
     ├── Content-ID
     ├── Content-Disposition
     └── encoded content
```
Never trust an attachment’s filename.

Normalize:
```
filename
MIME type
size
extension
Content-ID
disposition
hash
```
For every attachment:
```
public record MessageAttachment(
    String id,
    String filename,
    String contentType,
    long size,
    String sha256,
    boolean inline
) {}
```
9. HTML mail security

HTML messages must be sanitized before being rendered by a client.

The pipeline should be:
```
Incoming MIME
      ↓
HTML extraction
      ↓
HTML sanitizer
      ↓
URL normalization
      ↓
remote-resource policy
      ↓
safe HTML
```
Block or control:
```js
<script>
javascript:
data:
embedded active content
unsafe forms
tracking pixels
dangerous URLs
```
The raw message should remain available separately from the rendered representation.

10. Authentication

V1 should define:

PASSWORD
OAUTH2

but make the authentication system extensible.
```
public interface AuthenticationProvider {
    AuthenticationResult authenticate(
        MailAccount account
    );
}
```
OAuth2 should be implemented as a provider, not hard-coded into SMTP or IMAP.
```
OAuth2
   ↓
Access Token
   ↓
AuthenticationProvider
   ↓
Jakarta Mail Authenticator

Credentials should never be committed to Git.

Configuration should use environment variables or an external secret store.
```
11. TLS

TLS policy deserves its own abstraction.

public record TlsPolicy(
    boolean required,
    boolean verifyCertificate,
    String[] enabledProtocols,
    String[] enabledCipherSuites
) {}

Default:

TLS required where supported
certificate verification enabled
modern TLS protocols only
no trust-all certificates
no hostname bypass

There are active security issues in the broader Angus codebase, including reports concerning XML processing and legacy authentication mechanisms, so dependency updates and security scanning should be part of the release pipeline rather than an afterthought. 

12. OpenPGP

The existing repository contains:
```cmd
Graph.pgp
0xF114C34E.asc
```
but the uploaded files do not contain a functioning OpenPGP implementation.

I would therefore make OpenPGP a defined V1 interface:
```pgp
public interface OpenPgpService {
    SignedMessage sign(MailMessage message);
    VerificationResult verify(MailMessage message);
    EncryptedMessage encrypt(
        MailMessage message,
        PublicKey recipient
    );
    MailMessage decrypt(
        EncryptedMessage message
    );
}
```
Implementation can be introduced behind this interface without contaminating the mail transport layer.

The architecture becomes:

```MailMessage
     │
     ├── normal
     │
     ├── signed
     │
     ├── encrypted
     │
     └── signed + encrypted
```
13. Storage

For V1 I recommend a relational database for application metadata and object storage/filesystem for large message bodies and attachments.

```Database
├── accounts
├── identities
├── mailboxes
├── messages
├── message_flags
├── recipients
├── attachments
├── sync_state
└── audit_events
Object storage
├── raw messages
├── attachments
└── large MIME bodies
```
Message table:

messages
───────────────
id
account_id
mailbox_id
uid
uid_validity
message_id
thread_id
subject
from_address
received_at
sent_at
size
raw_object_key
created_at
updated_at

14. Threading

Conversation threading should use:
```
Message-ID
In-Reply-To
References
subject normalization
```
Do not use subject alone.

Conceptually:

Message A
   │
   ├── Message B
   │      │
   │      └── Message C
   │
   └── Message D

becomes:

MailThread
   ├── MailMessage
   ├── MailMessage
   ├── MailMessage
   └── MailMessage

15. Search

V1 search:

from:
to:
cc:
subject:
body:
before:
after:
has:attachment
is:read
is:unread
is:starred

Example:
```md
from:alice@example.com
subject:"Web4"
has:attachment
after:2026-09-01
```
Internally:

SearchQuery
      ↓
SearchParser
      ↓
SearchPlan
      ↓
MessageRepository
      ↓
SearchResult

16. REST API

I would expose:
```
/api/v1/accounts
/api/v1/accounts/{id}
/api/v1/mailboxes
/api/v1/mailboxes/{id}
/api/v1/messages
/api/v1/messages/{id}
/api/v1/messages/{id}/send
/api/v1/messages/{id}/reply
/api/v1/messages/{id}/forward
/api/v1/search
/api/v1/sync
/api/v1/health
/api/v1/ready
```
Example:
```yaml
GET /api/v1/mailboxes/inbox/messages

Response:

{
  "items": [
    {
      "id": "msg_01",
      "threadId": "thread_01",
      "subject": "Web4 Architecture",
      "from": {
        "name": "Alice",
        "email": "alice@example.com"
      },
      "receivedAt": "2026-09-18T10:30:00Z",
      "read": false,
      "hasAttachments": true
    }
  ]
}
```

17. Sending mail API
```json
POST /api/v1/messages/send
{
  "accountId": "account_01",
  "to": [
    {
      "email": "user@example.com"
    }
  ],
  "subject": "Hello",
  "text": "Hello from javamail.app"
}
```
Pipeline:

HTTP
 ↓
validation
 ↓
authorization
 ↓
MailMessage
 ↓
MIME builder
 ↓
security policy
 ↓
SMTP
 ↓
delivery result
 ↓
audit event

18. Reply

POST /api/v1/messages/{id}/reply

The service automatically handles:

In-Reply-To
References
quoted text
recipient selection
thread ID

19. Forward
```bash
POST /api/v1/messages/{id}/forward
```
Attachments should be explicitly represented rather than blindly copying arbitrary MIME structures.

20. Synchronization engine

The synchronization engine is the heart of the application.

```ch
    │
    ├── discover mailboxes
    ├── fetch UID state
    ├── detect additions
    ├── detect flag changes
    ├── detect deletions
    ├── fetch headers
    ├── fetch bodies
    ├── process attachments
    ├── update local index
    └── persist SyncState
```
State machine:

IDLE
 ↓
CONNECTING
 ↓
AUTHENTICATING
 ↓
DISCOVERING
 ↓
SYNCING
 ↓
COMMITTING
 ↓
IDLE

Failure:

SYNCING
   ↓
ERROR
   ↓
BACKOFF
   ↓
RECONNECTING

21. Event model

Define internal events:
```console
AccountConnected
AccountDisconnected
MailboxDiscovered
MessageReceived
MessageUpdated
MessageDeleted
MessageSent
MessageFailed
SyncStarted
SyncCompleted
SyncFailed
AuthenticationFailed
SecurityViolation
```
This gives the application an event-driven foundation without requiring Kafka or another distributed system in V1.

22. Observability

Expose:
```java
mail.smtp.sent
mail.smtp.failed
mail.imap.connected
mail.imap.disconnected
mail.sync.started
mail.sync.completed
mail.sync.failed
mail.auth.failed
mail.mime.parse.failed
mail.attachment.rejected
```
Health endpoints:

/health
/ready

Example:

{
  "status": "UP",
  "components": {
    "database": "UP",
    "mail": "UP",
    "storage": "UP"
  }
}

23. Configuration

Environment variables:
```
JAVAMAIL_APP_ENV
JAVAMAIL_APP_PORT
JAVAMAIL_DB_URL
JAVAMAIL_DB_USER
JAVAMAIL_DB_PASSWORD
JAVAMAIL_STORAGE_PATH
JAVAMAIL_SMTP_HOST
JAVAMAIL_SMTP_PORT
JAVAMAIL_SMTP_TLS
JAVAMAIL_IMAP_HOST
JAVAMAIL_IMAP_PORT
JAVAMAIL_IMAP_TLS
JAVAMAIL_MAX_MESSAGE_SIZE
JAVAMAIL_MAX_ATTACHMENT_SIZE
JAVAMAIL_SECURITY_REQUIRE_TLS
JAVAMAIL_HTML_SANITIZATION_ENABLED
```
No secrets in:

application.properties
application.yml
Git
Dockerfile
README
tests
logs

24. Maven

The project should be Maven-first.

Core dependencies conceptually:
```maven
<dependencies>
    <dependency>
        <groupId>jakarta.mail</groupId>
        <artifactId>jakarta.mail-api</artifactId>
        <version>2.1.x</version>
    </dependency>
    <dependency>
        <groupId>org.eclipse.angus</groupId>
        <artifactId>angus-mail</artifactId>
        <version>2.x</version>
    </dependency>
</dependencies>
```
The exact dependency versions should be pinned to the current stable releases when implementation begins rather than hard-coding a stale version into this architectural specification. Jakarta Mail 2.1 explicitly separates the API from the Angus implementation. 

25. Java version

Use:

Java 21 LTS

as the V1 baseline.

That gives the project:

records
sealed classes
modern concurrency
virtual threads
modern TLS/runtime support
long-term LTS foundation

Virtual threads are particularly useful for blocking mail operations without forcing the entire mail stack into an asynchronous programming model.

26. Application bootstrap

Conceptually:
```
public final class Application {
    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        MailSessionFactory sessions =
            new MailSessionFactory(config.mail());
        MailClient client =
            new MailClient(sessions);
        SyncEngine sync =
            new SyncEngine(client);
        ApplicationRuntime runtime =
            new ApplicationRuntime(
                config,
                client,
                sync
            );
        runtime.start();
    }
}
```
27. MailSessionFactory

This is the boundary between your application and Jakarta Mail.

public interface MailSessionFactory {
    Session smtp(MailAccount account);
    Session imap(MailAccount account);
    Session pop3(MailAccount account);
}

Implementation:

MailSessionFactory
       ↓
Jakarta Mail Session
       ↓
Angus provider

Nothing above this layer should depend directly on:

com.sun.mail.*
org.eclipse.angus.*

That keeps the application portable.

28. Provider abstraction

Define:

public interface MailProvider {
    MailConnection connect(
        MailAccount account
    );
}

Then:

MailProvider
├── AngusMailProvider
├── FutureProvider
└── TestMailProvider

This is especially useful for testing.

29. Test mail server

Do not run integration tests against Gmail, Outlook, Yahoo, etc.

Use a disposable test mail environment.

tests
   ↓
TestMailProvider
   ↓
local SMTP/IMAP test server

Test cases:

send
receive
reply
forward
attachment
HTML
multipart
TLS
authentication
UID synchronization
flags
deletion
duplicate prevention
connection failure
server restart

30. Security architecture

Security boundaries:

                 ┌───────────────┐
                 │ API / Client  │
                 └───────┬───────┘
                         │
                    Validation
                         │
                 ┌───────▼───────┐
                 │ Mail Service  │
                 └───────┬───────┘
                         │
                 Security Policy
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
       TLS/Auth       MIME         OpenPGP
          │              │              │
          └──────────────┼──────────────┘
                         ↓
                  Mail Provider

Threats to address:

credential leakage
TLS downgrade
certificate bypass
malicious HTML
malicious attachments
oversized MIME
header injection
SMTP command injection
SSRF through remote resources
path traversal through attachment names
message spoofing
OpenPGP key substitution
replay/duplicate synchronization
audit-log leakage

31. Audit log

Security-sensitive operations should create audit events.

AUTHENTICATION_SUCCESS
AUTHENTICATION_FAILURE
ACCOUNT_CREATED
ACCOUNT_UPDATED
ACCOUNT_DELETED
MESSAGE_SENT
MESSAGE_DELETED
ATTACHMENT_REJECTED
TLS_FAILURE
SECURITY_POLICY_VIOLATION
OPENPGP_VERIFICATION_FAILED

Never log:

passwords
OAuth tokens
private keys
full message bodies
encrypted secrets

32. CI/CD

GitHub Actions:

push
 │
 ├── compile
 ├── unit tests
 ├── integration tests
 ├── dependency audit
 ├── secret scanning
 ├── static analysis
 ├── SBOM
 ├── container build
 └── artifact

Release:

tag v1.0.0
      ↓
CI
      ↓
tests
      ↓
security
      ↓
SBOM
      ↓
Docker image
      ↓
GitHub Release

33. Docker

Runtime:

Java 21
non-root user
read-only filesystem where practical
minimal runtime image
healthcheck
no embedded credentials

Conceptually:

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/javamail.app.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

34. compose.yaml

Development stack:

javamail
postgres
mail-test

Conceptually:

                 ┌──────────────┐
                 │ javamail.app │
                 └──────┬───────┘
                        │
              ┌─────────┴─────────┐
              ↓                   ↓
        PostgreSQL             Mail Test
                              SMTP/IMAP

35. Original strange files

I would not simply delete the unusual files from the original archive.

Instead, preserve their historical presence under:

docs/archive/original-repository/

For example:

docs/archive/original-repository/
├── Project-Info.plist
├── email.ch/
├── email.eml.md
├── protocol/
├── models/
└── original-file-manifest.txt

Then explicitly document:

These files were present in the original repository snapshot but
were not considered functional implementation components because
their contents were empty or metadata-only.

That preserves provenance without polluting the production architecture.

36. Web4/Aura extension boundary

This is where I would leave room for your broader ecosystem without making V1 dependent on it.

javamail.app
       │
       └── integration/
             ├── web4/
             ├── aura/
             └── lmlm/

Interfaces:

public interface MailSemanticAnalyzer {
    MailAnalysis analyze(
        MailMessage message
    );
}

Potential result:

{
  "messageId": "msg_123",
  "entities": [],
  "topics": [],
  "intent": null,
  "risk": [],
  "classification": null
}

That gives you a clean future path:

Email
 ↓
MIME
 ↓
MailMessage
 ↓
LMLM / Web4 semantic analysis
 ↓
structured knowledge

But the mail system must continue working if LMLM/Web4 is unavailable.

That’s an important architectural constraint.

37. LMLM integration

Eventually:

javamail.app
       │
       ├── Mail ingestion
       │
       ├── MIME normalization
       │
       └── Message event
                ↓
              LMLM
                ↓
       ┌────────┼─────────┐
       ↓        ↓         ↓
    classify  summarize  extract

The mail application should never directly depend on a specific LLM.

Use:

public interface SemanticMailProcessor {
    SemanticResult process(
        MailMessage message
    );
}

Then an LMLM adapter can be plugged in.

38. API/event boundary for LMLM

A future event could be:

{
  "event": "mail.message.received",
  "version": "1",
  "messageId": "msg_123",
  "accountId": "acct_123",
  "timestamp": "2026-09-18T12:00:00Z"
}

The semantic engine then retrieves the normalized message through an authorized API rather than receiving credentials or raw mailbox connections.

39. Versioning

Use:

v1

for the external API.

Internal package evolution can happen independently.

Example:

/api/v1/messages

Later:

/api/v2/messages

without breaking the existing client.

40. Repository README

The README should start with something substantially like:

# javamail.app
Modern Jakarta Mail application infrastructure for sending,
receiving, synchronizing, searching, and securing email.
javamail.app provides an application-level mail architecture over
Jakarta Mail and Eclipse Angus Mail.
Protocols:
- SMTP
- SMTPS
- IMAP
- IMAPS
- POP3
- POP3S
Features:
- mail accounts
- mailbox synchronization
- MIME processing
- attachments
- HTML sanitization
- TLS
- authentication
- OAuth2-ready architecture
- OpenPGP integration boundary
- message search
- threading
- audit events
- REST API
- observability

Then:

Architecture
Configuration
Development
Testing
Security
Deployment
API
Contributing
License

41. V1 implementation phases

I would implement this in six stages.

Phase 1:

repository
Maven
Java 21
configuration
logging
Jakarta Mail
Angus
basic SMTP
basic IMAP

Phase 2:

domain model
mail accounts
mailboxes
messages
MIME
attachments
database

Phase 3:

IMAP synchronization
UID state
flags
threads
search

Phase 4:

REST API
authentication
authorization
health
metrics
audit

Phase 5:

TLS hardening
HTML sanitization
attachment security
OAuth2
OpenPGP adapter
security testing

Phase 6:

Docker
CI/CD
SBOM
release automation
Web4 integration boundary
LMLM semantic adapter

42. V1 definition of done

javamail.app v1.0.0 should not be considered complete merely because SMTP sends a test message.

The acceptance criteria should be:

[ ] Java 21 build works
[ ] clean Maven build
[ ] Jakarta Mail API integrated
[ ] Angus Mail integrated
[ ] SMTP send works
[ ] IMAP receive works
[ ] POP3 compatibility works
[ ] STARTTLS works
[ ] TLS certificate verification works
[ ] authentication abstraction works
[ ] MIME parsing works
[ ] multipart messages work
[ ] attachments work
[ ] attachment size limits work
[ ] HTML sanitization works
[ ] mailbox synchronization works
[ ] UID/UIDVALIDITY state works
[ ] flags synchronize
[ ] threading works
[ ] message search works
[ ] REST API works
[ ] audit events work
[ ] secrets aren't logged
[ ] unit tests exist
[ ] integration tests exist
[ ] security tests exist
[ ] Docker image builds
[ ] CI passes
[ ] dependency scanning passes
[ ] SBOM generated
[ ] documentation complete

One final architectural point: don’t fork Angus Mail in V1. The current Angus project is actively maintained, and its issue tracker shows ongoing work around SMTP, IMAP, OAuth2, security, and connection behavior.  Keep Angus behind MailProvider/MailSessionFactory; that gives javamail.app control over its application architecture without taking responsibility for maintaining an entire mail protocol implementation.

The resulting architecture is therefore:

                         javamail.app
                              │
             ┌────────────────┼────────────────┐
             │                │                │
          REST API          Sync            Events
             │                │                │
             └────────────────┼────────────────┘
                              │
                         Mail Service
                              │
              ┌───────────────┼────────────────┐
              │               │                │
           Domain           MIME            Security
              │               │                │
              └───────────────┼────────────────┘
                              │
                     Mail Provider API
                              │
                       Angus Adapter
                              │
                 ┌────────────┼────────────┐
                 │            │            │
                SMTP         IMAP         POP3
                 │            │            │
                 └────────────┼────────────┘
                              │
                         Mail Servers
Future:
                         MailMessage
                              │
                       Semantic Adapter
                              │
                       ┌──────┴──────┐
                       │             │
                      LMLM          Web4
                       │             │
                       └──────┬──────┘
                              │
                        semantic layer

That is the javamail.app v1 specification I would use as the implementation contract. It turns the current essentially empty repository into a real, modular mail platform while preserving a clean path into the Aura/Web4/LMLM ecosystem.
