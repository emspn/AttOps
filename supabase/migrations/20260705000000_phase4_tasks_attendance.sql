-- Phase 4: Task Management and Location Based Attendance

-- 1. Create Task Priority Enum-like Check Constraint
-- 2. Create Task Status Enum-like Check Constraint

-- 3. Tasks Table
CREATE TABLE IF NOT EXISTS public.tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES public.users(id),
    assigned_to UUID REFERENCES public.users(id),
    priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    location_name TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 4. Task Attendance Table
CREATE TABLE IF NOT EXISTS public.task_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    check_in_time TIMESTAMPTZ DEFAULT now(),
    check_in_lat DOUBLE PRECISION,
    check_in_lng DOUBLE PRECISION,
    check_out_time TIMESTAMPTZ,
    check_out_lat DOUBLE PRECISION,
    check_out_lng DOUBLE PRECISION,
    distance_from_task_location DOUBLE PRECISION,
    status TEXT NOT NULL DEFAULT 'CHECKED_IN' CHECK (status IN ('CHECKED_IN', 'CHECKED_OUT'))
);

-- 5. Enable RLS
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_attendance ENABLE ROW LEVEL SECURITY;

-- 6. RLS Policies for Tasks

-- Owners and Admins can see all tasks in their organization
CREATE POLICY "Owners and Admins can view all tasks" ON public.tasks
    FOR SELECT
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

-- Employees can only see tasks assigned to them
CREATE POLICY "Employees can view assigned tasks" ON public.tasks
    FOR SELECT
    USING (
        assigned_to = auth.uid()
    );

-- Owners and Admins can create and update tasks in their organization
CREATE POLICY "Owners and Admins can manage tasks" ON public.tasks
    FOR ALL
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

-- Employees can update the status of their assigned tasks (e.g., mark as in progress via check-in)
-- Note: This is usually handled via Edge Functions or strict RLS.
-- For simplicity in Phase 4 foundation, we allow UPDATE on specific columns via RLS if possible,
-- but Postgres RLS is row-level. We'll rely on app logic + RLS on the row.
CREATE POLICY "Employees can update assigned task status" ON public.tasks
    FOR UPDATE
    USING (assigned_to = auth.uid())
    WITH CHECK (assigned_to = auth.uid());

-- 7. RLS Policies for Task Attendance

-- Owners and Admins can see all attendance records in their organization
CREATE POLICY "Owners and Admins can view all attendance" ON public.task_attendance
    FOR SELECT
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

-- Employees can see their own attendance records
CREATE POLICY "Employees can view own attendance" ON public.task_attendance
    FOR SELECT
    USING (
        employee_id = auth.uid()
    );

-- Employees can create their own attendance records (Check-In)
CREATE POLICY "Employees can check-in" ON public.task_attendance
    FOR INSERT
    WITH CHECK (
        employee_id = auth.uid()
    );

-- Employees can update their own attendance records (Check-Out)
CREATE POLICY "Employees can check-out" ON public.task_attendance
    FOR UPDATE
    USING (
        employee_id = auth.uid()
    );

-- 8. Trigger for updated_at on tasks
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON public.tasks
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
