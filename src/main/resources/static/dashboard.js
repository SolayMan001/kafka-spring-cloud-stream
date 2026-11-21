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
const visibleDuration = 12_000;
const renderDelay = 500;
const windowTime = new Intl.DateTimeFormat("fr-FR", { timeStyle: "medium" });
let plot = { width: 0, height: 0 };

function resize() {
    plot = chart.getBoundingClientRect();
    const scale = window.devicePixelRatio || 1;
    chart.width = Math.round(plot.width * scale);
    chart.height = Math.round(plot.height * scale);
    context.setTransform(scale, 0, 0, scale, 0, 0);
}

function draw(now) {
    const { width, height } = plot;
    const end = now - renderDelay;
    const start = end - visibleDuration;
    const x = time => (time - start) * width / visibleDuration;

    // Keep the sample just before the left edge so the curve enters the plot smoothly.
    while (history.length > 1 && history[1].time < start) history.shift();

    const maximum = Math.max(4, ...history.flatMap(({ counts }) => pages.map(page => counts[page.name] ?? 0)));
    const y = count => height - 3 - (height - 6) * count / maximum;

    context.fillStyle = "#000";
    context.fillRect(0, 0, width, height);
    context.beginPath();
    context.strokeStyle = "#626262";
    context.lineWidth = 1;
    for (let tick = Math.ceil(start / 1000) * 1000; tick <= end; tick += 1000) {
        const column = Math.round(x(tick)) + 0.5;
        context.moveTo(column, 0);
        context.lineTo(column, height);
    }
    context.moveTo(0, Math.round(height / 2) + 0.5);
    context.lineTo(width, Math.round(height / 2) + 0.5);
    context.stroke();

    if (history.length > 0 && history.at(-1).time + 1000 >= start) {
        for (const page of pages) {
            const points = history.map(({ time, counts }) => [x(time), y(counts[page.name] ?? 0)]);
            const [firstX, firstY] = points[0];
            const line = new Path2D();
            line.moveTo(firstX, firstY);
            for (let index = 1; index < points.length; index++) {
                const [previousX, previousY] = points[index - 1];
                const [nextX, nextY] = points[index];
                const middleX = (previousX + nextX) / 2;
                line.bezierCurveTo(middleX, previousY, middleX, nextY, nextX, nextY);
            }

            const [, lastY] = points.at(-1);
            const endX = x(history.at(-1).time + 1000);
            line.lineTo(endX, lastY);
            const fill = new Path2D(line);
            fill.lineTo(endX, height);
            fill.lineTo(firstX, height);
            fill.closePath();
            context.fillStyle = `${page.color}33`;
            context.fill(fill);
            context.strokeStyle = page.color;
            context.lineWidth = 2;
            context.stroke(line);
        }
    }

    context.font = "12px monospace";
    context.textAlign = "right";
    context.fillStyle = "#fff";
    context.fillText(String(maximum), width - 6, 14);
    context.fillText("0", width - 6, height - 5);
    requestAnimationFrame(draw);
}

resize();
new ResizeObserver(resize).observe(chart);
requestAnimationFrame(draw);

const stream = new EventSource("/analytics");
stream.onmessage = event => {
    const { counts, windowStart } = JSON.parse(event.data);
    connection.textContent = `Fenêtre ouverte à ${windowTime.format(new Date(windowStart))}`;
    history.push({ time: performance.now(), counts });
    pages.forEach(page => { page.label.textContent = counts[page.name] ?? 0; });
};

stream.onerror = () => {
    connection.textContent = "Flux interrompu. Reconnexion en cours…";
};
