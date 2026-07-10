-- Phase 4.10: Hierarchical Task Deletion
-- Rules:
-- 1. If HEAD created the task: Only HEAD can delete.
-- 2. If ADMIN created the task: Both ADMIN and HEAD can delete.
-- 3. EMPLOYEES cannot delete tasks.

-- Drop existing delete policy if any
DROP POLICY IF EXISTS "Hierarchy Task Deletion" ON public.tasks;

CREATE POLICY "Hierarchy Task Deletion" ON public.tasks
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.users u
            WHERE u.id = auth.uid()
            AND (
                -- HEAD can delete any task in their organization
                (u.role = 'HEAD' AND u.organization_id = tasks.organization_id)
                OR
                -- ADMIN can delete tasks they created
                (u.role = 'ADMIN' AND tasks.created_by = auth.uid())
            )
        )
    );
