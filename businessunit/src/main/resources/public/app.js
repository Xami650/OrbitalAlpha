async function loadRisks() {
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