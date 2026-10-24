-- =====================================================================================
-- Demodata til eksamen
--
-- Virker både på en tom database og på en eksisterende (lokal eller prod).
--   1. Start backend én gang, så Hibernate har oprettet tabellerne.
--   2. Opret demo-brugeren via POST /auth/register (e-mail som i afsnit 1 nedenfor).
--   3. Kør hele scriptet i pgAdmin (Query Tool, F5).
--
-- Scriptet sletter ALLE projekter og opretter kun demoprojekterne igen, så testdata ikke
-- ses i demoen. Medarbejdere, kompetencer og brugere bliver. Det kan køres flere gange, og
-- er demoen øvet, nulstilles den ved at køre scriptet igen.
--
-- Alle datoer regnes ud fra demo.monday, som er sat til mandag i præsentationsugen
-- (tirsdag den 27. oktober 2026). Så er det lige meget, hvilken dag scriptet køres.
-- Status på opgaverne passer til en demo om tirsdagen: det, der slutter før tirsdag, er
-- færdigt, og det, der er i gang tirsdag, står som igangværende.
-- Til en anden demo: ret datoen nedenfor. Fjernes linjen, bruges mandag i den uge, scriptet
-- køres (i weekenden mandag i ugen efter).
--
-- Kontrollen nederst skal vise 9 rækker, alle med demoprojekt = true.
--
-- Demo-forløb (se også afsnittet "Demo-forløb" nederst):
--   1. Dashboard: Tagudskiftning Roskilde er i risiko, og klokken viser overbelastninger.
--   2. Kapacitet: Morten Jensen er overbooket (Gipsvægge i Hellerup + Nyt tag i Roskilde).
--      "Find ledige" på opgaven viser Mikkel Berg og Thomas Krag som ledige tømrere.
--   3. Hellerup > Gipsvægge og lofter: byt Morten ud med Mikkel. Konflikten forsvinder.
--   4. Roskilde > Lægning af nyt tag: tilføj Thomas. Projektet kan nu nås.
--
-- Hvad demoen viser:
--   Villarenovering Hellerup   Hovedprojekt med 8 faser og 32 opgaver over ca. 13 uger.
--                              Færdige, igangværende og kommende opgaver, faser der afløser
--                              hinanden, opgaveafhængigheder, minimumsvarighed (udtørring af
--                              beton, tørretid mellem lag, akklimatisering af parket og 10
--                              dages produktion af bordplade), en ubemandet opgave og to
--                              bemandingsadvarsler:
--                                - Henrik Olsen er inaktiv (Flisearbejde)
--                                - Mette Hansen mangler kompetencen Gulvlægger (Fodlister)
--   Køkkenrenovering Valby     I gang. Deadline fredag i demougen og bliver færdig på dagen.
--   Gulvrenovering Amager      I gang. Deadline om 3 uger.
--   Kælderrenovering Lyngby    Startede på demodagen. Deadline om 4,5 uger.
--   Tagudskiftning Roskilde    I risiko: bliver færdig efter deadline.
--                              Morten Jensen er dobbeltbooket med Gipsvægge i Hellerup.
--   Lejlighed Frederiksberg    Planlagt. Emma Kristensen er dobbeltbooket med Maling i Hellerup.
--                              Flisearbejdet er ikke bemandet og venter på vådrumssikringen.
--   Tilbygning Gentofte        Kan ikke beregnes: kun en inaktiv medarbejder er tildelt.
--   Kontorombygning Ørestad    Kladde uden bemanding.
--   Altanrenovering Nørrebro   Afsluttet.
-- =====================================================================================

SET demo.reset_all = 'true';
SET demo.monday = '2026-10-26';

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

-- Kompetencer (navnene matcher ikonerne i frontend).
-- Timeprisen er kundepris ekskl. moms. Findes kompetencen allerede, opdateres prisen,
-- så en database med gamle priser også får de nye. Opgavernes pris regnes ud fra den.
INSERT INTO competences (name_key, name, rate, active, created_at, updated_at)
VALUES
    ('tømrer',      'Tømrer',      575, true, now(), now()),
    ('maler',       'Maler',       525, true, now(), now()),
    ('vvs',         'VVS',         650, true, now(), now()),
    ('elektriker',  'Elektriker',  650, true, now(), now()),
    ('murer',       'Murer',       575, true, now(), now()),
    ('arbejdsmand', 'Arbejdsmand', 425, true, now(), now()),
    ('ventilation', 'Ventilation', 625, true, now(), now()),
    ('gulvlægger',  'Gulvlægger',  550, true, now(), now()),
    ('montør',      'Montør',      500, true, now(), now())
