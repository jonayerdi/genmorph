#!/usr/bin/env python3

import sys
from os.path import realpath, join
sys.path.append(realpath(join(__file__, '..', '..')))

from states.method_test_rename import main

if __name__ == '__main__':
    main()
