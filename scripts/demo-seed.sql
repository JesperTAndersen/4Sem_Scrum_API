-- =====================================================================================
-- Demodata til eksamen
--
-- Virker både på en tom database og på en eksisterende (lokal eller prod).
--   1. Start backend én gang, så Hibernate har oprettet tabellerne.
--   2. Opret demo-brugeren via POST /auth/register (e-mail som i afsnit 1 nedenfor).
--   3. Kør hele scriptet i pgAdmin (Query Tool, F5).
--
-- Scriptet kan køres flere gange. Demoprojekterne genkendes på titlen og oprettes på ny,
-- så der aldrig opstår dubletter. Andre projekter røres ikke.
--
-- Alle datoer regnes ud fra mandag i den uge, scriptet køres. Kør det derfor igen ugen
-- før eksamen, så "i dag" ligger midt i hovedprojektet.
--
-- Hvad demoen viser:
--   Villarenovering Hellerup   Hovedprojekt med 8 faser og 27 opgaver over ca. 10 uger.
--                              Færdige, igangværende og kommende opgaver, faser der afløser
--                              hinanden, opgaveafhængigheder, minimumsvarighed (udtørring),
--                              en ubemandet opgave og to bemandingsadvarsler:
--                                - Henrik Olsen er inaktiv (Flisearbejde)
--                                - Mette Hansen mangler kompetencen Gulvlægger (Fodlister)
--   Tagudskiftning Roskilde    I risiko: bliver færdig efter deadline.
--                              Morten Jensen er dobbeltbooket med Gipsvægge i Hellerup.
--   Lejlighed Frederiksberg    Planlagt. Emma Kristensen er dobbeltbooket med Maling i Hellerup.
--                              Flisearbejdet er ikke bemandet.
--   Tilbygning Gentofte        Kan ikke beregnes: kun en inaktiv medarbejder er tildelt.
--   Kontorombygning Ørestad    Kladde uden bemanding.
--   Altanrenovering Nørrebro   Afsluttet.
-- =====================================================================================

BEGIN;

-- -------------------------------------------------------------------------------------
-- 0. Migrering: én estimate-kolonne -> trepunktsestimat (best/likely/worst)
--    Gør ingenting, hvis databasen allerede har de nye kolonner.
-- -------------------------------------------------------------------------------------
ALTER TABLE task ADD COLUMN IF NOT EXISTS bestestimate   float8;
ALTER TABLE task ADD COLUMN IF NOT EXISTS likelyestimate float8;
ALTER TABLE task ADD COLUMN IF NOT EXISTS worstestimate  float8;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'task' AND column_name = 'estimate') THEN
        UPDATE task
           SET bestestimate   = COALESCE(bestestimate,   estimate, 0),
               likelyestimate = COALESCE(likelyestimate, estimate, 0),
               worstestimate  = COALESCE(worstestimate,  estimate, 0);
        ALTER TABLE task DROP COLUMN estimate;
    ELSE
        UPDATE task
           SET bestestimate   = COALESCE(bestestimate,   0),
               likelyestimate = COALESCE(likelyestimate, 0),
               worstestimate  = COALESCE(worstestimate,  0)
         WHERE bestestimate IS NULL OR likelyestimate IS NULL OR worstestimate IS NULL;
    END IF;
END $$;

ALTER TABLE task ALTER COLUMN bestestimate   SET NOT NULL;
ALTER TABLE task ALTER COLUMN likelyestimate SET NOT NULL;
ALTER TABLE task ALTER COLUMN worstestimate  SET NOT NULL;

-- -------------------------------------------------------------------------------------
-- 1. Projektleder: projekter skal have en opretter.
--    /auth/register opretter altid en EMPLOYEE, så brugeren forfremmes her.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    v_demo_manager_email CONSTANT text := 'demo@estimo.dk';
BEGIN
    UPDATE users SET user_role = 'PROJECT_MANAGER', updated_at = now()
     WHERE lower(email) = lower(v_demo_manager_email) AND user_role <> 'PROJECT_MANAGER';

    IF NOT EXISTS (SELECT 1 FROM users WHERE user_role = 'PROJECT_MANAGER') THEN
        RAISE EXCEPTION 'Der findes ingen PROJECT_MANAGER. Opret % via POST /auth/register og kør scriptet igen.',
            v_demo_manager_email;
    END IF;
