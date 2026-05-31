# listing-toolkit

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5+](https://img.shields.io/badge/Spring%20Boot-3.5+-brightgreen.svg)](https://spring.io/projects/spring-boot)

> 🇷🇺 **Русская версия:** [README.ru.md](README.ru.md)

Spring Data JPA toolkit for tabular listings — filters, sorting, pagination, autocomplete and streaming export. A single artifact gathers patterns that are typically scattered across project-specific utilities.

**What's inside:**
- `JpaSpecificationExecutor` extension with EntityGraph, streaming reads and unique-value extraction
- `Pageable` with `offset`/`limit` semantics for REST contracts like `?limit=20&offset=100`
- Sort string parser for `asc(field)` / `desc(field)`
- CriteriaBuilder helpers: `getOrCreateJoin`, case-insensitive `LIKE`
- Streaming XLSX export on top of SXSSF with typed columns
- Caffeine-backed cache for `totalCount` across filter combinations

## Installation

```xml
<dependency>
    <groupId>io.github.millersa</groupId>
    <artifactId>listing-toolkit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Requirements: **Java 21+**, **Spring Boot 3.5+**, PostgreSQL (other JDBC databases work for most of the API, but `RepositoryUtils.likeIgnoreCase…` uses PostgreSQL-specific `regexp_replace` / `to_char`).

Wire the custom repository base into your main class:

```java
import io.github.millersa.listing.pagination.CustomJpaSpecificationExecutorImpl;

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "your.app.repository",
    repositoryBaseClass = CustomJpaSpecificationExecutorImpl.class
)
public class YourApplication { }
```

Declare your repositories:

```java
import io.github.millersa.listing.pagination.CustomJpaSpecificationExecutor;

public interface MyRepository extends JpaRepository<MyEntity, Long>,
                                       CustomJpaSpecificationExecutor<MyEntity> {
    // findAll(spec, pageable, graph), streamAll(...), findUniqueValues(...) are now available
}
```

## Stability

Under active development (SNAPSHOT). The public API may change before `1.0.0`.

## Package overview

### `io.github.millersa.listing.pagination`
- **`CustomJpaSpecificationExecutor`** — `JpaSpecificationExecutor` with EntityGraph, Stream and unique-value queries.
- **`CustomJpaSpecificationExecutorImpl`** — implementation wired via `repositoryBaseClass`.
- **`SimpleOffsetSortedRequest`** / **`AbstractOffsetSortedRequest`** — `Pageable` based on offset/limit.
- **`SortUtils`** — parser for `"asc(field)"` / `"desc(field)"` strings.
- **`RepositoryUtils`** — `getOrCreateJoin` (reuse JOINs across predicates), `likeIgnoreCase…` (case-insensitive LIKE).

### `io.github.millersa.listing.uniquefield`
- **`UniqueFieldHelperMap`**, **`QueryPair`**, **`SelectAndPredicateCreator`** — infrastructure to query unique values for a single field with a search filter. Used by autocomplete filters.

### `io.github.millersa.listing.xlsx`
- **`XlsxColumn<T>`** — declarative column description (`string` / `longCol` / `date`).
- **`XlsxStreamWriter<T>`** — generic streaming XLSX writer on top of SXSSF. Window size configurable via constructor overload, default 100.

### `io.github.millersa.listing.cache`
- **`FilteredCountCache`** — Caffeine cache for `totalCount`. Enabled via property.

### `io.github.millersa.listing.dto` (optional convenience)
- **`Pagination`** (`limit`, `offset`), **`Sorting`** (`sortBy: List<String>`), **`UniqueField`** (`fieldName`, `fieldSearch`) — Java records mirroring the recommended REST contract.
- **`ListRequest<F>`** — wrap your filter + pagination + sorting + (optional) uniqueField. Usable directly as `@RequestBody`.
- **`ListResponse<T>`** — `data + totalElements + totalPages`, with factory `ListResponse.from(Page<T>)`.
- All raw APIs still accept primitive arguments — DTOs are an optional fast path, not a requirement.

## Backend usage

### Controller with DTO records

```java
import io.github.millersa.listing.dto.*;
import io.github.millersa.listing.pagination.SimpleOffsetSortedRequest;

