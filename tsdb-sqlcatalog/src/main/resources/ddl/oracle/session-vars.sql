-- ====================================================================
-- tsdb-sqlcatalog Session Property API for Oracle
-- Whitehead, 2013
-- ====================================================================

-- =================================================================
-- Per Connection ENV_VAR Support
-- =================================================================

create or replace 
package tsdb_support as 
  --======================================================================================
  --  PACKAGE VAR DEFS
  --======================================================================================
  TYPE map_varchar IS TABLE OF VARCHAR2(30) INDEX BY VARCHAR2(30);  
  TYPE VARSOUT IS RECORD (
    key VARCHAR2(30),
    value VARCHAR2(30));
  TYPE VARSOUTS IS TABLE OF VARSOUT;
    
  TYPE map_type IS REF CURSOR;
  --======================================================================================
  --  SET ENV VAR
  --======================================================================================
  FUNCTION SET_ENV_VAR(
    KEY   VARCHAR2,
    VALUE VARCHAR2
  ) RETURN VARCHAR2;
  --======================================================================================
  --  GET ENV VAR
  --======================================================================================
  FUNCTION GET_ENV_VAR(
    KEY   VARCHAR2
  ) RETURN VARCHAR2;
  --======================================================================================
  --  GET VARS
  --======================================================================================
  FUNCTION GET_VARS RETURN VARSOUTS PIPELINED; -- TO READ THIS, USE 'select * from TABLE(TSDB_SUPPORT.GET_VARS)'
  --======================================================================================
  --  IS SQPROCESSOR
  --======================================================================================
  FUNCTION IS_SQPROCESSOR RETURN BOOLEAN;
  --======================================================================================
  --  IS EQPROCESSOR
  --======================================================================================
  FUNCTION IS_EQPROCESSOR RETURN BOOLEAN;
  --======================================================================================
end tsdb_support;
/

create or replace
package body tsdb_support as
  ENV_VARS map_varchar;
-- ==================================================================
  FUNCTION SET_ENV_VAR(
    KEY   VARCHAR2,
    VALUE VARCHAR2
  ) RETURN VARCHAR2 IS
    CURRENTV VARCHAR2(30) := NULL;
    BEGIN
      IF ENV_VARS.EXISTS(KEY) THEN
        CURRENTV := ENV_VARS(KEY);
      END IF;      
      ENV_VARS(KEY) := VALUE;
    RETURN CURRENTV;
  END SET_ENV_VAR;
-- ==================================================================  
  FUNCTION GET_ENV_VAR(
    KEY   VARCHAR2
  ) RETURN VARCHAR2 IS
    BEGIN    
    RETURN ENV_VARS(KEY);
  END GET_ENV_VAR;
  --======================================================================================
  --  GET VARS
  --======================================================================================
  FUNCTION GET_VARS RETURN VARSOUTS PIPELINED IS  -- TO READ THIS, USE 'select * from TABLE(TSDB_SUPPORT.GET_VARS)'
    out_rec VARSOUT;
    ikey varchar2(30) := ENV_VARS.FIRST;
  BEGIN
    WHILE ikey IS NOT NULL LOOP
      out_rec.key := ikey;
      out_rec.value := ENV_VARS(ikey);
      PIPE ROW(out_rec);
      ikey := ENV_VARS.NEXT(ikey);
    END LOOP;
    RETURN;
  END GET_VARS;
  --======================================================================================
  --  IS SQPROCESSOR
  --======================================================================================
  FUNCTION IS_SQPROCESSOR RETURN BOOLEAN IS
  BEGIN
	IF get_env_var('TSDCONNTYPE')='SYNCPROCESSOR' THEN
		RETURN true;
	END IF;
	RETURN false;	  
  END IS_SQPROCESSOR;
  --======================================================================================
  --  IS EQPROCESSOR
  --======================================================================================
  FUNCTION IS_EQPROCESSOR RETURN BOOLEAN IS
  BEGIN
	IF get_env_var('TSDCONNTYPE')='EQPROCESSOR' THEN
		RETURN true;
	END IF;
	RETURN false;	  
  END IS_EQPROCESSOR;
  
  --======================================================================================  
end tsdb_support;
