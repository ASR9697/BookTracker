// Generates .agents/maps/code_map.md: every Kotlin file with its purpose,
// size, and declarations (name:line), so agents can jump straight to code.
// Usage: npm run update-maps   |   npm run check-maps (exit 1 if stale)
const fs = require('fs');
const path = require('path');

const MODULES = ['app', 'shared', 'wear'];
const PKG_ROOT = 'java/com/example/booktracker/';
const OUTPUT_FILE = path.join('.agents', 'maps', 'code_map.md');

// Short labels for package folders; unknown folders still appear, unlabeled.
const DOMAIN_LABELS = {
    'app': 'Application entry points',
    'app/analytics': 'Pure stats engines (goals, streaks, badges, loans); unit-tested',
    'app/camera': 'ML Kit barcode + OCR analyzers',
    'app/data': 'Repository, backup/import, settings, DI',
    'app/data/local': 'Room: entities, DAOs, migrations, mappers',
    'app/data/remote': 'Open Library / Google Books lookup',
    'app/format': 'Book format (paper/ebook/audio) handling',
    'app/hardware': 'Stylus, sensors, device integrations',
    'app/journal': 'Smart planning, soundscapes',
    'app/notifications': 'Notification channels and builders',
    'app/service': 'Foreground reading-timer service',
    'app/sync': 'Phone side of Wear OS sync',
    'app/ui': 'Compose screens, dialogs, ViewModel, navigation',
    'app/ui/animations': 'Reusable Compose animations',
    'app/ui/reader': 'In-app EPUB reader',
    'app/ui/theme': 'Colors, typography, theme',
    'app/util': 'Exporters and helpers',
    'app/widget': 'Home-screen widget',
    'app/worker': 'WorkManager jobs',
    'data/epub': 'EPUB parsing and HTML-to-Compose rendering',
    'util/gif': 'GIF decoding',
    'shared': 'Constants shared by phone and watch',
    'shared/models': 'Domain models shared by phone and watch',
    'wear': 'Wear OS app, complication, data listener',
    'wear/journal': 'Wear voice notes',
};

const MODIFIERS = '(?:(?:public|private|internal|protected|open|sealed|abstract|override|suspend|inline|data|enum|annotation|inner|value|const|operator|infix|tailrec|external|fun(?=\\s+interface))\\s+)*';
const DECL = new RegExp(`^(\\s*)${MODIFIERS}(class|interface|object|fun|typealias)\\s+(?:<[^>]*>\\s*)?([A-Za-z_][A-Za-z0-9_.]*)`);
const ROUTE = /composable\(\s*"([^"]+)"/;

function walk(dir, out = []) {
    if (!fs.existsSync(dir)) return out;
    for (const name of fs.readdirSync(dir).sort()) {
        const p = path.join(dir, name);
        if (fs.statSync(p).isDirectory()) walk(p, out);
        else if (p.endsWith('.kt')) out.push(p.replace(/\\/g, '/'));
    }
    return out;
}

// First sentence of the KDoc attached to the declaration at `declLine`.
function kdocBefore(lines, declLine) {
    let end = declLine - 1;
    while (end >= 0 && lines[end].trim().startsWith('@')) end--;
    if (end < 0 || !lines[end].trim().endsWith('*/')) return '';
    let start = end;
    while (start >= 0 && !lines[start].trim().startsWith('/**')) start--;
    if (start < 0) return '';
    const text = lines.slice(start, end + 1)
        .map(l => l.replace(/^\s*\/?\*+\/?/, '').replace(/\*\/\s*$/, ''))
        .join(' ').replace(/\s+/g, ' ').trim();
    const sentence = text.match(/^.*?[.!?](?=\s|$)/);
    const first = sentence ? sentence[0] : text;
    return first.length > 120 ? first.slice(0, 117) + '...' : first;
}

// File purpose: the KDoc on the declaration named after the file, else on
// the first top-level declaration. Never a helper's KDoc further down.
function purposeOf(lines, topLevel, baseName) {
    const main = topLevel.find(d => d.name === baseName) || topLevel[0];
    return main ? kdocBefore(lines, main.line) : '';
}

