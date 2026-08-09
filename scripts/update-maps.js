const fs = require('fs');
const path = require('path');

const TARGET_DIRS = ['app/src', 'shared/src', 'wear/src'];
const OUTPUT_FILE = path.join('.agents', 'maps', 'types_index.json');

function findKotlinFiles(dir, fileList = []) {
    if (!fs.existsSync(dir)) return fileList;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            findKotlinFiles(filePath, fileList);
        } else if (filePath.endsWith('.kt')) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

function extractTypes() {
    const typeMap = {};
    const regex = /^(?:(?:public|private|internal|protected|open|sealed|abstract)\s+)*(data class|class|interface|enum class)\s+([A-Za-z0-9_]+)/;

    for (const dir of TARGET_DIRS) {
        const files = findKotlinFiles(dir);
        for (const file of files) {
            const content = fs.readFileSync(file, 'utf8');
            const lines = content.split('\n');
            lines.forEach((line, index) => {
                const match = line.trim().match(regex);
                if (match) {
                    const typeType = match[1];
                    const typeName = match[2];
                    typeMap[typeName] = {
                        type: typeType,
                        file: file.replace(/\\/g, '/'),
                        line: index + 1
                    };
                }
            });
        }
    }
    return typeMap;
}

function main() {
    console.log('Extracting Kotlin types...');
    const types = extractTypes();
    
    // Ensure output directory exists
    const outDir = path.dirname(OUTPUT_FILE);
    if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
    }
    
    fs.writeFileSync(OUTPUT_FILE, JSON.stringify(types, null, 2));
    console.log(`Saved ${Object.keys(types).length} types to ${OUTPUT_FILE}`);
}

main();
