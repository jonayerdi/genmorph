#!/usr/bin/env python3
'''
Plot the FN fitnesses obtained by GAssertMRs for some MRs

Used to compare FN fitness performance of GAssertMRs VS Unguided
'''

from math import ceil
from os.path import join

import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

def main():
    GASSERT = join('commons-math3-evaluation-2', 'fns.csv')
    UNGUIDED = join('commons-math3-evaluation-2', 'fns-unguided.csv')

    SUT_NAME = lambda sut: sut.split('%')[1]

    def load_data(file):
        data = {}
        with open(file, mode='r') as fp:
            assert next(fp).strip() == 'SUT,STRATEGY,FN'
            for row in filter(lambda l: l, map(lambda l: l.strip().split(','), fp)):
                sut, strategy, fn = row
                data.setdefault(sut, {})
                data[sut].setdefault(strategy, [])
                data[sut][strategy].append(float(fn))
        return data

    data_gassert = load_data(GASSERT)
    data_unguided = load_data(UNGUIDED)

    COLUMNS = 4
    ROWS = ceil(len(data_gassert) / COLUMNS)

    plt.rcdefaults()
    fig, axes = plt.subplots(nrows=ROWS, ncols=COLUMNS, gridspec_kw={ 'top': 0.90, 'bottom': 0.1, 'left': 0.05, 'right': 0.95 })
    plt.subplots_adjust(wspace=0, hspace=0.3)
    fig.set_figheight(20)
    fig.set_figwidth(20)

    for sut_idx, sut in enumerate(data_gassert.keys()):
        ax = axes[sut_idx // COLUMNS][sut_idx % COLUMNS]
        ax.margins(x=0, y=20) 
        sut_name = SUT_NAME(sut)

        labels = ['STRICT\nUnguided', 'STRICT\nGenMorph', 'RELAXED\nUnguided', 'RELAXED\nGenMorph']
        strategies = {
            'STRICT\nUnguided': data_unguided[sut]['transform'],
            'STRICT\nGenMorph': data_gassert[sut]['transform'],
            'RELAXED\nUnguided': data_unguided[sut]['transform_relax'],
            'RELAXED\nGenMorph': data_gassert[sut]['transform_relax'],
        }
        x = np.arange(len(labels))

        y = np.arange(0.0, 1.1, .2)
        ylabels = list(map(lambda n: f'{n:.1f}', y))

        data = pd.DataFrame(strategies)

        box = sns.boxplot(ax=ax, orient='v', data=data, width=.45, fliersize=.0, palette="colorblind")
        sns.stripplot(ax=ax, data=data, jitter=True, marker='o', alpha=0.5, color='black')
        
        ax.set_ylim(0.0, 1.0)
        ax.set_yticks(y)
        if sut_idx % COLUMNS == 0:
            ax.set_ylabel('FN-fitness')
            ax.set_yticklabels(ylabels)
        else:
            ax.set_yticklabels([])
        ax.set_xticks(x, labels)
        ax.set_xticklabels(labels, rotation=0)
        ax.set_title(sut_name, fontdict={'fontsize': 16.0})

    plt.show()
