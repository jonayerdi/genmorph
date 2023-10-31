#!/usr/bin/env python3
'''
Class containing information about SUTs (Java methods)
'''

import json

class SUTConfig:
    def __init__(self, root: str, sources: str, suts: dict, classpaths: list=[]):
        self.root = root
        self.sources = sources
        self.suts = suts
        self.classpaths = classpaths
    def __str__(self) -> str:
        return f'{self.as_dict()}'
    def as_dict(self) -> dict:
        return {
            'root': self.root,
            'sources': self.sources,
            'suts': self.suts,
            'classpaths': self.classpaths,
        }
    def iter_methods(self):
        for clazz in self.suts:
            for method in self.suts[clazz]:
                for index in self.suts[clazz][method]:
                    yield clazz, method, index
    def iter_class_methods(self, clazz):
        for method in self.suts[clazz]:
            for index in self.suts[clazz][method]:
                yield method, index
    @staticmethod
    def from_dict(data: dict):
        return SUTConfig(root=data['root'], sources=data['sources'], suts=data['suts'], classpaths=data.get('classpaths', []))
    @staticmethod
    def from_str(data: str):
        return SUTConfig.from_dict(json.loads(data))
    @staticmethod
    def from_file(fp):
        return SUTConfig.from_dict(json.load(fp))
    @staticmethod
    def from_filename(path: str):
        with open(path, mode='r') as fp:
            return SUTConfig.from_file(fp)
