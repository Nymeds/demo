<script setup>
defineProps({ version: { type: String, default: '' } })
</script>

<template>
  <article class="legal-doc">
    <p class="legal-draft" role="note"><strong>Rascunho — revisar com a equipe antes da publicação</strong></p>
    <h3>Política de Privacidade</h3>
    <p class="legal-version">Versão: {{ version || 'indisponível' }}</p>

    <h4>1. Dados que coletamos</h4>
    <ul>
      <li>Conta e perfil: nome, e-mail, senha (armazenada apenas como hash), nome de usuário, telefone, data de nascimento, gênero e localização, quando você os informa, e foto de perfil.</li>
      <li>Dados acadêmicos que você cadastra: disciplinas e horários, atividades e provas, notas, faltas e frequência, eventos de calendário e preferências.</li>
      <li>Dados de sessão: tokens de renovação de login.</li>
      <li>Registro do aceite: data e versões dos Termos de Uso e da Política de Privacidade aceitos.</li>
      <li>Segurança: o sistema limita tentativas de login por endereço IP, mantendo essa contagem apenas em memória do servidor.</li>
    </ul>

    <h4>2. Para que usamos</h4>
    <p>Usamos os dados para autenticar você, exibir e calcular suas informações acadêmicas e proteger a conta contra tentativas abusivas.</p>

    <h4>3. Armazenamento no seu navegador</h4>
    <p>O navegador guarda seus tokens de acesso em localStorage (se você escolher manter conectado) ou sessionStorage. Não utilizamos cookies de rastreamento nem ferramentas de análise.</p>

    <h4>4. Compartilhamento</h4>
    <p>Não compartilhamos seus dados com terceiros.</p>

    <h4>5. Onde e por quanto tempo</h4>
    <p>Local de armazenamento e prazo de retenção: a definir. Ao excluir a conta, seus dados são removidos do sistema.</p>

    <h4>6. Seus direitos (LGPD)</h4>
    <p>Você pode acessar e corrigir seus dados em Perfil e Configurações e solicitar a exclusão da conta em Configurações.</p>

    <h4>7. Contato</h4>
    <p>Encarregado e canal de contato: a definir.</p>
  </article>
</template>
