import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";
import { JWT } from "https://esm.sh/google-auth-library@9.4.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const firebaseServiceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}");
    const supabaseAdmin = createClient(Deno.env.get("SUPABASE_URL") ?? "", Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "");

    const payload = await req.json();
    const notificationId = payload.record?.id;

    const { data: notification } = await supabaseAdmin.from("notifications_log").select("*").eq("id", notificationId).single();
    if (!notification) throw new Error("Log not found");

    const { data: tokens } = await supabaseAdmin.from("fcm_tokens").select("token").eq("user_id", notification.recipient_id);
    if (!tokens || tokens.length === 0) return new Response("No tokens", { headers: corsHeaders });

    // Auth with Google FCM v1
    const client = new JWT({
      email: firebaseServiceAccount.client_email,
      key: firebaseServiceAccount.private_key,
      scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
    });
    const accessToken = await client.getAccessToken();

    const results = await Promise.all(tokens.map(async (t) => {
      return fetch(`https://fcm.googleapis.com/v1/projects/${firebaseServiceAccount.project_id}/messages:send`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken.token}` },
        body: JSON.stringify({
          message: {
            token: t.token,
            notification: { title: notification.title, body: notification.body },
            data: {
                ...notification.data,
                notification_id: notification.id,
                task_id: notification.data?.task_id || ""
            },
            android: {
              priority: "high",
              notification: {
                channel_id: "attops_notifications",
                color: "#4F6BED",
                notification_priority: "PRIORITY_HIGH",
                default_vibrate_timings: true,
                default_sound: true
              }
            }
          }
        }),
      });
    }));

    return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
  }
});