END $$;

-- -------------------------------------------------------------------------------------
-- 2. Stamdata: kapacitet, kompetencer og medarbejdere
-- -------------------------------------------------------------------------------------
-- Virksomhedens standardkapacitet (7,5 timer/dag)
INSERT INTO companycapacity (id, dailycapacity)
VALUES (1, 7.5)
ON CONFLICT (id) DO NOTHING;

-- Kompetencer (navnene matcher ikonerne i frontend)
INSERT INTO competences (name_key, name, rate, active, created_at, updated_at)
VALUES
    ('tømrer',      'Tømrer',      950, true, now(), now()),
    ('maler',       'Maler',       850, true, now(), now()),
    ('vvs',         'VVS',         975, true, now(), now()),
    ('elektriker',  'Elektriker',  995, true, now(), now()),
    ('murer',       'Murer',       925, true, now(), now()),
    ('arbejdsmand', 'Arbejdsmand', 650, true, now(), now()),
    ('ventilation', 'Ventilation', 900, true, now(), now()),
    ('gulvlægger',  'Gulvlægger',  875, true, now(), now()),
    ('montør',      'Montør',      800, true, now(), now())
ON CONFLICT (name_key) DO NOTHING;

-- Medarbejdere. dailycapacity bruges kun, når usesstandard er false.
INSERT INTO employee (firstname, lastname, dailycapacity, usesstandardcapacity, standardcapacity_id, active, createdat, updatedat)
SELECT v.firstname, v.lastname, v.dailycapacity, v.usesstandard, CASE WHEN v.usesstandard THEN 1 END, v.active, now(), now()
FROM (VALUES
    ('Lars',     'Nielsen',    0,   true,  true),
    ('Mette',    'Hansen',     0,   true,  true),
    ('Ali',      'Hassan',     6.0, false, true),
    ('Sofie',    'Larsen',     0,   true,  true),
    ('Jonas',    'Petersen',   5.0, false, true),
    ('Peter',    'Madsen',     0,   true,  true),
    ('Emma',     'Kristensen', 7.0, false, true),
    ('Morten',   'Jensen',     0,   true,  true),
    ('Kasper',   'Holm',       0,   true,  true),
    ('Nanna',    'Friis',      0,   true,  true),
    ('Rasmus',   'Bach',       0,   true,  true),
    ('Ida',      'Lund',       0,   true,  true),
    ('Frederik', 'Dam',        0,   true,  true),
    ('Camilla',  'Krogh',      0,   true,  true),
    ('Anders',   'Skov',       0,   true,  true),
    ('Louise',   'Bang',       6.0, false, true),
    ('Henrik',   'Olsen',      0,   true,  false)
) AS v(firstname, lastname, dailycapacity, usesstandard, active)
WHERE NOT EXISTS (
    SELECT 1 FROM employee e
    WHERE e.firstname = v.firstname AND e.lastname = v.lastname
);

-- Kompetencer pr. medarbejder
INSERT INTO employee_competences (employee_id, competences_competence_id)
SELECT e.id, c.competence_id
FROM (VALUES
    ('Lars',     'Nielsen',    'tømrer'),
    ('Lars',     'Nielsen',    'gulvlægger'),
    ('Mette',    'Hansen',     'maler'),
    ('Mette',    'Hansen',     'arbejdsmand'),
    ('Ali',      'Hassan',     'vvs'),
    ('Ali',      'Hassan',     'ventilation'),
    ('Sofie',    'Larsen',     'elektriker'),
    ('Jonas',    'Petersen',   'arbejdsmand'),
    ('Jonas',    'Petersen',   'murer'),
    ('Peter',    'Madsen',     'tømrer'),
    ('Peter',    'Madsen',     'montør'),
    ('Emma',     'Kristensen', 'maler'),
    ('Emma',     'Kristensen', 'gulvlægger'),
    ('Morten',   'Jensen',     'tømrer'),
    ('Kasper',   'Holm',       'tømrer'),
    ('Nanna',    'Friis',      'elektriker'),
    ('Rasmus',   'Bach',       'murer'),
    ('Ida',      'Lund',       'vvs'),
    ('Frederik', 'Dam',        'arbejdsmand'),
    ('Camilla',  'Krogh',      'maler'),
    ('Anders',   'Skov',       'montør'),
    ('Anders',   'Skov',       'ventilation'),
    ('Louise',   'Bang',       'gulvlægger'),
    ('Henrik',   'Olsen',      'murer')
) AS v(firstname, lastname, competence)
JOIN employee e ON e.firstname = v.firstname AND e.lastname = v.lastname
JOIN competences c ON c.name_key = v.competence
WHERE NOT EXISTS (
    SELECT 1 FROM employee_competences ec
    WHERE ec.employee_id = e.id AND ec.competences_competence_id = c.competence_id
);

