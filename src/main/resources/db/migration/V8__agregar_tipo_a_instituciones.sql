ALTER TABLE instituciones
    ADD tipo VARCHAR(80) NULL DEFAULT 'Sin especificar';

ALTER TABLE instituciones_aud
    ADD tipo VARCHAR(80);
