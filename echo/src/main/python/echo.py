from flask import Flask, request

app = Flask(__name__)


@app.route("/")
def hello():
    return "Hello!"

@app.route("/health")
def health():
    return "", 200

@app.route("/user_tags", methods=["GET", "POST"])
def user_tags():
    return "", 204

@app.route("/user_profiles/<cookie>", methods=["GET", "POST"])
def user_profiles(cookie: str):
    return request.data, 200

@app.route("/aggregates", methods=["GET", "POST"])
def user_aggregates():
    return request.data, 200
    
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
