#!/usr/bin/env python3
'''
Checks <STATES_DIR> for GAssert states, and validates them.

./check_gassert_states.py <STATES_DIR>
'''

import json
from os import listdir
from os.path import join, isdir, isfile

from config import SEPARATORS

def get_all_vars(states_dir):
    all_vars = {'inputs': set(), 'outputs': set()}
    for file in filter(lambda f: f.endswith('.state.json') and f'{SEPARATORS[0]}original{SEPARATORS[0]}' in f, listdir(states_dir)):
        state = join(states_dir, file)
        with open(state, mode='r') as fp:
            vars = json.load(fp)['variables']
        all_in = all(map(lambda var: var in vars['inputs'], all_vars['inputs']))
        all_out = all(map(lambda var: var in vars['outputs'], all_vars['outputs']))
        if all_in:
            all_vars['inputs'] = set(vars['inputs'].keys())
        if all_out:
            all_vars['outputs'] = set(vars['outputs'].keys())
    return all_vars

def check_gassert_state_inputs(state, reference_state):
    if state['testId'] != reference_state['testId']:
        return 'Test IDs do not match'
    if state['variables']['inputs'] != reference_state['variables']['inputs']:
        return 'Inputs do not match with reference state'
    return None

def check_gassert_state_variable(value):
    if type(value) in [int, float, bool]:
        return None # Boolean or Numeric
    if type(value) is str and value.startswith('$'):
        return None # Sequence: String
    if type(value) is list and value[0] in ['A', 'L']:
        return None # Sequence: Array or List
    return f'Invalid variable value: {value}'

def check_gassert_state(state_file, all_vars=None, reference_state_file=None):
    try:
        with open(state_file, mode='r') as fp:
            state = json.load(fp)
            for key in ['systemId', 'testId', 'variables']:
                if key not in state:
                    return f'Missing key: {key}'
            for key in ['inputs', 'outputs']:
                if key not in state['variables']:
                    return f'Missing key: variables.{key}'
            for var in state['variables']['inputs']:
                problem = check_gassert_state_variable(state['variables']['inputs'][var])
                if problem is not None:
                    return f'Input variable {var}: {problem}'
            for var in state['variables']['outputs']:
                problem = check_gassert_state_variable(value=state['variables']['outputs'][var])
                if problem is not None:
                    return f'Output variable {var}: {problem}'
            if all_vars is not None:
                for var in all_vars['inputs']:
                    if var not in state['variables']['inputs']:
                        return f'Input variable {var}: Missing'
                for var in all_vars['outputs']:
                    if var not in state['variables']['outputs']:
                        return f'Output variable {var}: Missing'
            if reference_state_file is not None:
                if not isfile(reference_state_file):
                    return state_file, 'Reference file missing'
                with open(reference_state_file, mode='r') as fp2:
                    reference_state = json.load(fp2)
                    return check_gassert_state_inputs(state, reference_state)
    except Exception:
        return 'Cannot parse file'
    return None

def check_gassert_states(states_dir):
    assert isdir(states_dir), f'{states_dir} is not a directory'
    all_vars = get_all_vars(states_dir)
    for state_filename in listdir(states_dir):
        state_file = join(states_dir, state_filename)
        if isfile(state_file) and state_filename.endswith('.state.json'):
            problem = check_gassert_state(state_file=state_file, all_vars=all_vars)
            if problem is not None:
                yield state_file, problem

def main():
    import sys
    if len(sys.argv) != 2:
        print('./check_gassert_states.py <STATES_DIR>')
        exit(0)
    for state_file, problem in check_gassert_states(sys.argv[1]):
        print(f'{state_file}: {problem}')
