#!/usr/bin/env python3
'''
Evaluates all the results for the given results directory.
'''

from os import listdir, makedirs
from os.path import join

from evaluation.evaluate_results import evaluate_results_dir

def evaluate_results_all(results_dir, evaluations_dir):
    for experiment in listdir(results_dir):
        experiment_dir = join(results_dir, experiment)
        experiment_evaluation_dir = join(evaluations_dir, experiment)
        makedirs(experiment_evaluation_dir, exist_ok=True)
        for sut in listdir(experiment_dir):
            sut_dir = join(experiment_dir, sut)
            sut_evaluation_file = f'{sut}.evaluation.csv'
            evaluations = evaluate_results_dir(sut_dir)
            if len(evaluations) > 0:
                with open(join(experiment_evaluation_dir, sut_evaluation_file), mode='w') as fp:
                    fp.write('\n'.join([evaluations[0].header_str(), *map(str, evaluations)]))

def main():
    import sys
    if len(sys.argv) != 3:
        print('./evaluate_results_all.py <RESULTS_DIR> <EVALUATIONS_DIR>')
        exit(1)
    evaluate_results_all(results_dir=sys.argv[1], evaluations_dir=sys.argv[2])
