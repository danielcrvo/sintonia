#!/usr/bin/env node
// Prints a fictional M3U playlist (20% live, 50% movies, 30% series) for testing imports.
// Usage: node scripts/generate-sample-playlist.mjs [itemCount] > playlist.m3u

const total = Math.max(1, Number(process.argv[2] ?? 1000));

const liveGroups = ['News', 'Sports', 'Kids', 'Documentaries', 'Music', 'Entertainment'];
const movieGroups = ['Action Movies', 'Drama Movies', 'Comedy Movies', 'Sci-Fi Movies', 'Adventure Movies'];
const seriesGroups = ['Drama Series', 'Comedy Series', 'Thriller Series', 'Kids Series'];

const words = ['Beacon', 'Horizon', 'Aurora', 'Tide', 'Harbor', 'Valley', 'Island', 'Route', 'Echo', 'Ember', 'Mist', 'Plaza', 'Lumen', 'Ridge', 'River'];
const suffixes = ['High', 'Low', 'North', 'South', 'New', 'Old', 'Blue', 'Golden', 'Distant', 'Hidden'];
const pick = (list, i, step = 1) => list[(i * step) % list.length];
const title = (i) => `${pick(words, i)} ${pick(suffixes, i, 7)} ${Math.floor(i / 150) + 1}`;

const lines = ['#EXTM3U url-tvg="http://guide.example.com/xmltv.xml"'];
for (let i = 0; i < total; i++) {
  const bucket = i % 10;
  if (bucket < 2) {
    lines.push(
      `#EXTINF:-1 tvg-id="channel${i}" tvg-name="Channel ${title(i)}" tvg-logo="http://img.example.com/channel${i}.png" group-title="${pick(liveGroups, i)}",Channel ${title(i)}`,
      `http://server.example.com/live/demo/demo/${i}.m3u8`,
    );
  } else if (bucket < 7) {
    lines.push(
      `#EXTINF:-1 tvg-id="movie${i}" tvg-logo="http://img.example.com/movie${i}.jpg" group-title="${pick(movieGroups, i)}",${title(i)}`,
      `http://server.example.com/movie/demo/demo/${i}.mp4`,
    );
  } else {
    lines.push(
      `#EXTINF:-1 tvg-id="series${i}" tvg-logo="http://img.example.com/series${i}.jpg" group-title="${pick(seriesGroups, i)}",${title(i)} S${(i % 3) + 1} E${(i % 10) + 1}`,
      `http://server.example.com/series/demo/demo/${i}.mkv`,
    );
  }
}
process.stdout.write(lines.join('\n') + '\n');
