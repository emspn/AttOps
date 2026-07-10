-- Phase 4.2: Storage Setup for Attendance Proofs

-- 1. Create the bucket
-- Using the storage API table directly is standard for migrations
INSERT INTO storage.buckets (id, name, public)
VALUES ('attendance-proof', 'attendance-proof', true)
ON CONFLICT (id) DO NOTHING;

-- 2. RLS Policies for attendance-proof bucket
-- We skip ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY as it is managed by the platform

-- Allow employees to upload photos (INSERT)
-- Path format: attendance/{org_id}/{filename}
DROP POLICY IF EXISTS "Allow employees to upload attendance photos" ON storage.objects;
CREATE POLICY "Allow employees to upload attendance photos"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'attendance-proof' AND
    (storage.foldername(name))[1] = 'attendance' AND
    (storage.foldername(name))[2] = (SELECT organization_id::text FROM public.users WHERE id = auth.uid())
);

-- Allow organization members to view photos (SELECT)
DROP POLICY IF EXISTS "Allow organization members to view attendance photos" ON storage.objects;
CREATE POLICY "Allow organization members to view attendance photos"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'attendance-proof' AND
    (storage.foldername(name))[1] = 'attendance' AND
    (storage.foldername(name))[2] = (SELECT organization_id::text FROM public.users WHERE id = auth.uid())
);

-- Allow owners and admins to manage (delete/update) if needed
DROP POLICY IF EXISTS "Allow managers to manage attendance photos" ON storage.objects;
CREATE POLICY "Allow managers to manage attendance photos"
ON storage.objects FOR ALL
TO authenticated
USING (
    bucket_id = 'attendance-proof' AND
    (storage.foldername(name))[1] = 'attendance' AND
    (storage.foldername(name))[2] = (SELECT organization_id::text FROM public.users WHERE id = auth.uid()) AND
    EXISTS (
        SELECT 1 FROM public.users
        WHERE id = auth.uid() AND role IN ('HEAD', 'ADMIN')
    )
);
