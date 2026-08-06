const http = require('http');
const fs = require('fs');
const path = require('path');

const port = Number(process.env.PORT || 3000);
const gatewayUrl = new URL(process.env.GATEWAY_URL || 'http://localhost:8080');
const publicDir = path.join(__dirname, 'public');
const mimeTypes = { '.css': 'text/css', '.html': 'text/html', '.js': 'application/javascript' };

function sendFile(response, filePath) {
  fs.readFile(filePath, (error, content) => {
    if (error) {
      response.writeHead(404, { 'Content-Type': 'text/plain' });
      response.end('Not found');
      return;
    }
    response.writeHead(200, { 'Content-Type': `${mimeTypes[path.extname(filePath)] || 'application/octet-stream'}; charset=utf-8` });
    response.end(content);
  });
}

http.createServer((request, response) => {
  if (request.url.startsWith('/api/')) {
    const proxyRequest = http.request({
      hostname: gatewayUrl.hostname,
      port: gatewayUrl.port || 80,
      path: request.url,
      method: request.method,
      headers: { ...request.headers, host: gatewayUrl.host }
    }, (proxyResponse) => {
      response.writeHead(proxyResponse.statusCode, proxyResponse.headers);
      proxyResponse.pipe(response);
    });
    proxyRequest.on('error', (error) => {
      response.writeHead(502, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ message: `Could not reach API gateway at ${gatewayUrl.origin}. Start docker compose first.`, detail: error.message }));
    });
    request.pipe(proxyRequest);
    return;
  }

  const requested = request.url === '/' ? 'index.html' : request.url.replace(/^\//, '');
  const filePath = path.normalize(path.join(publicDir, requested));
  if (!filePath.startsWith(publicDir)) {
    response.writeHead(403);
    response.end();
    return;
  }
  sendFile(response, filePath);
}).listen(port, () => console.log(`Frontend ready at http://localhost:${port} (gateway: ${gatewayUrl.origin})`));
