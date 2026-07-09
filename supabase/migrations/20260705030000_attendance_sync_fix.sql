-- Phase 4.5: Robust Offline-First Sync Fix
-- Adding a client-generated ID to prevent sync collisions and handle offline check-out

ALTER TABLE public.attendance_logs
ADD COLUMN IF NOT EXISTS client_side_id UUID UNIQUE;

-- Add separate columns for check-in and check-out photos to prevent overwriting
ALTER TABLE public.attendance_logs
RENAME COLUMN image_url TO check_in_image_url;

ALTER TABLE public.attendance_logs
ADD COLUMN IF NOT EXISTS check_out_image_url TEXT;

-- Update RLS for the new columns
DROP POLICY IF EXISTS "View attendance logs" ON public.attendance_logs;
CREATE POLICY "View attendance logs" ON public.attendance_logs
FOR SELECT USING (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org()));

DROP POLICY IF EXISTS "Employee manage own attendance" ON public.attendance_logs;
CREATE POLICY "Employee manage own attendance" ON public.attendance_logs
FOR ALL USING (employee_id = auth.uid());
