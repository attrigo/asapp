# MANUAL RELEASE STEPS

> RELEASE_TAG=0.1.0 \
> CURRENT_VERSION=0.1.0-SNAPSHOT \
> NEXT_VERSION=0.2.0-SNAPSHOT \

1. Checkout main branch
    ```sh
    git checkout main
    ```

2. Close current version
    1. Remove SNAPSHOT version in pom files
        ```sh
        mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true
        ```

    2. Create Liquibase tag

3. Commit all changes
    ```sh
    git add .
    git commit -m "chore: close version ${CURRENT_VERSION}"
    ```

4. Create tag
    ```sh
    git tag ${RELEASE_TAG}
    ```

5. Prepare main branch for next version
    1. Update pom files to next SNAPSHOT version (TODO: to check)
        ```sh
        mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true
        mvn release:prepare
        ```

    2. Update version occurrences in all Java files

    3. Create Liquibase resources

6. Commit all changes
    ```sh
    git add .
    git commit -m "chore: prepare next development version ${NEXT_VERSION}"
    ```

7. Push all changes (TODO: to check)
    ```sh
    git push --atomic origin main ${RELEASE_TAG}
    ```