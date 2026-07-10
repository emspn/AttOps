-- Phase 4.3: Refined Storage RLS Policies

-- Drop old policies to replace them with more robust ones
DROP POLICY IF EXISTS "Allow employees to upload attendance photos" ON storage.objects;
DROP POLICY IF EXISTS "Allow organization members to view attendance photos" ON storage.objects;
DROP POLICY IF EXISTS "Allow managers to manage attendance photos" ON storage.objects;
DROP POLICY IF EXISTS "Enable upload for own organization" ON storage.objects;
DROP POLICY IF EXISTS "Enable update for own organization" ON storage.objects;
DROP POLICY IF EXISTS "Enable read for own organization" ON storage.objects;
DROP POLICY IF EXISTS "Enable delete for managers" ON storage.objects;

-- Helper to check if user belongs to the organization in the path
-- We use string_to_array to be independent of internal storage helper functions
CREATE OR REPLACE FUNCTION public.check_storage_org_access(path text, user_id uuid)
RETURNS boolean AS $$
DECLARE
    path_parts text[];
    user_org_id text;
BEGIN
    -- Path format: attendance/<org_id>/<filename>
    path_parts := string_to_array(path, '/');
    SELECT organization_id::text INTO user_org_id FROM public.users WHERE id = user_id;
    RETURN path_parts[2] = user_org_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 1. Allow employees to upload and update their own organization's photos
CREATE POLICY "Enable upload for own organization"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'attendance-proof' AND
    (string_to_array(name, '/'))[1] = 'attendance' AND
    public.check_storage_org_access(name, auth.uid())
);

CREATE POLICY "Enable update for own organization"
ON storage.objects FOR UPDATE
TO authenticated
USING (
    bucket_id = 'attendance-proof' AND
    (string_to_array(name, '/'))[1] = 'attendance' AND
    public.check_storage_org_access(name, auth.uid())
);

-- 2. Allow organization members to view photos
CREATE POLICY "Enable read for own organization"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'attendance-proof' AND
    (string_to_array(name, '/'))[1] = 'attendance' AND
    public.check_storage_org_access(name, auth.uid())
);

-- 3. Allow managers to delete photos if needed
CREATE POLICY "Enable delete for managers"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'attendance-proof' AND
    public.check_storage_org_access(name, auth.uid()) AND
    EXISTS (
        SELECT 1 FROM public.users
        WHERE id = auth.uid() AND role IN ('HEAD', 'ADMIN')
    )
);
