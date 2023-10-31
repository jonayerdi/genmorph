#!/usr/bin/env python3
'''
Constants for all the scripts
'''

import os
from os.path import abspath, dirname, join, realpath, isdir
import sys

# Separators we use for file/directory naming schemes.
# Some characters like '$' or '%' can break tools like Maven on Linux.
# Keep in sync with the definitions for Java: ch.usi.gassert.util.FileUtils
SEPARATORS = ["@", "?"]

SCRIPTS_ROOT = realpath(dirname(realpath(__file__)))
PROJECT_ROOT = realpath(join(SCRIPTS_ROOT, '..'))

from tools.evosuite import DEFAULT_EVOSUITE_HOME, DEFAULT_EVOSUITE_JAR
from tools.major import DEFAULT_MAJOR_HOME
from tools.pitest import DEFAULT_PITEST_HOME, DEFAULT_PITEST_JAR
from tools.randoop import DEFAULT_RANDOOP_HOME, DEFAULT_RANDOOP_JAR

GASSERT_JAR = join('build', 'libs', 'GAssert-1.0-SNAPSHOT-all.jar')
GASSERT_HEAP = '16g'

if sys.platform == 'win32':
    def find_subdir_with_prefix(dir, prefix):
        for subdir in os.listdir(dir):
            path = join(dir, subdir)
            if subdir.startswith(prefix) and isdir(path):
                return path
        return None
    JAVA_INSTALL_PATH = 'C:\\Program Files\\Java'
    JAVA8 = find_subdir_with_prefix(JAVA_INSTALL_PATH, 'jdk1.8')
elif sys.platform.startswith('linux'):
    JAVA8 = '/usr/lib/jvm/java-8-openjdk-amd64'
else:
    JAVA8 = None

EVOSUITE_HOME = abspath(os.environ.get('EVOSUITE_HOME', DEFAULT_EVOSUITE_HOME))
EVOSUITE_JAR = os.environ.get('EVOSUITE_JAR', DEFAULT_EVOSUITE_JAR)
RANDOOP_HOME = abspath(os.environ.get('RANDOOP_HOME', DEFAULT_RANDOOP_HOME))
RANDOOP_JAR = os.environ.get('RANDOOP_JAR', DEFAULT_RANDOOP_JAR)
PITEST_HOME = abspath(os.environ.get('PITEST_HOME', DEFAULT_PITEST_HOME))
PITEST_JAR = os.environ.get('PITEST_JAR', DEFAULT_PITEST_JAR)
MAJOR_HOME = abspath(os.environ.get('MAJOR_HOME', DEFAULT_MAJOR_HOME))

EVOSUITE_TEST_DEPENDENCIES = [join(EVOSUITE_HOME, EVOSUITE_JAR)]
EVOSUITE_RUN_DEPENDENCIES = [join(EVOSUITE_HOME, EVOSUITE_JAR), GASSERT_JAR] # Should probably use the standalone Evosuite runtime, but then JUnit needs to be added separately
RANDOOP_TEST_DEPENDENCIES = [join(RANDOOP_HOME, RANDOOP_JAR)]
RANDOOP_RUN_DEPENDENCIES = [GASSERT_JAR]
