export interface CreateEmployeeRequest {
  employee_id: string;
  full_name: string;
  role: "ADMIN" | "EMPLOYEE";
  password: string;
  email?: string;
  phone?: string;
  department?: string;
  designation?: string;
}

export interface CallerProfile {
  id: string;
  organization_id: string;
  role: string;
  org_code: string;
}

export class HttpError extends Error {
  constructor(
    public message: string,
    public status: number,
    public errorType: string
  ) {
    super(message);
    this.name = "HttpError";
  }
}
