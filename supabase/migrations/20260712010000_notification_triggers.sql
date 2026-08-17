-- Phase 6: Notification Triggers

-- 1. Function to handle new task assignments
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_task_assigned
    AFTER INSERT ON public.tasks
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_task_notification();

-- 2. Function to handle task approval
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_task_approved
    AFTER UPDATE ON public.tasks
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_task_approval_notification();

-- 3. Function to handle integrity alerts (For Admins/Head)
CREATE OR REPLACE FUNCTION public.handle_integrity_alert_notification()
RETURNS TRIGGER AS $$
DECLARE
    org_head_id UUID;
    employee_name TEXT;
BEGIN
    IF NEW.integrity_distance > 200 THEN
        -- Get Org Owner/Head ID
        SELECT owner_id INTO org_head_id
        FROM public.organizations
        WHERE id = NEW.organization_id;

        -- Get Employee Name
        SELECT full_name INTO employee_name
        FROM public.users
        WHERE id = NEW.employee_id;

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
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_integrity_flag
    AFTER INSERT ON public.attendance_logs
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_integrity_alert_notification();

-- 4. CRITICAL: Trigger to call Edge Function
-- This trigger listens to notifications_log and calls the 'notify' Edge Function
CREATE OR REPLACE FUNCTION public.trigger_edge_notification()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM
    net.http_post(
      url := 'https://' || current_setting('request.headers')::json->>'host' || '/functions/v1/send-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || current_setting('request.headers')::json->>'authorization'
      ),
      body := jsonb_build_object('notification_id', NEW.id)
    );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Note: We'll keep the edge function trigger commented or handled via Supabase Webhooks
-- as it's easier to manage in the dashboard UI.