@RestController
@RequiredArgsConstructor
public class MyController {

    private final MyRepository repo;
    private final MySpecification mySpec;
    private final MyMapper mapper;

    @PostMapping("/api/v1/items")
    public ListResponse<MyDto> list(@RequestBody ListRequest<MyFilter> req) {
        // mySpec is YOUR application class — you write a Specification builder per entity.
        Specification<MyEntity> spec = mySpec.getFilter(req.filter())
                .and(mySpec.getSort(req.sorting()));
        Pageable pageable = new SimpleOffsetSortedRequest(req.pagination());

        if (req.isUniqueFieldMode()) {
            return ListResponse.from(
                repo.findUniqueValues(spec, req, mySpec.helperMap(), mySpec.graph()),
                mapper::toDto);
        }
        return ListResponse.from(
            repo.findAll(spec, pageable, mySpec.graph()),
            mapper::toDto);
    }
}
```

DTO records match the JSON contract one-to-one. Spring auto-deserializes them with Jackson.

### Streaming query + XLSX

```java
@Transactional(readOnly = true, propagation = Propagation.MANDATORY)
public void exportLargeDataset(FilterDto filter, OutputStream out) throws IOException {
    Specification<MyEntity> spec = mySpec.getFilter(filter);
    try (Stream<MyEntity> stream = myRepo.streamAll(spec, Pageable.unpaged(), mySpec.fetchGraph());
         XlsxStreamWriter<MyDto> writer = new XlsxStreamWriter<>("Sheet", COLUMNS)) {
        stream.forEach(e -> {
            writer.writeRow(mapper.toDto(e));
            entityManager.detach(e);
        });
        writer.writeTo(out);
    }
}

private static final List<XlsxColumn<MyDto>> COLUMNS = List.of(
    XlsxColumn.longCol("ID",       MyDto::getId),
    XlsxColumn.string ("Name",     MyDto::getName),
    XlsxColumn.date   ("Created",  MyDto::getCreatedAt)
);
```

### `totalCount` cache

`application.yml`:
```yaml
listing:
  count-cache:
    enabled: true
    ttl: 60s
    max-size: 2000
```

In a service:
```java
@Autowired private FilteredCountCache countCache;

public Page<MyDto> list(MyFilter filter, Pageable pageable) {
    Specification<MyEntity> spec = mySpec.getFilter(filter);
    String key = countCache.key("my-entity", filter);
    long total = countCache.getOrCompute(key, () -> repo.count(spec));
    List<MyEntity> data = repo.findAll(spec, pageable).getContent();
    return new PageImpl<>(data.stream().map(mapper::toDto).toList(), pageable, total);
}
```

**Cache key note:** `key(namespace, filter)` uses Jackson serialization. If your Spring context has no `ObjectMapper` (unusual for Spring Boot apps), the method throws `IllegalStateException`. This is intentional — without Jackson, the default `Object.hashCode()` is identity-based and the cache would be useless.

### Unique values (autocomplete)

```java
import io.github.millersa.listing.uniquefield.UniqueFieldHelperMap;
import io.github.millersa.listing.uniquefield.QueryPair;
import io.github.millersa.listing.pagination.RepositoryUtils;

public final class MySpecification {
    private static final UniqueFieldHelperMap<MyEntity> helperMap = new UniqueFieldHelperMap<>();

    static {
        helperMap.put("name",
            (root, q, cb, search) -> {
                Path<String> field = root.get("name");
                Predicate p = StringUtils.isBlank(search)
                    ? cb.isNotNull(field)
                    : cb.and(cb.isNotNull(field),
                             RepositoryUtils.likeIgnoreCaseWithWhitespaceNormalize(cb, field, search));
                return new QueryPair(field, p);
            },
            tuple -> { var e = new MyEntity(); e.setName((String) tuple.get(0)); return e; }
        );
    }

