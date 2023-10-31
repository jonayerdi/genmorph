#!/usr/bin/env python3
'''
Remove irrelevant mutants from directory.
Specifically, it searches for any *.class files, and assumes that any mutant that has not been compiled is irrelevant.

./remove_unused_mutants.py <MUTANTS_DIR>
'''

from os import listdir
from os.path import join, isdir
from shutil import rmtree

from states.generate_test_executions import REGEX_SYSTEM_ID

def remove_unused_mutants(mutants_dir):
    for md in listdir(mutants_dir):
        md = join(mutants_dir, md)
        if isdir(md):
            irrelevant = True
            for c in listdir(md):
                if REGEX_SYSTEM_ID.match(c) is not None:
                    irrelevant = False
                    break
            if irrelevant:
                rmtree(md)

def main():
    import sys
    if len(sys.argv) != 2:
        print('./remove_unused_mutants.py <MUTANTS_DIR>')
        exit(1)
    remove_unused_mutants(mutants_dir=sys.argv[1])
