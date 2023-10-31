from os.path import join, split, realpath

TEMPLATE = realpath(join('configs', 'evaluation-config-template.json'))
FILENAME = lambda v: f'evaluation-config-seed{v}.json'
VALUES = list(map(str, range(12)))
KEY = '%SEED%'

def instantiate_template():
    template = None
    with open(TEMPLATE, mode='r') as fp:
        template = fp.readlines()
    for value in VALUES:
        filename = join(split(TEMPLATE)[0], FILENAME(value))
        with open(filename, mode='w') as fp:
            for line in template:
                fp.write(line.replace(KEY, value))

def main():
    instantiate_template()
