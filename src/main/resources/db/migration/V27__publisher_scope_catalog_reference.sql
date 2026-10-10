ALTER TABLE publishers
MODIFY publisher_scope VARCHAR(50) NOT NULL,
ADD CONSTRAINT fk_publishers_scope FOREIGN KEY (publisher_scope) REFERENCES type_resource_scopes (code);
