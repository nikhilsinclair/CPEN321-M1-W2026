import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const pixelSourceUrl = process.env.PIXEL_SOURCE_URL || 'wss://8.229.22.124';
if (new URL(pixelSourceUrl).protocol !== 'wss:') {
  throw new Error('PIXEL_SOURCE_URL must use wss://');
}

export const env = {
  port,
  pixelSourceUrl,
  pixelSourceCaFile: process.env.PIXEL_SOURCE_CA_FILE,
} as const;
