#!/usr/bin/env python3
'''
Obtains aggregate mutation score for matching assertions
'''

from evaluation.evaluate_results import Evaluation

MS = lambda kills: len(list(filter(lambda k: k > 0, kills)))
AGGREGATE_KILLS = lambda lists: list(map(lambda k: sum(k), zip(*lists)))
RESULTS_FILE_HEADER = Evaluation(None, []).header_str().split(Evaluation.SEPARATOR)
ASSERTION_ID_COLUMN = next(filter(lambda h: h[1] == 'assertion_id', enumerate(RESULTS_FILE_HEADER)))[0]
MUTANT_KILLS_FIRST_COLUMN = len(RESULTS_FILE_HEADER)

def get_kills_from_evaluation_file(evaluation_filename, assertion_ids_filter):
    with open(evaluation_filename, mode='r') as fd:
        lines = iter(fd)
        headers = next(lines).strip().split(Evaluation.SEPARATOR)
        assert headers[:MUTANT_KILLS_FIRST_COLUMN] == RESULTS_FILE_HEADER
        assertion_ids = []
        kills = []
        for row in filter(lambda r: r, map(lambda l: l.strip().split(Evaluation.SEPARATOR), lines)):
            assertion_id = row[ASSERTION_ID_COLUMN]
            if assertion_ids_filter(assertion_id):
                assertion_ids.append(assertion_id)
                kills.append(list(map(lambda v: int(v), row[MUTANT_KILLS_FIRST_COLUMN:])))
        return assertion_ids, kills

def main():
    import sys
    import re
    from util.normalized_number import NormalizedNumber
    if len(sys.argv) != 3:
        print('./mutation_score_aggregate.py <EVALUATION_FILE> <ASSERTION_IDS_REGEX>')
        exit(1)
    results_filename = sys.argv[1]
    assertion_ids_regex = re.compile(sys.argv[2])
    assertion_ids, kills = get_kills_from_evaluation_file(
        evaluation_filename=results_filename,
        assertion_ids_filter=lambda x: assertion_ids_regex.match(x) is not None
    )
    print(assertion_ids)
    kills_aggregate = AGGREGATE_KILLS(kills)
    ms = MS(kills_aggregate)
    print(NormalizedNumber(ms, len(kills_aggregate) - 1))
