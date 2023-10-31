
from os import makedirs
from os.path import join, realpath
from shutil import rmtree

from generation.gassert import gassert_copy_class
from util.fsutil import split_path

def get_java_url_path(root):
    '''
    This function generates paths that should work for ClassLoader URLs
    '''
    parts = split_path(realpath(root))
    path = '/'.join(parts)
    if parts[0] == '/':
        # Avoid double leading '/'s on unix paths: //home/...
        path = path[1:]
    return path

OASIS_GASSERT_DEPENDENCIES = [
    *[
        f'ch.usi.gassert.data.types.{clazz}' for clazz in
        ('ErrorValue', 'Sequence', 'Sequence$ArraySequence', 'Sequence$ListSequence', 'Sequence$StringSequence')
    ],
    'ch.usi.gassert.util.ClassUtils',
]

def oasis_before_gassert(root):
    # Make OASIs tests directory
    oasis_tests_dir = join(root, OASIS_TESTS_DIR)
    rmtree(oasis_tests_dir, ignore_errors=True)
    makedirs(oasis_tests_dir)
    # Copy GAssert-generated code dependencies to SUT target directory
    target_dir = join(root, 'target', 'classes')
    gassert_copy_class(classes=OASIS_GASSERT_DEPENDENCIES, path=target_dir)

STATES_UPDATERS = {
        'NullStatesUpdater': (
            'ch.usi.gassert.data.state.updater.NullStatesUpdater',
            lambda root, sut_class, method_name: '',
            lambda root: None,
        ),
    }

OASIS_TESTS_DIR = 'oasis_tests'

class StatesUpdater:
    def __init__(self, name):
        self.clazz, self.args, self.before_gassert = STATES_UPDATERS[name]