-- -------------------------------------------------------------------------------------
-- 3. Slet demoprojekter (nuværende og tidligere titler, så gamle dubletter også ryddes op)
-- -------------------------------------------------------------------------------------
CREATE TEMP TABLE demo_projects ON COMMIT DROP AS
    SELECT id FROM projects WHERE title IN (
        -- Nuværende demoprojekter
        'Villarenovering Hellerup',
        'Tagudskiftning Roskilde',
        'Lejlighed Frederiksberg',
        'Tilbygning Gentofte',
        'Kontorombygning Ørestad',
        'Altanrenovering Nørrebro',
        -- Fra tidligere versioner af scriptet
        'Køkkenrenovering Valby',
        'Gulvrenovering Amager',
        'Badeværelse Frederiksberg',
        'Tilbygning Hellerup',
        'Elinstallation Lyngby'
    );

CREATE TEMP TABLE demo_stages ON COMMIT DROP AS
    SELECT id FROM stage WHERE project_id IN (SELECT id FROM demo_projects);

CREATE TEMP TABLE demo_tasks ON COMMIT DROP AS
    SELECT id FROM task WHERE stage_id IN (SELECT id FROM demo_stages);

DELETE FROM task_employee WHERE task_id IN (SELECT id FROM demo_tasks);
DELETE FROM task_predecessors
    WHERE task_id IN (SELECT id FROM demo_tasks) OR predecessor_id IN (SELECT id FROM demo_tasks);
DELETE FROM task WHERE id IN (SELECT id FROM demo_tasks);
DELETE FROM stage_predecessors
    WHERE stage_id IN (SELECT id FROM demo_stages) OR predecessor_id IN (SELECT id FROM demo_stages);
DELETE FROM stage WHERE id IN (SELECT id FROM demo_stages);
DELETE FROM projects WHERE id IN (SELECT id FROM demo_projects);

-- -------------------------------------------------------------------------------------
-- 4. Hjælpefunktioner (pg_temp: de forsvinder igen, når forbindelsen lukkes)
-- -------------------------------------------------------------------------------------
-- p_start og p_deadline er antal dage fra mandag i indeværende uge.
CREATE OR REPLACE FUNCTION pg_temp.add_project(p_title text, p_status text, p_start integer, p_deadline integer, p_description text)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    v_monday  date := date_trunc('week', current_date)::date;
    v_manager bigint;
    v_id      bigint;
BEGIN
    SELECT id INTO v_manager FROM users WHERE user_role = 'PROJECT_MANAGER' ORDER BY id LIMIT 1;

    INSERT INTO projects (title, description, status, startdate, deadline, created_by, updated_by, createdat, updatedat)
    VALUES (p_title, p_description, p_status, v_monday + p_start, v_monday + p_deadline,
            v_manager, v_manager, now(), now())
    RETURNING id INTO v_id;

    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.add_stage(p_project_id bigint, p_name text, p_predecessors bigint[] DEFAULT '{}')
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    v_id bigint;
BEGIN
    INSERT INTO stage (project_id, name, createdat, updatedat)
    VALUES (p_project_id, p_name, now(), now())
    RETURNING id INTO v_id;

    INSERT INTO stage_predecessors (stage_id, predecessor_id)
    SELECT v_id, unnest(p_predecessors);

    RETURN v_id;
END $$;

