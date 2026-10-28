#!/usr/bin/env node
// Mock Xtream Codes server with fictional data, for testing without a provider.
// Usage: node scripts/mock-xtream-server.mjs [port]   (default 8089; username "demo", password "demo")
// Optional playback media: scripts/.media/movie.mp4 for movies and episodes, scripts/.media/live/stream.m3u8 (plus .ts segments) for live channels.
import { existsSync, readFileSync, statSync, createReadStream } from 'node:fs';
import { createServer } from 'node:http';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const port = Number(process.argv[2] ?? 8089);
const USERNAME = 'demo';
const PASSWORD = 'demo';

const words = ['Beacon', 'Horizon', 'Aurora', 'Tide', 'Harbor', 'Valley', 'Island', 'Route', 'Echo', 'Ember'];
const suffixes = ['High', 'Low', 'North', 'South', 'New', 'Old', 'Blue', 'Golden'];
const name = (i) => `${words[i % words.length]} ${suffixes[(i * 7) % suffixes.length]} ${Math.floor(i / 80) + 1}`;
const categories = (names) => names.map((category_name, i) => ({ category_id: String(i + 1), category_name, parent_id: 0 }));

const liveCategories = categories(['News', 'Sports', 'Kids', 'Music']);
const vodCategories = categories(['Adventure', 'Drama', 'Comedy', 'Sci-Fi']);
const seriesCategories = categories(['Drama', 'Comedy', 'Thriller']);

const live = Array.from({ length: 2000 }, (_, i) => ({
  num: i + 1,
  name: `Channel ${name(i)}`,
  stream_type: 'live',
  stream_id: 1000 + i,
  stream_icon: `http://img.example.com/channel${i}.png`,
  epg_channel_id: `channel${i}`,
  category_id: String((i % liveCategories.length) + 1),
}));
const vod = Array.from({ length: 5000 }, (_, i) => ({
  num: i + 1,
  name: name(i + 3),
  stream_id: String(20000 + i),
  stream_icon: `http://img.example.com/movie${i}.jpg`,
  rating: i % 5 === 0 ? '' : String(5 + (i % 5)),
  rating_5based: String(2 + (i % 3)),
  category_id: String((i % vodCategories.length) + 1),
  container_extension: i % 2 ? 'mp4' : 'mkv',
  releasedate: `${2015 + (i % 11)}-03-01`,
}));
const series = Array.from({ length: 1000 }, (_, i) => ({
  series_id: 30000 + i,
  name: name(i + 11),
  cover: `http://img.example.com/series${i}.jpg`,
  rating: String(6 + (i % 4)),
  category_id: String((i % seriesCategories.length) + 1),
  releaseDate: `${2018 + (i % 8)}-01-10`,
}));

const media = join(dirname(fileURLToPath(import.meta.url)), '.media');
const shows = ['Harbor News', 'Regional Cup', 'Match Talk', 'Lighthouse Cinema', 'Small World', 'Music Hour', 'Valley Echoes', 'Night Route'];
const pad = (n) => String(n).padStart(2, '0');
const stamp = (ms) => {
  const d = new Date(ms);
  return `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}00 +0000`;
};

// Fictional guide: 300 channels, 1 h programmes from yesterday to 2 days ahead.
function xmltv() {
  const hour = 3_600_000;
  const first = Math.floor((Date.now() - 24 * hour) / hour) * hour;
  const out = ['<?xml version="1.0" encoding="UTF-8"?>', '<tv>'];
  for (let c = 0; c < 300; c++) {
    for (let h = 0; h < 72; h++) {
      const start = first + h * hour;
      const title = shows[(c + h) % shows.length];
      out.push(
        `<programme start="${stamp(start)}" stop="${stamp(start + hour)}" channel="channel${c}"><title>${title}</title><desc>Fictional test programme.</desc></programme>`,
      );
    }
  }
  out.push('</tv>');
  return out.join('\n');
}

