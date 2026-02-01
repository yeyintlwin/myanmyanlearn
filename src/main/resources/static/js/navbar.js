// Shared navbar interactions: language and profile dropdowns
(function () {
  try {
    if (
      typeof MutationObserver !== "undefined" &&
      MutationObserver &&
      MutationObserver.prototype &&
      typeof MutationObserver.prototype.observe === "function"
    ) {
      var originalObserve = MutationObserver.prototype.observe;
      MutationObserver.prototype.observe = function (target, options) {
        if (!target || typeof Node === "undefined" || !(target instanceof Node))
          return;
        return originalObserve.call(this, target, options);
      };
    }
  } catch (_) {}
})();

(function () {
  const byId = (id) => document.getElementById(id);
  const safeStorageGet = (key) => {
    try {
      return localStorage.getItem(key);
    } catch (_) {
      return null;
    }
  };
  const safeStorageSet = (key, value) => {
    try {
      localStorage.setItem(key, value);
    } catch (_) {}
  };
  const safeCookieSet = (name, value) => {
    try {
      document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=31536000; SameSite=Lax`;
    } catch (_) {}
  };
  const setSelectedLanguage = (code) => {
    safeStorageSet("selectedLanguage", code);
    safeCookieSet("selectedLanguage", code);
  };
  const syncLanguageDisplay = (name, flag) => {
    if (currentLanguageSmall && name) currentLanguageSmall.textContent = name;
    if (currentLanguage && name) currentLanguage.textContent = name;
    if (currentLanguageFlag && flag) currentLanguageFlag.textContent = flag;
  };
  const setActiveItem = (items, activeItem, { checkmark = false } = {}) => {
    items.forEach((i) => i.classList.remove("active"));
    activeItem.classList.add("active");
    if (!checkmark) return;
    items.forEach((i) => {
      const chk = i.querySelector(".lang-check");
      if (chk) chk.remove();
    });
    try {
      const check = document.createElement("i");
      check.className = "lang-check fas fa-check text-blue-600 text-xs ml-auto";
      activeItem.appendChild(check);
    } catch (_) {}
  };
  const languageMenu = byId("languageMenu"); // legacy (top-level)
  const languageDropdown = byId("languageDropdown"); // legacy (top-level)
  const currentLanguage = byId("currentLanguage"); // legacy label
  const currentLanguageFlag = byId("currentLanguageFlag"); // legacy flag
  const languageSubmenu = byId("languageSubmenu"); // new submenu container
  const currentLanguageSmall = byId("currentLanguageSmall"); // new small label
  const profileMenu = byId("profileMenu");
  const profileDropdown = byId("profileDropdown");
  const languagesItem = byId("languagesItem");

  // Helper to toggle dropdown and rotate chevron
  function toggleWithChevron(container, dropdown) {
    if (!container || !dropdown) return;
    dropdown.classList.toggle("show");
    const chev = container.querySelector(".fa-chevron-down");
    if (chev)
      chev.style.transform = dropdown.classList.contains("show")
        ? "rotate(180deg)"
        : "rotate(0deg)";
  }

  // Languages submenu (new inside Profile dropdown)
  if (languageSubmenu) {
    const languageItems = languageSubmenu.querySelectorAll(".language-item");
    const applySelection = (item, { persist = true, reload = false } = {}) => {
      const nameEl = item.querySelector(".font-medium");
      const flagEl = item.querySelector("span");
      setActiveItem(languageItems, item, { checkmark: true });
      syncLanguageDisplay(
        nameEl ? nameEl.textContent : "",
        flagEl ? flagEl.textContent : "",
      );
      const code = item.getAttribute("data-lang");
      if (persist) {
        setSelectedLanguage(code);
      }
      if (reload) {
        // Small delay to allow UI to update before reload
        setTimeout(() => {
          window.location.reload();
        }, 50);
      }
    };

    languageItems.forEach((item) => {
      item.addEventListener("click", function (e) {
        e.preventDefault();
        applySelection(this, { persist: true, reload: true });
      });
    });

    // Load saved
    try {
      const saved = safeStorageGet("selectedLanguage");
      if (saved) {
        const savedItem = languageSubmenu.querySelector(
          `[data-lang="${saved}"]`,
        );
        if (savedItem)
          applySelection(savedItem, { persist: false, reload: false });
      }
    } catch (_) {}

    // Submenu open/close only on click
    const showSubmenu = () => {
      languageSubmenu.style.opacity = "1";
      languageSubmenu.style.visibility = "visible";
      languageSubmenu.style.transform = "translateY(0)";
    };
    const hideSubmenu = () => {
      languageSubmenu.style.opacity = "";
      languageSubmenu.style.visibility = "";
      languageSubmenu.style.transform = "";
    };

    if (languagesItem) {
      languagesItem.addEventListener("click", function (e) {
        e.preventDefault();
        // Toggle on click
        const visible = languageSubmenu.style.visibility === "visible";
        if (visible) hideSubmenu();
        else showSubmenu();
      });
    }
    // Close submenu when clicking inside profile dropdown but outside submenu or item
    if (profileDropdown) {
      profileDropdown.addEventListener("click", function (e) {
        if (
          !languageSubmenu.contains(e.target) &&
          (!languagesItem || !languagesItem.contains(e.target))
        ) {
          hideSubmenu();
        }
      });
    }
  }
  function closeWithChevron(container, dropdown) {
    if (!container || !dropdown) return;
    dropdown.classList.remove("show");
    const chev = container.querySelector(".fa-chevron-down");
    if (chev) chev.style.transform = "rotate(0deg)";
  }

  // Language dropdown (legacy top-level)
  if (languageMenu && languageDropdown) {
    if (!languageMenu.dataset.navInit) {
      languageMenu.dataset.navInit = "1";
      languageMenu.addEventListener("click", function (e) {
        e.stopPropagation();
        toggleWithChevron(languageMenu, languageDropdown);
      });
      document.addEventListener("click", function (e) {
        if (!languageMenu.contains(e.target)) {
          closeWithChevron(languageMenu, languageDropdown);
        }
      });
      languageDropdown.addEventListener("click", function (e) {
        e.stopPropagation();
      });

      // Options
      const languageItems = languageDropdown.querySelectorAll(".language-item");
      languageItems.forEach((item) => {
        item.addEventListener("click", function (e) {
          e.preventDefault();
          const nameEl = this.querySelector(".font-medium");
          const flagEl = this.querySelector("span");
          setActiveItem(languageItems, this);
          syncLanguageDisplay(
            nameEl ? nameEl.textContent : "",
            flagEl ? flagEl.textContent : "",
          );
          const code = this.getAttribute("data-lang");
          setSelectedLanguage(code);
          closeWithChevron(languageMenu, languageDropdown);
        });
      });

      // Load saved
      try {
        const saved = safeStorageGet("selectedLanguage");
        if (saved) {
          const savedItem = languageDropdown.querySelector(
            `[data-lang="${saved}"]`,
          );
          if (savedItem) {
            const nameEl = savedItem.querySelector(".font-medium");
            const flagEl = savedItem.querySelector("span");
            setActiveItem(languageItems, savedItem);
            syncLanguageDisplay(
              nameEl ? nameEl.textContent : "",
              flagEl ? flagEl.textContent : "",
            );
          }
        }
      } catch (_) {}
    }
  }

  // Profile dropdown
  if (profileMenu && profileDropdown) {
    if (!profileMenu.dataset.navInit) {
      profileMenu.dataset.navInit = "1";
      profileMenu.addEventListener("click", function (e) {
        e.stopPropagation();
        toggleWithChevron(profileMenu, profileDropdown);
        // When opening profile, ensure submenu hidden initially
        if (!profileDropdown.classList.contains("show") && languageSubmenu) {
          // Just closed
          languageSubmenu.style.opacity = "";
          languageSubmenu.style.visibility = "";
          languageSubmenu.style.transform = "";
        }
      });
      document.addEventListener("click", function (e) {
        if (
          !profileMenu.contains(e.target) &&
          !profileDropdown.contains(e.target)
        ) {
          closeWithChevron(profileMenu, profileDropdown);
          if (languageSubmenu) {
            languageSubmenu.style.opacity = "";
            languageSubmenu.style.visibility = "";
            languageSubmenu.style.transform = "";
          }
        }
      });
      profileDropdown.addEventListener("click", function (e) {
        e.stopPropagation();
      });
    }
  }
})();
