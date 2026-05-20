const COMMODITIES = {
    WEAT: { name: "Wheat",       icon: "\u{1F33E}" },
    CORN: { name: "Corn",        icon: "\u{1F33D}" },
    SOYB: { name: "Soybeans",    icon: "\u{1FAD8}" },
    JO:   { name: "Coffee",      icon: "☕" },
    UNG:  { name: "Natural Gas", icon: "⛽" },
};

const TIER_CONFIG = {
    "LOW":         { css: "low",         label: "Low",      severity: 20 },
    "LOW_MEDIUM":  { css: "low-medium",  label: "Low-Med",  severity: 40 },
    "MEDIUM":      { css: "medium",      label: "Medium",   severity: 60 },
    "MEDIUM_HIGH": { css: "medium-high", label: "Med-High", severity: 80 },
    "HIGH":        { css: "high",        label: "High",     severity: 100 },
};

const GAUGE_CIRCUMFERENCE = 2 * Math.PI * 38;

let allRisks = [];
let activeFilter = "ALL";

document.addEventListener("DOMContentLoaded", () => {
    loadDashboard();
});

async function loadDashboard() {
    showLoading(true);
    await Promise.all([checkApiStatus(), checkMlStatus()]);
    await loadRisks();
    showLoading(false);
}

async function refreshDashboard() {
    const btn = document.getElementById("refresh-btn");
    btn.classList.add("refreshing");
    await loadDashboard();
    setTimeout(() => btn.classList.remove("refreshing"), 600);
}

async function loadRisks() {
    try {
        const response = await fetch("/api/risks");
        if (!response.ok) throw new Error("API error");
        allRisks = await response.json();
        renderCards(filterRisks());
        updateSummary();
        updateTimestamp();
    } catch {
        allRisks = [];
        renderCards([]);
        updateSummary();
        showAlert("Could not load risk data. Is the Business Unit running?");
    }
}

async function checkApiStatus() {
    const chip = document.getElementById("api-status");
    try {
        const res = await fetch("/api/health");
        if (res.ok) {
            chip.className = "status-chip status-online";
        } else {
            chip.className = "status-chip status-offline";
        }
    } catch {
        chip.className = "status-chip status-offline";
    }
}

async function checkMlStatus() {
    const chip = document.getElementById("ml-status");
    const banner = document.getElementById("alert-banner");
    try {
        const res = await fetch("/api/status");
        const status = await res.json();
        if (status.mlAvailable) {
            chip.className = "status-chip status-online";
            banner.classList.add("hidden");
        } else {
            chip.className = "status-chip status-offline";
            showAlert("ML API unavailable — predictions may use heuristic fallback.");
        }
    } catch {
        chip.className = "status-chip status-offline";
    }
}

function showAlert(message) {
    const banner = document.getElementById("alert-banner");
    document.getElementById("alert-text").textContent = message;
    banner.classList.remove("hidden");
}

function setFilter(filter, el) {
    activeFilter = filter;
    document.querySelectorAll(".filter-chip").forEach(c => c.classList.remove("active"));
    el.classList.add("active");
    renderCards(filterRisks());
}

function filterRisks() {
    if (activeFilter === "ALL") return allRisks;
    return allRisks.filter(r => r.commodity === activeFilter);
}

function tierFor(risk) {
    return TIER_CONFIG[risk.riskLevel] || TIER_CONFIG["MEDIUM"];
}

function renderCards(risks) {
    const container = document.getElementById("risk-container");
    const empty = document.getElementById("empty-state");

    if (risks.length === 0) {
        container.innerHTML = "";
        empty.classList.remove("hidden");
        return;
    }

    empty.classList.add("hidden");
    container.innerHTML = risks.map((risk, i) => buildCard(risk, i)).join("");

    requestAnimationFrame(() => {
        document.querySelectorAll(".gauge-fill").forEach(el => {
            el.style.strokeDashoffset = el.dataset.offset;
        });
        document.querySelectorAll(".gauge-bar-value").forEach(el => {
            el.style.width = el.dataset.width;
        });
    });
}

