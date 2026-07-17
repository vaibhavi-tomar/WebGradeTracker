// All requests go to the same origin the page was served from,
// since the Java Server serves both the frontend files and the API.
const API_BASE = '/api';

const form = document.getElementById('student-form');
const formMessage = document.getElementById('form-message');
const tbody = document.getElementById('students-tbody');

document.addEventListener('DOMContentLoaded', () => {
  refreshEverything();
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  formMessage.textContent = '';
  formMessage.className = 'form-message';

  const name = document.getElementById('name').value.trim();
  const roll = parseInt(document.getElementById('roll').value, 10);
  const marksRaw = document.getElementById('marks').value.trim();

  const marks = marksRaw
    .split(',')
    .map(part => part.trim())
    .filter(part => part.length > 0)
    .map(Number);

  if (marks.some(isNaN)) {
    showFormMessage('Marks must be numbers, separated by commas.', 'error');
    return;
  }
  if (marks.length === 0) {
    showFormMessage('Enter at least one mark.', 'error');
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/students`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, rollNumber: roll, marks })
    });
    const data = await response.json();

    if (!response.ok) {
      showFormMessage(data.error || 'Could not add student.', 'error');
      return;
    }

    showFormMessage(`Added ${data.name} to the ledger.`, 'success');
    form.reset();
    refreshEverything();

  } catch (err) {
    showFormMessage('Could not reach the server. Is it running?', 'error');
  }
});

function showFormMessage(text, type) {
  formMessage.textContent = text;
  formMessage.className = `form-message ${type}`;
}

async function refreshEverything() {
  await Promise.all([loadStudents(), loadSummary()]);
}

// ---------- Ledger table ----------

async function loadStudents() {
  try {
    const response = await fetch(`${API_BASE}/students`);
    const students = await response.json();
    renderStudents(students);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-row">Could not load data. Is the server running?</td></tr>`;
  }
}

function renderStudents(students) {
  if (students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-row">No entries yet. Register a student above.</td></tr>`;
    return;
  }

  tbody.innerHTML = students.map(s => `
    <tr>
      <td><span class="roll-badge">${s.rollNumber}</span></td>
      <td>${escapeHtml(s.name)}</td>
      <td>${s.average.toFixed(2)}</td>
      <td>${s.highest}</td>
      <td>${s.lowest}</td>
      <td><span class="grade-pill ${gradeClass(s.grade)}">${s.grade}</span></td>
      <td><button class="delete-btn" data-roll="${s.rollNumber}">Remove</button></td>
    </tr>
  `).join('');

  tbody.querySelectorAll('.delete-btn').forEach(btn => {
    btn.addEventListener('click', () => deleteStudent(btn.dataset.roll));
  });
}

function gradeClass(grade) {
  if (grade === 'A+' || grade === 'A') return 'grade-good';
  if (grade === 'B' || grade === 'C') return 'grade-mid';
  return 'grade-low';
}

async function deleteStudent(roll) {
  try {
    const response = await fetch(`${API_BASE}/students/${roll}`, { method: 'DELETE' });
    if (response.ok) {
      refreshEverything();
    }
  } catch (err) {
    console.error('Delete failed', err);
  }
}

// ---------- Report ----------

async function loadSummary() {
  const emptyEl = document.getElementById('report-empty');
  const contentEl = document.getElementById('report-content');

  try {
    const response = await fetch(`${API_BASE}/summary`);
    const summary = await response.json();

    if (!summary.totalStudents) {
      emptyEl.classList.remove('hidden');
      contentEl.classList.add('hidden');
      return;
    }

    emptyEl.classList.add('hidden');
    contentEl.classList.remove('hidden');

    document.getElementById('stat-total').textContent = summary.totalStudents;
    document.getElementById('stat-average').textContent = summary.classAverage.toFixed(2);

    document.getElementById('topper-name').textContent = summary.topper.name;
    document.getElementById('topper-average').textContent = `Average ${summary.topper.average.toFixed(2)} · Grade ${summary.topper.grade}`;

    document.getElementById('weakest-name').textContent = summary.weakest.name;
    document.getElementById('weakest-average').textContent = `Average ${summary.weakest.average.toFixed(2)} · Grade ${summary.weakest.grade}`;

    const rankingList = document.getElementById('ranking-list');
    rankingList.innerHTML = summary.ranked.map(r => `
      <li>
        <span class="rank-num">#${r.rank}</span>
        <span class="rank-name">${escapeHtml(r.name)}</span>
        <span>${r.average.toFixed(2)} · ${r.grade}</span>
      </li>
    `).join('');

  } catch (err) {
    console.error('Could not load summary', err);
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
