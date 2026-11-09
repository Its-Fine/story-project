import requests
import time


class Client:
    def __init__(self, url, credentials):
        self.credentials = credentials
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.url = url
        self.login()

    def login(self):
        response = self.session.request(method="POST", url=f"{self.url}/auth/login", json=self.credentials)
        if response.status_code != 200:
            raise Exception(response.status_code, response.text)
        self.session.headers["Authorization"] = f"Bearer {response.json()['accessToken']}"

    def request(self, method, path, **kwargs):
        response = self.session.request(method=method, url=f"{self.url}/{path.lstrip('/')}", **kwargs)
        if response.status_code > 399:
            self.login()
            time.sleep(0.5)
            response = self.session.request(method=method, url=f"{self.url}/{path.lstrip('/')}", **kwargs)
            if response.status_code > 399:
                raise Exception(response.status_code, response.text)
        return {"type": response.headers.get("Content-Type"), "data": response.content}
