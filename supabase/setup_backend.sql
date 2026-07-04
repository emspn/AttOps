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
