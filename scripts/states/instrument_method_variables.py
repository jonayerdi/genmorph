#!/usr/bin/env python3
'''
Instrument the given Java file and method

./instrument_method.py backup ./demo/demo/MyClass.java sin sin 0
'''

from states.instrument_method import instrument_method

MAIN_CLASS_VALUES = 'ch.usi.gassert.filechange.AddInstrumentationMethodValues'

def instrument_method_variables(args, **kwargs):
    return instrument_method(args=args, main_class=MAIN_CLASS_VALUES, **kwargs)

def main():
    import sys
    instrument_method_variables(args=sys.argv[1:])
