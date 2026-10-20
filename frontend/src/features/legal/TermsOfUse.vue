<script setup>
defineProps({ version: { type: String, default: '' } })
</script>

<template>
  <article class="legal-doc">
    <p class="legal-draft" role="note"><strong>Rascunho — revisar com a equipe antes da publicação</strong></p>
    <h3>Termos de Uso</h3>
    <p class="legal-version">Versão: {{ version || 'indisponível' }}</p>

    <h4>1. Sobre o sistema</h4>
    <p>O AcadOrganize é um sistema de organização acadêmica que ajuda estudantes a registrar disciplinas, atividades, provas, notas, frequência e compromissos de calendário.</p>

    <h4>2. Conta e responsabilidades</h4>
    <p>Você é responsável pelas informações que cadastra e por manter sua senha em sigilo. Não use a opção de manter conectado em computadores compartilhados.</p>

    <h4>3. Uso adequado</h4>
    <p>O sistema deve ser usado apenas para fins acadêmicos pessoais. Não é permitido tentar acessar dados de outras pessoas, sobrecarregar o serviço ou burlar mecanismos de segurança.</p>

    <h4>4. Cálculos e simulações</h4>
    <p>Médias, situação de frequência e simulações de notas são apoios de organização e podem divergir das regras oficiais da sua instituição. Confirme sempre com a instituição.</p>

    <h4>5. Exclusão da conta</h4>
    <p>Você pode excluir sua conta em Configurações. A exclusão remove seus dados do sistema.</p>

    <h4>6. Alterações destes termos</h4>
    <p>Os termos podem ser atualizados. Quando isso ocorrer, será pedido que você leia e aceite a versão atual.</p>

    <h4>7. Contato</h4>
    <p>Canal de contato: a definir.</p>
  </article>
</template>
