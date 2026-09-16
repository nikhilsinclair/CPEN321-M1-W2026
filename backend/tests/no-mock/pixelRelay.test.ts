import { createServer, type Server } from 'node:http';
import { once } from 'node:events';
import type { AddressInfo } from 'node:net';
import WebSocket, { WebSocketServer } from 'ws';
import { attachPixelRelay } from '../../src/pixelRelay';

const url = (server: Server): string => `ws://127.0.0.1:${(server.address() as AddressInfo).port}`;

describe('Pixel relay', () => {
  let sourceServer: Server;
  let backend: Server;
  let source: WebSocketServer;
  let stop: () => void;
  let clients: WebSocket[];

  beforeEach(async () => {
    clients = [];
    sourceServer = createServer();
    source = new WebSocketServer({ server: sourceServer });
    sourceServer.listen(0, '127.0.0.1');
    await once(sourceServer, 'listening');
    backend = createServer();
    stop = attachPixelRelay(backend, { upstreamUrl: url(sourceServer) });
    backend.listen(0, '127.0.0.1');
    await once(backend, 'listening');
  });

  afterEach(async () => {
    for (const client of clients) client.terminate();
    stop();
    for (const connection of source.clients) connection.terminate();
    await new Promise<void>((resolve) => source.close(() => resolve()));
    await Promise.all([backend, sourceServer].map((server) =>
      new Promise<void>((resolve) => server.close(() => resolve()))));
  });

  async function connect(): Promise<[WebSocket, WebSocket]> {
    const connected = once(source, 'connection');
    const viewer = new WebSocket(`${url(backend)}/pixels`);
    clients.push(viewer);
    await once(viewer, 'open');
    const [upstream] = await connected;
    return [viewer, upstream as WebSocket];
  }

  test('forwards successive text payloads byte-for-byte without waiting for another frame', async () => {
    const [viewer, upstream] = await connect();
    for (const payload of ['{ "x": 0, "y": 15, "color": "#Aa00ff" }', '{"x":1,"y":0,"color":"#000000"}']) {
      const message = once(viewer, 'message');
      upstream.send(payload);
      const [data, binary] = await message;
      expect(data.toString()).toBe(payload);
      expect(binary).toBe(false);
    }
  });

  test('preserves binary messages', async () => {
    const [viewer, upstream] = await connect();
    const message = once(viewer, 'message');
    upstream.send(Buffer.from([0, 1, 255]));
    const [data, binary] = await message;
    expect(data).toEqual(Buffer.from([0, 1, 255]));
    expect(binary).toBe(true);
  });

  test('closes upstream when viewer leaves', async () => {
    const [viewer, upstream] = await connect();
    const closed = once(upstream, 'close');
    viewer.close();
    await closed;
  });

  test('signals source failure with a close frame, not an invented pixel payload', async () => {
    const [viewer, upstream] = await connect();
    const closed = once(viewer, 'close');
    upstream.terminate();
    const [code] = await closed;
    expect(code).toBe(1011);
  });
});
