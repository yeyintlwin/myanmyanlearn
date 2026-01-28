(function () {
  var root = document.getElementById("coursesManagementRoot");
  if (!root) return;

  var storageKey =
    root.getAttribute("data-storage-key") || "adminCoursesDraft_v1";
  var state = { version: 2, order: "oldest-first", courses: [] };

  function csrfHeaders() {
    try {
      var tokenMeta = document.querySelector('meta[name="_csrf"]');
      var headerMeta = document.querySelector('meta[name="_csrf_header"]');
      var token = tokenMeta ? tokenMeta.getAttribute("content") : null;
      var headerName = headerMeta ? headerMeta.getAttribute("content") : null;
      if (!token || !headerName) return {};
      var headers = {};
      headers[headerName] = token;
      return headers;
    } catch (e) {
      return {};
    }
  }

  function apiFetch(url, opts) {
    var options = opts && typeof opts === "object" ? opts : {};
    if (!options.headers) options.headers = {};
    var csrf = csrfHeaders();
    var keys = Object.keys(csrf);
    for (var i = 0; i < keys.length; i++) {
      options.headers[keys[i]] = csrf[keys[i]];
    }
    options.credentials = "same-origin";
    return fetch(url, options);
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
      var raw = localStorage.getItem(storageKey);
      if (!raw) return;
      var loaded = safeJsonParse(raw);
      if (!loaded) return;
      if (Array.isArray(loaded)) {
        state.courses = loaded;
        state.order = "oldest-first";
        state.version = 2;
        state.courses.reverse();
        return;
      }
      if (
        loaded &&
        typeof loaded === "object" &&
        Array.isArray(loaded.courses)
      ) {
        state.courses = loaded.courses;
        state.order =
          typeof loaded.order === "string" && loaded.order.trim()
            ? loaded.order
            : "oldest-first";
        state.version =
          typeof loaded.version === "number" && loaded.version > 0
            ? loaded.version
            : 1;
        if (state.order !== "oldest-first" || state.version < 2) {
          state.courses.reverse();
          state.order = "oldest-first";
          state.version = 2;
        }
      }
    } catch (e) {}
  }

  function saveState() {
    try {
      if (!storageKey) return;
      localStorage.setItem(storageKey, JSON.stringify(state));
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
    return apiFetch("/api/admin/courses", {
      method: "GET",
      headers: { Accept: "application/json" },
    }).then(function (res) {
      if (!res || !res.ok) throw new Error("Failed to load courses");
      return res.json();
    });
  }

  function saveCourseMetaToServer(course) {
    if (!course || !course.id) return Promise.reject(new Error("Missing id"));
    return apiFetch("/api/admin/courses/" + encodeURIComponent(course.id) + "/meta", {
      method: "PUT",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({
        id: course.id,
        title: course.title || "Untitled course",
        description: course.description || "",
        language: course.language || "",
        published: !!course.published,
        targetStudents: course.targetStudents || { schoolYears: [], classes: [] },
        coverImageDataUrl: course.coverImageDataUrl || null,
      }),
    }).then(function (res) {
      if (!res || !res.ok) throw new Error("Failed to save course");
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
    if (confirmDeleteTitle) confirmDeleteTitle.textContent = title || "Confirm";
    if (confirmDeleteMessage)
      confirmDeleteMessage.textContent = message || "Are you sure?";
    if (confirmDeleteConfirm) {
      confirmDeleteConfirm.textContent = confirmLabel || confirmDefaultText;
      confirmDeleteConfirm.className =
        confirmClassName || confirmDefaultClassName;
    }
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
    var ts =
      targetStudents && typeof targetStudents === "object"
        ? targetStudents
        : {};
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
    if (!course.title) course.title = "Untitled course";
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
    if (!course) return "All students";
    var ts = normalizeTargetStudents(course.targetStudents);
    var parts = [];
    if (ts.schoolYears.length)
      parts.push("Years: " + ts.schoolYears.join(", "));
    if (ts.classes.length) parts.push("Classes: " + ts.classes.join(", "));
    return parts.length ? parts.join(" · ") : "All students";
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
        img.alt = course.title || "Course cover";
        cover.appendChild(img);
      } else {
        cover.className += " bg-white/10";
        cover.innerHTML = '<i class="fas fa-book text-white/50 text-xl"></i>';
      }

      var text = document.createElement("div");
      text.className = "min-w-0 flex-1";

      var title = document.createElement("div");
      title.className = "text-white font-semibold leading-tight break-words";
      title.textContent = course.title || "Untitled course";

      var description = document.createElement("div");
      description.className = "mt-1 text-white/70 text-sm break-words";
      description.textContent =
        course.description || "No description available.";

      var meta = document.createElement("div");
      meta.className = "mt-2 flex items-center gap-2 flex-wrap";

      var lang = document.createElement("span");
      lang.className =
        "inline-flex items-center rounded-full border border-sky-500/30 bg-sky-500/10 px-2.5 h-7 text-[11px] font-semibold text-sky-100";
      lang.textContent = languageShort(course.language) || "N/A";

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
        ? '<i class="fas fa-eye text-[10px]"></i><span>Published</span>'
        : '<i class="fas fa-eye-slash text-[10px]"></i><span>Private</span>';

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
        '" class="block w-full text-left px-4 py-2 text-sm text-white/90 hover:bg-white/10 transition">Edit</button>' +
        '<button type="button" data-course-action="delete" data-course-id="' +
        escapeHtml(course.id) +
        '" class="block w-full text-left px-4 py-2 text-sm text-rose-100 hover:bg-rose-500/10 transition">Delete</button>' +
        '<button type="button" data-course-action="export" data-course-id="' +
        escapeHtml(course.id) +
        '" class="block w-full text-left px-4 py-2 text-sm text-white/90 hover:bg-white/10 transition">Export</button>';

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
    if (addCourseTitleEl) addCourseTitleEl.textContent = "Add course";
    if (addCourseSubmitBtn) addCourseSubmitBtn.textContent = "Create course";
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
    if (addCourseTitleEl) addCourseTitleEl.textContent = "Edit course";
    if (addCourseSubmitBtn) addCourseSubmitBtn.textContent = "Save changes";
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
    course.title = title || "Untitled course";
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
      setError(addCourseError, "Course title is required.");
      return;
    }

    var existing = editingCourseId ? findCourseById(editingCourseId) : null;
    var course =
      existing || ensureCourseShape({ id: uniqueCourseIdFromTitle(title) });

    var file =
      courseCoverInput && courseCoverInput.files && courseCoverInput.files[0]
        ? courseCoverInput.files[0]
        : null;

    var finalize = function (coverDataUrl) {
      updateCourseFromForm(course, coverDataUrl);
      var optimistic = !existing;
      if (optimistic) state.courses.push(course);
      normalizeState();
      renderList();
      saveCourseMetaToServer(course)
        .then(function () {
          return fetchCoursesFromServer();
        })
        .then(function (courses) {
          state.courses = courses;
          normalizeState();
          saveState();
          renderList();
          closeAddCourse();
          showFlash(
            "success",
            existing ? "Course updated." : "Course created.",
          );
        })
        .catch(function () {
          if (optimistic) deleteCourseById(course.id);
          showFlash("error", "Failed to save course to database.");
        });
    };

    if (!file) {
      finalize(null);
      return;
    }
    if (!file.type || file.type.indexOf("image/") !== 0) {
      setError(addCourseError, "Please select a valid image file.");
      if (courseCoverInput) courseCoverInput.value = "";
      return;
    }
    var reader = new FileReader();
    reader.onload = function () {
      removeCourseCover = false;
      finalize(String(reader.result || ""));
    };
    reader.onerror = function () {
      setError(addCourseError, "Failed to read cover image.");
    };
    reader.readAsDataURL(file);
  }

  function exportCourses(coursesToExport, filenamePrefix) {
    var list = Array.isArray(coursesToExport) ? coursesToExport : state.courses;
    if (!list.length) {
      showFlash("info", "No courses to export.");
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
      list.length === 1 ? "Course exported." : "Courses exported.",
    );
  }

  function exportCourseById(courseId) {
    var course = findCourseById(courseId);
    if (!course) {
      showFlash("error", "Course not found.");
      return;
    }
    exportCourses([course], "course_export");
  }

  function parseImportedCourses(parsed) {
    if (Array.isArray(parsed)) return parsed;
    if (parsed && typeof parsed === "object" && Array.isArray(parsed.courses))
      return parsed.courses;
    return null;
  }

  function importCoursesFromFile(file) {
    if (!file) return;
    var reader = new FileReader();
    reader.onload = function () {
      var parsed = safeJsonParse(String(reader.result || ""));
      var courses = parseImportedCourses(parsed);
      if (!courses) {
        showFlash("error", "Invalid import file format.");
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
          return fetchCoursesFromServer();
        })
        .then(function (fresh) {
          state.courses = fresh;
          normalizeState();
          saveState();
          renderList();
          showFlash("success", "Courses imported.");
        })
        .catch(function () {
          showFlash("error", "Import saved locally, but DB save failed.");
          saveState();
        });
    };
    reader.onerror = function () {
      showFlash("error", "Failed to read import file.");
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
                next ? "Course published." : "Course set to private.",
              );
            })
            .catch(function () {
              course.published = !next;
              normalizeState();
              renderList();
              showFlash("error", "Failed to update publish state in database.");
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
          var course = findCourseById(courseId);
          configureConfirm(
            "Delete course",
            'Delete "' +
              (course && course.title ? course.title : "this course") +
              '"?',
            "Delete",
            confirmDefaultClassName,
            function () {
              deleteCourseOnServer(courseId)
                .then(function () {
                  deleteCourseById(courseId);
                  showFlash("success", "Course deleted.");
                })
                .catch(function () {
                  showFlash("error", "Failed to delete course from database.");
                });
            },
          );
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
          setError(addCourseError, "Please select a valid image file.");
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

  renderTargetPickers();
  attachEvents();
  fetchCoursesFromServer()
    .then(function (courses) {
      state.courses = courses;
      normalizeState();
      saveState();
      renderList();
    })
    .catch(function () {
      loadState();
      normalizeState();
      saveState();
      renderList();
      showFlash("error", "Failed to load courses from database.");
    });
})();
