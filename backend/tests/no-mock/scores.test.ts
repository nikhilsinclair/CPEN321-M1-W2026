import request from 'supertest';
import { createApp } from '../../src/app';
import { createScoreLoader, parseScores, scoreWindow } from '../../src/scores';
const match = {id: 1, status: 'FINISHED', utcDate: '2026-09-15T12:00:00Z', homeTeam: {name:'A'}, awayTeam:{name:'B'}, score:{fullTime:{home:0,away:2}}, competition:{name:'League'}};
describe('weekly scores', () => {
 const original = process.env.FOOTBALL_DATA_API_TOKEN;
 beforeEach(() => { process.env.FOOTBALL_DATA_API_TOKEN='test-token'; });
 afterEach(() => { jest.restoreAllMocks(); if(original === undefined) delete process.env.FOOTBALL_DATA_API_TOKEN; else process.env.FOOTBALL_DATA_API_TOKEN=original; });
 test('date window crosses months and excludes unfinished or out-of-range games', () => {
  expect(scoreWindow(Date.parse('2026-03-02T12:00:00Z'))).toEqual({from:'2026-02-24',to:'2026-03-03'});
  expect(parseScores({matches:[match,{...match,status:'SCHEDULED'},{...match,utcDate:'2026-09-17T00:00:00Z'},{...match,score:{fullTime:{home:null,away:2}}}]},'2026-09-10','2026-09-17')).toHaveLength(1);
 });
 test('caches results and sends token only in provider header', async () => {
  const mock=jest.spyOn(globalThis,'fetch').mockResolvedValue(new Response(JSON.stringify({matches:[]})));
  const app=createApp();
  expect((await request(app).get('/scores')).status).toBe(200);
  const response=await request(app).get('/scores');
  expect(response.body.matches).toEqual([]);
  expect(JSON.stringify(response.body)).not.toContain('test-token');
  expect(mock).toHaveBeenCalledTimes(1);
  expect(mock.mock.calls[0]?.[1]?.headers).toEqual({'X-Auth-Token':'test-token'});
 });
 test('rate limits pause retries rather than repeatedly calling provider', async () => {
  const mock=jest.spyOn(globalThis,'fetch').mockResolvedValue(new Response('{}',{status:429,headers:{'X-RequestCounter-Reset':'120'}}));
  const load=createScoreLoader();
  await expect(load()).rejects.toThrow(); await expect(load()).rejects.toThrow();
  expect(mock).toHaveBeenCalledTimes(1);
 });
 test('missing key fails safely without contacting provider', async () => {
  delete process.env.FOOTBALL_DATA_API_TOKEN;
  const mock=jest.spyOn(globalThis,'fetch');
  expect((await request(createApp()).get('/scores')).status).toBe(503);
  expect(mock).not.toHaveBeenCalled();
 });
});
