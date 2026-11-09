import time
import json

# --

def get_users(client, count=None, page=0):
    if count is None: return [i for i in json.loads(client.request("GET", f"/admin/users")["data"]) if i["applicationsCount"] > 0]
    return [i for i in json.loads(client.request("GET", f"/admin/users?count={count}&page={page}")["data"]) if i["applicationsCount"] > 0]


def get_applications(client, user_id):
    return [i for i in json.loads(client.request("GET", f"/admin/applications?userId={user_id}")["data"]) if i["worksCount"] > 0]


def get_works(client, application_id):
    return json.loads(client.request("GET", f"/admin/works?appId={application_id}")["data"])


def get_all_data(client, request_delay=0.1, **kwargs):
    users_count = len(users := get_users(client, **kwargs))
    time.sleep(request_delay)

    for uid, user in enumerate(users):
        user["applications"] = get_applications(client, user["userId"])
        application_count = len(user["applications"])
        time.sleep(request_delay)

        for aid, application in enumerate(user["applications"]):
            print(f"U {uid:>3}/{users_count:>3} : A {aid:>3}/{application_count:>3}")

            application["works"] = get_works(client, application["appId"])
            time.sleep(request_delay)

    return users
