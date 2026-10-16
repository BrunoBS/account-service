-- Ensure the catalog state required to retire the standalone contract on upgrades.
INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}');

-- Publish the unified request contract without changing previously applied migrations.
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 2, 'configuration-resource v2', '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Configuration request",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "type",
    "value"
  ],
  "properties": {
    "version": {
      "type": [
        "integer",
        "null"
      ],
      "minimum": 0,
      "maximum": 9223372036854775807
    },
    "microserviceIdentifier": {
      "type": "string",
      "format": "uuid",
      "maxLength": 36
    },
    "key": {
      "type": "string",
      "maxLength": 80,
      "pattern": "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$"
    },
    "description": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 200
    },
    "type": {
      "type": "string",
      "enum": [
        "CONFIGURATION",
        "EXPRESSION"
      ]
    },
    "value": {}
  },
  "allOf": [
    {
      "oneOf": [
        {
          "required": [
            "microserviceIdentifier",
            "key"
          ],
          "properties": {
            "version": {
              "type": "null"
            }
          }
        },
        {
          "required": [
            "version"
          ],
          "properties": {
            "version": {
              "type": "integer"
            },
            "microserviceIdentifier": false,
            "key": false
          }
        }
      ]
    },
    {
      "if": {
        "properties": {
          "type": {
            "const": "EXPRESSION"
          }
        },
        "required": [
          "type"
        ]
      },
      "then": {
        "properties": {
          "value": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "operator",
              "valueType",
              "value"
            ],
            "properties": {
              "operator": {
                "enum": [
                  "EQUALS",
                  "NOT_EQUALS",
                  "CONTAINS",
                  "NOT_CONTAINS",
                  "GREATER_THAN",
                  "GREATER_THAN_OR_EQUALS",
                  "LESS_THAN",
                  "LESS_THAN_OR_EQUALS"
                ]
              },
              "valueType": {
                "enum": [
                  "STRING",
                  "INTEGER",
                  "LONG",
                  "DECIMAL"
                ]
              },
              "value": {}
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "CONTAINS",
                        "NOT_CONTAINS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "type": "array",
                      "minItems": 1,
                      "uniqueItems": true
                    }
                  }
                },
                "else": {
                  "properties": {
                    "value": {
                      "not": {
                        "type": "array"
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "GREATER_THAN",
                        "GREATER_THAN_OR_EQUALS",
                        "LESS_THAN",
                        "LESS_THAN_OR_EQUALS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "valueType": {
                      "enum": [
                        "INTEGER",
                        "LONG",
                        "DECIMAL"
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "STRING"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "string"
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "string"
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "INTEGER"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -2147483648,
                          "maximum": 2147483647
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -2147483648,
                            "maximum": 2147483647
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "LONG"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -9223372036854775808,
                          "maximum": 9223372036854775807
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -9223372036854775808,
                            "maximum": 9223372036854775807
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "DECIMAL"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "number"
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "number"
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
          }
        }
      }
    }
  ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

-- Retire the standalone expression contract while retaining its published history.
UPDATE schema_configuration
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE resource_type = 'PLATFORM' AND resource_code = 'configuration-expression' AND lifecycle_code = 'ACTIVE';

UPDATE schema_definitions
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression' AND lifecycle_code = 'ACTIVE';
