# DocFlow API

API REST de gestão de arquivos com autenticação JWT, controle de acesso por papéis e
upload assíncrono para armazenamento em object storage (AWS S3, emulado localmente
via LocalStack).

Projeto desenvolvido como estudo aprofundado do ecossistema Spring, com foco em
arquitetura em camadas, segurança e decisões de design documentadas.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation) |
| Banco de dados | PostgreSQL |
| Object storage | AWS SDK v2 (S3) — LocalStack em desenvolvimento |
| Autenticação | JWT (jjwt 0.12.6) + BCrypt |
| Testes | JUnit, Mockito, AssertJ |
| Build | Maven |
| Infra local | Docker Compose |

---

## Funcionalidades

- **Autenticação e autorização** — cadastro e login com JWT stateless, papéis `ADMIN`
  e `USER`, filtro de autenticação na cadeia do Spring Security.
- **Upload assíncrono** — o envio responde imediatamente com status `PENDING`; o
  upload ao S3 e o cálculo do hash de integridade ocorrem em segundo plano via `@Async`,
  atualizando o registro para `PROCESSED` ao concluir.
- **Gestão de arquivos** — listagem e consulta restritas ao dono do recurso, com
  verificação de propriedade na camada de serviço.
- **Gestão de usuários** — CRUD com atualização parcial, troca de senha com confirmação
  da senha atual, alteração de papel restrita a ADMIN, e desativação lógica (soft delete).
- **Tratamento global de erros** — `@RestControllerAdvice` traduz exceptions de domínio
  em respostas HTTP padronizadas.

---

## Como rodar

### Pré-requisitos

- Java 17 ou superior
- Docker e Docker Compose
- Maven (ou o wrapper `./mvnw` incluído)

### 1. Subir a infraestrutura

```bash
docker compose up -d
```

Sobe o PostgreSQL e o LocalStack. O bucket do S3 é criado automaticamente na
inicialização da aplicação (`S3BucketInitializer`), de forma idempotente.

### 2. Configurar as variáveis de ambiente

| Variável | Descrição |
|---|---|
| `JWT_SECRET` | Chave de assinatura dos tokens. Qualquer string longa e aleatória. |

```bash
export JWT_SECRET="sua-chave-secreta-aqui"
```

> No IntelliJ, defina a variável na Run Configuration da aplicação.

### 3. Executar

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Rodar os testes

```bash
./mvnw test
```

---

## Endpoints

### Autenticação

| Método | Rota | Acesso | Sucesso |
|---|---|---|---|
| `POST` | `/auth/register` | Público | `201` |
| `POST` | `/auth/login` | Público | `200` |

### Arquivos

| Método | Rota | Acesso | Sucesso |
|---|---|---|---|
| `POST` | `/files` | Autenticado | `202` |
| `GET` | `/files` | Autenticado | `200` |
| `GET` | `/files/{id}` | Dono | `200` |

### Usuários

| Método | Rota | Acesso | Sucesso |
|---|---|---|---|
| `GET` | `/users` | ADMIN | `200` |
| `GET` | `/users/{id}` | Dono ou ADMIN | `200` |
| `PATCH` | `/users/{id}` | Dono ou ADMIN | `200` |
| `PATCH` | `/users/{id}/password` | Dono | `204` |
| `PATCH` | `/users/{id}/role` | ADMIN | `200` |
| `DELETE` | `/users/me` | Dono | `204` |
| `DELETE` | `/users/{id}` | ADMIN | `204` |

### Status de erro

| Código | Situação |
|---|---|
| `400` | Falha de validação do corpo da requisição |
| `401` | Token ausente ou inválido |
| `403` | Autenticado, sem permissão para a operação |
| `404` | Recurso inexistente — ou existente, porém inacessível (ver decisões técnicas) |
| `409` | Conflito com o estado atual (email já cadastrado, regra de negócio violada) |

---

