// Shared navbar interactions: language and profile dropdowns
(function () {
  const byId = (id) => document.getElementById(id);
  const languageMenu = byId('languageMenu'); // legacy (top-level)
  const languageDropdown = byId('languageDropdown'); // legacy (top-level)
  const currentLanguage = byId('currentLanguage'); // legacy label
  const currentLanguageFlag = byId('currentLanguageFlag'); // legacy flag
  const languageSubmenu = byId('languageSubmenu'); // new submenu container
  const currentLanguageSmall = byId('currentLanguageSmall'); // new small label
  const profileMenu = byId('profileMenu');
  const profileDropdown = byId('profileDropdown');
  const languagesItem = byId('languagesItem');

  // Helper to toggle dropdown and rotate chevron
  function toggleWithChevron(container, dropdown) {
    if (!container || !dropdown) return;
    dropdown.classList.toggle('show');
    const chev = container.querySelector('.fa-chevron-down');
    if (chev) chev.style.transform = dropdown.classList.contains('show') ? 'rotate(180deg)' : 'rotate(0deg)';
  }

  // Languages submenu (new inside Profile dropdown)
  if (languageSubmenu) {
    const languageItems = languageSubmenu.querySelectorAll('.language-item');
    const removeChecks = () => {
      languageItems.forEach((i) => {
        const chk = i.querySelector('.lang-check');
        if (chk) chk.remove();
      });
    };
    const applySelection = (item, { persist = true, reload = false } = {}) => {
      languageItems.forEach((i) => i.classList.remove('active'));
      item.classList.add('active');
      const nameEl = item.querySelector('.font-medium');
      const flagEl = item.querySelector('span');
      if (currentLanguageSmall && nameEl) currentLanguageSmall.textContent = nameEl.textContent;
      if (currentLanguage && nameEl) currentLanguage.textContent = nameEl.textContent; // keep legacy in sync if present
      if (currentLanguageFlag && flagEl) currentLanguageFlag.textContent = flagEl.textContent; // keep legacy in sync
      // Inject a checkmark on the selected item
      removeChecks();
      try {
        const check = document.createElement('i');
        check.className = 'lang-check fas fa-check text-blue-600 text-xs ml-auto';
        item.appendChild(check);
      } catch (_) {}
      const code = item.getAttribute('data-lang');
      if (persist) {
        try { localStorage.setItem('selectedLanguage', code); } catch (_) {}
        try {
          document.cookie = `selectedLanguage=${encodeURIComponent(code)}; path=/; max-age=31536000; SameSite=Lax`;
        } catch (_) {}
      }
      if (reload) {
        // Small delay to allow UI to update before reload
        setTimeout(() => { window.location.reload(); }, 50);
      }
    };

    languageItems.forEach((item) => {
      item.addEventListener('click', function (e) {
        e.preventDefault();
        applySelection(this, { persist: true, reload: true });
      });
    });

    // Load saved
    try {
      const saved = localStorage.getItem('selectedLanguage');
      if (saved) {
        const savedItem = languageSubmenu.querySelector(`[data-lang="${saved}"]`);
        if (savedItem) applySelection(savedItem, { persist: false, reload: false });
      }
    } catch (_) {}

    // Submenu open/close only on click
    const showSubmenu = () => {
      languageSubmenu.style.opacity = '1';
      languageSubmenu.style.visibility = 'visible';
      languageSubmenu.style.transform = 'translateY(0)';
    };
    const hideSubmenu = () => {
      languageSubmenu.style.opacity = '';
      languageSubmenu.style.visibility = '';
      languageSubmenu.style.transform = '';
    };

    if (languagesItem) {
      languagesItem.addEventListener('click', function (e) {
        e.preventDefault();
        // Toggle on click
        const visible = languageSubmenu.style.visibility === 'visible';
        if (visible) hideSubmenu(); else showSubmenu();
      });
    }
    // Close submenu when clicking inside profile dropdown but outside submenu or item
    if (profileDropdown) {
      profileDropdown.addEventListener('click', function (e) {
        if (!languageSubmenu.contains(e.target) && !languagesItem.contains(e.target)) {
          hideSubmenu();
        }
      });
    }
  }
  function closeWithChevron(container, dropdown) {
    if (!container || !dropdown) return;
    dropdown.classList.remove('show');
    const chev = container.querySelector('.fa-chevron-down');
    if (chev) chev.style.transform = 'rotate(0deg)';
  }

  // Language dropdown (legacy top-level)
  if (languageMenu && languageDropdown) {
    if (!languageMenu.dataset.navInit) {
      languageMenu.dataset.navInit = '1';
      languageMenu.addEventListener('click', function (e) {
        e.stopPropagation();
        toggleWithChevron(languageMenu, languageDropdown);
      });
      document.addEventListener('click', function (e) {
        if (!languageMenu.contains(e.target)) {
          closeWithChevron(languageMenu, languageDropdown);
        }
      });
      languageDropdown.addEventListener('click', function (e) {
        e.stopPropagation();
      });

      // Options
      const languageItems = document.querySelectorAll('.language-item');
      languageItems.forEach((item) => {
        item.addEventListener('click', function (e) {
          e.preventDefault();
          languageItems.forEach((i) => i.classList.remove('active'));
          this.classList.add('active');
          const nameEl = this.querySelector('.font-medium');
          const flagEl = this.querySelector('span');
          if (currentLanguage && nameEl) currentLanguage.textContent = nameEl.textContent;
          if (currentLanguageFlag && flagEl) currentLanguageFlag.textContent = flagEl.textContent;
          const code = this.getAttribute('data-lang');
          try { localStorage.setItem('selectedLanguage', code); } catch (_) {}
          closeWithChevron(languageMenu, languageDropdown);
        });
      });

      // Load saved
      try {
        const saved = localStorage.getItem('selectedLanguage');
        if (saved) {
          const savedItem = document.querySelector(`[data-lang="${saved}"]`);
          if (savedItem) {
            languageItems.forEach((i) => i.classList.remove('active'));
            savedItem.classList.add('active');
            const nameEl = savedItem.querySelector('.font-medium');
            const flagEl = savedItem.querySelector('span');
            if (currentLanguage && nameEl) currentLanguage.textContent = nameEl.textContent;
            if (currentLanguageFlag && flagEl) currentLanguageFlag.textContent = flagEl.textContent;
          }
        }
      } catch (_) {}
    }
  }

  // Profile dropdown
  if (profileMenu && profileDropdown) {
    if (!profileMenu.dataset.navInit) {
      profileMenu.dataset.navInit = '1';
      profileMenu.addEventListener('click', function (e) {
        e.stopPropagation();
        toggleWithChevron(profileMenu, profileDropdown);
        // When opening profile, ensure submenu hidden initially
        if (!profileDropdown.classList.contains('show') && languageSubmenu) {
          // Just closed
          languageSubmenu.style.opacity = '';
          languageSubmenu.style.visibility = '';
          languageSubmenu.style.transform = '';
        }
      });
      document.addEventListener('click', function (e) {
        if (!profileMenu.contains(e.target) && !profileDropdown.contains(e.target)) {
          closeWithChevron(profileMenu, profileDropdown);
          if (languageSubmenu) {
            languageSubmenu.style.opacity = '';
            languageSubmenu.style.visibility = '';
            languageSubmenu.style.transform = '';
          }
        }
      });
      profileDropdown.addEventListener('click', function (e) {
        e.stopPropagation();
      });
    }
  }
})();
