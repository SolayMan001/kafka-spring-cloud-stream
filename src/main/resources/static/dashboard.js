const chart = document.querySelector("#visits-chart");
const context = chart.getContext("2d");
const connection = document.querySelector("#connection");
const styles = getComputedStyle(document.documentElement);
const pages = ["P1", "P2"].map(name => ({
    name,
    label: document.querySelector(`#${name.toLowerCase()}-count`),
    color: styles.getPropertyValue(`--${name.toLowerCase()}`).trim()
}));

const history = [];
const historyLimit = 61;
const windowTime = new Intl.DateTimeFormat("fr-FR", { timeStyle: "medium" });

function draw() {
    const { width, height } = chart.getBoundingClientRect();
    const scale = window.devicePixelRatio || 1;
    chart.width = Math.round(width * scale);
    chart.height = Math.round(height * scale);
    context.scale(scale, scale);

    const left = 34;
    const right = width - 5;
    const top = 10;
    const bottom = height - 10;
    const maximum = Math.max(4, ...history.flatMap(sample => pages.map(page => sample[page.name] ?? 0)));
    const step = Math.ceil(maximum / 4);
    const y = count => bottom - (bottom - top) * count / (step * 4);

    context.font = "12px system-ui";
    context.textAlign = "right";
    context.textBaseline = "middle";
    context.fillStyle = styles.getPropertyValue("--muted").trim();
    context.strokeStyle = styles.getPropertyValue("--line").trim();
    context.beginPath();

    for (let count = 0; count <= step * 4; count += step) {
        const row = y(count);
        context.fillText(String(count), left - 8, row);
        context.moveTo(left, row);
        context.lineTo(right, row);
    }
    context.stroke();

    for (const page of pages) {
        context.beginPath();
        context.strokeStyle = page.color;
        context.lineWidth = 2;
        history.forEach((sample, index) => {
            const secondsAgo = history.length - 1 - index;
            const x = right - (right - left) * secondsAgo / (historyLimit - 1);
            const row = y(sample[page.name] ?? 0);
            if (index === 0) context.moveTo(x, row);
            else context.lineTo(x, row);
        });
        context.stroke();
    }
}

new ResizeObserver(draw).observe(chart);
const stream = new EventSource("/analytics");

stream.onmessage = event => {
    const snapshot = JSON.parse(event.data);
    const { counts, windowStart } = snapshot;
    connection.textContent = `Fenêtre ouverte à ${windowTime.format(new Date(windowStart))}`;
    history.push(counts);
    if (history.length > historyLimit) history.shift();
    pages.forEach(page => { page.label.textContent = counts[page.name] ?? 0; });
    draw();
};

stream.onerror = () => {
    connection.textContent = "Flux interrompu. Reconnexion en cours…";
};