## Exemplo de uso

**Cadastro e login:**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Gabriel","email":"gabriel@exemplo.com","password":"senha1234"}'

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"gabriel@exemplo.com","password":"senha1234"}'
```

**Upload de arquivo:**

```bash
curl -X POST http://localhost:8080/files \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@documento.pdf"
```

Resposta `202 Accepted`:

```json
{
  "id": 1,
  "name": "documento.pdf",
  "status": "PENDING"
}
```

Consultando o mesmo recurso após o processamento, o status passa a `PROCESSED`
e o `fileHash` é preenchido.

---

## Arquitetura

```
controller  →  recebe HTTP, valida o DTO, extrai o usuário autenticado
service     →  regras de negócio, autorização de recurso, transações
repository  →  acesso a dados (Spring Data JPA)
entity      →  modelo de domínio persistido
dto         →  contratos de entrada e saída (records)
```

O controller não contém lógica de negócio e o service não conhece HTTP. DTOs isolam
o modelo de domínio do contrato público da API — a entidade `User` nunca é serializada
diretamente, o que torna estruturalmente impossível vazar o hash da senha em uma resposta.

---

## Testes

A suíte cobre a camada de serviço com testes unitários (JUnit + Mockito), isolando as
regras de negócio do banco de dados.

Os testes verificam não apenas o resultado das operações, mas também os efeitos
colaterais que **não** devem ocorrer: que a senha permanece inalterada quando a
confirmação falha, que o encoder não é invocado em caminhos de erro, e que consultas
desnecessárias não são disparadas.

```bash
./mvnw test
```

---

## Decisões técnicas

Esta seção registra as escolhas de arquitetura do projeto e o raciocínio por trás
de cada uma. Mais do que *o que* foi implementado, o objetivo é documentar *por quê*.

### Upload assíncrono responde 202, não 201

O endpoint `POST /files` grava o registro com status `PENDING` e devolve imediatamente,
delegando o upload ao S3 para processamento em segundo plano com `@Async`.

O status `201 Created` afirma que o recurso está criado e disponível no `Location`
retornado — o que seria mentira aqui, já que o arquivo ainda não subiu. `202 Accepted`
significa exatamente "a requisição foi aceita e será processada", que é o contrato real.
O cliente acompanha a conclusão consultando o status do recurso.

### `@Async` exige invocação por outro bean

O processamento assíncrono vive em um bean separado (`FileProcessingService`), e não
em um método privado do mesmo service que recebe o upload.

O Spring implementa `@Async` via proxy: o bean injetado não é a sua classe, é um objeto
que a envolve e intercepta as chamadas. Uma chamada interna (`this.processar()`) não
passa pelo proxy — e a anotação é silenciosamente ignorada, executando de forma síncrona
sem nenhum erro. Separar em outro bean garante que a chamada atravesse o proxy.

O mesmo raciocínio vale para `@Transactional` e `@PreAuthorize`.

### Usuário usa soft delete

`DELETE /users/{id}` marca o registro como inativo em vez de removê-lo.

A entidade `User` é referenciada por `File`: apagar a linha quebraria o histórico de
quem subiu cada arquivo, inviabilizando qualquer auditoria. Além disso, deleção é
irreversível e um clique errado não teria volta.

O soft delete impõe três consequências que o código respeita: o login recusa usuários
inativos (via `UserDetails.isEnabled()`), a listagem filtra por `active = true`, e o
email permanece ocupado na tabela — um novo cadastro com o mesmo endereço retorna 409.

### Acesso negado responde 404, não 403

Quando um usuário comum solicita o recurso de outro, a API responde `404 Not Found`,
o mesmo status de um id inexistente.

Com `403`, a API vaza informação: variando o id na URL, um atacante distingue recursos
existentes (403) de inexistentes (404) e enumera os ids válidos do sistema. Responder
404 nos dois casos elimina o canal.

A escolha tem custo — um usuário legítimo não distingue "não existe" de "não é seu" —
e é deliberada: esse custo é menor que o da enumeração.

### Senha e role têm rotas próprias

Alterar senha e alterar role não passam pelo `PATCH /users/{id}` genérico. Cada uma
tem seu endpoint: `PATCH /users/{id}/password` e `PATCH /users/{id}/role`.

Se `role` fosse aceita no DTO de atualização, qualquer usuário poderia se promover a
ADMIN editando o próprio cadastro — a autorização passaria, porque o id é dele mesmo.
Trata-se de *mass assignment*, e o DTO é o que o impede: campo que não existe na classe
é descartado na desserialização, nunca chega ao service.

A senha segue a mesma lógica por outro motivo: trocá-la exige comprovar a senha atual,
e um PATCH genérico não tem onde exigir isso. O endpoint dedicado recebe
`currentPassword` e `newPassword`, e nenhum ADMIN entra nele — ele não conhece a senha
atual de ninguém.

### Encerramento de conta é separado da desativação administrativa

`DELETE /users/me` (o próprio usuário, confirmando a senha) e `DELETE /users/{id}`
(ADMIN desativando terceiro) são rotas distintas.

São operações com autorização, risco e confirmação diferentes. Unificá-las significaria
um único método cheio de ramificações condicionais — e a regra "um ADMIN não se
autodeleta" existiria para compensar a mistura, em vez de resolver o problema.

### A camada de serviço não conhece HTTP

O service lança exceptions de domínio (`ResourceNotFoundException`, `BusinessException`)
e um `@RestControllerAdvice` as traduz em status HTTP.

Com `ResponseStatusException` dentro do service, a regra de negócio passaria a depender
da camada web: chamado por um job agendado ou por um consumidor de fila, "404" não
significaria nada. A separação mantém o service reutilizável e concentra o mapeamento
de erros em um ponto único.

### Usuário autenticado chega como parâmetro

Os métodos do service recebem o usuário autenticado explicitamente, extraído no
controller via `@AuthenticationPrincipal`.

Ler do `SecurityContextHolder` dentro do service criaria dependência de estado global
em ThreadLocal — invisível na assinatura do método, custosa de montar em testes
unitários e vazia em contexto assíncrono, onde a thread é outra. O parâmetro explícito
funciona em qualquer contexto e dispensa setup nos testes.

### Hash de senha e hash de integridade usam algoritmos diferentes

Senhas usam BCrypt; a verificação de integridade dos arquivos usa SHA-256.

São objetivos opostos. BCrypt é deliberadamente lento e salgado, para tornar ataques
de força bruta inviáveis. SHA-256 é rápido e determinístico, que é o necessário para
comparar se dois arquivos têm conteúdo idêntico. Usar SHA-256 em senha a deixaria
vulnerável a rainbow tables; usar BCrypt em arquivo seria lento e inútil, já que o
salt impede a comparação.

### Atualização parcial usa PATCH

`PATCH /users/{id}` aceita campos opcionais; os ausentes permanecem inalterados.

`PUT` significa substituição integral do recurso: um corpo contendo apenas `name`
deveria, pela especificação, apagar o email. Como a operação é parcial por natureza,
o verbo correto é PATCH.

A validação acompanha essa semântica — `@Size` e `@Email`, que ignoram `null` por
contrato, em vez de `@NotNull` ou `@NotBlank`, que rejeitariam campos simplesmente
não enviados.

---

## Próximos passos

- Testes de integração da camada web com `@WebMvcTest`, cobrindo as regras de
  `@PreAuthorize` (que não são exercidas por testes unitários, por dependerem de proxy)
- Documentação interativa com OpenAPI / Swagger
- Deploy em ambiente cloud

---

## Autor

Gabriel Anselmo — [GitHub](https://github.com/) · [LinkedIn](https://linkedin.com/)
