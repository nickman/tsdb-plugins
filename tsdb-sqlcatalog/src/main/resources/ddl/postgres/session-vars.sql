
CREATE OR REPLACE FUNCTION ensure_session_table_exists()
  RETURNS VARCHAR AS
$BODY$DECLARE
	tmp VARCHAR(100);
BEGIN
	SELECT relname into tmp 
	FROM pg_catalog.pg_class
	WHERE relname = 'session_variables' AND
	relnamespace = pg_catalog.pg_my_temp_schema();
	IF NOT FOUND THEN
		CREATE TEMPORARY TABLE session_variables (
	     	key TEXT PRIMARY KEY,
	        value TEXT
		);
		RETURN 'CREATED';
	END IF;
	RETURN 'EXISTS';
END;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION ensure_session_table_exists() SET application_name='tsdb';

ALTER FUNCTION ensure_session_table_exists()
  OWNER TO tsdb;


CREATE OR REPLACE FUNCTION set_env_var(xkey text, xvalue text)
  RETURNS text AS
$BODY$DECLARE
	tmp TEXT;
BEGIN
	PERFORM ensure_session_table_exists();
	
	SELECT value into tmp 
	FROM session_variables
	WHERE key = xkey;
	
	IF NOT FOUND THEN
		tmp := NULL;
	END IF;
	
	DELETE FROM session_variables WHERE key = xkey;
	INSERT INTO session_variables VALUES (xkey, xvalue);

	RETURN tmp;
END;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION set_env_var(text, text) SET application_name='tsdb';

ALTER FUNCTION set_env_var(text, text)
  OWNER TO tsdb;
  
CREATE OR REPLACE FUNCTION get_env_var(xkey text)
  RETURNS text AS
$BODY$DECLARE
	tmp TEXT;
BEGIN
	PERFORM ensure_session_table_exists();
	
	SELECT value into tmp 
	FROM session_variables
	WHERE key = xkey;
	
	IF NOT FOUND THEN
		tmp := NULL;
	END IF;

	RETURN tmp;
END;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION get_env_var(text) SET application_name='tsdb';

ALTER FUNCTION get_env_var(text)
  OWNER TO tsdb;
  
  