from os import listdir
from os.path import join

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

from util.fsutil import isnonemptydir

WORKDIRS = [join('baseline_ICST', f'{i}') for i in range(12)]
SEPARATOR = ','

def inner_dir(root):
    for child in listdir(root):
        c = join(root, child)
        if isnonemptydir(c):
            return c

def killed(file):
    m = []
    with open(file, mode='r') as fd:
        for line in fd:
            row = line.split(SEPARATOR)
            if row:
                m.append(row[5].strip() in ['KILLED', 'TIMED_OUT'])
    return sum(filter(lambda x: x, m)) / len(m)

def main():
    data = {}
    for workdir in WORKDIRS:
        pitdir = join(workdir, 'pitest')
        for sut in listdir(pitdir):
            ex = inner_dir(join(pitdir, sut))
            results = killed(join(ex, 'mutations.csv'))
            sutname = sut.split('%')[-2]
            data.setdefault(sutname, [])
            data[sutname].append(results)

    pdata = pd.DataFrame(data=data)
    sns.set_theme()
    sns.boxplot(orient='v', data=pdata, width=.45, fliersize=.0, palette="colorblind")
    sns.stripplot(data=pdata, jitter=True, marker='o', alpha=0.5, palette='dark:black')
    plt.show()
