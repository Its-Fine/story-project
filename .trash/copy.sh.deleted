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
#!/usr/bin/bash

REPO=/home/vladimir/repositories
ROOT=/story

# --

sudo mkdir -p $ROOT
sudo chmod 777 $ROOT

# --

cp $REPO/story-data/docker-compose.yml $ROOT
cp -r $REPO/story-data/shells $ROOT
cp -r $REPO/story-data/environments $ROOT
cp -r $REPO/story-data/services $ROOT
cp -r $REPO/story-data/dist-troubles $ROOT

# --

cd $REPO/story-back
echo -n "Ветка story-back: $(git branch --show-current). Выполнить сборку и перенос? [yes/no]: "
read FLAG
if [[ "$FLAG" == "yes" ]]; then
    mvn clean;
    mvn package -DskipTests;
    cp $REPO/story-back/target/*.jar $ROOT/services/spring/app.jar
fi

files=(./target/*.jar)
if [[  "$FLAG" != "yes" && -e "${files[0]}" ]]; then
    echo -n "Содержится ${files[0]}. Выполнить перенос? [yes/no]: "
    read FLAG
    if [[ "$FLAG" == "yes" ]]; then
        cp $REPO/story-back/${files[0]} $ROOT/services/spring/app.jar
    fi
fi

# --

cd $REPO/story-front
echo -n "Ветка story-front: $(git branch --show-current). Выполнить сборку и перенос? [yes/no]: "
read FLAG
if [[ "$FLAG" == "yes" ]]; then
    rm -rf dist/;
    npm run build;
    cp -r $REPO/story-front/dist/ $ROOT/dist-new
fi

if [[  "$FLAG" != "yes" && -d "dist" ]]; then
    echo -n "Содержится dist. Выполнить перенос? [yes/no]: "
    read FLAG
    if [[ "$FLAG" == "yes" ]]; then
        cp -r $REPO/story-front/dist/ $ROOT/dist-new
    fi
fi
