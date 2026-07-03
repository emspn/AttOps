import "@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "@supabase/server";
import { validateRequest, sanitizeRequest } from "./validation.ts";
import { getCallerProfile } from "./auth.ts";
import * as service from "./employee.ts";
import { CreateEmployeeRequest, HttpError } from "./types.ts";

export default {
  fetch: withSupabase(
    { auth: ["publishable"] },
    async (req, ctx) => {
      try {
        let body: CreateEmployeeRequest = await req.json();

        // 1. Sanitize & Validation
        body = sanitizeRequest(body);
        const validationError = validateRequest(body);
        if (validationError) {
          return Response.json({
            success: false,
            error: "Validation Error",
            message: validationError
          }, { status: 400 });
        }

        // 2. Authorization (HEAD/ADMIN check and scope retrieval)
        const caller = await getCallerProfile(ctx.supabase);

        // 3. Uniqueness Validation
        await service.checkUniqueness(ctx.supabaseAdmin, caller.organization_id, body.employee_id, body.email);

        // 4. Identity Generation
        const finalEmail = body.email || service.generateDeterministicEmail(body.employee_id, caller.org_code);

        // 5. Atomic Provisioning Flow
        console.log(`[Provisioning] Creating auth user for email: ${finalEmail}`);
        const newAuthUser = await service.createAuthUser(ctx.supabaseAdmin, finalEmail, body.password, caller.organization_id);

        try {
          console.log(`[Provisioning] Inserting profile for user: ${newAuthUser.id}`);
          await service.insertUserProfile(ctx.supabaseAdmin, newAuthUser.id, caller.organization_id, body, finalEmail);
        } catch (dbError) {
          // ROLLBACK: Cleanup the Auth user if profile insertion fails to prevent "ghost accounts"
          console.error(`[Provisioning] Profile insertion failed, triggering rollback:`, dbError);
          await service.rollbackAuthUser(ctx.supabaseAdmin, newAuthUser.id);
          throw dbError;
        }

        console.log(`[Provisioning] Success: ${body.employee_id} provisioned.`);
        return Response.json({
          success: true,
          employee_id: body.employee_id,
          user_id: newAuthUser.id,
          message: "Employee provisioned successfully."
        }, { status: 201 });

      } catch (err: unknown) {
        if (err instanceof HttpError) {
           return Response.json({
            success: false,
            error: err.errorType,
            message: err.message
          }, { status: err.status });
        }

        const error = err as Error;
        console.error(`[Fatal] Unexpected execution error:`, error);
        return Response.json({
          success: false,
          error: "ExecutionError",
          message: error.message || "Internal Server Error"
        }, { status: 500 });
      }
    }
  ),
};
