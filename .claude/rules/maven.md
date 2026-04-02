---
paths:
  - "**/pom.xml"
---

# Maven POM Conventions

## Ordering

All entries are sorted alphabetically within their section:

- `<properties>`: sorted by property name
- `<dependencies>` / `<dependencyManagement>`: sorted by `<groupId>` and `<artifactId>` within each comment-delimited group
- `<plugins>` / `<pluginManagement>`: sorted by `<groupId>` and `<artifactId>` within each comment-delimited group
