const publicModes = { '/': 'login', '/login': 'login', '/cadastro': 'register', '/recuperar-senha': 'recovery' }
const sections = {
  dashboard: 'dashboard', disciplinas: 'disciplines', disciplines: 'disciplines',
  atividades: 'activities', activities: 'activities', provas: 'exams', exams: 'exams',
  frequencia: 'frequency', frequency: 'frequency', notas: 'grades', grades: 'grades',
  simulador: 'simulator', simulator: 'simulator', perfil: 'profile', profile: 'profile',
  configuracoes: 'settings', settings: 'settings', calendario: 'calendar', calendar: 'calendar',
}

export function currentRoute() {
  const path = (window.location.hash.startsWith('#/')
    ? window.location.hash.slice(1).split('?')[0]
    : window.location.pathname).replace(/\/+$/, '') || '/'
  const sectionKey = path.split('/').filter(Boolean).at(-1)
  return {
    publicMode: Object.hasOwn(publicModes, path) ? publicModes[path] : null,
    section: Object.hasOwn(sections, sectionKey) ? sections[sectionKey] : 'dashboard',
  }
}
