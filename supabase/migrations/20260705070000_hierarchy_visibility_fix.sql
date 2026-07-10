-- Phase 4.6: Strict Hierarchy Visibility (HEAD -> ADMIN -> EMPLOYEE)

-- 1. Helper to get the role of a specific user safely
CREATE OR REPLACE FUNCTION public.get_user_role(target_user_id uuid)
RETURNS text AS $$
    SELECT role FROM public.users WHERE id = target_user_id;
$$ LANGUAGE sql SECURITY DEFINER;

-- 2. Update Task Visibility Policies

DROP POLICY IF EXISTS "Owners and Admins can view all tasks" ON public.tasks;
DROP POLICY IF EXISTS "Employees can view assigned tasks" ON public.tasks;

-- HEAD (OWNER) sees everything in their organization
CREATE POLICY "HEAD view all tasks" ON public.tasks
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'HEAD' AND
        organization_id = (SELECT organization_id FROM public.users WHERE id = auth.uid())
    );

-- ADMIN sees tasks assigned to them, created by them, or assigned to employees
CREATE POLICY "ADMIN view filtered tasks" ON public.tasks
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'ADMIN' AND
        organization_id = (SELECT organization_id FROM public.users WHERE id = auth.uid()) AND
        (
            assigned_to = auth.uid() OR
            created_by = auth.uid() OR
            public.get_user_role(assigned_to) = 'EMPLOYEE'
        )
    );

-- EMPLOYEE sees only tasks assigned to them
CREATE POLICY "EMPLOYEE view assigned tasks" ON public.tasks
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'EMPLOYEE' AND
        assigned_to = auth.uid()
    );

-- 3. Update Attendance Visibility Policies

DROP POLICY IF EXISTS "View attendance logs" ON public.attendance_logs;
DROP POLICY IF EXISTS "Employee manage own attendance" ON public.attendance_logs;

-- HEAD sees all attendance in their organization
CREATE POLICY "HEAD view all attendance" ON public.attendance_logs
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'HEAD' AND
        organization_id = (SELECT organization_id FROM public.users WHERE id = auth.uid())
    );

-- ADMIN sees their own attendance and employee attendance
CREATE POLICY "ADMIN view filtered attendance" ON public.attendance_logs
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'ADMIN' AND
        organization_id = (SELECT organization_id FROM public.users WHERE id = auth.uid()) AND
        (
            employee_id = auth.uid() OR
            public.get_user_role(employee_id) = 'EMPLOYEE'
        )
    );

-- EMPLOYEE sees only their own attendance
CREATE POLICY "EMPLOYEE view own attendance" ON public.attendance_logs
    FOR SELECT USING (
        (SELECT role FROM public.users WHERE id = auth.uid()) = 'EMPLOYEE' AND
        employee_id = auth.uid()
    );

-- 4. Enable INSERT/UPDATE for Attendance appropriately
DROP POLICY IF EXISTS "Employees can check-in" ON public.attendance_logs;
DROP POLICY IF EXISTS "Employees can check-out" ON public.attendance_logs;

CREATE POLICY "Users can check-in/out" ON public.attendance_logs
    FOR ALL USING (
        employee_id = auth.uid()
    ) WITH CHECK (
        employee_id = auth.uid()
    );
