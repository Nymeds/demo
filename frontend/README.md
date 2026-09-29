# Frontend — AcadOrganize

Interface do Sistema de Organização Acadêmica, em Vue 3 e Vite. O backend (Spring Boot) e a visão geral do projeto estão no [README da raiz](../README.md).

## Stack

- Vue 3 (`vue ^3.5.40`)
- Vite (`^8.2.0`) com `@vitejs/plugin-vue` (`^6.0.8`)
- Testes de lógica com `node:test` (sem framework de interface)

## Como rodar

```bash
cd frontend
npm ci              # instala as dependências
npm run dev         # servidor de desenvolvimento em http://localhost:5173
npm run build       # build de produção
npm test            # node --test tests/*.test.mjs
```

Se o `npm test` não funcionar no seu shell, use `node --test "tests/*.test.mjs"`.

## API e proxy

O Vite encaminha `/api` para o alvo definido em `VITE_API_PROXY_TARGET` (padrão `http://localhost:8080`, veja `vite.config.js`). Para outro backend, copie `.env.example` para `.env.local` (ignorado pelo Git), ajuste a variável e reinicie o `npm run dev`. Não versione segredos nem URLs de ambiente.

Para subir o backend localmente, consulte o README da raiz.

## Organização

```text
src/features/<feature>/   Uma pasta por funcionalidade:
                          tela que orquestra (XxxScreen.vue) + subcomponentes irmãos
                          + arquivos .js de lógica pura + .css irmão
src/components/ui/        Componentes compartilhados App*.vue (AppSelect, AppDatePicker...)
src/shared/               Código compartilhado (http, auth, date, format, a11y...)
tests/                    Testes de lógica (*.test.mjs)
```

- `src/shared/http/apiRequest.js` é o **único cliente HTTP**: adiciona o token Bearer, padroniza erros e renova a sessão; quando a renovação falha, encerra a sessão e emite o evento de sessão expirada. Não use `fetch` direto nas telas.
- A sessão fica em `src/shared/auth/session.js`.
- Mantenha regras e cálculos em `.js` puro, para testá-los sem montar componentes.

## Padrão visual

Disciplinas e Frequência são a referência de dimensões para as demais telas: cards de resumo, filtros, ordenação, botões de adicionar e campos de pesquisa devem manter as mesmas fontes, ícones, bordas e espaçamentos, com adaptação para telas pequenas (ver `AGENTS.md`).