function buildCard(risk, index) {
    const tier = tierFor(risk);
    const info = COMMODITIES[risk.commodity] || { name: risk.commodity, icon: "\u{1F4CA}" };
    const score = Math.round(risk.riskScore);
    const offset = GAUGE_CIRCUMFERENCE - (score / 100) * GAUGE_CIRCUMFERENCE;

    return `
    <article class="risk-card risk-${tier.css}" style="animation-delay: ${index * 0.08}s">
        <div class="card-header">
            <div class="card-identity">
                <div class="card-icon">${info.icon}</div>
                <div>
                    <div class="card-name">${info.name}</div>
                    <div class="card-ticker">${risk.commodity}</div>
                </div>
            </div>
            <span class="risk-badge badge-${tier.css}">${tier.label}</span>
        </div>

        <div class="card-gauge">
            <div class="gauge-ring">
                <svg viewBox="0 0 84 84">
                    <circle class="gauge-bg" cx="42" cy="42" r="38"/>
                    <circle class="gauge-fill"
                        cx="42" cy="42" r="38"
                        stroke-dasharray="${GAUGE_CIRCUMFERENCE}"
                        stroke-dashoffset="${GAUGE_CIRCUMFERENCE}"
                        data-offset="${offset}"/>
                </svg>
                <div class="gauge-center">
                    <div class="gauge-value">${score}</div>
                    <div class="gauge-label">Score</div>
                </div>
            </div>
            <div class="gauge-details">
                <div class="gauge-bar-container">
                    <div class="gauge-bar-label">
                        <span>Risk Score</span>
                        <span>${score}/100</span>
                    </div>
                    <div class="gauge-bar-track">
                        <div class="gauge-bar-value" data-width="${score}%"></div>
                    </div>
                </div>
                <div class="gauge-bar-container">
                    <div class="gauge-bar-label">
                        <span>Severity</span>
                        <span>${tier.label}</span>
                    </div>
                    <div class="gauge-bar-track">
                        <div class="gauge-bar-value" data-width="${tier.severity}%"></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="card-reason">
            <strong>Analysis:</strong> ${escapeHtml(risk.reason)}
        </div>
    </article>`;
}

function updateSummary() {
    const total = allRisks.length;
    const counts = { high: 0, "medium-high": 0, medium: 0, "low-medium": 0, low: 0 };

    allRisks.forEach(r => {
        const tier = tierFor(r);
        counts[tier.css]++;
    });

    const avg = total > 0
        ? Math.round(allRisks.reduce((s, r) => s + r.riskScore, 0) / total)
        : "—";

    animateValue("total-count", total);
    animateValue("high-count", counts["high"]);
    animateValue("medium-high-count", counts["medium-high"]);
    animateValue("medium-count", counts["medium"]);
    animateValue("low-medium-count", counts["low-medium"]);
    animateValue("low-count", counts["low"]);
    animateValue("avg-score", avg);
}

function animateValue(id, target) {
    const el = document.getElementById(id);
    if (typeof target !== "number") { el.textContent = target; return; }

    const start = parseInt(el.textContent) || 0;
    const duration = 600;
    const startTime = performance.now();

    function step(now) {
        const progress = Math.min((now - startTime) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(start + (target - start) * eased);
        if (progress < 1) requestAnimationFrame(step);
    }

    requestAnimationFrame(step);
}

function updateTimestamp() {
    const el = document.getElementById("last-update");
    const now = new Date();
    el.textContent = "Last update: " + now.toLocaleTimeString();
}

function showLoading(show) {
    document.getElementById("loading-state").classList.toggle("hidden", !show);
    if (show) {
        document.getElementById("risk-container").innerHTML = "";
        document.getElementById("empty-state").classList.add("hidden");
    }
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
}
