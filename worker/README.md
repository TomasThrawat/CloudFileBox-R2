# CloudFileBox R2 Worker

The Worker keeps R2 credentials server-side and creates short-lived presigned URLs for the Android client. Cloudflare documents this pattern for mobile uploads.

Setup:
1. Create an R2 bucket.
2. Replace REPLACE_WITH_YOUR_BUCKET in wrangler.jsonc and src/index.ts.
3. Run npm install.
4. Set R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY and APP_KEY with wrangler secrets.
5. Run npm run deploy.
6. Put the Worker URL and the same APP_KEY into the Android BuildConfig.

Never put R2 secret credentials in the Android APK.
