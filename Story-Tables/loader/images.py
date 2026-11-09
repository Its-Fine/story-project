import time
import utilities.file as file


def get_all_images(client, users, save_path, request_delay=0.1):
    usize = len(users)

    for uid, user in enumerate(users):
        asize = user["applicationsCount"]

        for aid, application in enumerate(user["applications"]):
            wsize = application["worksCount"]

            for wid, work in enumerate(application["works"]):
                print(f"U {uid:>3}/{usize:>3} : A {aid:>3}/{asize:>3} : W {wid:>3}/{wsize:>3}")

                if not work["broken"] and not work.get("downloaded"):
                    work["downloaded"] = True
                    filedata = client.request("GET", f"/admin/works/{work["workId"]}/file")["data"]
                    file.save(f"{save_path}/{work["fileName"]}", filedata, binary=True)
                    time.sleep(request_delay)

    return users
