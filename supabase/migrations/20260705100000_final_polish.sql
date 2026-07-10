-- Phase 4.9: Final Polish & Audit Trail
-- Ensure integrity_distance is writable by users
DROP POLICY IF EXISTS "Users can check-in/out" ON public.attendance_logs;
CREATE POLICY "Users can check-in/out" ON public.attendance_logs
    FOR ALL USING (
        employee_id = auth.uid()
    ) WITH CHECK (
        employee_id = auth.uid()
    );

-- Add column comment for documentation
COMMENT ON COLUMN public.attendance_logs.integrity_distance IS 'Distance in meters between check-in and check-out GPS points.';
