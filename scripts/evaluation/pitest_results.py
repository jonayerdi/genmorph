#!/usr/bin/env python3
'''
Aggregate multiple PITest results into a single CSV, based on the output of `evaluate_pitest.py`
'''

import math
import re
from os import listdir
from os.path import join

from util.normalized_number import NormalizedNumber

PITEST_DIRS = re.compile(r'^pitest_seed_(\d+)$')
STRATEGIES = re.compile(r'^assertions_(.+)_seed(\d+)$')

def pitest_list_dirs(pitest_root):
    for pitest_dir in listdir(pitest_root):
        match = PITEST_DIRS.match(pitest_dir)
        if match:
            yield join(pitest_root, pitest_dir), match.group(1)

def read_oasis_file(column_name, oasis_file=None):
    if oasis_file is None:
        return {}
    HEADER = ['Approach', 'Seed', 'Class', 'Method', 'MR', 'FP', 'Satisfiability Ratio']
    column_name = column_name.strip().upper()
    column = 0
    for i, name in enumerate(HEADER):
        if name.upper() == column_name:
            column = i
            break
    if not column:
        raise Exception(f'Invalid column: {column_name}')
    data = {}
    with open(oasis_file, mode='r') as fd:
        lines = iter(fd)
        header = next(lines).strip().split(',')
        assert header == HEADER, f'Unexpected header in {oasis_file}'
        for line in lines:
            line = line.strip().split(',')
            if len(line) > 1:
                sut = f'{line[2]}%{line[3]}%0'
                strategy, seed, mr = line[0], line[1], f'MR{line[4]}'
                data.setdefault(sut, {})
                data[sut][(strategy, seed, mr)] = float(line[column])
    return data

def get_random_fps(mrs_status_file):
    data = {}
    with open(mrs_status_file, mode='r') as fd:
        lines = iter(fd)
        header = next(lines).strip().split(',')
        assert header == ['EXPERIMENT', 'MR', 'FP', 'MS'], f'Unexpected header in {mrs_status_file}'
        for line in lines:
            line = line.strip().split(',')
            if len(line) > 1:
                experiment, mr, fp, ms = line
                strategy, seed = STRATEGIES.match(experiment).groups()
                fp = NormalizedNumber.parse(fp)
                data[(strategy, seed, mr)] = fp.value
    return data

def aggregate_fp(mrs_status_file, oasis_fps={}):
    results = {}
    with open(mrs_status_file, mode='r') as fd:
        lines = iter(fd)
        header = next(lines).strip().split(',')
        assert header == ['EXPERIMENT', 'MR', 'FP', 'MS'], f'Unexpected header in {mrs_status_file}'
        for line in lines:
            line = line.strip().split(',')
            if len(line) > 1:
                experiment, mr, fp, ms = line
                strategy, seed = STRATEGIES.match(experiment).groups()
                oasis_fp = int(oasis_fps.get((strategy, seed, mr), 0))
                fp = NormalizedNumber.parse(fp)
                results.setdefault(experiment, NormalizedNumber(0, 0))
                results[experiment].divisor += 1
                if fp.value != 0 or oasis_fp != 0:
                    results[experiment].value += 1
    mrs = None
    for experiment in results:
        strategy, seed = STRATEGIES.match(experiment).groups()
        fp = results[experiment]
        if mrs is None:
            mrs = fp.divisor
        assert fp.divisor == mrs, f'Expected {mrs} MRs, found {fp.divisor} for {experiment} in {mrs_status_file}'
        yield strategy, seed, fp.percentage()

def aggregate_ms(mutants_killed_file, oasis_fps={}):
    results = {}
    with open(mutants_killed_file, mode='r') as fd:
        lines = iter(fd)
        header = next(lines).strip().split(',')
        assert header[0] == 'EXPERIMENT' and header[1] == 'MR' and header[-1] == 'COUNT', f'Unexpected header in {mutants_killed_file}'
        mutants = len(header) - 3
        for line in lines:
            line = line.strip().split(',')
            if len(line) > 1:
                experiment = line[0]
                mr = line[1]
                if experiment != '*' and mr != '*':
                    strategy, seed = STRATEGIES.match(experiment).groups()
                    oasis_fp = int(oasis_fps.get((strategy, seed, mr), 0))
                    results.setdefault(experiment, [0 for _ in range(mutants)])
                    if oasis_fp == 0:
                        kills = list(map(lambda k: int(k), line[2:-1]))
                        results[experiment] = list(map(lambda k: k[0] + k[1], zip(results[experiment], kills)))
    for experiment in results:
        strategy, seed = STRATEGIES.match(experiment).groups()
        ms = sum((1 for _ in filter(lambda k: k > 0, results[experiment]))) / mutants
        yield strategy, seed, ms