const server = createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://localhost:${port}`);
  const send = (body) => {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(body));
  };
  console.log(new Date().toISOString().slice(11, 19), url.pathname, url.searchParams.get('action') ?? '(auth)');

  if (url.pathname === '/xmltv.php') {
    res.writeHead(200, { 'Content-Type': 'application/xml' });
    return res.end(xmltv());
  }
  const vodFile = url.pathname.match(new RegExp(`^/(?:movie|series)/${USERNAME}/${PASSWORD}/[^/]+\\.(?:mp4|mkv)$`));
  if (vodFile) {
    const path = join(media, 'movie.mp4');
    if (!existsSync(path)) {
      res.writeHead(404);
      return res.end();
    }
    const size = statSync(path).size;
    const range = /bytes=(\d*)-(\d*)/.exec(req.headers.range ?? '');
    const start = range && range[1] ? Number(range[1]) : 0;
    const end = range && range[2] ? Math.min(Number(range[2]), size - 1) : size - 1;
    res.writeHead(range ? 206 : 200, {
      'Content-Type': 'video/mp4',
      'Accept-Ranges': 'bytes',
      'Content-Length': end - start + 1,
      ...(range ? { 'Content-Range': `bytes ${start}-${end}/${size}` } : {}),
    });
    return createReadStream(path, { start, end }).pipe(res);
  }
  const stream = url.pathname.match(new RegExp(`^/(?:live|movie|series)/${USERNAME}/${PASSWORD}/([^/]+)$`));
  if (stream) {
    const file = stream[1].endsWith('.m3u8') ? 'stream.m3u8' : stream[1].endsWith('.ts') ? stream[1] : null;
    const path = file && join(media, 'live', file);
    if (!path || !existsSync(path)) {
      res.writeHead(404);
      return res.end();
    }
    res.writeHead(200, { 'Content-Type': file.endsWith('.m3u8') ? 'application/vnd.apple.mpegurl' : 'video/mp2t' });
    return res.end(readFileSync(path));
  }
  if (url.pathname !== '/player_api.php') {
    res.writeHead(404);
    return res.end();
  }
  const authorized = url.searchParams.get('username') === USERNAME && url.searchParams.get('password') === PASSWORD;
  const action = url.searchParams.get('action');
  if (!action) {
    return send({
      user_info: authorized
        ? { auth: 1, status: 'Active', exp_date: '1893456000', allowed_output_formats: ['m3u8', 'ts'] }
        : { auth: 0 },
      server_info: { url: 'localhost', port: String(port) },
    });
  }
  if (!authorized) return send({ user_info: { auth: 0 } });
  if (action === 'get_vod_info') {
    return send({
      info: {
        plot: 'Alone on an island during the biggest storm of the century, a lighthouse keeper has to guide a lost ship using only light signals. (Fictional test text.)',
        cast: 'Helen Prado, Tom Reese, Iris Mendel',
        director: 'Bruno Valence',
        genre: 'Adventure',
        duration_secs: 120,
      },
      movie_data: { container_extension: 'mp4' },
    });
  }
  if (action === 'get_series_info') {
    const episodes = {};
    for (const season of [1, 2]) {
      episodes[season] = Array.from({ length: 4 }, (_, i) => ({
        id: String(season * 100 + i + 1),
        episode_num: i + 1,
        title: `Test episode ${i + 1}`,
        container_extension: 'mp4',
        info: { duration_secs: 120 },
      }));
    }
    return send({
      info: { plot: 'An architect discovers a network of tunnels. (Fictional test text.)', genre: 'Thriller' },
      episodes,
    });
  }
  const body = {
    get_live_categories: liveCategories,
    get_live_streams: live,
    get_vod_categories: vodCategories,
    get_vod_streams: vod,
    get_series_categories: seriesCategories,
    get_series: series,
  }[action];
  return send(body ?? []);
});

server.listen(port, () => console.log(`Mock Xtream server at http://localhost:${port} (username ${USERNAME}, password ${PASSWORD})`));
