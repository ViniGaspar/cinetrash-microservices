# Guia de Implementação — Movie Battle (Microsserviços)

Documento para o agente implementador. Stack: Java 21, Spring Boot 3.3+, Spring Cloud 2023.0.x, Maven multi-módulo, PostgreSQL, Eureka, Gateway, OpenFeign.

## 1. Parent pom.xml

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
    <relativePath/>
  </parent>
  <groupId>br.edu.moviebattle</groupId>
  <artifactId>movie-battle-parent</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>
  <modules>
    <module>discovery-service</module>
    <module>gateway-service</module>
    <module>catalog-service</module>
    <module>battle-service</module>
    <module>ranking-service</module>
  </modules>
  <properties>
    <java.version>21</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

## 2. discovery-service

Dependências: `spring-cloud-starter-netflix-eureka-server`, `spring-boot-starter-web`.

`DiscoveryServiceApplication.java`:
```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
  public static void main(String[] args) { SpringApplication.run(DiscoveryServiceApplication.class, args); }
}
```

`application.properties`:
```properties
server.port=8761
spring.application.name=discovery-service
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.server.enable-self-preservation=false
```

## 3. gateway-service

Dependências: `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`.

`application.properties`:
```properties
server.port=8080
spring.application.name=gateway-service
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true
spring.cloud.gateway.routes[0].id=catalog
spring.cloud.gateway.routes[0].uri=lb://catalog-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/catalog/**
spring.cloud.gateway.routes[1].id=battles
spring.cloud.gateway.routes[1].uri=lb://battle-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/battles/**
spring.cloud.gateway.routes[2].id=ranking
spring.cloud.gateway.routes[2].uri=lb://ranking-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/ranking/**
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

## 4. catalog-service

Dependências: web, data-jpa, postgresql, eureka-client, openfeign, validation, (opcional) springdoc-openapi.

### Entidade `Movie`
```java
@Entity @Table(name="movies")
public class Movie {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="external_id", unique=true, nullable=false) private Long externalId; // id do TMDB (inteiro)
  @Column(nullable=false) private String title;
  @Column(name="poster_path") private String posterPath;
  @Column(name="release_year") private Integer releaseYear;
  @Column(name="created_at") private LocalDateTime createdAt;
  @PrePersist void prePersist(){ createdAt = LocalDateTime.now(); }
  // getters/setters
}
```

### Repository
```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
  Optional<Movie> findByExternalId(String externalId);
  @Query(value = "SELECT * FROM movies ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
  Optional<Movie> findRandom();
  long count();
}
```

### TMDB client (Feign)
Usar um `@FeignClient` para o TMDB (URL externa), em vez de RestClient/WebClient, para manter padrão único de chamadas HTTP.

```java
@FeignClient(name = "tmdb", url = "${tmdb.base-url}")
public interface TmdbClient {
  @GetMapping("/search/movie")
  TmdbSearchResponse search(@RequestParam("query") String query,
                            @RequestParam("api_key") String apiKey,
                            @RequestParam(value = "language", defaultValue = "pt-BR") String language);

