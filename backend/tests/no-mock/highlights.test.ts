import request from 'supertest';
import { createApp } from '../../src/app';
import { parseHighlights } from '../../src/highlights';

const feed = { response: [{ title: 'Team A - Team B', competition: 'Cup', date: '2026-09-15', videos: [
  { embed: '<iframe src="https://www.scorebat.com/embed/v/example/?a=1&amp;b=2"></iframe>' },
] }] };

describe('ScoreBat highlights', () => {
  const original = process.env.SCOREBAT_API_TOKEN;
  afterEach(() => {
    jest.restoreAllMocks();
    if (original === undefined) delete process.env.SCOREBAT_API_TOKEN;
    else process.env.SCOREBAT_API_TOKEN = original;
  });
  test('returns safe URLs and rejects untrusted embed hosts', () => {
    expect(parseHighlights(feed)[0]?.videoUrl).toBe('https://www.scorebat.com/embed/v/example/?a=1&b=2');
    expect(parseHighlights({ response: [{ title: 'Bad', videos: [{ embed: '<iframe src="https://evil.example/embed/v/test"></iframe>' }] }] })).toEqual([]);
  });
  test('missing token returns a friendly unavailable response', async () => {
    delete process.env.SCOREBAT_API_TOKEN;
    const response = await request(createApp()).get('/highlights');
    expect(response.status).toBe(503);
    expect(response.body).toEqual({ error: 'Highlights are unavailable right now' });
  });
  test('caches provider response and never returns the API token', async () => {
    process.env.SCOREBAT_API_TOKEN = 'test-only-secret';
    const fetchMock = jest.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(feed)));
    const app = createApp();
    const first = await request(app).get('/highlights');
    const second = await request(app).get('/highlights');
    expect(first.status).toBe(200);
    expect(second.body.highlights).toHaveLength(1);
    expect(JSON.stringify(first.body)).not.toContain('test-only-secret');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
  test('provider errors do not leak request details', async () => {
    process.env.SCOREBAT_API_TOKEN = 'test-only-secret';
    jest.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('token=test-only-secret'));
    const response = await request(createApp()).get('/highlights');
    expect(response.status).toBe(503);
    expect(JSON.stringify(response.body)).not.toContain('test-only-secret');
  });
});