// `a.b.Receiver.fn` -> `Receiver.fn`
const shortName = name => name.split('.').slice(-2).join('.');

function parse(file) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    const types = [];
    const funs = [];
    const routes = [];
    const topLevel = [];
    lines.forEach((line, i) => {
        const route = line.match(ROUTE);
        if (route) routes.push(`${route[1]}:${i + 1}`);
        const m = line.match(DECL);
        if (!m) return;
        const [, indent, kind, name] = m;
        // Top level: everything (private Composables are the jump targets in
        // big screens). One level in: the public API of classes/objects.
        if (indent.length > 4) return;
        if (indent.length === 4 && /\b(private|override)\b/.test(line)) return;
        if (indent.length > 0 && indent.length < 4) return;
        if (indent.length === 0) topLevel.push({ name, line: i });
        const entry = `${indent.length === 4 ? '.' : ''}${shortName(name)}:${i + 1}`;
        (kind === 'fun' ? funs : types).push(entry);
    });
    const baseName = path.basename(file, '.kt');
    return { lines: lines.length, purpose: purposeOf(lines, topLevel, baseName), types, funs, routes };
}

function domainOf(file) {
    const idx = file.indexOf(PKG_ROOT);
    if (idx < 0) return file.split('/')[0];
    const dir = path.posix.dirname(file.slice(idx + PKG_ROOT.length));
    return dir === '.' ? file.split('/')[0] : dir;
}

function build() {
    const groups = new Map();
    const tests = [];
    for (const mod of MODULES) {
        for (const file of walk(path.join(mod, 'src'))) {
            if (/\/src\/(test|androidTest)\//.test(file)) { tests.push(file); continue; }
            const domain = domainOf(file);
            if (!groups.has(domain)) groups.set(domain, []);
            groups.get(domain).push(file);
        }
    }

    const out = [
        '# Code map (generated by `npm run update-maps`; do not edit by hand)',
        '',
        'Format: `File.kt (lines) - purpose`, then `types` and `fun` as `name:line`.',
        'A leading `.` marks a public member of the class above it. Read big files',
        'with offset/limit at these lines instead of reading them whole.',
        'Package root: `<module>/src/main/java/com/example/booktracker/`.',
        '',
    ];
    for (const domain of [...groups.keys()].sort()) {
        const files = groups.get(domain);
        const dir = path.posix.dirname(files[0]);
        const label = DOMAIN_LABELS[domain];
        out.push(`## ${domain}${label ? ' - ' + label : ''}`, `\`${dir}/\``, '');
        for (const file of files) {
            const info = parse(file);
            const name = path.posix.basename(file);
            out.push(`- **${name}** (${info.lines})${info.purpose ? ' - ' + info.purpose : ''}`);
            if (info.types.length) out.push(`  - types: ${info.types.join(', ')}`);
            if (info.funs.length) out.push(`  - fun: ${info.funs.join(', ')}`);
            if (info.routes.length) out.push(`  - nav routes: ${info.routes.join(', ')}`);
        }
        out.push('');
    }
    out.push('## Tests', ...tests.map(t => `- \`${t}\``), '');
    return out.join('\n');
}

function main() {
    const map = build();
    if (process.argv.includes('--check')) {
        const current = fs.existsSync(OUTPUT_FILE) ? fs.readFileSync(OUTPUT_FILE, 'utf8').replace(/\r\n/g, '\n') : '';
        if (current !== map) {
            console.error(`${OUTPUT_FILE} is stale. Run: npm run update-maps`);
            process.exit(1);
        }
        console.log(`${OUTPUT_FILE} is up to date.`);
        return;
    }
    fs.mkdirSync(path.dirname(OUTPUT_FILE), { recursive: true });
    fs.writeFileSync(OUTPUT_FILE, map);
    console.log(`Saved ${OUTPUT_FILE} (${map.length} chars, ~${Math.round(map.length / 4)} tokens)`);
}

main();
