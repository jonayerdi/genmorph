#!/usr/bin/env python3
'''
Scatterplot FPs and MSs of indicidual assertions.
'''

from random import random
import re 

from math import isfinite
from os import listdir
from os.path import join

import numpy as np
import matplotlib.pyplot as plt

from evaluation.evaluate_results import Evaluation

EXPERIMENTS = re.compile(r'^assertions_(.+)_seed(\d+)$')
EVALUATIONS_FILE = '{sut}.evaluation.csv'

def plot_results_scatter(results, title='Plot'):
    colors = (c for c in (
        'royalblue',
        'mediumseagreen',
        'violet',
        'purple'
    ))
    for strategy in results:
        results_strategy = results[strategy]
        JITTER = 0.01
        for i in range(len(results_strategy['FP'])):
            results_strategy['FP'][i] += (random() * JITTER) - (JITTER / 2)
            results_strategy['MS'][i] += (random() * JITTER) - (JITTER / 2)
        plt.scatter(results_strategy['FP'], results_strategy['MS'], s=10, c=next(colors), alpha=1.0, marker='x', label=strategy)
    plt.xlabel("FP")
    plt.ylabel("MS")
    plt.legend(loc='upper left')
    plt.gca().invert_xaxis()
    plt.title(title)
    plt.show()

def plot_results_scatter_dir(evaluations_dir, sut):
    results = {}
    evaluations_file = EVALUATIONS_FILE.format(sut=sut)
    for experiment in listdir(evaluations_dir):
        experiment_match = EXPERIMENTS.match(experiment)
        if experiment_match is not None:
            experiment_dir = join(evaluations_dir, experiment)
            strategy = experiment_match.group(1)
            results.setdefault(strategy, { k: [] for k in ('FP', 'MS') })
            with open(join(experiment_dir, evaluations_file), mode='r') as fp:
                for evaluation in Evaluation.read_from_file(fp):
                    fpp = evaluation.fp().percentage()
                    msp = evaluation.ms().percentage()
                    if isfinite(fpp) and isfinite(msp):
                        results[strategy]['FP'].append(fpp)
                        results[strategy]['MS'].append(msp)
                    else:
                        results[strategy]['FP'].append(0.0)
                        results[strategy]['MS'].append(0.0)
    plot_results_scatter(results=results, title=sut.replace('%', '/'))

def main():
    import sys
    if len(sys.argv) != 3:
        print('./plot_results_scatter.py <EVALUATIONS_DIR> <SUT>')
        exit(1)
    plot_results_scatter_dir(evaluations_dir=sys.argv[1], sut=sys.argv[2])
