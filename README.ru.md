# listing-toolkit

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5+](https://img.shields.io/badge/Spring%20Boot-3.5+-brightgreen.svg)](https://spring.io/projects/spring-boot)

> 🇬🇧 **English version:** [README.md](README.md)

Spring Data JPA toolkit для построения «листингов» — табличных представлений с фильтрами, сортировкой, пагинацией и экспортом. Один артефакт собирает в одном месте паттерны, которые в обычных проектах разбросаны по разным утилитам.

**Что внутри:**
- Расширение `JpaSpecificationExecutor` с EntityGraph, потоковым чтением и выборкой уникальных значений
- Pageable на основе `offset/limit` для контракта типа `?limit=20&offset=100`
- Парсер строк сортировки `asc(field)`/`desc(field)` для REST API
- Хелперы CriteriaBuilder: `getOrCreateJoin`, case-insensitive `LIKE`
- Потоковый XLSX-экспорт на SXSSF с типизированными колонками
- Caffeine-кеш для `totalCount` по комбинациям фильтров

## Подключение

```xml
<dependency>
    <groupId>io.github.millersa</groupId>
    <artifactId>listing-toolkit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Требования: **Java 21+**, **Spring Boot 3.5+**, PostgreSQL.

В главном классе приложения подключить кастомный repository-base:

```java
import io.github.millersa.listing.pagination.CustomJpaSpecificationExecutorImpl;

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "your.app.repository",
    repositoryBaseClass = CustomJpaSpecificationExecutorImpl.class
)
public class YourApplication { }
```

Репозитории объявить так:

```java
import io.github.millersa.listing.pagination.CustomJpaSpecificationExecutor;

public interface MyRepository extends JpaRepository<MyEntity, Long>,
                                       CustomJpaSpecificationExecutor<MyEntity> {
    // findAll(spec, pageable, graph), streamAll(...), findUniqueValues(...) — теперь доступны
}
```

## Stability

В стадии активной разработки (SNAPSHOT). Публичный API может меняться до релиза `1.0.0`.

## Что внутри

### `io.github.millersa.listing.pagination`
- **`CustomJpaSpecificationExecutor`** — расширение `JpaSpecificationExecutor` с EntityGraph, Stream, уникальными значениями.
- **`CustomJpaSpecificationExecutorImpl`** — реализация. Подключается через `repositoryBaseClass = ...`.
- **`SimpleOffsetSortedRequest`** / **`AbstractOffsetSortedRequest`** — `Pageable` на основе `offset`/`limit`.
- **`SortUtils`** — парсинг строк сортировки вида `"asc(field)"`/`"desc(field)"`.
- **`RepositoryUtils`** — `getOrCreateJoin` (переиспользование JOIN'ов), `likeIgnoreCase…` (case-insensitive LIKE).

### `io.github.millersa.listing.uniquefield`
- **`UniqueFieldHelperMap`**, **`QueryPair`**, **`SelectAndPredicateCreator`** — инфраструктура для выборки уникальных значений по одному полю с фильтром поиска. Используется в autocomplete фильтрах.

### `io.github.millersa.listing.xlsx`
- **`XlsxColumn<T>`** — декларативное описание колонки (`string` / `longCol` / `date`).
- **`XlsxStreamWriter<T>`** — generic потоковый писатель XLSX на SXSSF. Окно строк настраивается через перегрузку конструктора, дефолт 100.

### `io.github.millersa.listing.cache`
- **`FilteredCountCache`** — Caffeine-кеш для `totalCount`. Включается через свойство.

### `io.github.millersa.listing.dto` (опционально)
- **`Pagination`** (`limit`, `offset`), **`Sorting`** (`sortBy: List<String>`), **`UniqueField`** (`fieldName`, `fieldSearch`) — Java records, повторяющие рекомендованный REST-контракт.
- **`ListRequest<F>`** — обёртка filter + pagination + sorting + (опционально) uniqueField. Можно использовать как `@RequestBody`.
- **`ListResponse<T>`** — `data + totalElements + totalPages`, с фабрикой `ListResponse.from(Page<T>)`.
- Все «сырые» API по-прежнему принимают примитивные параметры — DTO это удобный fast-path, не обязательство.

## Использование

### Контроллер на DTO-records

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
        // mySpec — это ВАШ класс приложения, реализует сборку Specification под свою entity.
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

DTO records один-в-один совпадают с JSON-контрактом. Spring сам десериализует через Jackson.

### Стримовая выборка + XLSX

```java
@Transactional(readOnly = true, propagation = Propagation.MANDATORY)
public void exportLargeDataset(SpecDto filter, OutputStream out) throws IOException {
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
    XlsxColumn.string ("Имя",      MyDto::getName),
    XlsxColumn.date   ("Создан",   MyDto::getCreatedAt)
);
```

### Кеш totalCount

`application.yml`:
```yaml
listing:
  count-cache:
    enabled: true
    ttl: 60s
    max-size: 2000
```

В сервисе:
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

**Важно про ключ кеша:** `key(namespace, filter)` использует Jackson-сериализацию. Если у тебя в Spring-контексте нет `ObjectMapper` (что необычно для Spring Boot), метод бросит `IllegalStateException`. Это сделано умышленно — без Jackson дефолтный `Object.hashCode()` основан на identity, и кеш стал бы бесполезным.

### Уникальные значения (autocomplete)

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

// в сервисе:
Page<MyEntity> page = repo.findUniqueValues(
    spec, "name", searchString, offset, limit, mySpec.getHelperMap(), graph);
```

## Интеграция с фронтом