-- p_best/p_likely/p_worst er timer. PERT-estimatet er (best + 4 * likely + worst) / 6.
-- p_employees er fulde navne ("Fornavn Efternavn") på eksisterende medarbejdere.
CREATE OR REPLACE FUNCTION pg_temp.add_task(p_stage_id bigint, p_name text, p_competence text,
                                            p_best double precision, p_likely double precision, p_worst double precision,
                                            p_status text DEFAULT 'NOT_STARTED', p_employees text[] DEFAULT '{}',
                                            p_predecessors bigint[] DEFAULT '{}', p_minimum_days integer DEFAULT 1)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    v_competence bigint;
    v_employee   text;
    v_id         bigint;
BEGIN
    SELECT competence_id INTO v_competence FROM competences WHERE name_key = lower(p_competence);
    IF v_competence IS NULL THEN
        RAISE EXCEPTION 'Kompetencen "%" findes ikke', p_competence;
    END IF;

    INSERT INTO task (stage_id, name, competence_competence_id, bestestimate, likelyestimate, worstestimate,
                      minimumdurationindays, status, createdat, updatedat)
    VALUES (p_stage_id, p_name, v_competence, p_best, p_likely, p_worst,
            p_minimum_days, p_status, now(), now())
    RETURNING id INTO v_id;

    FOREACH v_employee IN ARRAY p_employees
    LOOP
        INSERT INTO task_employee (task_id, employees_id)
        SELECT v_id, e.id FROM employee e WHERE e.firstname || ' ' || e.lastname = v_employee;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Medarbejderen "%" findes ikke', v_employee;
        END IF;
    END LOOP;

    INSERT INTO task_predecessors (task_id, predecessor_id)
    SELECT v_id, unnest(p_predecessors);

    RETURN v_id;
END $$;

-- -------------------------------------------------------------------------------------
-- 5. Hovedprojekt: Villarenovering Hellerup
--    Startede for 3 uger siden. Kommentarerne viser PERT-estimat og planlagt periode,
--    hvor M er mandag i indeværende uge og arbejdsdage tælles fra projektstart.
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    p  bigint;
    s1 bigint; s2 bigint; s3 bigint; s4 bigint; s5 bigint; s6 bigint; s7 bigint; s8 bigint;
    t1 bigint; t2 bigint; t3 bigint;
