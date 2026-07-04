import { CreateEmployeeRequest } from "./types.ts";

export function validateRequest(body: Partial<CreateEmployeeRequest>): string | null {
  if (!body.employee_id?.trim()) {
    return "Employee ID is required.";
  }

  if (!body.full_name?.trim()) {
    return "Full name is required.";
  }

  if (!body.role) {
    return "Role is required.";
  }

  if (!body.password || body.password.length < 8) {
    return "Password must be at least 8 characters.";
  }

  return null;
}