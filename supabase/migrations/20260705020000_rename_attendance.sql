-- Standardizing table name to user requirement
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'task_attendance')
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'attendance_logs') THEN
    ALTER TABLE public.task_attendance RENAME TO attendance_logs;
  ELSE
    RAISE NOTICE 'Rename skipped: task_attendance missing or attendance_logs already exists.';
  END IF;
END $$;