BEGIN
    p := pg_temp.add_project('Villarenovering Hellerup', 'IN_PROGRESS', -21, 53,
                             'Totalrenovering af 1930''er-villa på 160 m2: nyt køkken og bad, el, VVS, ventilation, gulve og overflader');

    -- Uge 1: nedrivning
    s1 := pg_temp.add_stage(p, 'Nedrivning og klargøring');
    t1 := pg_temp.add_task(s1, 'Byggepladsindretning og afdækning', 'Arbejdsmand', 12, 14, 22, 'DONE',
                           ARRAY['Frederik Dam']);                                                        -- 15 t
    PERFORM pg_temp.add_task(s1, 'Nedrivning af køkken og bad', 'Arbejdsmand', 36, 44, 58, 'DONE',
                             ARRAY['Frederik Dam', 'Mette Hansen'], ARRAY[t1]);                           -- 45 t
    PERFORM pg_temp.add_task(s1, 'Nedtagning af gamle gulve', 'Arbejdsmand', 12, 14, 22, 'DONE',
                             ARRAY['Jonas Petersen'], ARRAY[t1]);                                         -- 15 t

    -- Uge 2-3: råhus
    s2 := pg_temp.add_stage(p, 'Råhus og konstruktion', ARRAY[s1]);
    PERFORM pg_temp.add_task(s2, 'Udskiftning af bjælkelag', 'Tømrer', 48, 58, 80, 'DONE',
                             ARRAY['Kasper Holm', 'Morten Jensen']);                                      -- 60 t
    t1 := pg_temp.add_task(s2, 'Opmuring af ny skillevæg', 'Murer', 24, 29, 40, 'DONE',
                           ARRAY['Rasmus Bach']);                                                         -- 30 t
    PERFORM pg_temp.add_task(s2, 'Nye dør- og vindueshuller', 'Tømrer', 18, 21, 30, 'DONE',
                             ARRAY['Peter Madsen'], ARRAY[t1]);                                           -- 22 t
    PERFORM pg_temp.add_task(s2, 'Støbning af gulv i bad', 'Murer', 12, 14, 22, 'DONE',
                             ARRAY['Rasmus Bach'], ARRAY[t1]);                                            -- 15 t

    -- Uge 3-4: installationer (igangværende, slutter fredag i indeværende uge)
    s3 := pg_temp.add_stage(p, 'Installationer', ARRAY[s2]);
    PERFORM pg_temp.add_task(s3, 'Kabelføring og ny el-tavle', 'Elektriker', 96, 116, 160, 'IN_PROGRESS',
                             ARRAY['Sofie Larsen', 'Nanna Friis']);                                       -- 120 t
    PERFORM pg_temp.add_task(s3, 'Rørføring vand og afløb', 'VVS', 70, 88, 118, 'IN_PROGRESS',
                             ARRAY['Ali Hassan', 'Ida Lund']);                                            -- 90 t
    PERFORM pg_temp.add_task(s3, 'Ventilationskanaler', 'Ventilation', 24, 29, 40, 'DONE',
                             ARRAY['Anders Skov']);                                                       -- 30 t
    -- Lille estimat, men betonen skal tørre i mindst 8 dage
    PERFORM pg_temp.add_task(s3, 'Udtørring af gulvstøbning', 'Murer', 3, 4, 5, 'IN_PROGRESS',
                             ARRAY['Rasmus Bach'], '{}', 8);                                              -- 4 t, min. 8 dage

    -- Uge 5-7: vægge og lofter
    s4 := pg_temp.add_stage(p, 'Vægge og lofter', ARRAY[s3]);
    t1 := pg_temp.add_task(s4, 'Gipsvægge og lofter', 'Tømrer', 60, 73, 98, 'NOT_STARTED',
                           ARRAY['Kasper Holm', 'Morten Jensen']);                                        -- 75 t (Morten dobbeltbooket)
    PERFORM pg_temp.add_task(s4, 'Isolering af ydervægge', 'Tømrer', 24, 29, 40, 'NOT_STARTED',
                             ARRAY['Peter Madsen']);                                                      -- 30 t
    PERFORM pg_temp.add_task(s4, 'Spartling og slibning', 'Maler', 36, 44, 58, 'NOT_STARTED',
                             ARRAY['Camilla Krogh'], ARRAY[t1]);                                          -- 45 t

    -- Uge 5-7: badeværelse (parallelt med vægge og lofter)
    s5 := pg_temp.add_stage(p, 'Badeværelse', ARRAY[s3]);
    t1 := pg_temp.add_task(s5, 'Vådrumssikring', 'Murer', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Rasmus Bach']);                                                         -- 15 t
    -- Henrik Olsen er inaktiv: bemandingsadvarsel, men Rasmus kan stadig planlægges
    t2 := pg_temp.add_task(s5, 'Flisearbejde gulv og væg', 'Murer', 42, 50, 70, 'NOT_STARTED',
                           ARRAY['Rasmus Bach', 'Henrik Olsen'], ARRAY[t1]);                              -- 52 t
    PERFORM pg_temp.add_task(s5, 'Gulvvarme i bad', 'Elektriker', 12, 14, 22, 'NOT_STARTED',
                             ARRAY['Nanna Friis'], ARRAY[t1]);                                            -- 15 t
    PERFORM pg_temp.add_task(s5, 'Montering af sanitet', 'VVS', 18, 21, 30, 'NOT_STARTED',
                             ARRAY['Ida Lund'], ARRAY[t2]);                                               -- 22 t

    -- Uge 7-9: overflader
    s6 := pg_temp.add_stage(p, 'Overflader', ARRAY[s4, s5]);
    t1 := pg_temp.add_task(s6, 'Maling af vægge og lofter', 'Maler', 56, 70, 96, 'NOT_STARTED',
                           ARRAY['Emma Kristensen', 'Camilla Krogh']);                                    -- 72 t (Emma dobbeltbooket)
    PERFORM pg_temp.add_task(s6, 'Lægning af parket', 'Gulvlægger', 48, 58, 80, 'NOT_STARTED',
                             ARRAY['Lars Nielsen', 'Louise Bang'], ARRAY[t1]);                            -- 60 t

    -- Uge 9-10: køkken
    s7 := pg_temp.add_stage(p, 'Køkken', ARRAY[s6]);
    t1 := pg_temp.add_task(s7, 'Montering af køkkenelementer', 'Montør', 36, 44, 58, 'NOT_STARTED',
                           ARRAY['Peter Madsen']);                                                        -- 45 t
    -- Ikke bemandet endnu: planlægges med 7,5 timer om dagen
    PERFORM pg_temp.add_task(s7, 'Montering af hvidevarer', 'Montør', 12, 14, 22, 'NOT_STARTED',
                             '{}', ARRAY[t1]);                                                            -- 15 t
    PERFORM pg_temp.add_task(s7, 'El-tilslutning i køkken', 'Elektriker', 6, 8, 10, 'NOT_STARTED',
                             ARRAY['Sofie Larsen'], ARRAY[t1]);                                           -- 8 t
    PERFORM pg_temp.add_task(s7, 'VVS-tilslutning i køkken', 'VVS', 9, 12, 15, 'NOT_STARTED',
                             ARRAY['Ali Hassan'], ARRAY[t1]);                                             -- 12 t

    -- Uge 11: aflevering
    s8 := pg_temp.add_stage(p, 'Aflevering', ARRAY[s7]);
    t1 := pg_temp.add_task(s8, 'Slutrengøring', 'Arbejdsmand', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Frederik Dam', 'Mette Hansen']);                                        -- 15 t
    -- Mette Hansen er ikke gulvlægger: bemandingsadvarsel
    t2 := pg_temp.add_task(s8, 'Fodlister og finish', 'Gulvlægger', 9, 12, 15, 'NOT_STARTED',
                           ARRAY['Louise Bang', 'Mette Hansen']);                                         -- 12 t
    PERFORM pg_temp.add_task(s8, 'Mangelgennemgang og aflevering', 'Tømrer', 3, 4, 5, 'NOT_STARTED',
                             ARRAY['Morten Jensen'], ARRAY[t1, t2]);                                      -- 4 t
END $$;

-- -------------------------------------------------------------------------------------
-- 6. Øvrige projekter: dashboard-variation og konflikter med hovedprojektet
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    p  bigint;
    s1 bigint;
    s2 bigint;
    t1 bigint;
BEGIN
    -- I risiko. Tagarbejdet overlapper Gipsvægge i Hellerup, så Morten Jensen er overbooket.
    p  := pg_temp.add_project('Tagudskiftning Roskilde', 'IN_PROGRESS', -7, 11,
                              'Udskiftning af tegltag og tagrender på parcelhus');
    s1 := pg_temp.add_stage(p, 'Stillads og nedtagning');
    s2 := pg_temp.add_stage(p, 'Nyt tag', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Opsætning af stillads', 'Montør', 12, 14, 22, 'DONE',
                           ARRAY['Peter Madsen']);                                                        -- 15 t
    PERFORM pg_temp.add_task(s1, 'Nedtagning af gammelt tag', 'Arbejdsmand', 36, 44, 58, 'DONE',
                             ARRAY['Jonas Petersen', 'Frederik Dam'], ARRAY[t1]);                         -- 45 t
    t1 := pg_temp.add_task(s2, 'Lægning af nyt tag', 'Tømrer', 90, 110, 142, 'IN_PROGRESS',
                           ARRAY['Morten Jensen', 'Lars Nielsen']);                                       -- 112 t
    PERFORM pg_temp.add_task(s2, 'Inddækning og tagrender', 'Tømrer', 18, 21, 30, 'NOT_STARTED',
                             ARRAY['Morten Jensen'], ARRAY[t1]);                                          -- 22 t

    -- Planlagt og kan nås. Maling overlapper Maling i Hellerup, så Emma Kristensen er overbooket.
    p  := pg_temp.add_project('Lejlighed Frederiksberg', 'PLANNED', 21, 39,
                              'Nyt badeværelse og maling af stue og gang i ejerlejlighed');
    s1 := pg_temp.add_stage(p, 'Badeværelse');
    s2 := pg_temp.add_stage(p, 'Maling');
    t1 := pg_temp.add_task(s1, 'Nedrivning af fliser', 'Arbejdsmand', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Frederik Dam']);                                                        -- 15 t
    t1 := pg_temp.add_task(s1, 'Rørføring', 'VVS', 24, 29, 40, 'NOT_STARTED',
                           ARRAY['Ali Hassan'], ARRAY[t1]);                                               -- 30 t
    PERFORM pg_temp.add_task(s1, 'Flisearbejde', 'Murer', 30, 37, 44, 'NOT_STARTED',
                             '{}', ARRAY[t1]);                                                            -- 37 t, ikke bemandet
    PERFORM pg_temp.add_task(s2, 'Maling af stue og gang', 'Maler', 22, 28, 34, 'NOT_STARTED',
                             ARRAY['Emma Kristensen']);                                                   -- 28 t

    -- Kan ikke beregnes: fundamentet er kun tildelt Henrik Olsen, som er inaktiv.
    p  := pg_temp.add_project('Tilbygning Gentofte', 'PLANNED', 63, 88,
                              'Tilbygning på 25 m2 til eksisterende villa');
    s1 := pg_temp.add_stage(p, 'Fundament');
    s2 := pg_temp.add_stage(p, 'Råhus', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Støbning af fundament', 'Murer', 48, 58, 80, 'NOT_STARTED',
                             ARRAY['Henrik Olsen']);                                                      -- 60 t
    PERFORM pg_temp.add_task(s2, 'Opbygning af vægge og tag', 'Tømrer', 60, 73, 98, 'NOT_STARTED',
                             ARRAY['Kasper Holm']);                                                       -- 75 t

    -- Kladde uden bemanding: varigheden regnes med 7,5 timer om dagen.
    p  := pg_temp.add_project('Kontorombygning Ørestad', 'DRAFT', 77, 140,
                              'Ombygning af åbent kontor til mødelokaler');
    s1 := pg_temp.add_stage(p, 'Skillevægge og ventilation');
    s2 := pg_temp.add_stage(p, 'Overflader', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Opsætning af skillevægge', 'Tømrer', 48, 58, 80);                       -- 60 t
    PERFORM pg_temp.add_task(s1, 'Nye ventilationskanaler', 'Ventilation', 24, 29, 40);                    -- 30 t
    PERFORM pg_temp.add_task(s2, 'Maling af kontorer', 'Maler', 36, 44, 58);                               -- 45 t
    PERFORM pg_temp.add_task(s2, 'Nyt tæppegulv', 'Gulvlægger', 24, 29, 40);                               -- 30 t

    -- Afsluttet.
    p  := pg_temp.add_project('Altanrenovering Nørrebro', 'COMPLETED', -77, -66,
                              'Renovering af altan i etageejendom');
    s1 := pg_temp.add_stage(p, 'Altan');
    t1 := pg_temp.add_task(s1, 'Reparation af altanplade', 'Murer', 60, 73, 98, 'DONE',
                           ARRAY['Jonas Petersen']);                                                      -- 75 t
    PERFORM pg_temp.add_task(s1, 'Montering af nyt rækværk', 'Montør', 12, 14, 22, 'DONE',
                             ARRAY['Peter Madsen'], ARRAY[t1]);                                           -- 15 t
END $$;

COMMIT;

-- -------------------------------------------------------------------------------------
-- 7. Kontrol: viser demoprojekterne
-- -------------------------------------------------------------------------------------
SELECT p.id, p.title, p.status, p.startdate, p.deadline,
       count(t.id)                                                                     AS opgaver,
       count(t.id) FILTER (WHERE t.status = 'DONE')                                    AS faerdige,
       coalesce(sum(round((t.bestestimate + 4 * t.likelyestimate + t.worstestimate) / 6)), 0) AS timer
FROM projects p
LEFT JOIN stage s ON s.project_id = p.id
LEFT JOIN task t ON t.stage_id = s.id
WHERE p.title IN ('Villarenovering Hellerup', 'Tagudskiftning Roskilde', 'Lejlighed Frederiksberg',
                  'Tilbygning Gentofte', 'Kontorombygning Ørestad', 'Altanrenovering Nørrebro')
GROUP BY p.id
ORDER BY p.startdate, p.id;
