import re
from statistics import mean

from util.normalized_number import NormalizedNumber

REGEX_EXPERIMENT = re.compile(r'^assertions_(.+)_seed\d+$')
EXPERIMENTS = ['baseline1', 'baseline2', 'transform', 'transform_relax']

def mrs_status_results(mrs_status_file, experiments=EXPERIMENTS):
    with open(mrs_status_file, mode='r') as fp:
        lines = iter(fp)
        header = next(lines).strip()
        assert header == 'EXPERIMENT,MR,FP,MS', f'Unexpected header: "{header}"'
        results = { k: (NormalizedNumber(0, 0), []) for k in experiments }
        for line in lines:
            if line.strip():
                experiment, mr, fps, ms = line.strip().split(',')
                fps = NormalizedNumber.parse(fps)
                ms = NormalizedNumber.parse(ms)
                k = REGEX_EXPERIMENT.match(experiment).group(1)
                results[k][0].divisor += 1
                if fps.value == 0:
                    if ms.value > 0:
                        results[k][0].value += 1
                    results[k][1].append(ms)
        return { k: (results[k][0], NormalizedNumber.sum(results[k][1])) for k in results}

def main():
    import sys
    results = mrs_status_results(mrs_status_file=sys.argv[1])
    print('EXPERIMENT,NO_FP,MS')
    for experiment in results:
        no_fp, ms = results[experiment]
        print(f'{experiment},{no_fp},{ms}')
