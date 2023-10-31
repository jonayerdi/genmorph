#!/usr/bin/env python3
'''
Generates classification CSVs from method states, given the set of states and the name of the original system.

./classify_states.py <STATES_DIR> <OUTPUT_DIR> <ORIGINAL_SYSTEM>
'''

import json

from copy import deepcopy
from os import listdir, makedirs
from os.path import isfile, join, splitext, split

CLASSIFICATIONS_FILE = lambda s: f'{s}.classifications.csv'
CLASSIFICATIONS_KEY = lambda f: splitext(split(f)[1])[0]
IS_CLASSIFICATIONS_FILE = lambda f: f.endswith('.classifications.csv')

class Classification:
    SEPARATOR = ','

    class Row:
        def __init__(self, test_id, values):
            self.test_id = test_id
            self.values = values

    def __init__(self, system_id, keys, rows={}):
        self.system_id = system_id
        self.keys = keys
        self.rows = rows

    def add_row(self, row, overwrite=False):
        assert overwrite or row.test_id not in self.rows, f'Duplicate test_id: {row.test_id}'
        self.rows[row.test_id] = row

    def add_rows(self, rows, overwrite=False):
        for row in rows:
            self.add_row(row, overwrite=overwrite)

    def to_file(self, file):
        with open(file, mode='w') as fd:
            fd.write(f'{Classification.SEPARATOR.join((self.system_id, *self.keys))}\n')
            for row in self.rows.values():
                fd.write(f'{Classification.SEPARATOR.join((row.test_id, *(value for value in row.values)))}\n')

    @staticmethod
    def from_file(file):
        with open(file, mode='r') as fd:
            lines = iter(fd)
            header = next(lines).strip().split(Classification.SEPARATOR)
            system_id = header[0]
            keys = header[1:]
            rows = {}
            for line in lines:
                line = line.strip()
                if line:
                    row = line.split(Classification.SEPARATOR)
                    assert len(row) == len(header), f'Cannot parse CSV row:\n{line}'
                    row = Classification.Row(test_id=row[0], values=row[1:])
                    assert row.test_id not in rows, f'Duplicate test_id: {row.test_id}'
                    rows[row.test_id] = row
            return Classification(system_id=system_id, keys=keys, rows=rows)

    @staticmethod
    def combined(classifications):
        it = iter(classifications)
        classification = deepcopy(next(it))
        for other in it:
            assert classification.system_id == other.system_id, f'"{classification.system_id}" != "{other.system_id}"'
            assert classification.keys == other.keys, f'"{classification.keys}" != "{other.keys}"'
            classification.add_rows(other.rows, overwrite=False)
        return classification

def encode_faulty(is_faulty):
    if is_faulty:
        return 'X'
    return '?'

def is_faulty(outputs, reference):
    for var in outputs:
        if outputs[var] != reference[var]:
            return True
    return False

def classify_system_states(outfile, system_id, original_system, outputs, reference):
    outfile.write(f'{system_id},@\n')
    if system_id == original_system:
        for test_id in outputs:
            outfile.write(f'{test_id},O\n')
    else:
        for test_id in outputs:
            reference_output = reference[test_id]
            mutant_output = outputs[test_id]
            outfile.write(f'{test_id},{encode_faulty(is_faulty(mutant_output, reference_output))}\n')

def classify_states(states_dir, output_dir, original_system):
    makedirs(output_dir, exist_ok=True)
    outputs = {}
    for filename in listdir(states_dir):
        filepath = join(states_dir, filename)
        if isfile(filepath) and filename.endswith('.state.json'):
            state = None
            with open(filepath, mode='r') as fd:
                state = json.load(fd)
                system_id = state['systemId']
                test_id = state['testId']
                outputs.setdefault(system_id, {})
                assert test_id not in outputs[system_id], f'Duplicate systemId/testId: {system_id}/{test_id}'
                outputs[system_id][test_id] = state['variables']['outputs']
    for system_id in outputs:
        with open(join(output_dir, CLASSIFICATIONS_FILE(system_id)), mode='w') as fd:
            classify_system_states(outfile=fd, system_id=system_id, original_system=original_system, outputs=outputs[system_id], reference=outputs[original_system])

def combine_classifications_dir(target_dir, other_dirs):
    classifications = {}
    for child in listdir(target_dir):
        file = join(target_dir, child)
        if isfile(file) and IS_CLASSIFICATIONS_FILE(child):
            classification = Classification.from_file(file)
            classifications.setdefault(classification.system_id, set())
            classifications[classification.system_id].add(classification)
    for other_dir in other_dirs:
        for child in listdir(other_dir):
            file = join(other_dir, child)
            if isfile(file) and IS_CLASSIFICATIONS_FILE(child):
                classification = Classification.from_file(file)
                classifications.setdefault(classification.system_id, set())
                classifications[classification.system_id].add(classification)
    for system_id in classifications:
        classification = Classification.combined(classifications[system_id])
        classification.to_file(join(target_dir, CLASSIFICATIONS_FILE(system_id)))

def main():
    import sys
    if len(sys.argv) != 4:
        print('./classify_states.py <STATES_DIR> <OUTPUT_DIR> <ORIGINAL_SYSTEM>')
        exit(1)
    classify_states(states_dir=sys.argv[1], output_dir=sys.argv[2], original_system=sys.argv[3])
