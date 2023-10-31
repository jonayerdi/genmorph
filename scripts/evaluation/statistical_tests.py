#!/usr/bin/env python3
'''
Perform statistical tests for a SUT, based on the output of `pitest_results.py`
'''

from os import listdir
from os.path import isfile, join
from statistics import mean
from scipy.stats import wilcoxon

from evaluation.results_aggregate import STRATEGIES, read_results_file

COMPARISONS = [
    ('transform', 'transform_relax'),
    ('transform', 'unguided_transform'),
    ('transform_relax', 'unguided_transform_relax'),
    ('transform', 'baseline1'),
    ('transform', 'baseline2'),
    ('transform_relax', 'baseline1'),
    ('transform_relax', 'baseline2'),
]

def a12_paired(a, b):
    '''
    A12 = P(a > b)
    '''
    more = same = 0.0
    for i in range(len(a)):
        if a[i] == b[i]:
            same += 1
        elif a[i] > b[i]:
            more += 1
    return (more + 0.5 * same) / len(a)

def effect_size(a12):
    e = 2 * abs(a12 - 0.5)
    if e < 0.147:
        return 'N'
    if e < 0.33:
        return 'S'
    if e < 0.474:
        return 'M'
    return 'L'

def statistical_tests(results_file, column_name):
    data = read_results_file(results_file=results_file, column_name=column_name)
    # Ensure same ordering for all strategies for Wilcoxon test
    keys_sorted = sorted(data[next(iter(STRATEGIES))].keys())
    data_sorted = { strategy: [] for strategy in STRATEGIES }
    for strategy in STRATEGIES:
        data_sorted[strategy] = [data[strategy][key] for key in keys_sorted]
    for strategy1, strategy2 in COMPARISONS:
        avg1, avg2 = mean(data_sorted[strategy1]), mean(data_sorted[strategy2])
        if avg1 > avg2:
            a12 = a12_paired(data_sorted[strategy1], data_sorted[strategy2])
        else:
            a12 = a12_paired(data_sorted[strategy2], data_sorted[strategy1])
        effect = effect_size(a12)
        try:
            w, p = wilcoxon(data_sorted[strategy1], data_sorted[strategy2], alternative='two-sided')
        except:
            if effect == 'N':
                w, p = float('nan'), 1
            else:
                w, p = float('nan'), 0
        yield strategy1, strategy2, avg1, avg2, w, p, a12, effect

def main():
    import sys
    if len(sys.argv) != 3:
        print('./statistical_tests.py <RESULTS_FILE_OR_DIR> <COLUMN>')
        exit(1)
    results_file = sys.argv[1]
    column_name = sys.argv[2]
    if isfile(results_file):
        print('STRATEGY1,STRATEGY2,AVG1,AVG2,WILCOXON,PVALUE,A12,EFFECT')
        for strategy1, strategy2, avg1, avg2, w, p, a12, effect in statistical_tests(results_file=results_file, column_name=column_name):
            print(f'{strategy1},{strategy2},{avg1},{avg2},{w},{p},{a12},{effect}')
    else:
        for file in listdir(results_file):
            try:
                if isfile(join(results_file, file)) and file.endswith('.results.csv'):
                    infile = join(results_file, file)
                    outfilename = file[:-len('.results.csv')] + f'.{column_name}.statistics.csv'
                    outfile = join(results_file, outfilename)
                    print(file[:-len(".results.csv")])
                    with open(outfile, mode='wb') as fd:
                        fd.write('STRATEGY1,STRATEGY2,AVG1,AVG2,WILCOXON,PVALUE,A12,EFFECT\n'.encode(encoding='utf-8'))
                        for strategy1, strategy2, avg1, avg2, w, p, a12, effect in statistical_tests(results_file=infile, column_name=column_name):
                            fd.write(f'{strategy1},{strategy2},{avg1},{avg2},{w},{p},{a12},{effect}\n'.encode(encoding='utf-8'))
            except Exception as e:
                print(f'Error in statistical tests for {file[:-len(".results.csv")]}:{column_name}: {e}')
