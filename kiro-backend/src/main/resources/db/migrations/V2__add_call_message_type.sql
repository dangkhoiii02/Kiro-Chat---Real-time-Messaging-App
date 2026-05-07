-- Add 'CALL' to the message type constraint
ALTER TABLE message
    DROP CONSTRAINT IF EXISTS message_type_check;

ALTER TABLE message
    ADD CONSTRAINT message_type_check
        CHECK (type IN ('TEXT', 'FILE', 'IMAGE', 'VIDEO', 'CALL'));
