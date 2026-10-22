-- =====================================================================================
-- Testdata til dashboardet: projekter, faser, opgaver og bemanding
--
-- Bygger på de kompetencer og medarbejdere, der allerede findes i databasen.
-- Alle testprojekter har en beskrivelse, der starter med "[Testdata]". Scriptet sletter
-- dem først, så det kan køres igen uden dubletter. Andre projekter røres ikke.
--
-- Kør hele scriptet i pgAdmin (Query Tool, F5) mod databasen "scrumAPI".
--
-- Forventet på dashboardet (beregnet ud fra planlægningsreglerne):
--   Projekt                     Status       Deadline    Forventet slut  Kan nås
--   Køkkenrenovering Valby      IN_PROGRESS  2026-10-30  2026-10-23      ja
--   Tagudskiftning Roskilde     IN_PROGRESS  2026-10-30  2026-11-03      nej   (i risiko)
--   Gulvrenovering Amager       IN_PROGRESS  2026-11-06  2026-10-23      ja
--   Badeværelse Frederiksberg   PLANNED      2026-11-13  2026-11-18      nej   (i risiko)
--   Tilbygning Hellerup         PLANNED      2026-11-20  -               kan ikke beregnes (inaktiv medarbejder)
--   Elinstallation Lyngby       PLANNED      2026-12-18  2026-12-01      ja
--   Kontorombygning Ørestad     DRAFT        2027-01-29  2026-12-18      ja    (ikke bemandet endnu)
--   Altanrenovering Nørrebro    COMPLETED    2026-08-14  2026-08-25      nej   (afsluttet, vises ikke)
-- =====================================================================================

BEGIN;

-- Projekter skal have en opretter. Scriptet bruger den første projektleder.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE user_role = 'PROJECT_MANAGER') THEN
        RAISE EXCEPTION 'Der findes ingen bruger med rollen PROJECT_MANAGER. Opret en først.';
    END IF;
END $$;

-- -------------------------------------------------------------------------------------
-- 1. Slet tidligere testdata
-- -------------------------------------------------------------------------------------
CREATE TEMP TABLE testdata_projects ON COMMIT DROP AS
    SELECT id FROM projects WHERE description LIKE '[Testdata]%';

CREATE TEMP TABLE testdata_stages ON COMMIT DROP AS
    SELECT id FROM stage WHERE project_id IN (SELECT id FROM testdata_projects);

CREATE TEMP TABLE testdata_tasks ON COMMIT DROP AS
    SELECT id FROM task WHERE stage_id IN (SELECT id FROM testdata_stages);

DELETE FROM task_employee WHERE task_id IN (SELECT id FROM testdata_tasks);
DELETE FROM task_predecessors
    WHERE task_id IN (SELECT id FROM testdata_tasks) OR predecessor_id IN (SELECT id FROM testdata_tasks);
DELETE FROM task WHERE id IN (SELECT id FROM testdata_tasks);
DELETE FROM stage_predecessors
    WHERE stage_id IN (SELECT id FROM testdata_stages) OR predecessor_id IN (SELECT id FROM testdata_stages);
DELETE FROM stage WHERE id IN (SELECT id FROM testdata_stages);
DELETE FROM projects WHERE id IN (SELECT id FROM testdata_projects);

-- -------------------------------------------------------------------------------------
-- 2. Hjælpefunktioner (pg_temp: de forsvinder igen, når forbindelsen lukkes)
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pg_temp.add_project(p_title text, p_status text, p_start date, p_deadline date, p_description text)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    v_manager bigint;
    v_id      bigint;
BEGIN
    SELECT id INTO v_manager FROM users WHERE user_role = 'PROJECT_MANAGER' ORDER BY id LIMIT 1;

    INSERT INTO projects (title, description, status, startdate, deadline, created_by, updated_by, createdat, updatedat)
    VALUES (p_title, '[Testdata] ' || p_description, p_status, p_start, p_deadline, v_manager, v_manager, now(), now())
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

