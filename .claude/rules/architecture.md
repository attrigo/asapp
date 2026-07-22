---
paths:
  - "**/*.java"
---

## Package structure

New classes follow this package structure:

```
com.attrigo.asapp.<service>/
├── domain/<aggregate>/
├── application/<aggregate>/
│   ├── in/
│   │   ├── UseCase interfaces            # input ports
│   │   ├── command/                      # immutable command records
│   │   ├── result/                       # result records
│   │   └── service/                      # use case implementations
│   └── out/                              # output port interfaces
└── infrastructure/
    ├── <aggregate>/
    │   ├── in/
    │   │   ├── API interfaces             # OpenAPI-annotated
    │   │   ├── REST controllers
    │   │   ├── request/                   # request DTOs
    │   │   └── response/                  # response DTOs
    │   ├── mapper/                       # MapStruct mappers
    │   ├── out/                          # port adapters
    │   └── persistence/                  # JDBC entities, Spring Data repositories
    ├── security/                         # security components (cross-cutting)
    ├── error/                            # error management (cross-cutting)
    └── config/                           # configuration (cross-cutting)
```

## Dependency rule

`infrastructure → application → domain`. Never reverse.

## Architecture tests

ArchUnit fitness functions live in `<service>.architecture` (test scope), grouped by concern (e.g. `JsonNamingConventionTests`) — never mirrored into the package they scan.
