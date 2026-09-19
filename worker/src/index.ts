import { AwsClient } from "aws4fetch";

interface Env {
  R2_ACCOUNT_ID: string;
  R2_ACCESS_KEY_ID: string;
  R2_SECRET_ACCESS_KEY: string;
  R2_BUCKET_NAME: string;
  APP_KEY: string;
}

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "X-App-Key, Content-Type",
  "Access-Control-Allow-Methods": "GET, DELETE, OPTIONS"
};

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json" }
  });

const authorized = (request: Request, env: Env) =>
  request.headers.get("X-App-Key") === env.APP_KEY;

const client = (env: Env) => new AwsClient({
  service: "s3",
  region: "auto",
  accessKeyId: env.R2_ACCESS_KEY_ID,
  secretAccessKey: env.R2_SECRET_ACCESS_KEY
});

const endpoint = (env: Env, key = "") =>
  "https://" + env.R2_ACCOUNT_ID + ".r2.cloudflarestorage.com/" +
  env.R2_BUCKET_NAME + "/" +
  key.split("/").map(encodeURIComponent).join("/");

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (!authorized(request, env)) return json({ error: "Unauthorized" }, 401);

    const url = new URL(request.url);

    if (url.pathname === "/api/list") {
      const signed = await client(env).sign(
        new Request(endpoint(env) + "?list-type=2&max-keys=1000"),
        { aws: { signQuery: true } }
      );
      const response = await fetch(signed.url);
      if (!response.ok) return json({ error: "R2 list failed" }, 502);

      const xml = await response.text();
      const objects = [...xml.matchAll(/<Contents>[sS]*?<Key>([^<]*)<\/Key>[\s\S]*?<Size>(\d+)<\/Size>[\s\S]*?<LastModified>([^<]*)<\/LastModified>[\s\S]*?<\/Contents>/g)]
        .map(m => ({ key: m[1], size: Number(m[2]), updated: m[3] }));

      return json(objects);
    }

    if (url.pathname === "/api/sign") {
      const op = url.searchParams.get("op");
      const key = url.searchParams.get("key");
      if (!key || !["put", "get", "delete"].includes(op ?? "")) {
        return json({ error: "Invalid request" }, 400);
      }

      const contentType =
        url.searchParams.get("contentType") || "application/octet-stream";
      const method = op === "put" ? "PUT" : op === "get" ? "GET" : "DELETE";
      const target = new URL(endpoint(env, key));
      target.searchParams.set("X-Amz-Expires", "3600");

      const signed = await client(env).sign(
        new Request(target, {
          method,
          headers: method === "PUT" ? { "Content-Type": contentType } : undefined
        }),
        { aws: { signQuery: true } }
      );

      return json({ url: signed.url.toString() });
    }

    return json({ error: "Not found" }, 404);
  }
};
