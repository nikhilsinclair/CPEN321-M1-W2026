import express, { type Express } from 'express';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/server-ip', (_req, res) => {
    res.json({ ip: '35.222.61.184' });
  });

  app.get('/server-time', (_req, res) => {
    const now = new Date();
    const pad = (value: number): string => String(value).padStart(2, '0');
    const time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

    // JavaScript's offset has the opposite sign to the GMT offset.
    const offsetMinutes = -now.getTimezoneOffset();
    const sign = offsetMinutes >= 0 ? '+' : '-';
    const offsetHours = pad(Math.floor(Math.abs(offsetMinutes) / 60));
    const offsetRemainder = pad(Math.abs(offsetMinutes) % 60);

    res.json({ time: `${time} GMT${sign}${offsetHours}:${offsetRemainder}` });
  });

  app.get('/name', (_req, res) => {
    res.json({ firstName: 'Nikhil', lastName: 'Sinclair' });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
