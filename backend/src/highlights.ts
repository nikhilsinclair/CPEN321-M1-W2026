export interface Highlight {
  title: string;
  competition: string;
  date: string;
  videoUrl: string;
}

type RecordValue = Record<string, unknown>;
const record = (value: unknown): RecordValue =>
  value !== null && typeof value === 'object' ? value as RecordValue : {};

export function parseHighlights(body: unknown): Highlight[] {
  const entries = record(body).response;
  if (!Array.isArray(entries)) return [];
  const highlights: Highlight[] = [];
  for (const entry of entries) {
    const match = record(entry);
    if (typeof match.title !== 'string' || !Array.isArray(match.videos)) continue;
    for (const item of match.videos) {
      const embed = record(item).embed;
      if (typeof embed !== 'string') continue;
      const src = /<iframe\b[^>]*\bsrc\s*=\s*["']([^"']+)["']/i.exec(embed)?.[1];
      if (!src) continue;
      try {
        const url = new URL(src.replace(/&amp;/g, '&'));
        if (url.protocol !== 'https:' || !['www.scorebat.com', 'scorebat.com'].includes(url.hostname) ||
            !url.pathname.startsWith('/embed/v/') || url.username || url.password) continue;
        highlights.push({
          title: match.title,
          competition: typeof match.competition === 'string' ? match.competition : '',
          date: typeof match.date === 'string' ? match.date : '',
          videoUrl: url.toString(),
        });
        break;
      } catch { /* Skip invalid provider URLs. */ }
    }
  }
  return highlights;
}

export function createHighlightLoader() {
  let cache: Highlight[] = [];
  let expires = 0;
  let pending: Promise<Highlight[]> | undefined;
  return async (): Promise<Highlight[]> => {
    if (Date.now() < expires) return cache;
    if (pending) return pending;
    const token = process.env.SCOREBAT_API_TOKEN?.trim();
    if (!token) throw new Error('Highlights unavailable');
    pending = (async () => {
      const url = new URL('https://www.scorebat.com/video-api/v3/free-feed/');
      url.searchParams.set('token', token);
      const response = await fetch(url, { signal: AbortSignal.timeout(10_000) });
      if (!response.ok) throw new Error('Highlights unavailable');
      cache = parseHighlights(await response.json());
      expires = Date.now() + 15 * 60 * 1000;
      return cache;
    })();
    try { return await pending; } finally { pending = undefined; }
  };
}
