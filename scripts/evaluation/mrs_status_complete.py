from os import remove
from shutil import copyfile
from tempfile import TemporaryFile

from util.normalized_number import NormalizedNumber

def EXPERIMENT_MRS():
    experiments = ['baseline1', 'baseline2', 'transform', 'transform_relax']
    seeds = [10 * d + n for d in range(1, 5) for n in range(1, 5)]
    mrs = [f'MR{n}' for n in range(4)]
    for experiment in experiments:
        for seed in seeds:
            for mr in mrs:
                yield f'assertions_{experiment}_seed{seed}', mr

def mrs_status_complete(mrs_status, experiment_mrs, mrs_status_completed=None):
    tmpfile = None
    outfile = None
    if (mrs_status_completed is None) or (mrs_status_completed == mrs_status):
        mrs_status_completed = mrs_status
        tmpfile = TemporaryFile(mode='wb', delete=False)
        outfile = tmpfile.file
    else:
        outfile = open(mrs_status, mode='r')
    data = {}
    with open(mrs_status, mode='r') as fp:
        lines = iter(fp)
        header = next(lines).strip()
        assert header == 'EXPERIMENT,MR,FP,MS', f'Unexpected header: "{header}"'
        for line in lines:
            experiment, mr, fps, ms = line.strip().split(',')
            data[(experiment, mr)] = (fps, ms)
    outfile.write('EXPERIMENT,MR,FP,MS\n'.encode(encoding='utf-8'))
    for experiment, mr in experiment_mrs:
        fps, ms = data.get((experiment, mr), (NormalizedNumber(0, 0), NormalizedNumber(0, 0)))
        outfile.write(f'{experiment},{mr},{fps},{ms}\n'.encode(encoding='utf-8'))
    outfile.close()
    if tmpfile is not None:
        copyfile(src=tmpfile.name, dst=mrs_status_completed)
        try:
            remove(tmpfile.name)
        except: pass

def main():
    import sys
    mrs_status = sys.argv[1]
    mrs_status_completed = None
    if len(sys.argv) > 2:
        mrs_status_completed=sys.argv[2]
    mrs_status_complete(mrs_status=mrs_status, mrs_status_completed=mrs_status_completed, experiment_mrs=EXPERIMENT_MRS())
