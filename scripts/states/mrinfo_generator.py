#!/usr/bin/env python3
'''
Generate MRInfo files from a given set of test executions + MRIPs file.

Example usage:
    ./mrinfo_generator.py <STATES_DIR> <ORIGINAL_SYSTEM_ID> <MRIPS_FILE> <MRINFOS_FILE>
'''

from subprocess import Popen

from config import *

MAIN_CLASS = 'ch.usi.gassert.mrip.MRInfoGenerator'

def mrinfo_generator(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    mrinfo_generator(args=sys.argv[1:])
