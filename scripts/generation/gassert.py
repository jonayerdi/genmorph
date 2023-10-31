#!/usr/bin/env python3
'''
Run GAssertMRs, a tool to automatically generate test oracles (regular assertions or metamorphic relations)
'''

from subprocess import Popen

from config import *

from tools.java import copy_jar_class

MAIN_CLASS = 'ch.usi.gassert.GAssertMRs'

def gassert_copy_class(classes, path, gassert_jar=GASSERT_JAR):
    return copy_jar_class(classes=classes, path=path, jar=gassert_jar)

def gassert(args, gassert_jar=GASSERT_JAR, main_class=MAIN_CLASS, stdout=None, heapsize=GASSERT_HEAP):
    pargs = ['java']
    if heapsize is not None:
        pargs.extend([f'-Xmx{heapsize}'])
    pargs.extend(['-cp', gassert_jar, main_class, *map(str, args)])
    return Popen(args=pargs, stdout=stdout).wait()

def main():
    import sys
    gassert(args=sys.argv[2:], main_class=sys.argv[1])
