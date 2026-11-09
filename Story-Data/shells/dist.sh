#!/usr/bin/bash

cd "$(dirname "$(realpath "$0")")/.." || exit 1

ROOT=/story
FLAG=$1

if [[ "$FLAG" != "disable" && "$FLAG" != "enable" ]]; then
    echo -e "Argument must be 'enable' / 'disable'\n"
    exit 1
fi

if [[ "$FLAG" == "disable" && ! -d "$ROOT/dist-disabled" ]]; then
    mv "$ROOT/dist" "$ROOT/dist-disabled" || exit 1
    mv "$ROOT/dist-troubles" "$ROOT/dist" || exit 1
    exit 0
fi

if [[ "$FLAG" == "enable" && -d "$ROOT/dist-disabled" ]]; then
    mv "$ROOT/dist" "$ROOT/dist-troubles" || exit 1
    mv "$ROOT/dist-disabled" "$ROOT/dist" || exit 1
    exit 0
fi

echo -e "Runtime error\n"
exit 1
