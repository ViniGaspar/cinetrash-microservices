# Regras de Negócio — Movie Battle

Documento de especificação das regras de negócio do sistema Movie Battle.
Complementa `README.md` e `docs/implementation-guide.md`.

## 1. Catálogo de filmes

### RN-01 — Busca externa por nome
O usuário informa um nome de filme; o sistema consulta o TMDB (`/search/movie`) com `language=pt-BR`
e retorna a lista de filmes encontrados (id, título, poster, ano). O usuário escolhe o `externalId`
(id do TMDB) do filme desejado.

### RN-02 — Registro idempotente
Ao registrar um filme (`POST /movies` com `externalId`):
- Se o `externalId` já existir no catálogo, retorna o filme existente (HTTP 200), sem criar duplicata.
- Caso contrário, busca os detalhes no TMDB (`/movie/{id}`), persiste e retorna (HTTP 201).

### RN-03 — Dados persistidos do filme
Persistir: `externalId` (Long, id TMDB), `title`, `posterPath` (URL completa do poster),
`releaseYear` (ano extraído de `release_date`), `createdAt`.

### RN-04 — Poster URL
`posterPath` retornado deve ser a URL completa: `https://image.tmdb.org/t/p/w500{poster_path}`.
Se `poster_path` for nulo, armazenar `null`.

### RN-05 — Ano de lançamento
Extrair o ano (4 dígitos) de `release_date` (`YYYY-MM-DD`). Se ausente, armazenar `null`.

## 2. Sorteio de filmes (batalha)

### RN-06 — Catálogo mínimo
Para iniciar uma batalha, o catálogo deve ter **pelo menos 2 filmes** cadastrados.
Caso contrário, retornar HTTP 400 com mensagem explicativa.

### RN-07 — Dois filmes distintos
O sorteio deve retornar **dois filmes distintos**. Se o primeiro sorteio repetir o mesmo filme,
repetir a busca até obter dois distintos (máximo de 10 tentativas). Se não for possível,
retornar HTTP 500 (erro inesperado).

### RN-08 — Aleatoriedade
O sorteio deve ser aleatório no banco (`ORDER BY RANDOM()` no PostgreSQL), sem viés de cadastro.

### RN-09 — Persistência da batalha
Cada batalha iniciada deve ser persistida com `id` (UUID), `movieAId`, `movieBId`, `createdAt`.
Os dados do filme não são duplicados na batalha (apenas referências ao id interno do catálogo).

## 3. Voto

### RN-10 — Voto válido
O usuário vota informando `chosenMovieId` (id interno do catálogo). O voto só é aceito se
`chosenMovieId` for **um dos dois filmes** da batalha (`movieAId` ou `movieBId`).
Caso contrário, HTTP 400.

### RN-11 — Voto único por batalha
Uma batalha não pode ser votada mais de uma vez. Se já votada, retornar HTTP 409 Conflict.

### RN-12 — Notificação ao ranking
Após voto válido, o battle-service notifica o ranking-service com os dados do filme escolhido
(`movieId` interno, `externalId`, `title`, `posterPath`, `releaseYear`).

### RN-13 — Detalhes do filme votado
O battle-service busca os detalhes do filme escolhido no catalog-service (`GET /movies/{id}`)
antes de notificar o ranking, garantindo dados consistentes.

## 4. Ranking

### RN-14 — Upsert de filme no ranking
Ao receber um voto, o ranking-service faz upsert em `ranked_movies` por `movie_id` (id interno
do catálogo): se não existir, cria com os dados recebidos e `badness_count = 0`; se existir,
atualiza `title`, `posterPath`, `releaseYear`, `externalId` (caso tenham mudado).

### RN-15 — Incremento de ruindade
A cada voto, incrementar `badness_count` em 1 e atualizar `last_vote_at` para o momento do voto.

### RN-16 — Histórico de votos
Inserir um registro em `votes` (`id`, `movie_id` → FK lógica para `ranked_movies.id`, `voted_at`)
para cada voto recebido, permitindo auditoria e recontagem.

### RN-17 — Transação
Upsert + incremento + insert de voto devem ocorrer em uma única transação (`@Transactional`).
Se qualquer passo falhar, rollback total.

### RN-18 — Top 10 piores
`GET /ranking/top10` retorna os 10 filmes com maior `badness_count`, ordenados por:
1. `badness_count` DESC
2. `last_vote_at` DESC (desempate: voto mais recente primeiro)

### RN-19 — Limite
Retornar no máximo 10 registros. Se houver menos de 10 filmes votados, retornar todos.

## 5. Comunicação e infraestrutura

### RN-20 — Discovery
Todos os serviços (exceto Eureka Server) se registram no Eureka. O gateway resolve serviços
por nome (`lb://service-name`).

### RN-21 — Gateway único
O cliente acessa apenas a porta 8080 (gateway). Rotas:
- `/catalog/**` → catalog-service
- `/battles/**` → battle-service
- `/ranking/**` → ranking-service

### RN-22 — Comunicação interna via Feign
battle-service → catalog-service e ranking-service via Feign (resolvido por Eureka).
catalog-service → TMDB via Feign (URL externa).

### RN-23 — Sem autenticação
Acesso anônimo; não há login nem controle de usuário.

### RN-24 — Idempotência de rede
Chamadas Feign podem ser reenviadas em caso de falha transitória; o ranking-service deve
tolerar voto duplicado? (A definir pelo grupo — ver perguntas em aberto.)

## 6. Tratamento de erros

### RN-25 — Erros padronizados
Erros retornados em formato `ProblemDetail` (RFC 7807) com `title`, `status`, `detail`.

### RN-26 — Códigos HTTP
- 200 OK: sucesso de leitura/voto
- 201 Created: criação de filme/voto
- 400 Bad Request: validação, catálogo insuficiente, voto inválido
- 404 Not Found: batalha/filme inexistente
- 409 Conflict: batalha já votada, (opcional) filme duplicado
- 500 Internal Server Error: falha inesperada no sorteio/comunicação

## 7. Perguntas em aberto

1. Remover/editar filme do catálogo?
2. Batalha persistida ou em memória?
3. Versionamento de API (`/v1/...`)?
4. Swagger/OpenAPI por serviço?
5. Idempotência de voto em caso de retry (evitar duplo incremento)?
6. Ranking deve exibir total global de votos?
7. Paginação na busca TMDB (hoje retorna primeira página)?
