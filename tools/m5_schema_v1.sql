PRAGMA foreign_keys = ON;

CREATE TABLE campaign_meta (
  campaign_id TEXT PRIMARY KEY NOT NULL,
  display_name TEXT NOT NULL,
  revision INTEGER NOT NULL,
  schema_version INTEGER NOT NULL,
  app_data_version INTEGER NOT NULL,
  state_checksum TEXT NOT NULL,
  rng_algorithm TEXT NOT NULL,
  master_seed TEXT NOT NULL,
  updated_at_audit INTEGER NOT NULL
);

CREATE TABLE campaign_state (
  campaign_id TEXT PRIMARY KEY NOT NULL,
  revision INTEGER NOT NULL,
  state_json TEXT NOT NULL,
  checksum TEXT NOT NULL
);

CREATE TABLE campaign_event (
  event_id TEXT PRIMARY KEY NOT NULL,
  campaign_id TEXT NOT NULL,
  revision INTEGER NOT NULL,
  event_kind TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  checksum_before TEXT,
  checksum_after TEXT NOT NULL,
  created_at_audit INTEGER NOT NULL
);
CREATE INDEX idx_campaign_event_campaign_revision ON campaign_event(campaign_id, revision);

CREATE TABLE campaign_snapshot (
  snapshot_id TEXT PRIMARY KEY NOT NULL,
  campaign_id TEXT NOT NULL,
  revision INTEGER NOT NULL,
  state_json TEXT NOT NULL,
  checksum TEXT NOT NULL,
  schema_version INTEGER NOT NULL,
  rng_algorithm TEXT NOT NULL,
  rng_counters_json TEXT NOT NULL,
  last_event_id TEXT,
  created_at_audit INTEGER NOT NULL
);
CREATE INDEX idx_campaign_snapshot_campaign_revision ON campaign_snapshot(campaign_id, revision);

CREATE TABLE app_meta (
  meta_key TEXT PRIMARY KEY NOT NULL,
  meta_value TEXT NOT NULL
);
PRAGMA user_version = 1;
