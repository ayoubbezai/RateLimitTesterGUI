
# 🔒 Flask Rate-Limited API for Ethical Testing

This is a simple Flask API designed for **ethical testing** of rate-limiting mechanisms.

It includes:
- ✅ A `/test` endpoint supporting both GET and POST
- ✅ A built-in rate limit of **50 requests per minute per IP**
- ✅ Ideal for testing your own clients, rate-limiting logic, or learning API security

> ⚠️ **This project is strictly for educational and ethical use.**
>
> Do not use this to test external APIs or services you don’t own or have written permission to test. The author is not responsible for misuse.

---

## 🚀 Features

- 🧩 Built using Flask and Flask-Limiter
- ⏱️ Automatic 429 responses if the rate limit is exceeded
- 🔄 Accepts both GET and JSON-based POST requests
- 💡 Useful for simulating load and understanding rate-limiting behavior

---

## 🛠️ Setup Instructions

### 1. Clone this repository

```bash
git clone https://github.com/YOUR_USERNAME/rate-limit-api.git
cd rate-limit-api
```

### 2. Install dependencies

```bash
pip install flask flask-limiter
```

### 3. Run the Flask server

```bash
python app.py
```

Server will start at:
```
http://127.0.0.1:5000/test
```

---

## 🔗 API Endpoints

### 🔹 GET `/test`

```bash
curl http://127.0.0.1:5000/test
```

**Response:**
```json
{
  "message": "GET request received"
}
```

---

### 🔹 POST `/test`

Send a JSON payload:

```bash
curl -X POST http://127.0.0.1:5000/test   -H "Content-Type: application/json"   -d '{"username": "testuser", "password": "1234"}'
```

**Response:**
```json
{
  "message": "POST request received",
  "your_data": {
    "username": "testuser",
    "password": "1234"
  }
}
```

---

## ⛔ Rate Limiting

- The API uses **Flask-Limiter** to enforce a **limit of 50 requests per minute** per IP.
- If the limit is exceeded, the server responds with:

**HTTP 429 Too Many Requests**
```json
{
  "message": "Rate limit exceeded: 50 per 1 minute"
}
```

---

## 🧪 How to Test the Rate Limit

You can use:
- `curl` in a `for` loop
- Postman (with a runner)
- A Python/Java multithreaded script (only against **your own server**)
- Load testing tools like `locust`, `hey`, or `ab`

✅ Make sure you’re only testing on **localhost** or servers you are authorized to test.

---

## 👨‍💻 Example Python Client (Optional)

```python
import requests

url = "http://127.0.0.1:5000/test"
for i in range(60):
    response = requests.get(url)
    print(i+1, response.status_code, response.text)
```

---

## 📂 Example `app.py` Server Code

```python
from flask import Flask, request, jsonify
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

app = Flask(__name__)

limiter = Limiter(
    key_func=get_remote_address,
    default_limits=["50 per minute"]
)
limiter.init_app(app)

@app.route('/test', methods=['GET', 'POST'])
@limiter.limit("50 per minute")
def test_endpoint():
    if request.method == 'POST':
        data = request.get_json() or {}
        return jsonify({
            "message": "POST request received",
            "your_data": data
        })
    return jsonify({
        "message": "GET request received"
    })

if __name__ == '__main__':
    app.run(debug=True, port=5000)
```

---

## 📄 License

This project is licensed under the **MIT License** — feel free to use and modify it for ethical and educational purposes.

---

## 🙏 Credits

Built as part of a web security university project to demonstrate API rate limiting and ethical testing practices.
