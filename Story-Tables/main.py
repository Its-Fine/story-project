import json

import utilities.file as file
from compiler import create_table
from loader import Client, get_all_images, get_all_data

# --

SECRET_PATH = "temp\\secret.json"


def main_1(save_path, version, data_local=False, image_load=False, **kwargs):
    secret_file = open(SECRET_PATH, "r")
    secrets = json.load(secret_file)
    secret_file.close()

    client = Client("https://fairytale-design.kosgos.ru/api/v1", secrets["credentials"])

    if data_local: users = json.loads(file.load(f"{save_path}\\story_{version}\\data.json", encoding="utf-8"))
    else: users = get_all_data(client, **kwargs)

    if image_load:
        try: users = get_all_images(client, users, f"{save_path}\\story_{version}\\images", **kwargs)
        except Exception as e: print(e)

    file.save(f"{save_path}\\story_{version}\\data.json", json.dumps(users, ensure_ascii=False, indent=4), encoding="utf-8")


def main_2(load_path, load_version):
    secret_file = open(SECRET_PATH, "r")
    secrets = json.load(secret_file)
    secret_file.close()

    data = json.loads(file.load(f"{load_path}\\story_{load_version}\\data.json", encoding="utf-8"))

    #

    contests = set()
    for user in data:
        for app in user["applications"]:
            contests.add(app["contestName"])

    divided_data = {i: {} for i in contests}
    for user in data:
        for app in user["applications"]:
            if divided_data[app["contestName"]].get(user["userId"]):
                divided_data[app["contestName"]][user["userId"]]["applications"].append(app.copy())
            else:
                divided_data[app["contestName"]][user["userId"]] = user.copy()
                divided_data[app["contestName"]][user["userId"]]["applications"] = [app.copy()]

    divided_data_list = {i: divided_data[i].values() for i in divided_data}

    #

    for data_list in divided_data_list:
        age_groups = {10: {}, 14: {}, 18: {}, 35: {}, 999: {}}
        age_groups_names = {
            10: "Младшая группа (10-13 лет)",
            14: "Средняя группа (14-17 лет)",
            18: "Старшая возрастная группа (18-35 лет)",
            35: "Художники, дизайнеры, преподаватели",
            999: "Нет группы"
        }

        for user in divided_data_list[data_list]:
            for app in user["applications"]:
                if app.get("age") is not None:
                    diff = {app["age"] - i: i for i in age_groups if app["age"] - i >= 0}
                    age_group = diff[min(diff.keys())]
                else:
                    age_group = 999

                if age_groups[age_group].get(user["userId"]):
                    age_groups[age_group][user["userId"]]["applications"].append(app.copy())
                else:
                    age_groups[age_group][user["userId"]] = user.copy()
                    age_groups[age_group][user["userId"]]["applications"] = [app.copy()]

        divided_data_list[data_list] = {age_groups_names[i][0:31]: age_groups[i].values() for i in age_groups}

    #

    for i in divided_data_list:
        create_table(divided_data_list[i], f"{load_path}\\reports\\{i}.xlsx", f"{load_path}\\story_{load_version}\\images", secrets["yandex_link"])

# --

if __name__ == '__main__':
    # main_1("temp", "2", data_local=True, image_load=True, request_delay=0.2)
    # main_2("temp", "3")
    pass
