-- Phase 4.8: Final Integrity Verification and Hierarchy Visibility
-- 1. Align attendance_logs table for In-vs-Out matching
ALTER TABLE public.attendance_logs
ADD COLUMN IF NOT EXISTS integrity_distance DOUBLE PRECISION;

-- 2. Refined RLS for Strict Hierarchy (HEAD -> ADMIN -> EMPLOYEE)
-- First, drop all existing visibility policies to ensure a clean slate
DROP POLICY IF EXISTS "HEAD view all tasks" ON public.tasks;
DROP POLICY IF EXISTS "ADMIN view filtered tasks" ON public.tasks;
DROP POLICY IF EXISTS "EMPLOYEE view assigned tasks" ON public.tasks;
DROP POLICY IF EXISTS "Hierarchy Task Visibility" ON public.tasks;
DROP POLICY IF EXISTS "HEAD view all attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "ADMIN view filtered attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "EMPLOYEE view own attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "Hierarchy Attendance Visibility" ON public.attendance_logs;

-- TASK VISIBILITY
CREATE POLICY "Hierarchy Task Visibility" ON public.tasks
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.users u
            WHERE u.id = auth.uid()
            AND (
                u.role = 'HEAD'
                OR (u.role = 'ADMIN' AND (tasks.assigned_to = auth.uid() OR tasks.created_by = auth.uid() OR EXISTS (SELECT 1 FROM public.users target WHERE target.id = tasks.assigned_to AND target.role = 'EMPLOYEE')))
                OR (u.role = 'EMPLOYEE' AND tasks.assigned_to = auth.uid())
            )
        )
    );

-- ATTENDANCE VISIBILITY
CREATE POLICY "Hierarchy Attendance Visibility" ON public.attendance_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.users u
            WHERE u.id = auth.uid()
            AND (
                (u.role = 'HEAD' AND u.organization_id = attendance_logs.organization_id)
                OR (u.role = 'ADMIN' AND (attendance_logs.employee_id = auth.uid() OR EXISTS (SELECT 1 FROM public.users target WHERE target.id = attendance_logs.employee_id AND target.role = 'EMPLOYEE')))
                OR (u.role = 'EMPLOYEE' AND attendance_logs.employee_id = auth.uid())
            )
        )
    );
