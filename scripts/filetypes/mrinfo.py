from typing import Callable, Iterator, Set

class MRInfo:
    """
    Class which holds MR-related metadata (MR, source, followup and MR-specific data)
    """
    SEPARATOR = ','
    ATTRIBUTES = ['mr', 'source', 'followup']
    def __init__(self, mr: str=None, source: str=None, followup: str=None):
        self.mr = mr
        self.source = source
        self.followup = followup
    def __dir__(self):
        return MRInfo.ATTRIBUTES
    def __str__(self):
        return self.as_csv()
    def __repr__(self):
        return self.as_csv()
    def as_csv(self) -> str:
        return MRInfo.SEPARATOR.join((getattr(self, attr) for attr in MRInfo.ATTRIBUTES))
    @staticmethod
    def from_csv(src: str):
        row = src.strip().split(MRInfo.SEPARATOR)
        return MRInfo(mr=row[0], source=row[1], followup=row[2])

class MRInfoDB:
    """
    Database of MRInfo entries
    """
    FILENAME = 'MRInfo.csv'
    def __init__(self, path=None):
        self.clear()
        if path is not None:
            self.read_from_csv(path=path)
    def __iter__(self):
        for mrinfo in self.entries:
            yield mrinfo
    def get_unique_mrs(self) -> Set[str]:
        return set(map(lambda r: r.mr, self))
    def get_unique_source_testcases(self) -> Set[str]:
        return set(map(lambda r: r.source, self))
    def get_unique_followup_testcases(self) -> Set[str]:
        return set(map(lambda r: r.followup, self))
    def get_unique_testcases(self) -> Set[str]:
        return self.get_unique_source_testcases() | self.get_unique_followup_testcases()
    def add(self, mr_info: MRInfo):
        self.entries.append(mr_info)
    def extend(self, relations: Iterator[MRInfo]):
        for relation in relations:
            self.add(relation)
    def get_filtered(self, f: Callable[[MRInfo], bool]) -> Iterator[MRInfo]:
        for mrinfo in filter(f, self.entries):
            yield mrinfo
    def clear(self):
        self.entries = []
    def read_from_csv_lines(self, lines):
        assert next(lines).strip().split(MRInfo.SEPARATOR) == MRInfo.ATTRIBUTES
        for line in lines:
            if line and not line.isspace():
                self.add(MRInfo.from_csv(line))
    def read_from_csv(self, path):
        with open(path, mode='r') as fd:
            self.read_from_csv_lines(iter(fd))
    def write_to_csv(self, path, **kwargs):
        with open(path, mode='w') as fd:
            fd.write(MRInfo.SEPARATOR.join(MRInfo.ATTRIBUTES) + '\n')
            for entry in self.entries:
                fd.write(MRInfo.as_csv(entry, **kwargs) + '\n')
