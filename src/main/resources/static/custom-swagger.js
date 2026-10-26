(function () {
  var STORAGE_KEY = 'swagger-ui-theme';

  function obtenerTemaInicial() {
    try {
      var guardado = localStorage.getItem(STORAGE_KEY);
      if (guardado === 'light' || guardado === 'dark') {
        return guardado;
      }
    } catch (e) {}
    return 'dark';
  }

  function aplicarTema(tema) {
    document.documentElement.setAttribute('data-theme', tema);
    try {
      localStorage.setItem(STORAGE_KEY, tema);
    } catch (e) {}
    actualizarBoton(tema);
  }

  function actualizarBoton(tema) {
    var btn = document.getElementById('swagger-theme-toggle');
    if (!btn) return;
    if (tema === 'dark') {
      btn.innerHTML = '☀️ Modo Claro';
      btn.setAttribute('title', 'Cambiar a tema claro');
    } else {
      btn.innerHTML = '🌙 Modo Oscuro';
      btn.setAttribute('title', 'Cambiar a tema oscuro');
    }
  }

  function crearBotonToggle() {
    if (document.getElementById('swagger-theme-toggle')) return;
    var btn = document.createElement('button');
    btn.id = 'swagger-theme-toggle';
    btn.type = 'button';
    btn.className = 'swagger-theme-toggle-btn';
    btn.addEventListener('click', function () {
      var actual = document.documentElement.getAttribute('data-theme') || 'dark';
      aplicarTema(actual === 'dark' ? 'light' : 'dark');
    });

    var topbarWrapper = document.querySelector('.swagger-ui .topbar .topbar-wrapper');
    if (topbarWrapper) {
      topbarWrapper.appendChild(btn);
    } else {
      document.body.appendChild(btn);
    }
    actualizarBoton(document.documentElement.getAttribute('data-theme') || 'dark');
  }

  // Aplica el tema antes del pintado para evitar parpadeo
  aplicarTema(obtenerTemaInicial());

  // Observa cuando React de Swagger UI renderiza el .topbar-wrapper
  var observer = new MutationObserver(function () {
    var topbarWrapper = document.querySelector('.swagger-ui .topbar .topbar-wrapper');
    var btn = document.getElementById('swagger-theme-toggle');
    if (topbarWrapper && (!btn || btn.parentElement !== topbarWrapper)) {
      if (btn) btn.remove();
      crearBotonToggle();
    } else if (!btn && document.body) {
      crearBotonToggle();
    }
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      crearBotonToggle();
      observer.observe(document.body, { childList: true, subtree: true });
    });
  } else {
    crearBotonToggle();
    observer.observe(document.body, { childList: true, subtree: true });
  }
})();