    public UniqueFieldHelperMap<MyEntity> getHelperMap() { return helperMap; }
}

// In a service:
Page<MyEntity> page = repo.findUniqueValues(
    spec, "name", searchString, offset, limit, mySpec.getHelperMap(), graph);
```

## Frontend integration

The toolkit doesn't ship REST endpoints — your controllers do. But the conventions it enforces (offset-based pagination, `asc(field)` sort strings, dedicated `uniqueField` query, streamed `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for exports) are uniform across all backends built on it. This section shows the recommended client contract and TypeScript examples.

### Recommended REST contract

A typical listing endpoint takes a JSON body like this:

```json
{
  "filter": {
    "name": ["Alice", "Bob"],
    "status": ["ACTIVE"]
  },
  "pagination": { "limit": 20, "offset": 0 },
  "sorting":   { "sortBy": ["desc(createdAt)", "asc(name)"] }
}
```

Response:

```json
{
  "data": [ { "id": 1, "name": "Alice", "createdAt": "2026-05-31T10:00:00Z" } ],
  "totalElements": 1234,
  "totalPages": 62
}
```

For unique-value autocomplete the same body has a `uniqueField` block:

```json
{
  "filter": { "status": ["ACTIVE"] },
  "pagination": { "limit": 20, "offset": 0 },
  "uniqueField": { "fieldName": "name", "fieldSearch": "ali" }
}
```

For XLSX export — same body, response is a binary stream with `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` and `Content-Disposition: attachment`.

### TypeScript types

```ts
export interface Pagination { limit: number; offset: number }
export interface Sorting    { sortBy: string[] }          // e.g. ["asc(name)", "desc(id)"]
export interface UniqueField { fieldName: string; fieldSearch?: string }

export interface ListRequest<F> {
  filter?: F
  pagination: Pagination
  sorting?: Sorting
  uniqueField?: UniqueField
}

export interface ListResponse<T> {
  data: T[]
  totalElements: number
  totalPages: number
}

// helper to build a sort token: sortToken('createdAt', 'desc') → "desc(createdAt)"
export const sortToken = (field: string, dir: 'asc' | 'desc') => `${dir}(${field})`
```

### Example 1: pagination + sorting (axios)

```ts
import axios from 'axios'

interface MatrixFilter { roles?: string[]; objectTypes?: string[] }
interface MatrixRow    { id: number; name: string; status: string; createdAt: string }

async function fetchMatrix(page: number, pageSize: number, filter: MatrixFilter) {
  const body: ListRequest<MatrixFilter> = {
    filter,
    pagination: { limit: pageSize, offset: page * pageSize },
    sorting:    { sortBy: [sortToken('createdAt', 'desc'), sortToken('id', 'desc')] }
  }
  const { data } = await axios.post<ListResponse<MatrixRow>>('/api/v2/matrix', body)
  return data
}

// Usage:
const page = await fetchMatrix(0, 20, { roles: ['admin'] })
console.log(page.totalElements, page.data)
```

### Example 2: autocomplete filter (debounced)

```ts
import { useEffect, useState } from 'react'
import { useDebounce } from 'use-debounce'  // or any other debounce hook
import axios from 'axios'

interface UniqueValueResponse { value: string; id?: number | string }

export function ObjectNameAutocomplete({ onSelect }: { onSelect: (v: string) => void }) {
  const [search, setSearch] = useState('')
  const [debounced] = useDebounce(search, 300)
  const [options, setOptions] = useState<UniqueValueResponse[]>([])

  useEffect(() => {
    const body: ListRequest<{}> = {
      pagination: { limit: 20, offset: 0 },
      uniqueField: { fieldName: 'objectName', fieldSearch: debounced }
    }
    axios.post<ListResponse<UniqueValueResponse>>('/api/v2/matrix', body)
         .then(r => setOptions(r.data.data))
  }, [debounced])

  return (
    <input list="opts" value={search}
           onChange={e => setSearch(e.target.value)}
           onBlur={e => onSelect(e.target.value)}>
      <datalist id="opts">
        {options.map((o, i) => <option key={i} value={o.value} />)}
      </datalist>
    </input>
  )
}
```

