(function () {
  var root = document.getElementById("coursesManagementRoot");
  if (!root) return;

  var storageKey =
    root.getAttribute("data-storage-key") || "adminCoursesDraft_v1";
  var state = { version: 2, order: "oldest-first", courses: [] };

  var i18n = {
    itemUntitled:
      root.getAttribute("data-i18n-item-untitled") || "Untitled course",
    itemNoDescription:
      root.getAttribute("data-i18n-item-no-description") ||
      "No description available.",
    itemNa: root.getAttribute("data-i18n-item-na") || "N/A",
    itemTargetsAllStudents:
      root.getAttribute("data-i18n-item-targets-all-students") ||
      "All students",
    itemTargetsYearsPrefix:
      root.getAttribute("data-i18n-item-targets-years-prefix") || "Years: ",
    itemTargetsClassesPrefix:
      root.getAttribute("data-i18n-item-targets-classes-prefix") || "Classes: ",
    itemStatusPublished:
      root.getAttribute("data-i18n-item-status-published") || "Published",
    itemStatusPrivate:
      root.getAttribute("data-i18n-item-status-private") || "Private",
    itemActionEdit: root.getAttribute("data-i18n-item-action-edit") || "Edit",
    itemActionDelete:
      root.getAttribute("data-i18n-item-action-delete") || "Delete",
    itemActionExport:
      root.getAttribute("data-i18n-item-action-export") || "Export",
    itemCoverAlt:
      root.getAttribute("data-i18n-item-cover-alt") || "Course cover",
    modalEditTitle:
      root.getAttribute("data-i18n-modal-edit-title") || "Edit course",
    actionSaveChanges:
      root.getAttribute("data-i18n-action-save-changes") || "Save changes",
    errorTitleRequired:
      root.getAttribute("data-i18n-error-title-required") ||
      "Course title is required.",
    errorInvalidImageFile:
      root.getAttribute("data-i18n-error-invalid-image-file") ||
      "Please select a valid image file.",
    toastDeleted:
      root.getAttribute("data-i18n-toast-deleted") || "Course deleted.",
    toastDeleteFailed:
      root.getAttribute("data-i18n-toast-delete-failed") ||
      "Failed to delete course from database.",
    toastCreated:
      root.getAttribute("data-i18n-toast-created") || "Course created.",
    toastUpdated:
      root.getAttribute("data-i18n-toast-updated") || "Course updated.",
    toastSaveFailed:
      root.getAttribute("data-i18n-toast-save-failed") ||
      "Failed to save course to database.",
    toastPublished:
      root.getAttribute("data-i18n-toast-published") || "Course published.",
    toastPrivate:
      root.getAttribute("data-i18n-toast-private") || "Course set to private.",
    toastPublishUpdateFailed:
      root.getAttribute("data-i18n-toast-publish-update-failed") ||
      "Failed to update publish state in database.",
    toastNoCoursesToExport:
      root.getAttribute("data-i18n-toast-no-courses-to-export") ||
      "No courses to export.",
    toastCourseExported:
      root.getAttribute("data-i18n-toast-course-exported") ||
      "Course exported.",
    toastCoursesExported:
      root.getAttribute("data-i18n-toast-courses-exported") ||
      "Courses exported.",
    toastCourseNotFound:
      root.getAttribute("data-i18n-toast-course-not-found") ||
      "Course not found.",
    toastInvalidImportFormat:
      root.getAttribute("data-i18n-toast-invalid-import-format") ||
      "Invalid import file format.",
    toastCoursesImported:
      root.getAttribute("data-i18n-toast-courses-imported") ||
      "Courses imported.",
    toastImportDbFailed:
      root.getAttribute("data-i18n-toast-import-db-failed") ||
      "Import saved locally, but DB save failed.",
    toastImportReadFailed:
      root.getAttribute("data-i18n-toast-import-read-failed") ||
      "Failed to read import file.",
  };

  var csrfHeaderName = null;
  var csrfToken = null;
  var csrfLoaded = false;

  function csrfHeaders() {
    if (!csrfLoaded) {
      csrfLoaded = true;
      try {
        var tokenMeta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        csrfToken = tokenMeta ? tokenMeta.getAttribute("content") : null;
        csrfHeaderName = headerMeta ? headerMeta.getAttribute("content") : null;
        if (!csrfToken) {
          var tokenInput = document.querySelector('input[name="_csrf"]');
          csrfToken = tokenInput ? tokenInput.getAttribute("value") : null;
        }
        if (!csrfHeaderName) {
          csrfHeaderName = "X-CSRF-TOKEN";
        }
      } catch (e) {
        csrfToken = null;
        csrfHeaderName = null;
      }
    }

    if (!csrfToken || !csrfHeaderName) return {};
    var headers = {};
    headers[csrfHeaderName] = csrfToken;
    return headers;
  }

  function apiFetch(url, opts) {
    var options = opts && typeof opts === "object" ? opts : {};
    if (!options.headers) options.headers = {};
    var csrf = csrfHeaders();
    var keys = Object.keys(csrf);
    for (var i = 0; i < keys.length; i++) {
      options.headers[keys[i]] = csrf[keys[i]];
    }
    options.credentials = "include";
    return fetch(url, options);
  }

  function storageGet(key) {
    try {
      return localStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function storageSet(key, value) {
    try {
      localStorage.setItem(key, value);
    } catch (e) {}
  }

  function uid(prefix) {
    return (
      (prefix || "id") +
      "_" +
      Math.random().toString(16).slice(2) +
      "_" +
      Date.now().toString(16)
    );
  }

  function safeJsonParse(raw) {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }

  function loadState() {
    try {
      if (!storageKey) return;
      var raw = storageGet(storageKey);
      if (!raw) return;
      var loaded = safeJsonParse(raw);
      if (!loaded) return;
      if (
        loaded &&
        typeof loaded === "object" &&
        (typeof loaded.order === "string" || typeof loaded.version === "number")
      ) {
        state.order =
          typeof loaded.order === "string" && loaded.order.trim()
            ? loaded.order
            : "oldest-first";
        state.version =
          typeof loaded.version === "number" && loaded.version > 0
            ? loaded.version
            : 1;
        state.order = "oldest-first";
        state.version = 2;
      }
    } catch (e) {}
  }

  function saveState() {
    try {
      if (!storageKey) return;
      storageSet(
        storageKey,
        JSON.stringify({ version: state.version, order: state.order }),
      );
    } catch (e) {}
  }

  function slugify(s) {
    var v = String(s || "")
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");
    return v;
  }

  function uniqueCourseIdFromTitle(title) {
    var base = slugify(title) || uid("course");
    var candidate = base;
    var n = 1;
    while (findCourseById(candidate)) {
      n++;
      candidate = base + "-" + n;
    }
    return candidate;
  }

  function fetchCoursesFromServer() {
    return Promise.resolve([]);
  }

  function saveCourseMetaToServer(course) {
    if (!course || !course.id) return Promise.reject(new Error("Missing id"));
    return apiFetch(
      "/api/admin/courses/" + encodeURIComponent(course.id) + "/meta",
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          id: course.id,
          title: course.title || i18n.itemUntitled,
          description: course.description || "",
          language: course.language || "",
          published: !!course.published,
          targetStudents: course.targetStudents || {
            schoolYears: [],
            classes: [],
          },
          coverImageDataUrl: course.coverImageDataUrl || null,
        }),
      },
    ).then(function (res) {
      if (!res) throw new Error("Failed to save course (no response)");
      if (res.ok) return;
      return res.text().then(function (t) {
        var body = String(t || "").trim();
        var suffix = body ? ": " + body : "";
        throw new Error("Failed to save course (" + res.status + ")" + suffix);
      });
    });
  }

  function uploadCourseCoverToServer(courseId, file) {
    if (!courseId) return Promise.reject(new Error("Missing course id"));
    if (!file) return Promise.reject(new Error("Missing file"));
    var form = new FormData();
    form.append("file", file);
    return apiFetch(
      "/api/admin/courses/" + encodeURIComponent(courseId) + "/cover-image",
      {
        method: "POST",
        headers: { Accept: "application/json" },
        body: form,
      },
    )
      .then(function (res) {
        if (!res || !res.ok) throw new Error("Failed to upload cover image");
        return res.json();
      })
      .then(function (data) {
        if (!data || !data.ok || !data.url)
          throw new Error("Invalid upload response");
        return String(data.url);
      });
  }

  function deleteCourseOnServer(courseId) {
    return apiFetch("/api/admin/courses/" + encodeURIComponent(courseId), {
      method: "DELETE",
      headers: { Accept: "application/json" },
    }).then(function (res) {
      if (!res || !res.ok) throw new Error("Failed to delete course");
    });
  }

  var coursesFlash = document.getElementById("coursesFlash");
  var addCourseBtn = document.getElementById("addCourseBtn");
  var importCoursesBtn = document.getElementById("importCoursesBtn");
  var importCoursesInput = document.getElementById("importCoursesInput");

  var courseList = document.getElementById("courseList");
  var courseListEmpty = document.getElementById("courseListEmpty");
  var courseCountBadge = document.getElementById("courseCountBadge");

  var addCourseModal = document.getElementById("addCourseModal");
  var addCourseForm = document.getElementById("addCourseForm");
  var addCourseCancel = document.getElementById("addCourseCancel");
  var addCourseClose = document.getElementById("addCourseClose");
  var addCourseTitleEl = document.getElementById("addCourseTitle");
  var addCourseSubmitBtn = document.getElementById("addCourseSubmit");
  var addCourseError = document.getElementById("addCourseError");
  var courseTitleInput = document.getElementById("courseTitle");
  var courseLanguageSelect = document.getElementById("courseLanguage");
  var targetSchoolYearsOptions = document.getElementById(
    "targetSchoolYearsOptions",
  );
  var targetClassesOptions = document.getElementById("targetClassesOptions");
  var courseDescriptionInput = document.getElementById("courseDescription");
  var courseCoverInput = document.getElementById("courseCover");
  var courseCoverPreview = document.getElementById("courseCoverPreview");
  var removeCourseCoverBtn = document.getElementById("removeCourseCoverBtn");

  var confirmDeleteModal = document.getElementById("confirmDeleteModal");
  var confirmDeleteTitle = document.getElementById("confirmDeleteTitle");
  var confirmDeleteMessage = document.getElementById("confirmDeleteMessage");
  var confirmDeleteCancel = document.getElementById("confirmDeleteCancel");
  var confirmDeleteConfirm = document.getElementById("confirmDeleteConfirm");

  var editingCourseId = null;
  var removeCourseCover = false;
  var pendingConfirmAction = null;

  var confirmDefaultText = confirmDeleteConfirm
    ? String(confirmDeleteConfirm.textContent || "").trim() || "Confirm"
    : "Confirm";
  var confirmDefaultClassName = confirmDeleteConfirm
    ? String(confirmDeleteConfirm.className || "")
    : "";

  var modalAddCourseTitle = addCourseTitleEl
    ? String(addCourseTitleEl.textContent || "").trim() || "Add course"
    : "Add course";
  var modalAddCourseSubmitLabel = addCourseSubmitBtn
    ? String(addCourseSubmitBtn.textContent || "").trim() || "Create course"
    : "Create course";

  var SCHOOL_YEAR_OPTIONS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"];
  var CLASS_OPTIONS = (function () {
    var arr = [];
    for (var i = 0; i < 26; i++) arr.push(String.fromCharCode(65 + i));
    return arr;
  })();

  var selectedSchoolYears = {};
  var selectedClasses = {};

  function escapeHtml(s) {
    return String(s || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function showFlash(variant, message) {
    if (!coursesFlash) return;
    if (!message) {
      coursesFlash.className = "hidden";
      coursesFlash.innerHTML = "";
      return;
    }
    var base =
      "rounded-xl border px-4 py-3 text-sm font-medium backdrop-blur-xl";
    var style = "border-white/10 bg-white/5 text-white/80";
    if (variant === "success")
      style = "border-emerald-500/30 bg-emerald-500/10 text-emerald-50";
    if (variant === "error")
      style = "border-rose-500/30 bg-rose-500/10 text-rose-100";
    if (variant === "info") style = "border-white/15 bg-white/5 text-white/80";
    coursesFlash.className = base + " " + style;
    coursesFlash.innerHTML = escapeHtml(message);
  }

  function openModal(modal) {
    if (!modal) return;
    modal.classList.remove("hidden");
    document.body.classList.add("overflow-hidden");
  }

  function closeModal(modal) {
    if (!modal) return;
    modal.classList.add("hidden");
    document.body.classList.remove("overflow-hidden");
  }

  function wireModalBackdrop(modal) {
    if (!modal) return;
    modal.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });
  }

  function setError(el, message) {
    if (!el) return;
    if (!message) {
      el.classList.add("hidden");
      el.innerHTML = "";
      return;
    }
    el.classList.remove("hidden");
    el.innerHTML =
      '<div class="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">' +
      escapeHtml(message) +
      "</div>";
  }

  function configureConfirm(
    title,
    message,
    confirmLabel,
    confirmClassName,
    onConfirm,
  ) {
    if (!confirmDeleteModal) return;
    if (confirmDeleteTitle && title != null)
      confirmDeleteTitle.textContent = title;
    if (confirmDeleteMessage && message != null)
      confirmDeleteMessage.textContent = message;
    if (confirmDeleteConfirm && confirmLabel != null)
      confirmDeleteConfirm.textContent = confirmLabel;
    if (confirmDeleteConfirm && confirmClassName != null)
      confirmDeleteConfirm.className = confirmClassName;
    pendingConfirmAction = typeof onConfirm === "function" ? onConfirm : null;
    openModal(confirmDeleteModal);
  }

  function closeConfirm() {
    pendingConfirmAction = null;
    if (confirmDeleteConfirm) {
      confirmDeleteConfirm.textContent = confirmDefaultText;
      confirmDeleteConfirm.className = confirmDefaultClassName;
    }
    closeModal(confirmDeleteModal);
  }

  function normalizeStringArray(value) {
    if (!Array.isArray(value)) return [];
    var seen = {};
    var out = [];
    for (var i = 0; i < value.length; i++) {
      var v = value[i];
      var s = "";
      if (typeof v === "string") s = v.trim();
      else if (typeof v === "number") s = String(v);
      if (!s) continue;
      if (seen[s]) continue;
      seen[s] = true;
      out.push(s);
    }
    return out;
  }

  function normalizeTargetStudents(targetStudents) {
    var ts = {};
    if (targetStudents && typeof targetStudents === "object") {
      ts = targetStudents;
    } else if (typeof targetStudents === "string" && targetStudents.trim()) {
      var parsed = safeJsonParse(targetStudents);
      if (parsed && typeof parsed === "object") ts = parsed;
    }
    var years = normalizeStringArray(ts.schoolYears);
    var classes = normalizeStringArray(ts.classes);
    years.sort(function (a, b) {
      return (parseInt(a, 10) || 0) - (parseInt(b, 10) || 0);
    });
    classes.sort();
    return { schoolYears: years, classes: classes };
  }

  function ensureCourseShape(course) {
    if (!course || typeof course !== "object") return null;
    if (!course.id) course.id = uid("course");
    if (!course.title) course.title = i18n.itemUntitled;
    if (typeof course.language !== "string") course.language = "";
    if (typeof course.published !== "boolean") course.published = true;
    course.targetStudents = normalizeTargetStudents(course.targetStudents);
    if (typeof course.description !== "string") course.description = "";
    if (typeof course.coverImageDataUrl !== "string")
      course.coverImageDataUrl = null;
    return course;
  }

  function normalizeState() {
    if (!Array.isArray(state.courses)) state.courses = [];
    var cleaned = [];
    for (var i = 0; i < state.courses.length; i++) {
      var c = ensureCourseShape(state.courses[i]);
      if (c) cleaned.push(c);
    }
    state.courses = cleaned;
  }

  function findCourseById(courseId) {
    for (var i = 0; i < state.courses.length; i++) {
      var c = state.courses[i];
      if (c && c.id === courseId) return c;
    }
    return null;
  }

  function languageShort(langCode) {
    if (typeof langCode !== "string") return "";
    var v = langCode.trim().toUpperCase();
    return v || "";
  }

  function describeTargets(course) {
    if (!course) return i18n.itemTargetsAllStudents;
    var ts = normalizeTargetStudents(course.targetStudents);
    var parts = [];
    if (ts.schoolYears.length)
      parts.push(i18n.itemTargetsYearsPrefix + ts.schoolYears.join(", "));
    if (ts.classes.length)
      parts.push(i18n.itemTargetsClassesPrefix + ts.classes.join(", "));
    return parts.length ? parts.join(" · ") : i18n.itemTargetsAllStudents;
  }

  function clearElement(el) {
    if (!el) return;
    while (el.firstChild) el.removeChild(el.firstChild);
  }

  function closeAllCourseMenus(exceptCourseId) {
    if (!courseList) return;
    var openMenus = courseList.querySelectorAll(
      "[data-course-menu]:not(.hidden)",
    );
    for (var i = 0; i < openMenus.length; i++) {
      var menu = openMenus[i];
      var menuCourseId = menu.getAttribute("data-course-id");
      if (exceptCourseId && menuCourseId === exceptCourseId) continue;
      menu.classList.add("hidden");
    }
  }

  function renderList() {
    if (courseCountBadge)
      courseCountBadge.textContent = String(state.courses.length);
    if (!courseList) return;
    clearElement(courseList);
    if (!state.courses.length) {
      if (courseListEmpty) courseListEmpty.classList.remove("hidden");
      return;
    }
    if (courseListEmpty) courseListEmpty.classList.add("hidden");

    for (var i = 0; i < state.courses.length; i++) {
      var course = state.courses[i];
      var row = document.createElement("div");
      row.className =
        "rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 transition px-4 py-3 flex items-start gap-3";

      var content = document.createElement("a");
      content.href = "/admin/courses/" + encodeURIComponent(course.id);
      content.className = "min-w-0 flex-1 no-underline flex items-start gap-3";
      content.setAttribute("data-course-action", "open");
      content.setAttribute("data-course-id", course.id);

      var cover = document.createElement("div");
      cover.className =
        "w-16 h-24 rounded-lg overflow-hidden flex-shrink-0 bg-white/5 flex items-center justify-center";
      if (course.coverImageDataUrl) {
        var img = document.createElement("img");
        img.src = course.coverImageDataUrl;
        img.className = "w-full h-full object-cover object-top block";
        img.alt = course.title || i18n.itemCoverAlt;
        cover.appendChild(img);
      } else {
        cover.className += " bg-white/10";
        cover.innerHTML = '<i class="fas fa-book text-white/50 text-xl"></i>';
      }

      var text = document.createElement("div");
      text.className = "min-w-0 flex-1";

      var title = document.createElement("div");
      title.className = "text-white font-semibold leading-tight break-words";
      title.textContent = course.title || i18n.itemUntitled;

      var description = document.createElement("div");
      description.className = "mt-1 text-white/70 text-sm break-words";
      description.textContent = course.description || i18n.itemNoDescription;

      var meta = document.createElement("div");
      meta.className = "mt-2 flex items-center gap-2 flex-wrap";

      var lang = document.createElement("span");
      lang.className =
        "inline-flex items-center rounded-full border border-sky-500/30 bg-sky-500/10 px-2.5 h-7 text-[11px] font-semibold text-sky-100";
      lang.textContent = languageShort(course.language) || i18n.itemNa;

      var targets = document.createElement("span");
      targets.className =
        "inline-flex items-center rounded-full border border-white/15 bg-white/5 px-2.5 h-7 text-[11px] font-semibold text-white/80";
      targets.textContent = describeTargets(course);

      meta.appendChild(lang);
      meta.appendChild(targets);

      text.appendChild(title);
      text.appendChild(description);
      text.appendChild(meta);

      content.appendChild(cover);
      content.appendChild(text);

      var publishBtn = document.createElement("button");
      publishBtn.type = "button";
      publishBtn.setAttribute("data-course-action", "toggle-publish");
      publishBtn.setAttribute("data-course-id", course.id);
      publishBtn.className =
        "inline-flex items-center justify-center gap-1.5 rounded-lg border px-3 h-9 text-[11px] font-semibold transition flex-shrink-0 " +
        (course.published
          ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-50 hover:bg-emerald-500/15"
          : "border-amber-500/30 bg-amber-500/10 text-amber-50 hover:bg-amber-500/15");
      publishBtn.innerHTML = course.published
        ? '<i class="fas fa-eye text-[10px]"></i><span>' +
          escapeHtml(i18n.itemStatusPublished) +
          "</span>"
        : '<i class="fas fa-eye-slash text-[10px]"></i><span>' +
          escapeHtml(i18n.itemStatusPrivate) +
          "</span>";

      var dropdown = document.createElement("div");
      dropdown.className = "relative flex-shrink-0";
      dropdown.setAttribute("data-course-dropdown", "true");

      var dropdownBtn = document.createElement("button");
      dropdownBtn.type = "button";
      dropdownBtn.className =
        "inline-flex items-center justify-center rounded-lg border border-white/15 bg-white/5 w-9 h-9 text-xs font-semibold text-white/90 hover:bg-white/10 transition";
      dropdownBtn.setAttribute("data-course-action", "menu");
      dropdownBtn.setAttribute("data-course-id", course.id);
      dropdownBtn.innerHTML = '<i class="fas fa-ellipsis-v text-[10px]"></i>';

      var dropdownMenu = document.createElement("div");
      dropdownMenu.className =
        "absolute right-0 mt-2 w-44 rounded-xl border border-white/15 bg-slate-950/80 backdrop-blur-xl hidden z-10 overflow-hidden";
      dropdownMenu.setAttribute("data-course-menu", "true");
      dropdownMenu.setAttribute("data-course-id", course.id);
      dropdownMenu.innerHTML =
        '<button type="button" data-course-action="edit" data-course-id="' +
        escapeHtml(course.id) +
        '" class="block w-full text-left px-4 py-2 text-sm text-white/90 hover:bg-white/10 transition">' +
        escapeHtml(i18n.itemActionEdit) +
        "</button>" +
        '<button type="button" data-course-action="delete" data-course-id="' +
        escapeHtml(course.id) +
        '" class="block w-full text-left px-4 py-2 text-sm text-rose-100 hover:bg-rose-500/10 transition">' +
        escapeHtml(i18n.itemActionDelete) +
        "</button>" +
        '<button type="button" data-course-action="export" data-course-id="' +
        escapeHtml(course.id) +
        '" class="block w-full text-left px-4 py-2 text-sm text-white/90 hover:bg-white/10 transition">' +
        escapeHtml(i18n.itemActionExport) +
        "</button>";

      dropdown.appendChild(dropdownBtn);
      dropdown.appendChild(dropdownMenu);

      row.appendChild(content);
      row.appendChild(publishBtn);
      row.appendChild(dropdown);
      courseList.appendChild(row);
    }
  }

  function resetTargetSelections() {
    selectedSchoolYears = {};
    selectedClasses = {};
  }

  function setTargetSelections(targetStudents) {
    resetTargetSelections();
    var ts = normalizeTargetStudents(targetStudents);
    for (var i = 0; i < ts.schoolYears.length; i++)
      selectedSchoolYears[ts.schoolYears[i]] = true;
    for (var j = 0; j < ts.classes.length; j++)
      selectedClasses[ts.classes[j]] = true;
  }

  function snapshotTargetSelections() {
    var years = [];
    var classes = [];
    for (var i = 0; i < SCHOOL_YEAR_OPTIONS.length; i++) {
      var y = SCHOOL_YEAR_OPTIONS[i];
      if (selectedSchoolYears[y]) years.push(y);
    }
    for (var j = 0; j < CLASS_OPTIONS.length; j++) {
      var c = CLASS_OPTIONS[j];
      if (selectedClasses[c]) classes.push(c);
    }
    return normalizeTargetStudents({ schoolYears: years, classes: classes });
  }

  function optionButton(label, selected) {
    var btn = document.createElement("button");
    btn.type = "button";
    btn.className =
      "h-8 px-2.5 rounded-lg border text-[11px] font-semibold transition " +
      (selected
        ? "border-white/25 bg-white/15 text-white"
        : "border-white/10 bg-white/5 text-white/80 hover:bg-white/10");
    btn.textContent = label;
    return btn;
  }

  function renderTargetPickers() {
    if (!targetSchoolYearsOptions || !targetClassesOptions) return;
    clearElement(targetSchoolYearsOptions);
    clearElement(targetClassesOptions);

    for (var i = 0; i < SCHOOL_YEAR_OPTIONS.length; i++) {
      var y = SCHOOL_YEAR_OPTIONS[i];
      var yBtn = optionButton(y, !!selectedSchoolYears[y]);
      yBtn.setAttribute("data-target-kind", "year");
      yBtn.setAttribute("data-target-value", y);
      targetSchoolYearsOptions.appendChild(yBtn);
    }
    for (var j = 0; j < CLASS_OPTIONS.length; j++) {
      var c = CLASS_OPTIONS[j];
      var cBtn = optionButton(c, !!selectedClasses[c]);
      cBtn.setAttribute("data-target-kind", "class");
      cBtn.setAttribute("data-target-value", c);
      targetClassesOptions.appendChild(cBtn);
    }
  }

  function openAddCourse() {
    editingCourseId = null;
    removeCourseCover = false;
    resetTargetSelections();
    renderTargetPickers();
    if (addCourseTitleEl) addCourseTitleEl.textContent = modalAddCourseTitle;
    if (addCourseSubmitBtn)
      addCourseSubmitBtn.textContent = modalAddCourseSubmitLabel;
    if (addCourseForm) addCourseForm.reset();
    if (courseCoverPreview) {
      courseCoverPreview.classList.add("hidden");
      courseCoverPreview.removeAttribute("src");
    }
    if (removeCourseCoverBtn) removeCourseCoverBtn.classList.add("hidden");
    if (courseCoverInput) courseCoverInput.value = "";
    setError(addCourseError, "");
    openModal(addCourseModal);
    if (courseTitleInput) courseTitleInput.focus();
  }

  function openEditCourse(courseId) {
    var course = findCourseById(courseId);
    if (!course) return;
    editingCourseId = courseId;
    removeCourseCover = false;
    setTargetSelections(course.targetStudents);
    renderTargetPickers();
    if (addCourseTitleEl) addCourseTitleEl.textContent = i18n.modalEditTitle;
    if (addCourseSubmitBtn)
      addCourseSubmitBtn.textContent = i18n.actionSaveChanges;
    if (courseTitleInput) courseTitleInput.value = course.title || "";
    if (courseLanguageSelect)
      courseLanguageSelect.value = course.language || "";
    if (courseDescriptionInput)
      courseDescriptionInput.value = course.description || "";
    if (courseCoverPreview) {
      if (course.coverImageDataUrl) {
        courseCoverPreview.src = course.coverImageDataUrl;
        courseCoverPreview.classList.remove("hidden");
      } else {
        courseCoverPreview.classList.add("hidden");
        courseCoverPreview.removeAttribute("src");
      }
    }
    if (removeCourseCoverBtn) {
      if (course.coverImageDataUrl)
        removeCourseCoverBtn.classList.remove("hidden");
      else removeCourseCoverBtn.classList.add("hidden");
    }
    if (courseCoverInput) courseCoverInput.value = "";
    setError(addCourseError, "");
    openModal(addCourseModal);
    if (courseTitleInput) courseTitleInput.focus();
  }

  function closeAddCourse() {
    setError(addCourseError, "");
    closeModal(addCourseModal);
  }

  function deleteCourseById(courseId) {
    state.courses = state.courses.filter(function (c) {
      return !(c && c.id === courseId);
    });
    normalizeState();
    saveState();
    renderList();
  }

  function updateCourseFromForm(course, coverDataUrl) {
    var title = courseTitleInput
      ? String(courseTitleInput.value || "").trim()
      : "";
    course.title = title || i18n.itemUntitled;
    course.language = courseLanguageSelect
      ? String(courseLanguageSelect.value || "")
      : "";
    course.description = courseDescriptionInput
      ? String(courseDescriptionInput.value || "")
      : "";
    course.targetStudents = snapshotTargetSelections();
    if (removeCourseCover) course.coverImageDataUrl = null;
    else if (typeof coverDataUrl === "string")
      course.coverImageDataUrl = coverDataUrl;
  }

  function submitCourse() {
    setError(addCourseError, "");
    var title = courseTitleInput
      ? String(courseTitleInput.value || "").trim()
      : "";
    if (!title) {
      setError(addCourseError, i18n.errorTitleRequired);
      return;
    }

    var existing = editingCourseId ? findCourseById(editingCourseId) : null;
    var course =
      existing || ensureCourseShape({ id: uniqueCourseIdFromTitle(title) });

    var file =
      courseCoverInput && courseCoverInput.files && courseCoverInput.files[0]
        ? courseCoverInput.files[0]
        : null;

    var optimistic = !existing;
    updateCourseFromForm(course, null);
    if (optimistic) state.courses.push(course);
    normalizeState();
    renderList();

    if (!file) {
      saveCourseMetaToServer(course)
        .then(function () {
          normalizeState();
          saveState();
          renderList();
          closeAddCourse();
          showFlash(
            "success",
            existing ? i18n.toastUpdated : i18n.toastCreated,
          );
        })
        .catch(function (err) {
          if (optimistic) deleteCourseById(course.id);
          showFlash(
            "error",
            err && err.message ? String(err.message) : i18n.toastSaveFailed,
          );
        });
      return;
    }
    if (!file.type || file.type.indexOf("image/") !== 0) {
      setError(addCourseError, i18n.errorInvalidImageFile);
      if (courseCoverInput) courseCoverInput.value = "";
      return;
    }
    removeCourseCover = false;

    saveCourseMetaToServer(course)
      .then(function () {
        return uploadCourseCoverToServer(course.id, file);
      })
      .then(function (url) {
        course.coverImageDataUrl = url;
        normalizeState();
        renderList();
        return saveCourseMetaToServer(course);
      })
      .then(function () {
        normalizeState();
        saveState();
        renderList();
        closeAddCourse();
        showFlash("success", existing ? i18n.toastUpdated : i18n.toastCreated);
      })
      .catch(function (err) {
        if (optimistic) deleteCourseById(course.id);
        showFlash(
          "error",
          err && err.message ? String(err.message) : i18n.toastSaveFailed,
        );
      });
  }

  function exportCourses(coursesToExport, filenamePrefix) {
    var list = Array.isArray(coursesToExport) ? coursesToExport : state.courses;
    if (!list.length) {
      showFlash("info", i18n.toastNoCoursesToExport);
      return;
    }
    var payload = {
      version: 1,
      exportedAt: new Date().toISOString(),
      courses: list,
    };
    var json = JSON.stringify(payload, null, 2);
    var blob = new Blob([json], { type: "application/json" });
    var url = URL.createObjectURL(blob);
    var a = document.createElement("a");
    a.href = url;
    var ts = new Date()
      .toISOString()
      .replace(/[:.]/g, "-")
      .replace("T", "_")
      .slice(0, 19);
    var base = String(filenamePrefix || "courses_export").trim();
    base = base
      ? base.replace(/[^a-z0-9_-]+/gi, "_").replace(/^_+|_+$/g, "")
      : "courses_export";
    a.download = base + "_" + ts + ".json";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    showFlash(
      "success",
      list.length === 1 ? i18n.toastCourseExported : i18n.toastCoursesExported,
    );
  }

  function exportCourseById(courseId) {
    var course = findCourseById(courseId);
    if (!course) {
      showFlash("error", i18n.toastCourseNotFound);
      return;
    }
    downloadCourseBll(courseId);
  }

  function downloadCourseBll(courseId) {
    var id = String(courseId || "").trim();
    if (!id) return;
    apiFetch("/api/admin/courses/" + encodeURIComponent(id) + "/export", {
      method: "GET",
      headers: { Accept: "application/octet-stream" },
    })
      .then(function (res) {
        if (!res || !res.ok) {
          throw new Error(i18n.toastCourseNotFound);
        }
        return res.blob();
      })
      .then(function (blob) {
        var url = URL.createObjectURL(blob);
        var a = document.createElement("a");
        a.href = url;
        var ts = new Date()
          .toISOString()
          .replace(/[:.]/g, "-")
          .replace("T", "_")
          .slice(0, 19);
        var safeId = id.replace(/[^a-z0-9_-]+/gi, "_").replace(/^_+|_+$/g, "");
        a.download = (safeId || "course") + "_" + ts + ".bll";
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        showFlash("success", i18n.toastCourseExported);
      })
      .catch(function (err) {
        showFlash(
          "error",
          err && err.message ? String(err.message) : i18n.toastCourseNotFound,
        );
      });
  }

  function parseImportedCourses(parsed) {
    if (Array.isArray(parsed)) return parsed;
    if (parsed && typeof parsed === "object" && Array.isArray(parsed.courses))
      return parsed.courses;
    return null;
  }

  function importCourseBllFromFile(file) {
    if (!file) return;
    var form = new FormData();
    form.append("file", file);
    apiFetch("/api/admin/courses/import", {
      method: "POST",
      headers: { Accept: "application/json" },
      body: form,
    })
      .then(function (res) {
        if (!res) throw new Error(i18n.toastInvalidImportFormat);
        var ct = String(
          res.headers && res.headers.get
            ? res.headers.get("content-type") || ""
            : "",
        ).toLowerCase();
        var isJson = ct.indexOf("application/json") >= 0;
        if (!res.ok) {
          if (isJson) {
            return res
              .json()
              .catch(function () {
                return null;
              })
              .then(function (json) {
                var msg = json && json.message ? String(json.message) : null;
                throw new Error(msg || i18n.toastImportDbFailed);
              });
          }
          return res
            .text()
            .catch(function () {
              return "";
            })
            .then(function () {
              throw new Error(i18n.toastImportDbFailed);
            });
        }
        if (!isJson) throw new Error(i18n.toastImportDbFailed);
        return res.json().catch(function () {
          return null;
        });
      })
      .then(function (json) {
        if (!json || !json.ok || !json.courseId) {
          var msg = json && json.message ? String(json.message) : null;
          throw new Error(msg || i18n.toastInvalidImportFormat);
        }
        return apiFetch(
          "/api/admin/courses/" + encodeURIComponent(json.courseId) + "/editor",
          { method: "GET", headers: { Accept: "application/json" } },
        ).then(function (res) {
          if (!res || !res.ok) return null;
          return res.json().catch(function () {
            return null;
          });
        });
      })
      .then(function (editor) {
        if (editor && editor.id) {
          var meta = ensureCourseShape({
            id: editor.id,
            title: editor.title || i18n.itemUntitled,
            description: editor.description || "",
            language: editor.language || "",
            published: !!editor.published,
            targetStudents: editor.targetStudents || {
              schoolYears: [],
              classes: [],
            },
            coverImageDataUrl: editor.coverImageDataUrl || null,
          });
          var existing = findCourseById(meta.id);
          if (existing) {
            for (var k in meta) existing[k] = meta[k];
          } else {
            state.courses.push(meta);
          }
          normalizeState();
          saveState();
          renderList();
        } else {
          window.location.reload();
          return;
        }
        showFlash("success", i18n.toastCoursesImported);
      })
      .catch(function (err) {
        showFlash(
          "error",
          err && err.message ? String(err.message) : i18n.toastImportDbFailed,
        );
      });
  }

  function importCoursesFromFile(file) {
    if (!file) return;
    var name = String(file.name || "")
      .toLowerCase()
      .trim();
    var type = String(file.type || "").toLowerCase();
    if (
      name.endsWith(".bll") ||
      name.endsWith(".zip") ||
      type.indexOf("zip") >= 0
    ) {
      importCourseBllFromFile(file);
      return;
    }
    var reader = new FileReader();
    reader.onload = function () {
      var parsed = safeJsonParse(String(reader.result || ""));
      var courses = parseImportedCourses(parsed);
      if (!courses) {
        showFlash("error", i18n.toastInvalidImportFormat);
        return;
      }
      state.courses = courses;
      normalizeState();
      renderList();

      var list = state.courses.slice();
      var chain = Promise.resolve();
      for (var i = 0; i < list.length; i++) {
        (function (c) {
          chain = chain.then(function () {
            return saveCourseMetaToServer(c);
          });
        })(list[i]);
      }

      chain
        .then(function () {
          normalizeState();
          saveState();
          renderList();
          showFlash("success", i18n.toastCoursesImported);
        })
        .catch(function () {
          showFlash("error", i18n.toastImportDbFailed);
          window.location.reload();
        });
    };
    reader.onerror = function () {
      showFlash("error", i18n.toastImportReadFailed);
    };
    reader.readAsText(file);
  }

  function attachEvents() {
    if (addCourseBtn) addCourseBtn.addEventListener("click", openAddCourse);

    if (importCoursesBtn && importCoursesInput) {
      importCoursesBtn.addEventListener("click", function () {
        importCoursesInput.value = "";
        importCoursesInput.click();
      });
    }
    if (importCoursesInput) {
      importCoursesInput.addEventListener("change", function () {
        var file = this.files && this.files[0] ? this.files[0] : null;
        importCoursesFromFile(file);
      });
    }

    if (courseList) {
      courseList.addEventListener("click", function (e) {
        var target = e.target;
        if (!target) return;
        var btn = target.closest
          ? target.closest("[data-course-action]")
          : null;
        if (!btn) return;
        var action = btn.getAttribute("data-course-action");
        var courseId = btn.getAttribute("data-course-id");
        if (!action || !courseId) return;
        if (action !== "menu") closeAllCourseMenus();
        if (action === "toggle-publish") {
          var course = findCourseById(courseId);
          if (!course) return;
          var next = !course.published;
          course.published = next;
          normalizeState();
          renderList();
          saveCourseMetaToServer(course)
            .then(function () {
              saveState();
              showFlash(
                "success",
                next ? i18n.toastPublished : i18n.toastPrivate,
              );
            })
            .catch(function () {
              course.published = !next;
              normalizeState();
              renderList();
              showFlash("error", i18n.toastPublishUpdateFailed);
            });
          return;
        }
        if (action === "open") {
          e.preventDefault();
          window.location.assign(
            "/admin/courses/" + encodeURIComponent(courseId),
          );
          return;
        }
        if (action === "menu") {
          e.preventDefault();
          var dropdown = btn.closest
            ? btn.closest("[data-course-dropdown]")
            : null;
          var menu = dropdown
            ? dropdown.querySelector("[data-course-menu]")
            : null;
          if (!menu) return;
          var willOpen = menu.classList.contains("hidden");
          closeAllCourseMenus(courseId);
          if (willOpen) menu.classList.remove("hidden");
          else menu.classList.add("hidden");
          return;
        }
        if (action === "edit") {
          openEditCourse(courseId);
          return;
        }
        if (action === "delete") {
          configureConfirm(null, null, null, null, function () {
            deleteCourseOnServer(courseId)
              .then(function () {
                deleteCourseById(courseId);
                showFlash("success", i18n.toastDeleted);
              })
              .catch(function () {
                showFlash("error", i18n.toastDeleteFailed);
              });
          });
          return;
        }
        if (action === "export") {
          exportCourseById(courseId);
        }
      });
    }

    wireModalBackdrop(addCourseModal);
    wireModalBackdrop(confirmDeleteModal);

    if (addCourseCancel)
      addCourseCancel.addEventListener("click", closeAddCourse);
    if (addCourseClose)
      addCourseClose.addEventListener("click", closeAddCourse);

    if (addCourseForm) {
      addCourseForm.addEventListener("submit", function (e) {
        e.preventDefault();
        submitCourse();
      });
    }

    if (removeCourseCoverBtn)
      removeCourseCoverBtn.addEventListener("click", function () {
        removeCourseCover = true;
        if (courseCoverInput) courseCoverInput.value = "";
        if (courseCoverPreview) {
          courseCoverPreview.classList.add("hidden");
          courseCoverPreview.removeAttribute("src");
        }
        this.classList.add("hidden");
      });

    if (courseCoverInput && courseCoverPreview) {
      courseCoverInput.addEventListener("change", function () {
        setError(addCourseError, "");
        var file = this.files && this.files[0] ? this.files[0] : null;
        if (!file) {
          courseCoverPreview.classList.add("hidden");
          courseCoverPreview.removeAttribute("src");
          return;
        }
        if (!file.type || file.type.indexOf("image/") !== 0) {
          setError(addCourseError, i18n.errorInvalidImageFile);
          this.value = "";
          courseCoverPreview.classList.add("hidden");
          courseCoverPreview.removeAttribute("src");
          return;
        }
        var reader = new FileReader();
        reader.onload = function () {
          removeCourseCover = false;
          courseCoverPreview.src = String(reader.result || "");
          courseCoverPreview.classList.remove("hidden");
          if (removeCourseCoverBtn)
            removeCourseCoverBtn.classList.remove("hidden");
        };
        reader.readAsDataURL(file);
      });
    }

    if (targetSchoolYearsOptions) {
      targetSchoolYearsOptions.addEventListener("click", function (e) {
        var t = e.target;
        var btn = t && t.closest ? t.closest("[data-target-kind]") : null;
        if (!btn) return;
        var kind = btn.getAttribute("data-target-kind");
        var value = btn.getAttribute("data-target-value");
        if (!kind || !value) return;
        if (kind === "year")
          selectedSchoolYears[value] = !selectedSchoolYears[value];
        renderTargetPickers();
      });
    }

    if (targetClassesOptions) {
      targetClassesOptions.addEventListener("click", function (e) {
        var t = e.target;
        var btn = t && t.closest ? t.closest("[data-target-kind]") : null;
        if (!btn) return;
        var kind = btn.getAttribute("data-target-kind");
        var value = btn.getAttribute("data-target-value");
        if (!kind || !value) return;
        if (kind === "class") selectedClasses[value] = !selectedClasses[value];
        renderTargetPickers();
      });
    }

    if (confirmDeleteCancel)
      confirmDeleteCancel.addEventListener("click", closeConfirm);
    if (confirmDeleteConfirm)
      confirmDeleteConfirm.addEventListener("click", function () {
        if (pendingConfirmAction) pendingConfirmAction();
        closeConfirm();
      });

    document.addEventListener("keydown", function (e) {
      if (!e || e.key !== "Escape") return;
      if (
        confirmDeleteModal &&
        !confirmDeleteModal.classList.contains("hidden")
      ) {
        closeConfirm();
        return;
      }
      if (addCourseModal && !addCourseModal.classList.contains("hidden")) {
        closeAddCourse();
      }
    });

    document.addEventListener("click", function (e) {
      if (!courseList) return;
      var t = e && e.target ? e.target : null;
      if (!t || !t.closest) return;
      var insideDropdown = t.closest("[data-course-dropdown]");
      if (insideDropdown) return;
      closeAllCourseMenus();
    });
  }

  var serverCourses = [];
  try {
    var initialRaw = root.getAttribute("data-initial-courses") || "";
    var parsed = safeJsonParse(initialRaw);
    if (Array.isArray(parsed)) serverCourses = parsed;
  } catch (e) {}

  loadState();
  var merged = [];
  for (var j = 0; j < serverCourses.length; j++) {
    merged.push(ensureCourseShape(serverCourses[j]));
  }
  state.courses = merged;

  normalizeState();
  saveState();
  renderTargetPickers();
  attachEvents();
  renderList();
})();
