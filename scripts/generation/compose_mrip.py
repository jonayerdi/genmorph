#!/usr/bin/env python3
'''
Compose a full MR (Input Relation => Output Relation) by combining the MRIPs file generated by `ch.usi.gassert.mrip.MRIPGenerator`,
a selected MRIP, and an output relation generated by GAssertMRs.

Arguments:
    MRIPs file
    MRIP
    Output relation file
    MR file

Example usage:
    ./compose_mrip.py demo/baseline1_assertions/demo.MyClass%cos%0.mrip.txt MRIP2 demo/baseline1_assertions/demo.MyClass%cos%0_MRIP2.txt demo/baseline1_assertions/demo.MyClass%cos%0_MRIP2_Full.txt 
'''

from subprocess import Popen

from config import *

MAIN_CLASS = 'ch.usi.gassert.mrip.MRIPComposer'

def compose_mrip(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None):
    args = ['java', '-cp', gassert_jar, main_class, *map(str, args)]
    return Popen(args=args, stdout=stdout).wait()

def main():
    import sys
    compose_mrip(args=sys.argv[1:])