Send `uniqueField.fieldName` matching one of the keys you registered in `UniqueFieldHelperMap.put(...)` on the backend. Pagination still applies — the suggestion list is essentially the first page of distinct values.

### Example 3: XLSX export download

```ts
import axios from 'axios'
import { saveAs } from 'file-saver'

async function exportMatrixToXlsx(filter: MatrixFilter, timeShift = 0) {
  const body: ListRequest<MatrixFilter> = {
    filter,
    pagination: { limit: 0, offset: 0 }  // ignored by streaming exports
  }
  const response = await axios.post('/api/v2/matrix/exporttoxlsx', body, {
    responseType: 'blob',
    params: { timeShift }
  })

  // Filename from Content-Disposition (server provides it)
  const cd: string | undefined = response.headers['content-disposition']
  const match = cd?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
  const filename = match ? decodeURIComponent(match[1]) : `export-${Date.now()}.xlsx`

  saveAs(response.data, filename)
}
```

**Important:** for large exports the server streams the response. Use `responseType: 'blob'` and don't try to parse the body as JSON — the browser receives a binary stream chunk by chunk, and `saveAs` (or `URL.createObjectURL`) writes it to disk.

If you need a **progress indicator** for big files, use `axios` `onDownloadProgress`:

```ts
const response = await axios.post(url, body, {
  responseType: 'blob',
  onDownloadProgress: e => {
    if (e.total) {
      const percent = Math.round((e.loaded / e.total) * 100)
      console.log(`Download ${percent}%`)
    }
  }
})
```

Note: server-side streaming usually doesn't set `Content-Length`, so `e.total` may be undefined. Show a spinner without a percentage in that case.

### Example 4: keep filter / pagination state in the URL

If you want shareable URLs (e.g. `/matrix?roles=admin&page=3&sort=desc(createdAt)`), parse them into the request body:

```ts
import { useSearchParams } from 'react-router-dom'

function useMatrixFilterFromUrl(): ListRequest<MatrixFilter> {
  const [searchParams] = useSearchParams()
  const limit  = Number(searchParams.get('size') ?? 20)
  const offset = Number(searchParams.get('page') ?? 0) * limit
  return {
    filter: {
      roles:       searchParams.getAll('role'),
      objectTypes: searchParams.getAll('type')
    },
    pagination: { limit, offset },
    sorting:    { sortBy: searchParams.getAll('sort') }   // e.g. ?sort=desc(createdAt)
  }
}
```

## Best practices checklist

When integrating into a new endpoint:

- [ ] Stream-returning methods are `@Transactional(propagation = MANDATORY)` (fail-fast if no transaction).
- [ ] In stream consumers — `entityManager.detach(entity)` after mapping and `clear()` every ~500 rows.
- [ ] `@ManyToMany`/`@OneToMany` collections read by mappers — use `@BatchSize(500)`, not `EAGER` + entity graph.
- [ ] Lookup tables (`*Status`, `*Type`, `Role`, `Abbreviation`, …) — `@Cache(usage = READ_ONLY)` + `@Cacheable` (Hibernate L2).
- [ ] EntityGraph covers exactly the associations the mapper reads — no more, no less.
- [ ] For large exports — `XlsxStreamWriter`, **never** `XSSFWorkbook` + `ByteArrayOutputStream`.
- [ ] For heavy count queries — `FilteredCountCache` with TTL of 30–60 seconds.
- [ ] For `LIKE '%x%'` filters — consider a GIN index via `pg_trgm`.

## Build

```bash
mvn clean install                 # local install into ~/.m2 (runs tests)
mvn test                          # tests only
mvn -P release clean deploy       # publish to Maven Central via Sonatype Central Portal
```

Detailed publishing steps — see [PUBLISHING.md](PUBLISHING.md).

## License

[MIT License](LICENSE).