  @GetMapping("/movie/{id}")
  TmdbMovieDetails details(@PathVariable("id") Long id,
                           @RequestParam("api_key") String apiKey,
                           @RequestParam(value = "language", defaultValue = "pt-BR") String language);
}
```

Serviço `TmdbService` (wrapper):
```java
@Service
public class TmdbService {
  @Value("${tmdb.api-key}") private String apiKey;
  private final TmdbClient client;
  public TmdbService(TmdbClient client) { this.client = client; }
  public List<MovieDto> search(String query) {
    TmdbSearchResponse resp = client.search(query, apiKey, "pt-BR");
    return resp.getResults().stream().map(this::toDto).toList();
  }
  public MovieDto details(Long id) {
    return toDto(client.details(id, apiKey, "pt-BR"));
  }
}
```

Habilitar Feign em todas as apps que usam clientes:
```java
@SpringBootApplication
@EnableFeignClients
public class CatalogServiceApplication { ... }
```
(Análogo em battle-service; ranking e gateway não usam Feign.)

DTO `MovieDto`: `externalId (Long)`, `title`, `posterPath`, `releaseYear`. (TMDB retorna id como inteiro; armazenado como Long.)

### Service `CatalogService`
- `search(query)` → `tmdbService.search(query)`
- `register(externalId)`:
  - se já existe (`findByExternalId`), retorna existente
  - senão busca no TMDB por id? (endpoint `/movie/{id}`) para obter dados; ou usar dados do search. Recomendado: buscar detalhes por id para garantir poster/ano.
  - salva e retorna
- `randomPair()`:
  - se `count() < 2` → throw `ResponseStatusException(400)`
  - loop: pegar random; se igual ao primeiro, tentar novamente (max 10 tentativas); retornar par distinto.

### Controller
```java
@RestController @RequestMapping("/catalog/movies")
public class MovieController {
  @GetMapping("/search") public List<MovieDto> search(@RequestParam String query) {...}
  @PostMapping public ResponseEntity<MovieDto> register(@RequestBody @Valid RegisterRequest req) {...}
  @GetMapping("/random-pair") public RandomPairResponse randomPair() {...}
  @GetMapping public List<MovieDto> list() {...}
}
```

Nota: as rotas no gateway usam prefixo `/catalog`, então o controller pode ser `/movies` e o gateway reescreve? Mais simples: controller em `/catalog/movies` como acima, gateway passa path. Ou controller `/movies` e gateway `StripPrefix=1`. Escolha consistente. Recomendo controller com path completo `/catalog/movies`? Isso duplica. Melhor: controller base `/movies`, gateway route com `StripPrefix=1` para `/catalog/**`? Hmm. Simplicidade: controller `@RequestMapping("/movies")` e gateway route Path=/catalog/** com `StripPrefix=1` e uri lb://catalog-service. Aí `/catalog/movies/search` → `/movies/search`. Vou usar essa abordagem (mais padrão). Atualizar README endpoints: externos continuam `/catalog/...`.

## 5. battle-service

Dependências: web, data-jpa, postgresql, eureka-client, openfeign, validation.

### Entidade `Battle`
```java
@Entity @Table(name="battles")
public class Battle {
  @Id private UUID id;
  @Column(name="movie_a_id") private Long movieAId;
  @Column(name="movie_b_id") private Long movieBId;
  @Column(name="chosen_movie_id") private Long chosenMovieId;
  @Column(name="created_at") private LocalDateTime createdAt;
  @Column(name="voted_at") private LocalDateTime votedAt;
  @PrePersist void pre(){ id = UUID.randomUUID(); createdAt = LocalDateTime.now(); }
}
```

### Feign clients
```java
@FeignClient(name = "catalog-service")
public interface CatalogClient {
  @GetMapping("/movies/random-pair")
  RandomPairResponse getRandomPair();
}

@FeignClient(name = "ranking-service")
public interface RankingClient {
  @PostMapping("/votes")
  void registerVote(VoteRequest req);
}
```

Atenção: Feign chama paths internos (sem prefixo /catalog). Garantir que catalog controller base seja `/movies`.

### Service `BattleService`
- `start()`:
  - `pair = catalogClient.getRandomPair()`
  - salva Battle com movieAId, movieBId
  - retorna `{ battleId, movieA, movieB }`
- `vote(battleId, chosenMovieId)`:
  - busca Battle; se não existe → 404
  - se já votada (`chosenMovieId != null`) → 409
  - valida chosenMovieId ∈ {movieAId, movieBId} → senão 400
  - set chosenMovieId, votedAt; save
  - montar VoteRequest com dados do filme escolhido. Precisa dos dados do filme (title, poster, year, externalId). Opções:
    - buscar no catalog via `GET /movies/{id}` (adicionar endpoint), ou
    - ranking-service busca no catalog (acoplamento). Melhor: battle-service busca detalhes do filme escolhido no catalog antes de notificar. Adicionar `CatalogClient.getMovie(Long id)`.
  - `rankingClient.registerVote(voteRequest)`

Adicionar endpoint no catalog: `GET /movies/{id}`.

### Controller
```java
@RestController @RequestMapping("/battles")
public class BattleController {
  @PostMapping public BattleStartResponse start() {...}
  @PostMapping("/{id}/vote") public ResponseEntity<Void> vote(@PathVariable UUID id, @RequestBody @Valid VoteRequest req) {...}
}
```

## 6. ranking-service

Dependências: web, data-jpa, postgresql, eureka-client, validation.

### Entidades
`RankedMovie`:
```java
@Entity @Table(name="ranked_movies")
public class RankedMovie {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="movie_id", unique=true) private Long movieId; // id interno catálogo
  @Column(name="external_id") private Long externalId; // id do TMDB (inteiro)
  @Column(nullable=false) private String title;
  @Column(name="poster_path") private String posterPath;
  @Column(name="release_year") private Integer releaseYear;
  @Column(name="badness_count") private int badnessCount = 0;
  @Column(name="last_vote_at") private LocalDateTime lastVoteAt;
}
```

`Vote`:
```java
@Entity @Table(name="votes")
public class Vote {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="movie_id") private Long movieId; // FK lógica para ranked_movies.id
  @Column(name="voted_at") private LocalDateTime votedAt;
  @PrePersist void pre(){ votedAt = LocalDateTime.now(); }
}
```

### Repository
```java
public interface RankedMovieRepository extends JpaRepository<RankedMovie, Long> {
  Optional<RankedMovie> findByMovieId(Long movieId);
  @Query("SELECT r FROM RankedMovie r ORDER BY r.badnessCount DESC, r.lastVoteAt DESC")
  List<RankedMovie> findTop10(Pageable pageable); // PageRequest.of(0,10)
}
```

### Service
```java
@Transactional
public void registerVote(VoteRequest req) {
  RankedMovie rm = repo.findByMovieId(req.getMovieId())
      .orElseGet(() -> new RankedMovie(req)); // popula dados
  rm.setBadnessCount(rm.getBadnessCount() + 1);
  rm.setLastVoteAt(LocalDateTime.now());
  rm = repo.save(rm);
  Vote v = new Vote();
  v.setMovieId(rm.getId());
  voteRepo.save(v);
}
```

### Controller
```java
@RestController @RequestMapping("/ranking")
public class RankingController {
  @PostMapping("/votes") public ResponseEntity<Void> vote(@RequestBody @Valid VoteRequest req) { service.registerVote(req); return ResponseEntity.status(201).build(); }
  @GetMapping("/top10") public List<RankedMovieDto> top10() { return service.top10(); }
}
```

Nota: ranking controller externo via gateway `/ranking/**` → interno `/ranking/**` (sem strip) ou com strip. Para consistência, usar controller base `/ranking` e gateway sem StripPrefix? Se gateway Path=/ranking/** e uri lb://ranking-service, o path chega completo `/ranking/top10`, que casa com controller. OK, sem StripPrefix. Mas para catalog/battle também poderia ser sem strip se controller usar prefixo completo. Escolha uma abordagem e mantenha. Recomendo: controllers usam prefixo completo (`/catalog/...`? não, serviço não sabe do gateway). 

Decisão final: controllers base sem prefixo de serviço (`/movies`, `/battles`, `/ranking`? ranking controller `/` com endpoints `/votes`, `/top10`). Gateway usa StripPrefix=1 para todos, mapeando:
- `/catalog/**` → catalog `/...`
- `/battles/**` → battle `/...`
- `/ranking/**` → ranking `/...`

Assim externo `/catalog/movies/search` → interno `/movies/search`. Atualizar Feign clients para paths internos (`/movies/random-pair`, `/movies/{id}`) e ranking (`/votes`). E ranking controller base `/` com `@PostMapping("/votes")`, `@GetMapping("/top10")`.

## 7. Validações e erros

- Usar `@Valid` + Bean Validation.
- `@RestControllerAdvice` em cada serviço retornando `ProblemDetail` (Spring 6) para erros.

## 8. Ordem de inicialização

1. discovery-service (8761)
2. catalog-service, ranking-service, battle-service (qualquer ordem, mas battle depende dos outros via Eureka)
3. gateway-service (8080)

## 9. Testes sugeridos

- Unitários: services com mocks (Mockito).
- Integração: `@SpringBootTest` + Testcontainers PostgreSQL (opcional).
- Smoke: curl via gateway.

## 10. Variáveis de ambiente

- `TMDB_API_KEY` (obrigatória no catalog-service)
- `SPRING_DATASOURCE_PASSWORD` (opcional, default postgres)

## 11. Exemplo de pom de serviço (catalog-service)

```xml
<project>
  <parent>
    <groupId>br.edu.moviebattle</groupId>
    <artifactId>movie-battle-parent</artifactId>
    <version>1.0.0</version>
  </parent>
  <artifactId>catalog-service</artifactId>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

## 12. .gitignore sugerido

```
target/
*.iml
.idea/
*.log
.env
```

## 13. Perguntas em aberto (respostas do grupo)

- Histórico de votos: SIM (tabela votes) — conforme enunciado.
- Sem login, sem Docker, Java 21, Spring Boot 3.3+.
- Idempotência no registro de filme: retorna existente.
- Top10 desempate por último voto desc.
- TMDB em pt-BR.
- Sorteio retorna dois filmes distintos (retry).
