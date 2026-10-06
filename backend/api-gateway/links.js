
function origemPublica(req) {
  return `${req.protocol}://${req.get("host")}`;
}

function paraOrigemPublica(href, origem) {
  const url = new URL(href, origem);
  return `${origem}${url.pathname}${url.search}`;
}

function reescreverLinks(valor, origem) {
  if (Array.isArray(valor)) {
    valor.forEach((item) => reescreverLinks(item, origem));
  } else if (valor && typeof valor === "object") {
    for (const [chave, filho] of Object.entries(valor)) {
      if (chave === "_links" && filho && typeof filho === "object") {
        for (const link of Object.values(filho)) {
          if (link && typeof link.href === "string") {
            link.href = paraOrigemPublica(link.href, origem);
          }
        }
      } else {
        reescreverLinks(filho, origem);
      }
    }
  }
  return valor;
}

function reescreverLinksDoCorpo(corpo, contentType, origem) {
  if (!contentType || !contentType.includes("application/json") || corpo.length === 0) {
    return corpo;
  }
  try {
    return JSON.stringify(reescreverLinks(JSON.parse(corpo.toString("utf8")), origem));
  } catch {
    return corpo;
  }
}

function link(origem, caminho) {
  return { href: `${origem}${caminho}` };
}

module.exports = { origemPublica, reescreverLinks, reescreverLinksDoCorpo, link };
