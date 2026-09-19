# CloudFileBox R2

New Android CloudFileBox implementation using Cloudflare R2 with a Material You-style UI.

Features:
- Cloudflare R2 instead of Supabase Storage.
- Direct streamed uploads/downloads through short-lived presigned URLs.
- No full-file ByteArray buffering.
- Material 3 dynamic colors.
- Single PutObject uploads can reach R2's 5 GiB limit; multipart is the next step for objects larger than that.
- No persistent diagnostic or logging files in the app.

R2 is not unlimited free storage. It has included free monthly usage and Cloudflare requires R2 account setup/API access.
