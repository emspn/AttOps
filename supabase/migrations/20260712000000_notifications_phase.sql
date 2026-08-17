-- Phase 6: Notification System Foundation

-- 1. FCM Tokens Table
CREATE TABLE IF NOT EXISTS public.fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    device_name TEXT,
    last_seen TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now(),
    -- Ensure a user doesn't have duplicate entries for the same token
    UNIQUE(user_id, token)
);

-- Enable RLS
ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

-- RLS Policies
DROP POLICY IF EXISTS "Users can manage their own tokens" ON public.fcm_tokens;
CREATE POLICY "Users can manage their own tokens" ON public.fcm_tokens
    FOR ALL USING (user_id = auth.uid());

-- 2. Notification Tracking (Optional but recommended for "Proof of Delivery")
CREATE TABLE IF NOT EXISTS public.notifications_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB,
    status TEXT DEFAULT 'SENT', -- SENT, DELIVERED, READ, FAILED
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.notifications_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own notification logs" ON public.notifications_log
    FOR SELECT USING (recipient_id = auth.uid());

-- 3. Cleanup Function (Keep DB lean)
CREATE OR REPLACE FUNCTION public.cleanup_old_fcm_tokens()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Delete tokens not seen for more than 60 days
    DELETE FROM public.fcm_tokens
    WHERE last_seen < now() - INTERVAL '60 days';
END;
$$;
-- FIX: Make trigger functions run with elevated privileges to prevent transaction rollback
CREATE OR REPLACE FUNCTION public.handle_new_task_notification()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.assigned_to IS NOT NULL THEN
        INSERT INTO public.notifications_log (recipient_id, title, body, data)
        VALUES (
            NEW.assigned_to,
            'New Task Assigned',
            'You have been assigned a new task: ' || NEW.title,
            jsonb_build_object(
                'type', 'NEW_TASK',
                'task_id', NEW.id,
                'refresh', 'true'
            )
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; -- CRITICAL FIX

CREATE OR REPLACE FUNCTION public.handle_task_approval_notification()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'APPROVED' AND OLD.status != 'APPROVED' THEN
        INSERT INTO public.notifications_log (recipient_id, title, body, data)
        VALUES (
            NEW.assigned_to,
            'Task Approved',
            'Your task "' || NEW.title || '" has been approved.',
            jsonb_build_object(
                'type', 'TASK_APPROVED',
                'task_id', NEW.id,
                'refresh', 'true'
            )
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; -- CRITICAL FIX

CREATE OR REPLACE FUNCTION public.handle_integrity_alert_notification()
RETURNS TRIGGER AS $$
DECLARE
    org_head_id UUID;
    employee_name TEXT;
BEGIN
    -- Added NULL check for integrity_distance
    IF NEW.integrity_distance IS NOT NULL AND NEW.integrity_distance > 200 THEN
        SELECT owner_id INTO org_head_id FROM public.organizations WHERE id = NEW.organization_id;
        SELECT full_name INTO employee_name FROM public.users WHERE id = NEW.employee_id;

        IF org_head_id IS NOT NULL THEN
            INSERT INTO public.notifications_log (recipient_id, title, body, data)
            VALUES (
                org_head_id,
                'Integrity Alert',
                'Employee ' || employee_name || ' flagged for ' || ROUND(NEW.integrity_distance::numeric, 1) || 'm deviation.',
                jsonb_build_object(
                    'type', 'INTEGRITY_ALERT',
                    'task_id', NEW.task_id,
                    'employee_id', NEW.employee_id,
                    'refresh', 'false'
                )
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; -- CRITICAL FIX

-- Also, let's give users permission to read their logs just in case
CREATE POLICY "Enable insert for authenticated users only" ON public.notifications_log
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');
