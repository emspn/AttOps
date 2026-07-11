-- AttOps Backend Setup Script (Phase 3 - RECURSION FIX)
-- This script ensures tables exist and RLS policies are applied correctly.

-- 1. Organizations Table
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    business_type TEXT NOT NULL,
    org_code TEXT NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    address TEXT NOT NULL,
    phone TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Users (Profiles) Table
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES public.organizations(id),
    employee_id TEXT,
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    department TEXT,
    designation TEXT,
    role TEXT NOT NULL CHECK (role IN ('HEAD', 'ADMIN', 'EMPLOYEE')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    profile_photo TEXT,
    login_password TEXT, -- For HEAD role to manage field staff access
    created_at TIMESTAMPTZ DEFAULT now(),
    -- Unique constraint for employee_id within an organization
    UNIQUE(organization_id, employee_id)
);

-- Enable RLS
ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- 3. HELPER FUNCTION TO PREVENT RLS RECURSION
-- This function runs with "SECURITY DEFINER" to bypass RLS loops.
CREATE OR REPLACE FUNCTION public.get_auth_user_role_and_org()
RETURNS TABLE (role TEXT, org_id UUID)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT u.role, u.organization_id
    FROM public.users u
    WHERE u.id = auth.uid();
END;
$$;

-- 4. RLS Policies for Organizations
DROP POLICY IF EXISTS "Users can view their own organization" ON public.organizations;
CREATE POLICY "Users can view their own organization" ON public.organizations
    FOR SELECT
    USING (
        id IN (SELECT (get_auth_user_role_and_org()).org_id)
    );

DROP POLICY IF EXISTS "Owners can update their organization" ON public.organizations;
CREATE POLICY "Owners can update their organization" ON public.organizations
    FOR UPDATE
    USING (
        owner_id = auth.uid()
    );

-- 5. RLS Policies for Users
DROP POLICY IF EXISTS "Users can view their own profile" ON public.users;
CREATE POLICY "Users can view their own profile" ON public.users
    FOR SELECT
    USING (id = auth.uid());

DROP POLICY IF EXISTS "Owners and Admins can view all members" ON public.users;
CREATE POLICY "Owners and Admins can view all members" ON public.users
    FOR SELECT
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

DROP POLICY IF EXISTS "Owners and Admins can manage members" ON public.users;
CREATE POLICY "Owners and Admins can manage members" ON public.users
    FOR ALL
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    )
    WITH CHECK (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

-- Note: The 'create-organization' and 'create-employee' Edge Functions
-- use the service_role key to bypass RLS for provisioning.

-- 6. Tasks & Approval (Phase 4 & 5)
CREATE TABLE IF NOT EXISTS public.tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES public.users(id),
    assigned_to UUID REFERENCES public.users(id),
    priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED')),
    location_name TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES public.users(id),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;

-- Policies for Tasks
DROP POLICY IF EXISTS "Owners and Admins can view all tasks" ON public.tasks;
CREATE POLICY "Owners and Admins can view all tasks" ON public.tasks
    FOR SELECT USING (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN')));

DROP POLICY IF EXISTS "Employees can view assigned tasks" ON public.tasks;
CREATE POLICY "Employees can view assigned tasks" ON public.tasks
    FOR SELECT USING (assigned_to = auth.uid());

DROP POLICY IF EXISTS "Owners and Admins can manage tasks" ON public.tasks;
CREATE POLICY "Owners and Admins can manage tasks" ON public.tasks
    FOR ALL USING (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN')));

DROP POLICY IF EXISTS "Employees can update assigned task status" ON public.tasks;
CREATE POLICY "Employees can update assigned task status" ON public.tasks
    FOR UPDATE USING (assigned_to = auth.uid()) WITH CHECK (assigned_to = auth.uid());

-- Supervisor approval enforcement
DROP POLICY IF EXISTS "Supervisor approval enforcement" ON public.tasks;
CREATE POLICY "Supervisor approval enforcement" ON public.tasks
    FOR UPDATE
    USING (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN')))
    WITH CHECK (
        (status != 'APPROVED') OR
        (organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN')))
    );
