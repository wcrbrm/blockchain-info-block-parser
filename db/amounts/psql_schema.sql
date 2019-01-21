DROP TABLE IF EXISTS btc_amounts;

CREATE TABLE IF NOT EXISTS btc_amounts (
   height integer NOT NULL,
   tm     integer NOT NULL,
   amt    bigint  NOT NULL,
   q      integer NOT NULL DEFAULT 1
);

CREATE INDEX idx_amounts_height ON btc_amounts (height);
CREATE INDEX idx_amounts_tm ON btc_amounts (tm);
CREATE INDEX idx_amounts_amt ON btc_amounts (amt);
