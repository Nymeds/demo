<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

defineProps({ status: { type: Number, default: 401 } })
const emit = defineEmits(['login'])
const message = 'olha só pra vc hacker , criatura patética eu tenho nojo de você'
// Durações de um ciclo, medidas nos GIFs originais (soma dos tempos dos frames).
const durations = { arrive: 1820, glasses: 3710, backflip: 980, goodbye: 1750 }
const phase = ref('arrive')
const voice = ref(null)
const explosion = ref(null)
const blockedAudio = ref('')
const mediaFeedback = ref('')
const muted = ref(false)
const leaving = ref(false)
const heading = ref(null)
const showMessage = computed(() => ['talking', 'backflip', 'goodbye', 'done'].includes(phase.value))
let timer
let disposed = false
let flipFinished = false
let explosionFinished = false

function schedule(callback, milliseconds) {
  clearTimeout(timer)
  timer = setTimeout(() => { if (!disposed) callback() }, milliseconds)
}

async function playAudio(track) {
  if (disposed) return
  const element = track === 'voice' ? voice.value : explosion.value
  try {
    await element.play()
    if (!disposed) blockedAudio.value = ''
  } catch (error) {
    if (disposed) return
    if (error.name === 'NotAllowedError') blockedAudio.value = track
    else audioFailed(track)
  }
}

function startTalking() {
  phase.value = 'talking'
  playAudio('voice')
}

function startBackflip() {
  if (disposed || phase.value !== 'talking') return
  blockedAudio.value = ''
  phase.value = 'backflip'
  flipFinished = false
  explosionFinished = false
  playAudio('explosion')
  schedule(() => { flipFinished = true; finishBackflip() }, durations.backflip)
}

function finishBackflip() {
  if (disposed || phase.value !== 'backflip' || !flipFinished || !explosionFinished) return
  phase.value = 'goodbye'
  schedule(() => { phase.value = 'done' }, durations.goodbye)
}

function onExplosionEnded() {
  explosionFinished = true
  finishBackflip()
}

function audioFailed(track) {
  mediaFeedback.value = 'Não foi possível reproduzir um dos áudios. A animação continua.'
  blockedAudio.value = ''
  if (track === 'voice' && phase.value === 'talking') schedule(startBackflip, 5000)
  if (track === 'explosion' && phase.value === 'backflip') onExplosionEnded()
}

function stopMedia() {
  disposed = true
  clearTimeout(timer)
  voice.value?.pause()
  explosion.value?.pause()
}

function goToLogin() {
  if (leaving.value) return
  leaving.value = true
  stopMedia()
  emit('login')
}

onMounted(async () => {
  await nextTick()
  if (disposed) return
  heading.value?.focus()
  schedule(() => {
    phase.value = 'glasses'
    schedule(startTalking, durations.glasses)
  }, durations.arrive)
})
onBeforeUnmount(stopMedia)
</script>

<template>
  <main class="denied-page">
    <section class="denied-card" aria-labelledby="denied-title">
      <p class="denied-eyebrow">AcadOrganize · Área protegida</p>
      <span class="denied-code">{{ status }}</span>
      <h1 id="denied-title" ref="heading" tabindex="-1">Acesso negado</h1>
      <p class="denied-description">{{ status === 403
        ? 'Sua conta não tem permissão para acessar esta área.'
        : 'Entre com uma sessão válida para acessar esta área.' }}</p>

      <div class="buddy-stage" :data-phase="phase">
        <div class="buddy-message" aria-live="polite">
          <p v-if="showMessage">{{ message }}</p>
          <p v-else class="buddy-intro">O Buddy tem um recado para você…</p>
        </div>
        <div class="buddy-character">
          <img v-if="phase !== 'done'" :key="phase" :src="`/buddy/${phase}.gif`"
            :alt="`Buddy: ${ { arrive: 'chegando', glasses: 'colocando os óculos', talking: 'falando', backflip: 'dando um backflip', goodbye: 'indo embora' }[phase] }`">
          <p v-else class="buddy-finished" role="status">O Buddy foi embora. Você pode voltar ao login.</p>
        </div>
      </div>

      <p v-if="blockedAudio" class="denied-notice" role="status">O navegador precisa de um clique para liberar o áudio.</p>
      <p v-if="mediaFeedback" class="denied-notice" role="status">{{ mediaFeedback }}</p>
      <div class="denied-actions">
        <button v-if="blockedAudio" class="denied-primary" type="button" @click="playAudio(blockedAudio)">Continuar com áudio</button>
        <button class="denied-primary" type="button" :disabled="leaving" @click="goToLogin">{{ leaving ? 'Aguarde…' : 'Voltar ao login' }}</button>
        <button class="denied-secondary" type="button" :aria-pressed="muted" @click="muted = !muted">{{ muted ? 'Ativar som' : 'Silenciar' }}</button>
      </div>
    </section>
    <audio ref="voice" src="/buddy/voce-nao-tem-aura.mp3" preload="auto" :muted="muted" @ended="startBackflip" @error="audioFailed('voice')" />
    <audio ref="explosion" src="/buddy/rojao-super-estourado.mp3" preload="auto" :muted="muted" @ended="onExplosionEnded" @error="audioFailed('explosion')" />
  </main>
</template>

<style scoped>
.denied-page { display: grid; min-height: 100dvh; place-items: center; padding: 24px; background: radial-gradient(ellipse at top, #eee6ff, #f6f7fb 65%); color: #29213d; }
.denied-card { width: min(100%, 640px); padding: 32px; border: 1px solid #e4dcee; border-radius: 24px; background: #fff; box-shadow: 0 18px 60px #39245f14; text-align: center; }
.denied-eyebrow { margin: 0 0 18px; color: #745999; font-size: .8rem; font-weight: 700; letter-spacing: .08em; }
.denied-code { display: inline-block; padding: 6px 16px; border-radius: 999px; background: #f1eaff; color: #7141cc; font-size: .85rem; font-weight: 800; }
.denied-card h1 { margin: 12px 0; font-size: clamp(1.8rem, 5vw, 2.4rem); }
.denied-card h1:focus { outline: none; }
.denied-description { margin: 0; color: #716a7d; line-height: 1.6; }
.buddy-stage { margin: 26px 0 16px; }
.buddy-message { display: grid; min-height: 96px; place-items: center; padding: 16px 22px; border: 1px solid #dfd1f3; border-radius: 18px; background: #faf7ff; }
.buddy-message p { margin: 0; line-height: 1.6; font-size: 1rem; overflow-wrap: anywhere; }
.buddy-intro { color: #80728e; }
.buddy-character { display: grid; height: 230px; place-items: center; padding-top: 14px; }
.buddy-character img { width: auto; height: auto; max-width: 100%; max-height: 210px; transform: scale(1.25); transform-origin: center; }
.buddy-finished, .denied-notice { color: #716a7d; line-height: 1.5; font-size: .85rem; }
.denied-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; }
.denied-actions button { min-height: 44px; padding: 12px 20px; border-radius: 12px; font: inherit; font-size: .9rem; font-weight: 700; cursor: pointer; }
.denied-primary { border: 1px solid #7141cc; background: #7141cc; color: #fff; }
.denied-primary:hover { background: #5b30ad; }
.denied-actions button:disabled { opacity: .65; cursor: wait; }
.denied-secondary { border: 1px solid #dfd1f3; background: #fff; color: #7141cc; }
.denied-actions button:focus-visible { outline: 3px solid #ba9be8; outline-offset: 3px; }
@media (max-width: 520px) { .denied-page { padding: 14px; } .denied-card { padding: 24px 16px; } .buddy-message { padding: 14px; } }
</style>
