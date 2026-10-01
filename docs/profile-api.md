# API de perfil

Todos os endpoints exigem `Authorization: Bearer <token>` e atuam somente sobre o usuário autenticado.

## Dados do perfil

### Consultar

`GET /api/v1/users/me`

### Atualizar

`PUT /api/v1/users/me`

```json
{
  "name": "Gabriel Silva",
  "email": "gabriel.silva@example.com",
  "username": "gabrielsilva",
  "phone": "(62) 99999-9999",
  "birthDate": "2005-03-18",
  "gender": "PREFER_NOT_TO_SAY",
  "location": "Goiânia - GO",
  "currentPassword": "senha-atual"
}
```

`name` e `email` são obrigatórios. Os demais campos são opcionais. `username` e `email` são únicos; e-mail e nome de usuário são armazenados em letras minúsculas.

`currentPassword` (até 72 caracteres) só é obrigatório quando o `email` enviado difere do e-mail atual, porque o e-mail é o login. Se o e-mail não mudar, o campo é ignorado. A confirmação usa a mesma regra das configurações e o mesmo limite de tentativas por usuário (excedido, a resposta é `429`).

Valores aceitos para `gender`: `FEMALE`, `MALE`, `NON_BINARY`, `OTHER` e `PREFER_NOT_TO_SAY`.

A resposta não expõe o hash da senha nem os bytes da foto. Quando há uma foto, `hasProfilePhoto` é `true` e `profilePhotoUrl` contém o endpoint autenticado da imagem.

#### Troca de e-mail e sessão

Ao trocar o e-mail, todas as sessões anteriores caem (access tokens antigos e os cookies de sessão dos outros navegadores deixam de valer) e a resposta traz o objeto `session` com um novo access token (`accessToken`, `tokenType`, `expiresIn`). O navegador que fez a troca recebe também um cookie de sessão novo (`Set-Cookie`), com a mesma escolha de "Lembrar de mim". O cliente deve passar a usar o token. Sem troca de e-mail, `session` não aparece na resposta. Na tela de perfil, o frontend só envia `currentPassword` quando o e-mail foi alterado e salva a `session` recebida.

#### Erros

| Status | Quando | Mensagem |
| --- | --- | --- |
| `400` | E-mail alterado sem `currentPassword` | `Informe sua senha atual para confirmar.` |
| `400` | `currentPassword` incorreto | `A senha atual está incorreta.` |
| `400` | Campo inválido (Bean Validation) | Mensagem do campo, por exemplo `Informe um e-mail válido.` |
| `409` | E-mail já usado por outra conta | `Já existe um usuário com este e-mail.` |
| `409` | Nome de usuário já usado | `Este nome de usuário já está em uso.` |
| `409` | Conflito detectado ao gravar | `O e-mail ou nome de usuário informado já está em uso.` |
| `429` | Muitas confirmações de senha incorretas | Limite por usuário (5 falhas a cada 15 minutos por padrão) |

### Somente leitura em configurações

`GET /api/v1/settings/profile` devolve os dados do perfil apenas para consulta; para alterar o perfil use `PUT /api/v1/users/me`.

## Foto de perfil

- `PUT /api/v1/users/me/profile-photo`: recebe `multipart/form-data`, campo `file`.
- `GET /api/v1/users/me/profile-photo`: devolve os bytes com o tipo correto da imagem.
- `DELETE /api/v1/users/me/profile-photo`: remove a foto e responde `204 No Content`.

São aceitos PNG e JPG de até 2 MB. O formato é identificado pela assinatura real do arquivo, sem confiar apenas no cabeçalho enviado pelo cliente. O servidor regrava a imagem como JPEG com no máximo 512 px no maior lado; `GET` devolve `image/jpeg`.

## Decisões de privacidade

- CPF não foi incluído porque não é necessário para a finalidade de organização acadêmica e aumentaria o risco relacionado ao tratamento de dados pessoais.
- Telefone, data de nascimento, gênero e localização são opcionais. Telefone e localização são apagados enviando `null` ou texto vazio (ou só espaços); data de nascimento e gênero são apagados enviando `null`.
- A foto fica em uma tabela separada para não ser carregada junto com o usuário em autenticações e consultas que não precisam da imagem.
