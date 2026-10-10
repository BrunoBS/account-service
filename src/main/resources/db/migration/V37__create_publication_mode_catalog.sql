CREATE TABLE type_publication_modes (
    code VARCHAR(30) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_publication_modes PRIMARY KEY (code),
    CONSTRAINT ck_type_publication_modes_code CHECK (code IN ('AUTOMATIC', 'MANUAL'))
);

INSERT INTO
    type_publication_modes (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'AUTOMATIC',
        'Automática',
        'Propaga publicações para os destinos mapeados.',
        1,
        true,
        '{}'
    ),
    (
        'MANUAL',
        'Manual',
        'Permite publicação explícita pelo destino.',
        2,
        true,
        '{}'
    );

DELETE FROM type_sharing_statuses
WHERE
    code IN (
        'WAITING_DESTINATION_APPROVAL',
        'WAITING_SOURCE_APPROVAL',
        'CANCELLED',
        'NOT_REQUESTED'
    );

INSERT INTO
    type_sharing_statuses (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'PENDING',
        'Pendente',
        'Solicitação aguardando decisão do proprietário.',
        1,
        true,
        '{}'
    ),
    (
        'APPROVED',
        'Aprovada',
        'Participação autorizada enquanto contrato e mapeamento estiverem aptos.',
        2,
        true,
        '{}'
    ),
    (
        'REJECTED',
        'Rejeitada',
        'Solicitação rejeitada pelo proprietário.',
        3,
        true,
        '{}'
    ),
    (
        'REVOKED',
        'Revogada',
        'Participação aprovada e posteriormente revogada.',
        4,
        true,
        '{}'
    );
