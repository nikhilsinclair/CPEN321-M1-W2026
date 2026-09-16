import type { Server } from 'node:http';
import WebSocket, { WebSocketServer } from 'ws';

export interface PixelRelayOptions {
  upstreamUrl: string;
  ca?: Buffer | undefined;
}

// A separate upstream connection gives each viewer its own pixel stream.
export function attachPixelRelay(server: Server, options: PixelRelayOptions): () => void {
  const relay = new WebSocketServer({ server, path: '/pixels', maxPayload: 4096 });
  const upstreams = new Set<WebSocket>();

  relay.on('connection', (client) => {
    const upstream = new WebSocket(options.upstreamUrl, {
      ca: options.ca,
      handshakeTimeout: 10_000,
      maxPayload: 4096,
    });
    upstreams.add(upstream);

    upstream.on('message', (data, isBinary) => {
      if (client.readyState !== WebSocket.OPEN) return;
      // Disconnect slow viewers instead of accumulating stale frames.
      if (client.bufferedAmount > 1024 * 1024) {
        client.close(1013, 'Viewer too slow; reconnect');
        upstream.terminate();
        return;
      }
      // Preserve the payload bytes and text/binary type, without JSON conversion.
      client.send(data, { binary: isBinary });
    });
    upstream.on('error', () => {
      client.close(1011, 'Pixel source unavailable');
    });
    upstream.on('close', () => {
      upstreams.delete(upstream);
      if (client.readyState === WebSocket.OPEN) {
        client.close(1011, 'Pixel source disconnected');
      }
    });
    client.on('error', () => upstream.terminate());
    client.on('close', () => upstream.terminate());
  });

  return () => {
    for (const upstream of upstreams) upstream.terminate();
    for (const client of relay.clients) client.terminate();
    relay.close();
  };
}
