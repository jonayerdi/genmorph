#!/usr/bin/env python3
'''
Aggregate multiple PITest results into a single CSV
'''

from os.path import split, join, isdir
from zipfile import ZipFile

def pitest_results_aggregate(entries, out_fp, names=None, line_filter=None):
    if names is None:
        def default_names():
            while True:
                yield '?'
        names = default_names()
    if line_filter is None:
        line_filter = lambda l: l and '*,' not in line
    name = next(names)
    data = next(entries).read()
    lines = iter(data.decode(encoding='utf-8').split('\n'))
    header = next(lines).strip()
    out_fp.write(f'{header}\n'.encode(encoding='utf-8'))
    for line in lines:
        line = line.strip()
        if line_filter(line):
            out_fp.write(f'{line}\n'.encode(encoding='utf-8'))
    for entry in entries:
        lines = iter(entry.read().decode(encoding='utf-8').split('\n'))
        header2 = next(lines).strip()
        name2 = next(names)
        assert header2 == header, f'Headers do not match:\n"{name}: {header}"\n"{name2}: {header2}"'
        for line in lines:
            line = line.strip()
            if line_filter(line):
                out_fp.write(f'{line}\n'.encode(encoding='utf-8'))

def pitest_results_extract_mutants_killed(files, outdir):
    entries = {}
    zfs = []
    for file in files:
        zf = ZipFile(file, mode='r')
        zfs.append(zf)
        for zi in zf.filelist:
            if split(zi.filename)[1] == 'mutants_killed.csv':
                sut = split(split(zi.filename)[0])[1]
                entries.setdefault(sut, {})
                entries[sut].setdefault(zf, [])
                entries[sut][zf].append(zi)
    for sut in entries:
        with open(join(outdir, f'{sut}.csv'), mode='wb') as out_fp:
            sut_entries = []
            for zf in entries[sut]:
                sut_entries.extend(map(lambda zi: zf.open(zi, mode='r'), entries[sut][zf]))
            pitest_results_aggregate(
                entries=iter(sut_entries),
                out_fp=out_fp,
            )
    for zf in zfs:
        zf.close()

def main():
    import sys
    outdir = '.'
    files = sys.argv[1:]
    if isdir(files[0]):
        outdir = files[0]
        files = files[1:]
    pitest_results_extract_mutants_killed(files=files, outdir=outdir)
