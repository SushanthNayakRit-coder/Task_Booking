(function () {
  const API = '/api';
  const opts = { credentials: 'same-origin', headers: { 'Content-Type': 'application/json' } };

  function get(path) {
    return fetch(API + path, { credentials: 'same-origin' }).then(r => {
      if (r.status === 401) { window.location.href = '/login'; return []; }
      return r.json();
    });
  }

  function post(path, body) {
    return fetch(API + path, { ...opts, method: 'POST', body: JSON.stringify(body) }).then(async r => {
      if (r.status === 401) { window.location.href = '/login'; throw new Error('Unauthorized'); }
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.error || data.details || r.statusText);
      return data;
    });
  }

  function put(path, body) {
    return fetch(API + path, { ...opts, method: 'PUT', body: JSON.stringify(body) }).then(async r => {
      if (r.status === 401) { window.location.href = '/login'; throw new Error('Unauthorized'); }
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.error || r.statusText);
      return data;
    });
  }

  window.taskBookingApi = { get, post, put };

  // Current user in nav
  get('/auth/me').then(user => {
    const el = document.getElementById('userInfo');
    if (el && user && user.name) el.textContent = user.name + ' (' + user.role + ')';
  }).catch(() => {});

  // Dashboard counts and recent
  const pendingCount = document.getElementById('pendingCount');
  const approvedCount = document.getElementById('approvedCount');
  const rejectedCount = document.getElementById('rejectedCount');
  const recentTasks = document.getElementById('recentTasks');
  if (pendingCount || recentTasks) {
    Promise.all([
      get('/tasks?status=PENDING'),
      get('/tasks?status=APPROVED'),
      get('/tasks?status=REJECTED'),
      get('/tasks?sortBy=date')
    ]).then(([pending, approved, rejected, all]) => {
      if (pendingCount) pendingCount.textContent = pending.length;
      if (approvedCount) approvedCount.textContent = approved.length;
      if (rejectedCount) rejectedCount.textContent = rejected.length;
      if (recentTasks) {
        const list = (all || []).slice(0, 5);
        recentTasks.innerHTML = list.length === 0
          ? '<p class="text-muted">No tasks yet.</p>'
          : '<div class="task-list">' + list.map(t => renderTaskRow(t, false)).join('') + '</div>';
      }
    }).catch(() => { if (recentTasks) recentTasks.innerHTML = '<p>Failed to load.</p>'; });
  }

  function formatDate(d) {
    if (!d) return '-';
    const dt = new Date(d);
    return dt.toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' });
  }

  function renderTaskRow(t, showActions) {
    const canApprove = window._currentUser && window._currentUser.role === 'MANAGER';
    let actions = '';
    if (showActions && canApprove && t.status === 'PENDING') {
      actions = '<div class="task-actions">' +
        '<button type="button" class="approve" data-id="' + t.id + '">Approve</button>' +
        '<button type="button" class="reject" data-id="' + t.id + '">Reject</button>' +
        '</div>';
    }
    return '<div class="task-item" data-id="' + t.id + '">' +
      '<div><div class="task-title">' + escapeHtml(t.title) + '</div>' +
      '<div class="task-meta">' + formatDate(t.dateTime) + ' &middot; ' + (t.priority || '') + (t.assignedUserName ? ' &middot; ' + escapeHtml(t.assignedUserName) : '') + '</div></div>' +
      '<div><span class="status ' + (t.status || '') + '">' + (t.status || '') + '</span> ' + actions + '</div>' +
      '</div>';
  }

  function escapeHtml(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
  }

  // Tasks list page: filter, sort, list, approve/reject
  const taskList = document.getElementById('taskList');
  const filterStatus = document.getElementById('filterStatus');
  const sortBy = document.getElementById('sortBy');
  const applyFilters = document.getElementById('applyFilters');

  function loadTaskList() {
    if (!taskList) return;
    const status = (filterStatus && filterStatus.value) || '';
    const sort = (sortBy && sortBy.value) || 'date';
    const path = '/tasks' + (status ? '?status=' + encodeURIComponent(status) : '') + (status ? '&' : '?') + 'sortBy=' + encodeURIComponent(sort) + '&_=' + Date.now();
    taskList.innerHTML = 'Loading...';
    get(path).then(tasks => {
      taskList.innerHTML = '<div class="task-list">' + (tasks || []).map(t => renderTaskRow(t, true)).join('') + '</div>';
      taskList.querySelectorAll('.task-actions .approve').forEach(btn => {
        btn.addEventListener('click', () => approveReject(btn.dataset.id, 'APPROVED'));
      });
      taskList.querySelectorAll('.task-actions .reject').forEach(btn => {
        btn.addEventListener('click', () => approveReject(btn.dataset.id, 'REJECTED'));
      });
      if (tasks.length === 0) taskList.innerHTML = '<p class="text-muted">No tasks match the filter.</p>';
    }).catch(() => { taskList.innerHTML = 'Failed to load tasks.'; });
  }

  function approveReject(id, status) {
    put('/tasks/' + id + '/approve', { status: status }).then(() => loadTaskList()).catch(err => {
      alert(err.message || 'Only Manager can approve or reject.');
    });
  }

  if (applyFilters) applyFilters.addEventListener('click', loadTaskList);
  if (filterStatus) filterStatus.addEventListener('change', loadTaskList);
  if (sortBy) sortBy.addEventListener('change', loadTaskList);
  // Load current user first so Approve/Reject buttons show for Manager/Admin (avoid race)
  if (taskList && !recentTasks) {
    get('/auth/me').then(function (u) {
      window._currentUser = u;
      loadTaskList();
    }).catch(function () {
      window._currentUser = null;
      loadTaskList();
    });
  }

  // Task form: load users, submit
  const taskForm = document.getElementById('taskForm');
  const assignedUserId = document.getElementById('assignedUserId');
  if (assignedUserId) {
    get('/users').then(users => {
      (users || []).forEach(u => {
        const opt = document.createElement('option');
        opt.value = u.id;
        opt.textContent = u.name + ' (' + u.role + ')';
        assignedUserId.appendChild(opt);
      });
    });
  }
  if (taskForm) {
    taskForm.addEventListener('submit', function (e) {
      e.preventDefault();
      const msg = document.getElementById('formMessage');
      const dateTime = document.getElementById('dateTime').value;
      const payload = {
        title: document.getElementById('title').value.trim(),
        description: document.getElementById('description').value.trim(),
        dateTime: dateTime ? new Date(dateTime).toISOString().slice(0, 19) : null,
        priority: document.getElementById('priority').value,
        assignedUserId: parseInt(document.getElementById('assignedUserId').value, 10)
      };
      post('/tasks', payload).then(() => {
        if (msg) msg.innerHTML = '<span class="alert alert-success">Task created.</span>';
        taskForm.reset();
        setTimeout(() => { window.location.href = '/tasks'; }, 1000);
      }).catch(err => {
        const text = (err && err.message) || (err && err.error) || 'Failed to create task.';
        if (msg) msg.innerHTML = '<span class="alert alert-error">' + escapeHtml(String(text)) + '</span>';
      });
    });
  }

  // Calendar
  const calendarTitle = document.getElementById('calendarTitle');
  const calendarGrid = document.getElementById('calendarGrid');
  const calendarTasks = document.getElementById('calendarTasks');
  let calendarYear, calendarMonth;
  if (calendarGrid) {
    const now = new Date();
    calendarYear = now.getFullYear();
    calendarMonth = now.getMonth() + 1;
    function renderCalendar() {
      get('/tasks/calendar?year=' + calendarYear + '&month=' + calendarMonth).then(tasks => {
        const first = new Date(calendarYear, calendarMonth - 1, 1);
        const last = new Date(calendarYear, calendarMonth, 0);
        const startDay = first.getDay();
        const daysInMonth = last.getDate();
        const prevMonth = new Date(calendarYear, calendarMonth - 2, 1);
        const prevDays = new Date(calendarYear, calendarMonth - 1, 0).getDate();
        const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December'];
        if (calendarTitle) calendarTitle.textContent = monthNames[calendarMonth - 1] + ' ' + calendarYear;
        const byDay = {};
        (tasks || []).forEach(t => {
          const d = t.dateTime ? new Date(t.dateTime).getDate() : 0;
          if (!byDay[d]) byDay[d] = [];
          byDay[d].push(t);
        });
        let html = '';
        for (let i = 0; i < startDay; i++) {
          const d = prevDays - startDay + i + 1;
          html += '<div class="calendar-day other-month">' + d + '</div>';
        }
        for (let d = 1; d <= daysInMonth; d++) {
          const dayTasks = byDay[d] || [];
          const dots = dayTasks.slice(0, 5).map(t => '<span class="task-dot" style="background:' + (t.status === 'APPROVED' ? 'var(--success)' : t.status === 'REJECTED' ? 'var(--danger)' : 'var(--warning)') + '"></span>').join('');
          html += '<div class="calendar-day" data-day="' + d + '"><div class="day-num">' + d + '</div>' + dots + '</div>';
        }
        const totalCells = Math.ceil((startDay + daysInMonth) / 7) * 7;
        for (let i = startDay + daysInMonth; i < totalCells; i++) {
          html += '<div class="calendar-day other-month">' + (i - startDay - daysInMonth + 1) + '</div>';
        }
        calendarGrid.innerHTML = html;
        calendarTasks.innerHTML = '<h3>Tasks this month</h3><ul>' + (tasks || []).map(t =>
          '<li><strong>' + escapeHtml(t.title) + '</strong> ' + formatDate(t.dateTime) + ' <span class="status ' + t.status + '">' + t.status + '</span></li>'
        ).join('') + '</ul>';
      });
    }
    renderCalendar();
    if (document.getElementById('prevMonth')) document.getElementById('prevMonth').addEventListener('click', () => {
      calendarMonth--;
      if (calendarMonth < 1) { calendarMonth = 12; calendarYear--; }
      renderCalendar();
    });
    if (document.getElementById('nextMonth')) document.getElementById('nextMonth').addEventListener('click', () => {
      calendarMonth++;
      if (calendarMonth > 12) { calendarMonth = 1; calendarYear++; }
      renderCalendar();
    });
  }

  // Set current user for other pages (e.g. dashboard); Tasks page sets it before loadTaskList
  get('/auth/me').then(function (u) { window._currentUser = u; }).catch(function () { window._currentUser = null; });
})();
