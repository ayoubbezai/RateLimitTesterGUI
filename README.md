
# 🔒 Rate Limiting Educational Project (Java + Python)

This repository contains a full-stack **ethical API security testing project** with:

- ✅ A **Java GUI application** to test rate limits
- ✅ A **Flask-based Python server** with rate-limited endpoints
- ✅ Support for both GET and POST
- ✅ Logging, stats, response times, and more
- ✅ Bonus: Basic CRUD and multithreading examples in Java

> ⚠️ **Ethical Use Only:** This tool is created solely for educational purposes to demonstrate how rate limiting works. Do not use this to target any system without full authorization.

---

## 📦 Project Structure

```
📁 rate-limit-project/
├── 📁 java-client/           # Java GUI app with threading
│   └── RateTester.java
├── 📁 python-server/         # Flask API with rate limit
│   └── app.py
└── README.md                 # This file
```

---

## 🧠 What You Learn

- 🔄 API request automation in Java
- 🧵 Multithreading and concurrency
- 🧪 How to test server rate limits
- 🎨 Simple Java Swing GUI
- 🔐 Flask server security features
- 📊 Approximate rate limit detection

---

## 🖥️ Java GUI Tester

### ✅ Features

- Choose request type: GET or POST
- Enter custom endpoint and JSON payload
- Launch multithreaded requests
- Detect server rate limit (req/min)
- Stop on rate-limit detection
- Output logs and response times

### 🚀 Run Java Client

Compile and run:

```bash
javac RateTester.java
java RateTester
```

Or use your IDE like IntelliJ or Eclipse.

---

## 🧪 Python Flask Server

### ✅ Features

- `/test` endpoint with GET and POST
- Built-in **50 requests/minute rate limit**
- Automatic 429 error handling
- JSON body handling in POST

### 📦 Install Dependencies

```bash
pip install flask flask-limiter
```

### ▶️ Run the Server

```bash
python app.py
```

Server starts on: `http://127.0.0.1:5000/test`

---

## 🧪 API Usage

### 🔹 GET Example

```bash
curl http://127.0.0.1:5000/test
```

### 🔹 POST Example

```bash
curl -X POST http://127.0.0.1:5000/test -H "Content-Type: application/json" -d '{"username": "test", "password": "123"}'
```

---

## 💡 Advanced Features (Java)

- Multithreaded stress test using `Executors`
- Auto-measure actual rate limit per minute
- Stops all threads after first 429 error
- JSON editor only appears if POST is selected
- Swing-based GUI with input validation

---

## 📊 Example Output (Java App)

```
Starting 100 requests with 10 threads
Request 37 failed with status 429 (Rate Limit)
Stopped after hitting rate limit.
Approx. Rate Limit: 48.5 req/min
```

---

## 📄 License

This project is open-source under the **MIT License**.

---

## 🙏 Credits

Built for a university project on **web security and API testing**. This repository is intended for **educational and ethical testing only**.
