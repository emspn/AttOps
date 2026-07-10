-- Phase 4.1.2: Site Registry Implementation

CREATE TABLE IF NOT EXISTS public.organization_sites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.organization_sites ENABLE ROW LEVEL SECURITY;

-- RLS Policies for Sites
DROP POLICY IF EXISTS "Users can view their organization sites" ON public.organization_sites;
CREATE POLICY "Users can view their organization sites" ON public.organization_sites
    FOR SELECT
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org())
    );

DROP POLICY IF EXISTS "Owners and Admins can manage organization sites" ON public.organization_sites;
CREATE POLICY "Owners and Admins can manage organization sites" ON public.organization_sites
    FOR ALL
    USING (
        organization_id = (SELECT org_id FROM public.get_auth_user_role_and_org() WHERE role IN ('HEAD', 'ADMIN'))
    );

-- Add site reference to tasks
ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS site_id UUID REFERENCES public.organization_sites(id) ON DELETE SET NULL;