Сама библиотека не приносит REST-эндпоинтов — их пишут consumer-проекты. Но контракт, который она навязывает (offset-пагинация, sortBy-строки `asc(field)`, отдельный `uniqueField`-режим, стриминговая выгрузка `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`), одинаковый во всех приложениях, построенных на ней. Ниже — рекомендованный контракт клиента и примеры на TypeScript.

### Рекомендованный REST-контракт

Типичный listing-запрос принимает JSON-тело:

```json
{
  "filter": {
    "name":   ["Алиса", "Боб"],
    "status": ["ACTIVE"]
  },
  "pagination": { "limit": 20, "offset": 0 },
  "sorting":    { "sortBy": ["desc(createdAt)", "asc(name)"] }
}
```

Ответ:

```json
{
  "data": [ { "id": 1, "name": "Алиса", "createdAt": "2026-05-31T10:00:00Z" } ],
  "totalElements": 1234,
  "totalPages": 62
}
```

Для autocomplete уникальных значений в том же теле появляется блок `uniqueField`:

```json
{
  "filter": { "status": ["ACTIVE"] },
  "pagination": { "limit": 20, "offset": 0 },
  "uniqueField": { "fieldName": "name", "fieldSearch": "али" }
}
```

Для xlsx-выгрузки — то же тело, ответ — бинарный поток с `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` и `Content-Disposition: attachment`.

### TypeScript-типы

```ts
export interface Pagination { limit: number; offset: number }
export interface Sorting    { sortBy: string[] }          // например ["asc(name)", "desc(id)"]
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

// helper для построения sort-токена: sortToken('createdAt', 'desc') → "desc(createdAt)"
export const sortToken = (field: string, dir: 'asc' | 'desc') => `${dir}(${field})`
```

### Пример 1: пагинация + сортировка (axios)

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

// Использование:
const page = await fetchMatrix(0, 20, { roles: ['admin'] })
console.log(page.totalElements, page.data)
```

### Пример 2: autocomplete-фильтр (с debounce)

```ts
import { useEffect, useState } from 'react'
import { useDebounce } from 'use-debounce'  // или любой другой debounce-hook
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

`uniqueField.fieldName` должно совпадать с ключом, зарегистрированным на бэкенде в `UniqueFieldHelperMap.put(...)`. Пагинация всё ещё работает — список подсказок это первая страница уникальных значений.

### Пример 3: скачивание xlsx-выгрузки

```ts
import axios from 'axios'
import { saveAs } from 'file-saver'

async function exportMatrixToXlsx(filter: MatrixFilter, timeShift = 0) {
  const body: ListRequest<MatrixFilter> = {
    filter,
    pagination: { limit: 0, offset: 0 }  // игнорируется стримящими экспортами
  }
  const response = await axios.post('/api/v2/matrix/exporttoxlsx', body, {
    responseType: 'blob',
    params: { timeShift }
  })

  // Имя файла из Content-Disposition (сервер его проставляет)
  const cd: string | undefined = response.headers['content-disposition']
  const match = cd?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
  const filename = match ? decodeURIComponent(match[1]) : `export-${Date.now()}.xlsx`

  saveAs(response.data, filename)
}
```

**Важно:** для больших выгрузок сервер отдаёт ответ потоком. Используй `responseType: 'blob'` и не пытайся парсить body как JSON — браузер получает бинарный поток порциями, а `saveAs` (или `URL.createObjectURL`) сохраняет его на диск.

Если нужен **индикатор прогресса** — `axios` поддерживает `onDownloadProgress`:

```ts
const response = await axios.post(url, body, {
  responseType: 'blob',
  onDownloadProgress: e => {
    if (e.total) {
      const percent = Math.round((e.loaded / e.total) * 100)
      console.log(`Скачано ${percent}%`)
    }
  }
})
```

Стриминг сервера обычно не выставляет `Content-Length`, так что `e.total` может быть `undefined` — в таком случае показывай неопределённый спиннер вместо процентов.

### Пример 4: хранение фильтра и пагинации в URL

Если нужны share-able ссылки (например `/matrix?roles=admin&page=3&sort=desc(createdAt)`), парси их в тело запроса:

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
    sorting:    { sortBy: searchParams.getAll('sort') }   // ?sort=desc(createdAt)
  }
}
```

## Best practices (чек-лист)

При использовании библиотеки в новых endpoint'ах:

- [ ] Метод, возвращающий `Stream<>` — `@Transactional(propagation = MANDATORY)` (fail-fast если транзакции нет).
- [ ] В цикле чтения стрима — `entityManager.detach(entity)` после маппинга + `clear()` каждые 500 строк.
- [ ] На `@ManyToMany`/`@OneToMany`-коллекциях, которые читаются маппером — `@BatchSize(500)` (не `EAGER` + entity graph).
- [ ] Справочники (`*Status`, `*Type`, `Role`, `Abbreviation` и т. п.) — `@Cache(usage = READ_ONLY)` + `@Cacheable` (Hibernate L2).
- [ ] EntityGraph покрывает ровно те ассоциации, что читает маппер — ни больше, ни меньше.
- [ ] При экспорте больших данных — `XlsxStreamWriter`, **никогда** не `XSSFWorkbook` + `ByteArrayOutputStream`.
- [ ] При тяжёлом count — `FilteredCountCache` с TTL 30–60 сек.
- [ ] Для `LIKE '%x%'` — рассмотреть GIN-индекс через `pg_trgm`.

## Сборка

```bash
mvn clean install                 # локально в ~/.m2 (включая тесты)
mvn test                          # только тесты
mvn -P release clean deploy       # публикация в Maven Central через Sonatype Central Portal
```

Подробная инструкция публикации — см. [PUBLISHING.md](PUBLISHING.md).

## Лицензия

[MIT License](LICENSE).
