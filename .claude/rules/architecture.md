---
paths:
  - "**/*.java"
---

# Package Structure

New classes follow this package structure:

```
com.bcn.asapp.<service>/
├── domain/<aggregate>/
├── application/<aggregate>/
│   ├── in/
│   │   ├── UseCase interfaces            # input ports
│   │   ├── command/                      # immutable command records
│   │   ├── result/                       # result records (multi-port aggregation)
│   │   └── service/                      # use case implementations
│   └── out/                              # output port interfaces
└── infrastructure/
    ├── <aggregate>/
    │   ├── in/                           # REST controllers, request/response DTOs
    │   ├── mapper/                       # MapStruct mappers
    │   ├── out/                          # port adapters
    │   └── persistence/                  # JDBC entities, Spring Data repositories
    ├── security/                         # security components (cross-cutting)
    ├── error/                            # error management (cross-cutting)
    └── config/                           # configuration (cross-cutting)
```

Dependency rule: `infrastructure → application → domain`. Never reverse.
