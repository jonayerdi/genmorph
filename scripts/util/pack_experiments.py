from pathlib import Path
from zipfile import ZipFile

ROOT = Path('.')
SUTs = { 'guava': 'guava-sut', 'lang': 'lang-sut', 'math': 'math-sut' }
SUT_ARTIFACTS = [
    [
        Path(f'configs/sut-config-{sut}.json'),
        Path(f'configs/experiment-config-{sut}.json'),
        Path(f'configs/evaluation-config-{sut}.json'),
        Path(f'configs/config-all-experiment-{sut}-GAssertMRs*.json'),
        Path(f'configs/config-all-evaluation-{sut}-GAssertMRs*.json'),
        Path(f'configs/{SUTs[sut]}/src/main/java/**/*.java'),
        Path(f'configs/{SUTs[sut]}/target/classes/**/*.class'),
    ]
    for sut in SUTs
]

TO_PACK = [
    Path(f'run*.sh'),
    Path(f'build/libs/GAssert-1.0-SNAPSHOT-all.jar'),
    Path(f'libs/oasis/build/libs/OASIs-1.0-SNAPSHOT-all.jar'),
    Path(f'evosuite-1.1.0.jar'),
    Path(f'randoop-all-4.3.0.jar'),
    Path(f'pitest-wrapper-1.7.4.jar'),
    Path(f'scripts/**/*.py'),
]
for sut_artifacts in SUT_ARTIFACTS:
    TO_PACK.extend(sut_artifacts)
PACKED_EXPERIMENTS = 'genmorph-experiments.zip'

def pack_experiments(root=ROOT, to_pack=TO_PACK, packed_experiments=PACKED_EXPERIMENTS):
    with ZipFile(packed_experiments, 'w') as packed:
        for path in to_pack:
            for file in root.glob(str(path)):
                packed.write(str(file))

def main():
    pack_experiments()
