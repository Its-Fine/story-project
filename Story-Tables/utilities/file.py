import os


def save(filename: str, data, binary=False, **kwargs):
    os.makedirs('/'.join(filename.split('/')[0:-1]), exist_ok=True)
    with open(filename, "wb" if binary else "w", **kwargs) as file: file.write(data)


def load(filename: str, binary=False, **kwargs):
    with open(filename, "rb" if binary else "r", **kwargs) as file: return file.read()
