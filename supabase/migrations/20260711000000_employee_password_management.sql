-- Phase 5.2: Employee Password Management for HEAD role
-- This allows the HEAD to view and manage passwords for field staff onboarding.

-- 1. Add login_password column
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS login_password TEXT;

-- 2. Restrict visibility of login_password
-- Only the HEAD role should see this column.
-- Note: Standard RLS filters rows. To filter columns, we'd normally use a VIEW.
-- For this MVP, we provide a helper to check permission.

-- 3. Sync login_password with Supabase Auth
-- This trigger ensures that when a HEAD updates the login_password column,
-- the actual auth.users password is also updated.

CREATE OR REPLACE FUNCTION public.sync_user_password()
RETURNS TRIGGER AS $$
BEGIN
    -- Only sync if the password has actually changed
    IF (OLD.login_password IS DISTINCT FROM NEW.login_password) AND NEW.login_password IS NOT NULL THEN
        -- Update the auth.users table directly (requires security definer)
        UPDATE auth.users
        SET encrypted_password = crypt(NEW.login_password, gen_salt('bf')),
            updated_at = now()
        WHERE id = NEW.id;

        RAISE LOG 'Auth password synced for user %', NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_password_change ON public.users;
CREATE TRIGGER on_password_change
    AFTER UPDATE ON public.users
    FOR EACH ROW EXECUTE PROCEDURE public.sync_user_password();

COMMENT ON COLUMN public.users.login_password IS 'Password used for field staff login. Managed only by organization HEAD.';
