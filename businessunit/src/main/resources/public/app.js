async function loadRisks() {
    const response = await fetch("/api/risks");
    const risks = await response.json();

    const container = document.getElementById("risk-container");
    container.innerHTML = "";

    risks.forEach(risk => {
        const card = document.createElement("article");
        card.className = `card risk-${risk.riskLevel.toLowerCase()}`;

        card.innerHTML = `
            <h2>${risk.commodity}</h2>
            <p><strong>Risk:</strong> ${risk.riskLevel}</p>
            <p><strong>Score:</strong> ${risk.riskScore}</p>
            <p><strong>Reason:</strong> ${risk.reason}</p>
            <p><strong>Model:</strong> ${risk.modelUsed}</p>
        `;

        container.appendChild(card);
    });
}

loadRisks();