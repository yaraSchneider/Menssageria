const historyKey = "menssageria.pedidos";
const form = document.querySelector("#order-form");
const itemsList = document.querySelector("#items-list");
const historyList = document.querySelector("#history-list");
const historyEmpty = document.querySelector("#history-empty");
const toast = document.querySelector("#toast");
const lookupInput = document.querySelector("#lookup-id");
const lookupResult = document.querySelector("#lookup-result");
let historyEntries = readHistory();
let toastTimeout;

function readHistory() {
  try {
    const stored = localStorage.getItem(historyKey);
    if (!stored) return [];
    const entries = JSON.parse(stored);
    if (!Array.isArray(entries) || entries.some(entry =>
      !entry || typeof entry.id !== "string" || typeof entry.cliente !== "string"
      || typeof entry.valorTotal !== "number" || typeof entry.dataCriacao !== "string"
    )) throw new Error("O formato do histórico local é inválido.");
    return entries;
  } catch (error) {
    showToast(`Não foi possível carregar o histórico local: ${error.message}`, true);
    return [];
  }
}

function saveHistory() {
  try {
    localStorage.setItem(historyKey, JSON.stringify(historyEntries));
    return true;
  } catch (error) {
    return false;
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Data indisponível" : new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(date);
}

function renderHistory() {
  const total = historyEntries.reduce((sum, entry) => sum + entry.valorTotal, 0);
  const latest = historyEntries[0];
  document.querySelector("#stat-orders").textContent = String(historyEntries.length);
  document.querySelector("#stat-value").textContent = formatCurrency(total);
  document.querySelector("#stat-last").textContent = latest ? formatDate(latest.dataCriacao) : "—";
  document.querySelector("#history-count").textContent = String(historyEntries.length);
  historyList.replaceChildren();
  historyEmpty.hidden = historyEntries.length > 0;

  for (const entry of historyEntries.slice(0, 4)) {
    const button = document.createElement("button");
    button.className = "history-entry";
    button.type = "button";
    button.setAttribute("aria-label", `Consultar pedido de ${entry.cliente}`);

    const avatar = document.createElement("span");
    avatar.className = "order-avatar";
    avatar.textContent = entry.cliente.trim().charAt(0).toUpperCase() || "?";

    const meta = document.createElement("span");
    meta.className = "history-meta";
    const name = document.createElement("span");
    name.className = "history-name";
    name.textContent = entry.cliente;
    const id = document.createElement("span");
    id.className = "history-id";
    id.textContent = entry.id;
    meta.append(name, id);

    const value = document.createElement("span");
    value.className = "history-value";
    value.textContent = formatCurrency(entry.valorTotal);
    button.append(avatar, meta, value);
    button.addEventListener("click", () => loadOrder(entry.id));
    historyList.append(button);
  }
}

function showToast(message, isError = false) {
  clearTimeout(toastTimeout);
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("visible");
  toastTimeout = setTimeout(() => toast.classList.remove("visible"), 5000);
}

function showLookupMessage(message, isError = false) {
  lookupResult.textContent = message;
  lookupResult.classList.toggle("error", isError);
}

function addItem() {
  const row = document.createElement("div");
  row.className = "item-row";
  const itemNumber = itemsList.children.length + 1;
  const productLabel = document.createElement("label");
  productLabel.className = "input-wrap product-input";
  const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  icon.setAttribute("viewBox", "0 0 24 24");
  icon.setAttribute("fill", "none");
  icon.setAttribute("aria-hidden", "true");
  const iconPath = document.createElementNS("http://www.w3.org/2000/svg", "path");
  iconPath.setAttribute("d", "m4 8 8-4 8 4v9l-8 4-8-4V8Zm.5.25 7.5 4 7.5-4M12 12.5V21M8 6l8 4");
  iconPath.setAttribute("stroke", "currentColor");
  iconPath.setAttribute("stroke-width", "1.6");
  iconPath.setAttribute("stroke-linejoin", "round");
  icon.append(iconPath);

  const product = document.createElement("input");
  product.id = `product-${itemNumber}`;
  product.name = "produto";
  product.type = "text";
  product.maxLength = 120;
  product.placeholder = "Nome do produto";
  product.required = true;
  productLabel.append(icon, product);

  const quantityLabel = document.createElement("label");
  quantityLabel.className = "quantity-wrap";
  const quantityDescription = document.createElement("span");
  quantityDescription.className = "sr-only";
  quantityDescription.textContent = "Quantidade";
  const quantity = document.createElement("input");
  quantity.name = "quantidade";
  quantity.type = "number";
  quantity.min = "1";
  quantity.step = "1";
  quantity.value = "1";
  quantity.required = true;
  quantityLabel.append(quantityDescription, quantity);

  const remove = document.createElement("button");
  remove.className = "remove-item";
  remove.type = "button";
  remove.setAttribute("aria-label", "Remover item");
  remove.title = "Remover item";
  remove.innerHTML = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 7h14M10 11v6m4-6v6M6.5 7l.8 13h9.4l.8-13M9 7V4h6v3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  remove.addEventListener("click", () => {
    row.remove();
    updateItemControls();
  });
  row.append(productLabel, quantityLabel, remove);
  itemsList.append(row);
  updateItemControls();
  product.focus();
}

function updateItemControls() {
  const rows = [...itemsList.querySelectorAll(".item-row")];
  rows.forEach((row, index) => {
    const removeButton = row.querySelector(".remove-item");
    removeButton.disabled = rows.length === 1;
    const product = row.querySelector('[name="produto"]');
    product.id = `product-${index + 1}`;
  });
}

function apiErrorMessage(body, status) {
  if (Array.isArray(body.campos) && body.campos.length) {
    return body.campos.map(field => `${field.campo}: ${field.mensagem}`).join(" · ");
  }
  if (body.mensagem) return body.mensagem;
  return `A API respondeu com erro ${status}.`;
}

async function loadOrder(id) {
  if (!id.trim()) {
    showLookupMessage("Informe o identificador do pedido.", true);
    return;
  }

  showLookupMessage("Consultando pedido...");
  try {
    const response = await fetch(`/pedidos/${encodeURIComponent(id.trim())}`);
    const body = await response.json();
    if (!response.ok) throw new Error(apiErrorMessage(body, response.status));

    lookupResult.replaceChildren();
    lookupResult.classList.remove("error");
    const title = document.createElement("strong");
    title.textContent = `${body.cliente} · ${formatCurrency(Number(body.valorTotal))}`;
    const description = document.createElement("span");
    const itemSummary = body.itens.map(item => `${item.quantidade}× ${item.produto}`).join(", ");
    description.textContent = `${formatDate(body.dataCriacao)} · ${itemSummary}`;
    lookupResult.append(title, description);
    lookupInput.value = body.id;
  } catch (error) {
    showLookupMessage(error.message || "Não foi possível consultar o pedido.", true);
  }
}

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!form.reportValidity()) return;

  const submitButton = document.querySelector("#submit-order");
  submitButton.disabled = true;
  submitButton.querySelector("span").textContent = "Publicando...";

  const request = {
    cliente: form.elements.cliente.value.trim(),
    itens: [...itemsList.querySelectorAll(".item-row")].map(row => ({
      produto: row.querySelector('[name="produto"]').value.trim(),
      quantidade: Number(row.querySelector('[name="quantidade"]').value)
    })),
    valorTotal: Number(form.elements.valorTotal.value)
  };

  try {
    const response = await fetch("/pedidos", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    });
    const body = await response.json();
    if (!response.ok) throw new Error(apiErrorMessage(body, response.status));

    const entry = {
      id: body.id,
      cliente: request.cliente,
      itens: request.itens,
      valorTotal: request.valorTotal,
      dataCriacao: new Date().toISOString()
    };
    historyEntries = [entry, ...historyEntries.filter(item => item.id !== entry.id)].slice(0, 50);
    const savedLocally = saveHistory();
    renderHistory();
    lookupInput.value = body.id;
    showToast(
      savedLocally
        ? `Pedido publicado com sucesso. ID: ${body.id}`
        : `Pedido criado, mas o histórico local não foi salvo. Guarde o ID: ${body.id}`,
      !savedLocally
    );
    form.reset();
    itemsList.replaceChildren();
    addItem();
  } catch (error) {
    showToast(error.message || "Não foi possível publicar o pedido. Verifique a API e o RabbitMQ.", true);
  } finally {
    submitButton.disabled = false;
    submitButton.querySelector("span").textContent = "Publicar pedido";
  }
});

document.querySelector("#add-item").addEventListener("click", addItem);
document.querySelector("#lookup-button").addEventListener("click", () => loadOrder(lookupInput.value));
lookupInput.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    loadOrder(lookupInput.value);
  }
});

renderHistory();
