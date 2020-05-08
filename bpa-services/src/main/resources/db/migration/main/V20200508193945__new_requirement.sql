ALTER TABLE eg_bpa_buildingplan
RENAME COLUMN permitorderno TO approvalno;

ALTER TABLE eg_bpa_buildingplan
DROP COLUMN IF EXISTS servicetype,
DROP COLUMN IF EXISTS risktype,
DROP COLUMN IF EXISTS applicationtype;

ALTER TABLE eg_bpa_auditdetails
RENAME COLUMN permitorderno TO approvalno;

ALTER TABLE eg_bpa_auditdetails
DROP COLUMN IF EXISTS servicetype,
DROP COLUMN IF EXISTS risktype,
DROP COLUMN IF EXISTS applicationtype;

ALTER TABLE eg_bpa_buildingplan
ADD COLUMN accountid character varying(256) DEFAULT null;
ALTER TABLE eg_bpa_auditdetails
ADD COLUMN accountid character varying(256) DEFAULT null;

ALTER TABLE eg_bpa_buildingplan
ADD COLUMN assignes character varying(256) DEFAULT null;

ALTER TABLE eg_bpa_auditdetails
ADD COLUMN assignes character varying(256) DEFAULT null;

ALTER TABLE eg_bpa_auditdetails
ADD COLUMN comments character varying(64) DEFAULT null;

ALTER TABLE eg_bpa_buildingplan
ADD COLUMN comments character varying(64) DEFAULT null;

ALTER TABLE eg_bpa_auditdetails
DROP COLUMN IF EXISTS ownershipcategory,
DROP COLUMN IF EXISTS occupancytype,
DROP COLUMN IF EXISTS suboccupancytype,
DROP COLUMN IF EXISTS usages,
DROP COLUMN IF EXISTS holdingno;

ALTER TABLE eg_bpa_buildingplan
DROP COLUMN IF EXISTS ownershipcategory,
DROP COLUMN IF EXISTS occupancytype,
DROP COLUMN IF EXISTS suboccupancytype,
DROP COLUMN IF EXISTS usages,
DROP COLUMN IF EXISTS holdingno;

ALTER TABLE eg_bpa_buildingplan
DROP COLUMN IF EXISTS registrationdetails,
DROP COLUMN IF EXISTS govtorquasi,
DROP COLUMN IF EXISTS validitydate,
DROP COLUMN IF EXISTS tradetype,
DROP COLUMN IF EXISTS remarks;

ALTER TABLE eg_bpa_auditdetails
DROP COLUMN IF EXISTS registrationdetails,
DROP COLUMN IF EXISTS govtorquasi,
DROP COLUMN IF EXISTS validitydate,
DROP COLUMN IF EXISTS tradetype,
DROP COLUMN IF EXISTS remarks;