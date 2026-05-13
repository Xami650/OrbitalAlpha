async function loadRisks() {
    await checkMlStatus();

    const response = await fetch("/api/risks");
    const risks = await response.json();

    const container = document.getElementById("risk-container");
    container.innerHTML = "";

    risks.forEach(risk => {
        const card = document.createElement("article");
        card.className = `card risk-${risk.riskLevel.toLowerCase()}`;

        card.innerHTML = `
            <h2>
                ${commodityName(risk.commodity)}
                <span class="ticker">(${risk.commodity})</span>
            </h2>
            <p><strong>Risk:</strong> ${risk.riskLevel}</p>
            <p><strong>Score:</strong> ${risk.riskScore}</p>
            <p><strong>Reason:</strong> ${risk.reason}</p>
        `;

        container.appendChild(card);
    });
}

async function checkMlStatus() {
    const banner = document.getElementById("fallback-banner");
    try {
        const response = await fetch("/api/status");
        const status = await response.json();
        banner.classList.toggle("hidden", status.mlAvailable === true);
    } catch {
        banner.classList.remove("hidden");
    }
}

function commodityName(symbol) {
    const names = {
        WEAT: "Wheat",
        CORN: "Corn",
        SOYB: "Soybeans",
        JO: "Coffee",
        UNG: "Natural Gas"
    };

    return names[symbol] || symbol;
}

loadRisks();
