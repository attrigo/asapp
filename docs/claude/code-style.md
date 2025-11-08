# Code Style & Standards

## Formatting
- Uses Spotless Maven plugin with Eclipse formatter (`asapp_formatter.xml`)
- License header required (`header-license` file)
- Import order: `java|javax, org, com, , com.bcn`
- Line endings: UNIX (LF)
- Run `mvn spotless:apply` before committing

## Annotation Ordering

Annotation order goes from most to least relevant, top-down, based on how directly they define or affect the purpose and behavior of the class, method, or field.

### For Non-Test Classes (Controllers, Services, Entities, etc.)
1. **Component Role**: declares the core responsibility or purpose of the class (`@Controller`, `@Service`, `@Entity`)
2. **Configuration / Routing / Scope**: configures routes, profiles, scopes, and conditional behaviors (`@RequestMapping`, `@Scope`, `@Profile`)
3. **API Documentation**: defines API metadata for documentation tools (`@Tag`, `@Operation`, `@Schema`)
4. **Security**: controls access, authorization, or roles (`@PreAuthorize`, `@Secured`)
5. **Transaction / Caching / Scheduling**: manages database transactions, cache, or scheduled tasks (`@Transactional`, `@Cacheable`)
6. **Persistence Mapping**: defines how the class maps to a database or storage system (`@Table`, `@Id`, `@Column`)
7. **Serialization / Deserialization**: controls how data is serialized or deserialized (`@JsonProperty`, `@JsonIgnore`)
8. **Validation**: applies constraints and validation rules (`@NotNull`, `@Size`, `@Valid`)
9. **Object Mapping / Transformation**: maps or transforms objects across layers (`@Mapping`, `@InheritInverseConfiguration`)
10. **Code Generation / Boilerplate Elimination**: generates constructors, builders, getters, setters (`@Data`, `@Builder`)
11. **Custom / Domain-Specific**: annotations specific to your business logic or architecture (`@Auditable`, `@TenantAware`)

### For Test Classes (Unit, Integration, Slice Tests, etc.)
1. **Test Context Setup**: defines how the test runs and loads context/environment (`@SpringBootTest`, `@WebMvcTest`, `@ContextConfiguration`, `@Import`, `@AutoConfigure...`, `@ExtendWith`, `@TestPropertySource`)
2. **Environment / Infrastructure Setup**: configures runtime environment like containers, mock servers, etc. (`@Testcontainers`, `@Container`)
3. **API Documentation**: documents test behaviors or contracts for external tools (`@Schema`, `@ApiResponse`)
4. **Lifecycle / Execution Order**: defines how tests are executed or ordered (`@TestInstance`, `@TestMethodOrder`)
5. **Dependency Mocking / Injection**: sets up test doubles and injection (`@MockBean`, `@Mock`, `@InjectMocks`)
6. **Transaction / Rollback Behavior**: manages transactional context in tests (`@Transactional`, `@Rollback`)
7. **Test Behavior / Markers**: standard JUnit or test framework annotations (`@Test`, `@DisplayName`, `@Disabled`)
8. **Custom Test Annotations**: project-specific markers for test type or category (`@IntegrationTest`, `@SmokeTest`)

## Commit Messages
- Follow Conventional Commits standard
- Git hooks automatically validate commit messages
- Format: `<type>: <description>`
- Types: feat, fix, chore, docs, test, refactor

## Git Hooks
Installed automatically on `mvn install`:
- **pre-commit**: Checks code style with Spotless
- **commit-msg**: Validates commit message format