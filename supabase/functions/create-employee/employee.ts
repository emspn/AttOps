import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";
import { CreateEmployeeRequest, HttpError } from "./types.ts";

export async function checkUniqueness(
  adminClient: SupabaseClient,
  orgId: string,
  employeeId: string,
  email?: string
) {
  // 1. Check Employee ID uniqueness within the organization
  const { data: idExists, error: idError } = await adminClient
    .from("users")
    .select("id")
    .eq("organization_id", orgId)
    .eq("employee_id", employeeId)
    .maybeSingle();

  if (idError) {
    console.error("[Employee] Error checking ID uniqueness:", idError);
    throw new HttpError("A database error occurred. Please try again.", 500, "DbError");
  }

  if (idExists) {
    throw new HttpError("This Employee ID is already registered in your organization.", 409, "DuplicateError");
  }

  // 2. Check global email uniqueness
  if (email) {
    const { data: emailExists, error: emailError } = await adminClient
      .from("users")
      .select("id")
      .eq("email", email)
      .maybeSingle();

    if (emailError) {
      console.error("[Employee] Error checking email uniqueness:", emailError);
      throw new HttpError("A database error occurred. Please try again.", 500, "DbError");
    }

    if (emailExists) {
      throw new HttpError("This email address is already in use.", 409, "DuplicateError");
    }
  }
}

export function generateDeterministicEmail(employeeId: string, orgCode: string): string {
  return `${employeeId.toLowerCase()}@${orgCode.toLowerCase()}.attops.com`;
}

export async function createAuthUser(
  adminClient: SupabaseClient,
  email: string,
  pass: string,
  orgId: string
) {
  const { data, error } = await adminClient.auth.admin.createUser({
    email: email,
    password: pass,
    email_confirm: true,
    user_metadata: { organization_id: orgId }
  });

  if (error || !data.user) {
    console.error("[Employee] Auth creation failed:", error);
    throw new HttpError(`Auth creation failed: ${error?.message || "Unknown error"}`, 400, "AuthError");
  }

  console.log(`[Employee] Auth user created: ${data.user.id}`);
  return data.user;
}

export async function insertUserProfile(
  adminClient: SupabaseClient,
  userId: string,
  orgId: string,
  req: CreateEmployeeRequest,
  email: string
) {
  const { error } = await adminClient.from("users").insert({
    id: userId,
    organization_id: orgId,
    employee_id: req.employee_id,
    full_name: req.full_name,
    email: email,
    phone: req.phone,
    department: req.department,
    designation: req.designation,
    role: req.role,
    login_password: req.password,
    status: "ACTIVE"
  });

  if (error) {
    console.error("[Employee] Profile insertion failed:", error);
    throw new HttpError(`Database profile insertion failed: ${error.message}`, 400, "DbError");
  }
  console.log(`[Employee] Profile record inserted for user: ${userId}`);
}

export async function rollbackAuthUser(adminClient: SupabaseClient, userId: string) {
  try {
    console.warn(`[Employee] Initiating rollback for user: ${userId}`);
    const { error } = await adminClient.auth.admin.deleteUser(userId);
    if (error) {
      console.error(`[Employee] Rollback FAILED for user ${userId}:`, error);
    } else {
      console.log(`[Employee] Rollback successful. Deleted auth user: ${userId}`);
    }
  } catch (err) {
    console.error(`[Employee] Unexpected error during rollback for user ${userId}:`, err);
  }
}