ON CONFLICT (name_key) DO UPDATE
    SET rate = EXCLUDED.rate,
        updated_at = now();

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
    -- Ledige tømrere: ikke tildelt noget, så de dukker op under "Ledig kapacitet" i demoen
    ('Mikkel',   'Berg',       0,   true,  true),
    ('Thomas',   'Krag',       0,   true,  true),
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
    ('Mikkel',   'Berg',       'tømrer'),
    ('Thomas',   'Krag',       'tømrer'),
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
-- Med SET demo.reset_all = 'true' slettes alle projekter, så testdata ikke ses i demoen.
CREATE TEMP TABLE demo_projects ON COMMIT DROP AS
    SELECT id FROM projects WHERE coalesce(current_setting('demo.reset_all', true), '') = 'true' OR title IN (
        -- Nuværende demoprojekter
        'Villarenovering Hellerup',
        'Tagudskiftning Roskilde',
        'Lejlighed Frederiksberg',
        'Tilbygning Gentofte',
        'Kontorombygning Ørestad',
        'Altanrenovering Nørrebro',
        'Køkkenrenovering Valby',
        'Gulvrenovering Amager',
        'Kælderrenovering Lyngby',
        -- Fra tidligere versioner af scriptet
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
-- p_start og p_deadline er antal dage fra mandag i indeværende uge (eller demo.monday).
-- I weekenden er det mandag i ugen efter (isodow 6 og 7 er lørdag og søndag).
CREATE OR REPLACE FUNCTION pg_temp.add_project(p_title text, p_status text, p_start integer, p_deadline integer, p_description text)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    v_monday  date := coalesce(nullif(current_setting('demo.monday', true), '')::date,
                               date_trunc('week', current_date)::date
                                   + CASE WHEN extract(isodow FROM current_date) >= 6 THEN 7 ELSE 0 END);
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
    p := pg_temp.add_project('Villarenovering Hellerup', 'IN_PROGRESS', -21, 81,
                             'Totalrenovering af 1930''er-villa på 160 m2: nyt køkken og bad, el, VVS, ventilation, gulve og overflader');

    -- Minimumsvarighed (sidste argument) er de arbejdsdage, en opgave mindst tager, uanset
    -- hvor mange der er på. Arbejde, der kan skaleres med folk, har en lav minimumsvarighed.
    -- Tørring, hærdning og leverancer har den tid, de reelt kræver.

    -- Nedrivning
    s1 := pg_temp.add_stage(p, 'Nedrivning og klargøring');
    t1 := pg_temp.add_task(s1, 'Byggepladsindretning og afdækning', 'Arbejdsmand', 12, 14, 22, 'DONE',
                           ARRAY['Frederik Dam'], '{}', 1);                                               -- 15 t
    PERFORM pg_temp.add_task(s1, 'Nedrivning af køkken og bad', 'Arbejdsmand', 36, 44, 58, 'DONE',
                             ARRAY['Frederik Dam', 'Mette Hansen'], ARRAY[t1], 2);                        -- 45 t
    PERFORM pg_temp.add_task(s1, 'Nedtagning af gamle gulve', 'Arbejdsmand', 12, 14, 22, 'DONE',
                             ARRAY['Jonas Petersen'], ARRAY[t1], 1);                                      -- 15 t

    -- Råhus
    s2 := pg_temp.add_stage(p, 'Råhus og konstruktion', ARRAY[s1]);
    PERFORM pg_temp.add_task(s2, 'Udskiftning af bjælkelag', 'Tømrer', 48, 58, 80, 'DONE',
                             ARRAY['Kasper Holm', 'Morten Jensen'], '{}', 2);                             -- 60 t
    -- Mures i lag, og mørtlen skal hærde, før væggen må belastes
    t1 := pg_temp.add_task(s2, 'Opmuring af ny skillevæg', 'Murer', 24, 29, 40, 'DONE',
                           ARRAY['Rasmus Bach'], '{}', 3);                                                -- 30 t, min. 3 dage
    PERFORM pg_temp.add_task(s2, 'Nye dør- og vindueshuller', 'Tømrer', 18, 21, 30, 'DONE',
                             ARRAY['Peter Madsen'], ARRAY[t1], 2);                                        -- 22 t
    -- Gulvafløbet skal ligge, før gulvet støbes
    t2 := pg_temp.add_task(s2, 'Gulvafløb i bad', 'VVS', 6, 8, 10, 'DONE',
                           ARRAY['Ali Hassan'], '{}', 1);                                                 -- 8 t
    PERFORM pg_temp.add_task(s2, 'Støbning af gulv i bad', 'Murer', 12, 14, 22, 'DONE',
                             ARRAY['Rasmus Bach'], ARRAY[t1, t2], 1);                                     -- 15 t

    -- Installationer (igangværende)
    s3 := pg_temp.add_stage(p, 'Installationer', ARRAY[s2]);
    PERFORM pg_temp.add_task(s3, 'Kabelføring og ny el-tavle', 'Elektriker', 96, 116, 160, 'IN_PROGRESS',
                             ARRAY['Sofie Larsen', 'Nanna Friis'], '{}', 2);                              -- 120 t
    PERFORM pg_temp.add_task(s3, 'Rørføring vand og afløb', 'VVS', 70, 88, 118, 'IN_PROGRESS',
                             ARRAY['Ali Hassan', 'Ida Lund'], '{}', 2);                                   -- 90 t
    PERFORM pg_temp.add_task(s3, 'Ventilationskanaler', 'Ventilation', 24, 29, 40, 'DONE',
                             ARRAY['Anders Skov'], '{}', 1);                                              -- 30 t
    -- Lille estimat, men gulvstøbningen skal tørre, før der må lægges vådrumssikring
    PERFORM pg_temp.add_task(s3, 'Udtørring af gulvstøbning', 'Murer', 3, 4, 5, 'IN_PROGRESS',
                             ARRAY['Rasmus Bach'], '{}', 8);                                              -- 4 t, min. 8 dage

    -- Vægge og lofter. Kasper og Morten tager indervægge og lofter, mens Peter isolerer
    -- og lukker ydervæggene. Spartling venter på begge.
    s4 := pg_temp.add_stage(p, 'Vægge og lofter', ARRAY[s3]);
    t1 := pg_temp.add_task(s4, 'Gipsvægge og lofter', 'Tømrer', 60, 73, 98, 'NOT_STARTED',
                           ARRAY['Kasper Holm', 'Morten Jensen'], '{}', 2);                               -- 75 t (Morten dobbeltbooket)
    t2 := pg_temp.add_task(s4, 'Isolering og beklædning af ydervægge', 'Tømrer', 24, 29, 40, 'NOT_STARTED',
                           ARRAY['Peter Madsen'], '{}', 1);                                               -- 30 t
    -- Tre lag spartel, og hvert lag skal tørre, før det næste kan slibes og lægges på
    PERFORM pg_temp.add_task(s4, 'Spartling og slibning', 'Maler', 36, 44, 58, 'NOT_STARTED',
                             ARRAY['Camilla Krogh'], ARRAY[t1, t2], 3);                                   -- 45 t, min. 3 dage

    -- Badeværelse (parallelt med vægge og lofter)
    s5 := pg_temp.add_stage(p, 'Badeværelse', ARRAY[s3]);
    -- Varmemåtten lægges i flydespartel på gulvet, før der vådrumssikres
    t1 := pg_temp.add_task(s5, 'Gulvvarme i bad', 'Elektriker', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Nanna Friis'], '{}', 1);                                                -- 15 t
    -- To lag membran med tørretid imellem
    t2 := pg_temp.add_task(s5, 'Vådrumssikring', 'Murer', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Rasmus Bach'], ARRAY[t1], 2);                                           -- 15 t, min. 2 dage
    -- Fliserne sættes, og der fuges tidligst dagen efter. Henrik Olsen er inaktiv:
    -- bemandingsadvarsel, men Rasmus kan stadig planlægges.
    t3 := pg_temp.add_task(s5, 'Flisearbejde gulv og væg', 'Murer', 42, 50, 70, 'NOT_STARTED',
                           ARRAY['Rasmus Bach', 'Henrik Olsen'], ARRAY[t2], 3);                           -- 52 t, min. 3 dage
    PERFORM pg_temp.add_task(s5, 'Montering af sanitet', 'VVS', 18, 21, 30, 'NOT_STARTED',
                             ARRAY['Ida Lund'], ARRAY[t3], 1);                                            -- 22 t

    -- Overflader
    s6 := pg_temp.add_stage(p, 'Overflader', ARRAY[s4, s5]);
    -- Grunding og to lag maling med tørretid imellem
    t1 := pg_temp.add_task(s6, 'Maling af vægge og lofter', 'Maler', 56, 70, 96, 'NOT_STARTED',
                           ARRAY['Emma Kristensen', 'Camilla Krogh'], '{}', 3);                           -- 72 t (Emma dobbeltbooket), min. 3 dage
    -- Parketten skal ligge i rummet og tilpasse sig temperatur og fugt, før den lægges
    t2 := pg_temp.add_task(s6, 'Akklimatisering af parket', 'Gulvlægger', 1, 1, 2, 'NOT_STARTED',
                           ARRAY['Louise Bang'], '{}', 3);                                                -- 1 t, min. 3 dage
    t3 := pg_temp.add_task(s6, 'Lægning af parket', 'Gulvlægger', 48, 58, 80, 'NOT_STARTED',
                           ARRAY['Lars Nielsen', 'Louise Bang'], ARRAY[t1, t2], 2);                       -- 60 t
    -- Olien skal hærde, før gulvet må betrædes og møbleres
    PERFORM pg_temp.add_task(s6, 'Oliebehandling af parket', 'Gulvlægger', 6, 8, 10, 'NOT_STARTED',
                             ARRAY['Lars Nielsen'], ARRAY[t3], 2);                                        -- 8 t, min. 2 dage

    -- Køkken
    s7 := pg_temp.add_stage(p, 'Køkken', ARRAY[s6]);
    t1 := pg_temp.add_task(s7, 'Montering af køkkenelementer', 'Montør', 36, 44, 58, 'NOT_STARTED',
                           ARRAY['Peter Madsen'], '{}', 2);                                               -- 45 t
    -- Bordpladen måles op på de monterede elementer og produceres hos leverandøren
    t2 := pg_temp.add_task(s7, 'Opmåling og produktion af stenbordplade', 'Montør', 2, 2, 3, 'NOT_STARTED',
                           ARRAY['Peter Madsen'], ARRAY[t1], 10);                                         -- 2 t, min. 10 dage
    t3 := pg_temp.add_task(s7, 'Montering af bordplade', 'Montør', 3, 4, 6, 'NOT_STARTED',
                           ARRAY['Peter Madsen'], ARRAY[t2], 1);                                          -- 4 t
    -- Kogepladen skæres ind i bordpladen. Ikke bemandet endnu: planlægges med 7,5 timer om dagen
    t1 := pg_temp.add_task(s7, 'Montering af hvidevarer', 'Montør', 12, 14, 22, 'NOT_STARTED',
                           '{}', ARRAY[t3], 1);                                                           -- 15 t
    PERFORM pg_temp.add_task(s7, 'El-tilslutning i køkken', 'Elektriker', 6, 8, 10, 'NOT_STARTED',
                             ARRAY['Sofie Larsen'], ARRAY[t1], 1);                                        -- 8 t
    PERFORM pg_temp.add_task(s7, 'VVS-tilslutning i køkken', 'VVS', 9, 12, 15, 'NOT_STARTED',
                             ARRAY['Ali Hassan'], ARRAY[t1], 1);                                          -- 12 t

    -- Aflevering
    s8 := pg_temp.add_stage(p, 'Aflevering', ARRAY[s7]);
    -- Mette Hansen er ikke gulvlægger: bemandingsadvarsel
    t1 := pg_temp.add_task(s8, 'Fodlister og finish', 'Gulvlægger', 9, 12, 15, 'NOT_STARTED',
                           ARRAY['Louise Bang', 'Mette Hansen'], '{}', 1);                                -- 12 t
    t2 := pg_temp.add_task(s8, 'Slutrengøring', 'Arbejdsmand', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Frederik Dam', 'Mette Hansen'], ARRAY[t1], 1);                          -- 15 t
    PERFORM pg_temp.add_task(s8, 'Mangelgennemgang og aflevering', 'Tømrer', 3, 4, 5, 'NOT_STARTED',
                             ARRAY['Morten Jensen'], ARRAY[t2], 1);                                       -- 4 t
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
    t2 bigint;
BEGIN
    -- Kørt før demodagen med deadlines om 1, 3 og 4,5 uger, så "Kommende deadlines" ligner
    -- en rigtig hverdag. Bemandet med folk, der er ledige i perioden, så de ikke giver konflikter.

    -- Deadline fredag i demougen. Køkkenelementerne monteres, og kun bordpladen mangler.
    p  := pg_temp.add_project('Køkkenrenovering Valby', 'IN_PROGRESS', -12, 4,
                              'Nyt køkken med laminatbordplade i rækkehus');
    s1 := pg_temp.add_stage(p, 'Nedrivning og installationer');
    s2 := pg_temp.add_stage(p, 'Maling og montering', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Nedrivning af gammelt køkken', 'Arbejdsmand', 12, 14, 22, 'DONE',
                           ARRAY['Mette Hansen'], '{}', 1);                                               -- 15 t
    PERFORM pg_temp.add_task(s1, 'Flytning af vand og afløb', 'VVS', 12, 14, 22, 'DONE',
                             ARRAY['Ida Lund'], ARRAY[t1], 1);                                            -- 15 t
    PERFORM pg_temp.add_task(s1, 'Nye stikkontakter og emhætte-udtag', 'Elektriker', 6, 8, 10, 'DONE',
                             ARRAY['Sofie Larsen'], ARRAY[t1], 1);                                        -- 8 t
    -- Grunding og to lag maling med tørretid imellem
    t1 := pg_temp.add_task(s2, 'Maling af køkken', 'Maler', 14, 16, 22, 'DONE',
                           ARRAY['Camilla Krogh'], '{}', 2);                                              -- 17 t, min. 2 dage
    t1 := pg_temp.add_task(s2, 'Montering af køkkenelementer', 'Montør', 24, 29, 40, 'IN_PROGRESS',
                           ARRAY['Peter Madsen'], ARRAY[t1], 2);                                          -- 30 t
    -- Laminatbordpladen er på lager, så der er ingen produktionstid som i Hellerup
    PERFORM pg_temp.add_task(s2, 'Montering af bordplade og hvidevarer', 'Montør', 10, 12, 16, 'NOT_STARTED',
                             ARRAY['Anders Skov'], ARRAY[t1], 1);                                         -- 12 t

    -- Deadline om 3 uger. Det gamle gulv er fjernet, og undergulvet slibes.
    p  := pg_temp.add_project('Gulvrenovering Amager', 'IN_PROGRESS', -5, 18,
                              'Nyt plankegulv i stue og to værelser i lejlighed');
    s1 := pg_temp.add_stage(p, 'Gulve');
    t1 := pg_temp.add_task(s1, 'Nedtagning af gamle gulve', 'Arbejdsmand', 12, 14, 22, 'DONE',
                           ARRAY['Mette Hansen'], '{}', 1);                                               -- 15 t
    -- Plankerne skal ligge i rummet og tilpasse sig temperatur og fugt, før de lægges
    t2 := pg_temp.add_task(s1, 'Akklimatisering af plankegulv', 'Gulvlægger', 1, 1, 2, 'DONE',
                           ARRAY['Emma Kristensen'], '{}', 3);                                            -- 1 t, min. 3 dage
    t1 := pg_temp.add_task(s1, 'Afslibning og udbedring af undergulv', 'Gulvlægger', 18, 21, 30, 'IN_PROGRESS',
                           ARRAY['Louise Bang'], ARRAY[t1], 2);                                           -- 22 t
    t1 := pg_temp.add_task(s1, 'Lægning af plankegulv', 'Gulvlægger', 32, 39, 54, 'NOT_STARTED',
                           ARRAY['Louise Bang', 'Emma Kristensen'], ARRAY[t1, t2], 2);                    -- 40 t
    -- Olien skal hærde, før gulvet må betrædes og møbleres
    t1 := pg_temp.add_task(s1, 'Oliebehandling', 'Gulvlægger', 6, 8, 10, 'NOT_STARTED',
                           ARRAY['Emma Kristensen'], ARRAY[t1], 2);                                       -- 8 t, min. 2 dage
    PERFORM pg_temp.add_task(s1, 'Fodlister', 'Gulvlægger', 9, 12, 15, 'NOT_STARTED',
                             ARRAY['Louise Bang'], ARRAY[t1], 1);                                         -- 12 t

    -- Deadline om 4,5 uger. Startede på demodagen med fugtsikring af kælderen.
    p  := pg_temp.add_project('Kælderrenovering Lyngby', 'IN_PROGRESS', 1, 32,
                              'Fugtsikring og indretning af kælder til hobbyrum');
    s1 := pg_temp.add_stage(p, 'Fugtsikring');
    s2 := pg_temp.add_stage(p, 'Indretning', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Afrensning og fugtsikring af kældervægge', 'Murer', 24, 29, 40, 'IN_PROGRESS',
                           ARRAY['Jonas Petersen'], '{}', 3);                                             -- 30 t
    -- Væggene skal tørre ud, før de må lukkes med isolering og gips
    PERFORM pg_temp.add_task(s1, 'Udtørring af kældervægge', 'Murer', 1, 1, 2, 'NOT_STARTED',
                             ARRAY['Jonas Petersen'], ARRAY[t1], 10);                                     -- 1 t, min. 10 dage
    t1 := pg_temp.add_task(s2, 'Isolering og gipsvægge', 'Tømrer', 24, 29, 40, 'NOT_STARTED',
                           ARRAY['Kasper Holm'], '{}', 2);                                                -- 30 t
    PERFORM pg_temp.add_task(s2, 'El-installation', 'Elektriker', 12, 14, 22, 'NOT_STARTED',
                             ARRAY['Sofie Larsen'], '{}', 1);                                             -- 15 t
    -- Grunding og to lag maling med tørretid imellem
    PERFORM pg_temp.add_task(s2, 'Maling af kælder', 'Maler', 12, 14, 22, 'NOT_STARTED',
                             ARRAY['Mette Hansen'], ARRAY[t1], 2);                                        -- 15 t, min. 2 dage

    -- I risiko. Tagarbejdet overlapper Gipsvægge i Hellerup, så Morten Jensen er overbooket.
    p  := pg_temp.add_project('Tagudskiftning Roskilde', 'IN_PROGRESS', -7, 11,
                              'Udskiftning af tegltag og tagrender på parcelhus');
    s1 := pg_temp.add_stage(p, 'Stillads og nedtagning');
    s2 := pg_temp.add_stage(p, 'Nyt tag', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Opsætning af stillads', 'Montør', 12, 14, 22, 'DONE',
                           ARRAY['Peter Madsen'], '{}', 1);                                               -- 15 t
    PERFORM pg_temp.add_task(s1, 'Nedtagning af gammelt tag', 'Arbejdsmand', 36, 44, 58, 'DONE',
                             ARRAY['Jonas Petersen', 'Frederik Dam'], ARRAY[t1], 2);                      -- 45 t
    -- Kan gå hurtigere med flere tømrere: den opgave, demoen tilføjer Thomas Krag til
    t1 := pg_temp.add_task(s2, 'Lægning af nyt tag', 'Tømrer', 90, 110, 142, 'IN_PROGRESS',
                           ARRAY['Morten Jensen', 'Lars Nielsen'], '{}', 3);                              -- 112 t
    PERFORM pg_temp.add_task(s2, 'Inddækning og tagrender', 'Tømrer', 18, 21, 30, 'NOT_STARTED',
                             ARRAY['Morten Jensen'], ARRAY[t1], 2);                                       -- 22 t

    -- Planlagt og kan nås. Maling overlapper Maling i Hellerup, så Emma Kristensen er overbooket.
    p  := pg_temp.add_project('Lejlighed Frederiksberg', 'PLANNED', 24, 53,
                              'Nyt badeværelse og maling af stue og gang i ejerlejlighed');
    s1 := pg_temp.add_stage(p, 'Badeværelse');
    s2 := pg_temp.add_stage(p, 'Maling');
    t1 := pg_temp.add_task(s1, 'Nedrivning af fliser', 'Arbejdsmand', 12, 14, 22, 'NOT_STARTED',
                           ARRAY['Frederik Dam'], '{}', 1);                                               -- 15 t
    t1 := pg_temp.add_task(s1, 'Rørføring', 'VVS', 24, 29, 40, 'NOT_STARTED',
                           ARRAY['Ali Hassan'], ARRAY[t1], 2);                                            -- 30 t
    -- To lag membran med tørretid imellem
    t1 := pg_temp.add_task(s1, 'Vådrumssikring', 'Murer', 10, 12, 16, 'NOT_STARTED',
                           ARRAY['Jonas Petersen'], ARRAY[t1], 2);                                        -- 12 t, min. 2 dage
    -- Fliserne sættes, og der fuges tidligst dagen efter
    t1 := pg_temp.add_task(s1, 'Flisearbejde', 'Murer', 30, 37, 44, 'NOT_STARTED',
                           '{}', ARRAY[t1], 3);                                                           -- 37 t, ikke bemandet, min. 3 dage
    PERFORM pg_temp.add_task(s1, 'Montering af sanitet', 'VVS', 12, 14, 22, 'NOT_STARTED',
                             ARRAY['Ali Hassan'], ARRAY[t1], 1);                                          -- 15 t
    -- Grunding og to lag maling med tørretid imellem
    PERFORM pg_temp.add_task(s2, 'Maling af stue og gang', 'Maler', 22, 28, 34, 'NOT_STARTED',
                             ARRAY['Emma Kristensen'], '{}', 2);                                          -- 28 t, min. 2 dage

    -- Kan ikke beregnes: fundamentet er kun tildelt Henrik Olsen, som er inaktiv.
    p  := pg_temp.add_project('Tilbygning Gentofte', 'PLANNED', 70, 102,
                              'Tilbygning på 25 m2 til eksisterende villa');
    s1 := pg_temp.add_stage(p, 'Fundament');
    s2 := pg_temp.add_stage(p, 'Råhus', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Støbning af fundament', 'Murer', 48, 58, 80, 'NOT_STARTED',
                           ARRAY['Henrik Olsen'], '{}', 2);                                               -- 60 t
    -- Betonen skal hærde, før der må bygges ovenpå
    PERFORM pg_temp.add_task(s1, 'Hærdning af fundament', 'Murer', 1, 1, 2, 'NOT_STARTED',
                             '{}', ARRAY[t1], 7);                                                         -- 1 t, min. 7 dage
    PERFORM pg_temp.add_task(s2, 'Opbygning af vægge og tag', 'Tømrer', 60, 73, 98, 'NOT_STARTED',
                             ARRAY['Kasper Holm'], '{}', 5);                                              -- 75 t

    -- Kladde uden bemanding: varigheden regnes med 7,5 timer om dagen.
    p  := pg_temp.add_project('Kontorombygning Ørestad', 'DRAFT', 77, 140,
                              'Ombygning af åbent kontor til mødelokaler');
    s1 := pg_temp.add_stage(p, 'Skillevægge og ventilation');
    s2 := pg_temp.add_stage(p, 'Overflader', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Opsætning af skillevægge', 'Tømrer', 48, 58, 80,
                             'NOT_STARTED', '{}', '{}', 2);                                               -- 60 t
    PERFORM pg_temp.add_task(s1, 'Nye ventilationskanaler', 'Ventilation', 24, 29, 40,
                             'NOT_STARTED', '{}', '{}', 1);                                               -- 30 t
    -- Grunding og to lag maling med tørretid imellem
    t1 := pg_temp.add_task(s2, 'Maling af kontorer', 'Maler', 36, 44, 58,
                           'NOT_STARTED', '{}', '{}', 3);                                                 -- 45 t, min. 3 dage
    -- Tæppet lægges efter maling, så det ikke bliver tilsmudset
    PERFORM pg_temp.add_task(s2, 'Nyt tæppegulv', 'Gulvlægger', 24, 29, 40,
                             'NOT_STARTED', '{}', ARRAY[t1], 1);                                          -- 30 t

    -- Afsluttet.
    p  := pg_temp.add_project('Altanrenovering Nørrebro', 'COMPLETED', -77, -53,
                              'Renovering af altan i etageejendom');
    s1 := pg_temp.add_stage(p, 'Altan');
    -- Reparationsmørtlen skal hærde, før rækværket må monteres
    t1 := pg_temp.add_task(s1, 'Reparation af altanplade', 'Murer', 60, 73, 98, 'DONE',
                           ARRAY['Jonas Petersen'], '{}', 3);                                             -- 75 t, min. 3 dage
    PERFORM pg_temp.add_task(s1, 'Montering af nyt rækværk', 'Montør', 12, 14, 22, 'DONE',
                             ARRAY['Peter Madsen'], ARRAY[t1], 1);                                        -- 15 t
END $$;

COMMIT;

-- -------------------------------------------------------------------------------------
-- 7. Kontrol: viser alle projekter. Til demoen skal alle 9 rækker have demoprojekt = true,
--    og Villarenovering Hellerup skal starte 3 uger før demougens mandag.
-- -------------------------------------------------------------------------------------
SELECT p.id, p.title, p.status, p.startdate, p.deadline,
       p.title IN ('Villarenovering Hellerup', 'Tagudskiftning Roskilde', 'Lejlighed Frederiksberg',
                   'Tilbygning Gentofte', 'Kontorombygning Ørestad', 'Altanrenovering Nørrebro',
                   'Køkkenrenovering Valby', 'Gulvrenovering Amager', 'Kælderrenovering Lyngby') AS demoprojekt,
       count(t.id)                                                                     AS opgaver,
       count(t.id) FILTER (WHERE t.status = 'DONE')                                    AS faerdige,
       coalesce(sum(round((t.bestestimate + 4 * t.likelyestimate + t.worstestimate) / 6)), 0) AS timer
FROM projects p
LEFT JOIN stage s ON s.project_id = p.id
LEFT JOIN task t ON t.stage_id = s.id
GROUP BY p.id
ORDER BY demoprojekt, p.startdate, p.id;

-- =====================================================================================
-- Demo-forløb (afprøvet mod API'et; M = mandag i den uge, scriptet køres)
--
-- Før:
--   Dashboard      9 projekter, 1 i risiko. Kommende deadlines: Valby M+4, Roskilde M+11,
--                  Amager M+18, Lyngby M+32 og Frederiksberg M+53.
--                  Tagudskiftning Roskilde i risiko: deadline M+11, forventet slut M+15.
--                  Villarenovering Hellerup kan nås: deadline M+81, forventet slut M+77.
--                  Planen springer kun weekender over, så jul og nytår regnes som arbejdsdage.
--   Klokken        7 overbelastninger de næste 4 uger (Morten Jensen 5, Emma Kristensen 2).
--   Kapacitet      Morten har 14,5 t mod 7,5 t fra M+7: Gipsvægge og lofter + Lægning af nyt tag.
--                  Søgeknappen på opgaven viser Mikkel Berg og Thomas Krag med 7,5 t ledig.
--
-- 1. Villarenovering Hellerup > Gipsvægge og lofter: fjern Morten Jensen, tilføj Mikkel Berg.
--    Kapacitet viser nu kun Emmas 2 konflikter. Hellerup kan stadig nås.
-- 2. Tagudskiftning Roskilde > Lægning af nyt tag: tilføj Thomas Krag.
--    Forventet slut rykker til M+10, før deadline. Projektet kan nås, og "I risiko" er tom.
--
-- Kør scriptet igen bagefter for at nulstille demoen. Mikkel og Thomas er så ledige igen.
-- =====================================================================================