-- p_employees er fulde navne ("Fornavn Efternavn") på eksisterende medarbejdere.
CREATE OR REPLACE FUNCTION pg_temp.add_task(p_stage_id bigint, p_name text, p_competence text, p_estimate double precision,
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

    INSERT INTO task (stage_id, name, competence_competence_id, estimate, minimumdurationindays, status, createdat, updatedat)
    VALUES (p_stage_id, p_name, v_competence, p_estimate, p_minimum_days, p_status, now(), now())
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
-- 3. Projekter
-- -------------------------------------------------------------------------------------
DO $$
DECLARE
    p  bigint;
    s1 bigint;
    s2 bigint;
    s3 bigint;
    t1 bigint;
BEGIN
    -- Kan nås. Opgaver i alle tre statusser.
    p  := pg_temp.add_project('Køkkenrenovering Valby', 'IN_PROGRESS', '2026-10-05', '2026-10-30',
                              'Nyt køkken med flytning af vand og el');
    s1 := pg_temp.add_stage(p, 'Nedrivning');
    s2 := pg_temp.add_stage(p, 'Installationer', ARRAY[s1]);
    s3 := pg_temp.add_stage(p, 'Montering', ARRAY[s2]);
    PERFORM pg_temp.add_task(s1, 'Nedrivning af gammelt køkken', 'Arbejdsmand', 30, 'DONE', ARRAY['Jonas Petersen', 'Mette Hansen']);
    PERFORM pg_temp.add_task(s2, 'VVS-arbejde', 'VVS', 22.5, 'IN_PROGRESS', ARRAY['Ali Hassan']);
    PERFORM pg_temp.add_task(s2, 'El-installation', 'Elektriker', 15, 'DONE', ARRAY['Sofie Larsen']);
    t1 := pg_temp.add_task(s3, 'Montering af elementer', 'Montør', 37.5, 'NOT_STARTED', ARRAY['Peter Madsen']);
    PERFORM pg_temp.add_task(s3, 'Maling', 'Maler', 21, 'NOT_STARTED', ARRAY['Emma Kristensen'], ARRAY[t1]);

    -- I risiko: tagarbejdet bliver først færdigt efter deadline.
    p  := pg_temp.add_project('Tagudskiftning Roskilde', 'IN_PROGRESS', '2026-10-12', '2026-10-30',
                              'Udskiftning af tegltag på parcelhus');
    s1 := pg_temp.add_stage(p, 'Stillads og nedtagning');
    s2 := pg_temp.add_stage(p, 'Nyt tag', ARRAY[s1]);
    t1 := pg_temp.add_task(s1, 'Opsætning af stillads', 'Montør', 15, 'DONE', ARRAY['Peter Madsen']);
    PERFORM pg_temp.add_task(s1, 'Nedtagning af gammelt tag', 'Arbejdsmand', 45, 'IN_PROGRESS',
                             ARRAY['Jonas Petersen', 'Mette Hansen'], ARRAY[t1]);
    t1 := pg_temp.add_task(s2, 'Lægning af nyt tag', 'Tømrer', 112.5, 'NOT_STARTED', ARRAY['Morten Jensen', 'Lars Nielsen']);
    PERFORM pg_temp.add_task(s2, 'Inddækning', 'Tømrer', 22.5, 'NOT_STARTED', ARRAY['Morten Jensen'], ARRAY[t1]);

    -- Kan nås. To medarbejdere deler parketarbejdet.
    p  := pg_temp.add_project('Gulvrenovering Amager', 'IN_PROGRESS', '2026-10-14', '2026-11-06',
                              'Nyt parketgulv i stue og værelser');
    s1 := pg_temp.add_stage(p, 'Klargøring');
    s2 := pg_temp.add_stage(p, 'Gulv', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Rydning og afdækning', 'Arbejdsmand', 15, 'DONE', ARRAY['Mette Hansen']);
    t1 := pg_temp.add_task(s2, 'Lægning af parket', 'Gulvlægger', 52.5, 'IN_PROGRESS', ARRAY['Lars Nielsen', 'Emma Kristensen']);
    PERFORM pg_temp.add_task(s2, 'Fodlister', 'Gulvlægger', 7.5, 'NOT_STARTED', ARRAY['Emma Kristensen'], ARRAY[t1]);

    -- I risiko: flisearbejdet er ikke bemandet og regnes med 7,5 timer om dagen.
    p  := pg_temp.add_project('Badeværelse Frederiksberg', 'PLANNED', '2026-11-02', '2026-11-13',
                              'Totalrenovering af badeværelse med gulvvarme');
    s1 := pg_temp.add_stage(p, 'Forberedelse');
    s2 := pg_temp.add_stage(p, 'Installation', ARRAY[s1]);
    s3 := pg_temp.add_stage(p, 'Finish', ARRAY[s2]);
    PERFORM pg_temp.add_task(s1, 'Nedrivning af fliser', 'Arbejdsmand', 15, 'NOT_STARTED', ARRAY['Jonas Petersen']);
    PERFORM pg_temp.add_task(s2, 'Rørføring', 'VVS', 30, 'NOT_STARTED', ARRAY['Ali Hassan']);
    PERFORM pg_temp.add_task(s2, 'Gulvvarme', 'Elektriker', 15, 'NOT_STARTED', ARRAY['Sofie Larsen']);
    PERFORM pg_temp.add_task(s3, 'Flisearbejde', 'Murer', 37.5);

    -- Kan ikke beregnes: fundamentet er kun tildelt Henrik Olsen, som er inaktiv.
    p  := pg_temp.add_project('Tilbygning Hellerup', 'PLANNED', '2026-11-16', '2026-11-20',
                              'Tilbygning på 25 m2 til eksisterende villa');
    s1 := pg_temp.add_stage(p, 'Fundament');
    s2 := pg_temp.add_stage(p, 'Råhus', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Støbning af fundament', 'Murer', 60, 'NOT_STARTED', ARRAY['Henrik Olsen']);
    PERFORM pg_temp.add_task(s2, 'Opbygning af vægge og tag', 'Tømrer', 75, 'NOT_STARTED', ARRAY['Lars Nielsen']);

    -- Kan nås.
    p  := pg_temp.add_project('Elinstallation Lyngby', 'PLANNED', '2026-11-23', '2026-12-18',
                              'Ny el-tavle og ventilation i rækkehus');
    s1 := pg_temp.add_stage(p, 'Installationer');
    PERFORM pg_temp.add_task(s1, 'Ny el-tavle og kabelføring', 'Elektriker', 52.5, 'NOT_STARTED', ARRAY['Sofie Larsen']);
    PERFORM pg_temp.add_task(s1, 'Ventilationsanlæg', 'Ventilation', 24, 'NOT_STARTED', ARRAY['Ali Hassan']);

    -- Kladde uden bemanding: varigheden regnes med 7,5 timer om dagen.
    p  := pg_temp.add_project('Kontorombygning Ørestad', 'DRAFT', '2026-12-01', '2027-01-29',
                              'Ombygning af åbent kontor til mødelokaler');
    s1 := pg_temp.add_stage(p, 'Skillevægge og ventilation');
    s2 := pg_temp.add_stage(p, 'Overflader', ARRAY[s1]);
    PERFORM pg_temp.add_task(s1, 'Opsætning af skillevægge', 'Tømrer', 60);
    PERFORM pg_temp.add_task(s1, 'Nye ventilationskanaler', 'Ventilation', 30);
    PERFORM pg_temp.add_task(s2, 'Maling af kontorer', 'Maler', 45);
    PERFORM pg_temp.add_task(s2, 'Nyt tæppegulv', 'Gulvlægger', 30);

    -- Afsluttet og for sent færdig: må hverken stå i "i risiko" eller "kommende deadlines".
    p  := pg_temp.add_project('Altanrenovering Nørrebro', 'COMPLETED', '2026-08-03', '2026-08-14',
                              'Renovering af altan i etageejendom');
    s1 := pg_temp.add_stage(p, 'Altan');
    t1 := pg_temp.add_task(s1, 'Reparation af altanplade', 'Murer', 75, 'DONE', ARRAY['Jonas Petersen']);
    PERFORM pg_temp.add_task(s1, 'Montering af nyt rækværk', 'Montør', 15, 'DONE', ARRAY['Peter Madsen'], ARRAY[t1]);
END $$;

COMMIT;

-- -------------------------------------------------------------------------------------
-- 4. Kontrol: viser de oprettede testprojekter
-- -------------------------------------------------------------------------------------
SELECT p.id, p.title, p.status, p.startdate, p.deadline,
       count(t.id)                                AS opgaver,
       count(t.id) FILTER (WHERE t.status = 'DONE') AS faerdige,
       coalesce(sum(t.estimate), 0)               AS timer
FROM projects p
LEFT JOIN stage s ON s.project_id = p.id
LEFT JOIN task t ON t.stage_id = s.id
WHERE p.description LIKE '[Testdata]%'
GROUP BY p.id
ORDER BY p.deadline, p.id;
