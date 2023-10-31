#!/usr/bin/env python3
'''
Write out aggregate results for a SUT, based on the output of `pitest_results.py`
'''

import math
from os import listdir
from os.path import isfile, join
from statistics import median, mean

AGGREGATE_FN = mean

STRATEGIES = [
    'transform',
    'transform_relax',
    'unguided_transform',
    'unguided_transform_relax',
    'baseline1',
    'baseline2',
]

def ignore_nan(fn):
    def new_fn(data):
        filtered = list(filter(lambda d: not math.isnan(d), data))
        if filtered:
            return fn(filtered)
        else:
            return float('nan')
    return new_fn

def read_results_file(results_file, column_name):
    HEADER = ['STRATEGY', 'SEED', 'TEST_SEED', 'FP', 'MS', 'SAT']
    column_name = column_name.strip().upper()
    column = 0
    for i, name in enumerate(HEADER):
        if name.upper() == column_name:
            column = i
            break
    if not column:
        raise Exception(f'Invalid column: {column_name}')
    data = { strategy: {} for strategy in STRATEGIES }
    with open(results_file, mode='r') as fd:
        lines = iter(fd)
        header = next(lines).strip().split(',')
        assert header == HEADER, f'Unexpected header in {results_file}'
        for line in lines:
            line = line.strip().split(',')
            if len(line) > 1:
                strategy, seed, test_seed = line[:3]
                data[strategy][(seed, test_seed)] = float(line[column])
    return data

def aggregate(data, fn=AGGREGATE_FN):
    return {
        strategy: fn((
            data[strategy][seed] 
            for seed in data[strategy]
        )) 
        for strategy in data 
    }

def results_aggregate(results_file, aggregate_fn=AGGREGATE_FN):
    fp = aggregate(read_results_file(results_file=results_file, column_name='fp'), fn=aggregate_fn)
    ms = aggregate(read_results_file(results_file=results_file, column_name='ms'), fn=aggregate_fn)
    sat = aggregate(read_results_file(results_file=results_file, column_name='sat'), fn=ignore_nan(aggregate_fn))
    for strategy in fp:
        yield strategy, fp[strategy], ms[strategy], sat.get(strategy, float('nan'))

def main():
    import sys
    if len(sys.argv) != 2:
        print('./results_aggregate.py <RESULTS_FILE_OR_DIR>')
        exit(1)
    results_file = sys.argv[1]
    if isfile(results_file):
        print('STRATEGY,FP,MS,SAT')
        for strategy, fp, ms, sat in results_aggregate(results_file=results_file):
            print(f'{strategy},{fp},{ms},{sat}')
    else:
        for file in listdir(results_file):
            if isfile(join(results_file, file)) and file.endswith('.results.csv'):
                infile = join(results_file, file)
                outfilename = file[:-len('.results.csv')] + '.aggregate.csv'
                outfile = join(results_file, outfilename)
                with open(outfile, mode='wb') as fd:
                    fd.write('STRATEGY,FP,MS,SAT\n'.encode(encoding='utf-8'))
                    for strategy, fp, ms, sat in results_aggregate(results_file=infile):
                        fd.write(f'{strategy},{fp},{ms},{sat}\n'.encode(encoding='utf-8'))
