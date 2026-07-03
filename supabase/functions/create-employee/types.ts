export type UserRole = "HEAD" | "ADMIN" | "EMPLOYEE";

export interface CreateEmployeeRequest {
  employee_id: string;
  full_name: string;
  role: UserRole;
  password: string;
  email?: string;
  phone?: string;
  department?: string;
  designation?: string;
}

export interface CallerProfile {
  id: string;
  organization_id: string;
  role: UserRole;
  org_code: string;
}

export interface FunctionResponse {
  success: boolean;
  employee_id?: string;
  user_id?: string;
  error?: string;
  message?: string;
}

export class HttpError extends Error {
  constructor(public message: string, public status: number, public errorType: string = "Error") {
    super(message);
    this.name = "HttpError";
  }
}
