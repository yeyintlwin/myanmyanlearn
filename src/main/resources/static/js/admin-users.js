(function () {
  var modal = document.getElementById("assignAdminModal");
  var targetEl = document.getElementById("assignAdminTargetUserId");
  var cancelBtn = document.getElementById("assignAdminCancel");
  var confirmBtn = document.getElementById("assignAdminConfirm");
  if (!modal || !targetEl || !cancelBtn || !confirmBtn) return;

  var pendingForm = null;

  function closeModal() {
    modal.classList.add("hidden");
    pendingForm = null;
    targetEl.textContent = "";
  }

  cancelBtn.addEventListener("click", function () {
    closeModal();
  });

  modal.addEventListener("click", function (e) {
    if (e.target === modal) closeModal();
  });

  confirmBtn.addEventListener("click", function () {
    if (pendingForm) pendingForm.submit();
  });

  window.confirmAdminAssign = function (form) {
    try {
      var select = form.querySelector('select[name="role"]');
      if (!select || select.value !== "ADMIN") return true;

      var userIdInput = form.querySelector('input[name="userId"]');
      targetEl.textContent = userIdInput ? userIdInput.value : "this user";
      pendingForm = form;
      modal.classList.remove("hidden");
      return false;
    } catch (e) {
      return true;
    }
  };
})();

(function () {
  var toast = document.getElementById("adminAsyncToast");
  var toastText = document.getElementById("adminAsyncToastText");
  var toastTimer = null;

  function showToast(message, ok) {
    if (!toast || !toastText) return;
    toastText.textContent = message || (ok ? "Saved" : "Failed to save");
    toast.classList.remove("hidden");
    toast.classList.add("flex");
    toast.classList.toggle("border-emerald-500/30", !!ok);
    toast.classList.toggle("border-rose-500/30", !ok);
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(
      function () {
        toast.classList.add("hidden");
        toast.classList.remove("flex");
      },
      ok ? 1200 : 2500,
    );
  }

  window.submitAsyncForm = function (form) {
    try {
      if (!form) return;
      if (!form.hasAttribute("data-async")) {
        form.submit();
        return;
      }
      if (form.dataset.submitting === "true") return;
      form.dataset.submitting = "true";

      var action = form.getAttribute("action") || form.action;
      var method = (form.getAttribute("method") || "GET").toUpperCase();
      if (method !== "POST" || !action) {
        form.submit();
        return;
      }

      fetch(action, {
        method: "POST",
        body: new FormData(form),
        credentials: "same-origin",
        headers: { "X-Requested-With": "XMLHttpRequest" },
      })
        .then(function (res) {
          if (res && res.ok) {
            showToast("Saved", true);
          } else {
            showToast("Failed to save", false);
          }
        })
        .catch(function () {
          showToast("Failed to save", false);
        })
        .finally(function () {
          form.dataset.submitting = "false";
        });
    } catch (e) {
      try {
        form.submit();
      } catch (e2) {}
    }
  };
})();
