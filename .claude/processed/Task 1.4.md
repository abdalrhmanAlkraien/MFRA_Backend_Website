# Task 1.4 — File Upload Service (S3)

**Status**: ✅ Completed
**Platform**: Backend
**Date**: 30/03/2026
**Actual cost**: $0.40

---

## What Was Built

- `config/S3Config.java` — S3Client + S3Presigner beans with region from config
- `module/upload/dto/UploadRequest.java` — fileName, contentType, fileSize with validation
- `module/upload/dto/UploadResponse.java` — presignedUrl, fileKey, publicUrl, expiresInSeconds
- `module/upload/service/FileStorageService.java` — Presigned URL generation + file validation
- `module/upload/controller/UploadController.java` — POST /api/admin/upload/presigned-url
- Updated `GlobalExceptionHandler.java` — InvalidFileException handler (400)

### File Validation Rules
- Allowed types: image/jpeg, image/png, image/webp
- Max file size: 5MB (configurable via app.file.max-size-mb)
- Presigned URL duration: 10 minutes

### Upload Flow
1. Client requests presigned URL with file metadata
2. Server validates file type and size
3. Server generates presigned PUT URL via S3Presigner
4. Client uploads directly to S3 using presigned URL
5. Client uses returned publicUrl to reference the file

---

## Verification Results

```
Backend:
  mvn clean compile       — BUILD SUCCESS
  App startup             — 2.638 seconds
  Endpoint registered     — POST /api/admin/upload/presigned-url

Smoke Tests:
  Valid request (with JWT)     — Reaches S3 presigner (fails locally without AWS creds — expected)
  Invalid content type         — 400 INVALID_FILE ✅
  File too large (10MB)        — 400 INVALID_FILE ✅
  No auth token                — 403 Forbidden ✅
```

---

## Files Created (5 new, 1 modified)

```
NEW:
  backend/src/main/java/com/mfra/website/config/S3Config.java
  backend/src/main/java/com/mfra/website/module/upload/dto/UploadRequest.java
  backend/src/main/java/com/mfra/website/module/upload/dto/UploadResponse.java
  backend/src/main/java/com/mfra/website/module/upload/service/FileStorageService.java
  backend/src/main/java/com/mfra/website/module/upload/controller/UploadController.java

MODIFIED:
  backend/src/main/java/com/mfra/website/common/exception/GlobalExceptionHandler.java
```

---

## Next Task

Task 1.6: Admin Layout Shell (Frontend)
