import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const generateOrgCode = () => {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let code = "ATT-";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ success: false, message: "Your session has expired. Please login again." }),
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

    // 1. Get user
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser();

    if (authError || !user) {
      return new Response(
        JSON.stringify({ success: false, message: "Authentication failed. Please restart the app." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 401 }
      );
    }

    const { name, business_type, address } = await req.json();

    if (!name || !business_type || !address) {
      return new Response(
        JSON.stringify({ success: false, message: "Please fill in all required organization details." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      );
    }

    // 2. Check if user already has an organization
    const { data: existingUser } = await supabaseAdmin
      .from("users")
      .select("organization_id")
      .eq("id", user.id)
      .maybeSingle();

    if (existingUser?.organization_id) {
      return new Response(
        JSON.stringify({ success: false, message: "You are already registered with an organization." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 409 }
      );
    }

    // 3. Create Organization
    const orgId = crypto.randomUUID();
    const orgCode = generateOrgCode();

    const { error: orgError } = await supabaseAdmin
      .from("organizations")
      .insert({
        id: orgId,
        name: name.trim(),
        business_type: business_type,
        org_code: orgCode,
        owner_id: user.id,
        address: address.trim(),
        phone: "0000000000"
      });

    if (orgError) {
      console.error("[CreateOrg] DB Error:", orgError);
      return new Response(
        JSON.stringify({ success: false, message: "Failed to save organization. Please try again later." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    // 4. Create User Profile
    const { error: profileError } = await supabaseAdmin
      .from("users")
      .insert({
        id: user.id,
        organization_id: orgId,
        employee_id: "OWNER",
        full_name: user.user_metadata?.full_name || "Owner",
        email: user.email,
        role: "HEAD",
        status: "ACTIVE"
      });

    if (profileError) {
      console.error("[CreateOrg] Profile Error:", profileError);
      await supabaseAdmin.from("organizations").delete().eq("id", orgId);
      return new Response(
        JSON.stringify({ success: false, message: "An error occurred while creating your profile." }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    return new Response(
      JSON.stringify({ success: true, org_code: orgCode, message: "Organization setup successful." }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 201 }
    );

  } catch (err) {
    console.error("[CreateOrg] Fatal:", err);
    return new Response(
      JSON.stringify({ success: false, message: "Something went wrong on our end. Please try again." }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});
