package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_parameter_key_types")
public class ParameterKeyType extends CatalogEntity {}
