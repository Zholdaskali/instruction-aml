CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO t_app_users (id, username, password, role, region) VALUES
('00000000-0000-0000-0000-000000000001', 'supervisor-north',
 crypt('supervisor123', gen_salt('bf', 10)), 'SUPERVISOR', 'NORTH'),

('00000000-0000-0000-0000-000000000002', 'anton',
 crypt('anton123', gen_salt('bf', 10)), 'INVESTIGATOR', 'NORTH'),

('00000000-0000-0000-0000-000000000003', 'vera',
 crypt('vera123', gen_salt('bf', 10)), 'INVESTIGATOR', 'NORTH'),

('00000000-0000-0000-0000-000000000004', 'supervisor-south',
 crypt('south123', gen_salt('bf', 10)), 'SUPERVISOR', 'SOUTH');

INSERT INTO t_case_files (id, case_number, region) VALUES
('10000000-0000-0000-0000-000000000001', 'DEMO-001', 'NORTH'),
('10000000-0000-0000-0000-000000000002', 'DEMO-002', 'SOUTH');

INSERT INTO t_case_participants (id, case_id, user_id) VALUES
(
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002'
);