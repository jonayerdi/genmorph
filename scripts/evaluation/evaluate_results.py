#!/usr/bin/env python3
'''
Evaluates the results for the given results CSV file(s), yielding TP, TN, FP, FN, DF, MS, and kill counts per mutant.
'''

from os import listdir
from os.path import isdir, isfile, join
import re

from util.normalized_number import NormalizedNumber

class Evaluation:
    SEPARATOR = ','
    def __init__(self, assertion_id, mutants):
        self.assertion_id = assertion_id
        self.mutants = mutants
        self.TP = 0
        self.TN = 0
        self.FP = 0
        self.FN = 0
        self.kills = [0 for _ in mutants]
    def fp(self):
        return NormalizedNumber(self.FP, self.FP + self.TN)
    def fn(self):
        return NormalizedNumber(self.FN, self.FN + self.TP)
    def df(self):
        return NormalizedNumber(self.TP, self.TP + self.FN)
    def ms(self):
        return NormalizedNumber(
            sum(1 for _ in filter(lambda n: n > 0, self.kills)),
            len(self.mutants) - 1,
        )
    def header_str(self):
        return Evaluation.SEPARATOR.join(('assertion_id', 'TP', 'TN', 'FP', 'FN', 'FP%', 'FN%', 'DF%', 'MS%', *self.mutants))
    def __str__(self):
        return Evaluation.SEPARATOR.join((
            self.assertion_id,
            f'{self.TP}', f'{self.TN}', f'{self.FP}', f'{self.FN}',
            f'{self.fp()}', f'{self.fn()}', f'{self.df()}', f'{self.ms()}',
            *map(lambda x: f'{x}', self.kills)
        ))
    @staticmethod
    def read_from_file(fp):
        lines = iter(fp)
        header = next(lines).split(Evaluation.SEPARATOR)
        assert header[:9] == ['assertion_id', 'TP', 'TN', 'FP', 'FN', 'FP%', 'FN%', 'DF%', 'MS%']
        mutants = header[9:]
        for line in lines:
            line = line.strip().split(Evaluation.SEPARATOR)
            if line:
                cells = iter(line)
                evaluation = Evaluation(assertion_id=next(cells), mutants=mutants)
                evaluation.TP = int(next(cells))
                evaluation.TN = int(next(cells))
                evaluation.FP = int(next(cells))
                evaluation.FN = int(next(cells))
                assert NormalizedNumber.parse(next(cells)) is not None
                assert NormalizedNumber.parse(next(cells)) is not None
                assert NormalizedNumber.parse(next(cells)) is not None
                assert NormalizedNumber.parse(next(cells)) is not None
                evaluation.kills = [int(c) for c in cells]
                assert len(evaluation.kills) == len(mutants)
                yield evaluation

def evaluate_results(assertion_id, mutants, rows):
    evaluation = Evaluation(assertion_id=assertion_id, mutants=mutants)
    for row in rows:
        assert len(mutants) + 1 == len(row)
        testcase_id = row[0]
        testcase_kills = [False for _ in mutants]
        testcase_has_fp = False
        for index, result in enumerate(row[1:]):
            if result == '-':
                pass
            elif result == 'X':
                evaluation.TP += 1
                testcase_kills[index] = True
            elif result == 'O':
                evaluation.TN += 1
            elif result == '!':
                evaluation.FP += 1
                testcase_has_fp = True
            elif result == '?':
                evaluation.FN += 1
            else:
                raise Exception(f'Unknown result value: {result}')
        if not testcase_has_fp:
            for index in range(len(mutants)):
                evaluation.kills[index] += int(testcase_kills[index])
    return evaluation

def evaluate_results_file(file, separator=Evaluation.SEPARATOR):
    assertion_id = None
    mutants = None
    rows = None
    with open(file, mode='r') as fd:
        lines = iter(fd)
        headers = next(lines).strip().split(separator)
        assertion_id = headers[0]
        mutants = headers[1:]
        rows = map(lambda line: line.strip().split(separator), lines)
        return evaluate_results(assertion_id=assertion_id, mutants=mutants, rows=rows)

def evaluate_results_dir(directory, separator=Evaluation.SEPARATOR):
    evaluations = []
    for child in listdir(directory):
        file = join(directory, child)
        if isfile(file) and child.endswith('.results.csv'):
            evaluation = evaluate_results_file(file=file, separator=separator)
            evaluation.assertion_id = child[:-len('.results.csv')]
            if len(evaluations) > 0:
                assert evaluation.mutants == evaluations[0].mutants, f'Mutants from "{file}" do not match the ones from other result files'
            evaluations.append(evaluation)
    return evaluations

def main():
    import sys
    if len(sys.argv) not in [2, 3]:
        print('./evaluate_results.py <RESULTS_FILE_OR_DIR> [<OUTPUT_FILE>]')
        exit(1)
    results_file_or_dir = sys.argv[1]
    fp = sys.stdout
    if len(sys.argv) > 2:
        fp = open(sys.argv[2], mode='w')
    try:
        if isdir(results_file_or_dir):
            evaluations = evaluate_results_dir(results_file_or_dir)
            if len(evaluations) > 0:
                fp.write('\n'.join([evaluations[0].header_str(), *map(str, evaluations)]))
        else:
            evaluation = evaluate_results_file(results_file_or_dir)
            fp.write('\n'.join([evaluation.header_str(), str(evaluation)]))
    finally:
        if fp != sys.stdout:
            fp.close()
