<script setup>
import { onBeforeUnmount, ref } from 'vue'

const audio = ref(null)
const showSurprise = ref(false)
const particles = ref([])
const currentSlide = ref(0)

let emojiInterval
let slideshowInterval

const emojis = ['🎉', '💥', '🔥', '✨', '🚀', '🤯', '🦍', '💜', '🌟', '⚡', '🎓', '📚']
const slideshowImages = ['/outro.jpg', '/_image.webp', '/lobisome.jpg']

function randomBetween(min, max) {
  return Math.random() * (max - min) + min
}

function createParticles() {
  particles.value = Array.from({ length: 72 }, (_, index) => ({
    id: `${Date.now()}-${index}`,
    emoji: emojis[Math.floor(Math.random() * emojis.length)],
    x: randomBetween(4, 96),
    y: randomBetween(3, 90),
    rotation: randomBetween(-180, 180),
    size: randomBetween(1.4, 3.2),
    delay: randomBetween(0, 0.45),
    duration: randomBetween(1.2, 2.3),
  }))
}

function startEffects() {
  window.clearInterval(emojiInterval)
  window.clearInterval(slideshowInterval)

  currentSlide.value = 0
  createParticles()
  emojiInterval = window.setInterval(createParticles, 900)
  slideshowInterval = window.setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % slideshowImages.length
  }, 2800)
}

function stopSurprise() {
  window.clearInterval(emojiInterval)
  window.clearInterval(slideshowInterval)
  showSurprise.value = false
  particles.value = []
}

function triggerSurprise() {
  startEffects()

  showSurprise.value = true

  if (audio.value) {
    audio.value.volume = 1
    audio.value.currentTime = 0
    audio.value.play().catch(() => {})
  }
}

onBeforeUnmount(stopSurprise)
</script>

<template>
  <button class="surprise-button" type="button" aria-label="Ativar surpresa" @click="triggerSurprise">?</button>

  <Teleport to="body">
    <div v-if="showSurprise" class="surprise-overlay" aria-live="polite">
      <Transition name="surprise-slide" mode="out-in">
        <img
          :key="slideshowImages[currentSlide]"
          class="surprise-slide"
          :src="slideshowImages[currentSlide]"
          alt="Imagem do efeito surpresa"
        >
      </Transition>
      <img class="surprise-gif" src="/5w78gh9f9mie1.gif" alt="Gorila roxo animado">
      <span
        v-for="particle in particles"
        :key="particle.id"
        class="emoji-particle"
        :style="{
          left: `${particle.x}%`,
          top: `${particle.y}%`,
          '--rotation': `${particle.rotation}deg`,
          '--size': `${particle.size}rem`,
          '--delay': `${particle.delay}s`,
          '--duration': `${particle.duration}s`,
        }"
      >{{ particle.emoji }}</span>
    </div>
  </Teleport>

  <audio ref="audio" preload="auto" src="/animals-auuuuuuuuuu.mp3" @ended="stopSurprise" />
</template>

<style scoped>
.surprise-button {
  align-items: center;
  background: #df3636;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 6px 16px rgba(125, 22, 22, .28);
  color: #fff;
  display: flex;
  font-size: 1.15rem;
  font-weight: 800;
  height: 38px;
  justify-content: center;
  position: fixed;
  right: 20px;
  top: 20px;
  transition: transform .2s, background .2s;
  width: 38px;
  z-index: 20;
}

.surprise-button:hover { background: #be2424; transform: scale(1.08) rotate(-8deg); }
.surprise-button:focus-visible { outline: 3px solid #f7a1a1; outline-offset: 3px; }

.surprise-overlay {
  animation: color-flash .45s linear infinite alternate;
  background: rgba(30, 13, 47, .78);
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  position: fixed;
  z-index: 100;
}

.surprise-slide {
  animation: image-pulse .8s ease-in-out infinite alternate;
  background: #09050d;
  border: 7px solid #fff;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .7), 0 0 0 8px rgba(208, 104, 246, .75);
  display: block;
  filter: contrast(1.12) saturate(1.2);
  height: auto;
  left: 50%;
  max-height: min(62vh, 560px);
  max-width: min(78vw, 460px);
  object-fit: contain;
  opacity: 1;
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: auto;
  z-index: 8;
}

.surprise-slide-enter-active,
.surprise-slide-leave-active { transition: opacity .18s ease, transform .18s ease; }
.surprise-slide-enter-from { opacity: 0; transform: translate(-50%, -50%) scale(.72) rotate(-5deg); }
.surprise-slide-leave-to { opacity: 0; transform: translate(-50%, -50%) scale(1.08) rotate(5deg); }

.surprise-gif {
  animation: gif-dance .65s ease-in-out infinite alternate;
  bottom: 4%;
  max-height: 230px;
  max-width: min(42vw, 220px);
  object-fit: contain;
  position: absolute;
  right: 4%;
  transform: rotate(-4deg);
  z-index: 6;
}

.emoji-particle {
  animation: explode var(--duration) cubic-bezier(.12, .8, .22, 1) var(--delay) both;
  font-size: var(--size);
  line-height: 1;
  position: absolute;
  transform: translate(-50%, -50%);
  user-select: none;
  z-index: 10;
}

@keyframes color-flash {
  from { background: rgba(43, 11, 64, .72); }
  to { background: rgba(7, 45, 62, .72); }
}

@keyframes image-pulse {
  from { filter: contrast(1.25) saturate(1.35) hue-rotate(0deg); }
  to { filter: contrast(1.5) saturate(1.8) hue-rotate(18deg); }
}

@keyframes gif-dance {
  from { opacity: .9; transform: rotate(-8deg) scale(.92); }
  to { opacity: 1; transform: rotate(2deg) scale(1.04); }
}

@keyframes explode {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(.1) rotate(0); }
  18% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -50%) translateY(-180px) scale(1.35) rotate(var(--rotation)); }
}

@media (max-width: 600px) {
  .surprise-button { right: 14px; top: 14px; }
  .surprise-slide { max-height: 55vh; max-width: 84vw; }
  .surprise-gif { bottom: 2%; max-height: 155px; max-width: 38vw; right: 2%; }
}
</style>
