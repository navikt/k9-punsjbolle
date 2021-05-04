CREATE TABLE behandling
(
    id                          BIGSERIAL PRIMARY KEY,
    behovssekvens_id            VARCHAR(50) NOT NULL,
    k9_saksnummer               VARCHAR(50) NOT NULL,
    k9_behandling_id            VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    tidspunkt                   TIMESTAMP WITH TIME ZONE NOT NULL,
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,
    grunnlag                    JSONB NOT NULL,
    UNIQUE (k9_behandling_id, type)
);

CREATE INDEX index_behandling_k9_saksnummer ON behandling(k9_saksnummer);
CREATE INDEX index_behandling_k9_behandling_id ON behandling(k9_behandling_id);