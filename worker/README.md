# CloudFileBox R2 Worker

The Worker validates the app key and creates short-lived S3-compatible presigned URLs for Cloudflare R2.

## Setup

1. Create an R2 bucket in Cloudflare.
2. Put the bucket name in `worker/wrangler.jsonc` under `vars.R2_BUCKET_NAME`.
3. Set these secrets with Wrangler:
   - `R2_ACCOUNT_ID`
   - `R2_ACCESS_KEY_ID`
   - `R2_SECRET_ACCESS_KEY`
   - `APP_KEY`
4. From the `worker` directory, run `npm install` and `npm run deploy`.
5. Put the deployed Worker URL and the same app key into the Android app's BuildConfig.

The Android app streams uploads directly from the selected ContentResolver stream to the presigned R2 URL. It does not create persistent diagnostic or logging files.
