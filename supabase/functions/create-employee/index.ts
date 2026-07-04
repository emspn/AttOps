import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";
import { validateRequest } from "./validation.ts";
import { getCallerProfile } from "./auth.ts";
import * as service from "./employee.ts";
import { CreateEmployeeRequest, HttpError } from "./types.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ success: false, message: "Session expired. Please login again." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 401 }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

    const supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } }
    });

    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey);

    let body: CreateEmployeeRequest = await req.json();

    // 1. Validation
    const validationError = validateRequest(body);
    if (validationError) {
      return new Response(
        JSON.stringify({ success: false, message: validationError }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      );
    }

    // 2. Authorization
    const caller = await getCallerProfile(supabaseClient);

    // 3. Uniqueness Check
    try {
      await service.checkUniqueness(supabaseAdmin, caller.organization_id, body.employee_id, body.email);
    } catch (e) {
      const msg = (e instanceof HttpError) ? e.message : "Employee ID or email already in use.";
      return new Response(
        JSON.stringify({ success: false, message: msg }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 409 }
      );
    }

    // 4. Provisioning
    const authEmail = service.generateDeterministicEmail(body.employee_id, caller.org_code);

    console.log(`[Provisioning] Creating account: ${authEmail}`);
    const newAuthUser = await service.createAuthUser(supabaseAdmin, authEmail, body.password, caller.organization_id);

    try {
      console.log(`[Provisioning] Creating profile: ${newAuthUser.id}`);
      await service.insertUserProfile(supabaseAdmin, newAuthUser.id, caller.organization_id, body, body.email || authEmail);
    } catch (dbError) {
      console.error(`[Provisioning] Rollback triggered for: ${newAuthUser.id}`, dbError);
      await service.rollbackAuthUser(supabaseAdmin, newAuthUser.id);
      return new Response(
        JSON.stringify({ success: false, message: "Account creation failed at the final step. Please try again." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    return new Response(
      JSON.stringify({
        success: true,
        message: `${body.full_name} has been added successfully.`
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 201 }
    );

  } catch (err: unknown) {
    if (err instanceof HttpError) {
      return new Response(
        JSON.stringify({ success: false, message: err.message }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: err.status }
      );
    }

    console.error(`[Fatal] Edge Error:`, err);
    return new Response(
      JSON.stringify({ success: false, message: "An unexpected error occurred. Please contact support if this persists." }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});
