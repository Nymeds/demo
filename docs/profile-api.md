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
  "location": "Goiânia - GO"
}
```

`name` e `email` são obrigatórios. Os demais campos são opcionais. `username` e `email` são únicos; e-mail e nome de usuário são armazenados em letras minúsculas.

Valores aceitos para `gender`: `FEMALE`, `MALE`, `NON_BINARY`, `OTHER` e `PREFER_NOT_TO_SAY`.

A resposta não expõe o hash da senha nem os bytes da foto. Quando há uma foto, `hasProfilePhoto` é `true` e `profilePhotoUrl` contém o endpoint autenticado da imagem.

## Foto de perfil

- `PUT /api/v1/users/me/profile-photo`: recebe `multipart/form-data`, campo `file`.
- `GET /api/v1/users/me/profile-photo`: devolve os bytes com o tipo correto da imagem.
- `DELETE /api/v1/users/me/profile-photo`: remove a foto e responde `204 No Content`.

São aceitos PNG e JPG de até 2 MB. O formato é identificado pela assinatura real do arquivo, sem confiar apenas no cabeçalho enviado pelo cliente.

## Decisões de privacidade

- CPF não foi incluído porque não é necessário para a finalidade de organização acadêmica e aumentaria o risco relacionado ao tratamento de dados pessoais.
- Telefone, data de nascimento, gênero e localização são opcionais e podem ser apagados enviando `null` ou texto vazio.
- A foto fica em uma tabela separada para não ser carregada junto com o usuário em autenticações e consultas que não precisam da imagem.
