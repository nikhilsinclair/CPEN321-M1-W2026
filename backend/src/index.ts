import { readFileSync } from 'node:fs';
import { attachPixelRelay } from './pixelRelay';
import { createApp } from './app';
import { env } from './config/env';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

const stopPixelRelay = attachPixelRelay(server, {
  upstreamUrl: env.pixelSourceUrl,
  ca: env.pixelSourceCaFile ? readFileSync(env.pixelSourceCaFile) : undefined,
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    stopPixelRelay();
    server.close(() => {
      process.exit(0);
    });
  });
}
