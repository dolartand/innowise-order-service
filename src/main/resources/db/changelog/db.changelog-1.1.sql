-- liquibase formatted sql

-- changeset dolartand:1
ALTER TABLE items ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;

-- changeset dolartand:2
CREATE INDEX IF NOT EXISTS idx_items_deleted ON items(deleted);