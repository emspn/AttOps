import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";
import { CallerProfile, HttpError } from "./types.ts";

export async function getCallerProfile(supabase: SupabaseClient): Promise<CallerProfile> {
  const { data: { user }, error: authError } = await supabase.auth.getUser();

  if (authError || !user) {
    throw new HttpError("Unauthenticated: Valid session required.", 401, "AuthError");
  }

  const { data: profile, error: dbError } = await supabase
    .from("users")
    .select(`
      id,
      organization_id,
      role,
      organizations ( org_code )
    `)
    .eq("id", user.id)
    .single();

  if (dbError || !profile) {
    console.error(`[Auth] Profile not found for user ${user.id}:`, dbError);
    throw new HttpError("Caller profile not found or database error.", 404, "DbError");
  }

  const orgCode = Array.isArray(profile.organizations)
    ? profile.organizations[0]?.org_code
    : (profile.organizations as unknown as { org_code: string })?.org_code;

  if (!orgCode) {
    console.error(`[Auth] Organization code missing for user ${user.id}`);
    throw new HttpError("Organization configuration error.", 404, "ConfigError");
  }

  if (!["HEAD", "ADMIN"].includes(profile.role)) {
    throw new HttpError("Forbidden: Only OWNER or ADMIN can perform this action.", 403, "PermissionError");
  }

  return {
    id: profile.id,
    organization_id: profile.organization_id,
    role: profile.role,
    org_code: orgCode
  };
}
