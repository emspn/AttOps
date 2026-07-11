-- Phase 5.1: Task Approval Lifecycle
-- This migration adds the 'APPROVED' state and necessary auditing columns.

-- 1. Update Tasks table check constraint
-- We need to drop the old constraint and add a new one that includes 'APPROVED'
DO $$
BEGIN
    ALTER TABLE public.tasks DROP CONSTRAINT IF EXISTS tasks_status_check;
    ALTER TABLE public.tasks ADD CONSTRAINT tasks_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED'));
END $$;

-- 2. Add Auditing columns for Approval
ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES public.users(id);

-- 3. Refine RLS for Approval
-- Ensure only HEAD and ADMIN can set a task to APPROVED
DROP POLICY IF EXISTS "Supervisor approval enforcement" ON public.tasks;
CREATE POLICY "Supervisor approval enforcement" ON public.tasks
    FOR UPDATE
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    )
    WITH CHECK (
        -- If status is being set to APPROVED, verify the caller has authority
        (status != 'APPROVED') OR
        (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN')))
    );

-- 4. Comment for schema cache refresh
COMMENT ON COLUMN public.tasks.status IS 'Current lifecycle state of the task, including supervisor approval.';
