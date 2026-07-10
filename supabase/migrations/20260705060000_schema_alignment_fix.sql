-- Phase 4.4: Final Schema Alignment Fix
-- This migration ensures the attendance_logs table matches the app's expectations exactly.

-- 1. Ensure columns exist with correct names
DO $$
BEGIN
  -- Handle distance column naming consistency
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='attendance_logs' AND column_name='distance_from_task_location') THEN
    -- Column already exists, all good.
    NULL;
  ELSE
    ALTER TABLE public.attendance_logs ADD COLUMN distance_from_task_location DOUBLE PRECISION;
  END IF;

  -- Ensure check_in_image_url exists (migrated from image_url previously)
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='attendance_logs' AND column_name='check_in_image_url') THEN
    ALTER TABLE public.attendance_logs ADD COLUMN check_in_image_url TEXT;
  END IF;

  -- Ensure check_out_image_url exists
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='attendance_logs' AND column_name='check_out_image_url') THEN
    ALTER TABLE public.attendance_logs ADD COLUMN check_out_image_url TEXT;
  END IF;
END $$;

-- 2. Force a schema cache refresh on PostgREST
-- In Supabase, this is usually automatic, but we can nudge it by performing a DDL operation.
COMMENT ON TABLE public.attendance_logs IS 'Employee attendance records with location and photo proof';

-- 3. Re-verify RLS to ensure column access
DROP POLICY IF EXISTS "Enable upload for own organization" ON storage.objects;
CREATE POLICY "Enable upload for own organization"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'attendance-proof' AND
    (string_to_array(name, '/'))[1] = 'attendance' AND
    public.check_storage_org_access(name, auth.uid())
);
