import type { CreateEmployeeRequest, UserRole } from "./types.ts";

export function validateRequest(body: CreateEmployeeRequest): string | null {
  const employee_id = body.employee_id?.trim();
  const full_name = body.full_name?.trim();
  const password = body.password;
  const role = body.role;

  if (!employee_id || employee_id.length < 2) {
    return "Invalid Employee ID. Must be at least 2 characters.";
  }

  if (!full_name || full_name.length < 2) {
    return "Full name is required and must be at least 2 characters.";
  }

  const allowedRoles: UserRole[] = ["ADMIN", "EMPLOYEE"];
  if (!role || !allowedRoles.includes(role)) {
    return "Invalid role assignment. Only ADMIN or EMPLOYEE roles can be provisioned.";
  }

  if (!password || password.length < 8) {
    return "Password must be at least 8 characters long.";
  }

  if (body.email) {
    const email = body.email.trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(email)) {
      return "Invalid email format.";
    }
  }

  return null;
}

export function sanitizeRequest(body: CreateEmployeeRequest): CreateEmployeeRequest {
  return {
    ...body,
    employee_id: body.employee_id?.trim(),
    full_name: body.full_name?.trim(),
    email: body.email?.trim().toLowerCase(),
    phone: body.phone?.trim(),
    department: body.department?.trim(),
    designation: body.designation?.trim(),
  };
}