def aggregate_sat(oasis_sat):
    aggregate = {}
    count = {}
    for strategy, seed, mr in oasis_sat:
        aggregate.setdefault((strategy, seed), 0.0)
        count.setdefault((strategy, seed), 0)
        value = oasis_sat[(strategy, seed, mr)]
        if math.isfinite(value):
            aggregate[(strategy, seed)] += oasis_sat[(strategy, seed, mr)]
            count[(strategy, seed)] += 1
    for strategy, seed in aggregate:
        sat = aggregate[(strategy, seed)] / count[(strategy, seed)]
        yield strategy, seed, sat

def pitest_results(pitest_dirs, outdir, oasis_file=None):
    pitest_dirs = list(pitest_dirs)
    results = {}
    oasis_fps = read_oasis_file(oasis_file=oasis_file, column_name='fp')
    oasis_sat = read_oasis_file(oasis_file=oasis_file, column_name='Satisfiability Ratio')
    for pitest_dir, pitest_seed in pitest_dirs:
        for sut in listdir(pitest_dir):
            oasis_fps.setdefault(sut, {})
            random_fps = get_random_fps(mrs_status_file=join(pitest_dir, sut, 'mrs_status.csv'))
            for mr in random_fps:
                if mr in oasis_fps[sut]:
                    oasis_fps[sut][mr] += random_fps[mr]
                else:
                    oasis_fps[sut][mr] = random_fps[mr]
    for pitest_dir, pitest_seed in pitest_dirs:
        for sut in listdir(pitest_dir):
            oasis_fps_sut = oasis_fps.get(sut, {})
            results.setdefault(sut, {})
            for strategy, seed, fp in aggregate_fp(mrs_status_file=join(pitest_dir, sut, 'mrs_status.csv'), oasis_fps=oasis_fps_sut):
                results[sut][(strategy, seed, pitest_seed)] = (fp, 0.0, float('nan'))
            for strategy, seed, ms in aggregate_ms(mutants_killed_file=join(pitest_dir, sut, 'mutants_killed.csv'), oasis_fps=oasis_fps_sut):
                fp, _, _ = results[sut][(strategy, seed, pitest_seed)]
                results[sut][(strategy, seed, pitest_seed)] = (fp, ms, float('nan'))
            for strategy, seed, sat in aggregate_sat(oasis_sat=oasis_sat.get(sut, {})):
                fp, ms, _ = results[sut][(strategy, seed, pitest_seed)]
                results[sut][(strategy, seed, pitest_seed)] = (fp, ms, sat)
    for sut in results:
        with open(join(outdir, f'{sut}.results.csv'), mode='wb') as fd:
            fd.write(f'STRATEGY,SEED,TEST_SEED,FP,MS,SAT\n'.encode(encoding='utf-8'))
            lines = []
            for strategy, seed, pitest_seed in results[sut]:
                fp, ms, sat = results[sut][(strategy, seed, pitest_seed)]
                lines.append(f'{strategy},{seed},{pitest_seed},{fp},{ms},{sat}\n')
            lines.sort()
            for line in lines:
                fd.write(line.encode(encoding='utf-8'))

def main():
    import sys
    if len(sys.argv) not in [3, 4]:
        print('./pitest_results.py <PITESTS_ROOT> <OUTDIR> [<OASIS_FILE>]')
        exit(1)
    pitest_root = sys.argv[1]
    outdir = sys.argv[2]
    oasis_file = None
    if len(sys.argv) == 4:
        oasis_file = sys.argv[3]
    pitest_results(pitest_dirs=pitest_list_dirs(pitest_root), outdir=outdir, oasis_file=oasis_file)
