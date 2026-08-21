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

## Enforced boundaries

- Dependencies point inward only: `infrastructure` → `application` → `domain`.
- The domain depends only on the JDK; the application layer adds only the logging facade and `@Transactional`.
- Infrastructure never reaches a use case implementation, only its interface; an output port is used only by the application and its own implementations, and implemented only in infrastructure.
