from copy import deepcopy
from os.path import join

from filetypes.sut_config import SUTConfig
from states.generate_test_executions import SYSTEM_ID

SEEDS = list(range(12))
SUT_CONFIG = SUTConfig.from_filename(join('configs', 'sut-config-commons-math3.json'))
TESTSUITE_KILLED = join('results-commons-math3', 'randoop_all_killed')
TESTSUITE_EXPERIMENT = lambda seed: f'randoop_seed_{seed}'
MRS_KILLED = lambda seed: join('results-commons-math3', f'pitest_seed_{seed}')

def load_data(file):
    results = {}
    with open(file, mode='r') as fp:
        lines = iter(fp)
        header = next(lines)
        for line in lines:
            data = line.strip().split(',')
            if data[1] == '*':
                results[data[0]] = list(map(int, data[2:-1]))
    return results

def merge_kills(killed):
    return list(map(lambda k: int(sum(k) != 0), zip(*killed)))
    merged = deepcopy(next(killed))
    if merged is None:
        return None
    for data in killed:
        assert len(data) == len(merged), f'merge_kills mismatched lengths: {len(data)} != {len(merged)}'
        merged = list(map(lambda k: int(bool(k[0] + k[1])), zip(merged, data)))
    return merged

def compare_testsuite_mrs():
    print('sut,seed,mutants,killed,missed,strict_delta,relax_delta')
    for sut_class, sut_method, sut_method_index in SUT_CONFIG.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        # Collect mutants killed by testsuite
        testsuite_noprocessed = load_data(join(TESTSUITE_KILLED, f'{system_id}.killed.csv'))
        testsuite = { seed: testsuite_noprocessed[TESTSUITE_EXPERIMENT(seed)] for seed in SEEDS }
        testsuite['*'] = testsuite_noprocessed['*']
        mutants = len(testsuite[SEEDS[0]])
        # Collect mutants killed by MRs
        strict = {}
        relax = {}
        for seed in SEEDS:
            mrs = load_data(join(MRS_KILLED(seed), system_id, 'mutants_killed.csv'))
            strict[seed] = merge_kills(map(lambda k: mrs[k], filter(lambda k: k.startswith('assertions_transform_seed'), mrs)))
            relax[seed] = merge_kills(map(lambda k: mrs[k], filter(lambda k: k.startswith('assertions_transform_relax_seed'), mrs)))
        strict['*'] = merge_kills(strict.values())
        relax['*'] = merge_kills(relax.values())
        # Compare mutants killed
        for seed in [*SEEDS, '*']:
            killed = sum(testsuite[seed])
            missed = mutants - killed
            strict_delta = sum(map(lambda _: 1, filter(lambda k: k[1] and not k[0], zip(testsuite[seed], strict[seed]))))
            relax_delta = sum(map(lambda _: 1, filter(lambda k: k[1] and not k[0], zip(testsuite[seed], relax[seed]))))
            print(f'{sut_method},{seed},{mutants},{killed},{missed},{strict_delta},{relax_delta}')

def main():
    compare_testsuite_mrs()
