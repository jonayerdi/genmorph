#!/usr/bin/env python3
'''
Runs results_assertion.py over all the assertions from a directory and saves the results into files
'''

import re
from argparse import ArgumentParser
from os.path import isfile, join, split, splitext
from os import makedirs, listdir

from evaluation.results_assertion import results_assertion_file

ASSERTIONS = 'assertions'
STATES = 'states'
RESULTS = 'results'
CLASSIFICATIONS_REGULAR = 'classifications_regular'
CLASSIFICATIONS_METAMORPHIC = 'classifications_metamorphic'
MANAGER_REGULAR = 'ch.usi.gassert.data.manager.method.MethodRegularDataManager'
MANAGER_METAMORPHIC_OR = 'ch.usi.gassert.data.manager.method.MethodMetamorphicORDataManager'
MANAGER_METAMORPHIC_FULL = 'ch.usi.gassert.data.manager.method.MethodMetamorphicFullDataManager'

REGULAR_ASSERTION_REGEX = re.compile(r'^([A-Za-z0-9%]+)_(REGULAR\d*)\.txt$')
MR_OR_REGEX = re.compile(r'^([A-Za-z0-9%]+)_(MRIP\d*)\.txt$')
MR_FULL_REGEX = re.compile(r'^([A-Za-z0-9%]+)_(MR\d*)\.txt$')


RESULTS_FILENAME = lambda filename: f'{splitext(split(filename)[1])[0]}.results.csv'

def results_assertion_dir(root=None, assertions_dir=ASSERTIONS, results_dir=RESULTS, states_dir=STATES, 
        classifications_regular_dir=CLASSIFICATIONS_REGULAR, classifications_metamorphic_dir=CLASSIFICATIONS_METAMORPHIC,
        manager_regular=MANAGER_REGULAR, manager_mr_or=MANAGER_METAMORPHIC_OR, manager_mr_full=MANAGER_METAMORPHIC_FULL,):
    if root is not None:
        assertions_dir = join(root, assertions_dir)
        results_dir = join(root, results_dir)
        states_dir = join(root, states_dir)
        classifications_regular_dir = join(root, classifications_regular_dir)
        classifications_metamorphic_dir = join(root, classifications_metamorphic_dir)
    makedirs(results_dir, exist_ok=True)
    for filename in listdir(assertions_dir):
        assertion_file = join(assertions_dir, filename)
        if isfile(assertion_file):
            match = REGULAR_ASSERTION_REGEX.match(filename)
            manager_class = None
            manager_args = None
            system_name = None
            if match is not None:
                system_name = match.group(1)
                manager_class = manager_regular
                manager_args = f'GASSERT;{join(states_dir, system_name)};{join(classifications_regular_dir, system_name)}'
            match = MR_OR_REGEX.match(filename)
            if match is not None:
                system_name = match.group(1)
                mrip = match.group(2)
                manager_class = manager_mr_or
                manager_args = f'GASSERT;{join(states_dir, system_name)};{join(classifications_metamorphic_dir, system_name)};{mrip}'
            match = MR_FULL_REGEX.match(filename)
            if match is not None:
                system_name = match.group(1)
                manager_class = manager_mr_full
                manager_args = f'GASSERT;{join(states_dir, system_name)};{join(classifications_regular_dir, system_name)}'
            if system_name:
                outfilename = RESULTS_FILENAME(assertion_file)
                with open(join(results_dir, outfilename), mode='w') as fd:
                    assert results_assertion_file(
                        manager_class=manager_class,
                        manager_args=manager_args,
                        assertion_file=assertion_file,
                        stdout=fd
                    ) == 0, f'Could not write results to {join(results_dir, outfilename)}'

def main():
    parser = ArgumentParser(description='Get the results for all the assertions in the given directory.')
    parser.add_argument('--root', help='Root directory', required=True)
    parser.add_argument('--assertions_dir', help=f'Assertions directory, $(dir)/{ASSERTIONS} by default', default=ASSERTIONS)
    parser.add_argument('--results_dir', help=f'Results directory, $(dir)/{RESULTS} by default', default=RESULTS)
    parser.add_argument('--manager_regular', help=f'Data manager class for regular assertions', default=MANAGER_REGULAR)
    parser.add_argument('--manager_mr_or', help=f'Data manager class for MR output relations', default=MANAGER_METAMORPHIC_OR)
    parser.add_argument('--manager_mr_full', help=f'Data manager class for full MRs', default=MANAGER_METAMORPHIC_FULL)
    parser.add_argument('--states_dir', help=f'States dir, $(dir)/{STATES} by default', default=STATES)
    parser.add_argument('--classifications_regular_dir', help=f'Regular classifications dir, $(dir)/{CLASSIFICATIONS_REGULAR} by default', default=CLASSIFICATIONS_REGULAR)
    parser.add_argument('--classifications_metamorphic_dir', help=f'Metamorphic classifications dir, $(dir)/{CLASSIFICATIONS_REGULAR} by default', default=CLASSIFICATIONS_METAMORPHIC)
    args = parser.parse_args()

    results_assertion_dir(
        root=args.root,
        assertions_dir=args.assertions_dir,
        results_dir=args.results_dir,
        states_dir=args.states_dir,
        classifications_regular_dir=args.classifications_regular_dir,
        classifications_metamorphic_dir=args.classifications_metamorphic_dir,
        manager_regular=args.manager_regular,
        manager_mr_or=args.manager_mr_or,
        manager_mr_full=args.manager_mr_full,
    )
