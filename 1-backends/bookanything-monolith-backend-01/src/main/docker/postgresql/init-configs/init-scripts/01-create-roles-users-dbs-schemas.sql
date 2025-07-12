SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Creating the Roles, Users and Databases for the specific application

CREATE USER dbabookanythingapps01 WITH PASSWORD '1a88a1' superuser;
\connect -reuse-previous=on "dbname='DBBookAnythingPlatform'"
CREATE SCHEMA "schm01";
ALTER SCHEMA "schm01" OWNER TO dbabookanythingapps01;
CREATE SCHEMA "schm02";
ALTER SCHEMA "schm02" OWNER TO dbabookanythingapps01;
CREATE SCHEMA "schm03";
ALTER SCHEMA "schm03" OWNER TO dbabookanythingapps01;
CREATE SCHEMA "schm04";
ALTER SCHEMA "schm04" OWNER TO dbabookanythingapps01;
CREATE SCHEMA "schm05";
ALTER SCHEMA "schm05" OWNER TO dbabookanythingapps01;


-- Creating the Roles, Users and Databases for the Keycloak instances

CREATE USER dbakeycloak01 WITH PASSWORD '1a88a1' superuser;
DROP DATABASE IF EXISTS "KeycloakDB_M0";
CREATE DATABASE "KeycloakDB_M0" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8';
ALTER DATABASE "KeycloakDB_M0" OWNER TO "dbakeycloak01";


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

