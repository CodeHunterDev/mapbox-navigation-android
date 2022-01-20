const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const prompts = require('prompts');
const semver = require('semver')
const { getBranchName } = require('./github');

// https://keepachangelog.com/en/1.0.0/
const ENTRY_TYPES = ['fixed', 'added', 'changed', 'removed', 'deprecated', 'security'];
const SUPPORTED_ENTRY_TYPES = ['added', 'fixed']
const HEADERS = {
    'fixed': "Bug fixes and improvements",
    'added': "Features"
}
const MONTHS = {
    0: 'January',
    1: 'February',
    2: 'March',
    3: 'April',
    4: 'May',
    5: 'June',
    6: 'July',
    7: 'August',
    8: 'September',
    9: 'October',
    10: 'November',
    11: 'December'
  }

const REPO_ROOT_DIR = path.join('.')
const UNRELEASED_CHANGELOG_DIR = path.join(REPO_ROOT_DIR, 'changelogs', 'unreleased');

function findPRNumber() {
    try {
        const number = JSON.parse(execSync('gh pr view --json number', { stdio: 'pipe' }).toString()).number;
        return number || null;
    } catch (e) {
        return null;
    }
}

function lastCommitMessage() {
    // Using the commit header (i.e. the firsts line in the commit) for the template
    return execSync("git log -1 --pretty=%s")
        .toString()
        .trim()
}


function isInteger(str) {
    return !isNaN(str) && Number.isInteger(parseFloat(str));
}

function isValidChangelogEntries(entries) {
    if (!Array.isArray(entries)){
        return false
    }
    for (const entry of entries) {
        let isValid = entry.title && SUPPORTED_ENTRY_TYPES.includes(entry.type) && isInteger(entry.ticket)
        if (!isValid) {
            return false
        }
    }
    return true
}

String.prototype.capitalize = function () {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

function processTitle(title) {
    // replace newlines with spaces to not break formatting in compiled changelog
    return title.replace(/\n/g, " ");
}

function compileChangelog(config) {
    const allEntries = {};
    for (const type of ENTRY_TYPES) {
        allEntries[type] = [];
    }

    let fileCreationDataProvider = config.fileCreationDataProvider === undefined
        ? function(path) { execSync(`git log -1 --format=%cd ${path}`).toString().trim() }
        : config.fileCreationDataProvider
    for (const entriesFile of fs.readdirSync(UNRELEASED_CHANGELOG_DIR)) {
        if (!entriesFile.endsWith('.json')) { continue; }

        const entriesFilePath = path.join(UNRELEASED_CHANGELOG_DIR, entriesFile);
        const entries = JSON.parse(fs.readFileSync(entriesFilePath));
        if (!isValidChangelogEntries(entries)) {
            throw `Cannot use entry "${entriesFile}"`
        }
        entries.forEach(function(e) {
            e['date'] = new Date(fileCreationDataProvider(entriesFilePath));
            allEntries[e.type].push(e);
        })
    }

    for (const type of Object.keys(allEntries)) {
        allEntries[type].sort((a, b) => b.date - a.date);
    }

    const owner = 'mapbox';
    const repo = 'mapbox-navigation-android';

    let output = '';
    for (const type of ENTRY_TYPES) {
        const typeEntries = allEntries[type];
        if (typeEntries.length === 0) { continue; }
        output += `#### ${HEADERS[type]}\n`;

        for (const entry of typeEntries) {
            const title = processTitle(entry.title);
            output += `- ${title} [#${entry.ticket}](https://github.com/${owner}/${repo}/pull/${entry.ticket})\n`;
        }
        output += "\n"
    }

    return output;
}

function addReleaseNotesToChangelogMD(currentChangelogMD, newReleaseChangelog) {
    let latestReleaseHeaderPosition = currentChangelogMD.indexOf("##")
    let updatedChangelog = currentChangelogMD.substring(0, latestReleaseHeaderPosition) 
        + newReleaseChangelog 
        + "\n\n"
        + currentChangelogMD.substring(latestReleaseHeaderPosition)
    return updatedChangelog
}

function compileReleaseNotesMd(config) {
    let version = config.version
    if (version == undefined) {
        throw "specify release version"
    }
    let releaseDate = config.releaseDate
    if (releaseDate == undefined) {
        throw "specify release date"
    }
    var output = `## Mapbox Navigation SDK ${version} - ${MONTHS[releaseDate.getMonth()]} ${releaseDate.getDate()}, ${releaseDate.getFullYear()}\n\n`
    let major = semver.major(version)
    let minor = semver.minor(version)
    if (major == "2") {
        if (minor == "0") {
            output += "This is a patch release on top of v2.0.x which does not include changes introduced in v2.1.x and later.  "
        }
        output += "For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see 2.0 Navigation SDK Migration Guide.\n\n" 
    }
    output += "##Changelog\n"
    output += compileChangelog(config)
    output += "### Mapbox dependencies\n"
    output += "This release depends on, and has been tested with, the following Mapbox dependencies:\n"
    output += config.dependenciesMd
    output += '\n'
    return output
}

function removeEntries() {
    fs.rmSync(UNRELEASED_CHANGELOG_DIR, { recursive: true, force: true })
}

function makeEntryPath(branchName) {
    return path.join(UNRELEASED_CHANGELOG_DIR, branchName.replace(/[^a-z0-9]/gi, '-') + '.json');
}

async function askQuestions(entry) {
    const questions = [];
    if (!entry.ticket) {
        questions.push({
            type: 'number',
            name: 'ticket',
            initial: findPRNumber() || undefined,
            message: 'Ticket number(PR or issue): '
        });
    }
    if (!entry.type) {
        const choices = SUPPORTED_ENTRY_TYPES.map(x => { return { title: x.capitalize(), value: x }; });
        questions.push({
            type: 'select',
            name: 'type',
            message: 'Type:',
            choices: choices
        })
    }
    if (!entry.title) {
        questions.push({
            type: 'text',
            name: 'title',
            initial: lastCommitMessage(),
            message: 'Title:',
        })
    }
    return Object.assign(entry, await prompts(questions));
}

async function createEntry(params, branchName) {
    const title = params.title;

    if (!isInteger(params.ticket) && params.ticket !== undefined) {
        throw `${params.ticket} is not valid PR/issue number`
    }

    const ticket = Number(params.ticket);
    const type = params.type;

    if (type && !ENTRY_TYPES.includes(type)) {
        throw `${type} is not valid changelog entry type. Valid types: ${ENTRY_TYPES.join(', ')}`
    }

    const entry = await askQuestions({
        ticket: ticket,
        type: type,
        title: title
    });

    branchName = branchName === undefined ? getBranchName() : branchName

    if (branchName === 'main') {
        throw 'Cannot create changelog entry on master branch'
    }
    
    const entryPath = makeEntryPath(branchName);
    let existingEntry = fs.existsSync(entryPath)
        ? JSON.parse(fs.readFileSync(entryPath))
        : []
    existingEntry.push(entry)
    let changelogPath = path.dirname(entryPath)
    let changeLogEntryContent = JSON.stringify(existingEntry, null, 2)
    if (!params.isDryRun) {
        fs.mkdirSync(changelogPath, { recursive: true });
        fs.writeFileSync(entryPath, changeLogEntryContent);
    } else {
        console.log(`dry-run: changelog entry created at ${entryPath}`)
        console.log(`dry-run: changelog entry content: \n ${changeLogEntryContent}`)
    }
}

module.exports = {
    compileReleaseNotesMd,
    isValidChangelogEntries
,
    removeEntries,
    makeEntryPath,
    createEntry,
    addReleaseNotesToChangelogMD
};