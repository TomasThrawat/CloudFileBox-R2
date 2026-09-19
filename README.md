# CloudFileBox R2

Android cloud storage client using Cloudflare R2 and Material 3 dynamic colors.

- Direct streaming upload from Android ContentResolver to R2.
- No upload staging file in cache.
- No persistent diagnostic or logging files.
- Single PUT uploads are intended for files up to the current R2 single-request limit; multipart upload can be added for larger objects.
- R2 credentials remain server-side.
- The public repository contains placeholders only; configure the Worker and Android BuildConfig before use.
