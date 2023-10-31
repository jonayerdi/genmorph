#!/usr/bin/env python3
'''
Handle ConjunctiveMRIPs.

Commands:
    generateMRIPs: Generate MRIPs file from a directory of `.cmrip` files

Example usage:
    ./cmrip.py generateMRIPs <INPUT_DIR> <OUTPUT_FILE>
'''

from subprocess import Popen

from config import *

MAIN_CLASS = 'ch.usi.methodtest.ConjunctiveMRIP'

def cmrip(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    cmrip(args=sys.argv[1:])
