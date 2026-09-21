// Minimal, dependency-free sample backend service used to demonstrate
// gateway routing, rate limiting, and quota enforcement end-to-end.
// No external packages required — pure Node http module.
import http from 'node:http'

const PORT = process.env.PORT || 8081

const products = [
  { id: 1, name: 'Laptop', price: 75000 },
  { id: 2, name: 'Wireless Mouse', price: 999 },
  { id: 3, name: 'Mechanical Keyboard', price: 4499 },
  { id: 4, name: '27" Monitor', price: 18999 },
];

const server = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json');

  if (req.url === '/health') {
    res.writeHead(200);
    res.end(JSON.stringify({ status: 'ok' }));
    return;
  }

  if (req.url === '/products' && req.method === 'GET') {
    res.writeHead(200);
    res.end(JSON.stringify(products));
    return;
  }

  if (req.url?.match(/^\/products\/\d+$/) && req.method === 'GET') {
    const id = Number(req.url.split('/').pop());
    const product = products.find((p) => p.id === id);
    if (!product) {
      res.writeHead(404);
      res.end(JSON.stringify({ error: 'Product not found' }));
      return;
    }
    res.writeHead(200);
    res.end(JSON.stringify(product));
    return;
  }

  res.writeHead(404);
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, () => {
  console.log(`Sample API listening on port ${PORT}`);
});
