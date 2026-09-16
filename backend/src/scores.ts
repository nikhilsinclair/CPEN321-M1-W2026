export interface MatchScore {
  id: number; home: string; away: string; homeGoals: number; awayGoals: number;
  competition: string; date: string;
}
const day = 86_400_000;
export function scoreWindow(now = Date.now()) {
  const today = new Date(now).toISOString().slice(0, 10);
  const start = Date.parse(today) - 6 * day;
  return { from: new Date(start).toISOString().slice(0, 10), to: new Date(start + 7 * day).toISOString().slice(0, 10) };
}
export function parseScores(body: unknown, from: string, to: string): MatchScore[] {
  const matches = (body as { matches?: unknown })?.matches;
  if (!Array.isArray(matches)) throw new Error('Invalid scores response');
  return matches.flatMap(m => {
    const score = m?.score?.fullTime;
    const date = Date.parse(m?.utcDate);
    if (m?.status !== 'FINISHED' || !Number.isInteger(m.id) || !Number.isFinite(date) ||
        date < Date.parse(from) || date >= Date.parse(to) ||
        typeof m.homeTeam?.name !== 'string' || typeof m.awayTeam?.name !== 'string' ||
        !Number.isInteger(score?.home) || !Number.isInteger(score?.away) || score.home < 0 || score.away < 0) return [];
    return [{ id: m.id, home: m.homeTeam.name, away: m.awayTeam.name,
      homeGoals: score.home, awayGoals: score.away,
      competition: typeof m.competition?.name === 'string' ? m.competition.name : '', date: m.utcDate }];
  }).sort((a, b) => Date.parse(b.date) - Date.parse(a.date));
}
export function createScoreLoader() {
  let cached: { from: string; to: string; matches: MatchScore[] } | undefined;
  let expires = 0;
  let blockedUntil = 0;
  let pending: Promise<NonNullable<typeof cached>> | undefined;
  return async () => {
    const window = scoreWindow();
    if (cached && cached.from === window.from && Date.now() < expires) return cached;
    if (pending) return pending;
    if (Date.now() < blockedUntil) throw new Error('Scores temporarily unavailable');
    const token = process.env.FOOTBALL_DATA_API_TOKEN?.trim();
    if (!token) throw new Error('Scores not configured');
    pending = (async () => {
      try {
        const url = new URL('https://api.football-data.org/v4/matches');
        url.searchParams.set('dateFrom', window.from);
        url.searchParams.set('dateTo', window.to);
        url.searchParams.set('status', 'FINISHED');
        const response = await fetch(url, { headers: { 'X-Auth-Token': token }, signal: AbortSignal.timeout(15_000) });
        const reset = Math.max(60, Number(response.headers.get('X-RequestCounter-Reset')) || 60,
          Number(response.headers.get('Retry-After')) || 0);
        const available = response.headers.get('X-Requests-Available-Minute') ?? response.headers.get('X-RequestsAvailable');
        if (response.status === 429 || available === '0') blockedUntil = Date.now() + reset * 1000;
        if (!response.ok) throw new Error('Scores unavailable');
        cached = { ...window, matches: parseScores(await response.json(), window.from, window.to) };
        expires = Date.now() + 15 * 60_000;
        return cached;
      } catch {
        blockedUntil = Math.max(blockedUntil, Date.now() + 60_000);
        throw new Error('Scores unavailable');
      }
    })();
    try { return await pending; } finally { pending = undefined; }
  };
}
